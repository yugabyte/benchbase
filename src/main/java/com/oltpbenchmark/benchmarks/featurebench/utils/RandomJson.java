package com.oltpbenchmark.benchmarks.featurebench.utils;


import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

public class RandomJson implements BaseUtil {

    protected int fields;
    protected int nestedness;
    protected Object valueType;
    protected int valueLength;


    public RandomJson(List<Object> values) {
        if (values.size() != 4) {
            throw new RuntimeException("Incorrect number of parameters for util function");
        }
        this.fields = (int) values.get(0);
        this.nestedness = (int) values.get(1);
        this.valueType = values.get(2);
        this.valueLength = (int) values.get(3);
    }

    @Override
    public Object run() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        JSONObject outer = new JSONObject();
        for (int i = 0; i < fields; i++) {
            // JSONObject inner = new JSONObject();
            if (valueType.getClass().equals(String.class)) {
                outer.put(Integer.toString(i), new RandomStringAlphabets(Collections.singletonList(valueLength)).run());
            } else if (valueType.getClass().equals(Integer.class)) {
                outer.put(Integer.toString(i), new RandomStringNumeric(Collections.singletonList(valueLength)).run());
            } else if (valueType.getClass().equals(Boolean.class)) {
                outer.put(Integer.toString(i), new RandomBoolean(List.of()).run());
            }
        }
        return outer.toString();
    }
/*
    public static void main(String args[]) {
        RandomJson js = new RandomJson(4, 1, "a", 9);

        String json = js.getJsonAsString();
        System.out.println(json);

    }*/


}