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
import com.oltpbenchmark.benchmarks.featurebench.util.LoadRule;
import com.oltpbenchmark.benchmarks.featurebench.util.UtilityFunc;
import com.oltpbenchmark.benchmarks.featurebench.util.YBMicroBenchmark;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * This doesn't load any data!
 *
 */
public class FeatureBenchLoader extends Loader<FeatureBenchBenchmark> {


    public String workloadClass = null;
    public FeatureBenchLoader(FeatureBenchBenchmark benchmark) {
        super(benchmark);
    }

    @Override
    public List<LoaderThread> createLoaderThreads() {

        YBMicroBenchmark ybm = null;
        try {
            ybm = (YBMicroBenchmark)Class.forName(workloadClass).getDeclaredConstructor().newInstance();
//            boolean loadOnce = useReflectionToCheckIfLoadOnceIsImplemented();
//            boolean loadOnce = false;
            ArrayList<LoadRule> loadRules = ybm.loadRule();
            /*if (loadOnce) {
                // Make a single thread and call loadOnce from there
                ArrayList<LoaderThread> lt = new ArrayList<>();
                lt.add(new GeneratorOnce(lrs.get(0), cfile));
                return lt;
            }*/


            ArrayList<LoaderThread> loaderThreads = new ArrayList<>();

            for (LoadRule loadRule : loadRules) {
                loaderThreads.add(new Generator(loadRule));
            }
            /*for(int i= 0; i < lrs.size(); i++) {
                lt.add(new Generator(lrs.get(i)));
            }*/
            return loaderThreads;
        } catch (InstantiationException | IllegalAccessException |
                 InvocationTargetException | NoSuchMethodException |
                 ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /*private class GeneratorOnce extends LoaderThread {
        public GeneratorOnce(File cfiile) {
            super(benchmark);
        }
        @Override
        public void load(Connection conn) {
            ybm.loadOnce(conn, cFile);
        }
    }*/
    private class Generator extends LoaderThread {
        final LoadRule loadRule;
        public Generator(LoadRule loadRule) {
            super(benchmark);
            this.loadRule = loadRule;
        }

        /*void bindParamBasedOnType(UtilityFunc utilf, PreparedStatement ps, int index) {
            if (utilf.getName().equalsIgnoreCase("randomString")) {
                stmtChecking.setInt(index, this.randomString(utilf.param1, utilf.param2));
            }
        }*/

        @Override
        public void load(Connection conn) {
            try {
                String insertStr = "Insert into %s (%s) values(%s)";
                String columnString = "";
                String valueString = "";

                for (int i=0; i < lr; i++) {
                    columnString += lr.columns[i].columnName;
                    valueString += "?";
                }

                // insertStr ==> "Insert into savings(accountid,balance) values(?, ?)"
                PreparedStatement insertStmt = conn.prepareStatement(insertStr);
                for (int i=0; i < lr.numValue; i++) {
                    for (int j=0; j < lr.numColumns; i++) {
                        insertStmt = bindParamBasedOnType(lr.utilityFunction, insertStmt, j + 1);
                    }
                    insertStmt.addBatch();
                    if (++batchSize >= workConf.getBatchSize()) {
                        insertStmt.executeBatch();
                        batchSize = 0;
                    }
                }
                insertStmt.executeBatch();
            } catch (SQLException ex) {
                LOG.error("Failed to load data", ex);
                throw new RuntimeException(ex);
            }
        }
    }


}