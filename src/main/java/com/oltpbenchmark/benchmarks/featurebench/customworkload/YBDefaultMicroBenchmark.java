package com.oltpbenchmark.benchmarks.featurebench.customworkload;

import com.oltpbenchmark.api.BenchmarkModule;
import com.oltpbenchmark.benchmarks.featurebench.YBMicroBenchmark;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;


public class YBDefaultMicroBenchmark extends YBMicroBenchmark {
    private static final Logger LOG = LoggerFactory.getLogger(YBDefaultMicroBenchmark.class);

    public YBDefaultMicroBenchmark(HierarchicalConfiguration<ImmutableNode> config) {
        super(config);
        if (!config.configurationsAt("executeOnce").isEmpty()) {
            this.executeOnceImplemented = true;
        }
    }

    @Override
    public void executeOnce(Connection conn, BenchmarkModule benchmarkModule) throws SQLException {
        List<HierarchicalConfiguration<ImmutableNode>> executeOnceBlocks = config.configurationsAt("executeOnce");
        if (executeOnceBlocks.isEmpty()) {
            return;
        }

        LOG.info("Executing executeOnce queries from YAML config");
        Statement stmtObj = conn.createStatement();
        long start = System.currentTimeMillis();

        for (HierarchicalConfiguration<ImmutableNode> block : executeOnceBlocks) {
            List<HierarchicalConfiguration<ImmutableNode>> runItems = block.configurationsAt("run");
            for (HierarchicalConfiguration<ImmutableNode> runItem : runItems) {
                List<HierarchicalConfiguration<ImmutableNode>> queries = runItem.configurationsAt("queries");
                for (HierarchicalConfiguration<ImmutableNode> queryConfig : queries) {
                    String query = queryConfig.getString("query");
                    if (query != null && !query.isEmpty()) {
                        LOG.info("Executing: {}", query);
                        stmtObj.execute(query);
                    }
                }
            }
        }

        long end = System.currentTimeMillis();
        LOG.info("Elapsed time in executeOnce phase: {} milliseconds", end - start);
        stmtObj.close();
    }
}
