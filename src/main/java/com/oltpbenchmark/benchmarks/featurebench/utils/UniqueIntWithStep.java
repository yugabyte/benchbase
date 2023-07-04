package com.oltpbenchmark.benchmarks.featurebench.utils;

import com.oltpbenchmark.DBWorkload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class UniqueIntWithStep implements BaseUtil {
    private final int upperLimit;
    private final int lowerLimit;
    private final int stepValue;
    private int currentValue;
    private static final Logger LOG = LoggerFactory.getLogger(DBWorkload.class);

    public UniqueIntWithStep(List<Object> values, int workerId, int totalWorkers) {
        if (values.size() != 3) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass());
        }
        int firstParam = ((Number) values.get(0)).intValue();
        int secondParam = ((Number) values.get(1)).intValue();
        int workerRange = (secondParam - firstParam) / totalWorkers;
        this.lowerLimit = firstParam + workerRange * workerId;
        this.currentValue = this.lowerLimit;
        this.upperLimit = lowerLimit + workerRange;
        stepValue = ((Number) values.get(2)).intValue();
        if (upperLimit < lowerLimit) {
            throw new RuntimeException("Upper bound less than lower bound");
        }
        if (stepValue >= (upperLimit - lowerLimit)) {
            throw new RuntimeException("Step value is larger than range");
        }
    }

    private int findNextHigherValue() {
        currentValue += stepValue;
        return currentValue;
    }

    @Override
    public Object run() {
        currentValue = findNextHigherValue();
        if (currentValue >= upperLimit) {
            throw new RuntimeException("Out of bounds primary key access. Current value: " + currentValue);
        }
        return currentValue;
    }
}
