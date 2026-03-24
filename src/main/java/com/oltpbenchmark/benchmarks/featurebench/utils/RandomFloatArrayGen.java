package com.oltpbenchmark.benchmarks.featurebench.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates a random float array for vector operations.
 * Usage: RandomFloatArrayGen with params [arraySize, minValue, maxValue]
 * Example: RandomFloatArrayGen with params [512, 0.0, 1.0] generates a 512-dimensional vector with values between 0 and 1
 */
public class RandomFloatArrayGen implements BaseUtil {

    private List<Float> floatArray;

    private Float minValue;
    private Float maxValue;
    private int arraySize;

    public int getArraySize() {
        return arraySize;
    }

    public RandomFloatArrayGen(List<Object> values) {
        if (values.size() != 3) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass() + ". Expected: [arraySize, minValue, maxValue]");
        }
        this.arraySize = ((Number) values.get(0)).intValue();
        if (arraySize <= 0)
            throw new RuntimeException("Please enter positive integer array length");
        this.minValue = ((Number) values.get(1)).floatValue();
        this.maxValue = ((Number) values.get(2)).floatValue();
        if (minValue > maxValue)
            throw new RuntimeException("Please enter correct bounds for max and min value");
    }

    public RandomFloatArrayGen(List<Object> values, int workerId, int totalWorkers) {
        if (values.size() != 3) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass() + ". Expected: [arraySize, minValue, maxValue]");
        }
        this.arraySize = ((Number) values.get(0)).intValue();
        if (arraySize <= 0)
            throw new RuntimeException("Please enter positive integer array length");
        this.minValue = ((Number) values.get(1)).floatValue();
        this.maxValue = ((Number) values.get(2)).floatValue();
        if (minValue > maxValue)
            throw new RuntimeException("Please enter correct bounds for max and min value");
    }

    @Override
    public Object run() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Random random = new Random();
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
