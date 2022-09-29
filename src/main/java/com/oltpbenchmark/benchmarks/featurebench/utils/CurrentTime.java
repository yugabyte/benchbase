package com.oltpbenchmark.benchmarks.featurebench.utils;

import java.sql.Timestamp;
import java.util.List;

public class CurrentTime implements BaseUtil {

    public CurrentTime(List<Object> values) {
        if (values.size() != 0) {
            throw new RuntimeException("Incorrect number of parameters for util function");
        }
    }

    public Object run() {
        return new Timestamp(System.currentTimeMillis());
    }
}