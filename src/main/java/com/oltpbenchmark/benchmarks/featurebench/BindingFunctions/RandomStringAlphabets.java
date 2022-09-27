package com.oltpbenchmark.benchmarks.featurebench.BindingFunctions;

import java.util.List;

public class RandomStringAlphabets implements BaseUtil {

    protected int desiredLength;

    public RandomStringAlphabets(List<Object> values) {
        this.desiredLength = (int) values.get(0);
    }

    @Override
    public Object run() {
        String AlphaString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvxyz";

        StringBuilder sb = new StringBuilder(desiredLength);
        for (int i = 0; i < desiredLength; i++) {
            int index = (int) (AlphaString.length() * Math.random());
            sb.append(AlphaString.charAt(index));
        }
        return sb.toString();
    }
}
