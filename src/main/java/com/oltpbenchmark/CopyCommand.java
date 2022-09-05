package com.oltpbenchmark;

import com.oltpbenchmark.api.Procedure;
import com.yugabyte.copy.CopyManager;
import com.yugabyte.core.BaseConnection;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CopyCommand extends Procedure {
    public static void main(String[] args) throws SQLException, IOException {
        String ybUrl = "jdbc:yugabytedb://localhost:5433/yugabyte?sslmode=disable&user=yugabyte";

        int numberOfRows = 10;
        String file = "orders.csv";
        int numberOfColums = 10;
        int indexCount = 0;
        CopyCommand cmd = new CopyCommand();
        cmd.createCSV(numberOfColums, numberOfRows, file);

        Connection conn = DriverManager.getConnection(ybUrl);
        cmd.createTable(conn, "orders", numberOfColums, indexCount);
        cmd.runCopyCommand(conn, "orders", "orders.csv", 2000);
        conn.close();
    }


    public void createTable(Connection conn, String tableName, int numberOfColums, int indexCount) {

//        String ddl1 = String.format("CREATE TABLE %s (id INT primary key, ", tableName);
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

        if(indexCount > 0 && indexCount <= numberOfColums) {
            for(int i=0; i < indexCount; i++) {
                ddls.add(String.format("CREATE INDEX idx%d on %s(col%d);", i+1, tableName, i+1));
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


    public void createCSV(int numberOfColums, int numberOfRows, String file) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(file);

        int stringLength = 16;
        for(int i=0; i < numberOfRows; i++) {
            StringBuilder row = new StringBuilder();
            row.append(i);
            for(int j=0; j < numberOfColums; j++) {
                row.append(", ");
                row.append(RandomStringUtils.random(stringLength, true, false));
            }
            row.append("\n");
            outputStream.write(row.toString().getBytes());
        }
        outputStream.close();
    }

    public void runCopyCommand(Connection conn, String tableName, String path, int rowsPerTransaction) throws SQLException, IOException {
        String copyCommand = String.format(
                                "COPY %s FROM STDIN (FORMAT CSV, HEADER false, ROWS_PER_TRANSACTION %d)",
                                tableName, rowsPerTransaction
                            );
        long rowsInserted = new CopyManager((BaseConnection) conn)
                            .copyIn(copyCommand,
                                new BufferedReader(new FileReader(path)));
        System.out.printf("%d row(s) inserted%n", rowsInserted);
    }

    public void run(Connection conn, String sqlStmt) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sqlStmt);
            while (rs.next()) {
                System.out.println(rs.getRow());
            }
        }
    }
}
