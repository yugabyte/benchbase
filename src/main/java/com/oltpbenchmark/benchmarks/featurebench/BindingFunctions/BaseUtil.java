package com.oltpbenchmark.benchmarks.featurebench.BindingFunctions;

import java.lang.reflect.InvocationTargetException;

public interface BaseUtil {

    public Object run() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException;

}
