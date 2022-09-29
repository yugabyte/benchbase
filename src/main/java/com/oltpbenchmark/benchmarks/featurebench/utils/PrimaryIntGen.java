package com.oltpbenchmark.benchmarks.featurebench.utils;


import java.util.List;

public class PrimaryIntGen implements BaseUtil {
    protected int currentValue;
    protected int upperRange;
    protected int lowerRange;

    public PrimaryIntGen(List<Object> values) {
        if (values.size() != 2) {
            throw new RuntimeException("Incorrect number of parameters for util function");
        }
        this.currentValue = (int) values.get(0) - 1;
        this.upperRange = (int) values.get(1);
        this.lowerRange = (int) values.get(0);
        if(upperRange<lowerRange)
        {
            throw new RuntimeException("Upper bound less than lower bound");
        }
    }

    private int findNextHigherValue() {
        currentValue++;
        return currentValue;
    }

    @Override
    public Object run() {
        if (currentValue >= upperRange) {
            throw new RuntimeException("Out of bounds primary key access");
        }
        currentValue = findNextHigherValue();
        return currentValue;
    }
}
