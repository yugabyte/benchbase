# FeatureBench YAML reference (what the code actually reads)

Keys are case-sensitive. A misspelled key is **silently ignored**, for example `loaderthreads` or `-afterLoad`, so `fbtool.py lint` rejects unknown keys. Line references are to `src/main/java/com/oltpbenchmark/`.

## Contents
1. Top level
2. works/work
3. microbenchmark/properties
4. loadRules
5. executeRules → run → queries → bindings
6. Behaviours that bite
7. Code-driven mode (when YAML is not enough)

## 1. Top level

| Key | Default | Notes |
|---|---|---|
| `type` | required | `YUGABYTE` or `POSTGRES`. Controls the use_dist check and PG-only stats collection. |
| `driver` | required | `com.yugabyte.Driver` / `org.postgresql.Driver` |
| `url` | required | Use `{{endpoint}}`. The db in the path must be `yugabyte` or `postgres`, because the createdb rewrite relies on that. |
| `username`, `password` | required | Always `{{username}}` / `{{password}}`. No other `{{x}}` tokens are allowed: pipelines pass only these three plus endpoint, and a local `-p` run fails on unknown tokens. |
| `createdb` | – | **Top level**, not under properties. It runs before `create`; the db name is parsed out and the URL rewritten to it. The regex needs a space after the name. Colocated form: `drop database if exists yb_colocated; create database yb_colocated with colocated=true`. |
| `batchsize` | 128 | Loader batch size. |
| `isolation` | **TRANSACTION_SERIALIZABLE** | Pipelines use `TRANSACTION_REPEATABLE_READ`. Any value outside the four `TRANSACTION_*` names is silently ignored. |
| `terminals` | 0 | Worker threads. |
| `loaderThreads` | #cores | **camelCase only.** The pool size for per-table loaders (one thread per loadRules table). |
| `collect_pg_stat_statements` | false | Resets pg_stat_statements before MEASURE and snapshots it in tearDown. Standard: `true`. |
| `use_dist_in_explain` | false | Adds `dist,debug` to EXPLAIN. **Throws on POSTGRES.** Standard `true` on YB variants. |
| `disable_explain` | false | Skips the pre-run EXPLAIN. Set it (or `raw_sql` on the workload) when a query can't be EXPLAINed (ANALYZE, DDL, CALL…); otherwise the EXPLAIN pass fails quietly (§6.1). |
| `force_capture_explain_analyze` | false | EXPLAIN ANALYZE for INSERT/DELETE too, which **executes them 4×**. |
| `analyze_on_all_tables` | false | **Not used in these microbenchmarks; don't add it.** (It issues a YB-only `ALTER DATABASE … SET yb_enable_optimizer_statistics`, which fails on Postgres.) |
| `yaml_version` | 1.0 | Metadata. New files use `v1.0`. **Bump it on every behavioural edit** (v1.0→v1.1 for small fixes, v2.0 for changes that break comparability). |
| `yaml_change_description` | "" | One line explaining the bump. |
| `optimalThreads` | false | Thread-scaling search. Only one workload per invocation (the pipeline runs `--workloads` one at a time). |
| `targetCPU`, `toleranceCPU`, `samplingTime`, `restingTimeSecs`, `flatMaxScalingSteps`, `useThroughputThreshold`, `scalingMinDeltaPercent`, `threadIncrement`, `linearPGthread`, `truncateBetweenIterations` | 80, 5, 0, 120, 2, false, 5, 3, false, false | optimalThreads tuning. Copy these from an existing throughput file. |
| `retries` | 3 | Retry count for retryable SQL errors. |
| `newConnectionPerTxn` | false | Also disables pg_stat collection. |
| `time` (top level) | – | **Dead.** Never read. Don't add it. |

## 2. `works: work:`

| Key | Default | Notes |
|---|---|---|
| `time_secs` | 0 | MEASURE seconds. It must be > 0 unless `serial` or `executeNtimes` is set. FeatureBench reads `time_secs`, **not** `time`. A per-workload `time_secs` overrides it. |
| `warmup` | 0 | Warmup seconds, not recorded. Cannot be overridden per workload. |
| `rate` | **required** | `unlimited` (always, in pipelines). If it's missing, the run exits. |
| `active_terminals` | = terminals | Latency files set it to 1. **Throughput/optimalThreads files must not set it** (#218). |
| `serial` | false | Runs each txn type once, in order (bulkload). |
| `executeNtimes` | 0 | Exact transaction count. Requires `terminals: 1`. Usually set per workload. |

## 3. `microbenchmark:`

```yaml
microbenchmark:
    class: com.oltpbenchmark.benchmarks.featurebench.customworkload.YBDefaultMicroBenchmark
    properties: {...}
```

| properties key | Notes |
|---|---|
| `setAutoCommit` | **Defaults to false** in FeatureBench. With false, every query in one `run` entry is a single transaction, committed by the framework. Pipelines set `true` unless the test is about multi-statement transactions or locking (FK, locking_semantics, skiplocked, MG2). |
| `create` | List of SQL statements, run once on `--create=true`. Start with `DROP TABLE IF EXISTS` for every table. Create secondary indexes here unless the test is about post-load index builds. Any error aborts the run. |
| `loadRules` | See §4. Required whenever `--load=true` ("Empty Load Rules" otherwise). |
| `afterLoad` | DDL run once, after all loader threads finish. Used for FK constraints and backfilled indexes (FK, scanG13). **It never runs if any loader thread failed.** |
| `executeRules` | See §5. |
| `executeOnce` | A YAML block shaped like executeRules. Its queries run once, serially, without bindings, timed as one transaction. Only used when there are **no** executeRules (e.g. insertG3's index backfill). |
| `cleanup` | Runs on `--cleanup=true`. Mirror every DROP (and reset any `ALTER DATABASE … SET`). |
| `execute` | `true` means call the custom class's `execute()`. Code-driven only. |

## 4. `loadRules`

```yaml
loadRules:
    - table: scang14_tbl_        # + count: 2  -> scang14_tbl_1, scang14_tbl_2 (same rule cloned)
      count: 1
      rows: 1000000
      columns:
          - name: col_bigint_    # + count: 5 -> col_bigint_1..col_bigint_5 (independent instances)
            count: 5
            util: CyclicSeqIntGen
            params: [1, 1000000]
```
- A loader builds `INSERT INTO t (listed cols) VALUES (?, …)`. Columns that aren't listed get their defaults (serial, identity, default now()).
- `table: "a, b"` loads identical data into two tables.
- One thread loads each table. Table count × rows drives load time.
- Loader-thread exceptions are logged, not fatal. A failed load can look like a success, so check the row counts.

## 5. `executeRules`

```yaml
executeRules:
    - workload: SCANG14_1_desc           # results folder + --workloads name; identical across variants
      customTags: schematype=regular,...  # key=value list for dashboards
      time_secs: 300                      # optional per-workload override
      skipReport: true                    # optional: setup/mutation step, hidden from reports
      zeroRowsValidation: false           # optional: statements that legitimately affect 0 rows
      raw_sql: true                       # optional: textual ? substitution (DDL with generated identifiers)
      executeNtimes: 500                  # optional: exact count, terminals must be 1
      workload_version: 1.1               # optional per-workload metadata (#216)
      workload_change_description: "..."
      run:
          - name: point_lookup           # required
            weight: 100                  # required integer; relative within the workload
            queries:                     # run in order; one transaction when autocommit is off
                - query: select * from t where k = ?
                  count: 1               # optional: execute N times per transaction (latency = sum)
                  pattern_count: 10      # optional: expands [(?,?)][pattern_count] into 10 tuples
                  explain-plan-rc-validation: 1000   # optional: assert EXPLAIN actual rows
                  bindings:
                      - util: RandomInt
                        params: [1, 1000000]
                        count: 1
                        referenceName: k1
```
- **Workloads run serially** in file order on the same data. Create and load run once, before the first workload. Mutations carry over, so a later workload sees earlier inserts, updates and deletes. The standard "before / mutate / after" shape (ORDG8) is: read workloads, then `perform_update` workloads with `skipReport: true`, then the `post_update` read workloads.
- Weights: selection is `nextInt(sum)+1` over cumulative weights. Pipelines use a single run of weight 100, except for mixed/contention tests (e.g. 20/80).
- The number of `?` must equal the number of generated values, after `count`, `split_min_max_for_count` and `pattern_count` expansion. `ExpressionEval` fills one `?`.
- The execute phase binds with `setObject`, so the Java type of the util's value decides the SQL type. Cast explicitly in SQL for json, vector and array columns.
- A query whose text contains `*/` has its type taken from the word after the hint comment. Hint-prefixed queries still work.

## 6. Behaviours that bite

1. **Pre-run EXPLAIN executes queries, and its failures are silent.** Unless `disable_explain: true`, the first worker EXPLAINs every query 4× before warmup. SELECT and UPDATE use `EXPLAIN (ANALYZE…)`, **so UPDATEs really run 4 times**; plain INSERT and DELETE use plain EXPLAIN.
   - If a statement can't be EXPLAINed (ANALYZE, CREATE, DROP, ALTER, VACUUM, CALL, DO, SET), the server error inside `runExplainAnalyse` is rethrown and caught in `initialize()` (`FeatureBenchWorker.java:253`), which only calls `printStackTrace()`. **The workload still runs**, but EXPLAIN output is lost for that query **and every query after it** in the file (the loop stops).
   - Missing EXPLAIN rows in `detailed.json` mean you should look for a stack trace near "Running explain" in the log. `explain-plan-rc-validation` on the affected queries then has nothing to check.
   - Use `raw_sql: true` on such workloads (it skips EXPLAIN for them) or `disable_explain: true` for the file.
2. **Zero rows count as failure.** A SELECT with 0 rows, or an UPDATE/DELETE affecting 0 rows, makes the transaction ZERO_ROWS. If a workload has no successful transactions, the process exits(1). Keep bind ranges inside the loaded key space, and set `zeroRowsValidation: false` only for statements that legitimately return nothing (ANALYZE, DDL).
3. **Sequential generators throw when exhausted** (PrimaryIntGen, RandomUniqueIntGen, PrimaryIntGenThroughput). In the execute phase this kills the worker. Size INSERT key ranges generously.
4. **PrimaryIntGen slices overlap** by 1-2 keys between workers. Multi-terminal inserts then hit duplicate keys. Use PrimaryIntGenThroughput instead.
5. **`createdb` URL rewrite:** the URL must contain `/yugabyte` (YB) or `/postgres` (PG). The colocated db must be named `yb_colocated`, because the pipeline's Ansible looks up the colocation tablet leader by that name and silently skips the step otherwise.
6. **Jinja:** a raw pipeline YAML is not valid YAML until `{{…}}` is rendered. `fbtool.py` handles this.
7. **The loader typecast index bug:** put array/json/vector columns before any `count`-ed column.
8. **Reserved words:** the loader auto-quotes a column named `order`; queries must quote it themselves.
9. **optimalThreads + more than one workload** in a plain local run exits. The pipeline runs workloads one at a time, so this only matters for local runs.
10. **`--load=true` with no loadRules** exits. Configs that populate data inside `create` must run with `--load=false`.

## 7. Code-driven mode

Use a Java subclass of `YBMicroBenchmark` (in `customworkload/`, with `class:` pointing at it and `properties` holding any custom keys) only when YAML can't express the test:
- COPY or bulk load from CSV (bulkload Goal1-4)
- client-side `addBatch` inside a measured transaction
- read-then-write control flow, or conditional aborts
- multiple result sets
- stateful setup across transactions

Everything else fits in YAML with YBDefaultMicroBenchmark:
- DDL: `raw_sql` + `executeNtimes`
- run-once statements: `executeOnce`
- post-load DDL: `afterLoad`
- coupled parameters: `ExpressionEval`
- multi-row inserts: `pattern_count`

When writing a class:
- Override the **2-arg** `executeOnce(Connection, BenchmarkModule)`. The 1-arg overload is never called.
- Set the `*Implemented` flags in the constructor.
- Omit the YAML `create` key if you want `create(conn)` to run. It runs during `--load`.
- Set `setAutoCommit` explicitly.
