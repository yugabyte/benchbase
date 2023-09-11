package com.oltpbenchmark.benchmarks.featurebench.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class ApplyAlias  implements BaseUtil {
    public ApplyAlias(List<Object> values) {
        super();

    }

    public ApplyAlias(List<Object> values, int workerId, int totalWorkers) {
        super();
    }
    @Override
    public Object run() throws ClassNotFoundException, InvocationTargetException,
        NoSuchMethodException, InstantiationException, IllegalAccessException {
        return 1;
    }
}
