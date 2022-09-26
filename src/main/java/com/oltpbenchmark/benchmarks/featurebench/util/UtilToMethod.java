package com.oltpbenchmark.benchmarks.featurebench.util;

import com.oltpbenchmark.benchmarks.featurebench.BindingFunctions.BaseUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class UtilToMethod {

    public Method run;
    public BaseUtil clsInstance;

    public UtilToMethod(Object util) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        String className = "com.oltpbenchmark.benchmarks.featurebench.BindingFunctions." + util;
        Class cls = Class.forName(className);
        List<Object> test=new ArrayList<>();
        test.add(2);
        this.clsInstance = (BaseUtil) cls.getDeclaredConstructor(List.class).newInstance(test);
    }
    public Object get() throws InvocationTargetException, IllegalAccessException {
        return this.clsInstance.run();
    }
}
