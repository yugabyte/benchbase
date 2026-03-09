package com.oltpbenchmark.benchmarks.featurebench.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Random;

/**
 * Generates a deterministic random number based on a seed.
 * This ensures reproducible numbers for consistent benchmarking across runs.
 * 
 * Usage: SeededRandomNumber with params [minValue, maxValue, seed]
 * Example: SeededRandomNumber with params [1, 1000000, 12345]
 * generates a number between 1 and 1000000 using seed 12345
 */
public class SeededRandomNumber implements BaseUtil {

    private Integer minValue;
    private Integer maxValue;
    private long seed;

    public SeededRandomNumber(List<Object> values) {
        if (values.size() != 3) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass() + ". Expected: [minValue, maxValue, seed]");
        }
        this.minValue = ((Number) values.get(0)).intValue();
        this.maxValue = ((Number) values.get(1)).intValue();
        if (minValue > maxValue)
            throw new RuntimeException("Please enter correct bounds for max and min value");
        this.seed = ((Number) values.get(2)).longValue();
    }

    public SeededRandomNumber(List<Object> values, int workerId, int totalWorkers) {
        if (values.size() != 3) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass() + ". Expected: [minValue, maxValue, seed]");
        }
        this.minValue = ((Number) values.get(0)).intValue();
        this.maxValue = ((Number) values.get(1)).intValue();
        if (minValue > maxValue)
            throw new RuntimeException("Please enter correct bounds for max and min value");
        this.seed = ((Number) values.get(2)).longValue();
    }

    @Override
    public Object run() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Random random = new Random(seed);
        return random.nextInt((maxValue - minValue) + 1) + minValue;
    }
}
