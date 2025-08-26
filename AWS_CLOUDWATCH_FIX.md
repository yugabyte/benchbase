# AWS CloudWatch DNS Resolution Fix

## Issue Summary
The application was failing with `UnknownHostException` when trying to connect to AWS CloudWatch for RDS monitoring. The error occurred at line 1323 in `DBWorkload.java` when calling `getRDSCPUUtilization()`.

## Root Causes Identified
1. **DNS Resolution Failure**: The AWS SDK was trying to resolve `monitoring.rds.amazonaws.com` but this hostname was not resolving in the network environment
2. **Network Connectivity**: The specific CloudWatch endpoint was not accessible from the current EC2 instance
3. **Missing Error Handling**: The original code didn't gracefully handle network connectivity issues
4. **AWS SDK Configuration**: The client configuration was not robust enough for unreliable network conditions

## Fixes Implemented

### 1. Enhanced AWS CloudWatch Client Configuration
- Added explicit timeout configurations (60s API timeout, 30s attempt timeout)
- Added retry policy with 3 retry attempts
- Improved credential provider chain using `DefaultCredentialsProvider`

### 2. Comprehensive Error Handling
- Added specific handling for `UnknownHostException` with helpful error messages
- Added separate handling for `CloudWatchException` (permissions/service errors)
- Added proper resource cleanup in finally blocks

### 3. Connectivity Testing
- Added `testCloudWatchConnectivity()` method to test AWS connectivity before monitoring
- Pre-flight check warns users if CloudWatch is not accessible

### 4. Configuration Options
- Added system property `benchbase.skip.cloudwatch` to disable CloudWatch monitoring entirely
- Graceful fallback when CloudWatch is unavailable

## Usage Options

### Option 1: Run with CloudWatch disabled (Recommended if connectivity issues persist)
```bash
java -Dbenchbase.skip.cloudwatch=true -jar benchbase.jar ...
```

### Option 2: Run with improved error handling (Default)
```bash
java -jar benchbase.jar ...
```
The application will now:
- Test CloudWatch connectivity first
- Provide better error messages if connectivity fails
- Continue execution with default thread counts if monitoring fails

### Option 3: Debug network issues
Check the logs for specific error messages:
- DNS resolution failures: Check network connectivity and DNS configuration
- Permission errors: Verify AWS credentials and IAM permissions
- Service errors: Check RDS instance identifier and region settings

## Network Troubleshooting
If CloudWatch connectivity issues persist:

1. **Check DNS Resolution**:
   ```bash
   ping monitoring.us-west-2.amazonaws.com  # Replace with your region
   ```

2. **Verify AWS Credentials**:
   ```bash
   aws sts get-caller-identity
   ```

3. **Test CloudWatch Access**:
   ```bash
   aws cloudwatch list-metrics --namespace AWS/RDS --region us-west-2
   ```

4. **Check VPC Configuration** (if running in private subnet):
    - Ensure NAT Gateway or Internet Gateway is configured
    - Check Security Groups allow outbound HTTPS traffic
    - Verify Route Tables have proper routes to internet

## Required IAM Permissions
The application requires these CloudWatch permissions:
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "cloudwatch:GetMetricStatistics",
                "cloudwatch:ListMetrics"
            ],
            "Resource": "*"
        }
    ]
}
```

## Testing the Fix
1. Compile the project: `mvn compile`
2. Run with CloudWatch enabled to test connectivity
3. If issues persist, use `-Dbenchbase.skip.cloudwatch=true` to bypass CloudWatch monitoring
