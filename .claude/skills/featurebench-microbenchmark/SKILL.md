---
name: featurebench-microbenchmark
description: End-to-end creation of new FeatureBench microbenchmarks for YugabyteDB's benchbase regression pipelines. It turns a plain-language requirement into an approved plan, then picks the category and next file name, writes the yugabyte YAML with variance-safe generators and bindings, and derives the postgres / yb_colocated / yugabyte_range variants. It lints and smoke-tests them, pushes a branch, and hands off to the perf-devops trigger-nightly-test skill (plus PerfStudio RDS for Postgres) to run them on a universe and check the results. Use it whenever the user wants to add, design, extend, port or run a microbenchmark, featurebench workload, or config/yugabyte/regression_pipelines YAML. That includes measuring or regression-testing the performance of a DB feature or query shape (scans, joins, indexes, aggregates, order by, foreign keys, DDL, locking, writes, vector, throughput), even if FeatureBench is not named, e.g. "write a yaml that measures X" or "add a test for Y to nightly".
---

# FeatureBench microbenchmark author

This skill takes a requirement ("measure how X behaves when Y varies") to a set of reviewed, lint-clean YAMLs. It writes them in the right `config/yugabyte/regression_pipelines/<category>/<variant>/` dirs, commits them to a pushed branch, and runs them on real universes. The work has phases, and phase 1 ends at a gate: **don't write YAML before the user approves the plan, and don't create universes before the user approves the run.** Both steps are cheap to redo on paper and expensive to redo once files or universes exist.

Paths below are relative to the benchbase repo root. The helper is `python3 .claude/skills/featurebench-microbenchmark/scripts/fbtool.py` (call it `fbtool` below). It needs only PyYAML and never touches a database.

Reference files. Read the one a phase points to, when you reach that phase:
- `references/categories_naming_variants.md`: category choice, naming, which variants, header blocks, variant transforms, customTags vocabulary
- `references/utils.md`: every data generator, with verified semantics and a "which generator" cheat sheet
- `references/yaml_reference.md`: every key the parser reads, and the behaviours that break runs
- `references/variance.md`: how to keep results stable run to run (this is what nightly alerting depends on)
- `references/execution.md`: running on universes through perf-devops, the Postgres/RDS path, results, promotion to nightly
- `assets/template_yugabyte_latency.yaml`: the starting skeleton for a latency microbenchmark

## Phase 0: Understand the requirement

Pull these out of what the user said. Ask only for the gaps, in one short batch. Propose sensible defaults instead of asking open questions.

1. **Hypothesis:** the feature or behaviour under test, and the one dimension being varied (rows fetched, #columns, datatype, cardinality, #indexes, index type, sharding, contention…).
2. **Query shapes:** the exact SQL patterns, or a description precise enough to write them.
3. **Data:** tables, row counts, the cardinality of the filtered/joined columns, and the row size.
4. **Kind:** single-terminal latency (the default), multi-terminal contention, or throughput (`optimalThreads`).
5. **Variants:** postgres, yb_colocated and yugabyte_range, if applicable (see the reference §3).
6. **Execution:** the YB build to run on, whether to run now or only produce files, the branch name, and whether the run goes on nightly later.

Also look for overlap before designing anything new. Search the existing workloads (`grep -ril "<keyword>" config/yugabyte/regression_pipelines`) and tell the user if an existing file already covers the shape; extending that file may be the better answer. When the request is to *modify* an existing workload, the same phases apply. Keep file and workload names unchanged, bump `yaml_version`, and add `yaml_change_description` (see "Editing existing workloads").

## Phase 1: Plan (gate)

Write the plan to a markdown file, `plans/featurebench/<FILE_PREFIX>_<desc>.md` in the scratchpad or a temp dir (not the repo), then show it to the user. Use this structure. It later becomes the PR description.

```markdown
# <FILE_BASENAME>  (<category>)
Goal: <hypothesis in one sentence>. Varied dimension: <X>. Fixed: <everything else>.
Kind: latency | contention | throughput   Variants: yugabyte, postgres, yb_colocated[, yugabyte_range]  (omitted: <variant> because <reason>)
Similar existing workloads checked: <files> -> <why this is new / extension>

## Schema & data
| table | rows | PK / sharding | indexes | key columns: generator -> distinct values x rows each |

## Workloads (run in this order)
| # | workload name | SQL (with ?) | bindings (util, range) | expected rows/exec | customTags |

## Run settings
terminals / time_secs / warmup / analyze_on_all_tables / setAutoCommit / explain + pg_stat flags, with one-line reasons.

## Execution plan
branch <name>; tags YB-<x> / YB-COLOCATED-<x>; build <ver>; PG via PerfStudio "Featurebench With RDS"; expected duration ≈ load + Σ(warmup+time_secs).

## Risks / open questions
```

Design rules for the plan:
- Follow `references/variance.md`: deterministic generators for any column a predicate touches, `?` bindings instead of literals, and fixed-width ranges via `referenceName` + `ExpressionEval`.
- Bind ranges must stay inside the loaded range. **Compute the expected rows per execution** for every query; this is what catches zero-row and wrong-selectivity mistakes early.
- Keep one variable per file. The workloads in a file should form a curve along the varied dimension.
- Estimate the runtime. The workloads run serially, each for warmup + time_secs, and the load of 1M rows × wide rows takes minutes. Keep one file under ~1 h unless the user wants longer.

Stop here and wait for the user to approve or change the plan.

## Phase 2: Categorise and name

Run `fbtool next-id <category>`. It prints the naming pattern, the next free number (max+1; gaps are never reused), the nightly tag, and the variant matrix.
- Pick the category from the "primary thing measured" table in `references/categories_naming_variants.md` §1.
- File: `<PREFIX><N>_<desc>.yaml`. Workloads: `<PREFIX><N>_<k>_<descriptor>`, identical in every variant. Tables: prefixed with the file prefix.
- A new category needs perf-devops changes before nightly will pick it up. Say so in the plan.

## Phase 3: Write the yugabyte master copy

Start from `assets/template_yugabyte_latency.yaml`, or from the closest sibling file in the chosen category (its header conventions win). Write `config/yugabyte/regression_pipelines/<category>/yugabyte/<FILE>.yaml`.

Rules that prevent the historical mistakes:
- **Header:** copy it exactly from `references/categories_naming_variants.md` §4.
  - Use `loaderThreads` in camelCase; the lowercase spelling is silently ignored.
  - Use only the `{{endpoint}}`, `{{username}}` and `{{password}}` placeholders.
  - New files start at `yaml_version: v1.0`.
- **create:** `DROP TABLE IF EXISTS` first, then the tables, then secondary indexes. Write PKs as constraints, `PRIMARY KEY(col ASC)` or `PRIMARY KEY((a) HASH, b ASC)`, so the variant transforms stay mechanical. `cleanup` mirrors every drop.
- **loadRules:** `table: <name>_` + `count: 1` gives `<name>_1`. Size PK generators to cover `rows`. Choose generators from the cheat sheet in `references/utils.md`.
- **executeRules:**
  - Every `run` needs `name` plus an integer `weight`.
  - The `?` count must equal the number of generated values.
  - Cast json, vector and array parameters in SQL (`?::jsonb`).
  - Give every workload `customTags` from the vocabulary.
  - Mark setup or mutation steps `skipReport: true`.
  - Only statements that legitimately return 0 rows get `zeroRowsValidation: false`.
  - Statements that can't be EXPLAINed (ANALYZE, DDL, CALL) need `raw_sql: true` or a file-level `disable_explain: true`.
- Add a comment header (`#TEST FOR YUGABYTE REGULAR TABLES`, `#GOAL…`, `#TESTS:`) that states the hypothesis and the data layout.
- If no util fits, see the end of `references/utils.md` for how to add one, including the Readme row.

The full list of keys is in `references/yaml_reference.md`. If you use anything beyond the template, check it there. A misspelled key does nothing, silently.

## Phase 4: Derive the variants

```bash
fbtool variants config/yugabyte/regression_pipelines/<category>/yugabyte/<FILE>.yaml --to postgres,yb_colocated     # dry run: diff + notes
fbtool variants ... --to postgres,yb_colocated[,yugabyte_range] --write
```

What the command does:
- Rewrites headers (type, driver, url, createdb) and drops YB-only flags.
- Converts PKs and indexes: HASH/ASC out for PG, all-ASC for range, and inline PK to a trailing constraint.
- Strips SPLIT clauses and sets `schematype` in customTags.

Then act on every `REVIEW:` line it prints (YB-only GUCs, plan-forcing settings for PG throughput files, hash-specific tests in colocated) and read the generated files. Workload names, row counts, bind ranges, time and warmup must be identical across variants; only engine-specific syntax may differ. Omit a variant when the plan says so, e.g. a hash-sharding test has no colocated copy.

## Phase 5: Validate

```bash
fbtool lint config/yugabyte/regression_pipelines/<category>/*/<FILE>.yaml
```
- **Errors** must be fixed. They cover wrong variant headers, unknown keys, weight or rate problems, `?`/binding count mismatches, unknown utils, PK ranges smaller than rows, non-EXPLAINable statements, and workload names that differ across variants.
- **Warnings** must be fixed or explicitly justified to the user. They cover random load generators on filter columns, literal predicates, bind ranges outside the loaded range, short runs, missing customTags, and unprefixed table names.
- Also lint the whole category dir once, so you know which warnings are pre-existing and which are yours.

**Local smoke test (recommended when a local YB or PG is reachable, e.g. `yugabyted start`).** It proves the SQL, the utils and the row counts before anything costs money:
```bash
fbtool smoke config/.../yugabyte/<FILE>.yaml --out /tmp/<FILE>.smoke.yaml --endpoint 127.0.0.1 --time 20 --warmup 5
# then run the mvn/java command it prints; for a quick run shrink `rows` in the smoke copy only
```
Check each workload's `Completed Transactions` (> 0), `Zero Rows` (0) and `Unexpected SQL Errors` (0), and compare EXPLAIN row counts with the plan's expected rows. Never commit smoke copies or shortened times. The uncommitted `time_secs: 60` in JOING5 is exactly this kind of leftover.

## Phase 6: Commit and push

Work on a feature branch, `ytyagi/<topic>` style. If the user is on `main`, create one. Stage **only** the new or changed workload files (and any new util or Readme row), and leave unrelated local changes alone. Show the diff summary, then commit with a message like `Adding <thing> microbenchmarks`. **Ask before pushing**; pushing publishes the branch to `yugabyte/benchbase`. The remote runner can only see pushed commits, so verify with `git ls-remote origin <branch>`.

## Phase 7: Run on a universe

Read `references/execution.md`, then the perf-devops `skills/trigger-nightly-test/SKILL.md` it points to, and follow them.
1. For each YB variant, `plan` in direct mode with `--set yaml-relative-path=<path(s)> --set benchbase-repo-branch=<branch> --test-user ytyagi --build <ver>`, using the `YB-<tag>` / `YB-COLOCATED-<tag>` row.
2. `submit <plan>` without `--yes` is a dry run. Show the user the summary: tag, template, build, user, YAML paths, and whether the universe will be deleted.
3. Only after an explicit go for that specific submission, run `submit <plan> --yes`. It creates a universe and costs money. One confirmation per submission; never retry a POST blindly.
4. For Postgres, give the user the PerfStudio "Featurebench With RDS" instructions and the exact extras to paste.
5. Track progress with `status <id>`. Report the test ids and dashboard links.

## Phase 8: Check results and close the loop

- Pull per-workload metrics (the SQL is in `references/execution.md` §3) and check them against the plan:
  - every workload present
  - zero rows and errors at 0
  - EXPLAIN rows equal to the expected rows
  - the latency trend across the varied dimension matching the hypothesis

  An unexpected trend usually means the YAML isn't measuring what the plan says. Revisit the YAML before blaming the DB.
- On failure, use perf-devops `triage-test-failure`. To compare against nightly or check for a regression, use `perf-regression-analysis`. For a variance check across identical universes, use `run_comparison.py` (`references/variance.md`).
- Next steps to offer:
  - open the PR, with the plan as the body
  - after the merge, nightly picks up files in existing category dirs automatically
  - the `announcing-new-benchmarks` skill (`.cursor/skills/`) drafts the team message

## Editing existing workloads

The phases are the same, with these additions:
- keep the file and workload names
- change all variants together (#182 is a fix for a copy that was updated in only one variant)
- bump `yaml_version`: v1.x → v1.(x+1) for fixes, v2.0 when results aren't comparable with history
- add a `yaml_change_description`, and optionally `workload_version` / `workload_change_description` per workload

## Guardrails

- No universe creation, `--yes`, push, or PR without the user's explicit OK for that action.
- Never run as `ptest-user`, and never print passwords from payload rows or configs. The perf-devops scripts redact them; keep it that way.
- Don't edit `modifyyaml.py`, the Java framework, or unrelated workloads as a side effect. If the framework blocks the test (e.g. an util bug), propose the fix separately.
- Remember that everything under `regression_pipelines/` runs nightly once merged.
