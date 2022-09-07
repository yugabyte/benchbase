package com.oltpbenchmark.benchmarks.featurebench.customworkload;

import com.oltpbenchmark.benchmarks.featurebench.YBMicroBenchmark;
import com.oltpbenchmark.benchmarks.featurebench.util.*;
import com.yugabyte.copy.CopyManager;
import com.yugabyte.core.BaseConnection;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;


public class CopyCommandBenchmark extends YBMicroBenchmark {
    private static final Logger LOG = LoggerFactory.getLogger(CopyCommandBenchmark.class);
    int numOfColumns = 10;
    int indexCount = 2;
    int numOfRows = 1000;

    int rowsPerTransaction = 1000;
    String tableName = "orders";
    String file = "orders.csv";

    public CopyCommandBenchmark(HierarchicalConfiguration<ImmutableNode> config) {
        super(config);
        this.executeOnceImplemented = true;
        this.loadOnceImplemented = true;
    }

    @Override
    public void create(Connection conn) throws SQLException {
        try {
            Statement stmtOBj = conn.createStatement();
            LOG.info("Recreate table if it exists");
            stmtOBj.executeUpdate(String.format("DROP TABLE IF EXISTS %s", tableName));
            createTable(conn, tableName, numOfColumns, indexCount);

            LOG.info("Create CSV file with data");
            createCSV(numOfColumns, numOfRows, file);
            stmtOBj.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public ArrayList<LoadRule> loadRules() {
//        return new ArrayList<>();
        return null;
    }

    @Override
    public ArrayList<ExecuteRule> executeRules() {
        ArrayList<ExecuteRule> list4 = new ArrayList<>();
        return list4;
    }

    @Override
    public void cleanUp(Connection conn) throws SQLException {
        try {
            Statement stmtOBj = conn.createStatement();
            LOG.info("=======DROP ALL THE TABLES=======");
            stmtOBj.executeUpdate(String.format("DROP TABLE IF EXISTS %s", tableName));
            stmtOBj.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void loadOnce(Connection conn) {
        runCopyCommand(conn, tableName, file, rowsPerTransaction);
    }

    @Override
    public void executeOnce(Connection conn) {
        runCopyCommand(conn, tableName, file, rowsPerTransaction);
    }


    public void createTable(Connection conn, String tableName, int numberOfColums, int indexCount) {
        List<String> ddls = new ArrayList<>();
        StringBuilder ddl1 = new StringBuilder();
        ddl1.append(String.format("CREATE TABLE %s (id INT primary key, ", tableName));
        for (int i = 1; i <= numberOfColums; i++) {
            ddl1.append(String.format("col%d TEXT", i));
            if (i != numberOfColums)
                ddl1.append(",");
            else
                ddl1.append(");");
        }
        ddls.add(ddl1.toString());

        if (indexCount > 0 && indexCount <= numberOfColums) {
            for (int i = 0; i < indexCount; i++) {
                ddls.add(String.format("CREATE INDEX idx%d on %s(col%d);", i + 1, tableName, i + 1));
            }
        }

        ddls.forEach(ddl -> {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(ddl);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void createCSV(int numberOfColums, int numberOfRows, String file) {
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            int stringLength = 16;
            for (int i = 0; i < numberOfRows; i++) {
                StringBuilder row = new StringBuilder();
                row.append(i);
                for (int j = 0; j < numberOfColums; j++) {
                    row.append(", ");
                    row.append(RandomStringUtils.random(stringLength, true, false));
                }
                row.append("\n");
                outputStream.write(row.toString().getBytes());
            }
            outputStream.close();
            LOG.info("CSV file {} created", file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void runCopyCommand(Connection conn, String tableName, String path, int rowsPerTransaction) {
        try {
            String copyCommand = String.format(
                "COPY %s FROM STDIN (FORMAT CSV, HEADER false, ROWS_PER_TRANSACTION %d)",
                tableName, rowsPerTransaction
            );
            long rowsInserted = new CopyManager((BaseConnection) conn)
                .copyIn(copyCommand,
                    new BufferedReader(new FileReader(path)));
            System.out.printf("%d row(s) inserted%n", rowsInserted);
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
