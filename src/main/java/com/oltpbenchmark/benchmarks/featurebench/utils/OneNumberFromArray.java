package com.oltpbenchmark.benchmarks.featurebench.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class OneNumberFromArray implements BaseUtil {
    private List<Integer> listOfIntegers;
    private final Random random;

    public OneNumberFromArray(List<Object> values) {
        if (values.size() == 0) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass());
        }
        listOfIntegers = new ArrayList<>();
        Long seed = extractSeed(values);
        List<Object> intValues = seed != null ? values.subList(0, values.size() - 1) : values;
        for (Object value : intValues) {
            listOfIntegers.add(convertToInteger(value));
        }
        this.random = seed != null ? new Random(seed) : new Random();
    }

    public OneNumberFromArray(List<Object> values, int workerId, int totalWorkers) {
        if (values.size() == 0) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass());
        }
        listOfIntegers = new ArrayList<>();
        Long seed = extractSeed(values);
        List<Object> intValues = seed != null ? values.subList(0, values.size() - 1) : values;
        for (Object value : intValues) {
            listOfIntegers.add(convertToInteger(value));
        }
        this.random = seed != null ? new Random(seed) : new Random();
    }

    /**
     * If the last element in values is a map with a "seed" key, extract and return it.
     * Otherwise return null, preserving the original unseeded behavior.
     */
    private Long extractSeed(List<Object> values) {
        Object last = values.get(values.size() - 1);
        if (last instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) last;
            if (map.containsKey("seed")) {
                return ((Number) map.get("seed")).longValue();
            }
        }
        return null;
    }

    private Integer convertToInteger(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Cannot convert value to Integer: " + value, e);
            }
        } else {
            throw new RuntimeException("Unsupported type for conversion to Integer: " + value.getClass());
        }
    }

    public Object run() throws ClassNotFoundException, InvocationTargetException,
        NoSuchMethodException, InstantiationException, IllegalAccessException {
        try {
            return listOfIntegers.get(random.nextInt(listOfIntegers.size()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}