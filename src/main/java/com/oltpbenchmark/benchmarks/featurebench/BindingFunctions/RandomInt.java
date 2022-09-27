package com.oltpbenchmark.benchmarks.featurebench.BindingFunctions;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Random;

public class RandomInt extends Random implements BaseUtil {

    private int minimum;
    private int maximum;

    public RandomInt(List<Object> values) {
        super((int) System.currentTimeMillis());
        this.minimum = (int) values.get(0);
        this.maximum = (int) values.get(1);
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
