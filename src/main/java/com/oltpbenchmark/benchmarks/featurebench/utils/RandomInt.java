package com.oltpbenchmark.benchmarks.featurebench.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Random;

public class RandomInt extends Random implements BaseUtil {

    private int minimum;
    private int maximum;

    public RandomInt(List<Object> values) {
        super((int) System.currentTimeMillis());
        if (values.size() != 2) {
            throw new RuntimeException("Incorrect number of parameters for util function");
        }
        this.minimum = ((Number) values.get(0)).intValue();
        this.maximum = ((Number) values.get(1)).intValue();
        if(maximum<minimum)
            throw new RuntimeException("Please enter correct values for min and max");
    }

    /**
     * Returns a random int value between minimum and maximum (inclusive)
     *
     * @param minimum
     * @param maximum
     * @returns a int in the range [minimum, maximum]. Note that this is inclusive.
     */
    @Override
    public Object run() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        int range_size = maximum - minimum + 1;
        int value = this.nextInt(range_size);
        value += minimum;
        return value;
    }
}
