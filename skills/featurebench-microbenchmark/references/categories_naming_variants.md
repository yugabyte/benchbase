# Categories, naming, DB variants, customTags

## Contents
1. Choosing a category
2. File and workload naming
3. Which DB variants to produce
4. Header blocks per variant
5. What changes between variants (the transform rules)
6. customTags vocabulary

## 1. Choosing a category

Pick the category by the **primary thing being measured**. Don't go by the SQL keywords that happen to appear: a join test that sorts its output is still a join test.

| Category dir | Put it here when the test measures… | Nightly tag / universe |
|---|---|---|
| `scan_workloads` | PK/secondary range scans, point lookups, full table scans, IN-lists, bitmap scans, wide rows, tablet count | YB-scan_workload · 3az leadAff |
| `Index_workloads` | index choice and behaviour: pkey vs secondary index, composite key parts, hash vs range index, data cardinality through an index | YB-index_workload · 3az leadAff |
| `aggregate_workloads` | COUNT/SUM/MAX/DISTINCT/GROUP BY, pushdown, aggregate cardinality | YB-aggregate_workload · 3az leadAff |
| `orderby_workloads` | ORDER BY on pk/skey/non-indexed columns, backward scans, datatype impact on sort | YB-orderby_workload · 3az leadAff |
| `join_workloads` | join strategies (NL/hash/merge/batched NL), join predicates, cascaded joins, join + group/order | YB-join_workload · 3az leadAff |
| `foreign_key` | FK constraint overhead on child/parent DML | YB-hash_/range_foreign_key_workload, YB-COLOCATED-foreign_key_workload · 3az leadAff |
| `conditional_workloads` | INSERT … ON CONFLICT, SELECT FOR UPDATE, conditional DML | YB-conditional_workload · 1az |
| `write_workloads` | INSERT/UPDATE cost vs #indexes, #columns, #rows, backfill | YB-write_workload · 1az |
| `range_write_workloads` | multi-terminal (24) writes on range tables | no nightly row |
| `locking_semantics` | contention and isolation behaviour (RC, row locks) | YB-locking_semantics_workload · 1az |
| `skiplocked_workloads` | SKIP LOCKED queues, read-ahead GUC | YB-skiplocked_workload (yugabyte only) · 1az |
| `ddl_workloads` | CREATE/DROP/ALTER latency (`raw_sql` + `executeNtimes`) | YB-ddl_workload (no colocated) · 1az |
| `miscellaneous` | multi-statement txns, triggers, sequences, retention, ANALYZE | YB-miscellaneous_workload · 1az |
| `vector_workloads` | pgvector / ybhnsw insert, search, update, delete | YB-vector_workload (yugabyte only) · 1az |
| `throughput_next_workloads` / `throughput_seek_workloads` / `throughput_write_workloads` | max throughput with `optimalThreads` for next-style scans / seek-style lookups / writes | YB-throughput_*_workload (yugabyte + postgres dirs) · 1az |
| `bulkload` | COPY bulk load (code-driven Goal classes) | YB-bulkload · 1az |
| `perfstudio` | YAMLs run by PerfStudio pipelines (MG1 analyze) | PerfStudio only |

If nothing fits, propose a **new category** in the plan: a new dir with its own variant sub-dirs. Tell the user that nightly won't pick it up until perf-devops registers it. The perf-devops `add-featurebench-workload` skill covers that; it is outside this skill.

## 2. Naming

Run `python3 skills/featurebench-microbenchmark/scripts/fbtool.py next-id <category>`. It prints the prefixes in use, the highest number, and the next free id.

- **New files take max+1**, never a gap. Gaps come from deleted files (scanG1/4/6, AGGRG1, ORDG1, INDG9), and reusing a number would merge the new test's history with a dead one.

| Category | File pattern | Example | Current next (Sep 2026, re-check with next-id) |
|---|---|---|---|
| Index_workloads | `INDG<N>_U_<desc>` | `INDG13_U_compare_index_scan` | INDG16_U_ |
| aggregate_workloads | `AGGRG<N>_<desc>` | `AGGRG10_aggregate_cardinality` | AGGRG11_ |
| conditional_workloads | `CW<N>_<desc>` | `CW6_on_conflict_do_update_range` | CW7_ |
| ddl_workloads | `DDL_G<N>_<desc>` | `DDL_G1_basic_ops` | DDL_G2_ |
| foreign_key | `FK_G<N>_<desc>` | `FK_G4_varyingFKcardinality_parent_table_ops` | FK_G5_ |
| join_workloads | `JOING<N>_<desc>` | `JOING12_compate_indexscan_on_rangetbl` | JOING14_ |
| miscellaneous | `MG<N>_<desc>` | `MG3_multi_statement_json_trigger_index` | MG4_ |
| orderby_workloads | `ORDG<N>_<desc>` | `ORDG8_orderby_backwardscan_post_updates` | ORDG9_ |
| range_write_workloads | `RW_G<N>_<desc>` | `RW_G7_update_varying_pk_columns` | RW_G8_ |
| scan_workloads | `scanG<N>_<camelCaseDesc>` | `scanG13_wideSchema_fullTableScan` | scanG14_ |
| write_workloads | `insertG<N>_` / `updateG<N>_` | `updateG5_update_included_colm` | insertG4_ / updateG6_ |
| skiplocked | `SL_<desc>` | `SL_no_read_ahead_varying_limit` | – |
| throughput_* | `<dimension dir>/THRPT_<kind>_<desc>` | `THRPT_next_varying_range/THRPT_next_pk_btwn_100` | – |
| vector | `<op>_<dim>d_<desc>` | `search_512d_100k_l2_allfilters` | – |

- `<desc>` is snake_case (scan uses camelCase). It names the varied dimension: `_varying_rows`, `_increasingColumn`, `_data_cardinality`, `_hash_tbl_`, `_range_tbl_`.
- **Workload names:** `<PREFIX>_<k>_<descriptor>`, e.g. `AGGRG11_1_count_point_select`. They must be identical in every variant, because results are keyed by (file basename, workload name). **Never rename an existing file or workload**; that breaks trend history.
- **Table names** carry the file prefix, lowercase or matching the file's case (`aggrg10_pkeyBigint1M_1`, `CW3_accounts_1`). Every file runs on the same universe in sorted order, so unprefixed names like `rangetbl_1` collide between files (fixed in f56eedd0). End table names with `_<n>` so `table: x_` + `count: 1` works.
- **Header comment** block at the top of the file:
  ```
  #TEST FOR YUGABYTE REGULAR TABLES
  #GOAL…
  #TESTS:
  #    …
  ```
  Use `#TEST FOR POSTGRES` or `#TEST FOR YUGABYTE COLOCATED TABLES` in the other variants.

## 3. Which variants to produce

| Variant dir | Produce when | Skip when |
|---|---|---|
| `yugabyte/` | always. **Author this one first**; it is the master copy. | – |
| `postgres/` | almost always. It's the PG baseline for the YB-vs-PG summary. | the test uses YB-only features: tablet splits (`SPLIT INTO` in scanG10), YB GUCs (SL read_ahead), ybhnsw-specific behaviour |
| `yb_colocated/` | for read/write/latency categories that have a `YB-COLOCATED-*` row | the test is *about* hash sharding or tablet splits (INDG1/3/5/7, JOING1, ORDG7 are absent from colocated), or the category has no colocated row (ddl, skiplocked, throughput, vector) |
| `yugabyte_range/` | only when range vs hash sharding is the variable (foreign_key, perfstudio) | everywhere else |

Name every omitted variant, and the reason, in the plan.

## 4. Header blocks

**yugabyte/**, for latency microbenchmarks with 1 terminal:
```yaml
type: YUGABYTE
driver: com.yugabyte.Driver
url: jdbc:yugabytedb://{{endpoint}}:5433/yugabyte?sslmode=require&ApplicationName=featurebench&reWriteBatchedInserts=true&load-balance=false
username: {{username}}
password: {{password}}
batchsize: 128
isolation: TRANSACTION_REPEATABLE_READ
loaderThreads: 1
terminals: 1
collect_pg_stat_statements: true
use_dist_in_explain: true
yaml_version: v1.0
works:
    work:
        time_secs: 180
        active_terminals: 1
        rate: unlimited
        warmup: 60
```

**postgres/**: same, except:
```yaml
type: POSTGRES
driver: org.postgresql.Driver
url: jdbc:postgresql://{{endpoint}}:5432/postgres?sslmode=require&ApplicationName=featurebench&reWriteBatchedInserts=true
# no use_dist_in_explain, no createdb
```

**yb_colocated/**: the yugabyte header plus one line right after `url` (keep `load-balance=false`):
```yaml
createdb: drop database if exists yb_colocated; create database yb_colocated with colocated=true
```

**yugabyte_range/**: identical header to yugabyte.

**Throughput (optimalThreads) header**, from `throughput_write_workloads/yugabyte`, current form after #211 and #218:
```yaml
url: jdbc:yugabytedb://{{endpoint}}:5433/yugabyte?sslmode=require&ApplicationName=featurebench&reWriteBatchedInserts=true&load-balance=true
batchsize: 128
isolation: TRANSACTION_REPEATABLE_READ
loaderThreads: 4
terminals: 1
collect_pg_stat_statements: true
use_dist_in_explain: true
yaml_version: v1.0
targetCPU: 80
toleranceCPU: 5
optimalThreads: true
samplingTime: 180
useThroughputThreshold: true
restingTimeSecs: 360
works:
    work:
        time_secs: 900
        rate: unlimited
        warmup: 60
# NO active_terminals. INSERT keys via PrimaryIntGenThroughput. 10M-row base tables. Fixed-size TEXT instead of JSON.
```
The next/seek throughput files use `targetCPU: 75`, `samplingTime: 120`, `time_secs: 180`, and `batchsize: 1`. The PG next files add `flatMaxScalingSteps: 2`. Copy the header from the nearest sibling file rather than from memory.

**Multi-terminal contention** (range_write, locking, skiplocked): `terminals: 24` or `10`, no `active_terminals`, `setAutoCommit: false`, `time_secs: 120-300`, `warmup: 30-60`.

**Per-category norms:**
- foreign_key: `time_secs: 60`, `warmup: 30`.
- write_workloads: `time_secs: 60`, `warmup: 30`, `loaderThreads: 4`.
- ddl: `time_secs: 0` + per-workload `executeNtimes`.

## 5. Variant transforms

`fbtool.py variants <yugabyte.yaml> --to postgres,yb_colocated[,yugabyte_range] [--write]` applies these rules. Always read its `REVIEW:` notes.

**yugabyte → postgres**
- header: type, driver, url (see §4); drop `use_dist_in_explain` and `createdb`
- `PRIMARY KEY((a,b) HASH, c ASC)` → `PRIMARY KEY(a, b, c)`. ASC/DESC/HASH are not allowed in a PG PK constraint.
- index `(col HASH)` → `(col)`. ASC/DESC stay (they are valid in PG indexes).
- remove `SPLIT AT VALUES (...)` and `SPLIT INTO n TABLETS`
- `ybhnsw` → `hnsw`
- manual: remove or replace `yb_*` GUCs and `ALTER DATABASE yugabyte …`
- throughput/skiplocked: add `ALTER DATABASE postgres SET enable_seqscan=off;` and `… enable_bitmapscan=off;` to create, and `… TO DEFAULT` to cleanup, so PG uses the same index plan
- keep the customTags, workload names, rows, bind ranges, time and warmup **identical**

**yugabyte → yb_colocated**
- add the `createdb` line after `url`; `load-balance=false`
- customTags: `schematype=regular` → `schematype=colocated`
- HASH key parts → ASC; inline `id int primary key` → trailing `primary key(id asc)`; remove SPLIT clauses
- everything else identical

**yugabyte → yugabyte_range**
- every PK column explicitly `ASC` (keep DESC); HASH → ASC; inline PK moved to a trailing `primary key(col asc)`
- index columns `ASC`
- customTags: `partition=hash` → `range`

**Author the yugabyte master copy so the transforms stay mechanical:**
- write PKs as a constraint, `PRIMARY KEY(col ASC)` or `PRIMARY KEY((a) HASH, b ASC)`
- one statement per list item
- no YB-only syntax outside `create`

## 6. customTags vocabulary

One comma-separated `key=value` string per workload. Use only the existing vocabulary, because dashboards slice by these keys. Include `schematype` at minimum; latency categories tag every workload.

- `schematype` = regular | colocated
- `partition` = range | hash | joinmixed
- `pkey` = asc | composite | hash | single | range
- `skey` = composite | asc | desc | hash
- `cardinality` = single | multiple
- `projection` = indexed | all | aggregate | multiple | hashpkey | nonindexed | rangepkey | hashskey | rangeskey
- `filtercount` = one | two
- `filteron` = rangepkey | rangeskey | hashpkey | nonindexed | hashskey | mixedskey | mixedpkey | none
- `filtertype` = comparision (sic — keep the existing spelling) | pointselect | IN | subquery
- `filterother` = IN
- `queryshape` = scan | join | orderby | aggregate
- `joinon` = rangepkey | rangeskey | pkey | mixedskey | mixedpkey | hashpkey | nonindexed | hashskey
- `joinconditions` = one | two
- `jointables` = three
- `aggregate` = count | sum | max | distinct
- `aggregateon`, `groupby`
- `orderbyon` = rangeskey | rangepkey | rangepkeydesc | hashpkey | hashpkeydesc | hashskey | nonindexed
- `columndatatype` = bigint | varchar | uuid | float | date
- `indexed` = true
- `indexexpr` = none | lower | cast | arithmetic, the key expression of the index the workload reads. `none` marks the plain-column baseline paired with an expression workload
- `customer` = <n>, which flags a customer-reported pattern (ask the user)
- vector only: `dim`, `magnitude`, `distance` (l2|ip|cosine), `filter`, `top_k`, `m`, `ef_construction`, `operation`, `deletetype`, `column`

Example:
```
customTags: schematype=regular,partition=range,pkey=asc,cardinality=single,projection=indexed,filtercount=one,filteron=rangepkey,filtertype=comparision,queryshape=scan
```
