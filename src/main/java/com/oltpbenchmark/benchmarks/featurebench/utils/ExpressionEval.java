package com.oltpbenchmark.benchmarks.featurebench.utils;

import com.oltpbenchmark.benchmarks.featurebench.helpers.SimpleExpressionEvaluator;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

/**
 * ExpressionEval utility for creating coupled parameters with expression-based relationships.
 * Supports arithmetic operations on named parameter references.
 * 
 * Example:
 *   bindings:
 *     - util: RandomNumber
 *       params: [1, 100]
 *       referenceName: minValue
 *     - util: ExpressionEval
 *       params: ["minValue + 10"]
 */
public class ExpressionEval implements BaseUtil {
    
    private final String expression;
    private Map<String, Object> context;


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
    }
    
    /**
     * Set the context map containing named variable references.
     * Must be called before run().
     */
    public void setContext(Map<String, Object> context) {
        this.context = context;
    }
    
    @Override
    public Object run() throws ClassNotFoundException, InvocationTargetException,
            NoSuchMethodException, InstantiationException, IllegalAccessException {
        
        if (context == null) {
            throw new RuntimeException("Context not set. ExpressionEval requires setContext() to be called before run()");
        }
        
        try {
            return SimpleExpressionEvaluator.evaluate(expression, context);
        } catch (Exception e) {
            throw new RuntimeException("Failed to evaluate expression: '" + expression + "'", e);
        }
    }
}
