package com.oltpbenchmark.benchmarks.featurebench.helpers;

import com.oltpbenchmark.benchmarks.featurebench.utils.BaseUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class UtilToMethod {
    private BaseUtil clsInstance;
    private String referenceName = null;
    private boolean isExpression = false;

    public boolean isExpression() {
        return isExpression;
    }

    public void setExpression(boolean isExpression) {
        this.isExpression = isExpression;
    }

    public String getReferenceName() {
        return referenceName;
    }

    public void setReferenceName(String referenceName) {
        this.referenceName = referenceName;
    }

    public BaseUtil getInstance() {
        return clsInstance;
    }
    public UtilToMethod(Object util, Object params, int workerId, int totalWorkers, String referenceName) {
        String className = "com.oltpbenchmark.benchmarks.featurebench.utils." + util;
        try {
            Class<?> cls = Class.forName(className);
            if (params == null) {
                params = new ArrayList<>(List.of());
            }
            this.clsInstance = (BaseUtil) cls.getDeclaredConstructor(
                List.class, int.class, int.class).newInstance((List) params, workerId, totalWorkers);
            this.referenceName = referenceName;

            if (util.equals("ExpressionEval")) {
                this.isExpression = true;
            }
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

    public UtilToMethod(Object util, Object params, String referenceName) {
        String className = "com.oltpbenchmark.benchmarks.featurebench.utils." + util;
        try {
            Class<?> cls = Class.forName(className);
            if (params == null) {
                params = new ArrayList<>(List.of());
            }
            this.clsInstance = (BaseUtil) cls.getDeclaredConstructor(
                List.class).newInstance((List) params);
            this.referenceName = referenceName;

            if (util.equals("ExpressionEval")) {
                this.isExpression = true;
            }
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
}
