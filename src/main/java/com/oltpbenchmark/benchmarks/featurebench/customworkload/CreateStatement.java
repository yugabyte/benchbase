package com.oltpbenchmark.benchmarks.featurebench.customworkload;

import com.oltpbenchmark.api.BenchmarkModule;
import com.oltpbenchmark.benchmarks.featurebench.YBMicroBenchmark;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class CreateStatement extends YBMicroBenchmark {
    public CreateStatement(HierarchicalConfiguration<ImmutableNode> config) {
        super(config);
        this.executeOnceImplemented = true;
    }

    public void executeOnce(Connection conn, BenchmarkModule benchmarkModule) throws SQLException {
        int count = 100;
        for (int i = 0; i < count; i++) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("select * from pkey_hash_point_lookup_tbl_128_par_1 where col_varchar_id_1='1aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa'");
            while (rs.next()) {
                System.out.println(rs.getString(1));
            }
        }
    }
}
