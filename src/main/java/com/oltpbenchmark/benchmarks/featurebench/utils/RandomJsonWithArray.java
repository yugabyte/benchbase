package com.oltpbenchmark.benchmarks.featurebench.utils;

import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RandomJsonWithArray implements BaseUtil {
    protected int fields;
    protected int arrayLength;
    protected PrimaryStringGen  randomAlphabets;

    public RandomJsonWithArray(List<Object> values) {
        if (values.size() < 2) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass());
        }
        this.fields = (int)values.get(0);
        this.arrayLength = (int)values.get(1);
        ArrayList<Object> params = new ArrayList<>();
        params.add(0);
        params.add(20);
        randomAlphabets = new PrimaryStringGen(params);

    }

    @Override
    public Object run() throws ClassNotFoundException, InvocationTargetException,
        NoSuchMethodException, InstantiationException, IllegalAccessException {
        JSONObject outer = new JSONObject();

        for (int i = 0; i < fields; i++) {
            ArrayList<String> arrValues = new ArrayList<>();
            for (int j = 0; j < arrayLength; j++) {
                arrValues.add(randomAlphabets.run().toString());
            }
            outer.put(Integer.toString(i), arrValues);
        }
        return outer.toString();
    }
}
