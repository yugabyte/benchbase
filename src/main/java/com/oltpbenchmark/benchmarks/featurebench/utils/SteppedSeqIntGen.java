package com.oltpbenchmark.benchmarks.featurebench.utils;


import java.util.List;


/*
Description :- Cyclic Sequential Integer Generator between a range with a fixed step.
Params :
1.int: lowerRange (values[0]) :- Lower Range (first value returned).
2.int: upperRange (values[1]) :- Upper Range (inclusive; returned only if it lies on the step).
3.int: step       (values[2]) :- Increment between consecutive values (> 0).

Eg:-
lowerRange:- 1, upperRange: 1000000, step: 100
Return type (Integer) :- lowerRange + k * step for every k such that the value is <= upperRange,
and repeats the sequence in cyclic way.
Sample output : 1 101 201 301 ... 999901 1 101 201 ...

With the worker-aware constructor, the steps are split into contiguous, non-overlapping
chunks per worker, and each worker cycles within its own chunk.
Eg:- [1, 1000000, 100] with 2 workers -> worker 0: 1 101 ... 499901, worker 1: 500001 ... 999901
*/

public class SteppedSeqIntGen implements BaseUtil {
    private final int lowerRange;
    private final int step;
    private final int firstIndex;
    private final int lastIndex;
    private int currentIndex;

    public SteppedSeqIntGen(List<Object> values) {
        this(values, 0, 1);
    }

    public SteppedSeqIntGen(List<Object> values, int workerId, int totalWorkers) {
        if (values.size() != 3) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass());
        }
        this.lowerRange = ((Number) values.get(0)).intValue();
        int upperRange = ((Number) values.get(1)).intValue();
        this.step = ((Number) values.get(2)).intValue();
        if (upperRange < lowerRange) {
            throw new RuntimeException("Upper bound less than lower bound");
        }
        if (step <= 0) {
            throw new RuntimeException("Step must be greater than 0");
        }

        long totalSteps = ((long) upperRange - lowerRange) / step + 1;
        long first = totalSteps * workerId / totalWorkers;
        long last = totalSteps * (workerId + 1) / totalWorkers - 1;
        if (last < first) {
            // More workers than steps: fall back to the full sequence
            first = 0;
            last = totalSteps - 1;
        }
        this.firstIndex = (int) first;
        this.lastIndex = (int) last;
        this.currentIndex = firstIndex - 1;
    }

    private int findNextIndex() {
        currentIndex++;
        //The current index reinitializes to firstIndex once it passes lastIndex
        if (currentIndex > lastIndex) {
            currentIndex = firstIndex;
        }
        return currentIndex;
    }

    @Override
    public Object run() {
        return lowerRange + findNextIndex() * step;
    }
}
