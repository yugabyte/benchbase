package com.oltpbenchmark.benchmarks.featurebench.BindingFunctions;


public class PrimaryIntGen {
    private int currentValue;
    private  int upperRange;
    private  int lowerRange;

    public PrimaryIntGen(int lowerRange, int upperRange) {
        this.currentValue = lowerRange - 1;
        this.upperRange = upperRange;
        this.lowerRange = lowerRange;
    }

    public long getValue() {
        return currentValue;
    }

    public int getNextValue() {
        if (currentValue == upperRange) {
            throw new RuntimeException("Out of bounds primary key access");
        }
        currentValue = findNextHigherValue();
        return currentValue;
    }

    private int findNextHigherValue() {
        currentValue++;
        return currentValue;
    }

}
