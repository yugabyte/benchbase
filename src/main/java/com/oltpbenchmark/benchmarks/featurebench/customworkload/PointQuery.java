package com.oltpbenchmark.benchmarks.featurebench.customworkload;

import com.oltpbenchmark.api.Procedure.UserAbortException;
import com.oltpbenchmark.benchmarks.featurebench.YBMicroBenchmark;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;

import java.sql.*;
import java.util.Random;

public class PointQuery extends YBMicroBenchmark {

    private Statement stmtObj = null;

    public PointQuery(HierarchicalConfiguration<ImmutableNode> config) {
        super(config);
    }

    @Override
    public void create(Connection conn) throws SQLException {
        stmtObj = conn.createStatement();
        stmtObj.execute("create table t1(a int primary key, b int)");
    }

    @Override
    public void cleanUp(Connection conn) throws SQLException {
        stmtObj = conn.createStatement();
        stmtObj.execute("drop table t1");
    }

    @Override
    public void loadOnce(Connection conn) throws SQLException {
        stmtObj = conn.createStatement();
        stmtObj.execute("insert into t1 select generate_series a, generate_series b from generate_series(1,100000)");
    }


    @Override
    public void execute(Connection conn) throws SQLException {

        Random random = new Random();
        int a = random.ints(1, 100000).findAny().getAsInt();
        PreparedStatement stmt = conn.prepareStatement("select * from t1 where a = ?");
        stmt.setLong(1, a);
        try (ResultSet result = stmt.executeQuery()) {
            if (!result.next()) {
                throw new UserAbortException("msg");
            }

        }
    }

}
