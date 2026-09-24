package com.oltpbenchmark.benchmarks.featurebench.utils;

import java.util.List;
import java.util.Random;

public class RandomJson implements BaseUtil {

    private final String jsonStr;

    public RandomJson(List<Object> values) {
        if (values.size() < 2) {
            throw new RuntimeException("Incorrect number of parameters for util function "
                + this.getClass());
        }
        int fields = (int) values.get(0);
        int valueLength = (int) values.get(1);

        byte[] alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".getBytes();
        Random rng = new Random();
        byte[] valBytes = new byte[valueLength];
        for (int i = 0; i < valueLength; i++) {
            valBytes[i] = alpha[rng.nextInt(52)];
        }
        String val = new String(valBytes);

        StringBuilder sb = new StringBuilder(2 + fields * (6 + valueLength));
        sb.append('{');
        for (int i = 0; i < fields; i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(i).append("\":\"").append(val).append('"');
        }
        sb.append('}');
        this.jsonStr = sb.toString();
    }

    public RandomJson(List<Object> values, int workerId, int totalWorkers) {
        this(values);
    }

    @Override
    public Object run() {
        return jsonStr;
    }
}
