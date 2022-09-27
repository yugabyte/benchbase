package com.oltpbenchmark.benchmarks.featurebench.BindingFunctions;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Random;

public class RandomString extends Random implements BaseUtil {

    private int minimum_length;
    private int maximum_length;
    private char base;
    private int numCharacters;

    public RandomString(List<Object> values) {
        super((int) System.currentTimeMillis());
        this.minimum_length = (int) values.get(0);
        this.maximum_length = (int) values.get(1);
        this.base = (char) values.get(2);
        this.numCharacters = (int) values.get(3);
    }

    public int number(int minimum, int maximum) {

        int range_size = maximum - minimum + 1;
        int value = this.nextInt(range_size);
        value += minimum;

        return value;
    }

    /**
     * @param minimum_length
     * @param maximum_length
     * @param base
     * @param numCharacters
     * @return
     */
    @Override
    public Object run() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException,
            InstantiationException, IllegalAccessException {
        int length = number(minimum_length, maximum_length);
        byte baseByte = (byte) base;
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; ++i) {
            bytes[i] = (byte) (baseByte + number(0, numCharacters - 1));
        }
        return new String(bytes);
    }
}
