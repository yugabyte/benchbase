package com.oltpbenchmark.benchmarks.featurebench.customworkload;

import com.oltpbenchmark.benchmarks.featurebench.YBMicroBenchmark;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.log4j.Logger;

public class YBMicroBenchmarkInsertsSequential extends YBMicroBenchmark {
  public final static Logger LOG =
      Logger.getLogger(com.oltpbenchmark.benchmarks.featurebench.customworkload
                           .YBMicroBenchmarkInsertsSequential.class);

  public YBMicroBenchmarkInsertsSequential(
      HierarchicalConfiguration<ImmutableNode> config) {
    super(config);
    this.loadOnceImplemented = true;
  }

  public void loadOnce(Connection conn) throws SQLException {
    String insertStmt = "call insert_demo(10000);";
    PreparedStatement stmt = conn.prepareStatement(insertStmt);
    stmt.execute();
    stmt.close();
  }

  public void executeOnce(Connection conn) throws SQLException {
    Statement stmtObj = conn.createStatement();
    String partialInsertQuery =
        "insert into demo (id, col1, col2, col3, col4, col5, col6, col7, col8, col9, col10) select n, n, n+100, n*10+1, n-45, n, n, n*50, n*10, n+1111, n";
    for (int id = 10001; id <= 20000; id++) {
      String suffixInsertQuery =
          String.format(" from generate_series(%d, %d) n;", id, id);
      stmtObj.execute(partialInsertQuery + suffixInsertQuery);
    }
    stmtObj.close();
  }
}
