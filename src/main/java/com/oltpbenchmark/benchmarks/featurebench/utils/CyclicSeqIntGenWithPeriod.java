package com.oltpbenchmark.benchmarks.featurebench.utils;


import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


/*
Description :- Cyclic Sequential Integer Generator between a range, where each
value in the cycle is returned `period` times in a row before advancing to
the next value.

Params :
1. int: lowerRange (values[0]) :- Lower Range.
2. int: upperRange (values[1]) :- Upper Range.
3. int: period     (values[2]) :- Number of consecutive calls that return the
                                  same value before advancing.

Eg:-
lowerRange: 1, upperRange: 3, period: 2
Sample output : 1 1 2 2 3 3 1 1 2 2 3 3 ...

Threading model (mirrors PrimaryIntGen):
The 3-arg (worker-aware) constructor uses a JVM-wide shared "next value"
counter keyed on (lowerRange, upperRange, period). Every util instance --
across all worker threads and across optimal-threads-search iterations
that re-create the worker pool -- draws the next value from this same
counter. This means a freshly-spawned worker continues the cycle from
where the previous worker left off rather than restarting at lowerRange.

Each instance still tracks its own `currentPeriod` so that a single worker
returns the same value for `period` consecutive calls (e.g. the same table
id is bound for all `count: <period>` inserts of one transaction). When
the period is exhausted the instance fetches the next value from the
shared counter atomically.
*/

public class CyclicSeqIntGenWithPeriod implements BaseUtil {

    /*
     * Shared, JVM-wide "next value" counters keyed by the original
     * (lowerRange, upperRange, period) parameters. See class-level comment
     * for rationale; same pattern as PrimaryIntGen.SHARED_COUNTERS.
     *
     * Stored as AtomicLong so the tick can grow unbounded without overflow
     * over long runs; the actual returned value is mod the range size.
     */
    private static final ConcurrentHashMap<String, AtomicLong> SHARED_VALUE_COUNTERS =
        new ConcurrentHashMap<>();

    private final int upperRange;
    private final int lowerRange;
    private final int period;
    private final int rangeSize;
    /** Non-null only for the worker-aware constructor. */
    private final AtomicLong sharedValueCounter;

    private int currentValue;
    private int currentPeriod;

    public CyclicSeqIntGenWithPeriod(List<Object> values) {
        if (values.size() != 3) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass());
        }
        this.lowerRange = ((Number) values.get(0)).intValue();
        this.upperRange = ((Number) values.get(1)).intValue();
        this.period = ((Number) values.get(2)).intValue();
        validate();
        this.rangeSize = upperRange - lowerRange + 1;
        this.sharedValueCounter = null;
        // Local-only state: first call advances currentValue from (lower-1) to lower.
        this.currentValue = this.lowerRange - 1;
        this.currentPeriod = this.period;
    }

    public CyclicSeqIntGenWithPeriod(List<Object> values, int workerId, int totalWorkers) {
        if (values.size() != 3) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass());
        }
        /*
         * Worker partitioning intentionally collapses into a single shared
         * counter (same rationale as PrimaryIntGen): pre-dividing the range
         * across workers works only if workers are created exactly once,
         * but the optimal-threads search re-creates the worker pool each
         * iteration. A shared atomic counter keyed on the original params
         * lets every worker -- and every iteration -- continue the same
         * monotonically advancing cycle.
         */
        this.lowerRange = ((Number) values.get(0)).intValue();
        this.upperRange = ((Number) values.get(1)).intValue();
        this.period = ((Number) values.get(2)).intValue();
        validate();
        this.rangeSize = upperRange - lowerRange + 1;
        this.sharedValueCounter = SHARED_VALUE_COUNTERS.computeIfAbsent(
            counterKey(this.lowerRange, this.upperRange, this.period),
            k -> new AtomicLong(-1L));
        // currentValue is fetched lazily from sharedValueCounter on the first
        // run() call; setting currentPeriod = period forces that fetch.
        this.currentValue = this.lowerRange - 1;
        this.currentPeriod = this.period;
    }

    private void validate() {
        if (upperRange < lowerRange) {
            throw new RuntimeException("Upper bound less than lower bound");
        }
        if (period <= 0) {
            throw new RuntimeException("Period must be greater than 0");
        }
    }

    private int advanceLocal() {
        currentValue++;
        if (currentValue > upperRange) {
            currentValue = lowerRange;
        }
        return currentValue;
    }

    private int advanceShared() {
        long tick = sharedValueCounter.incrementAndGet();
        // tick is monotonic; map it back into [lowerRange, upperRange] cyclically.
        long offset = ((tick % rangeSize) + rangeSize) % rangeSize;
        return (int) (lowerRange + offset);
    }

    @Override
    public Object run() {
        if (currentPeriod >= period) {
            currentPeriod = 0;
            currentValue = (sharedValueCounter != null) ? advanceShared() : advanceLocal();
        }
        currentPeriod++;
        return currentValue;
    }

    private static String counterKey(int lower, int upper, int period) {
        return lower + "_" + upper + "_" + period;
    }
}
