#!/bin/bash
# Vector Workload Quick Start Script - VectorDBBench Style
# This script runs all vector regression tests following VectorDBBench naming conventions

set -e

# Configuration
BENCHBASE_JAR="${BENCHBASE_JAR:-./benchbase-yugabyte/benchbase.jar}"
CONFIG_DIR="${CONFIG_DIR:-./config/yugabyte/regression_pipelines/vector_workloads}"
DB_TYPE="${DB_TYPE:-yugabyte}"  # Options: yugabyte, postgres, yb_colocated
ENDPOINT="${ENDPOINT:-localhost}"
USERNAME="${USERNAME:-yugabyte}"
PASSWORD="${PASSWORD:-yugabyte}"
RESULTS_DIR="${RESULTS_DIR:-./results/vector_workloads}"

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "=========================================="
echo "Vector Workload Regression Tests"
echo "VectorDBBench Naming Convention"
echo "=========================================="
echo "Database Type: $DB_TYPE"
echo "Endpoint: $ENDPOINT"
echo "Results Directory: $RESULTS_DIR"
echo "=========================================="
echo ""

# Create results directory
mkdir -p "$RESULTS_DIR"

# Function to run a workload
run_workload() {
    local workload_file=$1
    local workload_name=$(basename "$workload_file" .yaml)
    
    echo -e "${YELLOW}Running: $workload_name${NC}"
    echo "Start time: $(date)"
    
    if java -jar "$BENCHBASE_JAR" \
        -b featurebench \
        -c "$CONFIG_DIR/$DB_TYPE/$workload_file" \
        --create=true \
        --load=true \
        --execute=true \
        --params "endpoint=$ENDPOINT,username=$USERNAME,password=$PASSWORD" \
        -d "$RESULTS_DIR/$workload_name" 2>&1 | tee "$RESULTS_DIR/${workload_name}.log"; then
        echo -e "${GREEN}✓ Completed: $workload_name${NC}"
        echo "End time: $(date)"
        echo ""
        return 0
    else
        echo -e "${RED}✗ Failed: $workload_name${NC}"
        echo "Check log at: $RESULTS_DIR/${workload_name}.log"
        echo ""
        return 1
    fi
}

# VectorDBBench-style workloads organized by category
# Combined workloads test multiple filter types (nofilter, 1percentile, 99percentile) in one run
echo -e "${BLUE}=== SEARCH WORKLOADS (with all filter types) ===${NC}"
SEARCH_WORKLOADS=(
    "search_128d_100k_l2_allfilters.yaml"
    "search_512d_100k_l2_allfilters.yaml"
    "search_1536d_50k_l2_nofilter.yaml"
    "search_512d_100k_cosine_nofilter.yaml"
)

echo -e "${BLUE}=== INSERT WORKLOADS ===${NC}"
INSERT_WORKLOADS=(
    "insert_512d_indexed_m16_ef200.yaml"
    "insert_768d_indexed_m16_ef200.yaml"
    "insert_1536d_indexed_m16_ef200.yaml"
)

echo -e "${BLUE}=== UPDATE WORKLOADS ===${NC}"
UPDATE_WORKLOADS=(
    "update_512d_embedding_indexed_m16_ef200.yaml"
    "update_768d_embedding_indexed_m16_ef200.yaml"
    "update_1536d_embedding_indexed_m16_ef200.yaml"
    "update_512d_embedding_cosine_indexed.yaml"
)

echo -e "${BLUE}=== DELETE WORKLOADS ===${NC}"
DELETE_WORKLOADS=(
    "delete_512d_single_indexed.yaml"
    "delete_768d_single_indexed.yaml"
    "delete_1536d_single_indexed.yaml"
)

ALL_WORKLOADS=("${SEARCH_WORKLOADS[@]}" "${INSERT_WORKLOADS[@]}" "${UPDATE_WORKLOADS[@]}" "${DELETE_WORKLOADS[@]}")

FAILED_WORKLOADS=()

# Run all workloads
echo -e "${BLUE}=== STARTING SEARCH TESTS ===${NC}"
echo "Note: Combined workloads test nofilter, 1percentile, and 99percentile filters"
for workload in "${SEARCH_WORKLOADS[@]}"; do
    if ! run_workload "$workload"; then
        FAILED_WORKLOADS+=("$workload")
    fi
done

echo -e "${BLUE}=== STARTING INSERT TESTS ===${NC}"
for workload in "${INSERT_WORKLOADS[@]}"; do
    if ! run_workload "$workload"; then
        FAILED_WORKLOADS+=("$workload")
    fi
done

echo -e "${BLUE}=== STARTING UPDATE TESTS ===${NC}"
for workload in "${UPDATE_WORKLOADS[@]}"; do
    if ! run_workload "$workload"; then
        FAILED_WORKLOADS+=("$workload")
    fi
done

echo -e "${BLUE}=== STARTING DELETE TESTS ===${NC}"
for workload in "${DELETE_WORKLOADS[@]}"; do
    if ! run_workload "$workload"; then
        FAILED_WORKLOADS+=("$workload")
    fi
done

# Summary
echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo "Total workload files: ${#ALL_WORKLOADS[@]}"
echo "Note: Combined workloads contain 3 sub-tests each"
echo "Passed: $((${#ALL_WORKLOADS[@]} - ${#FAILED_WORKLOADS[@]}))"
echo "Failed: ${#FAILED_WORKLOADS[@]}"

if [ ${#FAILED_WORKLOADS[@]} -gt 0 ]; then
    echo ""
    echo -e "${RED}Failed workloads:${NC}"
    for failed in "${FAILED_WORKLOADS[@]}"; do
        echo "  - $failed"
    done
    exit 1
else
    echo ""
    echo -e "${GREEN}All workloads completed successfully!${NC}"
    echo "Results available in: $RESULTS_DIR"
    exit 0
fi
