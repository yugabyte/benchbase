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

import com.oltpbenchmark.api.Loader;
import com.oltpbenchmark.api.LoaderThread;
import com.oltpbenchmark.benchmarks.featurebench.customworkload.YBMicroBenchmarkImplementation;
import com.oltpbenchmark.benchmarks.featurebench.customworkload.YBMicroBenchmarkImplementation2;
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
import java.util.logging.Logger;

/**
 * This doesn't load any data!
 */
public class FeatureBenchLoader extends Loader<FeatureBenchBenchmark> {
    public final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(YBMicroBenchmarkImplementation2.class);

    public String workloadClass = null;
    public HierarchicalConfiguration<ImmutableNode> properties = null;
    public YBMicroBenchmark ybm = null;
    PreparedStatement stmt;

    public FeatureBenchLoader(FeatureBenchBenchmark benchmark) {
        super(benchmark);
    }

    @Override
    public List<LoaderThread> createLoaderThreads() {

        try {
            ybm = (YBMicroBenchmark) Class.forName(workloadClass).getDeclaredConstructor().newInstance();
            ArrayList<LoadRule> loadRules = ybm.loadRule(this.properties);
            ArrayList<LoaderThread> loaderThreads = new ArrayList<>();

            for (LoadRule loadRule : loadRules) {
                loaderThreads.add(new Generator(loadRule));
            }
            return loaderThreads;
        } catch (InstantiationException | IllegalAccessException |
                 InvocationTargetException | NoSuchMethodException |
                 ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private class Generator extends LoaderThread {
        final LoadRule loadRule;

        public Generator(LoadRule loadRule) {
            super(benchmark);
            this.loadRule = loadRule;
        }


        @Override
        public void load(Connection conn) throws SQLException {
            try {
                ybm.createDB(conn);
                int batchSize = 0;
                TableInfo t = loadRule.getTableInfo();
                long no_of_rows = t.getNo_of_rows();
                String table_name = t.get_table_name();
                ArrayList<ColumnsDetails> cd = t.getColumn_Det();
                int no_of_columns = cd.size();
                //System.out.println(no_of_columns);
                StringBuilder columnString = new StringBuilder();
                StringBuilder valueString = new StringBuilder();

                int c=0;
                for (ColumnsDetails columnsDetails : cd) {
                    //System.out.println(++c);
                    columnString.append(columnsDetails.getName()).append(",");
                    if(Objects.equals(columnsDetails.getUtilFunc().getName(), "serial_no.nextval")){
                        valueString.append("DEFAULT,");
                        //System.out.println("Yes");
                    }
                    else{
                        valueString.append("?,");
                    }

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
                        if (Objects.equals(funcname, "astring")) {
                            if (i % 2 == 0) {
                                RandomGenerator rno = new RandomGenerator(1);
                                String dname = rno.astring(UtilGenerators.getMin_len_string(), UtilGenerators.getMax_len_string());
                                stmt.setString(j + 1, dname);
                            } else {
                                String dname = (FeatureBenchConstants2.random_names.values()[new Random().nextInt(FeatureBenchConstants2.random_names.values().length)]).toString();
                                stmt.setString(j + 1, dname);
                            }
                        }
                        else if (Objects.equals(funcname, "get_int_primary_key")) {
                            stmt.setInt(j + 1, UtilGenerators.get_int_primary_key());
                        }
                        else if (Objects.equals(funcname, "numberToIdString")) {
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
                throw new RuntimeException(e);
            }
        }

        public void bindParamBasedOnType(UtilityFunc uf) throws SQLException {

            if (Objects.equals(uf.getName(), "serial_no.nextval")) {
                ArrayList<ParamsForUtilFunc> ob1 = uf.getParams();
                ParamsForUtilFunc puf = ob1.get(0);
                ArrayList<Integer> range = puf.getParameters();
                int upper_range = range.get(1);
                int lower_range = range.get(0);
                UtilGenerators.setUpper_range_for_primary_int_keys(upper_range);
                UtilGenerators.setLower_range_for_primary_int_keys(lower_range);
            }
            else if (Objects.equals(uf.getName(), "astring")) {
                ArrayList<ParamsForUtilFunc> ob1 = uf.getParams();
                ParamsForUtilFunc puf = ob1.get(0);
                ArrayList<Integer> len_range = puf.getParameters();
                int min_len = len_range.get(0);
                int max_len = len_range.get(1);
                UtilGenerators.setMax_len_string(max_len);
                UtilGenerators.setMin_len_string(min_len);
            }
            else if(Objects.equals(uf.getName(), "get_int_primary_key"))
            {
                ArrayList<ParamsForUtilFunc> ob1=uf.getParams();
                ParamsForUtilFunc puf=ob1.get(0);
                ArrayList<Integer> range=puf.getParameters();
                int upper_range=range.get(1);
                int lower_range=range.get(0);
                UtilGenerators.setUpper_range_for_primary_int_keys(upper_range);
                UtilGenerators.setLower_range_for_primary_int_keys(lower_range);
            }
            else if(Objects.equals(uf.getName(), "numberToIdString"))
            {
                ArrayList<ParamsForUtilFunc> ob1=uf.getParams();
                ParamsForUtilFunc puf=ob1.get(0);
                ArrayList<Integer> max_len=puf.getParameters();
                int desired_len=max_len.get(0);
                UtilGenerators.setDesired_length_string_pkeys(desired_len);
            }

        }

        public String findFuncname(UtilityFunc uf) {

            if (Objects.equals(uf.getName(), "serial_no.nextval")) {
                return "serial_no.nextval";
            } else if (Objects.equals(uf.getName(), "astring")) {
                return "astring";
            } else if(Objects.equals(uf.getName(), "get_int_primary_key"))
            {
                return "get_int_primary_key";
            }
            else if(Objects.equals(uf.getName(), "numberToIdString")){
                return "numberToIdString";
            }
         else return null;
        }

        private void loadTables(Connection conn) throws SQLException {
            stmt.executeBatch();
        }
    }


}