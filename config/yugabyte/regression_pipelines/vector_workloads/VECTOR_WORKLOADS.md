# Vector Workloads for YugabyteDB - VectorDBBench Compatible

Comprehensive vector benchmarking regression tests following [VectorDBBench](https://github.com/yugabyte/VectorDBBench) naming conventions with optimized combined filter testing.

## Table of Contents
- [Overview](#overview)
- [Naming Convention](#naming-convention)
- [Workload Files](#workload-files)
- [Combined Filter Design](#combined-filter-design)
- [Quick Start](#quick-start)
- [Utility Classes](#utility-classes)
- [Configuration Details](#configuration-details)
- [Performance Expectations](#performance-expectations)
- [Troubleshooting](#troubleshooting)

---

## Overview

This test suite provides reproducible vector performance benchmarks for YugabyteDB with:
- ✅ **VectorDBBench naming convention** for standardization
- ✅ **Combined filter workloads** for efficiency (3-in-1 testing)
- ✅ **Deterministic data generation** for reproducibility
- ✅ **Multiple dimensions** (512, 768, 1536) and distance metrics (L2, Cosine)
- ✅ **CRUD operation coverage** (Search, Insert, Update, Delete)
- ✅ **Simplified schema** (id + embedding only) for focused vector performance testing

### Key Optimization: Combined Filters

Filter variations (nofilter, 1percentile, 99percentile) are **combined into single YAML files**:
- **Old**: 3 separate files = 3× load time
- **New**: 1 file with 3 workloads = 1× load time, same coverage
- **Result**: 66% time savings on filter tests + perfect data consistency

---

## Naming Convention

Following VectorDBBench standards:

```
{operation}_{dimension}d_{magnitude}_{distance}_{filter}.yaml
```

**Components:**
- `operation`: search, insert, update, delete
- `dimension`: 128d, 512d, 1536d (vector dimensions)
- `magnitude`: 50k, 100k, 1m (data size)
- `distance`: l2, cosine, ip (distance metric)
- `filter`: allfilters (combined), nofilter (single), 1percentile, 99percentile
- `params`: indexed_m16_ef200, noindex (for insert/update/delete)

**Examples:**
- `search_512d_100k_l2_allfilters.yaml` - 3 workloads: nofilter, 1percentile, 99percentile
- `insert_512d_indexed_m16_ef200.yaml` - Insert with HNSW (m=16, ef_construction=200)
- `update_512d_embedding_indexed_m16_ef200.yaml` - Update vector column with index

---

## Workload Files

**Total: 30 files** (10 per database type: yugabyte, postgres, yb_colocated)

### Search Workloads (4 files = 8 test scenarios)

| File | Workloads | Dim | Rows | Distance | Description |
|------|-----------|-----|------|----------|-------------|
| `search_128d_100k_l2_allfilters.yaml` | **3** | 128 | 100k | L2 | No filter + 1% filter + 99% filter |
| `search_512d_100k_l2_allfilters.yaml` | **3** | 512 | 100k | L2 | No filter + 1% filter + 99% filter |
| `search_1536d_50k_l2_nofilter.yaml` | 1 | 1536 | 50k | L2 | OpenAI embedding size |
| `search_512d_100k_cosine_nofilter.yaml` | 1 | 512 | 100k | Cosine | Cosine similarity |

### Insert Workloads (3 files)

| File | Dim | Indexed | Description |
|------|-----|---------|-------------|
| `insert_512d_indexed_m16_ef200.yaml` | 512 | Yes | Insert with HNSW index (m=16, ef=200) |
| `insert_768d_indexed_m16_ef200.yaml` | 768 | Yes | Insert with HNSW index (m=16, ef=200) |
| `insert_1536d_indexed_m16_ef200.yaml` | 1536 | Yes | Insert with HNSW index (m=16, ef=200) |

**Note**: All inserts use `DeterministicVectorGen` with seed 42 for reproducible benchmarking.

### Update Workloads (1 file)

| File | Dim | Column | Description |
|------|-----|--------|-------------|
| `update_512d_embedding_indexed_m16_ef200.yaml` | 512 | embedding | Vector update triggers delete-then-insert in HNSW index |

**Note**: For HNSW indexes, updating embeddings is expensive as it triggers graph reconstruction. Uses `DeterministicVectorGen` with seed 43 for different vectors.

### Delete Workloads (3 files)

| File | Dim | Type | Description |
|------|-----|------|-------------|
| `delete_512d_single_indexed.yaml` | 512 | Single | Primary key deletion with HNSW index |
| `delete_768d_single_indexed.yaml` | 768 | Single | Primary key deletion with HNSW index |
| `delete_1536d_single_indexed.yaml` | 1536 | Single | Primary key deletion with HNSW index |

---

## Combined Filter Design

### Structure

Each `*_allfilters.yaml` file contains 3 executeRules workloads:

```yaml
executeRules:
    # Workload 1: Pure ANN search
    - workload: search_512d_100k_l2_nofilter
      run:
          - queries:
              - query: SELECT ... ORDER BY embedding <-> ? LIMIT 10

    # Workload 2: Selective filter (~1% of data)
    - workload: search_512d_100k_l2_1percentile
      run:
          - queries:
              - query: SELECT ... WHERE category = ? ORDER BY embedding <-> ? LIMIT 10

    # Workload 3: Broad filter (~99% of data)
    - workload: search_512d_100k_l2_99percentile
      run:
          - queries:
              - query: SELECT ... WHERE category <= ? ORDER BY embedding <-> ? LIMIT 10
```

### Filter Implementation

**No Filter**: Pure ANN search without WHERE clause

**1 Percentile** (~1% selectivity):
```yaml
loadRules:
    category: RandomNumber [1, 100]  # 100 distinct values
query: WHERE category = ?  # Each category ~1% of data
```

**99 Percentile** (~99% selectivity):
```yaml
loadRules:
    category: RandomNumber [1, 100]
query: WHERE category <= ?  # Returns 95-100% of data
bindings: SeededRandomNumber [95, 100, 99999]
```

### Benefits

1. **Load once, test thrice** - 66% faster than separate tests
2. **Identical data** for all filter comparisons
3. **Single file** to maintain per dimension/distance combo
4. **Consistent results** - eliminates load variance

---

## Quick Start

### Prerequisites

1. YugabyteDB with pgvector extension enabled
2. BenchBase compiled with vector utilities:
   ```bash
   mvn clean package -DskipTests
   ```

### Run All Tests

```bash
cd config/yugabyte/regression_pipelines/vector_workloads
./run_all_vector_tests.sh
```

### Run Single Workload

```bash
# Combined workload (3 filter tests)
java -jar benchbase-yugabyte/benchbase.jar -b featurebench \
    -c config/yugabyte/regression_pipelines/vector_workloads/yugabyte/search_512d_100k_l2_allfilters.yaml \
    --create=true --load=true --execute=true \
    --params "endpoint=localhost,username=yugabyte,password=yugabyte"

# Single workload
java -jar benchbase-yugabyte/benchbase.jar -b featurebench \
    -c config/yugabyte/regression_pipelines/vector_workloads/yugabyte/insert_512d_indexed_m16_ef200.yaml \
    --create=true --load=true --execute=true \
    --params "endpoint=localhost,username=yugabyte,password=yugabyte"
```

### Environment Variables

```bash
export BENCHBASE_JAR="./path/to/benchbase.jar"
export ENDPOINT="your-db-host"
export USERNAME="yugabyte"
export PASSWORD="your-password"
export DB_TYPE="yugabyte"  # or "postgres" or "yb_colocated"

./run_all_vector_tests.sh
```

---

## Utility Classes

Two custom utilities for vector benchmarking:

### 1. DeterministicVectorGen

Generates **reproducible** vectors using a seed for consistent benchmarking across runs.

**Usage:**
```yaml
bindings:
    - util: DeterministicVectorGen
      params: [512, -1.0, 1.0, 42]  # [dim, min, max, seed]
```

**Output:** `[0.123, -0.456, 0.789, ...]` (vector format)

**Key Feature:** Same seed = same vector every run (eliminates variance)

**Best Practices:**
- Use seed 42 for load phase data generation
- Use different seeds (e.g., 43) for update operations to generate distinct vectors
- Ensures reproducible benchmarks across test runs

### 2. SeededRandomNumber

Generates deterministic random numbers for consistent row selection.

**Usage:**
```yaml
bindings:
    - util: SeededRandomNumber
      params: [1, 100, 99999]  # [min, max, seed]
```

### Example: Fully Reproducible Test

```yaml
queries:
    - query: SELECT ... WHERE category = ? ORDER BY embedding <-> ? LIMIT 10
      count: 100
      bindings:
          # Query vector (always same)
          - util: DeterministicVectorGen
            params: [512, -1.0, 1.0, 54321]
          # Filter value (always same)
          - util: SeededRandomNumber
            params: [1, 100, 99999]
          # Sort vector (same as query)
          - util: DeterministicVectorGen
            params: [512, -1.0, 1.0, 54321]
```

### Schema Design

All workloads use a simplified schema focused on vector operations:

```sql
CREATE TABLE vector_table (
    id bigint PRIMARY KEY,
    embedding vector(N)
);
```

**Benefits:**
- Pure vector performance measurement without metadata overhead
- Deterministic embeddings using `DeterministicVectorGen`
- Consistent results across test runs
- Simplified troubleshooting

---

## Configuration Details

### HNSW Index Parameters

All indexed workloads use production-ready parameters:

```sql
CREATE INDEX USING ybhnsw (embedding vector_l2_ops) 
WITH (m=16, ef_construction=200);
```

- **m=16**: Standard connectivity (balances recall and speed)
- **ef_construction=200**: High-quality index build
- Matches VectorDBBench defaults

### Data Sizes

| Dimension | Rows | Table Size | Load Time |
|-----------|------|------------|-----------|
| 128d | 100,000 | ~50MB | ~30s |
| 512d | 100,000 | ~200MB | ~60s |
| 1536d | 50,000 | ~300MB | ~90s |

### Execution Settings

All workloads use:
- **Warmup**: 30 seconds
- **Execution**: 120 seconds per workload
- **Terminals**: 1 (single client)
- **Rate**: Unlimited
- **Batch Size**: 128
- **Queries**: 100 iterations per workload

### Combined Workload Runtime

```
search_512d_100k_l2_allfilters.yaml:
├─ Load phase: ~60 seconds
├─ Workload 1 (nofilter): 30s warmup + 120s execution
├─ Workload 2 (1percentile): 30s warmup + 120s execution  
└─ Workload 3 (99percentile): 30s warmup + 120s execution
Total: ~8.5 minutes (vs 15+ minutes for 3 separate files)
```

---

## Performance Expectations

### Search Latency (P95)

| Configuration | Expected Latency | Throughput | Index Usage |
|---------------|-----------------|------------|-------------|
| 128d, nofilter | < 5ms | > 1000 QPS | HNSW Scan |
| 512d, nofilter | < 10ms | > 500 QPS | HNSW Scan |
| 1536d, nofilter | < 20ms | > 200 QPS | HNSW Scan |
| 512d, 1% filter | < 15ms | > 400 QPS | HNSW + Filter |
| 512d, 99% filter | < 50ms | > 100 QPS | May use Seq Scan |

### Write Operations

| Operation | Configuration | Expected Latency | Notes |
|-----------|--------------|------------------|-------|
| Insert | With HNSW (512d) | < 10ms | HNSW index overhead |
| Insert | With HNSW (768d) | < 12ms | Larger vectors |
| Insert | With HNSW (1536d) | < 20ms | Large embeddings (OpenAI) |
| Update | Embedding (512d) | < 15ms | Delete-then-insert in HNSW |
| Delete | Primary key | < 3ms | Fast PK delete |

**HNSW Update Behavior**: When updating embeddings with HNSW indexes, the database performs a delete-then-insert operation, which requires graph reconstruction. This is significantly more expensive than non-indexed updates.

### Regression Criteria

Alert if metrics show:
- > 10% drop in throughput
- > 15% increase in P95 latency
- > 20% increase in P99 latency
- HNSW index not being used when expected

---

### Troubleshooting

### Common Issues

**1. Vector format error**
```
ERROR: Vector contents must start with "["
```
**Fix**: Ensure using `DeterministicVectorGen` (generates `[...]` format)

**2. Dimension mismatch**
```
ERROR: column "embedding" is of type vector(512) but expression is of type vector(128)
```
**Fix**: Ensure vector dimension in CREATE TABLE matches utility parameters and query casts.

**3. Index method not found**
```
ERROR: unrecognized index method "ybhnsw"
```
**Fix**: For PostgreSQL, use `hnsw` instead of `ybhnsw`. Check postgres/ directory files.

**4. Combined workload results**
**Expected**: Results appear in separate subdirectories:
```
results/search_512d_100k_l2_allfilters/
├── search_512d_100k_l2_nofilter/
├── search_512d_100k_l2_1percentile/
└── search_512d_100k_l2_99percentile/
```

### Verification Steps

1. **Check vector format**: Vectors should be `[val1,val2,...]` with `DeterministicVectorGen`
2. **Verify index creation**: Run `\d+ tablename` to see indexes
3. **Check EXPLAIN plans**: Ensure HNSW index scans are used
4. **Monitor pg_stat_statements**: Verify queries are hitting the database
5. **Test reproducibility**: Run workload twice - metrics should be nearly identical

---

## Directory Structure

```
vector_workloads/
├── VECTOR_WORKLOADS.md (this file)
├── run_all_vector_tests.sh
├── yugabyte/ (11 files)
│   ├── search_128d_100k_l2_allfilters.yaml
│   ├── search_512d_100k_l2_allfilters.yaml
│   ├── search_1536d_50k_l2_nofilter.yaml
│   ├── search_512d_100k_cosine_nofilter.yaml
│   ├── insert_512d_indexed_m16_ef200.yaml
│   ├── insert_768d_indexed_m16_ef200.yaml
│   ├── insert_1536d_indexed_m16_ef200.yaml
│   ├── update_512d_embedding_indexed_m16_ef200.yaml
│   ├── delete_512d_single_indexed.yaml
│   ├── delete_768d_single_indexed.yaml
│   └── delete_1536d_single_indexed.yaml
├── postgres/ (same 11 files, PostgreSQL config)
└── yb_colocated/ (same 11 files, colocated tables)
```

---

## Adding Custom Workloads

### Add New Dimension

```bash
# Copy existing
cp insert_512d_indexed_m16_ef200.yaml insert_768d_indexed_m16_ef200.yaml

# Update in new file:
# 1. Table names: vector_insert_768d_indexed
# 2. Vector types: vector(768)
# 3. Utility params: DeterministicVectorGen [768, -1.0, 1.0, 42]
# 4. Workload names: insert_768d_indexed_m16_ef200
# 5. Seeds: Use consistent seed (42) for reproducibility
```

### Add New Distance Metric

```yaml
# For Inner Product
create:
    - CREATE INDEX USING ybhnsw (embedding vector_ip_ops) ...
queries:
    - query: SELECT ... ORDER BY embedding <#> ?::vector(512) LIMIT 10
```

### Schema Design Notes

All workloads use simplified schema:
```sql
CREATE TABLE table_name (
    id bigint PRIMARY KEY,
    embedding vector(N)
);
```

**Benefits:**
- Pure vector performance without metadata overhead
- Deterministic embeddings with `DeterministicVectorGen`
- Simplified troubleshooting and reproducibility

---

## CI/CD Integration

### Example Pipeline

```yaml
vector_regression_tests:
  stage: nightly
  script:
    - cd config/yugabyte/regression_pipelines/vector_workloads
    - ./run_all_vector_tests.sh
  artifacts:
    paths:
      - results/vector_workloads/
    expire_in: 7 days
  only:
    - schedules
```

### Metrics Collection

Track over time:
- Throughput (ops/sec) per workload
- P50, P95, P99 latencies
- Index scan vs sequential scan ratios
- Resource usage (CPU, memory, I/O)

Store in time-series DB (Prometheus, InfluxDB) and alert on regressions.

---

## VectorDBBench Comparison

| Feature | VectorDBBench | This Implementation |
|---------|---------------|---------------------|
| Naming Convention | ✓ Standard | ✓ **Followed** |
| Filter Types | nofilter, 1%, 99% | ✓ **Same** |
| HNSW Parameters | m=16, ef=200 | ✓ **Same** |
| Dimensions | 128, 512, 768, 1536 | ✓ **512, 768, 1536** |
| Distance Metrics | L2, Cosine, IP | ✓ L2, Cosine |
| **Combined Filters** | ❌ Separate | ✓ **3-in-1** |
| Reproducibility | Limited | ✓ **Deterministic** |
| Schema Design | Multiple columns | ✓ **id + embedding only** |
| Framework | Python | BenchBase/Java |

**Key Advantages**: 
- Combined filter workloads reduce test time by ~66%
- Deterministic vectors ensure reproducible benchmarks
- Simplified schema focuses on pure vector performance

---

## References

- [VectorDBBench](https://github.com/yugabyte/VectorDBBench) - Naming convention source
- [pgvector](https://github.com/pgvector/pgvector) - PostgreSQL vector extension
- [HNSW Paper](https://arxiv.org/abs/1603.09320) - Algorithm details
- [YugabyteDB Docs](https://docs.yugabyte.com/) - Vector features

---

## Summary

**Files**: 11 per database type (33 total)  
**Test Scenarios**: 14 unique tests (8 search + 3 insert + 1 update + 3 delete)  
**Optimization**: Combined filters save 66% time on filter testing  
**Reproducibility**: Fully deterministic with `DeterministicVectorGen` (seed 42)  
**Schema**: Simplified to id + embedding for focused vector benchmarking  
**Convention**: VectorDBBench compatible  
**Status**: Production ready ✅

### Key Changes from Previous Version
- ✅ Removed noindex insert workload (not critical for HNSW testing)
- ✅ Added 768d and 1536d dimension workloads for inserts and deletes
- ✅ Removed metadata-only update workload (not relevant for HNSW)
- ✅ Simplified schema to id + embedding columns only
- ✅ Using `DeterministicVectorGen` for all embeddings (reproducible benchmarks)
- ✅ Update workloads now test HNSW delete-then-insert behavior

---

**Version**: 3.0  
**Last Updated**: 2026-01-29  
**Maintained By**: YugabyteDB Performance Team
