package com.oltpbenchmark.benchmarks.featurebench.BindingFunctions;

import java.lang.reflect.InvocationTargetException;
import java.util.*;


public class OneStringFromArray implements BaseUtil {
    private List<String> str;

    public OneStringFromArray(List<Object> values) {
        str = new ArrayList<>();
        for (Object value : values) {
            str.add((String) value);
            System.out.println(value);
        }

    }

    public Object run() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        try {
            String s=str.get(new Random().nextInt(str.size()));
            System.out.println(s);
            return s;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return null;
    }
}
