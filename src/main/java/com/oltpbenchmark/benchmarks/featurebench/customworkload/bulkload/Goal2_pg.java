package com.oltpbenchmark.benchmarks.featurebench.customworkload.bulkload;

import com.oltpbenchmark.benchmarks.featurebench.YBMicroBenchmark;
import com.yugabyte.copy.CopyManager;
import com.yugabyte.core.BaseConnection;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Goal2_pg extends YBMicroBenchmark {

    private static final Logger LOG = LoggerFactory.getLogger(Goal2_pg.class);

    String tableName;
    int numOfColumns;
    int numOfRows;
    int indexCount;
    String filePath;
    int stringLength;

    boolean create_index_before_load;
    boolean create_index_after_load;

    public Goal2_pg(HierarchicalConfiguration<ImmutableNode> config) {
        super(config);
        this.executeOnceImplemented = true;
        this.loadOnceImplemented = true;
        this.tableName = config.getString("/tableName");
        this.numOfColumns = config.getInt("/columns");
        this.numOfRows = config.getInt("/rows");
        this.indexCount = config.getInt("/indexes");
        this.filePath = config.getString("/filePath");
        this.stringLength = config.getInt("/stringLength");
        this.create_index_before_load = config.getBoolean("/create_index_before_load");
        this.create_index_after_load = config.getBoolean("/create_index_after_load");
    }

    public void create(Connection conn) throws SQLException {
        Statement stmtOBj = conn.createStatement();
        LOG.info("Recreate table if it exists");
        stmtOBj.executeUpdate(String.format("DROP TABLE IF EXISTS %s", this.tableName));
        stmtOBj.close();
        LOG.info("Creating table");
        createTable(conn);

        LOG.info("Create CSV file with data");
        createCSV();

    }

    public void loadOnce(Connection conn) throws SQLException {
    }

    public void executeOnce(Connection conn) throws SQLException {
        if(this.create_index_before_load && (this.indexCount > 0 && this.indexCount <= this.numOfColumns)){
            LOG.info("Creating indexes before load");
            createIndexes(conn);
            LOG.info("Done creating indexes");
        }
        runCopyCommand(conn);
        if(this.create_index_after_load && (this.indexCount > 0 && this.indexCount <= this.numOfColumns)) {
            LOG.info("Creating indexes after load(index back-filling)");
            createIndexes(conn);
            LOG.info("Done creating indexes(Index back-filling done)");
        }

    }

    public void createTable(Connection conn) {
        StringBuilder createStmt = new StringBuilder();
        createStmt.append(String.format("CREATE TABLE %s (id INT primary key, ", tableName));
        for (int i = 1; i <= this.numOfColumns; i++) {
            createStmt.append(String.format("col%d TEXT", i));
            if (i != this.numOfColumns)
                createStmt.append(",");
            else
                createStmt.append(");");
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createStmt.toString());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void createCSV() {
        try {
            FileOutputStream outputStream = new FileOutputStream(this.filePath);

            for (int i = 0; i < this.numOfRows; i++) {
                StringBuilder row = new StringBuilder();
                row.append(i);
                for (int j = 0; j < this.numOfColumns; j++) {
                    row.append(", ");
                    row.append(RandomStringUtils.random(this.stringLength, true, false));
                }
                row.append("\n");
                outputStream.write(row.toString().getBytes());
            }
            outputStream.close();
            LOG.info("CSV file {} created", this.filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void runCopyCommand(Connection conn) {
        try {
            String copyCommand = String.format(
                "COPY %s FROM STDIN (FORMAT CSV, HEADER false)",
                this.tableName);
            long rowsInserted = new CopyManager((BaseConnection) conn)
                .copyIn(copyCommand,
                    new BufferedReader(new FileReader(this.filePath)));
            LOG.info("Number of rows Inserted: {}", rowsInserted);
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void createIndexes(Connection conn) {
        List<String> ddls = new ArrayList<>();
        for (int i = 0; i < this.indexCount; i++) {
            ddls.add(String.format("CREATE INDEX idx%d on %s(col%d);", i + 1, this.tableName, i + 1));
        }
        ddls.forEach(ddl -> {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(ddl);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
