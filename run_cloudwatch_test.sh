#!/bin/bash

# CloudWatch Test Runner Script
# Usage: ./run_cloudwatch_test.sh <postgres-endpoint> <username> <password>

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== CloudWatch RDS Monitoring Test Setup ===${NC}"

# Check if Maven is available and dependencies are compiled
if [ ! -d "target/dependency" ]; then
    echo -e "${YELLOW}Setting up dependencies...${NC}"
    mvn dependency:copy-dependencies -DoutputDirectory=target/dependency -q
fi

# Get the classpath for AWS SDK and other dependencies
CLASSPATH="target/dependency/*:."

echo -e "${GREEN}✓ Dependencies ready${NC}"

# Check parameters
if [ $# -lt 3 ]; then
    echo -e "${RED}Error: Missing parameters${NC}"
    echo "Usage: $0 <postgres-endpoint> <username> <password>"
    echo ""
    echo "Example:"
    echo "  $0 mydb.cluster-xyz.us-east-1.rds.amazonaws.com:5432 postgres mypassword"
    echo ""
    echo "Required environment variables:"
    echo "  AWS_ACCESS_KEY_ID     - Your AWS access key"
    echo "  AWS_SECRET_ACCESS_KEY - Your AWS secret key"
    echo "  AWS_DEFAULT_REGION    - (optional) Default AWS region"
    exit 1
fi

POSTGRES_ENDPOINT="$1"
USERNAME="$2"
PASSWORD="$3"

echo -e "${BLUE}Parameters:${NC}"
echo "  PostgreSQL Endpoint: $POSTGRES_ENDPOINT"
echo "  Username: $USERNAME"
echo "  Password: $(printf '*%.0s' $(seq 1 ${#PASSWORD}))"
echo ""

# Check AWS environment variables
echo -e "${BLUE}Checking AWS environment...${NC}"
if [ -z "$AWS_ACCESS_KEY_ID" ]; then
    echo -e "${RED}✗ AWS_ACCESS_KEY_ID not set${NC}"
    echo "Please export your AWS credentials:"
    echo "  export AWS_ACCESS_KEY_ID=your_access_key"
    exit 1
else
    echo -e "${GREEN}✓ AWS_ACCESS_KEY_ID set${NC}"
fi

if [ -z "$AWS_SECRET_ACCESS_KEY" ]; then
    echo -e "${RED}✗ AWS_SECRET_ACCESS_KEY not set${NC}"
    echo "Please export your AWS credentials:"
    echo "  export AWS_SECRET_ACCESS_KEY=your_secret_key"
    exit 1
else
    echo -e "${GREEN}✓ AWS_SECRET_ACCESS_KEY set${NC}"
fi

if [ -z "$AWS_DEFAULT_REGION" ]; then
    echo -e "${YELLOW}○ AWS_DEFAULT_REGION not set (will use region from endpoint)${NC}"
else
    echo -e "${GREEN}✓ AWS_DEFAULT_REGION: $AWS_DEFAULT_REGION${NC}"
fi

echo ""

# Compile the test
echo -e "${BLUE}Compiling CloudWatch test...${NC}"
if javac -cp "$CLASSPATH" CloudWatchTest.java; then
    echo -e "${GREEN}✓ Compilation successful${NC}"
else
    echo -e "${RED}✗ Compilation failed${NC}"
    exit 1
fi

echo ""

# Run the test
echo -e "${BLUE}Running CloudWatch test...${NC}"
echo "================================================"

java -cp "$CLASSPATH" CloudWatchTest "$POSTGRES_ENDPOINT" "$USERNAME" "$PASSWORD"

echo "================================================"
echo -e "${GREEN}Test execution complete!${NC}"

# Cleanup
echo ""
echo -e "${BLUE}Cleaning up...${NC}"
rm -f CloudWatchTest.class
echo -e "${GREEN}✓ Cleanup complete${NC}"
