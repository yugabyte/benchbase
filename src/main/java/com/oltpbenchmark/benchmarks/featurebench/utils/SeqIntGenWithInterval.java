package com.oltpbenchmark.benchmarks.featurebench.utils;


import java.util.List;


public class SeqIntGenWithInterval implements BaseUtil {
    private final int upperRange;
    private final int lowerRange;
    private final int interval;
    private int currentValue, counter=-1;

    public SeqIntGenWithInterval(List<Object> values) {
        if (values.size() != 3) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass());
        }
        this.currentValue = ((Number) values.get(0)).intValue();
        this.upperRange = ((Number) values.get(1)).intValue();
        this.lowerRange = ((Number) values.get(0)).intValue();
        this.interval = ((Number) values.get(2)).intValue();
        if (upperRange < lowerRange) {
            throw new RuntimeException("Upper bound less than lower bound");
        }
    }

    public SeqIntGenWithInterval(List<Object> values, int workerId, int totalWorkers) {
        if (values.size() != 3) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass());
        }
        int divide = (((Number) values.get(1)).intValue() - ((Number) values.get(0)).intValue()) / totalWorkers;
        this.currentValue = ((Number) values.get(0)).intValue() + divide * workerId;
        int upperRangeTemp = (((Number) values.get(0)).intValue() + (divide) * (workerId + 1) + (workerId == 0 ? 0 : 1));
        this.upperRange = Math.min(upperRangeTemp, ((Number) values.get(1)).intValue());
        this.lowerRange = ((Number) values.get(0)).intValue() + divide * (workerId) + (workerId == 0 ? 0 : 1);
        this.interval = ((Number) values.get(2)).intValue();
        if (upperRange < lowerRange) {
            throw new RuntimeException("Upper bound less than lower bound");
        }

    }

    private int findNextHigherValue() {
        counter++;
        if(counter==interval)
        {
            currentValue++;
            counter=0;
        }
        return currentValue;
    }

    @Override
    public Object run() {
        currentValue = findNextHigherValue();
        if (currentValue > upperRange) {
            currentValue=1;
        }
        return currentValue;
    }
}