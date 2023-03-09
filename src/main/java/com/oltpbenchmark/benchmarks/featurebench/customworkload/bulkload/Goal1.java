package com.oltpbenchmark.benchmarks.featurebench.customworkload.bulkload;

import com.oltpbenchmark.api.BenchmarkModule;
import com.oltpbenchmark.benchmarks.featurebench.YBMicroBenchmark;
import com.oltpbenchmark.benchmarks.featurebench.customworkload.bulkload.utils.BulkloadUtils;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Goal1 extends YBMicroBenchmark {
    private static final Logger LOG = LoggerFactory.getLogger(Goal1.class);
    String tableName;
    int numOfColumns;
    int numOfRows;
    int indexCount;
    String filePath;
    int stringLength;
    public List<HashMap<String, String>> workerWorkloads = new ArrayList<>();

    public Goal1(HierarchicalConfiguration<ImmutableNode> config) {
        super(config);
        this.executeOnceImplemented = true;
        this.loadOnceImplemented = true;

        if (config.containsKey("type") && config.getString("type").equalsIgnoreCase("list")) {
            config.configurationsAt("/workloads").forEach(work -> {
                HashMap<String, String> temp = new HashMap<>();
                temp.put("tableName", work.getString("/tableName"));
                temp.put("numOfColumns", work.getString("/columns"));
                temp.put("numOfRows", work.getString("/rows"));
                temp.put("indexCount", work.getString("/indexes"));
                temp.put("filePath", work.getString("/filePath"));
                temp.put("stringLength", work.getString("/stringLength"));
                this.workerWorkloads.add(temp);
            });
        }
        else {
            this.tableName = config.getString("/tableName");
            this.numOfColumns = config.getInt("/columns");
            this.numOfRows = config.getInt("/rows");
            this.indexCount = config.getInt("/indexes");
            this.filePath = config.getString("/filePath");
            this.stringLength = config.getInt("/stringLength");
        }
    }

    public void create(Connection conn) throws SQLException {
        for (HashMap<String, String> work: this.workerWorkloads) {
            Statement stmtOBj = conn.createStatement();
            LOG.info("Recreate table if it exists");
            stmtOBj.executeUpdate(String.format("DROP TABLE IF EXISTS %s", work.get("tableName")));
            stmtOBj.close();
            createTableAndIndexesPerWork(conn, work);
            LOG.info("Create CSV file with data");
            BulkloadUtils.createCSV(work.get("filePath"), Integer.parseInt(work.get("numOfRows")), Integer.parseInt(work.get("numOfColumns")),
                Integer.parseInt(work.get("stringLength")));
        }
    }

    public void loadOnce(Connection conn) throws SQLException {
    }

    public void executeOnce(Connection conn, BenchmarkModule benchmarkModule) throws SQLException {
        BulkloadUtils.runCopyCommand(conn, this.tableName, this.filePath);
    }

    public void createTableAndIndexes(Connection conn) {

        BulkloadUtils.createTable(conn, this.tableName, this.numOfColumns);
        BulkloadUtils.createIndexes(conn, this.indexCount, this.tableName);

    }

    public void createTableAndIndexesPerWork(Connection conn, HashMap<String, String> work) {
        BulkloadUtils.createTable(conn, work.get("tableName"), Integer.parseInt(work.get("numOfColumns")));
        BulkloadUtils.createIndexes(conn, Integer.parseInt(work.get("indexCount")), work.get("tableName"));

    }


    @Override
    public void cleanUp(Connection conn) throws SQLException {
        BulkloadUtils.cleanUp(conn, this.tableName);
    }
}
