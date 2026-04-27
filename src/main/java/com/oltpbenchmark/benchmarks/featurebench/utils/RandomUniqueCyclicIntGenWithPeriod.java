package com.oltpbenchmark.benchmarks.featurebench.utils;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


/*
Description :- Random Unique Cyclic Integer Generator between a range with
period semantics. Combines the behavior of RandomUniqueIntGen
(each value in the range is visited exactly once per pass, in random
order; the list is re-shuffled and iteration continues when exhausted)
with the period feature of CyclicSeqIntGenWithPeriod (each value in the
cycle is returned `period` times in a row before advancing).

Params :
1. int: lowerRange (values[0]) :- Lower Range.
2. int: upperRange (values[1]) :- Upper Range.
3. int: period     (values[2]) :- Number of consecutive calls that return the
                                  same value before advancing.

Eg:-
lowerRange: 1, upperRange: 3, period: 2
Sample output (one possible interleaving):
2 2 3 3 1 1   (pass 1 -- random permutation of [1..3], each value 2 times)
3 3 1 1 2 2   (pass 2 -- fresh random permutation)
...

Threading model (mirrors PrimaryIntGen / CyclicSeqIntGenWithPeriod):
The 3-arg (worker-aware) constructor uses a JVM-wide shared shuffled list
and shared cursor keyed on (lowerRange, upperRange, period). Every util
instance -- across all worker threads and across optimal-threads-search
iterations that re-create the worker pool -- draws the next value from
this same shared shuffle. When the cursor reaches the end, the first
worker to observe exhaustion re-shuffles the list under a lock and resets
the cursor; other workers retry without producing duplicate reshuffles.

Each instance still tracks its own `currentPeriod` and `currentValue` so
a single worker returns the same value for `period` consecutive calls
(e.g. the same table id is bound for all `count: <period>` inserts of one
transaction). When the period is exhausted the instance fetches the next
value from the shared shuffle.

The 2-arg (legacy / non-worker) constructor remains fully per-instance:
it owns its own shuffled list and reshuffles it on exhaust independently.
*/

public class RandomUniqueCyclicIntGenWithPeriod implements BaseUtil {

    /*
     * Shared, JVM-wide shuffle state keyed by the original
     * (lowerRange, upperRange, period) parameters. See class-level comment
     * for rationale; same pattern as PrimaryIntGen.SHARED_COUNTERS and
     * CyclicSeqIntGenWithPeriod.SHARED_VALUE_COUNTERS.
     */
    private static final ConcurrentHashMap<String, SharedShuffleState> SHARED_SHUFFLE_STATES =
        new ConcurrentHashMap<>();

    /**
     * Per-key shared state used by the worker-aware constructor. Holds the
     * current shuffled view, an atomic cursor into it, and a lock used only
     * to coordinate the re-shuffle when the cursor reaches the end.
     */
    private static final class SharedShuffleState {
        /**
         * The active shuffled view. Replaced (not mutated) on every
         * re-shuffle so concurrent readers always see a consistent
         * snapshot via the volatile field.
         */
        private volatile List<Integer> shuffled;
        private final AtomicInteger cursor = new AtomicInteger(0);
        private final Random random = new Random();
        private final Object reshuffleLock = new Object();

        SharedShuffleState(int lower, int upper) {
            List<Integer> initial = new ArrayList<>(upper - lower + 1);
            for (int i = lower; i <= upper; i++) {
                initial.add(i);
            }
            Collections.shuffle(initial, random);
            this.shuffled = initial;
        }

        int nextValue() {
            while (true) {
                List<Integer> view = shuffled;
                int idx = cursor.getAndIncrement();
                if (idx < view.size()) {
                    return view.get(idx);
                }
                // End of pass: only the first thread that finds the cursor
                // beyond the active view re-shuffles; others fall through
                // and retry against the fresh state.
                synchronized (reshuffleLock) {
                    if (cursor.get() >= shuffled.size()) {
                        List<Integer> fresh = new ArrayList<>(shuffled);
                        Collections.shuffle(fresh, random);
                        shuffled = fresh;
                        cursor.set(0);
                    }
                }
            }
        }
    }

    private final int period;

    /** Non-null only for the 2-arg (per-instance) constructor. */
    private final List<Integer> localShuffled;
    /** Non-null only for the 2-arg (per-instance) constructor. */
    private final Random localRandom;
    /** Non-null only for the 3-arg (worker-aware) constructor. */
    private final SharedShuffleState sharedState;

    private int localIndex;
    private int currentValue;
    private int currentPeriod;

    public RandomUniqueCyclicIntGenWithPeriod(List<Object> values) {
        if (values.size() != 3) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass());
        }

        int lower = ((Number) values.get(0)).intValue();
        int upper = ((Number) values.get(1)).intValue();
        this.period = ((Number) values.get(2)).intValue();
        validate(lower, upper, this.period);

        this.localShuffled = new ArrayList<>(upper - lower + 1);
        for (int i = lower; i <= upper; i++) {
            this.localShuffled.add(i);
        }
        this.localRandom = new Random();
        Collections.shuffle(this.localShuffled, this.localRandom);
        this.sharedState = null;

        this.localIndex = 0;
        this.currentValue = lower;
        // Force the first run() call to fetch the first value from the shuffle.
        this.currentPeriod = this.period;
    }

    public RandomUniqueCyclicIntGenWithPeriod(List<Object> values, int workerId, int totalWorkers) {
        if (values.size() != 3) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass());
        }

        int lower = ((Number) values.get(0)).intValue();
        int upper = ((Number) values.get(1)).intValue();
        this.period = ((Number) values.get(2)).intValue();
        validate(lower, upper, this.period);

        /*
         * Worker partitioning intentionally collapses into a single shared
         * shuffle (same rationale as PrimaryIntGen / CyclicSeqIntGenWithPeriod):
         * pre-dividing the range across workers works only if workers are
         * created exactly once, but the optimal-threads search re-creates
         * the worker pool each iteration. A shared shuffle + cursor keyed
         * on the original params lets every worker -- and every iteration --
         * continue the same monotonically advancing random walk through
         * the range, with re-shuffles at pass boundaries.
         */
        this.sharedState = SHARED_SHUFFLE_STATES.computeIfAbsent(
            stateKey(lower, upper, this.period),
            k -> new SharedShuffleState(lower, upper));
        this.localShuffled = null;
        this.localRandom = null;

        this.localIndex = 0;
        this.currentValue = lower;
        // Force the first run() call to fetch the first value from the shuffle.
        this.currentPeriod = this.period;
    }

    private static void validate(int lower, int upper, int period) {
        if (upper < lower) {
            throw new RuntimeException("Upper bound less than lower bound");
        }
        if (period <= 0) {
            throw new RuntimeException("Period must be greater than 0");
        }
    }

    @Override
    public Object run() {
        if (currentPeriod >= period) {
            currentPeriod = 0;
            currentValue = (sharedState != null) ? sharedState.nextValue() : nextLocal();
        }
        currentPeriod++;
        return currentValue;
    }

    private int nextLocal() {
        if (localIndex >= localShuffled.size()) {
            // End of pass: re-shuffle and start the next pass.
            Collections.shuffle(localShuffled, localRandom);
            localIndex = 0;
        }
        return localShuffled.get(localIndex++);
    }

    private static String stateKey(int lower, int upper, int period) {
        return lower + "_" + upper + "_" + period;
    }
}
