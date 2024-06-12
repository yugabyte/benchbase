package com.oltpbenchmark.benchmarks.featurebench.customworkload;

import com.oltpbenchmark.benchmarks.featurebench.YBMicroBenchmark;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.log4j.Logger;

public class YBMicroBenchmarkDeletesBatched extends YBMicroBenchmark {
  public final static Logger LOG =
      Logger.getLogger(com.oltpbenchmark.benchmarks.featurebench.customworkload
                           .YBMicroBenchmarkDeletesBatched.class);
  private static final int NUM_ROWS = 1000;

  public YBMicroBenchmarkDeletesBatched(
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
    for (int i = 101; i <= NUM_ROWS; i++) {
      inClause += String.format("%d", i);
      if (i < NUM_ROWS) {
        inClause += ",";
      }
    }
    inClause += ")";

    // Delete the last 900 rows.
    String batchedDeleteStatement =
        String.format("delete from demo where id in %s", inClause);
    Statement stmtObj = conn.createStatement();
    stmtObj.execute(batchedDeleteStatement);
    stmtObj.close();
  }
}
