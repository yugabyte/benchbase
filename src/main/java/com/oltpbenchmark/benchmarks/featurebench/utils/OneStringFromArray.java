package com.oltpbenchmark.benchmarks.featurebench.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;


public class OneStringFromArray implements BaseUtil {
    private List<String> str;

    public OneStringFromArray(List<Object> values) {
        if (values.size() == 0) {
            throw new RuntimeException("Incorrect number of parameters for util function");
        }
        str = new ArrayList<>();
        for (Object value : values) {
            str.add((String) value);
        }
    }

    public Object run() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        try {
            String s = str.get(new Random().nextInt(str.size()));
            return s;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}