package com.oltpbenchmark.benchmarks.featurebench.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class UtilToMethod {

    public Method run;
    public Object clsInstance;

    public UtilToMethod(Object util) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        String className = "com.oltpbenchmark.benchmarks.featurebench.BindingFunctions." + util;
        Class cls = Class.forName(className);
        List<Object> test=new ArrayList<>();
        test.add(2);
         this.clsInstance = cls.getDeclaredConstructor(List.class).newInstance(test);
         this.run = this.clsInstance.getClass().getDeclaredMethod("run");
    }
    public Object get() throws InvocationTargetException, IllegalAccessException {
        return this.run.invoke(this.clsInstance);
    }
}
