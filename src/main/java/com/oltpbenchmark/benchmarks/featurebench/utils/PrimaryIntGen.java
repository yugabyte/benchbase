package com.oltpbenchmark.benchmarks.featurebench.utils;


import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


/*
Description :- Integer Primary key generator between a range.
Params :
1.int: lowerRange (values[0]) :- Lower Range for Integer Primary key.
2.int: upperRange (values[1]) :- Upper Range for Integer Primary key.

Eg:-
lowerRange:- 10, upperRange: 20
Return type (Integer) :- Any value between 10 and 20 including these bounds.
*/

public class PrimaryIntGen implements BaseUtil {

    /*
     * Shared, JVM-wide counters keyed by the original (lowerRange, upperRange)
     * parameters. The 3-arg (worker-aware) constructor uses these so that
     * every worker thread - and every iteration of the optimal-threads
     * search that re-creates workers - draws from the SAME monotonically
     * increasing sequence. Without this, iteration N's worker 0 would
     * always restart at lowerRange and collide with iteration N-1's
     * worker 0, producing duplicate primary keys.
     */
    private static final ConcurrentHashMap<String, AtomicInteger> SHARED_COUNTERS =
        new ConcurrentHashMap<>();

    private final int upperRange;
    private final int lowerRange;
    private final AtomicInteger sharedCounter;
    private int currentValue;

    public PrimaryIntGen(List<Object> values) {
        if (values.size() != 2) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass());
        }
        this.currentValue = ((Number) values.get(0)).intValue() - 1;
        this.upperRange = ((Number) values.get(1)).intValue();
        this.lowerRange = ((Number) values.get(0)).intValue();
        if (upperRange < lowerRange) {
            throw new RuntimeException("Upper bound less than lower bound");
        }
        this.sharedCounter = null;
    }

    public PrimaryIntGen(List<Object> values, int workerId, int totalWorkers) {
        if (values.size() != 2) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass());
        }
        this.lowerRange = ((Number) values.get(0)).intValue();
        this.upperRange = ((Number) values.get(1)).intValue();
        if (upperRange < lowerRange) {
            throw new RuntimeException("Upper bound less than lower bound");
        }
        this.currentValue = this.lowerRange - 1;

        /*
         * Worker partitioning intentionally collapses into a single shared
         * counter: dividing the range up-front works only when workers are
         * created exactly once, but the optimal-threads search re-creates
         * the worker pool each iteration, which would reset every
         * partition's starting point and emit duplicate keys across
         * iterations. A shared atomic counter, scoped to the original
         * (lowerRange, upperRange) params, hands out unique values to all
         * workers across all iterations within the JVM.
         */
        this.sharedCounter = SHARED_COUNTERS.computeIfAbsent(
            counterKey(this.lowerRange, this.upperRange),
            k -> new AtomicInteger(this.lowerRange - 1));
    }

    private int findNextHigherValue() {
        currentValue++;
        return currentValue;
    }

    @Override
    public Object run() {
        if (sharedCounter != null) {
            int next = sharedCounter.incrementAndGet();
            if (next > upperRange) {
                throw new RuntimeException("Out of bounds primary key access");
            }
            return next;
        }
        if (currentValue >= upperRange) {
            throw new RuntimeException("Out of bounds primary key access");
        }
        currentValue = findNextHigherValue();
        return currentValue;
    }

    private static String counterKey(int lower, int upper) {
        return lower + "_" + upper;
    }
}
