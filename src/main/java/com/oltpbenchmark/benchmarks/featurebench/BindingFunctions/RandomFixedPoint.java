package com.oltpbenchmark.benchmarks.featurebench.BindingFunctions;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Random;

public class RandomFixedPoint extends Random implements BaseUtil {

    private int decimal_places;
    private double minimum;
    private double maximum;

    public RandomFixedPoint(List<Object> values) {
        super((int) System.currentTimeMillis());
        this.decimal_places = (int) values.get(0);
        this.minimum = (double) values.get(1);
        this.maximum = (double) values.get(2);
    }

    public int number(int minimum, int maximum) {

        int range_size = maximum - minimum + 1;
        int value = this.nextInt(range_size);
        value += minimum;

        return value;
    }

    /**
     * @param decimal_places
     * @param minimum
     * @param maximum
     * @return
     */
    @Override
    public Object run() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException,
            InstantiationException, IllegalAccessException {
        int multiplier = 1;
        for (int i = 0; i < decimal_places; ++i) {
            multiplier *= 10;
        }

        int int_min = (int) (minimum * multiplier + 0.5);
        int int_max = (int) (maximum * multiplier + 0.5);

        return (double) this.number(int_min, int_max) / (double) multiplier;
    }
}