package com.oltpbenchmark.benchmarks.featurebench.BindingFunctions;

import java.util.Random;

public class RandomNumber {

    final private int minimum;
    final private int maximum;

     public RandomNumber(Object... value) {
        this.minimum=(int) value[0];
        this.maximum=(int) value[0];
    }
    public int run() {

        int range_size = maximum - minimum + 1;
        int value = minimum;

        return value;
    }
}
