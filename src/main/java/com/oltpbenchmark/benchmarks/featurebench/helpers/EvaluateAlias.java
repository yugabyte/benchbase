package com.oltpbenchmark.benchmarks.featurebench.helpers;


import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import javax.script.ScriptException;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class EvaluateAlias {



    public EvaluateAlias(){

    }

     public void Evaluate( PreparedStatement stmt,  HashMap<String,Object> aliasValues,HashMap<Integer,Object> applyAlias) throws ScriptException {

         for (Map.Entry<Integer, Object> entry : applyAlias.entrySet()){
             System.out.println(entry.getKey());
             String exp = (String) ((List) entry.getValue()).get(0);
             Expression e = new ExpressionBuilder(exp).variables(aliasValues.keySet()).build();
             for (Map.Entry<String, Object> aliasValue : aliasValues.entrySet()){
                 e.setVariable(aliasValue.getKey(), (double) aliasValue.getValue());
             }
             double result = e.evaluate();
             System.out.println(result);

         }
//
    }
}
