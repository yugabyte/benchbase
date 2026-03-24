package com.oltpbenchmark.benchmarks.featurebench.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates a deterministic float array (vector) based on a seed.
 * This ensures reproducible vectors for consistent benchmarking across runs.
 * 
 * Usage: DeterministicVectorGen with params [arraySize, minValue, maxValue, seed]
 * Example: DeterministicVectorGen with params [512, 0.0, 1.0, 12345] 
 * generates a 512-dimensional vector with values between 0 and 1 using seed 12345
 */
public class DeterministicVectorGen implements BaseUtil {

    private List<Float> floatArray;

    private Float minValue;
    private Float maxValue;
    private int arraySize;
    private long seed;

    public int getArraySize() {
        return arraySize;
    }

    public DeterministicVectorGen(List<Object> values) {
        if (values.size() != 4) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass() + ". Expected: [arraySize, minValue, maxValue, seed]");
        }
        this.arraySize = ((Number) values.get(0)).intValue();
        if (arraySize <= 0)
            throw new RuntimeException("Please enter positive integer array length");
        this.minValue = ((Number) values.get(1)).floatValue();
        this.maxValue = ((Number) values.get(2)).floatValue();
        if (minValue > maxValue)
            throw new RuntimeException("Please enter correct bounds for max and min value");
        this.seed = ((Number) values.get(3)).longValue();
    }

    public DeterministicVectorGen(List<Object> values, int workerId, int totalWorkers) {
        if (values.size() != 4) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass() + ". Expected: [arraySize, minValue, maxValue, seed]");
        }
        this.arraySize = ((Number) values.get(0)).intValue();
        if (arraySize <= 0)
            throw new RuntimeException("Please enter positive integer array length");
        this.minValue = ((Number) values.get(1)).floatValue();
        this.maxValue = ((Number) values.get(2)).floatValue();
        if (minValue > maxValue)
            throw new RuntimeException("Please enter correct bounds for max and min value");
        this.seed = ((Number) values.get(3)).longValue();
    }

    @Override
    public Object run() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Random random = new Random(seed);
        floatArray = new ArrayList<>();
        for (int i = 0; i < arraySize; i++) {
            Float rd = random.nextFloat() * (maxValue - minValue) + minValue;
            floatArray.add(rd);
        }
        // Format as vector literal: [val1,val2,val3]
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < floatArray.size(); i++) {
            sb.append(floatArray.get(i));
            if (i < floatArray.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
