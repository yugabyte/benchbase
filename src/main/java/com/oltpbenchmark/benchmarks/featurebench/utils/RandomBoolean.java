package com.oltpbenchmark.benchmarks.featurebench.utils;

import java.util.List;
import java.util.Random;

public class RandomBoolean implements BaseUtil {
    protected Random rd;

    public RandomBoolean(List<Object> values) {
        if (values.size() != 0) {
            throw new RuntimeException("Incorrect number of parameters for util function");
        }
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

