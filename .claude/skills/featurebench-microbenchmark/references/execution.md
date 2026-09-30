# Running new microbenchmarks on a universe (hand-off to perf-devops)

Nothing in benchbase creates a universe. Remote runs go through perfservice, which creates a universe on YBA portal **7302** from the payload row's template and deletes it after the test. The tooling lives in the **perf-devops** repo:
- `PERF_DEVOPS_REPO`, or by default `/Users/yashtyagi/VScode/perf_devops/perf-devops`.
- Its skills are in `skills/<name>/SKILL.md`. **Read the relevant SKILL.md before running its script**, because it is the source of truth and may have changed since this file was written. Run every script **from the perf-devops repo root**.
- To make those skills invocable by name from any repo, run `python3 skills/install.py claude-user` there (optional).

| Need | perf-devops skill | Script |
|---|---|---|
| Run YAML(s) on a YB or colocated universe | `trigger-nightly-test` | `skills/trigger-nightly-test/scripts/nightly_trigger.py` |
| Run the PG baseline on RDS, or a multi-step scenario | `perfstudio-pipeline-builder` (read-only here), plus the PerfStudio UI | `skills/perfstudio-pipeline-builder/scripts/ps_pipeline.py` |
| Test failed, stuck or empty | `triage-test-failure` | `skills/triage-test-failure/scripts/triage.py <test_id>` |
| Universe misbehaving | `debug-universe` | `skills/debug-universe/scripts/universe_info.py <test_id>` |
| Template or portal questions, leaked universes | `yba-portal-ops` | `skills/yba-portal-ops/scripts/yba_readonly.py` |
| Compare against nightly, regression verdict | `perf-regression-analysis` | perfservice SQL MCP |

Access needs the Yugabyte **VPN**. The scripts are stdlib Python.

**Run as the user's own test user** (`--test-user ytyagi`, or `export PERF_TEST_USER=ytyagi`), **never as `ptest-user`**. ptest-user runs feed nightly regression alerting and the plot page. Without the flag, the script derives the user from `git config user.email` (`yashtyagi`), which is not this user's historical id.

## 0. Preconditions (check before planning a run)

1. The YAMLs are committed and **pushed** to a branch on `origin` (`git@github.com:yugabyte/benchbase.git`). The runner VM clones `yugabyte/benchbase` at `benchbase-repo-branch` and builds it with `-P yugabyte`, so local files are invisible to it and Java changes on the branch are exercised too.
   ```bash
   git ls-remote origin <branch>          # must print a sha equal to `git rev-parse HEAD`
   ```
2. `fbtool.py lint` shows 0 errors.
3. There is a YB build to test on, e.g. `2.31.0.0-b500`. Ask the user if unsure; `nightly_trigger.py list` shows what nightly used.

## 1. YugabyteDB (`yugabyte/`) and colocated (`yb_colocated/`) runs

Variant → base payload row (tag):

| Variant | Tag |
|---|---|
| yugabyte | `YB-<tag base>`, e.g. `YB-scan_workload`, `YB-index_workload`, `YB-join_workload` |
| yb_colocated | `YB-COLOCATED-<tag base>` (only where it exists) |
| yugabyte_range | `YB-range_foreign_key_workload` (foreign_key only) |
| new category with no row | borrow a row with the right universe class: 3az leadAff (read-type) or 1az (write-type). The run then carries that tag. |

Confirm the tag with `nightly_trigger.py list --type FEATUREBENCH`. The tag base for each category is in `categories_naming_variants.md` §1, and `fbtool.py next-id` prints it too.

**Direct mode** is required for a YAML that hasn't run in nightly yet: `--yamls` only accepts file names from the latest completed nightly run of that tag.

```bash
cd "${PERF_DEVOPS_REPO:-/Users/yashtyagi/VScode/perf_devops/perf-devops}"
python3 skills/trigger-nightly-test/scripts/nightly_trigger.py plan \
  --type FEATUREBENCH --tag YB-scan_workload --build <yb-build> --test-user ytyagi \
  --set yaml-relative-path=config/yugabyte/regression_pipelines/scan_workloads/yugabyte/scanG14_x.yaml \
  --set benchbase-repo-branch=<pushed-branch> \
  --comment "<short purpose>"
# -> prints a plan summary and writes $TMPDIR/perf-devops-plans/nightly-plan-featurebench-<ts>.json
python3 skills/trigger-nightly-test/scripts/nightly_trigger.py submit <plan.json>          # DRY RUN: shows the requests
# only after the user explicitly says go:
python3 skills/trigger-nightly-test/scripts/nightly_trigger.py submit <plan.json> --yes    # creates a universe (costs money)
python3 skills/trigger-nightly-test/scripts/nightly_trigger.py status <test_id>
```

Options:
- **Several files:** use a comma list of full relative paths, `--set yaml-relative-path=a.yaml,b.yaml`. They run in order on one universe. You can also give a directory, which runs every `*.yaml` in it, sorted.
- **Colocated:** the same command with `--tag YB-COLOCATED-<tag base>` and the `yb_colocated/` path. It's the same kind of universe; colocation comes from the YAML's `createdb`.
- **Iterating on the same universe:** add `--keep-universe`. The universe then costs money until it is deleted, and the cleanup cron removes it after 3 days unless it is tagged `perf_dnd`. Afterwards run `nightly_trigger.py cleanup <test_id>` (a dry run), then add `--yes` after the user OKs it.
- **Quick functional smoke:** add `--set config.time=60 --set config.warmup=0 --allow-new-keys`. That overrides every workload's duration, so the numbers are not comparable. Label the run with `--comment smoke`.
- **Dev YB builds** (`--pr`, `--branch-build`, `--diff`) only work in baseline mode, which cannot take a new YAML. Build first, then pass `--build <image tag>` in direct mode.
- Never retry a failed POST blindly. Check `status` and the UI first.
- If a direct-mode test ends FAILED/NOT_FOUND, the universe may linger. Use `cleanup <id>`.

**Universe the tags create** (portal 7302, us-west-2, 3 nodes, RF3, m6i.2xlarge, 250 GB gp3, TLS). The client VM is c5.xlarge.
- `portal7302_aws_3node_3az_m6i_2xl_leadAff_2a_secure`: 3 AZs with leaders in 2a. Used by the aggregate, index, join, orderby, scan and foreign_key tags.
- `portal7302_m6i_8core_3node_1az_secure`: 1 AZ. Used by everything else.

## 2. Postgres (`postgres/`) runs

PG is not a nightly suite. PG baselines are one-off runs of the PerfStudio pipeline **"Featurebench With RDS"** (id 117, category Sanity Check):

1. Create DB: RDS postgres, `db.m6i.2xlarge`, gp3.
2. Run Query.
3. Run Benchmark.
4. Delete DB.

`nightly_trigger.py` can't launch it, and `ps_pipeline.py submit --run` fails because the pipeline has no `steps_to_update`. **The practical path is the PerfStudio UI.** Give the user exactly:

- Open PerfStudio → "Featurebench With RDS" → Run.
- Run Benchmark step extras:
  - `yaml-relative-path = config/yugabyte/regression_pipelines/<category>/postgres/<file>.yaml`. A comma list or directory also works.
  - `benchbase-repo-branch = <pushed-branch>`
  - optionally `client-instance-type = c5.4xlarge` for throughput
- Leave the yaml_config_parameters as they are (`endpoint={{1,endpoint}}`, username/password from the pipeline).

You can inspect the live pipeline read-only: `python3 skills/perfstudio-pipeline-builder/scripts/ps_pipeline.py show --live 117`. To make a run the *official* PG baseline for the YB-vs-PG summary, it has to be relabelled as `ptest-user` + `PG-<tag>`. That is a manual perf-team action, so tell the user to ask the perf team.

## 3. Results and follow-up

- Status and links: `nightly_trigger.py status <id>` shows the Jenkins build, universe and Grafana.
- UI (`https://perf.dev.yugabyte.com`):
  - `/dashboard/detail/<id>`
  - `/dashboard/compare/<mine>/<nightly>`: compare against the latest ptest-user run of the same tag and build family
  - `/plot-microbenchmark-yb/branch/master/50`: nightly trend; ptest-user runs only, so the user's own runs won't appear
  - `/summary_view/<branch>/<db_type>`: YB vs PG
- SQL (perfservice MCP, read-only). Per-workload metrics for a test:
  ```sql
  SELECT m.yaml_name, kv.key AS workload,
         (kv.val->'Summary'->>'Throughput (requests/second)')::numeric AS tps,
         (kv.val->'Summary'->'Latency Distribution'->>'Average Latency (microseconds)')::numeric/1000 AS avg_ms,
         (kv.val->'Summary'->'Latency Distribution'->>'99th Percentile Latency (microseconds)')::numeric/1000 AS p99_ms
  FROM service_test t JOIN service_microbenchmarkoutput m ON m.test_id_id = t.test_id,
       jsonb_each(m.output_json) kv(key, val)
  WHERE t.test_id = <id> AND jsonb_typeof(m.output_json) = 'object'
  ORDER BY 1, 2;
  ```
- **Sanity checks on a finished run.** Report these to the user:
  1. Every workload has results. A missing workload usually means it hit zero completed transactions, a bad util name, or an EXPLAIN failure.
  2. `Zero Rows` and `Unexpected SQL Errors` are 0.
  3. The row counts in EXPLAIN match the plan's expected selectivity.
  4. The latency ordering across workloads matches the hypothesis. If it doesn't, suspect the YAML before you suspect the DB.
- On failure, use `triage.py <id>`. Common FeatureBench silent failures:
  - an undefined `{{var}}` renders as empty
  - the colocated DB is not named `yb_colocated`
  - a `--yamls` mismatch deletes every YAML, and the run "passes" empty
  - the load partially failed, so afterLoad never ran

## 4. Promotion to nightly

- **Existing category directory:** merging the files to benchbase `main` is enough. The nightly rows point at the directory, and the next run picks the file up. Open a PR titled `Adding <thing> (#NNN)`; it gets squash-merged.
  - The PR body is the plan: goal, workloads, variants and reasons, and the run links.
  - After the merge, the `announcing-new-benchmarks` skill (in `.cursor/skills/`) drafts the team announcement.
- **New category or tag:** it also needs perf-devops changes (see `categories_naming_variants.md` §1). Hand the user that checklist; don't attempt it silently.
