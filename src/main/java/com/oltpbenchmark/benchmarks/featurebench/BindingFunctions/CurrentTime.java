package com.oltpbenchmark.benchmarks.featurebench.BindingFunctions;

import java.sql.Timestamp;

public class CurrentTime implements BaseUtil {

    public CurrentTime() {

    }

    public Object run() {
        return new Timestamp(System.currentTimeMillis());
    }
}