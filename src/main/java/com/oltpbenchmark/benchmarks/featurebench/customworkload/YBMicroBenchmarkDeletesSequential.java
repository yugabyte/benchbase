package com.oltpbenchmark.benchmarks.featurebench.customworkload;

import com.oltpbenchmark.benchmarks.featurebench.YBMicroBenchmark;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.log4j.Logger;

public class YBMicroBenchmarkDeletesSequential extends YBMicroBenchmark {
  public final static Logger LOG =
      Logger.getLogger(com.oltpbenchmark.benchmarks.featurebench.customworkload
                           .YBMicroBenchmarkDeletesSequential.class);
  private static final int NUM_ROWS = 1000;

  public YBMicroBenchmarkDeletesSequential(
      HierarchicalConfiguration<ImmutableNode> config) {
    super(config);
    this.loadOnceImplemented = true;
  }

  public void loadOnce(Connection conn) throws SQLException {
    String insertStmt = String.format("call insert_demo(%d);", NUM_ROWS);
    PreparedStatement stmt = conn.prepareStatement(insertStmt);
    stmt.execute();
    stmt.close();
  }

  public void executeOnce(Connection conn) throws SQLException {
    Statement stmtObj = conn.createStatement();
    // Delete last 900 rows sequentially.
    for (int id = 101; id <= NUM_ROWS; id++) {
      stmtObj.execute(String.format("delete from demo where id = %d;", id));
    }
    stmtObj.close();
  }
}
