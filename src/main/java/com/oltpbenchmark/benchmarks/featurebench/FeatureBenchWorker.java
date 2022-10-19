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
import com.oltpbenchmark.types.State;
import com.oltpbenchmark.types.TransactionStatus;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 *
 */
public class FeatureBenchWorker extends Worker<FeatureBenchBenchmark> {
    private static final Logger LOG = LoggerFactory.getLogger(FeatureBenchWorker.class);
    static AtomicBoolean isCleanUpDone = new AtomicBoolean(false);
    public String workloadClass = null;
    public HierarchicalConfiguration<ImmutableNode> config = null;
    public YBMicroBenchmark ybm = null;
    public List<List<Map<String, Object>>> rules = new ArrayList<>();

    public FeatureBenchWorker(FeatureBenchBenchmark benchmarkModule, int id, HierarchicalConfiguration<ImmutableNode> testconfif) {
        super(benchmarkModule, id);
        List<HierarchicalConfiguration<ImmutableNode>> executeRules =
            testconfif.configurationsAt("executeRules");
        for (HierarchicalConfiguration<ImmutableNode> executeRule : executeRules) {
            List<HierarchicalConfiguration<ImmutableNode>> queriesTransaction = executeRule.configurationsAt("queries");
            List<Map<String, Object>> que = new ArrayList<>();
            for (HierarchicalConfiguration<ImmutableNode> query : queriesTransaction) {
                Map<String, Object> singlequery = new HashMap<>();
                try {
                    PreparedStatement querystmt = conn.prepareStatement(query.getString("query"));
                    singlequery.put("query", querystmt);
                    List<HierarchicalConfiguration<ImmutableNode>> bl = query.configurationsAt("bindings");
                    List<UtilToMethod> baseutils = new ArrayList<>();
                    for (HierarchicalConfiguration<ImmutableNode> bindingsList : bl) {
                        UtilToMethod obj = new UtilToMethod(bindingsList.getString("util"), bindingsList.getList("params"));
                        baseutils.add(obj);
                    }
                    singlequery.put("utils", baseutils);

                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                que.add(singlequery);
            }
            rules.add(que);
        }
    }


    @Override
    protected TransactionStatus executeWork(Connection conn, TransactionType txnType) throws
        UserAbortException, SQLException {


        try {
            ybm = (YBMicroBenchmark) Class.forName(workloadClass)
                .getDeclaredConstructor(HierarchicalConfiguration.class)
                .newInstance(config);

            if (config.containsKey("execute") && config.getBoolean("execute")) {
                ybm.execute(conn);
                //LOG.info("in execute work");
                return TransactionStatus.SUCCESS;
            } else if (config.configurationsAt("executeRules") == null || config.configurationsAt("executeRules").size() == 0) {
                if (this.configuration.getWorkloadState().getGlobalState() == State.MEASURE) {
                    ybm.executeOnce(conn);
                }
                return TransactionStatus.SUCCESS;
            }

            int executeRuleIndex = txnType.getId() - 1;
            List<Map<String, Object>> queries = rules.get(executeRuleIndex);
            for (Map<String, Object> query : queries) {
                PreparedStatement stmt = (PreparedStatement) query.get("query");
                List<UtilToMethod> baseutils = (List<UtilToMethod>) query.get("utils");
                for (int j = 0; j < baseutils.size(); j++) {
                    stmt.setObject(j + 1, baseutils.get(j).get());
                }
                stmt.executeQuery();
            }


        } catch (ClassNotFoundException | InvocationTargetException
                 | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return TransactionStatus.SUCCESS;
    }

    private void executeQueryFromYaml(String stmt, List<String> utilNames, List<List<Object>> params, int id)
        throws ClassNotFoundException, InvocationTargetException,
        NoSuchMethodException, InstantiationException,
        IllegalAccessException, SQLException {
        LOG.info("Using YAML for execute phase " + "transaction no: " + id);
        //System.out.println(stmt);
        PreparedStatement query = conn.prepareStatement(stmt);
        for (int i = 0; i < utilNames.size(); i++) {
            UtilToMethod obj = new UtilToMethod(utilNames.get(i), params.get(i));
            query.setObject(i + 1, obj.get());
        }
        ResultSet res = query.executeQuery();
        ResultSetMetaData rsmd = res.getMetaData();
        int columnsNumber = rsmd.getColumnCount();
        while (res.next()) {
            for (int i = 1; i <= columnsNumber; i++) {
                if (i > 1) System.out.print("  ");
                String columnValue = res.getString(i);
                System.out.print(rsmd.getColumnName(i) + "->" + columnValue);
            }
            System.out.println("");
        }
        System.out.println("\n");
    }

    @Override
    public void tearDown() {

        if (!this.configuration.getNewConnectionPerTxn() && this.conn != null && ybm != null) {
            try {
                if (this.configuration.getWorkloadState().getGlobalState() == State.EXIT && !isCleanUpDone.get()) {
                    if (config.containsKey("cleanup")) {
                        LOG.info("\n=================Cleanup Phase taking from Yaml=========\n");
                        List<String> ddls = config.getList(String.class, "cleanup");
                        try {
                            Statement stmtOBj = conn.createStatement();
                            for (String ddl : ddls) {
                                stmtOBj.execute(ddl);
                            }
                            stmtOBj.close();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                    } else {
                        ybm.cleanUp(conn);
                    }
                    conn.close();
                    isCleanUpDone.set(true);
                }
            } catch (SQLException e) {
                LOG.error("Connection couldn't be closed.", e);
            }
        }
    }
}
