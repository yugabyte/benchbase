package com.oltpbenchmark.benchmarks.featurebench.BindingFunctions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class RandomAstring implements BaseUtil {

    private int minimum_length;
    private int maximum_length;

    public RandomAstring(List<Object> values) {
        this.minimum_length = (int) values.get(0);
        this.maximum_length = (int) values.get(1);
    }

    /**
     * @returns a random alphabetic string with length in range [minimum_length,
     *          maximum_length].
     */
    @Override
    public Object run() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException,
            InstantiationException, IllegalAccessException {
        return (new RandomString(new ArrayList<>(List.of(minimum_length, maximum_length, 'a', 26)))).run();
    }

}
