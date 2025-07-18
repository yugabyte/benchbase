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
import com.oltpbenchmark.benchmarks.featurebench.workerhelpers.ExecuteRule;
import com.oltpbenchmark.benchmarks.featurebench.workerhelpers.Query;
import com.oltpbenchmark.types.State;
import com.oltpbenchmark.types.TransactionStatus;
import com.oltpbenchmark.util.FileUtil;
import com.yugabyte.util.PSQLException;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.json.JSONObject;
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
    static AtomicBoolean isCleanUpDone = new AtomicBoolean(false);
    private final String workloadClass;
    private final HierarchicalConfiguration<ImmutableNode> config;
    private YBMicroBenchmark ybm;
    private final List<ExecuteRule> executeRules;
    private final String workloadName;
    private HashMap<String, PreparedStatement> preparedStatementsPerQuery;
    public static Map<String,JSONObject> queryToExplainMap = new HashMap<>();

    static AtomicBoolean isPGStatStatementCollected = new AtomicBoolean(false);

    static AtomicBoolean isPGStatResetCalled = new AtomicBoolean(false);

    static AtomicBoolean isInitializeDone = new AtomicBoolean(false);

    public FeatureBenchWorker(FeatureBenchBenchmark benchmarkModule,
                              int id,
                              String workloadClass,
                              HierarchicalConfiguration<ImmutableNode> workerConfig,
                              List<ExecuteRule> executeRules,
                              String workloadName) {
        super(benchmarkModule, id);
        this.workloadClass = workloadClass;
        this.executeRules = executeRules;
        this.config = workerConfig;
        this.workloadName = workloadName;
        isInitializeDone.set(false);
        isPGStatResetCalled.set(false);
        isPGStatStatementCollected.set(false);
        isCleanUpDone.set(false);
        try {
            ybm = (YBMicroBenchmark) Class.forName(workloadClass)
                .getDeclaredConstructor(HierarchicalConfiguration.class)
                .newInstance(config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

        if (isInitializeDone.get()) return;
        synchronized (FeatureBenchWorker.class) {
            if (isInitializeDone.get()) return;
            if (!isPGStatResetCalled.get() &&
                this.getWorkloadConfiguration().getXmlConfig().getBoolean("collect_pg_stat_statements", false)
            ) {
                LOG.info("Resetting pg_stat_statements for workload : " + this.workloadName);
                try {
                    Statement stmt = conn.createStatement();
                    stmt.executeQuery("SELECT pg_stat_statements_reset();");
                    if (!conn.getAutoCommit())
                        conn.commit();
                    isPGStatResetCalled.set(true);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
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
                    throw new RuntimeException(e);
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
                            for (int j = 0; j < baseUtils.size(); j++) {
                                try {
                                    stmt.setObject(j + 1, baseUtils.get(j).get());
                                } catch (SQLException | InvocationTargetException | IllegalAccessException |
                                         ClassNotFoundException | NoSuchMethodException |
                                         InstantiationException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            explainDDLMap.put(query.getQuery(), stmt);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }

                    }
                }
            }
            try {
                if (!explainDDLMap.isEmpty())
                    runExplainAnalyse(explainDDLMap);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            isInitializeDone.set(true);
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
                try {
                    ddl.executeQuery();
                    if(!conn.getAutoCommit())
                        conn.commit();
                } catch (PSQLException e) {
                    if (distOptionPresent && e.getMessage().contains("unrecognized EXPLAIN option \"dist\"")) {
                        String modifiedQuery = ddl.toString().replace("dist,", "");
                        ddl = conn.prepareStatement(modifiedQuery);
                        distOptionPresent = false;
                        continue;
                    } else if (distOptionPresent && e.getMessage().contains("unrecognized EXPLAIN option \"debug\"")) {
                        String modifiedQuery = ddl.toString().replace("debug", "");
                        ddl = conn.prepareStatement(modifiedQuery);
                        distOptionPresent = false;
                        continue;
                    } else {
                        throw e;
                    }
                }
                countResultSetGen++;
            }
            jsonObject.put("SQL", ddl);
            double explainStart = System.currentTimeMillis();
            ResultSet rs = ddl.executeQuery();
            if(!conn.getAutoCommit())
                conn.commit();
            StringBuilder data = new StringBuilder();
            while (rs.next()) {
                data.append(rs.getString(1));
                data.append("\n");
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
                    for (int j = 0; j < baseUtils.size(); j++)
                        stmt.setObject(j + 1, baseUtils.get(j).get());
                    if (query.isSelectQuery() || stmt.toString().toUpperCase().contains(" RETURNING ")) {
                        ResultSet rs = stmt.executeQuery();
                        int countSet = 0;
                        while (rs.next()) countSet++;
                        if (countSet == 0) zeroRowsTransaction = true;
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
            boolean shouldCollect = this.getWorkloadConfiguration().getXmlConfig().getBoolean("collect_pg_stat_statements", false);
            if (!this.configuration.getNewConnectionPerTxn() && (shouldCollect && !isPGStatStatementCollected.get()) && this.conn != null) {

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
                try {
                    LOG.info("Collecting pg_stat_statements for workload : " + this.workloadName);
                    pgStatOutputs = callPGStats();
                    /*TODO: remove collecting prepared_statements*/
                    pgPreparedStatementOutputs = collectPgPreparedStatements();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                for (Map.Entry<String, Integer> entry : queryStringsAndRC.entrySet()) {
                    JSONObject inner = new JSONObject();
                    inner.put("query", entry.getKey());
                    inner.put("pg_stat_statements", pgStatOutputs == null ? new JSONObject() : findQueryInPgStatUsingCosine(pgStatOutputs, entry.getKey()));

                    if (entry.getValue() != -1)
                        inner.put("explainPlanRcValidationSuccess", Integer.parseInt((String) queryToExplainMap.getOrDefault(entry.getKey(), new JSONObject()).get("ExplainPlanRows")) == entry.getValue());
                    inner.put("explain", queryToExplainMap.getOrDefault(entry.getKey(), new JSONObject()));
                    /*TODO: remove prepared_statements*/
                    inner.put("prepared_statements", pgPreparedStatementOutputs == null ? new JSONObject() : pgPreparedStatementOutputs);
                    jsonResultsList.add(inner);
                }
                this.featurebenchAdditionalResults.setJsonResultsList(jsonResultsList);
                isPGStatStatementCollected.set(true);
            }

            if (((config.containsKey("execute") && config.getBoolean("execute")) || (executeRules == null || executeRules.isEmpty()) )&& !isCleanUpDone.get()) {
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
        String pgStatQuery = "select * from pg_stat_statements;";
        Statement stmt = this.conn.createStatement();
        ResultSet resultSet = stmt.executeQuery(pgStatQuery);
        if(!conn.getAutoCommit())
            conn.commit();
        ResultSetMetaData rsmd = resultSet.getMetaData();
        int resultSetCount = 0;
        JSONObject pgStatOutputs = new JSONObject();
        while (resultSet.next()) {
            JSONObject pgStatOutputPerRecord = new JSONObject();
            for ( int i = 1; i <= rsmd.getColumnCount(); i++) {
                pgStatOutputPerRecord.put(rsmd.getColumnName(i), resultSet.getString(i));
            }
            pgStatOutputs.put("Record_" + resultSetCount, pgStatOutputPerRecord);
            resultSetCount++;
        }
        return pgStatOutputs;
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

    /*TODO: remove collectPgPreparedStatements*/
    private JSONObject collectPgPreparedStatements() throws SQLException{
        String pgPreparedStatements = "select * from pg_prepared_statements;";
        Statement stmt = this.conn.createStatement();
        ResultSet resultSet = stmt.executeQuery(pgPreparedStatements);
        if(!conn.getAutoCommit())
            conn.commit();
        ResultSetMetaData rsmd = resultSet.getMetaData();
        int resultSetCount = 0;
        JSONObject pgPreparedStatementOutputs = new JSONObject();
        while (resultSet.next()) {
            JSONObject pgPreparedStatementOutputPerRecord = new JSONObject();
            for ( int i = 1; i <= rsmd.getColumnCount(); i++) {
                pgPreparedStatementOutputPerRecord.put(rsmd.getColumnName(i), resultSet.getString(i));
            }
            pgPreparedStatementOutputs.put("Record_" + resultSetCount, pgPreparedStatementOutputPerRecord);
            resultSetCount++;
        }
        return pgPreparedStatementOutputs;
    }
}
