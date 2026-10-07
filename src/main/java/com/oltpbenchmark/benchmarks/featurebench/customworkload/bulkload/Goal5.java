package com.oltpbenchmark.benchmarks.featurebench.customworkload.bulkload;

import com.oltpbenchmark.api.BenchmarkModule;
import com.oltpbenchmark.benchmarks.featurebench.YBMicroBenchmark;
import com.oltpbenchmark.benchmarks.featurebench.customworkload.bulkload.utils.BulkloadUtils;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Goal5 - parallel bulkload: COPY the same CSV into tableCount tables concurrently,
 * each table on its own connection/thread (at most `parallelism` at a time).
 */
public class Goal5 extends YBMicroBenchmark {

    private static final Logger LOG = LoggerFactory.getLogger(Goal5.class);

    String tableName;
    int tableCount;
    int parallelism;
    int numOfColumns;
    int numOfRows;
    String filePath;
    int stringLength;
    int tablets;

    public Goal5(HierarchicalConfiguration<ImmutableNode> config) {
        super(config);
        this.executeOnceImplemented = true;
        this.loadOnceImplemented = true;
        this.tableName = config.getString("/tableName");
        this.tableCount = config.getInt("/tableCount");
        this.parallelism = config.getInt("/parallelism", this.tableCount);
        this.numOfColumns = config.getInt("/columns");
        this.numOfRows = config.getInt("/rows");
        this.filePath = config.getString("/filePath");
        this.stringLength = config.getInt("/stringLength");
        // YugabyteDB only (not postgres / colocated): hash-shard each table and SPLIT INTO this many tablets
        this.tablets = config.getInt("/tablets", 0);
    }

    private String tableName(int i) {
        return this.tableName + i;
    }

    public void create(Connection conn) throws SQLException {
        LOG.info("Recreating {} tables", this.tableCount);
        for (int i = 1; i <= this.tableCount; i++) {
            try (Statement stmtOBj = conn.createStatement()) {
                stmtOBj.executeUpdate(String.format("DROP TABLE IF EXISTS %s", tableName(i)));
            }
            BulkloadUtils.createTable(conn, tableName(i), this.numOfColumns, this.tablets);
        }
        LOG.info("Create CSV file with data");
        File parent = new File(this.filePath).getAbsoluteFile().getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        BulkloadUtils.createCSV(this.filePath, this.numOfRows, this.numOfColumns, this.stringLength);
    }

    public void loadOnce(Connection conn) throws SQLException {
    }

    public void executeOnce(Connection conn, BenchmarkModule benchmarkModule) throws SQLException {
        ExecutorService pool = Executors.newFixedThreadPool(this.parallelism);
        List<Future<?>> futures = new ArrayList<>();
        long start = System.currentTimeMillis();
        for (int i = 1; i <= this.tableCount; i++) {
            String table = tableName(i);
            futures.add(pool.submit(() -> {
                try (Connection copyConn = benchmarkModule.makeConnection()) {
                    copyConn.setAutoCommit(true);
                    BulkloadUtils.runCopyCommand(copyConn, table, this.filePath);
                } catch (SQLException e) {
                    throw new RuntimeException("COPY into " + table + " failed", e);
                }
            }));
        }
        pool.shutdown();
        try {
            for (Future<?> f : futures) {
                f.get();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            pool.shutdownNow();
            throw new RuntimeException(e.getCause());
        }
        LOG.info("COPY into {} tables ({} parallel) took {} ms",
            this.tableCount, this.parallelism, System.currentTimeMillis() - start);
    }

    @Override
    public void cleanUp(Connection conn) throws SQLException {
        for (int i = 1; i <= this.tableCount; i++) {
            BulkloadUtils.cleanUp(conn, tableName(i));
        }
    }

}
