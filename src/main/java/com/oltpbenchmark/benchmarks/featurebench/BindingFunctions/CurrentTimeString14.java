package com.oltpbenchmark.benchmarks.featurebench.BindingFunctions;


import java.text.SimpleDateFormat;

public class CurrentTimeString14 implements BaseUtil {

    protected static SimpleDateFormat DATE_FORMAT_14;

    public CurrentTimeString14() {
        DATE_FORMAT_14 = new SimpleDateFormat("yyyyMMddHHmmss");
    }

    public Object run() {
        return com.oltpbenchmark.benchmarks.featurebench.BindingFunctions.CurrentTimeString14.DATE_FORMAT_14.format(new java.util.Date());
    }
}
