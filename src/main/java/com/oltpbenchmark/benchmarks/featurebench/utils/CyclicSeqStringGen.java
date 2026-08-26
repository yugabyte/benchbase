package com.oltpbenchmark.benchmarks.featurebench.utils;

import java.util.List;

/*
Description :- Cyclic Sequential String Generator that produces prefixed ID strings within a range.
Params :
1. int lowerRange (values[0]) :- Lower range (inclusive).
2. int upperRange (values[1]) :- Upper range (inclusive).
3. String prefix  (values[2], optional) :- Prefix for the generated string. Default: "id-"

Eg:-
lowerRange: 0, upperRange: 5, prefix: "id-"
Return type (String) :- "id-1000000", "id-1000001", ..., "id-1000005", "id-1000000", "id-1000001", ...

The numeric portion is offset by 1000000 to maintain fixed-width strings.
*/

public class CyclicSeqStringGen implements BaseUtil {
    private static final int OFFSET = 1000000;
    private final int upperRange;
    private final int lowerRange;
    private int currentValue;
    private final String prefix;

    public CyclicSeqStringGen(List<Object> values) {
        if (values.size() < 2 || values.size() > 3) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass() + ". Expected 2-3 params: [lowerRange, upperRange, prefix(optional)]");
        }
        this.lowerRange = ((Number) values.get(0)).intValue();
        this.upperRange = ((Number) values.get(1)).intValue();
        this.currentValue = this.lowerRange - 1;
        this.prefix = values.size() >= 3 ? values.get(2).toString() : "id-";
        if (upperRange < lowerRange) {
            throw new RuntimeException("Upper bound less than lower bound");
        }
    }

    public CyclicSeqStringGen(List<Object> values, int workerId, int totalWorkers) {
        if (values.size() < 2 || values.size() > 3) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass() + ". Expected 2-3 params: [lowerRange, upperRange, prefix(optional)]");
        }
        this.prefix = values.size() >= 3 ? values.get(2).toString() : "id-";
        int lo = ((Number) values.get(0)).intValue();
        int hi = ((Number) values.get(1)).intValue();
        int divide = (hi - lo) / totalWorkers;
        this.lowerRange = lo + divide * workerId + (workerId == 0 ? 0 : 1);
        int upperRangeTemp = lo + divide * (workerId + 1) + (workerId == 0 ? 0 : 1);
        this.upperRange = Math.min(upperRangeTemp, hi);
        this.currentValue = this.lowerRange - 1;
        if (upperRange < lowerRange) {
            throw new RuntimeException("Upper bound less than lower bound");
        }
    }

    @Override
    public Object run() {
        currentValue++;
        if (currentValue > upperRange) {
            currentValue = lowerRange;
        }
        return prefix + (OFFSET + currentValue);
    }
}
