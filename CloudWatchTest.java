import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Standalone test for AWS CloudWatch RDS monitoring functionality
 * Tests the fixes implemented for DNS resolution and connectivity issues
 */
public class CloudWatchTest {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java CloudWatchTest <postgres-endpoint> <username> <password>");
            System.out.println("Example: java CloudWatchTest mydb.cluster-xyz.us-east-1.rds.amazonaws.com:5432 postgres mypassword");
            System.out.println("\nAWS credentials should be available as environment variables:");
            System.out.println("- AWS_ACCESS_KEY_ID");
            System.out.println("- AWS_SECRET_ACCESS_KEY");
            System.out.println("- AWS_DEFAULT_REGION (optional)");
            System.exit(1);
        }

        String postgresEndpoint = args[0];
        String username = args[1];
        String password = args[2];

        System.out.println("=== CloudWatch RDS Monitoring Test ===");
        System.out.println("PostgreSQL Endpoint: " + postgresEndpoint);
        System.out.println("Username: " + username);
        System.out.println("Password: " + "*".repeat(password.length()));

        // Check AWS environment variables
        checkAwsEnvironment();

        // Extract RDS details from endpoint
        String rdsInstanceIdentifier = extractRDSInstanceIdentifier(postgresEndpoint);
        String awsRegion = extractAWSRegion(postgresEndpoint);

        if (rdsInstanceIdentifier == null || awsRegion == null) {
            System.err.println("ERROR: Could not extract RDS instance identifier or region from endpoint: " + postgresEndpoint);
            System.err.println("Expected format: mydb.cluster-xyz.us-east-1.rds.amazonaws.com:5432");
            System.exit(1);
        }

        System.out.println("Extracted RDS Instance: " + rdsInstanceIdentifier);
        System.out.println("Extracted AWS Region: " + awsRegion);
        System.out.println();

        // Test 1: CloudWatch connectivity
        System.out.println("=== Test 1: CloudWatch Connectivity ===");
        boolean connectivityTest = testCloudWatchConnectivity(awsRegion);
        if (!connectivityTest) {
            System.err.println("CloudWatch connectivity test failed. Proceeding anyway...");
        }
        System.out.println();

        // Test 2: Get RDS CPU utilization
        System.out.println("=== Test 2: RDS CPU Utilization ===");
        double cpuUtilization = getRDSCPUUtilization(rdsInstanceIdentifier, awsRegion);
        System.out.println("CPU Utilization Result: " + cpuUtilization + "%");
        System.out.println();

        // Test 3: Multiple readings simulation
        System.out.println("=== Test 3: Multiple Readings (3 attempts) ===");
        for (int i = 0; i < 3; i++) {
            System.out.print("Reading " + (i + 1) + "/3: ");
            double reading = getRDSCPUUtilization(rdsInstanceIdentifier, awsRegion);
            System.out.println(reading + "%");

            if (i < 2) {
                try {
                    Thread.sleep(5000); // 5 second interval
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        System.out.println("\n=== Test Complete ===");
        System.out.println("If all tests passed without DNS errors, the fix is working correctly!");
    }

    private static void checkAwsEnvironment() {
        System.out.println("=== AWS Environment Check ===");
        String accessKeyId = System.getenv("AWS_ACCESS_KEY_ID");
        String secretAccessKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        String defaultRegion = System.getenv("AWS_DEFAULT_REGION");

        if (accessKeyId != null) {
            System.out.println("✓ AWS_ACCESS_KEY_ID: " + accessKeyId.substring(0, Math.min(8, accessKeyId.length())) + "...");
        } else {
            System.out.println("✗ AWS_ACCESS_KEY_ID: Not set");
        }

        if (secretAccessKey != null) {
            System.out.println("✓ AWS_SECRET_ACCESS_KEY: " + "*".repeat(8) + "...");
        } else {
            System.out.println("✗ AWS_SECRET_ACCESS_KEY: Not set");
        }

        if (defaultRegion != null) {
            System.out.println("✓ AWS_DEFAULT_REGION: " + defaultRegion);
        } else {
            System.out.println("○ AWS_DEFAULT_REGION: Not set (will use region from endpoint)");
        }
        System.out.println();
    }

    private static String extractRDSInstanceIdentifier(String endpoint) {
        try {
            // Remove port if present
            String hostPart = endpoint.split(":")[0];

            if (hostPart.contains(".rds.amazonaws.com")) {
                return hostPart.split("\\.")[0];
            }
        } catch (Exception e) {
            System.err.println("Error extracting RDS instance identifier: " + e.getMessage());
        }
        return null;
    }

    private static String extractAWSRegion(String endpoint) {
        try {
            String hostPart = endpoint.split(":")[0];

            if (hostPart.contains(".rds.amazonaws.com")) {
                String[] parts = hostPart.split("\\.");
                if (parts.length >= 3) {
                    return parts[parts.length - 3]; // us-east-1 in example
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting AWS region: " + e.getMessage());
        }
        return null;
    }

    private static boolean testCloudWatchConnectivity(String awsRegion) {
        System.out.println("Testing CloudWatch connectivity for region: " + awsRegion);

        try {
            CloudWatchClient testClient = CloudWatchClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                    .apiCallTimeout(Duration.ofSeconds(30))
                    .build())
                .build();

            System.out.println("CloudWatch client created successfully");

            // Simple test call to list metrics
            ListMetricsRequest testRequest = ListMetricsRequest.builder()
                .namespace("AWS/RDS")
                .build();

            System.out.println("Attempting to list RDS metrics...");
            ListMetricsResponse response = testClient.listMetrics(testRequest);

            testClient.close();
            System.out.println("✓ CloudWatch connectivity test PASSED");
            System.out.println("Found " + response.metrics().size() + " RDS metrics");
            return true;

        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            if (e.getCause() instanceof java.net.UnknownHostException) {
                System.err.println("✗ DNS resolution failed for CloudWatch endpoint");
                System.err.println("  Error: " + e.getMessage());
                System.err.println("  This indicates network connectivity issues");
            } else {
                System.err.println("✗ AWS SDK error: " + e.getMessage());
            }
            return false;
        } catch (software.amazon.awssdk.services.cloudwatch.model.CloudWatchException e) {
            System.err.println("✗ CloudWatch service error: " + e.awsErrorDetails().errorMessage());
            System.err.println("  This might be a permissions issue");
            return false;
        } catch (Exception e) {
            System.err.println("✗ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static double getRDSCPUUtilization(String dbInstanceIdentifier, String awsRegion) {
        double cpuUtilization = 0.0;
        CloudWatchClient cloudWatchClient = null;

        try {
            System.out.println("Getting CPU utilization for RDS instance: " + dbInstanceIdentifier);

            // Configure client with improved settings
            ClientOverrideConfiguration clientConfig = ClientOverrideConfiguration.builder()
                .apiCallTimeout(Duration.ofSeconds(60))
                .apiCallAttemptTimeout(Duration.ofSeconds(30))
                .retryPolicy(RetryPolicy.builder()
                    .numRetries(3)
                    .build())
                .build();

            cloudWatchClient = CloudWatchClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .overrideConfiguration(clientConfig)
                .build();

            Instant endTime = Instant.now();
            Instant startTime = endTime.minusSeconds(300); // 5 minutes ago

            Dimension dbInstanceDimension = Dimension.builder()
                .name("DBInstanceIdentifier")
                .value(dbInstanceIdentifier)
                .build();

            GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                .namespace("AWS/RDS")
                .metricName("CPUUtilization")
                .dimensions(dbInstanceDimension)
                .startTime(startTime)
                .endTime(endTime)
                .period(60) // 1 minute periods
                .statistics(Statistic.AVERAGE)
                .build();

            System.out.println("Requesting CPU metrics from CloudWatch...");
            GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);

            if (response.datapoints().size() > 0) {
                // Get the most recent datapoint
                Datapoint latestDatapoint = response.datapoints().stream()
                    .max((d1, d2) -> d1.timestamp().compareTo(d2.timestamp()))
                    .orElse(null);

                if (latestDatapoint != null) {
                    cpuUtilization = latestDatapoint.average();
                    System.out.println("✓ Successfully retrieved CPU utilization: " + cpuUtilization + "%");
                    System.out.println("  Timestamp: " + latestDatapoint.timestamp());
                } else {
                    System.out.println("○ No datapoints found in response");
                }
            } else {
                System.out.println("○ No CPU utilization data found for RDS instance: " + dbInstanceIdentifier);
                System.out.println("  This might be normal if the instance is new or has no recent activity");
            }

        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            if (e.getCause() instanceof java.net.UnknownHostException) {
                System.err.println("✗ DNS resolution failed for CloudWatch endpoint");
                System.err.println("  Region: " + awsRegion + ", Instance: " + dbInstanceIdentifier);
                System.err.println("  Check network connectivity and DNS configuration");
            } else {
                System.err.println("✗ AWS SDK error: " + e.getMessage());
            }
        } catch (software.amazon.awssdk.services.cloudwatch.model.CloudWatchException e) {
            System.err.println("✗ CloudWatch service error: " + e.awsErrorDetails().errorMessage());
            System.err.println("  Check permissions and RDS instance identifier");
        } catch (Exception e) {
            System.err.println("✗ Unexpected error getting RDS CPU utilization");
            e.printStackTrace();
        } finally {
            if (cloudWatchClient != null) {
                try {
                    cloudWatchClient.close();
                } catch (Exception e) {
                    System.err.println("Warning: Error closing CloudWatch client: " + e.getMessage());
                }
            }
        }

        return cpuUtilization;
    }
}

