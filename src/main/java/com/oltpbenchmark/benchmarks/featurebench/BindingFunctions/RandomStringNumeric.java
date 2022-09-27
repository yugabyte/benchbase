package com.oltpbenchmark.benchmarks.featurebench.BindingFunctions;

import java.util.List;

public class RandomStringNumeric implements BaseUtil {
    protected int desiredLength;

    public RandomStringNumeric(List<Object> values) {
        this.desiredLength = (int) values.get(0);
    }


    @Override
    public Object run() {
        String NumericString = "0123456789";

        StringBuilder sb = new StringBuilder(desiredLength);
        for (int i = 0; i < desiredLength; i++) {
            int index = (int) (NumericString.length() * Math.random());
            sb.append(NumericString.charAt(index));
        }
        return sb.toString();
    }
}
