package com.oltpbenchmark.benchmarks.featurebench.helpers;

import com.oltpbenchmark.benchmarks.featurebench.utils.BaseUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UtilToMethod {
    private BaseUtil clsInstance;
    String utilName = "";

    String alias = null;

    Object params = null;

    public BaseUtil getInstance() {
        return clsInstance;
    }
    public UtilToMethod(Object util, Object params, int workerId, int totalWorkers) {
        this.utilName = (String) util;
        this.params=params;
        String className = "com.oltpbenchmark.benchmarks.featurebench.utils." + util;
        try {
            Class<?> cls = Class.forName(className);
            if (params == null) {
                params = new ArrayList<>(List.of());
            }
            this.clsInstance = (BaseUtil) cls.getDeclaredConstructor(
                List.class, int.class, int.class).newInstance((List) params, workerId, totalWorkers);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
            throw new RuntimeException(String.format("Oops! Are you sure that " +
                "you provided the right utility function name: %s ? ", util));
        } catch (InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    public UtilToMethod(Object util, Object params) {
        this.utilName = (String) util;
        this.params = params;
        String className = "com.oltpbenchmark.benchmarks.featurebench.utils." + util;
        try {
            Class<?> cls = Class.forName(className);
            if (params == null) {
                params = new ArrayList<>(List.of());
            }
            this.clsInstance = (BaseUtil) cls.getDeclaredConstructor(
                List.class).newInstance((List) params);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new RuntimeException(String.format("Oops! Are you sure that " +
                "you provided the right utility function name: %s ? ", util));
        } catch (InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    public Object get() throws InvocationTargetException, IllegalAccessException,
        ClassNotFoundException, NoSuchMethodException, InstantiationException {
        return this.clsInstance.run();
    }

    public void setAlias(String alias){
        this.alias = alias;
    }
    public String getAlias(){
        return this.alias;
    }
    public boolean isApplyAlias(){
        return Objects.equals(this.utilName, "ApplyAlias");
    }

    public Object getParams(){
        return  this.params;
    }
}
