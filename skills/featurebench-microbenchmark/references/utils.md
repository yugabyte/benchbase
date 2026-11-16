# FeatureBench util (generator) catalog

This catalog is verified against the source in `src/main/java/com/oltpbenchmark/benchmarks/featurebench/utils/`. The repo's `featurebench/Readme.md` misses 12 of these utils and describes several of them wrongly, so trust this file or the Java source over the Readme.

Utils are named in YAML by their simple class name, and the name is case-sensitive.

- **Load phase** (`loadRules` columns): each column gets one instance from the 1-arg constructor.
- **Execute phase** (`bindings`): each worker gets one instance per binding from the 3-arg constructor `(params, workerId, totalWorkers)`.
- A binding with `count: N` creates N independent instances, so N `PrimaryIntGen` instances all yield the same sequence.
- A query with `pattern_count: N` reuses the same instances, so a sequential generator keeps advancing across the repeats.

Column key: **Seq** = sequential and stateful; **Rand** = random; **Det** = returns the same value on every call.

## Contents
1. Integer keys and numbers
2. Strings
3. Floats and decimals
4. Dates and times
5. Arrays, JSON, vectors
6. Special: ExpressionEval, RangeDivider
7. Choosing a generator (cheat sheet)

## 1. Integer keys and numbers

| Util | Params | Returns | Kind | Execute-phase worker split | Use / pitfalls |
|---|---|---|---|---|---|
| PrimaryIntGen | `[lo, hi]` | int lo..hi, +1 per call | Seq | yes, but the slices overlap by 1-2 keys and never reach the top | The standard load PK (`rows` must be ≤ hi-lo+1). **Throws "Out of bounds" when exhausted**, which kills the worker. For execute-phase INSERTs, size hi ≥ terminals × rate × (warmup+time). |
| PrimaryIntGenThroughput | `[lo, hi]` | int | Seq, **JVM-wide shared counter** | no (shared) | INSERT keys for multi-terminal / `optimalThreads` runs, with no duplicates across workers or iterations. Throughput files use `[10000001, 2147483646]`. |
| RandomUniqueIntGen | `[lo, hi]` | int, shuffled permutation, no repeats | Rand, unique | yes, exact disjoint chunks | Unique keys in random order. Holds the whole list in memory. Throws when exhausted. #214 replaced it with PrimaryIntGen for id columns. |
| CyclicSeqIntGen | `[lo, hi]` | int lo..hi, then wraps | Seq, cyclic | yes, each worker wraps within its own slice | Loading gives exact uniform cardinality (hi-lo+1 distinct values). Executing sweeps keys evenly. Never throws. |
| RandomUniqueCyclicIntGen | `[lo, hi, period?=1]` | int; each value repeated `period` times in a row, every value once per pass, reshuffled per pass | Rand, cyclic | no, JVM-wide shared shuffle | "K distinct values × `period` rows each", e.g. `[1000001, 1000050, 10000]` = 50 values × 10k rows. |
| RandomInt | `[min, max]` | int, inclusive | Rand | no | Random lookup keys in execute bindings. Avoid it in loadRules for filtered columns. |
| RandomNumber | `[min, max]` | int, inclusive | Rand | no | Same as RandomInt, but without a min ≤ max check. |
| RowRandomBoundedInt | `[lo, hi]` | int, inclusive | Rand | no | Same as RandomNumber. |
| PrimaryIntRandomForExecutePhase | `[lo, hi]` | int, **hi exclusive**, random within a worker-private slice | Rand | yes | **Execute only** (it has no load constructor). Random keys with no cross-worker contention. |
| RandomLong | `[min, max]` | long, inclusive | Rand | no | bigint values beyond 2^31. |
| RowRandomBoundedLong | `[lo, hi]` | long, hi effectively exclusive | Rand | no | |
| SeededRandomNumber | `[min, max, seed]` | int, **the same value every call** | Det | no | A reproducible constant. Not a random stream. |
| OneNumberFromArray | `[v1, v2, ...]` | a random pick | Rand | no | Fine for a single constant `[1000000]` or a tiny enum. #214 replaced long literal lists with CyclicSeqIntGen. |

## 2. Strings

| Util | Params | Returns | Kind | Notes |
|---|---|---|---|---|
| RandomAString | `[minLen, maxLen]` | lowercase a-z | Rand | The most common filler. **Use `[N, N]`** for a fixed row size. `[0,0]` throws. |
| PrimaryStringGen | `[start, length]` | `"1aaaa"`: counter padded with `a` | Seq | Text PKs in load. Buggy in execute (param[1] is misread as an upper range). |
| HashedPrimaryStringGen | `[start, length]` | MD5(counter), repeated or truncated to length | Seq | Unique, non-monotonic text PK. |
| RandomPKString | `[start, end, length]` | random int padded with `a` | Rand | Lookups against PrimaryStringGen keys. |
| RandomString | `[min, max, length]` | same format as RandomPKString | Rand | |
| HashedRandomString | `[min, max, length]` | MD5 of a random int | Rand | Lookups against HashedPrimaryStringGen keys. |
| CyclicSeqStringGen | `[lo, hi, prefix?="id-"]` | `prefix + (1000000+v)`, cyclic | Seq | String twin of CyclicSeqIntGen (#214). `[1, 1000]` gives `id-1000001..id-1001000`. |
| RandomUniqueCyclicStringGen | `[lo, hi, period?, prefix?]` | `prefix + (1000000+v)` | Rand, cyclic | String twin of RandomUniqueCyclicIntGen. |
| OneStringFromArray | `[s1, ...]` | a random pick | Rand | Enums, or one constant (e.g. CW2's `["addr for the client"]`, so updates write the same bytes). |
| RandomStringAlphabets | `[len]` | mixed-case letters (no `w`) | Rand | |
| RandomStringNumeric | `[len]` | digits | Rand | |
| RandomNstring | `[min, max]` | digits, variable length | Rand | |
| RandomUUID | `[]` | `java.util.UUID` | Rand | |
| OneUUIDFromArray | `[uuid, ...]` | UUID | Rand | |
| RandomBytea | `[min, max]` | `byte[]` (not hex) | Rand | bytea columns. |
| RandomBoolean | `[]` | Boolean | Rand | |

## 3. Floats and decimals

| Util | Params | Returns | Notes |
|---|---|---|---|
| RandomNoWithDecimalPoints | `[lo≥0, hi, decimals]` | double in [lo, hi) | **Use this one** for float/numeric columns. |
| RandomFixedPoint | `[decimals, min, max]` | double; **decimals come first** | numeric. |
| RandomFloat | `[lo, hi, decimals]` | **π × randint(lo..hi)**, so not uniform in [lo,hi] | Misleading. Prefer RandomNoWithDecimalPoints. |
| PrimaryFloatGen | `[lo, hi, decimals]` | π × counter, unique and increasing | Unique float keys. |

## 4. Dates and times

| Util | Params | Returns | Notes |
|---|---|---|---|
| RandomDateBtwYears | `[yearLo, yearHi]` | LocalDate (day 1-28) | **The util for "a date between years".** |
| RandomDate | `[numberOfDays, offsetDays?]` | 2023-01-01 + offset + rand(days) | **Not years:** `[2000, 2026]` gives dates starting around 2028. |
| PrimaryDateGen | `[days, offset?]` | 2023-01-01 + offset + counter | Unique dates. |
| CurrentTime | `[]` | Timestamp now | created_at. Time-dependent. |
| RandomTimestamp / …WithoutTimeZone / …WithTimeZone | `[n]` | random point on a ~2.78 h grid from 2022-12-31 | Random (the Readme's "deterministic" is wrong). |
| RandomTimestampWithTimezoneBetweenDates | `["2023-01-01T00:00:00", "2023-12-31T00:00:00"]` | uniform between the two | |
| RandomTimestampWithTimezoneBtwMonths | `[year, m1, m2]` | | |

## 5. Arrays, JSON, vectors

**The loader casts these values automatically. The execute phase does not**, so queries must write the cast themselves: `?::jsonb`, `?::int[]`, `?::vector`.

| Util | Params | Returns | Loader cast |
|---|---|---|---|
| RandomJson | `[fields, valueLen, nest?]` (nest is ignored) | JSON string | `?::JSON` |
| RandomIntegerArrayGen | `[size, min, max]` | `"{1, 5, ...}"` | `?::int[]` |
| RandomLongArrayGen | `[size, min, max]` | | `?::bigint[]` |
| RandomTextArrayGen | `[size, minLen, maxLen]` | | `?::text[]` |
| RandomFloatArrayGen | `[dim, min, max]` | `"[0.1,...]"` | `?::vector(dim)`. Use it to load embeddings. |
| DeterministicVectorGen | `[dim, min, max, seed]` | **the identical vector on every call** | `?::vector(dim)`. Use it for a fixed ANN query vector, never for loading. |

Loader bug: the cast is looked up by column index before `count` expansion. Put array/json/vector columns **before** any column that has `count`.

## 6. Special

**ExpressionEval** `["expr"]` computes a value from other bindings of the *same query* that carry a `referenceName`. It supports `+ - * / %` and parentheses, string concatenation, and `date ± days`. A whole-number result becomes an `int` (it overflows past 2^31).
```yaml
- util: RandomInt
  params: [1, 999900]
  referenceName: lo
- util: ExpressionEval
  params: ["lo + 100"]         # BETWEEN ? AND ? always scans exactly 101 keys
```

**RangeDivider** `[total]` (**execute only**) returns `workerId × (total / terminals)`, a constant per worker. Pair it with ExpressionEval to give each terminal a disjoint key window (see the skiplocked `SL_*` files).

**Binding forms:**
- `count: K` on a binding fills K consecutive `?`s, e.g. for `IN (?,?,?,?,?)`.
- The mapping form (`bindings: {count: 5, util: RandomInt, params: [...]}`) also works.
- `split_min_max_for_count: K` with `params: [min, max]` gives K instances over disjoint sub-ranges, so the K values are distinct.

## 7. Cheat sheet: choosing a generator

| Need | Use |
|---|---|
| Load PK | `PrimaryIntGen [1, N]` with `rows: N` |
| Load a filter/join/group column with exact cardinality K | `CyclicSeqIntGen [base+1, base+K]` |
| "K values × M rows each", random order | `RandomUniqueCyclicIntGen [lo, lo+K-1, M]` |
| Load filler text | `RandomAString [L, L]` (fixed length) |
| Load float | `RandomNoWithDecimalPoints [1, 1000000, 2]` |
| Execute point lookup | `RandomInt [1, N]` (random) or `CyclicSeqIntGen [1, N]` (even sweep) |
| Execute fixed-width range | `RandomInt` + `referenceName` + `ExpressionEval "x + W"`, with the RandomInt upper bound = hi - W |
| Execute IN-list of K | binding `count: K`, or `split_min_max_for_count: K` for distinct values |
| Execute INSERT, one terminal | `PrimaryIntGen [N+1, big]` |
| Execute INSERT, many terminals / optimalThreads | `PrimaryIntGenThroughput [N+1, 2147483646]` |
| Execute UPDATE of existing rows | `CyclicSeqIntGen [1, N]` (#187 fixed zero-row updates this way) |
| Per-terminal disjoint window | `RangeDivider [N]` + `ExpressionEval` |
| Multi-row INSERT | `VALUES [(?,?,?)][pattern_count]` + `pattern_count: K` |

If no existing util fits, add one under `utils/`:
- Write a Javadoc-style header (`Description / Params / Eg / Return type`).
- Provide both constructors, `(List<Object>)` and `(List<Object>, int workerId, int totalWorkers)`.
- Validate the parameter count and throw `RuntimeException("Incorrect number of parameters for util function " + getClass())`.
- **Add a row to `featurebench/Readme.md`.** Most recent utils skipped this step.
