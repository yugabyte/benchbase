package com.oltpbenchmark.benchmarks.featurebench.customworkload;


import com.oltpbenchmark.benchmarks.featurebench.util.*;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;


public class YBMicroBenchmarkImplementation implements YBMicroBenchmark{

    public final static Logger logger = Logger.getLogger(YBMicroBenchmarkImplementation.class);

    @Override
    public void createDB(Connection conn) throws SQLException {

        Statement stmtOBj;
        stmtOBj = conn.createStatement();

        // DDL Statement 1 - DROP EXISTING TABLES IF THEY EXIST
        logger.info("\n=======DROPPING TABLES IF THEY EXIST=======");
        String DROP_QUERY_1="DROP TABLE IF EXISTS accounts";
        stmtOBj.executeUpdate(DROP_QUERY_1);
        logger.info("\n=======DATABASE IS SUCCESSFULLY CREATED=======\n");
        String CREATE_TABLE_1 = " CREATE TABLE accounts ("
            +"id int NOT NULL,"
            +"name varchar(64) NOT NULL,"
            +"CONSTRAINT pk_accounts PRIMARY KEY (id)"
            +");";
        stmtOBj.execute(CREATE_TABLE_1);
        String INDEX_TABLE_1= "CREATE INDEX idx_accounts_name ON accounts (name);";
        stmtOBj.execute(INDEX_TABLE_1);

        try {
            stmtOBj.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public ArrayList<LoadRule> loadRule(HierarchicalConfiguration<ImmutableNode> properties) {
        // range for the primary key
        int startIndex = 0;
        int endIndex = 10000;
        ArrayList<Integer> range = new ArrayList<>();
        range.add(startIndex);
        range.add(endIndex);
        // util parameter for int primary key generation.
        ParamsForUtilFunc paramsFunc1 = new ParamsForUtilFunc(range);
        //desired length of the name of account user added to below list
        ArrayList<Integer> string_len = new ArrayList<>();
        int desired_len = 10;
        string_len.add(desired_len);
        //util parameter for string name generation
        ParamsForUtilFunc paramsFunc2 = new ParamsForUtilFunc(string_len);
        //ArrayList for above parameters for util function
        ArrayList<ParamsForUtilFunc> list1 = new ArrayList<>();
        ArrayList<ParamsForUtilFunc> list2 = new ArrayList<>();
        list1.add(paramsFunc1);
        list2.add(paramsFunc2);
        // Initializing the util functions with their names and above parameters.
        UtilityFunc uf1 = new UtilityFunc("get_int_primary_key", list1);
        UtilityFunc uf2 = new UtilityFunc("numberToIdString", list2);
        //binding the column details(name) with the utility functions
        ColumnsDetails cd1 = new ColumnsDetails("id", uf1);
        ColumnsDetails cd2 = new ColumnsDetails("name", uf2);
        //All the column details for a table is stored as arrayList.
        ArrayList<ColumnsDetails> col_det = new ArrayList<>();
        col_det.add(cd1);
        col_det.add(cd2);
        //add table information to the tableInfo object:- no_of_rows, table_name and column details
        TableInfo ti = new TableInfo(10, "accounts", col_det);
        //creating a load rule for the above table info.
        LoadRule lr = new LoadRule(ti);
        ArrayList<LoadRule> rule = new ArrayList<>();
        rule.add(lr);
        //return load rule
        return rule;

    }

    public ArrayList<ExecuteRule> executeRule(HierarchicalConfiguration<ImmutableNode> properties) {
        //range for query
        int startIndex = 1;
        int endIndex = 10;
        ArrayList<Integer> range = new ArrayList<>();
        range.add(startIndex);
        range.add(endIndex);
        //parameter for id querying passed.
        ParamsForUtilFunc paramsFunc1 = new ParamsForUtilFunc(range);
        ArrayList<ParamsForUtilFunc> list1 = new ArrayList<>();
        list1.add(paramsFunc1);
        //utility function for querying within range initialized
        UtilityFunc uf1 = new UtilityFunc("Random_no_within_range", list1);
        ArrayList<UtilityFunc> list_of_util = new ArrayList<>();
        list_of_util.add(uf1);
        //All the utility functions for a query bound together.
        BindParams bd = new BindParams(list_of_util);
        //query to be executed
        String query = "SELECT * FROM ACCOUNTS WHERE ID > ?;";
        ArrayList<BindParams> bp = new ArrayList<>();
        bp.add(bd);
        //Query details and the util functions with their parameters passed.
        QueryDetails qd = new QueryDetails(query, bp);
        ArrayList<QueryDetails> list3 = new ArrayList<>();
        list3.add(qd);
        //Transaction details with name, weight of transaction and the list of all query details passed.
        TransactionDetails td = new TransactionDetails("Account_query", 100, list3);
        //execute rule made from the above transaction type.
        ExecuteRule ob = new ExecuteRule(td);
        ArrayList<ExecuteRule> list4 = new ArrayList<>();
        list4.add(ob);
        //execute rule returned.
        return list4;
    }

    @Override
    public void cleanUp(Connection conn) throws SQLException {

        Statement stmtOBj;
        stmtOBj = conn.createStatement();

        // DDL Statement - DROP TABLES
        logger.info("\n=======DROP ALL THE TABLES=======");
        String DROP_TABLE_1 = "DROP TABLE accounts";
        stmtOBj.executeUpdate(DROP_TABLE_1);
        logger.info("\n=======TABLES ARE SUCCESSFULLY DROPPED FROM THE DATABASE=======\n");
        try {
            stmtOBj.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}









