package com.oltpbenchmark.benchmarks.featurebench.customworkload;

import com.oltpbenchmark.benchmarks.featurebench.YBMicroBenchmark;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.log4j.Logger;

public class YBMicroBenchmarkInsertsBatched extends YBMicroBenchmark {
  public final static Logger LOG =
      Logger.getLogger(com.oltpbenchmark.benchmarks.featurebench.customworkload
                           .YBMicroBenchmarkInsertsBatched.class);

  public YBMicroBenchmarkInsertsBatched(
      HierarchicalConfiguration<ImmutableNode> config) {
    super(config);
    this.loadOnceImplemented = true;
  }

  public void loadOnce(Connection conn) throws SQLException {
    String insertStmt = "call insert_demo(100);";
    PreparedStatement stmt = conn.prepareStatement(insertStmt);
    stmt.execute();
    stmt.close();
  }

  public void executeOnce(Connection conn) throws SQLException {
    String values = "";
    for (int i = 101; i <= 1000; i++) {
      values += "(";
      for (int col = 1; col <= 11; col++) {
        values += String.format("%d", i);
        if (col < 11) {
          values += ",";
        }
      }
      values += ")";
      if (i < 1000) {
        values += ",";
      }
    }

    String insertStmt1 = String.format("insert into demo values %s;", values);
    Statement stmtObj = conn.createStatement();
    stmtObj.execute(insertStmt1);
    stmtObj.close();
  }
}
