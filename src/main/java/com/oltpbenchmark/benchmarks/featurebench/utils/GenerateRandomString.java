package com.oltpbenchmark.benchmarks.featurebench.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GenerateRandomString implements BaseUtil {
    protected int desiredLength;
    protected int sizeOfStringArray;

    protected ArrayList<String> RandomStringGen;

    GenerateRandomString(List<Object> values) {
        if (values.size() != 2) {
            throw new RuntimeException("Incorrect number of parameters for util function");
        }
        this.desiredLength = ((Number) values.get(0)).intValue();
        this.sizeOfStringArray = ((Number) values.get(1)).intValue();
        if (desiredLength < 0 || sizeOfStringArray <= 0) {
            throw new RuntimeException("Please enter valid desired Length and size of string array for random picking");
        }
        RandomStringGen = new ArrayList<String>();
        this.generateList();
    }

    private void generateList() {
        for (int i = 0; i < sizeOfStringArray; i++) {
            RandomStringGen.add((String) new RandomStringAlphabets(List.of(desiredLength)).run());
            System.out.println(RandomStringGen.get(i));
        }
    }


    @Override
    public Object run() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return RandomStringGen.get(new Random().nextInt(RandomStringGen.size()));
    }
}


