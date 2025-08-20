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
    // Update last 900 rows sequentially.
    for (int id = 101; id <= NUM_ROWS; id++) {
      stmtObj.execute(String.format(
          "update demo set col1 = col1 + 10000, col2 = col2 + 10000, col3 = col3 + 10000, col4 = col4 + 10000, col5 = col5 + 10000, col6 = col6 + 10000, col7 = col7 + 10000, col8 = col8 + 10000, col9 = col9 + 10000, col10 = col10 + 10000 where id = %d;",
          id));
    }
    stmtObj.close();
  }
}
