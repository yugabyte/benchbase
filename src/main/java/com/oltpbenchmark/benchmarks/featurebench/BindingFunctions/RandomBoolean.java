package com.oltpbenchmark.benchmarks.featurebench.BindingFunctions;

import java.util.Random;

public class RandomBoolean implements BaseUtil {
    protected Random rd;

    public RandomBoolean() {
        Random rd = new Random();
    }

    @Override
    public Object run() {
        if (this.rd == null) {
            this.rd = new Random();
        }
        return rd.nextBoolean();
    }
}

