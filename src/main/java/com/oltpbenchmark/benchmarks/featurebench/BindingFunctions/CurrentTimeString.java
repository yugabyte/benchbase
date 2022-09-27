package com.oltpbenchmark.benchmarks.featurebench.BindingFunctions;


import java.text.SimpleDateFormat;

public class CurrentTimeString implements BaseUtil {

    protected static SimpleDateFormat DATE_FORMAT;

    public CurrentTimeString() {
        DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    }

    public Object run() {
        return com.oltpbenchmark.benchmarks.featurebench.BindingFunctions.CurrentTimeString.DATE_FORMAT.format(new java.util.Date());
    }
}