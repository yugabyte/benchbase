/*
 * Copyright 2020 by OLTPBenchmark Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.oltpbenchmark.benchmarks.featurebench;

import com.oltpbenchmark.api.Procedure.UserAbortException;
import com.oltpbenchmark.api.TransactionType;
import com.oltpbenchmark.api.Worker;
import com.oltpbenchmark.benchmarks.featurebench.helpers.UtilToMethod;
import com.oltpbenchmark.benchmarks.featurebench.utils.BaseUtil;
import com.oltpbenchmark.benchmarks.featurebench.utils.ExpressionEval;
import com.oltpbenchmark.benchmarks.featurebench.workerhelpers.ExecuteRule;
import com.oltpbenchmark.benchmarks.featurebench.workerhelpers.Query;
import com.oltpbenchmark.types.DatabaseType;
import com.oltpbenchmark.types.State;
import com.oltpbenchmark.types.TransactionStatus;
import com.oltpbenchmark.util.FileUtil;
import com.yugabyte.util.PSQLException;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.json.JSONObject;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;




/**
 *
 */
public class FeatureBenchWorker extends Worker<FeatureBenchBenchmark> {
    private static final Logger LOG = LoggerFactory.getLogger(FeatureBenchWorker.class);
    // SQL strings and explain plans are collected verbatim and can balloon the detailed results
    // JSON (e.g. multi-row INSERTs inline every value). Beyond this length they are trimmed, since
    // only the standard SQL structure is of interest, not the actual bound values.
    private static final int MAX_COLLECTED_STRING_LENGTH = 1000;
    private final HierarchicalConfiguration<ImmutableNode> config;
    private YBMicroBenchmark ybm;
    private final List<ExecuteRule> executeRules;
    private final String workloadName;
    private HashMap<String, PreparedStatement> preparedStatementsPerQuery;
    public static Map<String,JSONObject> queryToExplainMap = new HashMap<>();

    static AtomicBoolean isInitializeDone = new AtomicBoolean(false);

    // tearDown() runs twice per worker -- once from the worker thread (Worker.run) and
    // once from the master thread (ThreadBench.finalizeWorkers) -- and each worker closes
    // its own connection at the end of tearDown(). These "run exactly once" guards make
    // the global collection / user cleanup happen a single time, on the FIRST invocation,
    // whose connection is still open. (A count/"last worker" gate is unsafe here: it fires
    // on a later invocation whose connection has already been closed, which surfaces as a
    // "connection closed" failure while collecting pg_stat_statements.)
    static AtomicBoolean isPGStatStatementCollected = new AtomicBoolean(false);
    static AtomicBoolean isCleanUpDone = new AtomicBoolean(false);

    public FeatureBenchWorker(FeatureBenchBenchmark benchmarkModule,
                              int id,
                              String workloadClass,
                              HierarchicalConfiguration<ImmutableNode> workerConfig,
                              List<ExecuteRule> executeRules,
                              String workloadName) {
        super(benchmarkModule, id);
        this.executeRules = executeRules;
        this.config = workerConfig;
        this.workloadName = workloadName;
        // NOTE: per-workload shared state is reset once in
        // FeatureBenchBenchmark.makeWorkersImpl (via resetSharedState()) before any
        // worker is constructed -- not here -- so the "run once" guards do not depend
        // on the ordering of individual worker constructors.
        try {
            ybm = (YBMicroBenchmark) Class.forName(workloadClass)
                .getDeclaredConstructor(HierarchicalConfiguration.class)
                .newInstance(config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reset the shared, static per-workload state. Must be called exactly once per
     * workload run, BEFORE the workers for that run are created (see
     * FeatureBenchBenchmark.makeWorkersImpl). Doing it here rather than in each worker
     * constructor keeps the "run once" guards correct regardless of construction order
     * or worker reuse, and clears the accumulated explain output between workloads.
     */
    public static void resetSharedState() {
        isInitializeDone.set(false);
        isPGStatStatementCollected.set(false);
        isCleanUpDone.set(false);
        queryToExplainMap.clear();
    }

    protected void initialize() {
        try {
            preparedStatementsPerQuery = new HashMap<>();
            for (ExecuteRule executeRule : executeRules) {
                for (Query query : executeRule.getQueries()) {
                    String queryStmt = query.getQuery();
                    PreparedStatement stmt = conn.prepareStatement(queryStmt);
                    preparedStatementsPerQuery.put(queryStmt, stmt);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        if (this.getWorkloadConfiguration().getIsOptimalThreadsWorkload()) {
            return;
        }

        if (isInitializeDone.get()) return;
        synchronized (FeatureBenchWorker.class) {
            if (isInitializeDone.get()) return;
            try {
            if (this.getWorkloadConfiguration().getXmlConfig().getBoolean("collect_pg_stat_statements", false)) {
                LOG.info("Resetting pg_stat_statements for workload : " + this.workloadName);
                try {
                    Statement stmt = conn.createStatement();
                    stmt.executeQuery("SELECT pg_stat_statements_reset();");
                    if (!conn.getAutoCommit())
                        conn.commit();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to reset pg_stat_statements for workload '"
                        + this.workloadName + "'. Is the pg_stat_statements extension installed?", e);
                }

            }
            // For Postgres, we need to reset pg_stat_user_indexes
            if (this.getWorkloadConfiguration().getDatabaseType().equals(DatabaseType.POSTGRES)) {
                LOG.info("Resetting pg_stat_user_indexes for workload : " + this.workloadName);
                try {
                    Statement stmt = conn.createStatement();
                    stmt.executeQuery("SELECT pg_stat_reset();");
                    if (!conn.getAutoCommit())
                        conn.commit();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to reset pg_stat (pg_stat_reset) for workload '"
                        + this.workloadName + "'.", e);
                }
            }

            if (this.getWorkloadConfiguration().getXmlConfig().getBoolean("analyze_on_all_tables", false)) {
                LOG.info("Running analyze on all tables");
                try {
                    Statement stmt = conn.createStatement();
                    stmt.execute("ANALYZE;");
                    if (!conn.getAutoCommit())
                        conn.commit();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to run ANALYZE for workload '"
                        + this.workloadName + "'.", e);
                }
            }

            String outputDirectory = "results";
            FileUtil.makeDirIfNotExists(outputDirectory);

            String explainSelect = "explain (analyze,verbose,costs,buffers) ";
            String explainUpdate = "explain (analyze) ";
            String explainOthers = "explain ";

            if (this.getWorkloadConfiguration().getXmlConfig().getBoolean("force_capture_explain_analyze", false)) {
                explainOthers = "explain (analyze,verbose,costs,buffers) ";
            }
            if (this.getWorkloadConfiguration().getXmlConfig().containsKey("use_dist_in_explain")
                && this.getWorkloadConfiguration().getXmlConfig().getBoolean("use_dist_in_explain")) {
                if (this.getWorkloadConfiguration().getXmlConfig().getString("type").equalsIgnoreCase("YUGABYTE")) {
                    explainSelect = explainSelect.replace(") ", ",debug,dist) ");
                    explainUpdate = explainUpdate.replace(") ", ",debug,dist) ");
                    explainOthers = explainOthers.replace(") ", ",debug,dist) ");
                } else {
                    throw new RuntimeException("dist and debug option for explain not supported by this database type, Please remove key!");
                }
            }

            Map<String, PreparedStatement> explainDDLMap = new LinkedHashMap<>();

            if (!this.getWorkloadConfiguration().getXmlConfig().getBoolean("disable_explain", false)) {
                for (ExecuteRule er : executeRules) {
                    for (Query query : er.getQueries()) {
                        String querystmt = query.getQuery();
                        try {
                            PreparedStatement stmt = conn.prepareStatement((query.isSelectQuery() ? explainSelect : query.isUpdateQuery() ? explainUpdate : explainOthers) + querystmt);
                            List<UtilToMethod> baseUtils = query.getBaseUtils();
                             Object[] generatedValues = generateParameterValues(baseUtils);
                            for (int j = 0; j < generatedValues.length; j++) {
                                stmt.setObject(j + 1, generatedValues[j]);
                            }
                            explainDDLMap.put(query.getQuery(), stmt);
                        } catch (SQLException | ClassNotFoundException | InvocationTargetException | NoSuchMethodException |
                                 InstantiationException | IllegalAccessException e) {
                            throw new RuntimeException("Failed to prepare EXPLAIN for query [" + querystmt
                                + "] in workload '" + this.workloadName + "'.", e);
                        }

                    }
                }
            }
            try {
                if (!explainDDLMap.isEmpty())
                    runExplainAnalyse(explainDDLMap);
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                // Explain statements are one-shot; close them here instead of leaking
                // them on the connection for the whole run. runExplainAnalyse keeps the
                // map pointing at the live statement even when it swaps one out for the
                // dist/debug retry, so this closes the right handles.
                for (PreparedStatement explainStmt : explainDDLMap.values()) {
                    try {
                        explainStmt.close();
                    } catch (SQLException ignore) {
                        // best-effort close
                    }
                }
            }
            } finally {
                // Mark done even if a step above threw, so sibling threads blocked on
                // the monitor do not re-run this once-only initialization work.
                isInitializeDone.set(true);
            }
        }

    }


    public void runExplainAnalyse(Map<String, PreparedStatement> explainDDLMap) throws SQLException {
        LOG.info("Running explain for select/update queries before execute phase for workload : " + this.workloadName);
        for (Map.Entry<String, PreparedStatement> entry : explainDDLMap.entrySet()) {
            String query = entry.getKey();
            PreparedStatement ddl = entry.getValue();
            JSONObject jsonObject = new JSONObject();
            int countResultSetGen = 0;
            boolean distOptionPresent = true;
            while (countResultSetGen < 3) {
                try (ResultSet ignored = ddl.executeQuery()) {
                    if(!conn.getAutoCommit())
                        conn.commit();
                } catch (PSQLException e) {
                    if (distOptionPresent && e.getMessage().contains("unrecognized EXPLAIN option \"dist\"")) {
                        String modifiedQuery = ddl.toString().replace("dist,", "");
                        ddl = swapStatement(entry, ddl, modifiedQuery);
                        distOptionPresent = false;
                        continue;
                    } else if (distOptionPresent && e.getMessage().contains("unrecognized EXPLAIN option \"debug\"")) {
                        String modifiedQuery = ddl.toString().replace("debug", "");
                        ddl = swapStatement(entry, ddl, modifiedQuery);
                        distOptionPresent = false;
                        continue;
                    } else {
                        throw e;
                    }
                }
                countResultSetGen++;
            }
            jsonObject.put("SQL", trimToMaxLength(ddl.toString()));
            double explainStart = System.currentTimeMillis();
            StringBuilder data = new StringBuilder();
            try (ResultSet rs = ddl.executeQuery()) {
                if(!conn.getAutoCommit())
                    conn.commit();
                while (rs.next()) {
                    data.append(rs.getString(1));
                    data.append("\n");
                }
            }
            double explainEnd = System.currentTimeMillis();
            jsonObject.put("ResultSet", data.toString());

            Pattern pattern = Pattern.compile("Planning Time: (.+?) ms Execution Time: (.+?) ms " +
                "Peak Memory Usage: (.+?)");
            Matcher matcher = pattern.matcher(data.toString());
            while(matcher.find()) {
                jsonObject.put("Planning Time(ms)", matcher.group(1));
                jsonObject.put("Execution Time(ms)", matcher.group(2));
                jsonObject.put("Peak Memory Usage(kB)", matcher.group(3));
            }
            Pattern rowsPattern = Pattern.compile("\\(actual[^)]*rows=(\\d+)\\s+loops=");
            Matcher rowsMatcher = rowsPattern.matcher(data.toString());
            if (rowsMatcher.find()) {
                jsonObject.put("ExplainPlanRows", rowsMatcher.group(1));
            }

            jsonObject.put("ClientSideExplainTime(ms)", explainEnd - explainStart);
            queryToExplainMap.put(query, jsonObject);
        }
    }

    /**
     * Prepare a replacement explain statement (used for the dist/debug retry), close the
     * old one, and update the map entry so the live statement is the one that gets closed
     * by initialize()'s cleanup. Returns the replacement.
     */
    private PreparedStatement swapStatement(Map.Entry<String, PreparedStatement> entry,
                                            PreparedStatement old, String newQuery) throws SQLException {
        PreparedStatement replacement = conn.prepareStatement(newQuery);
        entry.setValue(replacement);
        try {
            old.close();
        } catch (SQLException ignore) {
            // best-effort close of the statement we are replacing
        }
        return replacement;
    }


    /**
     * Generates parameter values for a query, evaluating expressions with context from named references.
     * Uses a two-pass approach:
     * 1. Generate all non-expression values and build context map
     * 2. Evaluate expressions using the context
     * 
     * @param baseUtils List of utility methods/expressions to generate values
     * @return Array of generated values ready for PreparedStatement binding
     */
    private Object[] generateParameterValues(List<UtilToMethod> baseUtils) 
            throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, 
                   InstantiationException, IllegalAccessException {
        
        int size = baseUtils.size();
        Object[] generatedValues = new Object[size];
        // Allocated lazily: most queries have neither reference names nor expressions,
        // so we avoid a throwaway HashMap on every execution in the worker hot loop.
        Map<String, Object> context = null;
        boolean hasExpression = false;

        // First pass: generate all non-expression values and build context
        for (int j = 0; j < size; j++) {
            UtilToMethod util = baseUtils.get(j);
            if (util.isExpression()) {
                // Skip expressions in first pass
                hasExpression = true;
                continue;
            }
            Object value = util.get();
            generatedValues[j] = value;

            // Add to context if it has a reference name
            if (util.getReferenceName() != null) {
                if (context == null) {
                    context = new HashMap<>();
                }
                context.put(util.getReferenceName(), value);
            }
        }

        // Second pass: evaluate expressions with context (skipped entirely when none)
        if (hasExpression) {
            Map<String, Object> ctx = (context != null) ? context : Collections.emptyMap();
            for (int j = 0; j < size; j++) {
                UtilToMethod utilMethod = baseUtils.get(j);
                if (utilMethod.isExpression()) {
                    // This is an expression - evaluate it
                    BaseUtil util = utilMethod.getInstance();
                    if (util instanceof ExpressionEval) {
                        ((ExpressionEval) util).setContext(ctx);
                        generatedValues[j] = utilMethod.get();
                    }
                }
            }
        }

        return generatedValues;
    }

    @Override
    protected TransactionStatus executeWork(Connection conn, TransactionType txnType) throws
        UserAbortException, SQLException {
        try {
            if (config.containsKey("execute") && config.getBoolean("execute")) {
                ybm.execute(conn);
                return TransactionStatus.SUCCESS;
            } else if (executeRules == null || executeRules.size() == 0) {
                if (this.configuration.getWorkloadState().getGlobalState() == State.MEASURE) {
                    ybm.executeOnce(conn, this.getBenchmark());
                }
                return TransactionStatus.SUCCESS;
            }

            int executeRuleIndex = txnType.getId() - 1;
            ExecuteRule executeRule = executeRules.get(executeRuleIndex);
            boolean zeroRowsTransaction = false;
            for (Query query : executeRule.getQueries()) {
                String queryStmt = query.getQuery();
                PreparedStatement stmt = this.preparedStatementsPerQuery.get(queryStmt);
                List<UtilToMethod> baseUtils = query.getBaseUtils();
                int count = query.getCount();
                
                for (int i = 0; i < count; i++) {
                    // Generate parameter values with expression evaluation
                    Object[] generatedValues = generateParameterValues(baseUtils);
                    
                    // Set all parameters in the prepared statement
                    for (int j = 0; j < generatedValues.length; j++) {
                        stmt.setObject(j + 1, generatedValues[j]);
                    }
                    
                    if (query.isSelectQuery() || query.isReturningQuery()) {
                        // try-with-resources so the (possibly large) result set is
                        // released as soon as we finish counting rows, rather than
                        // lingering on the cached PreparedStatement until its next reuse.
                        try (ResultSet rs = stmt.executeQuery()) {
                            long countSet = 0;
                            while (rs.next()) countSet++;
                            if (countSet == 0) zeroRowsTransaction = true;
                        }
                    } else {
                        int updatedRows = stmt.executeUpdate();
                        if (updatedRows == 0) zeroRowsTransaction = true;
                    }
                }
            }
            if (zeroRowsTransaction)
                return TransactionStatus.ZERO_ROWS;

        } catch (ClassNotFoundException | InvocationTargetException
                 | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return TransactionStatus.SUCCESS;
    }


    @Override
    public void tearDown() {
        synchronized (FeatureBenchWorker.class) {
            // Collect exactly once, on the first tearDown() invocation to get here (whose
            // connection is still open). See the isPGStatStatementCollected/isCleanUpDone
            // field comment for why a "last worker" gate is unsafe given tearDown() runs
            // twice per worker and each worker closes its own connection.
            boolean shouldCollect = this.getWorkloadConfiguration().getXmlConfig().getBoolean("collect_pg_stat_statements", false);
            if ( !isPGStatStatementCollected.get() && !this.getWorkloadConfiguration().getIsOptimalThreadsWorkload() && !this.configuration.getNewConnectionPerTxn() && shouldCollect && this.conn != null) {

                Map<String, Integer> queryStringsAndRC = new LinkedHashMap<>();
                for (ExecuteRule er : executeRules) {
                    for (int i = 0; i < er.getQueries().size(); i++) {
                        Query q = er.getQueries().get(i);
                        queryStringsAndRC.put(q.getQuery(), q.getExplainPlanRCValidateCount());
                    }
                }

                List<JSONObject> jsonResultsList = new ArrayList<>();
                JSONObject pgStatOutputs = null;
                JSONObject pgPreparedStatementOutputs = null;
                JSONArray pgStatUserIndexesOutputs = null;
                try {
                    LOG.info("Collecting pg_stat_statements for workload : " + this.workloadName);
                    pgStatOutputs = callPGStats();
                    pgPreparedStatementOutputs = collectPgPreparedStatements();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to collect pg_stat_statements / pg_prepared_statements for workload '"
                        + this.workloadName + "'.", e);
                }

                // For Postgres, we need to collect pg_stat_user_indexes
                if (this.getWorkloadConfiguration().getDatabaseType().equals(DatabaseType.POSTGRES)) {
                    LOG.info("Collecting pg_stat_user_indexes for workload : " + this.workloadName);
                    try {
                        pgStatUserIndexesOutputs = callPGStatUserIndexes();
                    } catch (SQLException e) {
                        throw new RuntimeException("Failed to collect pg_stat_user_indexes for workload '"
                            + this.workloadName + "'.", e);
                    }
                }

                for (Map.Entry<String, Integer> entry : queryStringsAndRC.entrySet()) {
                    JSONObject inner = new JSONObject();
                    inner.put("query", entry.getKey());
                    inner.put("pg_stat_statements", pgStatOutputs == null ? new JSONObject() : findQueryInPgStatUsingCosine(pgStatOutputs, entry.getKey()));

                    if (entry.getValue() != -1)
                        inner.put("explainPlanRcValidationSuccess", Integer.parseInt((String) queryToExplainMap.getOrDefault(entry.getKey(), new JSONObject()).get("ExplainPlanRows")) == entry.getValue());
                    inner.put("explain", queryToExplainMap.getOrDefault(entry.getKey(), new JSONObject()));
                    inner.put("prepared_statements", pgPreparedStatementOutputs == null ? new JSONObject() : pgPreparedStatementOutputs);
                    jsonResultsList.add(inner);
                }
                if (pgStatUserIndexesOutputs != null) {
                    jsonResultsList.add(new JSONObject().put("pg_stat_user_indexes", pgStatUserIndexesOutputs));
                }
                this.featurebenchAdditionalResults.setJsonResultsList(jsonResultsList);
                isPGStatStatementCollected.set(true);
            }

            if (((config.containsKey("execute") && config.getBoolean("execute")) || (executeRules == null || executeRules.isEmpty())) && !isCleanUpDone.get()) {
                try {
                    ybm.cleanUp(conn);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                LOG.info("\n=================Cleanup Phase taking from User=========\n");
                isCleanUpDone.set(true);
            }

            if (!this.configuration.getNewConnectionPerTxn() && this.conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOG.error("Connection couldn't be closed.", e);
                }
            }
        }
    }

    private JSONObject callPGStats() throws SQLException{
        // Filter EXPLAIN rows server-side (findQueryInPgStatUsingCosine discards them
        // anyway) so the in-memory snapshot stays smaller on busy clusters.
        String pgStatQuery = "select * from pg_stat_statements where lower(btrim(query)) not like 'explain%';";
        JSONObject pgStatOutputs = new JSONObject();
        try (Statement stmt = this.conn.createStatement();
             ResultSet resultSet = stmt.executeQuery(pgStatQuery)) {
            if(!conn.getAutoCommit())
                conn.commit();
            ResultSetMetaData rsmd = resultSet.getMetaData();
            int resultSetCount = 0;
            while (resultSet.next()) {
                JSONObject pgStatOutputPerRecord = new JSONObject();
                for ( int i = 1; i <= rsmd.getColumnCount(); i++) {
                    pgStatOutputPerRecord.put(rsmd.getColumnName(i), resultSet.getString(i));
                }
                pgStatOutputs.put("Record_" + resultSetCount, pgStatOutputPerRecord);
                resultSetCount++;
            }
        }
        return pgStatOutputs;
    }

    /**
     * Trims a collected SQL/explain string to {@link #MAX_COLLECTED_STRING_LENGTH} characters so the
     * detailed results JSON stays small. The bound values (e.g. the VALUES (...) list of a multi-row
     * INSERT) are not of interest — only the standard SQL structure is — so anything beyond the limit
     * is dropped and replaced with a truncation marker noting how many characters were removed.
     */
    private static String trimToMaxLength(String value) {
        if (value == null || value.length() <= MAX_COLLECTED_STRING_LENGTH) {
            return value;
        }
        int trimmed = value.length() - MAX_COLLECTED_STRING_LENGTH;
        return value.substring(0, MAX_COLLECTED_STRING_LENGTH) + "...[truncated " + trimmed + " chars]";
    }

    private JSONObject collectPgPreparedStatements() throws SQLException{
        String pgPreparedStatements = "select * from pg_prepared_statements;";
        JSONObject pgPreparedStatementOutputs = new JSONObject();
        try (Statement stmt = this.conn.createStatement();
             ResultSet resultSet = stmt.executeQuery(pgPreparedStatements)) {
            if(!conn.getAutoCommit())
                conn.commit();
            ResultSetMetaData rsmd = resultSet.getMetaData();
            int resultSetCount = 0;
            while (resultSet.next()) {
                JSONObject pgPreparedStatementOutputPerRecord = new JSONObject();
                for ( int i = 1; i <= rsmd.getColumnCount(); i++) {
                    String columnName = rsmd.getColumnName(i);
                    // parameter_types can be very large for multi-row prepared statements; drop it entirely.
                    if (columnName.equals("parameter_types")) {
                        continue;
                    }
                    String value = resultSet.getString(i);
                    // statement can be very large for multi-row INSERTs; trim it to the standard structure.
                    if (columnName.equals("statement")) {
                        value = trimToMaxLength(value);
                    }
                    pgPreparedStatementOutputPerRecord.put(columnName, value);
                }
                pgPreparedStatementOutputs.put("Record_" + resultSetCount, pgPreparedStatementOutputPerRecord);
                resultSetCount++;
            }
        }
        return pgPreparedStatementOutputs;
    }

    private List<String> tokenizeQuery(String query) {
        // Remove placeholders
        String cleaned = query.replaceAll("\\?", "");

        // Remove huge IN (...) clauses completely
        cleaned = cleaned.replaceAll("(?i)in\\s*\\(([^)]*)\\)", "in()");

        // Normalize whitespace and lowercase
        cleaned = cleaned.replaceAll("\\s+", " ").trim().toLowerCase();

        // Tokenize by word characters
        return Arrays.asList(cleaned.split("\\W+"));
    }

    private double cosineSimilarity(List<String> tokens1, List<String> tokens2) {
        Map<String, Integer> freq1 = getFrequencyMap(tokens1);
        Map<String, Integer> freq2 = getFrequencyMap(tokens2);

        Set<String> allTokens = new HashSet<>();
        allTokens.addAll(freq1.keySet());
        allTokens.addAll(freq2.keySet());

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (String token : allTokens) {
            int v1 = freq1.getOrDefault(token, 0);
            int v2 = freq2.getOrDefault(token, 0);

            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }

        return (norm1 == 0 || norm2 == 0) ? 0.0 : dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private Map<String, Integer> getFrequencyMap(List<String> tokens) {
        Map<String, Integer> freqMap = new HashMap<>();
        for (String token : tokens) {
            freqMap.put(token, freqMap.getOrDefault(token, 0) + 1);
        }
        return freqMap;
    }

    private JSONObject findQueryInPgStatUsingCosine(JSONObject pgStatOutputs, String inputQuery) {
        List<String> inputTokens = tokenizeQuery(inputQuery);

        double maxSimilarity = -1.0;
        int minLengthDiff = Integer.MAX_VALUE;
        String matchedKey = null;

        for (String key : pgStatOutputs.keySet()) {
            JSONObject value = (JSONObject) pgStatOutputs.get(key);
            String pgQuery = value.getString("query").trim();

            // ❌ Skip explain queries
            if (pgQuery.toLowerCase().startsWith("explain")) {
                continue;
            }

            List<String> pgTokens = tokenizeQuery(pgQuery);
            double similarity = cosineSimilarity(inputTokens, pgTokens);
            int lengthDiff = Math.abs(inputTokens.size() - pgTokens.size());

            if (similarity > maxSimilarity || (similarity == maxSimilarity && lengthDiff < minLengthDiff)) {
                maxSimilarity = similarity;
                minLengthDiff = lengthDiff;
                matchedKey = key;
            }
        }

        return matchedKey != null ? (JSONObject) pgStatOutputs.get(matchedKey) : null;
    }

    private JSONArray callPGStatUserIndexes() throws SQLException{
        String pgStatUserIndexes = "SELECT relname AS table_name, seq_scan, idx_scan, seq_tup_read, idx_tup_fetch FROM pg_stat_user_tables;";
        JSONArray pgStatUserIndexesOutputs = new JSONArray();
        try (Statement stmt = this.conn.createStatement();
             ResultSet resultSet = stmt.executeQuery(pgStatUserIndexes)) {
            if(!conn.getAutoCommit())
                conn.commit();
            ResultSetMetaData rsmd = resultSet.getMetaData();
            while (resultSet.next()) {
                JSONObject pgStatUserIndexesOutputPerRecord = new JSONObject();
                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                    pgStatUserIndexesOutputPerRecord.put(
                        rsmd.getColumnName(i),
                        resultSet.getString(i)
                    );
                }
                pgStatUserIndexesOutputs.put(pgStatUserIndexesOutputPerRecord);
            }
        }
        return pgStatUserIndexesOutputs;
    }
}
