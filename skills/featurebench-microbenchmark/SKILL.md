---
name: featurebench-microbenchmark
description: Creates new FeatureBench microbenchmarks for YugabyteDB's benchbase regression pipelines. It turns a plain-language requirement into an approved plan, picks the category and next file name, writes the yugabyte YAML, derives the postgres / yb_colocated / yugabyte_range variants, and lints them. It stops once the lint-clean files are in config/yugabyte/regression_pipelines and does not run them, commit, push or open a PR. Use it whenever the user wants to add, design, extend or port a microbenchmark, featurebench workload, or regression_pipelines YAML. That includes performance tests for a DB feature or query shape (scans, joins, indexes, aggregates, order by, foreign keys, DDL, locking, writes, vector, throughput), even if FeatureBench is not named, e.g. "write a yaml that measures X" or "add a microbenchmark for Y".
---

# FeatureBench microbenchmark author

This skill takes a requirement ("measure how X behaves when Y varies") to a set of reviewed, lint-clean YAMLs in the right `config/yugabyte/regression_pipelines/<category>/<variant>/` dirs. It has three steps:

1. **Plan** (gate)
2. **Write the yugabyte YAML**
3. **Derive the variants and lint**

**Don't write YAML before the user approves the plan.** A plan is cheap to change; four variant files are not.

**Scope ends at the files.** Don't run the YAMLs against any database or universe, and don't commit, push or open a PR. When the files are done, list them and stop. If the user asks for any of those later, treat it as a separate request.

Paths are relative to the benchbase repo root. The helper is `python3 skills/featurebench-microbenchmark/scripts/fbtool.py` (called `fbtool` below). It needs only PyYAML and never touches a database.

Reference files. Read the one a step points to, when you reach that step:
- `references/categories_naming_variants.md`: category choice, naming, which variants, header blocks, variant transforms, customTags vocabulary
- `references/utils.md`: every data generator, with verified semantics and a "which generator" cheat sheet
- `references/yaml_reference.md`: every key the parser reads, and the behaviours that bite
- `assets/template_yugabyte_latency.yaml`: the starting skeleton for a latency microbenchmark

## Step 1: Plan (gate)

### Understand the requirement

Pull these out of what the user said. Ask only for the gaps, in one short batch, and propose sensible defaults instead of asking open questions.
1. **Hypothesis:** the feature or behaviour under test, and the one dimension being varied (rows fetched, #columns, datatype, cardinality, #indexes, index type, sharding, contention…).
2. **Query shapes:** the exact SQL patterns, or a description precise enough to write them.
3. **Data:** tables, row counts, the cardinality of the filtered/joined columns, and the row size.
4. **Kind:** single-terminal latency (the default), multi-terminal contention, or throughput (`optimalThreads`).
5. **Variants:** postgres, yb_colocated and yugabyte_range, where they apply (reference §3).

Look for overlap before designing anything new. Search the existing workloads (`grep -ril "<keyword>" config/yugabyte/regression_pipelines`) and tell the user if a file already covers the shape; extending it may be the better answer.

### Categorise and name

- Pick the category from the "primary thing measured" table in `references/categories_naming_variants.md` §1.
- Run `fbtool next-id <category>`. It prints the naming pattern, the next free number (max+1; gaps are never reused) and the variant matrix.
- File: `<PREFIX><N>_<desc>.yaml`. Workloads: `<PREFIX><N>_<k>_<descriptor>`, identical in every variant. Tables: prefixed with the file prefix.
- If nothing fits, propose a new category and say that it needs nightly-pipeline changes before it will run there.

### Write the plan

Write the plan to a markdown file in the scratchpad or a temp dir (not the repo), then show it to the user:

```markdown
# <FILE_BASENAME>  (<category>)
Goal: <hypothesis in one sentence>. Varied dimension: <X>. Fixed: <everything else>.
Kind: latency | contention | throughput   Variants: yugabyte, postgres, yb_colocated[, yugabyte_range]  (omitted: <variant> because <reason>)
Similar existing workloads checked: <files> -> <why this is new / extension>

## Schema & data
| table | rows | PK / sharding | indexes | key columns: generator -> distinct values x rows each |

## Workloads (run in this order)
| # | workload name | SQL (with ?) | bindings (util, range) | expected rows/exec | customTags |

## Settings
terminals / time_secs / warmup / setAutoCommit / explain + pg_stat flags, with one-line reasons.

## Risks / open questions
```

Design rules:
- **Bind ranges must stay inside the loaded range.** Compute the expected rows per execution for every query; this catches zero-row and wrong-selectivity mistakes before any YAML exists.
- **One variable per file.** The workloads in a file should form a curve along the varied dimension.
- **Mind the total duration.** Workloads run serially, each for warmup + time_secs, after the load. Keep one file under about an hour unless the user wants longer.

Stop here and wait for the user to approve or change the plan.

## Step 2: Write the yugabyte YAML

Start from `assets/template_yugabyte_latency.yaml`, or from the closest sibling file in the chosen category (its header conventions win). Write `config/yugabyte/regression_pipelines/<category>/yugabyte/<FILE>.yaml`. This file is the master copy; every other variant is derived from it.

Rules that prevent the historical mistakes:
- **Header:** copy it exactly from `references/categories_naming_variants.md` §4.
  - Use `loaderThreads` in camelCase; the lowercase spelling is silently ignored.
  - Use only the `{{endpoint}}`, `{{username}}` and `{{password}}` placeholders.
  - New files start at `yaml_version: 1.0`, written as a number. Older files use the string `v1.0`.
  - Don't add `analyze_on_all_tables`.
- **create:** `DROP TABLE IF EXISTS` first, then the tables, then secondary indexes. Write PKs as constraints, `PRIMARY KEY(col ASC)` or `PRIMARY KEY((a) HASH, b ASC)`, so the variant transforms stay mechanical. `cleanup` mirrors every drop.
- **loadRules:** use the real table and column names as created (`table: <prefix>_tbl_1`, `name: col_bigint_id_1`), without `count`. `count: N` is only for N copies of the same table or column; never write `count: 1`. Size PK generators to cover `rows`. Choose generators from the cheat sheet in `references/utils.md`.
- **executeRules:**
  - Every `run` needs `name` plus an integer `weight`.
  - The `?` count must equal the number of generated values.
  - Cast json, vector and array parameters in SQL (`?::jsonb`).
  - Give every workload `customTags` from the vocabulary.
  - Mark setup or mutation steps `skipReport: true`.
  - Only statements that legitimately return 0 rows get `zeroRowsValidation: false`.
  - Statements that can't be EXPLAINed (ANALYZE, DDL, CALL) should use `raw_sql: true` or a file-level `disable_explain: true`. Otherwise the pre-run EXPLAIN fails quietly: the error is only printed, and that query and every query after it in the file lose their EXPLAIN output.
- **Comment header:** `#TEST FOR YUGABYTE REGULAR TABLES`, `#GOAL…`, `#TESTS:`, stating the hypothesis and the data layout.
- **New util:** if no generator fits, see the end of `references/utils.md` for how to add one, including the Readme row.

For any key beyond the template, check `references/yaml_reference.md`. A misspelled key does nothing, silently.

## Step 3: Derive the variants and lint

```bash
fbtool variants config/yugabyte/regression_pipelines/<category>/yugabyte/<FILE>.yaml --to postgres,yb_colocated     # dry run: diff + notes
fbtool variants ... --to postgres,yb_colocated[,yugabyte_range] --write
```

The command:
- rewrites the headers (type, driver, url, createdb) and drops YB-only flags;
- converts PKs and indexes: HASH/ASC out for PG, all-ASC for range, and an inline PK moved to a trailing constraint;
- strips SPLIT clauses and sets `schematype` in customTags.

Act on every `REVIEW:` line it prints (YB-only GUCs, plan-forcing settings for PG throughput files, hash-specific tests in colocated), then read the generated files. Workload names, row counts, bind ranges, time and warmup must be identical across variants; only engine-specific syntax may differ. Omit a variant when the plan says so, e.g. a hash-sharding test gets no colocated copy.

Then lint every variant:
```bash
fbtool lint config/yugabyte/regression_pipelines/<category>/*/<FILE>.yaml
```
- **Errors** must be fixed. They cover:
  - wrong variant headers
  - unknown keys
  - weight or rate problems
  - `?`/binding count mismatches
  - unknown utils
  - PK ranges smaller than rows
  - workload names that differ across variants
- **Warnings** must be fixed or explicitly justified to the user. They cover:
  - bind ranges outside the loaded range
  - non-EXPLAINable statements without `raw_sql`/`disable_explain`
  - missing customTags
  - unprefixed table names
- Lint the whole category dir once too, so you can tell which warnings are pre-existing and which are yours.

**Finish** by listing the files written, the lint result, and any decisions or open questions for the user. Then stop.

## Editing existing workloads

The steps are the same, with these additions:
- Keep the file and workload names; renaming breaks result history.
- Change all variants together. #182 fixed a copy that had been updated in only one variant.
- Bump `yaml_version`: 1.x → 1.(x+1) for fixes, 2.0 when results aren't comparable with history. Write the bumped value as a number, even if the file had `v1.x`.
- Add a `yaml_change_description`, and optionally `workload_version` / `workload_change_description` per workload.

## Guardrails

- No database or universe runs, commits, pushes or PRs.
- Don't touch unrelated workloads or local changes, e.g. an uncommitted edit in another YAML.
- Don't edit `modifyyaml.py` or the Java framework as a side effect. If the framework blocks the test (e.g. an util bug), propose the fix separately.
- Files under `regression_pipelines/` run nightly once someone merges them, so write them as if they will.
