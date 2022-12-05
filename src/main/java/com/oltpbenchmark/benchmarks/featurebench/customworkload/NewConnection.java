package com.oltpbenchmark.benchmarks.featurebench.customworkload;

import com.oltpbenchmark.api.BenchmarkModule;
import com.oltpbenchmark.benchmarks.featurebench.YBMicroBenchmark;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;

import java.sql.*;

public class NewConnection extends YBMicroBenchmark {
    public NewConnection(HierarchicalConfiguration<ImmutableNode> config) {
        super(config);
        this.executeOnceImplemented = true;
    }
    public void executeOnce(Connection conn, BenchmarkModule benchmarkModule) throws SQLException {
        int count = 100;
        for (int i = 0; i < count; i++) {
            Connection newconn = benchmarkModule.makeConnection();
            Statement stmt = newconn.createStatement();
            stmt.execute("select * from pkey_hash_point_lookup_tbl_128_par_1 where col_varchar_id_1='1aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa';");
        }
    }
}
