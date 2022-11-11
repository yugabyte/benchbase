package com.oltpbenchmark.benchmarks.featurebench.customworkload;

import com.oltpbenchmark.benchmarks.featurebench.YBMicroBenchmark;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.log4j.Logger;

public class YBMicroBenchmarkUpdatesSequential extends YBMicroBenchmark {
  public final static Logger LOG =
      Logger.getLogger(com.oltpbenchmark.benchmarks.featurebench.customworkload
                           .YBMicroBenchmarkUpdatesSequential.class);
  private static final int NUM_ROWS = 10000;

  public YBMicroBenchmarkUpdatesSequential(
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
    for (int id = 1; id <= NUM_ROWS; id++) {
      // Update all the columns which have an index in the 10-index benchmark so
      // that we can accurately determine the impact of maintaining indexes.
      stmtObj.execute(String.format(
          "update demo set col1 = col1 + 1000, col2 = 101, col3 = col3 - 100, col4 = 1111, col5 = 2222, col6 = 7777, col7 = 1234, col8 = col7 + col6, col9 = col1 + col3, col10 = col2 - col6 where id = %d;",
          id));
    }
    stmtObj.close();
  }
}
