package com.oltpbenchmark.benchmarks.featurebench.BindingFunctions;

import java.util.List;

public class RandomNumber implements BaseUtil{

    final private int minimum;
    final private int maximum;

     public RandomNumber(List<Object> value) {
        this.minimum=(int) value.get(0);
        this.maximum=(int) value.get(0);
    }

    @Override
    public Object run() {
        int range_size = maximum - minimum + 1;
        int value = minimum;
        return value;
    }
}