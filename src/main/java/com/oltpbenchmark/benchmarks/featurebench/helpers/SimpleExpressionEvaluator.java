package com.oltpbenchmark.benchmarks.featurebench.helpers;

import java.util.Map;
import java.util.Stack;

/**
 * Lightweight expression evaluator for basic arithmetic and string operations.
 * Supports: +, -, *, /, %, parentheses, and named variable references.
 * Optimized for performance with minimal overhead.
 */
public class SimpleExpressionEvaluator {
    
    /**
     * Evaluates an expression with named variable context.
     * 
     * @param expression The expression to evaluate (e.g., "param1 + 10")
     * @param context Map of variable names to their values
     * @return The evaluated result
     */
    public static Object evaluate(String expression, Map<String, Object> context) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new RuntimeException("Expression cannot be null or empty");
        }
        
        // Replace named references with their values
        String processedExpr = replaceReferences(expression, context);
        
        // Check if it's a simple value (no operators)
        if (isSimpleValue(processedExpr)) {
            return parseValue(processedExpr);
        }
        
        // Determine if it's string concatenation or numeric operation
        if (containsStringOperands(processedExpr, context)) {
            return evaluateStringExpression(processedExpr, context);
        } else {
            return evaluateNumericExpression(processedExpr);
        }
    }
    
    /**
     * Replace named references with their actual values
     */
    private static String replaceReferences(String expression, Map<String, Object> context) {
        String result = expression;
        
        // Sort by length descending to avoid partial replacements
        // e.g., "param10" should be replaced before "param1"
        java.util.List<String> sortedKeys = new java.util.ArrayList<>(context.keySet());
        sortedKeys.sort((a, b) -> Integer.compare(b.length(), a.length()));
        
        for (String key : sortedKeys) {
            Object value = context.get(key);
            if (value != null) {
                // Replace whole word only (not part of another word)
                result = result.replaceAll("\\b" + key + "\\b", 
                    value instanceof String ? "\"" + value + "\"" : String.valueOf(value));
            }
        }
        
        return result;
    }
    
    private static boolean isSimpleValue(String expr) {
        expr = expr.trim();
        return !expr.contains("+") && !expr.contains("-") && !expr.contains("*") 
            && !expr.contains("/") && !expr.contains("%") && !expr.contains("(");
    }
    
    private static Object parseValue(String value) {
        value = value.trim();
        
        // String literal
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        
        // Try to parse as number
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Integer.parseInt(value);
            }
        } catch (NumberFormatException e) {
            return value; // Return as string if not a number
        }
    }
    
    private static boolean containsStringOperands(String expr, Map<String, Object> context) {
        // Check if expression contains string literals
        if (expr.contains("\"")) {
            return true;
        }
        
        // Check if any referenced values are strings
        for (Object value : context.values()) {
            if (value instanceof String && expr.contains(String.valueOf(value))) {
                return true;
            }
        }
        
        return false;
    }
    
    private static String evaluateStringExpression(String expression, Map<String, Object> context) {
        // Simple string concatenation with +
        StringBuilder result = new StringBuilder();
        String[] parts = expression.split("\\+");
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            
            // Remove quotes if present
            if (part.startsWith("\"") && part.endsWith("\"")) {
                result.append(part.substring(1, part.length() - 1));
            } else {
                result.append(part);
            }
        }
        
        return result.toString();
    }
    
    /**
     * Evaluate numeric expression using Shunting Yard algorithm
     * Supports: +, -, *, /, %, parentheses
     */
    private static Object evaluateNumericExpression(String expression) {
        try {
            // Tokenize
            java.util.List<String> tokens = tokenize(expression);
            
            // Convert to postfix notation using Shunting Yard
            java.util.List<String> postfix = toPostfix(tokens);
            
            // Evaluate postfix expression
            return evaluatePostfix(postfix);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to evaluate expression: " + expression, e);
        }
    }
    
    private static java.util.List<String> tokenize(String expression) {
        java.util.List<String> tokens = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            
            if (Character.isWhitespace(c)) {
                continue;
            }
            
            if (isOperator(c) || c == '(' || c == ')') {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current = new StringBuilder();
                }
                tokens.add(String.valueOf(c));
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        
        return tokens;
    }
    
    private static boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '%';
    }
    
    private static int precedence(String op) {
        switch (op) {
            case "+":
            case "-":
                return 1;
            case "*":
            case "/":
            case "%":
                return 2;
            default:
                return 0;
        }
    }
    
    private static java.util.List<String> toPostfix(java.util.List<String> tokens) {
        java.util.List<String> output = new java.util.ArrayList<>();
        Stack<String> operators = new Stack<>();
        
        for (String token : tokens) {
            if (isNumeric(token)) {
                output.add(token);
            } else if (token.equals("(")) {
                operators.push(token);
            } else if (token.equals(")")) {
                while (!operators.isEmpty() && !operators.peek().equals("(")) {
                    output.add(operators.pop());
                }
                if (!operators.isEmpty()) {
                    operators.pop(); // Remove '('
                }
            } else if (isOperator(token.charAt(0)) && token.length() == 1) {
                while (!operators.isEmpty() && !operators.peek().equals("(") 
                       && precedence(operators.peek()) >= precedence(token)) {
                    output.add(operators.pop());
                }
                operators.push(token);
            }
        }
        
        while (!operators.isEmpty()) {
            output.add(operators.pop());
        }
        
        return output;
    }
    
    private static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private static Object evaluatePostfix(java.util.List<String> postfix) {
        Stack<Double> stack = new Stack<>();
        
        for (String token : postfix) {
            if (isNumeric(token)) {
                stack.push(Double.parseDouble(token));
            } else {
                if (stack.size() < 2) {
                    throw new RuntimeException("Invalid expression");
                }
                
                double b = stack.pop();
                double a = stack.pop();
                double result;
                
                switch (token) {
                    case "+":
                        result = a + b;
                        break;
                    case "-":
                        result = a - b;
                        break;
                    case "*":
                        result = a * b;
                        break;
                    case "/":
                        if (b == 0) {
                            throw new RuntimeException("Division by zero");
                        }
                        result = a / b;
                        break;
                    case "%":
                        result = a % b;
                        break;
                    default:
                        throw new RuntimeException("Unknown operator: " + token);
                }
                
                stack.push(result);
            }
        }
        
        if (stack.size() != 1) {
            throw new RuntimeException("Invalid expression");
        }
        
        double finalResult = stack.pop();
        
        // Return as integer if it's a whole number
        if (finalResult == Math.floor(finalResult) && !Double.isInfinite(finalResult)) {
            return (int) finalResult;
        }
        
        return finalResult;
    }
}

