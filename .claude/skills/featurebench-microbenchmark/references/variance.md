# Variance-safe microbenchmark design

These rules come from the latency-variance work: PR #214 (`c107b3f9`), the throughput fixes (#206, #211, #218), and the zero-row fixes (#181, #187).

Nightly regression alerting compares each run against the trend of previous runs. Any run-to-run noise the YAML introduces either hides a real regression or raises a false alarm. The aim is that **two runs on identical universes and builds produce the same data, the same plans and the same key-access pattern.**

## Rules

1. **Load deterministic data.**
   - Use `PrimaryIntGen` for ids and `CyclicSeqIntGen` for columns you filter, join, group or sort on. Use `RandomUniqueCyclicIntGen [lo, hi, period]` when you need "K values × M rows each".
   - `RandomNumber`, `RandomInt` and `OneNumberFromArray` with long literal lists produce a different histogram on every load, so plans and row counts drift between runs.
   - Random fillers are fine for payload columns that no predicate touches. Keep them **fixed length** (`RandomAString [N, N]`) so row size is constant.

   ```diff
   -  util: RandomInt                 # card column, random per load
   -  params: [1000001, 1010001]      # (and off-by-one)
   +  util: CyclicSeqIntGen           # exactly 10000 distinct values, 100 rows each
   +  params: [1000001, 1010000]
   ```
2. **Parameterise predicates.** Don't hard-code a literal: `where k = 1000498` hits one cached key, one tablet and one plan forever. Use `?` with a binding over the loaded domain. The SQL text stays constant, so pg_stat_statements still matches it.
   - IN-lists: `IN (?,?,?,?,?)` with `bindings: {count: 5, util: RandomInt, params: [lo, hi]}`.
   - Range width must be constant: `between ? and ?` with `RandomInt [lo, hi-W] referenceName: s` and then `ExpressionEval "s + W"`. The number of scanned rows then never varies.
3. **Keep bind ranges inside the loaded range.** A value outside it gives a zero-row transaction; enough of them fail the whole run, and a mix of hits and misses doubles the variance.
   - An UPDATE or DELETE must target existing keys. Use `CyclicSeqIntGen [1, N]` so each key is touched evenly.
   - Execute-phase INSERTs must use a key range disjoint from the loaded one, and large enough for the whole run.
4. **Pin the selectivity you're testing.** Put the expected row count in the plan: "returns 100 rows", "scans 1% of the table". With a deterministic load you can compute it exactly. Where it's cheap, assert it with `explain-plan-rc-validation: <rows>`, which requires `collect_pg_stat_statements: true`.
5. **Run long enough.** Latency files use `time_secs: 180` and `warmup: 60`; older 120/30 files are being moved up. Heavy queries of 100 ms or more per execution need enough executions: aim for **at least 1,000 measured transactions** per workload, or raise the per-workload `time_secs`.
6. **Stable statistics.** Set `analyze_on_all_tables: true` on YB (and `ANALYZE t…;` in `afterLoad` on PG) whenever the plan choice depends on stats: joins, aggregates, index choice.
7. **One variable per file.** Within one file, workloads should differ only in the dimension under test (rows fetched, #columns, datatype, index kind). Everything else stays fixed: schema, row count, projection. The file then reads as a curve.
8. **Isolate mutations.** Workloads run in order on shared data. Put reads before writes, or reload into separate tables. Mark helper or mutation steps with `skipReport: true`. Remember that EXPLAIN ANALYZE runs each UPDATE 4× before warmup.
9. **No duplicates.** Check that an existing workload doesn't already measure the same shape. #191 and #197 deleted about 80 redundant files.
10. **Throughput files:**
    - `load-balance=true` and `optimalThreads: true`, with no `active_terminals`.
    - Insert keys from `PrimaryIntGenThroughput`.
    - Fixed-size `TEXT` payloads instead of JSON, and a 10M-row base table so the working set doesn't fit in cache trivially.

## Tried and reverted (don't repeat these)

- Seeding `OneNumberFromArray` to make it deterministic. Cyclic generators are simpler.
- A new `SeqStringFromArray` util. `CyclicSeqStringGen` covers it.
- An afterLoad "pre-warm" `DO $$ … PERFORM * FROM t … $$`. It is being moved into the framework (the `ytyagi/prewarm_cache` branch, not merged). Don't add it per-YAML.
- Extra `split_id` columns just to shard work.

## How to check variance

1. **Statically:** `fbtool.py lint` flags random load generators on filter columns, literal predicates, bind ranges outside the load range, and short runs.
2. **On clusters:** the repo-root `run_comparison.py` (untracked; YB profile only) runs one YAML against N endpoints in parallel. It reports min/p50/p95/p99/max per workload and the worst/best P95 ratio. To measure variance, point it at 2-3 identical universes (or run it several times against one). A ratio close to 1.0 is the goal; 1.1 or more on a single-terminal latency test means the YAML still has a noise source.
   ```bash
   python3 run_comparison.py -c <yaml> -n <ip1> <ip2> -l runA runB -u yugabyte -P <pw> -o ./comparison_results
   ```
   Its introspection EXPLAIN substitutes `'1'` for every `?`, so read its plans loosely.
3. **Across nightly runs:** after the file has been on main for a while, look at the plot page trend, or query `service_microbenchmarkoutput` for the workload's coefficient of variation over the last N ptest-user runs. The `perf-regression-analysis` skill can do this.
