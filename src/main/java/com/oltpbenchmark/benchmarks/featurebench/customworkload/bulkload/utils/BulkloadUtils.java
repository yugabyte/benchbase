package com.oltpbenchmark.benchmarks.featurebench.customworkload.bulkload.utils;

import org.apache.commons.lang3.RandomStringUtils;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BulkloadUtils {
    private static final Logger LOG = LoggerFactory.getLogger(BulkloadUtils.class);

    private static final String AWS_ACCESS_KEY = "your-access-key";
    private static final String AWS_SECRET_KEY = "your-secret-key";
    private static final String AWS_REGION = "us-west-2";
    private static final String BUCKET_NAME = "perf-team";
    private static final String S3_FILE_PREFIX = "csv_files";

    public static void createCSV(String workload, String filePath, int numOfRows, int numOfColumns, int stringLength) {
        String accessKeyId = System.getenv("AWS_ACCESS_KEY_ID");
        String secretAccessKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        String sessionToken = System.getenv("AWS_SESSION_TOKEN");
        String s3Key = S3_FILE_PREFIX + "/" + workload + "/" + filePath;

        BasicSessionCredentials sessionCredentials = new BasicSessionCredentials(
                accessKeyId,
                secretAccessKey,
                sessionToken
        );

        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                // .withCredentials(new AWSStaticCredentialsProvider(sessionCredentials))
                .withRegion(AWS_REGION)
                .build();

        try {
            if (s3Client.doesObjectExist(BUCKET_NAME, s3Key)) {
                LOG.info("File {} exists in S3. Downloading...", s3Key);
                downloadFromS3(s3Client, s3Key, filePath);
            } else {
                LOG.info("File {} does not exist in S3. Creating and uploading...", filePath);
                createAndUploadCSV(s3Client, s3Key, filePath, numOfRows, numOfColumns, stringLength);
            }
        } catch (AmazonS3Exception e) {
            LOG.error("Amazon S3 error: {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            s3Client.shutdown();
        }
    }

        private static void createAndUploadCSV(AmazonS3 s3Client, String s3Key, String filePath, int numOfRows, int numOfColumns, int stringLength) {
        try {
            FileOutputStream outputStream = new FileOutputStream(filePath);

            for (int i = 0; i < numOfRows; i++) {
                StringBuilder row = new StringBuilder();
                row.append(i);
                for (int j = 0; j < numOfColumns; j++) {
                    row.append(", ");
                    row.append(RandomStringUtils.random(stringLength, true, false));
                }
                row.append("\n");
                outputStream.write(row.toString().getBytes());
            }
            outputStream.close();
            LOG.info("CSV file {} created", filePath);

            s3Client.putObject(new PutObjectRequest(BUCKET_NAME, s3Key, new File(filePath)));
            LOG.info("CSV file {} uploaded to S3", filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void downloadFromS3(AmazonS3 s3Client, String s3Key, String filePath) {
        try {
            S3Object s3Object = s3Client.getObject(new GetObjectRequest(BUCKET_NAME, s3Key));
            InputStream objectData = s3Object.getObjectContent();
            Files.copy(objectData, Paths.get(filePath));
            objectData.close();
            LOG.info("CSV file downloaded from S3 bucket to {}", filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void createCSV(String filePath, int numOfRows, int numOfColumns, int stringLength) {
        try {
            FileOutputStream outputStream = new FileOutputStream(filePath);

            for (int i = 0; i < numOfRows; i++) {
                StringBuilder row = new StringBuilder();
                row.append(i);
                for (int j = 0; j < numOfColumns; j++) {
                    row.append(", ");
                    row.append(RandomStringUtils.random(stringLength, true, false));
                }
                row.append("\n");
                outputStream.write(row.toString().getBytes());
            }
            outputStream.close();
            LOG.info("CSV file {} created", filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void createTable(Connection conn, String tableName, int numOfColumns) {
        StringBuilder createStmt = new StringBuilder();
        createStmt.append(String.format("CREATE TABLE %s (id INT, ", tableName));
        for (int i = 1; i <= numOfColumns; i++) {
            createStmt.append(String.format("col%d TEXT ,", i));
        }
        try {
            if (conn.getMetaData().getUserName().equalsIgnoreCase("yugabyte")) {
                createStmt.append("primary key (id asc) );");
            } else {
                createStmt.append("primary key (id) );");
            }

            Statement stmt = conn.createStatement();
            stmt.execute(createStmt.toString());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void createIndexes(Connection conn, int indexCount, String tableName) {
        List<String> ddls = new ArrayList<>();
        for (int i = 0; i < indexCount; i++) {
            ddls.add(String.format("CREATE INDEX idx%d on %s(col%d);", i + 1, tableName, i + 1));
        }
        ddls.forEach(ddl -> {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(ddl);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void runCopyCommand(Connection conn, String tableName, String filePath) {
        try {
            String copyCommand = String.format(
                "COPY %s FROM STDIN (FORMAT CSV, HEADER false)",
                tableName);
            long rowsInserted = 0;
            if (conn.getMetaData().getUserName().equalsIgnoreCase("yugabyte")) {
                rowsInserted = new com.yugabyte.copy.CopyManager((com.yugabyte.core.BaseConnection) conn)
                    .copyIn(copyCommand,
                        new BufferedReader(new FileReader(filePath)));
            } else {
                rowsInserted = new CopyManager((BaseConnection) conn)
                    .copyIn(copyCommand,
                        new BufferedReader(new FileReader(filePath)));
            }
            LOG.info("Number of rows Inserted: {}", rowsInserted);
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void cleanUp(Connection conn,String tableName) throws SQLException {
        String countQuery = "select count(*) from " + tableName;
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(countQuery);
        long count = 0;
        while (rs.next()) {
            count = rs.getLong(1);
        }
        LOG.info("Number of rows inserted are: " + count);
    }

}
