package com.oltpbenchmark.benchmarks.featurebench;

import com.oltpbenchmark.api.Loader;
import com.oltpbenchmark.api.LoaderThread;
import com.oltpbenchmark.benchmarks.featurebench.util.*;
import com.oltpbenchmark.util.RandomGenerator;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;



/**
 * This doesn't load any data!
 */
public class FeatureBenchLoader extends Loader<FeatureBenchBenchmark> {
    public String workloadClass = null;
    public HierarchicalConfiguration<ImmutableNode> config = null;
    public YBMicroBenchmark ybm = null;
    public int sizeOfLoadRule = 0;
    PreparedStatement stmt;

    public FeatureBenchLoader(FeatureBenchBenchmark benchmark) {
        super(benchmark);
    }

    @Override
    public List<LoaderThread> createLoaderThreads() {
        try {
            ybm = (YBMicroBenchmark) Class.forName(workloadClass)
                .getDeclaredConstructor(HierarchicalConfiguration.class)
                .newInstance(config);


            createPhaseAndBeforeLoad();

            ArrayList<LoaderThread> loaderThreads = new ArrayList<>();

            if (ybm.loadOnceImplemented) {
                loaderThreads.add(new GeneratorOnce(ybm));
            } else {
                ArrayList<LoadRule> loadRules = ybm.loadRules();
                sizeOfLoadRule = loadRules.size();
                // TODO: list of loaderthreads will call beforeLoad and afterLoad everytime
                for (LoadRule loadRule : loadRules) {
                    loaderThreads.add(new Generator(loadRule));
                }
            }
            return loaderThreads;
        } catch (InstantiationException | IllegalAccessException |
                 InvocationTargetException | NoSuchMethodException |
                 ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void createPhaseAndBeforeLoad() {
        try {
            Connection conn = benchmark.makeConnection();
            if (ybm.createDBImplemented) {
                ybm.create(conn);
            }
            if (ybm.beforeLoadImplemented) {
                ybm.beforeLoad(conn);
            }
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void afterLoadPhase() {
        try {
            if (ybm.afterLoadImplemented) {
                // TODO: see if we can utilise connection object instead of creating new one
                Connection conn = benchmark.makeConnection();
                ybm.afterLoad(conn);
                conn.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private class GeneratorOnce extends LoaderThread {
        final YBMicroBenchmark ybm;

        public GeneratorOnce(YBMicroBenchmark ybm) {
            super(benchmark);
            this.ybm = ybm;
        }

        //        @Override
//        public void beforeLoad() {
//            try {
//                // TODO: see if we can utilise connection object instead of creating new one
//                Connection conn = benchmark.makeConnection();
//                if(ybm.createDBImplemented) {
//                    ybm.create(conn);
//                    ybm.createDBImplemented = false;
//                }
//                if(ybm.beforeLoadImplemented) {
//                    ybm.beforeLoad(conn);
//                }
//                conn.close();
//            } catch (SQLException e) {
//                throw new RuntimeException(e);
//            }
//        }
        @Override
        public void load(Connection conn) throws SQLException {
            ybm.loadOnce(conn);
        }

        @Override
        public void afterLoad() {
            afterLoadPhase();
        }
    }


    private class Generator extends LoaderThread {
        static int numberOfGeneratorFinished = 0;
        final LoadRule loadRule;

        public Generator(LoadRule loadRule) {
            super(benchmark);
            this.loadRule = loadRule;
        }


//        @Override
//        public void beforeLoad() {
//            try {
//                // TODO: see if we can utilise connection object instead of creating new one
//                Connection conn = benchmark.makeConnection();
//                if(ybm.createDBImplemented) {
//                    ybm.create(conn);
//                    ybm.createDBImplemented = false;
//                }
//                if(ybm.beforeLoadImplemented) {
//                    ybm.beforeLoad(conn);
//                }
//                conn.close();
//            } catch (SQLException e) {
//                throw new RuntimeException(e);
//            }
//        }


        @Override
        public void load(Connection conn) throws SQLException {
            try {
                int batchSize = 0;
                TableInfo t = loadRule.getTableInfo();
                long no_of_rows = t.getNo_of_rows();
                String table_name = t.get_table_name();
                ArrayList<ColumnsDetails> cd = t.getColumn_Det();
                int no_of_columns = cd.size();
                StringBuilder columnString = new StringBuilder();
                StringBuilder valueString = new StringBuilder();

                for (ColumnsDetails columnsDetails : cd) {
                    columnString.append(columnsDetails.getName()).append(",");
                    valueString.append("?,");
                }
                columnString.setLength(columnString.length() - 1);
                valueString.setLength(valueString.length() - 1);
                String insertStmt = "INSERT INTO " + table_name + " (" + columnString + ") VALUES " + "(" + valueString + ")";
                System.out.println(insertStmt);
                stmt = conn.prepareStatement(insertStmt);
                for (int i = 0; i < no_of_rows; i++) {
                    for (ColumnsDetails columnsDetails : cd) {
                        UtilityFunc uf = columnsDetails.getUtilFunc();
                        bindParamBasedOnType(uf);
                    }
                }

                for (int i = 0; i < no_of_rows; i++) {
                    for (int j = 0; j < no_of_columns; j++) {
                        UtilityFunc uf = cd.get(j).getUtilFunc();
                        String funcname = findFuncname(uf);
                        if (Objects.equals(funcname, "get_int_primary_key")) {
                            stmt.setInt(j + 1, UtilGenerators.get_int_primary_key());
                        } else if (Objects.equals(funcname, "numberToIdString")) {
                            stmt.setString(j + 1, UtilGenerators.numberToIdString());
                        }
                    }
                    stmt.addBatch();
                    if (++batchSize >= workConf.getBatchSize()) {
                        this.loadTables(conn);
                        batchSize = 0;
                    }
                }
                stmt.executeBatch();
                if (batchSize > 0) {
                    this.loadTables(conn);
                }
            } catch (SQLException e) {
                numberOfGeneratorFinished += 1;
                throw new RuntimeException(e);
            }
            numberOfGeneratorFinished += 1;
        }

        public void bindParamBasedOnType(UtilityFunc uf) throws SQLException {
            if (Objects.equals(uf.getName(), "get_int_primary_key")) {
                ArrayList<ParamsForUtilFunc> ob1 = uf.getParams();
                ParamsForUtilFunc puf = ob1.get(0);
                ArrayList<Integer> range = puf.getParameters();
                int upper_range = range.get(1);
                int lower_range = range.get(0);
                UtilGenerators.setUpper_range_for_primary_int_keys(upper_range);
                UtilGenerators.setLower_range_for_primary_int_keys(lower_range);
            } else if (Objects.equals(uf.getName(), "numberToIdString")) {
                ArrayList<ParamsForUtilFunc> ob1 = uf.getParams();
                ParamsForUtilFunc puf = ob1.get(0);
                ArrayList<Integer> max_len = puf.getParameters();
                int desired_len = max_len.get(0);
                UtilGenerators.setDesired_length_string_pkeys(desired_len);
            }

        }

        public String findFuncname(UtilityFunc uf) {
            if (Objects.equals(uf.getName(), "get_int_primary_key")) {
                return "get_int_primary_key";
            } else if (Objects.equals(uf.getName(), "numberToIdString")) {
                return "numberToIdString";
            } else return null;

        }

        private void loadTables(Connection conn) throws SQLException {
            stmt.executeBatch();
        }

        @Override
        public void afterLoad() {
            if (numberOfGeneratorFinished != sizeOfLoadRule) return;
            afterLoadPhase();
        }
    }
}




