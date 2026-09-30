#!/usr/bin/env python3
"""FeatureBench microbenchmark helper for config/yugabyte/regression_pipelines.

Run from the benchbase repo root (or pass --repo).

    fbtool.py next-id <category>                 # prefixes in use, next free number, variant matrix
    fbtool.py variants <yugabyte.yaml> [--to postgres,yb_colocated,yugabyte_range] [--write]
                                                 # derive sibling variants from the yugabyte master copy
    fbtool.py lint <file-or-dir> [...]           # static checks (+ cross-variant consistency)

Only needs Python 3 + PyYAML. Nothing here talks to a database.
"""

import argparse
import difflib
import os
import re
import sys
from pathlib import Path

try:
    import yaml
except ImportError:  # pragma: no cover
    sys.exit("PyYAML is required: pip install pyyaml")

RP = Path("config/yugabyte/regression_pipelines")
UTILS_DIR = Path("src/main/java/com/oltpbenchmark/benchmarks/featurebench/utils")
VARIANTS = ("postgres", "yugabyte", "yb_colocated", "yugabyte_range")
NOT_UTILS = {"BaseUtil", "SQLUtil", "TextGenerator"}
EXECUTE_ONLY_UTILS = {"RangeDivider", "PrimaryIntRandomForExecutePhase"}
UNIQUE_KEY_UTILS = {"PrimaryIntGen", "RandomUniqueIntGen", "PrimaryIntGenThroughput"}
RANDOM_LOAD_UTILS = {"RandomNumber", "RandomInt", "RowRandomBoundedInt", "OneNumberFromArray",
                     "RandomUniqueIntGen"}
CAST_UTILS = ("Json", "ArrayGen", "VectorGen")

COLOCATED_CREATEDB = "drop database if exists yb_colocated; create database yb_colocated with colocated=true"
URLS = {
    "postgres": "jdbc:postgresql://{{endpoint}}:5432/postgres?sslmode=require&ApplicationName=featurebench&reWriteBatchedInserts=true",
    "yugabyte": "jdbc:yugabytedb://{{endpoint}}:5433/yugabyte?sslmode=require&ApplicationName=featurebench&reWriteBatchedInserts=true&load-balance=false",
}
HEADER_COMMENT = {
    "postgres": "#TEST FOR POSTGRES",
    "yugabyte": "#TEST FOR YUGABYTE REGULAR TABLES",
    "yb_colocated": "#TEST FOR YUGABYTE COLOCATED TABLES",
    "yugabyte_range": "#TEST FOR YUGABYTE RANGE SHARDED TABLES",
}

# Category -> (file-name prefix pattern shown to humans, nightly tag base, template class)
CATEGORY_INFO = {
    "Index_workloads": ("INDG<N>_U_", "index_workload", "3az leadAff"),
    "aggregate_workloads": ("AGGRG<N>_", "aggregate_workload", "3az leadAff"),
    "bulkload": ("goalN_<desc>/ (code-driven)", "bulkload", "1az"),
    "conditional_workloads": ("CW<N>_", "conditional_workload", "1az"),
    "ddl_workloads": ("DDL_G<N>_", "ddl_workload", "1az"),
    "foreign_key": ("FK_G<N>_", "hash_foreign_key_workload / range_foreign_key_workload / COLOCATED-foreign_key_workload", "3az leadAff"),
    "join_workloads": ("JOING<N>_", "join_workload", "3az leadAff"),
    "locking_semantics": ("<descriptive>", "locking_semantics_workload", "1az"),
    "miscellaneous": ("MG<N>_", "miscellaneous_workload", "1az"),
    "orderby_workloads": ("ORDG<N>_", "orderby_workload", "3az leadAff"),
    "perfstudio": ("MG<N>_ (PerfStudio pipelines only)", "-", "PerfStudio"),
    "range_write_workloads": ("RW_G<N>_", "- (no nightly row)", "-"),
    "scan_workloads": ("scanG<N>_", "scan_workload", "3az leadAff"),
    "skiplocked_workloads": ("SL_<desc>", "skiplocked_workload (yugabyte only)", "1az"),
    "throughput_next_workloads": ("THRPT_next_<desc> in <dimension>/ subdir", "throughput_next_workload (yugabyte only)", "1az"),
    "throughput_seek_workloads": ("THRPT_seek_<desc> in <dimension>/ subdir", "throughput_seek_workload (yugabyte only)", "1az"),
    "throughput_write_workloads": ("THRPT_<desc> in <dimension>/ subdir", "throughput_write_workload (yugabyte only)", "1az"),
    "vector_workloads": ("<op>_<dim>d_<desc>", "vector_workload (yugabyte only)", "1az"),
    "write_workloads": ("insertG<N>_ / updateG<N>_", "write_workload", "1az"),
}

KNOWN_TOP = {
    "type", "driver", "url", "username", "password", "batchsize", "isolation", "terminals",
    "loaderThreads", "loaderthreads", "collect_pg_stat_statements", "use_dist_in_explain",
    "disable_explain", "force_capture_explain_analyze", "analyze_on_all_tables", "yaml_version",
    "yaml_change_description", "works", "microbenchmark", "createdb", "optimalThreads", "targetCPU",
    "toleranceCPU", "samplingTime", "scalingMinDeltaPercent", "linearPGthread", "threadIncrement",
    "restingTimeSecs", "flatMaxScalingSteps", "useThroughputThreshold", "truncateBetweenIterations",
    "retries", "newConnectionPerTxn", "randomSeed", "scalefactor", "datadir", "ddlpath", "selectivity",
    "time", "transactiontypes",
}
KNOWN_WORK = {"time_secs", "warmup", "rate", "active_terminals", "serial", "executeNtimes", "weights",
              "@arrival", "arrival", "time"}
KNOWN_PROPS = {"setAutoCommit", "create", "loadRules", "executeRules", "afterLoad", "cleanup", "execute",
               "executeOnce", "iterationCleanup", "workload",
               # bulkload Goal* custom keys
               "tableName", "columns", "rows", "indexes", "filePath", "stringLength", "recreateCsvIfExists",
               "create_index_before_load", "create_index_after_load", "testDrop", "testCreate", "numTables"}
KNOWN_WORKLOAD = {"workload", "run", "time_secs", "executeNtimes", "raw_sql", "customTags", "skipReport",
                  "zeroRowsValidation", "workload_version", "workload_change_description"}
KNOWN_RUN = {"name", "weight", "queries"}
KNOWN_QUERY = {"query", "bindings", "count", "pattern_count", "explain-plan-rc-validation"}
KNOWN_BINDING = {"util", "params", "count", "split_min_max_for_count", "referenceName"}
KNOWN_LOADRULE = {"table", "rows", "count", "columns"}
KNOWN_COLUMN = {"name", "util", "params", "count"}
ISOLATIONS = {"TRANSACTION_SERIALIZABLE", "TRANSACTION_READ_COMMITTED", "TRANSACTION_REPEATABLE_READ",
              "TRANSACTION_READ_UNCOMMITTED"}
NON_EXPLAINABLE = re.compile(r"^\s*(analyze|vacuum|create|drop|alter|truncate|call|do|refresh|reindex|grant|set|reset|comment|cluster)\b", re.I)
JINJA = re.compile(r"\{\{\s*([A-Za-z0-9_,]+)\s*\}\}")


# --------------------------------------------------------------------------------------------- helpers
def repo_root(arg):
    root = Path(arg or os.getcwd()).resolve()
    for p in [root, *root.parents]:
        if (p / RP).is_dir():
            return p
    sys.exit(f"could not find {RP} above {root}; run from the benchbase repo or pass --repo")


def load_yaml_text(text):
    """Parse a pipeline YAML that still contains {{jinja}} placeholders."""
    tokens = set(JINJA.findall(text))
    safe = JINJA.sub(lambda m: f"__JINJA_{m.group(1).replace(',', '_')}__", text)
    return yaml.safe_load(safe), tokens


def variant_of(path):
    parts = Path(path).resolve().parts
    # only look below regression_pipelines/<category>/ so "config/yugabyte/..." is never taken for the
    # yugabyte/ variant dir; category-root files (e.g. bulkload/300M_rows_pg.yaml) have no variant
    start = parts.index("regression_pipelines") + 2 if "regression_pipelines" in parts else 0
    for part in reversed(parts[start:-1]):
        if part in VARIANTS:
            return part
    return None


def category_of(path):
    parts = Path(path).resolve().parts
    if "regression_pipelines" in parts:
        i = parts.index("regression_pipelines")
        if i + 1 < len(parts):
            return parts[i + 1]
    return None


def sibling(path, variant):
    parts = list(Path(path).resolve().parts)
    cur = variant_of(path)
    if not cur:
        return None
    parts[len(parts) - 1 - parts[::-1].index(cur)] = variant
    return Path(*parts)


def as_list(x):
    if x is None:
        return []
    return x if isinstance(x, list) else [x]


def balanced(text, open_idx):
    """Return index just past the paren that closes text[open_idx] == '('."""
    depth = 0
    for i in range(open_idx, len(text)):
        if text[i] == "(":
            depth += 1
        elif text[i] == ")":
            depth -= 1
            if depth == 0:
                return i + 1
    return -1


def split_top(s):
    out, depth, cur = [], 0, ""
    for ch in s:
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
        if ch == "," and depth == 0:
            out.append(cur)
            cur = ""
        else:
            cur += ch
    out.append(cur)
    return out


# --------------------------------------------------------------------------------------------- next-id
def cmd_next_id(args):
    root = repo_root(args.repo)
    cats = sorted(p.name for p in (root / RP).iterdir() if p.is_dir())
    if args.category not in cats:
        close = difflib.get_close_matches(args.category, cats, n=3, cutoff=0.3)
        sys.exit(f"unknown category {args.category!r}. Known: {', '.join(cats)}"
                 + (f"\nDid you mean: {', '.join(close)}" if close else ""))
    cdir = root / RP / args.category
    files = sorted(cdir.rglob("*.yaml"))
    by_name = {}
    for f in files:
        v = variant_of(f) or "(category root)"
        by_name.setdefault(f.name, set()).add(v)
    prefixes = {}
    for name in by_name:
        m = re.match(r"^([A-Za-z]+(?:_[A-Za-z]+)?_?G?)(\d+)_", name) or re.match(r"^([A-Za-z]+)(\d+)_", name)
        if m:
            prefixes.setdefault(m.group(1), set()).add(int(m.group(2)))
    info = CATEGORY_INFO.get(args.category, ("?", "?", "?"))
    print(f"category        : {args.category}")
    print(f"naming pattern  : {info[0]}")
    print(f"nightly tag     : YB-{info[1]}   (colocated: YB-COLOCATED-{info[1].split(' ')[0]} where it exists)")
    if prefixes:
        print("numbered prefixes (max -> next; gaps from deleted files are NOT reused):")
        for p, nums in sorted(prefixes.items(), key=lambda kv: -len(kv[1])):
            print(f"  {p:<12} used {sorted(nums)} -> next {p}{max(nums) + 1}_")
    else:
        print("no numbered prefix in use: name files descriptively like the existing ones")
    present = [v for v in VARIANTS if (cdir / v).is_dir()]
    print(f"variant dirs    : {', '.join(present) or '(none)'}")
    print("\nfile -> variants present")
    width = max((len(n) for n in by_name), default=10)
    for name, vs in sorted(by_name.items()):
        missing = [v for v in present if v not in vs]
        print(f"  {name:<{width}}  {','.join(sorted(vs))}" + (f"   (missing: {','.join(missing)})" if missing else ""))


# --------------------------------------------------------------------------------------------- variants
def transform_key_list(inner, mode):
    """Rewrite the column list of a PRIMARY KEY(...) or index (...).

    mode: 'pg'         -> strip ASC/DESC/HASH, unwrap (a,b) HASH   (PRIMARY KEY)
          'pg_index'   -> strip HASH, unwrap (a,b) HASH, keep ASC/DESC
          'range'      -> every column explicit ASC (keep DESC), HASH -> ASC
          'colocated'  -> HASH -> ASC, unwrap (a,b) HASH, leave the rest
    """
    cols = []
    for el in split_top(inner):
        e = el.strip()
        if not e:
            continue
        m = re.match(r"^\((.*)\)\s*(hash)?\s*$", e, re.I | re.S)
        if m:  # (a, b) HASH
            members = [c.strip() for c in m.group(1).split(",") if c.strip()]
            if mode in ("pg", "pg_index"):
                cols.extend(members)
            else:
                cols.extend(f"{c} ASC" for c in members)
            continue
        m = re.match(r"^(.*?)\s+(asc|desc|hash)\s*$", e, re.I | re.S)
        col, order = (m.group(1).strip(), m.group(2).upper()) if m else (e, "")
        if mode == "pg":
            cols.append(col)
        elif mode == "pg_index":
            cols.append(col if order in ("", "HASH") else f"{col} {order}")
        elif mode == "range":
            cols.append(f"{col} {'DESC' if order == 'DESC' else 'ASC'}")
        elif mode == "colocated":
            cols.append(f"{col} ASC" if order == "HASH" else (f"{col} {order}" if order else col))
    return ", ".join(cols)


def rewrite_sql(text, target, notes):
    # 1. tablet split clauses
    def drop_split(t):
        out, i = "", 0
        pat = re.compile(r"\s*\bsplit\s+at\s+values\s*\(", re.I)
        while True:
            m = pat.search(t, i)
            if not m:
                return out + t[i:]
            end = balanced(t, m.end() - 1)
            out += t[i:m.start()]
            i = end if end > 0 else len(t)
            notes.add("removed SPLIT AT VALUES clause(s)")

    text = drop_split(text)
    text, n = re.subn(r"\s*\bsplit\s+into\s+\d+\s+tablets\b", "", text, flags=re.I)
    if n:
        notes.add("removed SPLIT INTO N TABLETS clause(s)")

    # 2. PRIMARY KEY (...) constraint lists
    pk_mode = {"postgres": "pg", "yugabyte_range": "range", "yb_colocated": "colocated"}[target]
    out, i = "", 0
    pat = re.compile(r"\bprimary\s+key\s*\(", re.I)
    while True:
        m = pat.search(text, i)
        if not m:
            out += text[i:]
            break
        end = balanced(text, m.end() - 1)
        if end < 0:
            out += text[i:]
            break
        inner = text[m.end():end - 1]
        new = transform_key_list(inner, pk_mode)
        if new.replace(" ", "").lower() != inner.replace(" ", "").lower():
            notes.add(f"rewrote PRIMARY KEY column lists for {target}")
        out += text[i:m.end()] + new + ")"
        i = end
    text = out

    # 3. index column lists
    idx_mode = {"postgres": "pg_index", "yugabyte_range": "range", "yb_colocated": "colocated"}[target]
    out, i = "", 0
    pat = re.compile(r"\bcreate\s+(?:unique\s+)?index\b[^;(]*?\bon\s+[\w.\"]+\s*(?:using\s+\w+\s*)?\(", re.I)
    while True:
        m = pat.search(text, i)
        if not m:
            out += text[i:]
            break
        end = balanced(text, m.end() - 1)
        if end < 0:
            out += text[i:]
            break
        inner = text[m.end():end - 1]
        if re.search(r"_ops\b|\(", inner):  # opclass / expression index: leave alone
            new = inner
        elif target == "yugabyte_range":
            new = transform_key_list(inner, "range")
        else:
            new = transform_key_list(inner, idx_mode) if re.search(r"\bhash\b", inner, re.I) else inner
        if new != inner:
            notes.add(f"rewrote index column lists for {target}")
        out += text[i:m.end()] + new + ")"
        i = end
    text = out

    # 4. inline "col type PRIMARY KEY" -> trailing "primary key(col asc)" (what the team does for
    #    yb_colocated / yugabyte_range, e.g. FK_G1)
    if target in ("yugabyte_range", "yb_colocated"):
        out, i = "", 0
        pat = re.compile(r"\bcreate\s+table\s+(?:if\s+not\s+exists\s+)?[\w.\"]+\s*\(", re.I)
        while True:
            m = pat.search(text, i)
            if not m:
                out += text[i:]
                break
            end = balanced(text, m.end() - 1)
            if end < 0:
                out += text[i:]
                break
            elems = split_top(text[m.end():end - 1])
            pk_cols = []
            for k, el in enumerate(elems):
                im = re.match(r"^(\s*)(\"?\w+\"?)(\s+.*?)\s+primary\s+key\b(?!\s*\()(.*)$", el, re.I | re.S)
                if im:
                    pk_cols.append(im.group(2))
                    elems[k] = f"{im.group(1)}{im.group(2)}{im.group(3)}{im.group(4)}"
            if pk_cols:
                elems.append(f" primary key({', '.join(c + ' asc' for c in pk_cols)})")
                notes.add(f"moved inline PRIMARY KEY columns to a trailing 'primary key(col asc)' for {target}")
            out += text[i:m.end()] + ",".join(elems) + ")"
            i = end
        text = out

    if target == "postgres":
        text, n = re.subn(r"\bybhnsw\b", "hnsw", text)
        if n:
            notes.add("ybhnsw -> hnsw")
        if re.search(r"\byb_\w+|\bcolocat", text, re.I):
            notes.add("REVIEW: YB-only GUC/function/colocation keyword still present (yb_*, colocation) - remove or replace for postgres")
        if re.search(r"\bALTER\s+DATABASE\s+yugabyte\b", text, re.I):
            notes.add("REVIEW: 'ALTER DATABASE yugabyte' should target 'postgres' (or be removed if YB-only)")
    return text


def make_variant(src_text, target, notes):
    lines = src_text.splitlines(keepends=True)
    out = []
    for ln in lines:
        s = ln.strip()
        if re.match(r"^analyze_on_all_tables\s*:", ln):
            notes.add("dropped analyze_on_all_tables (not used in these microbenchmarks)")
            continue
        if s.startswith("#TEST FOR"):
            out.append(HEADER_COMMENT[target] + "\n")
            continue
        if target == "postgres":
            if re.match(r"^type\s*:", ln):
                out.append("type: POSTGRES\n"); continue
            if re.match(r"^driver\s*:", ln):
                out.append("driver: org.postgresql.Driver\n"); continue
            if re.match(r"^url\s*:", ln):
                out.append(f"url: {URLS['postgres']}\n"); continue
            if re.match(r"^use_dist_in_explain\s*:", ln):
                notes.add("dropped use_dist_in_explain (throws on POSTGRES)"); continue
            if re.match(r"^createdb\s*:", ln):
                notes.add("dropped createdb"); continue
        elif target == "yb_colocated":
            if re.match(r"^createdb\s*:", ln):
                continue
            if re.match(r"^url\s*:", ln):
                ln = re.sub(r"load-balance=true", "load-balance=false", ln)
                out.append(ln)
                out.append(f"createdb: {COLOCATED_CREATEDB}\n")
                continue
            if "customTags" in ln:
                new = ln.replace("schematype=regular", "schematype=colocated")
                if re.search(r"partition=hash|pkey=hash|hashpkey|hashskey", new):
                    notes.add("REVIEW: customTags mention hash partitioning - colocated tables are range; hash-specific workloads are usually omitted from yb_colocated")
                ln = new
        elif target == "yugabyte_range":
            if "customTags" in ln:
                ln = ln.replace("partition=hash", "partition=range").replace("pkey=hash", "pkey=asc")
        out.append(ln)
    text = rewrite_sql("".join(out), target, notes)

    return text


def cmd_variants(args):
    src = Path(args.file)
    if variant_of(src) != "yugabyte":
        sys.exit("the master copy must live in a yugabyte/ dir; author yugabyte first, then derive the rest")
    text = src.read_text()
    cat = category_of(src)
    targets = [t.strip() for t in args.to.split(",")] if args.to else ["postgres", "yb_colocated"]
    for t in targets:
        if t not in ("postgres", "yb_colocated", "yugabyte_range"):
            sys.exit(f"unknown target variant {t}")
        notes = set()
        new = make_variant(text, t, notes)
        if t == "postgres" and cat and cat.startswith(("throughput", "skiplocked")):
            notes.add("REVIEW: this category's PG files add 'ALTER DATABASE postgres SET enable_seqscan=off; ... enable_bitmapscan=off;' "
                      "to create (reset TO DEFAULT in cleanup) so PG uses the same index plan as YB")
        dst = sibling(src, t)
        print(f"==== {t}: {dst}")
        if cat and not (dst.parent.exists()) and t != "yugabyte_range":
            print(f"   note: {dst.parent} does not exist yet in {cat}")
        for n in sorted(notes):
            print(f"   - {n}")
        old = dst.read_text() if dst.exists() else text
        diff = difflib.unified_diff(old.splitlines(), new.splitlines(),
                                    "existing" if dst.exists() else "yugabyte", t, lineterm="", n=0)
        for d in diff:
            print("   " + d)
        if args.write:
            dst.parent.mkdir(parents=True, exist_ok=True)
            dst.write_text(new)
            print(f"   wrote {dst}")
    if not args.write:
        print("\n(dry run - pass --write to create the files)")


# --------------------------------------------------------------------------------------------- lint
class Report:
    def __init__(self):
        self.items = []

    def add(self, level, where, msg):
        self.items.append((level, str(where), msg))

    def err(self, w, m): self.add("ERROR", w, m)
    def warn(self, w, m): self.add("WARN", w, m)
    def info(self, w, m): self.add("INFO", w, m)


def created_tables(data):
    props = ((data or {}).get("microbenchmark") or {}).get("properties") or {}
    out = []
    for s in as_list(props.get("create")):
        for m in re.finditer(r"create\s+table\s+(?:if\s+not\s+exists\s+)?([\w.\"]+)(\s+partition\s+of\b)?", str(s), re.I):
            if not m.group(2):  # partitions are dropped with their parent
                out.append(m.group(1).strip('"').lower())
    return out


def loadrule_tables(rule):
    names = [t.strip() for t in str(rule.get("table", "")).split(",") if t.strip()]
    c = rule.get("count")
    if c:
        return [f"{n}{i}".lower() for n in names for i in range(1, int(c) + 1)]
    return [n.lower() for n in names]


def expected_bindings(query_obj):
    """(placeholders, bindings, problems) after pattern_count/count/split expansion."""
    sql = str(query_obj.get("query", ""))
    pc = query_obj.get("pattern_count")
    b = query_obj.get("bindings")
    blist = [b] if isinstance(b, dict) else as_list(b)
    per = 0
    for bd in blist:
        if not isinstance(bd, dict):
            continue
        if "split_min_max_for_count" in bd:
            per += int(bd["split_min_max_for_count"]) if len(as_list(bd.get("params"))) == 2 else 0
        else:
            per += int(bd.get("count", 1) or 1)
    if pc and int(pc) > 0:
        sql = re.sub(r"\[(.*?)\]\[pattern_count\]", lambda m: ",".join([m.group(1)] * int(pc)), sql)
        per *= int(pc)
    # ignore ? inside quoted literals and the jsonb ?| ?& operators
    stripped = re.sub(r"'(?:[^']|'')*'", "''", sql)
    stripped = re.sub(r"\?[|&]", "", stripped)
    return stripped.count("?"), per, blist


RANGE_UTILS = ("RandomInt", "RandomNumber", "CyclicSeqIntGen", "RandomUniqueCyclicIntGen", "PrimaryIntGen")


def check_bind_ranges(sql, blist, load_ranges, rep, qw):
    """Pair each ? with the column it is compared to and flag bind ranges outside the loaded range."""
    slots = []  # (util, params) per placeholder, in order
    for bd in blist:
        if not isinstance(bd, dict):
            continue
        if "split_min_max_for_count" in bd:
            slots.extend([(None, None)] * int(bd["split_min_max_for_count"]))
        else:
            slots.extend([(bd.get("util"), as_list(bd.get("params")))] * int(bd.get("count", 1) or 1))
    referenced = [t for t in load_ranges if re.search(rf"\b{re.escape(t)}\b", sql, re.I)]
    stripped = re.sub(r"'(?:[^']|'')*'", "''", sql)
    w = re.search(r"\bwhere\b", stripped, re.I)
    if not w:
        return
    for idx, m in enumerate(re.finditer(r"\?(?![|&])", stripped)):
        if idx >= len(slots):
            break
        if m.start() < w.start():
            continue  # SET col=? / select-list values are not predicates
        u, p = slots[idx]
        if u not in RANGE_UTILS or not p or len(p) < 2 or not all(isinstance(x, (int, float)) for x in p[:2]):
            continue
        cmp = list(re.finditer(r"([\w.]+)\s*(=|<=|>=|<>|<|>|\bbetween\b|\bin\b)", stripped[:m.start()], re.I))
        if not cmp:
            continue
        col = cmp[-1].group(1).split(".")[-1].lower()
        owners = [t for t in referenced if col in load_ranges[t]]
        if len(owners) != 1:
            continue  # unknown or ambiguous column (e.g. same name in joined tables)
        lu, lo, hi = load_ranges[owners[0]][col]
        if p[0] < lo or p[1] > hi:
            rep.warn(qw, f"binding range {p[:2]} for {col} is outside its loaded range [{lo}, {hi}] "
                         f"({owners[0]}) -> zero-row transactions")


def lint_file(path, rep, utils):
    where = str(path)
    text = Path(path).read_text()
    try:
        data, tokens = load_yaml_text(text)
    except yaml.YAMLError as e:
        rep.err(where, f"YAML does not parse: {e}")
        return None
    if not isinstance(data, dict):
        rep.err(where, "top level is not a mapping")
        return None
    variant = variant_of(path)
    cat = category_of(path)

    # -- templating
    extra = tokens - {"endpoint", "username", "password"}
    if extra:
        rep.err(where, f"jinja placeholders other than endpoint/username/password: {sorted(extra)} "
                       "(pipelines only pass those three; local -p runs fail on unknown tokens)")
    for k in ("username", "password"):
        if str(data.get(k)) != f"__JINJA_{k}__":
            rep.warn(where, f"{k} should be {{{{{k}}}}}")
    if "__JINJA_endpoint__" not in str(data.get("url", "")):
        rep.err(where, "url must use {{endpoint}}")

    # -- unknown keys
    for k in data:
        if k not in KNOWN_TOP:
            rep.err(where, f"unknown top-level key {k!r} (silently ignored by the framework)")
    if "loaderthreads" in data:
        rep.warn(where, "'loaderthreads' (lowercase) is ignored - the code reads 'loaderThreads'")
    if "time" in data:
        rep.warn(where, "top-level 'time' is never read (use works.work.time_secs)")
    for k in ("type", "driver", "url", "username", "password"):
        if k not in data:
            rep.err(where, f"missing required key {k}")
    if "yaml_version" not in data:
        rep.warn(where, "missing yaml_version (use v1.0 for new files; bump on behavioural edits)")
    iso = data.get("isolation")
    if iso and iso not in ISOLATIONS:
        rep.err(where, f"isolation {iso!r} is not one of {sorted(ISOLATIONS)} (silently falls back to SERIALIZABLE)")
    if not iso:
        rep.warn(where, "no isolation set: default is TRANSACTION_SERIALIZABLE; pipelines use TRANSACTION_REPEATABLE_READ")

    if "analyze_on_all_tables" in data and variant != "postgres":
        rep.warn(where, "analyze_on_all_tables is not used in these microbenchmarks - remove it")
    typ = str(data.get("type", "")).upper()
    url = str(data.get("url", ""))
    drv = str(data.get("driver", ""))
    terminals = int(data.get("terminals") or 0)
    optimal = bool(data.get("optimalThreads"))

    # -- variant header
    if variant == "postgres":
        if typ != "POSTGRES": rep.err(where, "postgres variant must have type: POSTGRES")
        if drv != "org.postgresql.Driver": rep.err(where, "postgres variant must use org.postgresql.Driver")
        if not re.search(r"jdbc:postgresql://__JINJA_endpoint__:5432/postgres\b", url):
            rep.err(where, "postgres url must be jdbc:postgresql://{{endpoint}}:5432/postgres?...")
        if data.get("use_dist_in_explain"): rep.err(where, "use_dist_in_explain throws on POSTGRES")
        if data.get("analyze_on_all_tables"):
            rep.err(where, "analyze_on_all_tables issues 'ALTER DATABASE .. SET yb_enable_optimizer_statistics' (YB-only) and fails on POSTGRES")
        if "createdb" in data: rep.err(where, "createdb does not belong in the postgres variant")
    elif variant in ("yugabyte", "yb_colocated", "yugabyte_range"):
        if typ != "YUGABYTE": rep.err(where, f"{variant} variant must have type: YUGABYTE")
        if drv != "com.yugabyte.Driver": rep.err(where, f"{variant} variant must use com.yugabyte.Driver")
        if not re.search(r"jdbc:yugabytedb://__JINJA_endpoint__:5433/yugabyte\b", url):
            rep.err(where, "yugabyte url must be jdbc:yugabytedb://{{endpoint}}:5433/yugabyte?...")
        if not data.get("use_dist_in_explain") and not data.get("disable_explain") and cat != "bulkload":
            rep.warn(where, "use_dist_in_explain: true is standard on YB variants")
        if "load-balance=" not in url:
            rep.warn(where, "url has no load-balance= parameter (false for single-terminal latency, true for throughput/multi-terminal)")
        elif (terminals > 1 or optimal) and "load-balance=false" in url and variant != "yb_colocated":
            rep.warn(where, "multi-terminal/optimalThreads workload with load-balance=false (throughput files use true)")
        if variant == "yb_colocated":
            cdb = str(data.get("createdb", ""))
            if not re.search(r"create\s+database\s+yb_colocated\s+with\s+colocated\s*=\s*true", cdb, re.I):
                rep.err(where, f"yb_colocated needs createdb: {COLOCATED_CREATEDB} "
                               "(db must be named yb_colocated or the pipeline's colocated-leader lookup silently no-ops)")
        elif "createdb" in data:
            rep.warn(where, "createdb outside yb_colocated - intended?")

    # -- works
    work = ((data.get("works") or {}).get("work")) or {}
    for k in work:
        if k not in KNOWN_WORK:
            rep.err(where, f"unknown works.work key {k!r}")
    if "rate" not in work:
        rep.err(where, "works.work.rate is required (use 'unlimited')")
    props = ((data.get("microbenchmark") or {}).get("properties")) or {}
    cls = str((data.get("microbenchmark") or {}).get("class", ""))
    code_driven = cls and not cls.endswith("YBDefaultMicroBenchmark")
    ers = as_list(props.get("executeRules"))
    t_secs = work.get("time_secs")
    warm = work.get("warmup", 0) or 0
    any_ntimes = any(isinstance(e, dict) and e.get("executeNtimes") for e in ers) or work.get("executeNtimes")
    if not code_driven and not any_ntimes and not work.get("serial") and not (t_secs and int(t_secs) > 0) \
            and not all(isinstance(e, dict) and e.get("time_secs") for e in ers):
        rep.err(where, "works.work.time_secs must be > 0 (FeatureBench reads time_secs, not time)")
    latency_style = terminals == 1 and not optimal and not any_ntimes
    if optimal and "active_terminals" in work:
        rep.warn(where, "throughput/optimalThreads files must not set active_terminals (#218)")
    if latency_style and t_secs is not None and cat not in ("foreign_key", "write_workloads", "bulkload", "perfstudio"):
        if int(t_secs) < 180 or int(warm) < 60:
            rep.warn(where, f"latency run is time_secs={t_secs}, warmup={warm}; current practice is >=180 / >=60 to limit variance")
    if "active_terminals" in work and terminals and int(work["active_terminals"]) > terminals:
        rep.err(where, "active_terminals > terminals")

    # -- properties
    for k in props:
        if k not in KNOWN_PROPS:
            rep.err(where, f"unknown microbenchmark.properties key {k!r}" + (" (typo? '-afterLoad' style keys are silently ignored)" if k.lstrip("-") in KNOWN_PROPS else ""))
    if "setAutoCommit" not in props and not code_driven:
        rep.warn(where, "setAutoCommit not set: FeatureBench defaults to false (each run = one transaction); pipelines set true unless testing txns")
    if code_driven:
        rep.info(where, f"code-driven class {cls}: YAML-level checks limited")

    ctables = created_tables(data)
    create_sql = " ".join(str(s) for s in as_list(props.get("create")))
    cleanup_sql = " ".join(str(s) for s in as_list(props.get("cleanup"))).lower()
    for t in ctables:
        if not re.search(rf"drop\s+table\s+if\s+exists\s+[^;]*\b{re.escape(t)}\b", create_sql, re.I) \
                and "if not exists" not in create_sql.lower():
            rep.warn(where, f"create does not start with DROP TABLE IF EXISTS {t}")
        if props.get("cleanup") is not None and t not in cleanup_sql:
            rep.warn(where, f"cleanup does not drop {t}")
    if ctables and props.get("cleanup") is None:
        rep.warn(where, "no cleanup list (mirror the DROP TABLEs)")
    fname = Path(path).stem
    m = re.match(r"^([A-Za-z]+_?G?\d+)", fname)
    file_prefix = m.group(1).lower() if m else None
    if file_prefix and ctables:
        unprefixed = [t for t in ctables if file_prefix.replace("_", "") not in t.replace("_", "")]
        if unprefixed:
            rep.warn(where, f"table names without the workload prefix {file_prefix!r}: {unprefixed} "
                            "(collisions across files on the same universe were a recurring bug, f56eedd0)")
    if variant == "postgres" and re.search(r"\bsplit\s+(at|into)\b|\bhash\b\s*[,)]|\bybhnsw\b", create_sql, re.I):
        rep.err(where, "YB-only DDL (SPLIT / HASH / ybhnsw) in the postgres variant")
    if variant == "postgres" and re.search(r"primary\s+key\s*\([^)]*\b(asc|desc)\b", create_sql, re.I):
        rep.err(where, "ASC/DESC inside PRIMARY KEY(...) is not valid PostgreSQL")
    if variant == "yb_colocated" and re.search(r"\bsplit\s+(at|into)\b", create_sql, re.I):
        rep.err(where, "SPLIT clauses are not valid for colocated tables")
    if variant == "yugabyte_range" and re.search(r"\bhash\b", create_sql, re.I):
        rep.err(where, "HASH in the yugabyte_range variant")

    # -- loadRules
    lrules = as_list(props.get("loadRules"))
    load_ranges = {}
    loaded = set()
    for i, r in enumerate(lrules):
        w = f"{where} loadRules[{i}]"
        if not isinstance(r, dict):
            rep.err(w, "not a mapping"); continue
        for k in r:
            if k not in KNOWN_LOADRULE:
                rep.err(w, f"unknown key {k!r}")
        tabs = loadrule_tables(r)
        loaded.update(tabs)
        for t in tabs:
            if ctables and t not in ctables:
                rep.err(w, f"loads table {t!r} which create does not create (remember table: foo_ + count: 1 -> foo_1)")
        rows = int(r.get("rows", 0) or 0)
        seen_counted = False
        for j, c in enumerate(as_list(r.get("columns"))):
            cw = f"{w}.columns[{j}]"
            if not isinstance(c, dict):
                continue
            for k in c:
                if k not in KNOWN_COLUMN:
                    rep.err(cw, f"unknown key {k!r}")
            u = str(c.get("util", ""))
            p = as_list(c.get("params"))
            if u not in utils:
                close = difflib.get_close_matches(u, utils, n=2)
                rep.err(cw, f"util {u!r} does not exist" + (f" (did you mean {close}?)" if close else ""))
            if u in EXECUTE_ONLY_UTILS:
                rep.err(cw, f"{u} has no load-phase constructor")
            if u in UNIQUE_KEY_UTILS and len(p) >= 2 and all(isinstance(x, (int, float)) for x in p[:2]):
                if p[1] - p[0] + 1 < rows:
                    rep.err(cw, f"{u} range {p[:2]} is smaller than rows={rows} (throws 'Out of bounds')")
            if u in RANDOM_LOAD_UTILS and not (u == "OneNumberFromArray" and len(p) == 1) \
                    and c.get("name") and not re.search(r"pad|filler|payload", str(c.get("name")), re.I):
                if u == "RandomUniqueIntGen":
                    rep.info(cw, "RandomUniqueIntGen loads the same key set in a random order each time; #214 switched id columns to PrimaryIntGen")
                else:
                    rep.warn(cw, f"{u} in loadRules gives a different data distribution every load; for columns that are "
                                 "filtered/joined/aggregated prefer CyclicSeqIntGen / RandomUniqueCyclicIntGen (#214)")
            if u == "OneNumberFromArray" and len(p) > 20:
                rep.warn(cw, f"OneNumberFromArray with {len(p)} literals: use CyclicSeqIntGen [lo, hi] instead")
            if u == "RandomDate" and len(p) == 2 and all(isinstance(x, int) and 1900 < x < 2200 for x in p):
                rep.warn(cw, "RandomDate takes [numberOfDays, offsetDays], not years - use RandomDateBtwYears for a year range")
            if u == "RandomFloat":
                rep.warn(cw, "RandomFloat returns PI * randint, not a uniform float - prefer RandomNoWithDecimalPoints")
            if u in ("DeterministicVectorGen", "SeededRandomNumber"):
                rep.warn(cw, f"{u} returns the SAME value on every call - every row will be identical")
            if any(s in u for s in CAST_UTILS) and seen_counted:
                rep.warn(cw, "array/json/vector column after a counted column: loader typecast lookup uses the wrong index (put it before counted columns)")
            if c.get("count"):
                seen_counted = True
            if u in ("PrimaryIntGen", "CyclicSeqIntGen", "RandomUniqueCyclicIntGen", "RandomNumber", "RandomInt") and len(p) >= 2:
                cnt = int(c.get("count") or 0)
                names = [f"{c.get('name')}{k}" for k in range(1, cnt + 1)] if cnt else [str(c.get("name"))]
                for nm in names:
                    for t in tabs:
                        load_ranges.setdefault(t, {})[nm.lower()] = (u, p[0], p[1])
    if lrules and not ers and not props.get("executeOnce") and not code_driven:
        rep.warn(where, "loadRules but no executeRules")
    if not lrules and ctables and not code_driven:
        rep.info(where, "no loadRules: run with --load=false (load=true exits with 'Empty Load Rules')")

    # -- executeRules
    names = []
    explain_on = not data.get("disable_explain")
    for i, er in enumerate(ers):
        w = f"{where} executeRules[{i}]"
        if not isinstance(er, dict):
            rep.err(w, "not a mapping"); continue
        for k in er:
            if k not in KNOWN_WORKLOAD:
                rep.err(w, f"unknown key {k!r}")
        wn = er.get("workload")
        if not wn:
            rep.err(w, "workload name missing (results folder becomes a timestamp and --workloads cannot select it)")
        else:
            names.append(wn)
            w = f"{where} [{wn}]"
            if file_prefix and not str(wn).lower().replace("_", "").startswith(file_prefix.replace("_", "")[:4]):
                rep.info(w, f"workload name does not start with the file prefix ({file_prefix}); convention is <PREFIX>_<k>_<desc>")
        if er.get("executeNtimes") and terminals > 1:
            rep.err(w, "executeNtimes requires terminals: 1")
        if er.get("time_secs") is not None and latency_style and int(er["time_secs"]) < 60:
            rep.info(w, f"per-workload time_secs={er['time_secs']}")
        if not er.get("customTags") and cat in ("Index_workloads", "aggregate_workloads", "join_workloads",
                                                 "orderby_workloads", "scan_workloads", "vector_workloads"):
            rep.warn(w, "no customTags (this category tags every workload for dashboard slicing)")
        ct = str(er.get("customTags", ""))
        if variant == "yb_colocated" and "schematype=regular" in ct:
            rep.err(w, "customTags says schematype=regular in the colocated variant")
        if variant in ("yugabyte", "postgres", "yugabyte_range") and "schematype=colocated" in ct:
            rep.err(w, "customTags says schematype=colocated outside yb_colocated")
        raw = bool(er.get("raw_sql"))
        runs = as_list(er.get("run"))
        if not runs:
            rep.err(w, "no run entries")
        for rj, r in enumerate(runs):
            rw = f"{w}.run[{rj}]"
            if not isinstance(r, dict):
                continue
            for k in r:
                if k not in KNOWN_RUN:
                    rep.err(rw, f"unknown key {k!r}")
            if "name" not in r:
                rep.err(rw, "run.name missing")
            if "weight" not in r:
                rep.err(rw, "run.weight missing (getInt with no default -> NoSuchElementException)")
            elif not isinstance(r["weight"], int):
                rep.err(rw, f"weight {r['weight']!r} must be an integer")
            for qk, q in enumerate(as_list(r.get("queries"))):
                qw = f"{rw}.queries[{qk}]"
                if not isinstance(q, dict):
                    continue
                for k in q:
                    if k not in KNOWN_QUERY:
                        rep.err(qw, f"unknown key {k!r}")
                sql = str(q.get("query", ""))
                nq, nb, blist = expected_bindings(q)
                if nq != nb:
                    rep.err(qw, f"{nq} '?' placeholders but bindings produce {nb} values")
                refs = set()
                for bd in blist:
                    if not isinstance(bd, dict):
                        continue
                    for k in bd:
                        if k not in KNOWN_BINDING:
                            rep.err(qw, f"unknown binding key {k!r}")
                    u = str(bd.get("util", ""))
                    if u not in utils:
                        close = difflib.get_close_matches(u, utils, n=2)
                        rep.err(qw, f"binding util {u!r} does not exist" + (f" (did you mean {close}?)" if close else ""))
                    if bd.get("referenceName"):
                        refs.add(bd["referenceName"])
                    if u == "ExpressionEval":
                        expr = " ".join(str(x) for x in as_list(bd.get("params")))
                        for ident in re.findall(r"[A-Za-z_]\w*", expr):
                            if ident not in refs and ident not in {b.get("referenceName") for b in blist if isinstance(b, dict)}:
                                rep.err(qw, f"ExpressionEval references {ident!r} which no binding in this query names")
                    if any(s in u for s in CAST_UTILS) and not re.search(r"\?\s*::", sql):
                        rep.warn(qw, f"{u} binding but no ?::type cast - the execute phase does not cast (write ?::jsonb / ?::vector / ?::int[])")
                    if u == "PrimaryIntGen" and terminals > 1 and re.match(r"\s*insert", sql, re.I):
                        rep.warn(qw, "PrimaryIntGen per-worker slices overlap by 1-2 keys: use PrimaryIntGenThroughput or RandomUniqueIntGen for multi-terminal inserts")
                    if optimal and u == "PrimaryIntGen" and re.match(r"\s*insert", sql, re.I):
                        rep.warn(qw, "optimalThreads + PrimaryIntGen inserts: use PrimaryIntGenThroughput (shared counter across iterations)")
                if nq == nb and not q.get("pattern_count") and not re.match(r"\s*insert", sql, re.I):
                    check_bind_ranges(sql, blist, load_ranges, rep, qw)
                if not raw and NON_EXPLAINABLE.match(sql):
                    if explain_on:
                        rep.warn(qw, "statement cannot be EXPLAINed: the pre-run EXPLAIN fails quietly (only a stack trace) and "
                                     "EXPLAIN output is lost for this and every later query in the file - set raw_sql: true "
                                     "on the workload or disable_explain: true")
                    if er.get("zeroRowsValidation", True) and not raw:
                        rep.warn(qw, "DDL/utility statements report 0 rows: set zeroRowsValidation: false")
                if not blist and re.search(r"\bwhere\b.*?(=|<|>|\bbetween\b|\bin\s*\()\s*'?-?\d", sql, re.I | re.S) \
                        and not re.match(r"\s*(insert|create|drop|alter)", sql, re.I):
                    rep.warn(qw, "hard-coded literal predicate: parameterise with ? + bindings so executions spread over the key space (#214)")
                if re.match(r"\s*update\b", sql, re.I) and explain_on:
                    rep.info(qw, "UPDATE runs 4x under EXPLAIN ANALYZE before measurement - keep it idempotent or budget for it")
                if q.get("explain-plan-rc-validation") is not None and (not data.get("collect_pg_stat_statements") or not explain_on):
                    rep.err(qw, "explain-plan-rc-validation needs collect_pg_stat_statements: true and EXPLAIN enabled")
                if re.search(r"\border\b(?!\s+by)", sql, re.I) and '"order"' not in sql:
                    rep.info(qw, "column named 'order'? quote it in queries (only the loader auto-quotes)")
    dup = {n for n in names if names.count(n) > 1}
    if dup:
        rep.err(where, f"duplicate workload names {sorted(dup)}")
    if optimal and len(ers) > 1:
        rep.info(where, "optimalThreads with several workloads: the pipeline runs them one by one via --workloads (fine); a plain local run exits")
    return {"workloads": names, "variant": variant, "data": data}


def cross_variant(paths, results, rep):
    groups = {}
    for p, res in zip(paths, results):
        if res and res["variant"]:
            groups.setdefault(Path(p).name, []).append((p, res))
    for name, items in groups.items():
        if len(items) < 2:
            continue
        base_p, base = next(((p, r) for p, r in items if r["variant"] == "yugabyte"), items[0])
        for p, r in items:
            if p == base_p:
                continue
            if r["workloads"] != base["workloads"]:
                rep.err(p, f"workload names differ from {base['variant']} copy: "
                           f"missing {sorted(set(base['workloads']) - set(r['workloads']))}, extra {sorted(set(r['workloads']) - set(base['workloads']))}")
            bv, ov = base["data"].get("yaml_version"), r["data"].get("yaml_version")
            if bv != ov:
                rep.warn(p, f"yaml_version {ov} differs from {base['variant']} copy ({bv})")
            bw = (base["data"].get("works") or {}).get("work") or {}
            ow = (r["data"].get("works") or {}).get("work") or {}
            for k in ("time_secs", "warmup"):
                if bw.get(k) != ow.get(k):
                    rep.warn(p, f"works.work.{k} {ow.get(k)} differs from {base['variant']} copy ({bw.get(k)})")
            brows = [x.get("rows") for x in as_list(((base["data"].get("microbenchmark") or {}).get("properties") or {}).get("loadRules")) if isinstance(x, dict)]
            orows = [x.get("rows") for x in as_list(((r["data"].get("microbenchmark") or {}).get("properties") or {}).get("loadRules")) if isinstance(x, dict)]
            if brows != orows:
                rep.warn(p, f"loadRules row counts {orows} differ from {base['variant']} copy {brows}")


def cmd_lint(args):
    root = repo_root(args.repo)
    utils = sorted(p.stem for p in (root / UTILS_DIR).glob("*.java") if p.stem not in NOT_UTILS)
    paths = []
    for a in args.paths:
        p = Path(a)
        if p.is_dir():
            paths.extend(sorted(p.rglob("*.yaml")))
        else:
            paths.append(p)
    # pull in sibling variants so cross-variant checks run even when one file is given
    if not args.no_siblings:
        extra = []
        for p in list(paths):
            for v in VARIANTS:
                s = sibling(p, v)
                if s and s.exists() and s.resolve() not in {x.resolve() for x in paths + extra}:
                    extra.append(s)
        paths += extra
    rep = Report()
    results = [lint_file(p, rep, utils) for p in paths]
    cross_variant(paths, results, rep)
    order = {"ERROR": 0, "WARN": 1, "INFO": 2}
    shown = [i for i in rep.items if not (args.quiet and i[0] == "INFO")]
    for lvl, w, m in sorted(shown, key=lambda x: (x[1].split(" ")[0], order[x[0]])):
        try:
            w = str(Path(w.split(" ")[0]).resolve().relative_to(root)) + (" " + " ".join(w.split(" ")[1:]) if " " in w else "")
        except ValueError:
            pass
        print(f"{lvl:5} {w}: {m}")
    n_err = sum(1 for i in rep.items if i[0] == "ERROR")
    n_warn = sum(1 for i in rep.items if i[0] == "WARN")
    print(f"\n{len(paths)} file(s): {n_err} error(s), {n_warn} warning(s)")
    return 1 if n_err else 0


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--repo", help="benchbase repo root (default: search upward from cwd)")
    sub = ap.add_subparsers(dest="cmd", required=True)
    s = sub.add_parser("next-id"); s.add_argument("category"); s.set_defaults(fn=cmd_next_id)
    s = sub.add_parser("variants"); s.add_argument("file")
    s.add_argument("--to", help="comma list of postgres,yb_colocated,yugabyte_range (default postgres,yb_colocated)")
    s.add_argument("--write", action="store_true"); s.set_defaults(fn=cmd_variants)
    s = sub.add_parser("lint"); s.add_argument("paths", nargs="+")
    s.add_argument("--no-siblings", action="store_true", help="do not auto-include sibling variant copies")
    s.add_argument("--quiet", action="store_true", help="hide INFO lines"); s.set_defaults(fn=cmd_lint)
    args = ap.parse_args()
    sys.exit(args.fn(args) or 0)


if __name__ == "__main__":
    main()
