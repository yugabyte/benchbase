# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

This is YugabyteDB's fork of [CMU BenchBase](https://github.com/cmu-db/benchbase) (formerly OLTPBench), a multi-DBMS SQL benchmarking framework that drives variable-rate, variable-mixture load against any JDBC database. Beyond the upstream benchmarks (TPC-C, TPC-H, YCSB, AuctionMark, etc.), this fork adds two major YugabyteDB-specific features:

- **FeatureBench** (`benchmarks/featurebench`) — a YAML-driven microbenchmark framework for building targeted workloads without writing Java. **This is the primary subsystem to understand in this fork** (see the dedicated section below).
- **perf-dataloader** (`benchmarks/dataloader`) — a tool that infers a table's schema from a live database and generates data to load into it. See `PERF-DATALOADER.md`.

Requires **Java 17+** and **Maven 3.6+**.

## Build, run, test

The Maven **profile (`-P`)** selects both the JDBC driver dependency and the output artifact classifier. Supported profiles: `postgres`, `mysql`, `mariadb`, `sqlite`, `cockroachdb`, `phoenix`, `yugabyte`, `spanner`, `sqlserver`.

```bash
# Build a distribution for a profile → target/benchbase-<profile>.{tgz,zip}
./mvnw clean package -P yugabyte

# Convenience script: checks Java/Maven versions, builds the yugabyte profile
# (-DskipTests), then extracts the tgz into target/benchbase-yugabyte/
./build.sh

# Run tests (unit tests need no profile). CI runs `mvn -B test`.
./mvnw test

# Run a single test class or method
./mvnw test -Dtest=TestTPCCBenchmark
./mvnw test -Dtest=TestTPCCBenchmark#testGetProcedureNames
```

Running a benchmark (main class `com.oltpbenchmark.DBWorkload`):

```bash
# FeatureBench from source without packaging (note the -P profile)
mvn clean compile exec:java -P yugabyte -Dexec.args="-b featurebench -c config/yugabyte/sample_featurebench_config.yaml --create=true --load=true --execute=true"

# From an extracted distribution
java -jar benchbase.jar -b tpcc -c config/postgres/sample_tpcc_config.xml --create=true --load=true --execute=true

# perf-dataloader (uses the extracted distribution jar under the hood)
./perf-data-loader --config <config.yaml> --table-name <table> --rows <n>
```

Key CLI flags: `-b` benchmark name (comma-separated for composite runs like `tpcc,chbenchmark`), `-c` config file, the phase toggles `--create` / `--load` / `--execute` / `--clear` / `--cleanup`, and `--workloads` (FeatureBench: run only a named subset of `executeRules`). Run `java -jar benchbase.jar -h` for the full list.

Per-run results (latency/throughput CSVs, and for FeatureBench optionally `pg_stat_statements` and EXPLAIN output as JSON) are written to the `results/` folder.

## Framework architecture

**Entry point → plugin registry.** `DBWorkload.main()` parses the CLI and config, then consults `config/plugin.xml` to map a benchmark short-name (e.g. `tpcc`, `featurebench`, `perf-dataloader`) to its `BenchmarkModule` implementation class. **Adding a new benchmark requires an entry in `config/plugin.xml`.** DBWorkload then orchestrates the create → load → execute phases. FeatureBench and perf-dataloader are detected by name in `DBWorkload` and read **YAML** configs; all other benchmarks read **XML**.

**Core API (`com.oltpbenchmark.api`)** — every benchmark is built from these abstractions:
- `BenchmarkModule` — abstract base per benchmark. Concrete benchmarks implement `makeWorkersImpl()`, `makeLoaderImpl()`, and `getProcedurePackageImpl()`. `createDatabase()` runs the create phase.
- `Loader` / `LoaderThread` — populate the schema during the **load** phase; `createLoaderThreads()` returns the parallel loaders.
- `Worker<T extends BenchmarkModule>` — a `Runnable` that drives transactions during the **execute** phase; subclasses implement `executeWork(conn, txnType)` and may override `initialize()` / `tearDown()`.
- `Procedure` + `SQLStmt` — encapsulate one transaction's SQL. `StatementDialects` (and `api/dialects/`) supply per-DBMS SQL variants so one procedure can target multiple databases.
- `TransactionType(s)` — the weighted transaction mix a Worker samples from. Each transaction type maps to one unit of work.
- `api/collectors/` — per-DBMS collectors (`PostgresCollector`, `YuagbyteCollector`, `MySQLCollector`, `CockroachCollector`) that snapshot DB parameters/stats.

**Standard benchmark layout.** Each benchmark under `benchmarks/<name>/` follows the same pattern: a `<Name>Benchmark` (extends `BenchmarkModule`), a `<Name>Loader`, a `<Name>Worker`, and a `procedures/` package of `Procedure` subclasses. Configs read via commons-configuration2, templated with Jinjava.

## FeatureBench (primary subsystem)

FeatureBench turns a single YAML file into a create → load → execute benchmark run. `plugin.xml` maps `featurebench` → `FeatureBenchBenchmark`, which wires up three cooperating classes:

- **`FeatureBenchBenchmark`** — the `BenchmarkModule`. In `makeWorkersImpl(workcount)` it reads `microbenchmark/properties/executeRules[<workcount>]`, converts each rule's queries+bindings into `ExecuteRule`/`Query`/`UtilToMethod` objects (`configToExecuteRules`), and creates one `FeatureBenchWorker` per terminal. `makeLoaderImpl()` builds the `FeatureBenchLoader`.
- **`FeatureBenchLoader`** — runs create + load. `createLoaderThreads()` first runs the create phase and optional `beforeLoad`, then either delegates to a custom `loadOnce()` (`GeneratorOnce`) or auto-generates INSERT loaders from YAML `loadRules` (`GeneratorYaml`). Loading is batched (`batchsize` in config) and type-casts array/vector/json columns based on the util's class name (see `typeCastDataTypes`). `afterLoad` DDLs run once after all loader threads finish.
- **`FeatureBenchWorker`** — runs execute. `executeWork()` picks the `ExecuteRule` for the sampled `TransactionType`, generates fresh parameter values for each query via reflection, binds them to a cached `PreparedStatement`, and executes. Returns `ZERO_ROWS` if a query affected/returned no rows.

**Config shape** (`microbenchmark` block; keys under `properties`):

| Key | Purpose |
|---|---|
| `class` | Fully-qualified `YBMicroBenchmark` subclass. Default `customworkload.YBDefaultMicroBenchmark` (the YAML interpreter). |
| `properties.create` | List of DDL strings run in the create phase. |
| `properties.createdb` | DDL that creates a *database* (parsed for the db name; the JDBC URL is then rewritten to point at it). |
| `properties.loadRules` | Per-table row counts + column generators (see util functions). |
| `properties.executeRules` | Named workloads → weighted `run` entries → `queries` with `bindings`. |
| `properties.afterLoad` | DDL run once after load (e.g. create indexes post-load). |
| `properties.cleanup` | DDL run with `--cleanup`. |
| `properties.execute` | `true` → call the custom class's multithreaded `execute()` instead of `executeRules`. |
| `properties.setAutoCommit` | Set `false` when using code-driven `execute`/`executeOnce`. |

Top-level run knobs also read from the YAML: `terminals`, `works/work` (`time_secs`, `active_terminals`, `rate`, `warmup`), `batchsize`, `isolation`, `loaderthreads`, plus FeatureBench-specific instrumentation flags: `collect_pg_stat_statements`, `disable_explain`, `force_capture_explain_analyze`, `use_dist_in_explain` (Yugabyte only), `analyze_on_all_tables`, `optimalThreads`.

### Two authoring modes

1. **YAML-driven (declarative, the common case).** Leave `class` at `YBDefaultMicroBenchmark` and describe everything in `create` / `loadRules` / `executeRules`. No Java. This is what the great majority of `config/yugabyte/**` samples do.

2. **Code-driven.** Set `properties: {}` and point `class` at your own subclass of `YBMicroBenchmark` in `benchmarks/featurebench/customworkload/`. Override the lifecycle hooks:
   - `create(conn)`, `beforeLoad(conn)`, `loadOnce(conn)`, `afterLoad(conn)`, `cleanUp(conn)`
   - `executeOnce(conn)` — single-threaded, runs once during the MEASURE state; or
   - `execute(conn)` — multithreaded, one invocation per terminal per transaction.

   The base class exposes boolean flags (`loadOnceImplemented`, `executeOnceImplemented`, `beforeLoadImplemented`, `afterLoadImplemented`) that a subclass sets to signal which hooks it provides — the loader/worker branch on these. When using `execute`/`executeOnce`, set `setAutoCommit: false` in the config.

### loadRules → data generation

Each `loadRules` entry names a `table`, a `rows` count, and a list of `columns`. Every column has a `util` (generator name) and `params`. The loader builds one INSERT per table and, for each row, calls each util's `run()` to produce a bound value. Extra keys:
- `count` on a *loadRule* → clone the rule into `<table>1`, `<table>2`, … (also `table: "a,b"` splits into multiple tables).
- `count` on a *column* → repeat that column N times, suffixing the name (`name1`, `name2`, …).

### executeRules → transaction mix

Structure: `executeRules` is a list of **workloads**; each has a `workload` name and a `run` list. Each `run` entry is a named transaction with a `weight` and one or more `queries`. Each query has a SQL string (`?` placeholders) and a list of `bindings`, each binding a `util`+`params` that generates one parameter value per execution.

- **Multiple `executeRules` = multiple serial runs.** DBWorkload iterates workloads; each becomes its own set of `TransactionType`s and its own results sub-folder (named after `workload`, or a timestamp). `--workloads name1,name2` restricts which run.
- `weight` sets the relative frequency of each `run` entry within a workload (relative to the other runs' weights — they need not sum to 100).
- Query-level extras: `count` (execute the query N times per transaction), `pattern_count` (repeat the binding list N times, for large multi-row INSERTs / IN-lists), `explain-plan-rc-validation` (assert the EXPLAIN row count).
- Binding-level extras: `count` (repeat a binding N times), `split_min_max_for_count` (partition a `[min,max]` range into N disjoint sub-ranges — one binding each, for sharding key space across placeholders), `referenceName` + the `ExpressionEval` util (generate a value, name it, then compute a later binding from it — two-pass evaluation in `generateParameterValues`).

### Util functions (data generators)

Utils live in `benchmarks/featurebench/utils/` and are referenced **by simple class name** in YAML (`util: PrimaryIntGen`). `helpers/UtilToMethod` resolves the name to `com.oltpbenchmark.benchmarks.featurebench.utils.<name>` via reflection and constructs it with the YAML `params` — the load path uses the `(List params)` constructor; the execute path uses `(List params, int workerId, int totalWorkers)` so a generator can partition its key space per worker. Every util implements the `BaseUtil` interface (`Object run()`), returning the next value.

**Before adding a generator, consult the catalog in `src/main/java/com/oltpbenchmark/benchmarks/featurebench/Readme.md`** — it documents every util, its params, and their types. `workerhelpers/` (`ExecuteRule`, `Query`, `Bindings`) are the parsed YAML model objects; `helpers/` (`UtilToMethod`, `SimpleExpressionEvaluator`, `MD5hash`) back the reflection and expression machinery.

### Instrumentation

When `collect_pg_stat_statements: true`, the worker resets `pg_stat_statements` (and, on Postgres, `pg_stat_user_tables`) before MEASURE and snapshots them in `tearDown()`, matching each configured query to its stats row via cosine similarity of tokenized SQL. Unless `disable_explain: true`, it also runs `EXPLAIN (ANALYZE, …)` for each query and captures planning/execution time, peak memory, and row counts. All of this is written as JSON into `results/` alongside the standard latency/throughput CSVs (`FeaturebenchAdditionalResults`).

### Sample configs

`config/yugabyte/` is organized by feature: `demo/` holds canonical single examples of each capability (`featurebench_count_query.yaml`, `featurebench_explain.yaml`, `featurebench_after_load.yaml`, `featurebench_colocated.yaml`, …). Start from `sample_featurebench_config.yaml`.

### Regression pipelines (`config/yugabyte/regression_pipelines/`)

**YugabyteDB's nightly regression pipelines run the FeatureBench configs under `config/yugabyte/regression_pipelines/`.** Treat these as the maintained, run-in-CI workloads — edits here affect the nightly runs, and new comparative workloads belong here.

The directory is a two-level **`<category>/<db-variant>/<CONFIG>.yaml`** matrix:

- **Categories** (workload themes, one dir each): `aggregate_workloads`, `bulkload`, `conditional_workloads`, `foreign_key`, `Index_workloads`, `join_workloads`, `locking_semantics`, `miscellaneous`, `orderby_workloads`, `perfstudio`, `range_write_workloads`, `scan_workloads`, `vector_workloads`, `write_workloads`.
- **DB variants** (each category holds parallel sub-dirs; the *same* workload file name is duplicated across them so the identical workload can be compared across engines/layouts):
  - `postgres` — vanilla PostgreSQL (`type: POSTGRES`, `org.postgresql.Driver`, port 5432) — the baseline.
  - `yugabyte` — YugabyteDB default, hash-sharded (`type: YUGABYTE`, `com.yugabyte.Driver`, port 5433).
  - `yb_colocated` — YugabyteDB with colocated tables, via `createdb: ... create database ... with colocated=true`.
  - `yugabyte_range` — YugabyteDB with range-sharded primary keys (`primary key(... asc)`); present only where sharding layout is the variable under test (e.g. `foreign_key/`, `perfstudio/`).

  A workload may be absent from a variant when it doesn't apply (e.g. a hash-partitioned-table case has no `yb_colocated` version).

- **File naming**: `<CATEGORY-PREFIX><N>_<descriptive_name>.yaml`, prefix per category — `JOING1_…` (joins), `FK_G2_…` (foreign key), `MG1_…` (miscellaneous), etc.
- **Deeper nesting**: `bulkload/<variant>/goalN_<description>/<file>.yaml` groups configs by test "goal" (varying indexes, pre-vs-backfilled indexes, varying rows, varying columns).
- **Endpoints are templated** with `{{endpoint}}` (Jinjava) and filled in by the pipeline at run time.
