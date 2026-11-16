package com.oltpbenchmark.benchmarks.featurebench.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/*
Description :- Random Unique Cyclic String Generator with period semantics.
String equivalent of RandomUniqueCyclicIntGen. Produces prefixed ID strings
("id-1000000", "id-1000001", ...) in a random order, visiting every value
in [lowerRange, upperRange] exactly once per pass. The list is re-shuffled
and iteration continues when exhausted. Each value is returned `period`
consecutive times before advancing.

Params :
1. int:    lowerRange (values[0]) :- Lower Range (inclusive).
2. int:    upperRange (values[1]) :- Upper Range (inclusive).
3. int/String: (values[2], optional) :- If int, treated as period (number of
                                        consecutive calls returning the same value).
                                        If String, treated as prefix (period defaults to 1).
4. String: prefix     (values[3], optional) :- Prefix for the generated string.
                                               Only used when values[2] is the period (int).
                                               Default: "id-"

Eg:-
lowerRange: 0, upperRange: 3, period: 2, prefix: "id-"
Sample output (one possible interleaving):
"id-1000002" "id-1000002" "id-1000000" "id-1000000" "id-1000003" "id-1000003" "id-1000001" "id-1000001"
(pass 1 -- random permutation of [0..3], each value 2 times)
"id-1000001" "id-1000001" "id-1000003" "id-1000003" "id-1000000" "id-1000000" "id-1000002" "id-1000002"
(pass 2 -- fresh random permutation)
...

Threading model: identical to RandomUniqueCyclicIntGen. The 3-arg
(worker-aware) constructor uses a JVM-wide shared shuffled list keyed on
(lowerRange, upperRange, period). Each instance tracks its own period
counter so a single worker returns the same string for `period` calls.
*/

public class RandomUniqueCyclicStringGen implements BaseUtil {

    private static final int OFFSET = 1000000;

    private static final ConcurrentHashMap<String, SharedShuffleState> SHARED_SHUFFLE_STATES =
        new ConcurrentHashMap<>();

    private static final class SharedShuffleState {
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
    private final String prefix;

    private final List<Integer> localShuffled;
    private final Random localRandom;
    private final SharedShuffleState sharedState;

    private int localIndex;
    private int currentValue;
    private int currentPeriod;

    private static int parsePeriod(List<Object> values) {
        if (values.size() >= 3 && values.get(2) instanceof Number) {
            return ((Number) values.get(2)).intValue();
        }
        return 1;
    }

    private static String parsePrefix(List<Object> values) {
        if (values.size() >= 3 && values.get(2) instanceof String) {
            return (String) values.get(2);
        }
        if (values.size() >= 4) {
            return values.get(3).toString();
        }
        return "id-";
    }

    public RandomUniqueCyclicStringGen(List<Object> values) {
        if (values.size() < 2 || values.size() > 4) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass()
                + ". Expected 2-4 params: [lowerRange, upperRange, period_or_prefix(optional), prefix(optional)]");
        }

        int lower = ((Number) values.get(0)).intValue();
        int upper = ((Number) values.get(1)).intValue();
        this.period = parsePeriod(values);
        this.prefix = parsePrefix(values);
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
        this.currentPeriod = this.period;
    }

    public RandomUniqueCyclicStringGen(List<Object> values, int workerId, int totalWorkers) {
        if (values.size() < 2 || values.size() > 4) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass()
                + ". Expected 2-4 params: [lowerRange, upperRange, period_or_prefix(optional), prefix(optional)]");
        }

        int lower = ((Number) values.get(0)).intValue();
        int upper = ((Number) values.get(1)).intValue();
        this.period = parsePeriod(values);
        this.prefix = parsePrefix(values);
        validate(lower, upper, this.period);

        this.sharedState = SHARED_SHUFFLE_STATES.computeIfAbsent(
            stateKey(lower, upper, this.period),
            k -> new SharedShuffleState(lower, upper));
        this.localShuffled = null;
        this.localRandom = null;

        this.localIndex = 0;
        this.currentValue = lower;
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
        return prefix + (OFFSET + currentValue);
    }

    private int nextLocal() {
        if (localIndex >= localShuffled.size()) {
            Collections.shuffle(localShuffled, localRandom);
            localIndex = 0;
        }
        return localShuffled.get(localIndex++);
    }

    private static String stateKey(int lower, int upper, int period) {
        return "str_" + lower + "_" + upper + "_" + period;
    }
}
