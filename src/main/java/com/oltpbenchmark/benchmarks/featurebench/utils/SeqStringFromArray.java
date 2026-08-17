package com.oltpbenchmark.benchmarks.featurebench.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/*
Description :- Chooses a string cyclically sequentially from an array of strings passed.
Params :
1.Array of Strings (values)
Eg:-
str :-  ["abc","hty","iki","pou","qwe"]
Return type (String) :- "abc" then "hty" then "iki" then "pou" then "qwe" then "abc" and so on (cyclically sequentially chosen).
*/


public class SeqStringFromArray implements BaseUtil {
    private List<String> str;
    private int currentIndex = 0;

    public SeqStringFromArray(List<Object> values) {
        if (values.isEmpty()) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass());
        }
        str = new ArrayList<>();
        for (Object value : values) {
            str.add(String.valueOf(value));
        }
    }
    public SeqStringFromArray(List<Object> values,int workerId,int totalWorkers) {
        if (values.isEmpty()) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass());
        }
        str = new ArrayList<>();
        for (Object value : values) {
            str.add(String.valueOf(value));
        }
    }

    public Object run() throws ClassNotFoundException, InvocationTargetException,
        NoSuchMethodException, InstantiationException, IllegalAccessException {
        try {
            int index = currentIndex % str.size();
            currentIndex = index + 1;
            return str.get(index);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
