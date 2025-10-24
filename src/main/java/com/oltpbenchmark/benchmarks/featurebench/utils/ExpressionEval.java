package com.oltpbenchmark.benchmarks.featurebench.utils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * ExpressionEval utility for creating coupled parameters with expression-based relationships.
 * Supports arithmetic operations on previous binding values using $0, $1, $2, etc.
 * 
 * Example:
 *   bindings:
 *     - util: RandomNumber
 *       params: [1, 100]
 *     - expression: "$0 + 10"
 */
public class ExpressionEval implements BaseUtil {
    private static final ScriptEngineManager SCRIPT_ENGINE_MANAGER = new ScriptEngineManager();
    private static final ThreadLocal<ScriptEngine> SCRIPT_ENGINE_THREAD_LOCAL = 
        ThreadLocal.withInitial(() -> {
            ScriptEngine engine = SCRIPT_ENGINE_MANAGER.getEngineByName("JavaScript");
            if (engine == null) {
                // Try alternative names
                engine = SCRIPT_ENGINE_MANAGER.getEngineByName("nashorn");
                if (engine == null) {
                    engine = SCRIPT_ENGINE_MANAGER.getEngineByName("Nashorn");
                }
                if (engine == null) {
                    engine = SCRIPT_ENGINE_MANAGER.getEngineByName("js");
                }
            }
            if (engine == null) {
                throw new RuntimeException(
                    "No JavaScript engine available. For Java 15+, add Nashorn dependency: " +
                    "org.openjdk.nashorn:nashorn-core:15.4 or use GraalVM JavaScript.");
            }
            return engine;
        });
    
    private final String expression;
    private Object[] referencedValues;
    
    public ExpressionEval(List<Object> params) {
        if (params == null || params.isEmpty()) {
            throw new RuntimeException("ExpressionEval requires at least one parameter (the expression string)");
        }
        this.expression = (String) params.get(0);
        validateExpression(this.expression);
    }
    
    public ExpressionEval(List<Object> params, int workerId, int totalWorkers) {
        this(params);
    }
    
    private void validateExpression(String expr) {
        if (expr == null || expr.trim().isEmpty()) {
            throw new RuntimeException("Expression cannot be null or empty");
        }
        // Basic validation - check if expression contains at least one $ reference
        if (!expr.contains("$")) {
            throw new RuntimeException("Expression must reference at least one previous binding using $0, $1, etc.");
        }
    }
    
    public void setReferencedValues(Object[] values) {
        this.referencedValues = values;
    }
    
    @Override
    public Object run() throws ClassNotFoundException, InvocationTargetException,
            NoSuchMethodException, InstantiationException, IllegalAccessException {
        
        if (referencedValues == null) {
            throw new RuntimeException("Referenced values not set. ExpressionEval requires setReferencedValues() to be called before run()");
        }
        
        // Replace $0, $1, etc. with actual values
        String evaluableExpression = expression;
        for (int i = 0; i < referencedValues.length; i++) {
            Object value = referencedValues[i];
            if (value == null) {
                continue;
            }
            
            // Format the value appropriately for JavaScript evaluation
            String valueStr;
            if (value instanceof String) {
                // Escape single quotes and wrap in quotes
                valueStr = "'" + value.toString().replace("'", "\\'") + "'";
            } else {
                valueStr = value.toString();
            }
            
            evaluableExpression = evaluableExpression.replace("$" + i, valueStr);
        }
        
        // Evaluate using thread-local ScriptEngine for better performance
        ScriptEngine engine = SCRIPT_ENGINE_THREAD_LOCAL.get();
        
        try {
            Object result = engine.eval(evaluableExpression);
            
            // Convert result to appropriate Java type
            if (result instanceof Double) {
                double d = (Double) result;
                // Check if it's actually an integer value
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    return (int) d;
                }
                return d;
            } else if (result instanceof Integer) {
                return result;
            } else if (result instanceof Long) {
                return result;
            } else if (result instanceof Boolean) {
                return result;
            } else if (result instanceof String) {
                return result;
            } else {
                return result;
            }
        } catch (ScriptException e) {
            throw new RuntimeException(
                "Failed to evaluate expression: '" + expression + 
                "' (evaluated as: '" + evaluableExpression + "')", e);
        }
    }
}

