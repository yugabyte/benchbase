package com.oltpbenchmark.benchmarks.featurebench.customworkload;

import com.oltpbenchmark.benchmarks.featurebench.YBMicroBenchmark;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.log4j.Logger;

public class YBMicroBenchmarkUpdatesBatched extends YBMicroBenchmark {
  public final static Logger LOG =
      Logger.getLogger(com.oltpbenchmark.benchmarks.featurebench.customworkload
                           .YBMicroBenchmarkUpdatesBatched.class);
  private static final int NUM_ROWS = 10000;

  public YBMicroBenchmarkUpdatesBatched(
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
    String inClause = "(";
    for (int i = 1; i <= NUM_ROWS; i++) {
      inClause += String.format("%d", i);
      if (i < NUM_ROWS) {
        inClause += ",";
      }
    }
    inClause += ")";

    // Update all the columns which have an index in the 10-index benchmark so
    // that we can accurately determine the impact of maintaining indexes.
    String batchedUpdateStatement = String.format(
        "update demo set col1 = col1 + 1000, col2 = 101, col3 = col3 - 100, col4 = 1111, col5 = 2222, col6 = 7777, col7 = 1234, col8 = col7 + col6, col9 = col1 + col3, col10 = col2 - col6 where id in %s;",
        inClause);
    Statement stmtObj = conn.createStatement();
    stmtObj.execute(batchedUpdateStatement);
    stmtObj.close();
  }
}
