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

import com.oltpbenchmark.DBWorkload;
import com.oltpbenchmark.WorkloadConfiguration;
import com.oltpbenchmark.api.BenchmarkModule;
import com.oltpbenchmark.api.Loader;
import com.oltpbenchmark.api.LoaderThread;
import com.oltpbenchmark.api.Worker;
import com.oltpbenchmark.benchmarks.featurebench.procedures.FeatureBench;

import com.oltpbenchmark.benchmarks.featurebench.util.ExecuteRule;
import com.oltpbenchmark.benchmarks.featurebench.util.LoadRule;
import com.oltpbenchmark.benchmarks.featurebench.util.YBMicroBenchmark;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class FeatureBenchBenchmark extends BenchmarkModule {

    private static final Logger LOG = LoggerFactory.getLogger(DBWorkload.class);
    public FeatureBenchBenchmark(WorkloadConfiguration workConf) {
        super(workConf);
    }

    @Override
    protected List<Worker<? extends BenchmarkModule>> makeWorkersImpl() {
        List<Worker<? extends BenchmarkModule>> workers = new ArrayList<>();
        LOG.info("\nSub benchmark for featurebench : {}\n", workConf.getMicroBenchmark());
        HierarchicalConfiguration<ImmutableNode> conf = workConf.getMicroBenchmark();
        String cls = conf.getString("class");
//        HierarchicalConfiguration<ImmutableNode> props = conf.configurationAt("properties");
        /*try {
            YBMicroBenchmark ybm = (YBMicroBenchmark)Class.forName(cls).getDeclaredConstructor().newInstance();
//            ybm.createDB(workConf);
            ArrayList<LoadRule> loadRules = ybm.loadRule();
            ArrayList<ExecuteRule> executeRules = ybm.executeRule();

        } catch (InstantiationException | IllegalAccessException |
                 InvocationTargetException | NoSuchMethodException |
                 ClassNotFoundException e) {
            throw new RuntimeException(e);
        }*/

        for (int i = 0; i < workConf.getTerminals(); ++i) {
            workers.add(new FeatureBenchWorker(this, i));
        }
        return workers;
    }

    @Override
    protected Loader<FeatureBenchBenchmark> makeLoaderImpl() {

        LOG.info("\nLoader implt for Sub benchmark for featurebench : {}\n", workConf.getMicroBenchmark());
        HierarchicalConfiguration<ImmutableNode> conf = workConf.getMicroBenchmark();
        String cls = conf.getString("class");
        FeatureBenchLoader loader = new FeatureBenchLoader(this);
        loader.workloadClass = cls;
        return loader;
    }

    @Override
    protected Package getProcedurePackageImpl() {
        return FeatureBench.class.getPackage();
    }

}
