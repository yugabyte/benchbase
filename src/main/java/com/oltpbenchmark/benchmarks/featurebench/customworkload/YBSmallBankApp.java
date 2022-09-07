package com.oltpbenchmark.benchmarks.featurebench.customworkload;

import com.oltpbenchmark.benchmarks.featurebench.FeatureBenchConstants;
import com.oltpbenchmark.benchmarks.featurebench.YBMicroBenchmark;
import com.oltpbenchmark.benchmarks.featurebench.util.*;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;


public class YBSmallBankApp extends YBMicroBenchmark implements FeatureBenchConstants {

    public final static Logger logger = Logger.getLogger(YBSmallBankApp.class);

    public YBSmallBankApp(HierarchicalConfiguration<ImmutableNode> config) {
        super(config);
        this.executeOnceImplemented = false;
        this.loadOnceImplemented = false;
        this.afterLoadImplemented = false;
    }

    @Override
    public void create(Connection conn) throws SQLException {

        Statement stmtOBj = null;
        stmtOBj = conn.createStatement();

        stmtOBj.executeUpdate("create table savings (accountid int primary key, balance float)");
        try {
            stmtOBj.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
//    {
//        "table": {
//        "name": "account",
//            "rows": 100000,
//            "columns": [
//        {
//            "name": "id",
//            "utilityFunction": {
//            "name": "randomString",
//                "params": [
//            {
//                "minLen": 8,
//                "maxLen": 16
//            }
//          ]
//        }
//        },
//        {
//            "name": "balance",
//            "utilityFunction": {
//            "name": "randomInteger",
//                "params": [
//            {
//                "minVal": 5000,
//                "maxVal": 100000
//            }
//          ]
//        }
//        }
//    ]
//    }
//    }


    @Override
    public ArrayList<LoadRule> loadRules() {
        long startIndex = 0;
        long endIndex = 10000;
        long fix_len = 20;
//        ParamsForUtilFunc paramsFunc = new ParamsForUtilFunc(startIndex,endIndex,fix_len);
        ParamsForUtilFunc paramsFunc = new ParamsForUtilFunc(new ArrayList<Integer>());
        ArrayList<ParamsForUtilFunc> list1 = new ArrayList<>();
        list1.add(paramsFunc);
        UtilityFunc uf = new UtilityFunc("", list1);
        ColumnsDetails cd = new ColumnsDetails("id", uf);
        ArrayList<ColumnsDetails> list2 = new ArrayList<ColumnsDetails>();
        list2.add(cd);
//        TableInfo ti=new TableInfo("",10,"",list2);
        TableInfo ti = new TableInfo(10, "sshaikh", list2);
        LoadRule lr = new LoadRule(ti);
        ArrayList<LoadRule> rule = new ArrayList<>();
        rule.add(lr);
        return rule;
    }

    /*public void loadOnce(WorkloadConfiguration wc, Connection conn){
        createCsvFile();
    }

    public void executeOnce(WorkloadConfiguration wc, Connection conn){
        createCsvFile();
    }*/

    public ArrayList<ExecuteRule> executeRules() {

        long startIndex = 0;
        long endIndex = 10000;
        long fix_len = 20;
//        ParamsForUtilFunc paramsFunc = new ParamsForUtilFunc(startIndex, endIndex, fix_len);
        ParamsForUtilFunc paramsFunc = new ParamsForUtilFunc(new ArrayList<Integer>());
        ArrayList<ParamsForUtilFunc> list1 = new ArrayList<>();
        list1.add(paramsFunc);
        UtilityFunc uf = new UtilityFunc("", list1);

        ArrayList<UtilityFunc> ufList = new ArrayList<>();
        ufList.add(uf);

        BindParams bd = new BindParams(ufList);
        ArrayList<BindParams> list2 = new ArrayList<>();
        list2.add(bd);
        QueryDetails qd = new QueryDetails("", list2);
        ArrayList<QueryDetails> list3 = new ArrayList<>();
        list3.add(qd);
        TransactionDetails td = new TransactionDetails("", 10, list3);
        ExecuteRule ob = new ExecuteRule(td);
        ArrayList<ExecuteRule> list4 = new ArrayList<>();
        list4.add(ob);
        return list4;
    }
//    {
//        "transaction": {
//        "name": "NewOrder",
//            "weight": 40,
//            "queries": [
//        {
//            "query": "select * from account where id > ? and balance > ?",
//            "bindParams": [
//            {
//                "utilityFunction": {
//                "name": "randomString",
//                    "params": [
//                {
//                    "minLen": 8,
//                    "maxLen": 16
//                }
//              ]
//            }
//            },
//            {
//                "utilityFunction": {
//                "name": "randomInteger",
//                    "params": [
//                {
//                    "minValue": 100000,
//                    "maxLen": 100000
//                }
//              ]
//            }
//            }
//        ]
//        },
//        {
//            "query": "select * from account where id = ?",
//            "bindParams": [
//            {
//                "utilityFunction": {
//                "name": "randomString",
//                    "params": [
//                {
//                    "minLen": 8,
//                    "maxLen": 16
//                }
//              ]
//            }
//            }
//        ]
//        }
//    ]
//    }
//    }

    @Override
    public void cleanUp(Connection conn) throws SQLException {

        Statement stmtOBj = null;
        stmtOBj = conn.createStatement();

        // DDL Statement - DROP TABLES
        logger.info("\n=======DROP ALL THE TABLES=======");
        stmtOBj.executeUpdate(DROP_TABLE_1);
/*        stmtOBj.executeUpdate(DROP_TABLE_2);
        stmtOBj.executeUpdate(DROP_TABLE_3);*/
        logger.info("\n=======TABLES ARE SUCCESSFULLY DROPPED FROM THE DATABASE=======\n");
        // DDL Statement DROP DATABASE
        logger.info("\n=======DROP DATABASE=======");
        stmtOBj.executeUpdate(DROP_DATABASE);
        logger.info("\n=======DATABASE IS SUCCESSFULLY DROPPED=======");
        try {

            stmtOBj.close();
            conn.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void loadOnce(Connection conn) {

    }

    @Override
    public void executeOnce(Connection conn) {

    }

}