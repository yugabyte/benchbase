package com.oltpbenchmark.benchmarks.featurebench.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Random;

public class RandomNstring extends Random implements BaseUtil {
    private final int minimum_length;
    private final int maximum_length;

    public RandomNstring(List<Object> values) {
        super((int) System.nanoTime());
        if (values.size() != 2) {
            throw new RuntimeException("Incorrect number of parameters for util function");
        }
        this.minimum_length = (int) values.get(0);
        this.maximum_length = (int) values.get(1);
        if (minimum_length > maximum_length || minimum_length == 0 && maximum_length == 0 || minimum_length < 0)
            throw new RuntimeException("Please enter correct bounds for max and min length");
    }

    /**
     * @returns a random numeric string with length in range [minimum_length,
     * maximum_length].
     */
    @Override
    public Object run() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException,
        InstantiationException, IllegalAccessException {
        return randomString(minimum_length, maximum_length, '0', 10);
    }

    private String randomString(int minimum_length, int maximum_length, char base, int numCharacters) {
        int length = number(minimum_length, maximum_length);
        byte baseByte = (byte) base;
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; ++i) {
            bytes[i] = (byte) (baseByte + number(0, numCharacters - 1));
        }
        return new String(bytes);
    }

    public int number(int minimum, int maximum) {

        int range_size = maximum - minimum + 1;
        int value = this.nextInt(range_size);
        value += minimum;

        return value;
    }
}
