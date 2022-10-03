package com.oltpbenchmark.benchmarks.featurebench.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Random;

public class RandomFixedPoint extends Random implements BaseUtil {

    private int decimal_places;
    private double minimum;
    private double maximum;

    public RandomFixedPoint(List<Object> values) {

        super((int) System.nanoTime());
        if (values.size() != 3) {
            throw new RuntimeException("Incorrect number of parameters for util function");
        }
        this.decimal_places = (int) values.get(0);
        this.minimum = ((Number) values.get(1)).doubleValue();
        this.maximum = ((Number) values.get(2)).doubleValue();
        if (maximum < minimum)
            throw new RuntimeException("Please enter a correct range for min and max values");
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
