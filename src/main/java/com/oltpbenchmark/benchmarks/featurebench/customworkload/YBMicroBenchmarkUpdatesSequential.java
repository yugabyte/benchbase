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
                           .YBMicroBenchmarkImplSonal.class);
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
          "update demo set col1 = col1 + 1000, col5 = 'UPDATED', col6 = '2015-07-17 22:00:00+00', col8 = 123456789012345, col13 = true, col9 = 'UPDATED', col10 = 100, col11 = 123456789012345, col12 = 'UPDATED', col2 = 123 where id = %d;",
          id));
    }
    stmtObj.close();
  }
}
