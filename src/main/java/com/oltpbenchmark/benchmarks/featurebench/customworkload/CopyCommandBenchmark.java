package com.oltpbenchmark.benchmarks.featurebench.customworkload;

import com.oltpbenchmark.WorkloadConfiguration;
import com.oltpbenchmark.benchmarks.featurebench.util.ExecuteRule;
import com.oltpbenchmark.benchmarks.featurebench.util.LoadRule;
import com.oltpbenchmark.benchmarks.featurebench.util.YBMicroBenchmark;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

public class CopyCommandBenchmark implements YBMicroBenchmark {
    private static final Logger LOG = LoggerFactory.getLogger(CopyCommandBenchmark.class);

    @Override
    public void createDB(Connection conn) throws SQLException {
        LOG.info("CreateDB called");
    }

    @Override
    public ArrayList<LoadRule> loadRule() {
        LOG.info("loadRule Called");
        return null;
    }

    @Override
    public ArrayList<ExecuteRule> executeRule() {
        LOG.info("executeRule called");
        return null;
    }

    @Override
    public void cleanUp(Connection conn) throws SQLException {
        LOG.info("CleanUp called");
    }
}
