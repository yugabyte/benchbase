package com.oltpbenchmark.benchmarks.featurebench.customworkload;

import com.oltpbenchmark.api.BenchmarkModule;
import com.oltpbenchmark.benchmarks.featurebench.YBMicroBenchmark;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;

import java.sql.*;

public class PrepareThresholdNot extends YBMicroBenchmark {
    Connection connTemp;

    public PrepareThresholdNot(HierarchicalConfiguration<ImmutableNode> config) {
        super(config);
        this.executeOnceImplemented = true;
        try {
            connTemp = DriverManager.getConnection(
                "jdbc:postgresql://sgupta-pg-i-westc-validate.cbtcvpszcgdq.us-west-2.rds.amazonaws.com:5432/postgres?sslmode=require&preferQueryMode=simple&prepareThreshold=0",
                "postgres",
                "Password321");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public void executeOnce(Connection conn, BenchmarkModule benchmarkModule) throws SQLException {
        int count = 100;
        PreparedStatement pstmt = connTemp.prepareStatement("select * from pkey_hash_point_lookup_tbl_128_par_1 where col_varchar_id_1='1aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa';");
        org.postgresql.PGStatement pgstmt = pstmt.unwrap(org.postgresql.PGStatement.class);
        for (int i = 0; i < count; i++) {
            ResultSet rs = pstmt.executeQuery();
            pgstmt.setPrepareThreshold(0);
            rs.next();
            rs.close();
        }
        pstmt.close();
    }
}
