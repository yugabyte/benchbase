package com.oltpbenchmark.benchmarks.featurebench.BindingFunctions;

import java.util.List;

public class PrimaryStringGen implements BaseUtil {
    protected int currentValue;
    protected int desiredLength;

    protected int startNumber;

    protected String key;

    public PrimaryStringGen(List<Object> values) {
        this.startNumber = (int) values.get(0);
        this.currentValue = startNumber - 1;
        this.desiredLength = (int) values.get(1);
        if(desiredLength<=0)
        {
            throw new RuntimeException("Please use positive desired length for string primary keys");
        }
    }

    public String numberToIdString(int number) {
        StringBuilder baseNumberStr = new StringBuilder(String.valueOf(currentValue));
        while (baseNumberStr.length() < desiredLength) {
            baseNumberStr.append('a');
        }
        return baseNumberStr.toString();
    }

    @Override
    public Object run() {
        currentValue++;
        key = numberToIdString(currentValue);
        return key;
    }
}


