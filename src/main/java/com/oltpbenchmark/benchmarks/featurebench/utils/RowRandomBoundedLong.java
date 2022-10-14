package com.oltpbenchmark.benchmarks.featurebench.utils;


import java.util.List;


public class RowRandomBoundedLong implements BaseUtil {
    private final long lowValue;
    private final long highValue;

    public RowRandomBoundedLong(List<Object> values) {
        if (values.size() != 2) {
            throw new RuntimeException("Incorrect number of parameters for util function");
        }
        this.lowValue = ((Number) (int) values.get(2)).longValue();
        this.highValue = ((Number) (int) values.get(3)).longValue();
        if (lowValue > highValue)
            throw new RuntimeException("Please enter correct value for max and min value");
    }

    @Override
    public Object run() {
        return lowValue + (long) (Math.random() * (highValue - lowValue));
    }
}
