package com.oltpbenchmark.benchmarks.featurebench.utils;


import java.text.SimpleDateFormat;
import java.util.List;

public class CurrentTimeString14 implements BaseUtil {

    protected static SimpleDateFormat DATE_FORMAT_14;

    public CurrentTimeString14(List<Object> values) {
        if (values.size() != 0) {
            throw new RuntimeException("Incorrect number of parameters for util function");
        }
        DATE_FORMAT_14 = new SimpleDateFormat("yyyyMMddHHmmss");
    }

    public Object run() {
        return com.oltpbenchmark.benchmarks.featurebench.utils.CurrentTimeString14.DATE_FORMAT_14.format(new java.util.Date());
    }
}