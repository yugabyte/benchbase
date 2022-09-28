package com.oltpbenchmark.benchmarks.featurebench.BindingFunctions;

import java.lang.reflect.InvocationTargetException;
import java.util.*;


public class OneStringFromArray implements BaseUtil {
    private List<String> str;

    public OneStringFromArray(List<Object> values) {
        str = new ArrayList<>();
        for (Object value : values) {
            str.add((String) value);
        }
    }

    public Object run() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return str.get(new Random().nextInt(str.size()));
    }
}
