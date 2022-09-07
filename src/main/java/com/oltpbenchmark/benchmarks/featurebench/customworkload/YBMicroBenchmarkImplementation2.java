package com.oltpbenchmark.benchmarks.featurebench.customworkload;
import com.oltpbenchmark.benchmarks.featurebench.FeatureBenchConstants2;
import com.oltpbenchmark.benchmarks.featurebench.util.*;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;


public class YBMicroBenchmarkImplementation2 implements YBMicroBenchmark, FeatureBenchConstants2 {

    public final static Logger logger = Logger.getLogger(YBMicroBenchmarkImplementation.class);

    @Override
    public void createDB(Connection conn) throws SQLException {

        Statement stmtOBj = null;
        stmtOBj = conn.createStatement();

        // DDL Statement 1 - DROP EXISTING TABLES IF THEY EXIST
        logger.info("\n=======DROPPING TABLES IF THEY EXIST=======");
        stmtOBj.executeUpdate(DROP_QUERY_1);

        stmtOBj.executeUpdate(drop_sequence);
        logger.info("\n========DROP SEQUENCE IF THEY EXIST========\n");
        logger.info("\n========Creating sequence for ids===========\n");
        stmtOBj.execute(Sequence_generator);
        stmtOBj.execute(CREATE_TABLE_1);
        logger.info("\n=======TABLE IS SUCCESSFULLY CREATED=======\n");
        try {
            stmtOBj.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public ArrayList<LoadRule> loadRule(HierarchicalConfiguration<ImmutableNode> properties) {
        int startIndex = 1;
        int endIndex = 10;
        ArrayList<Integer> range = new ArrayList<>();
        range.add(startIndex);
        range.add(endIndex);
        ParamsForUtilFunc paramsFunc1 = new ParamsForUtilFunc(range);
        ArrayList<Integer> string_len = new ArrayList<>();
        int min_len = 1;
        int max_len=20;
        string_len.add(min_len);
        string_len.add(max_len);
        ParamsForUtilFunc paramsFunc2 = new ParamsForUtilFunc(string_len);
        ArrayList<ParamsForUtilFunc> list1 = new ArrayList<>();
        ArrayList<ParamsForUtilFunc> list2 = new ArrayList<>();
        list1.add(paramsFunc1);
        list2.add(paramsFunc2);
        UtilityFunc uf1 = new UtilityFunc("serial_no.nextval", list1);
        UtilityFunc uf2 = new UtilityFunc("astring", list2);
        ColumnsDetails cd1 = new ColumnsDetails("did", uf1);
        ColumnsDetails cd2 = new ColumnsDetails("dname", uf2);
        ArrayList<ColumnsDetails> col_det = new ArrayList<>();
        col_det.add(cd1);
        col_det.add(cd2);
        TableInfo ti = new TableInfo(10, "distributors", col_det);
        LoadRule lr = new LoadRule(ti);
        ArrayList<LoadRule> rule = new ArrayList<>();
        rule.add(lr);
        return rule;

    }

    public ArrayList<ExecuteRule> executeRule(HierarchicalConfiguration<ImmutableNode> properties) {

        int startIndex = 1;
        int endIndex = 10;
        int min_len=1;
        int max_len=20;
        ArrayList<Integer> range = new ArrayList<>();
        range.add(startIndex);
        range.add(endIndex);
        ArrayList<Integer> string_len = new ArrayList<>();
        string_len.add(min_len);
        string_len.add(max_len);
        ParamsForUtilFunc paramsFunc1 = new ParamsForUtilFunc(range);
        ParamsForUtilFunc paramsFunc2 = new ParamsForUtilFunc(string_len);
        ArrayList<ParamsForUtilFunc> list1 = new ArrayList<>();
        ArrayList<ParamsForUtilFunc> list2 = new ArrayList<>();
        list1.add(paramsFunc1);
        list2.add(paramsFunc2);
        UtilityFunc uf1 = new UtilityFunc("nextval", list1);
        UtilityFunc uf2 = new UtilityFunc("astring", list2);
        ArrayList<UtilityFunc> list_of_util = new ArrayList<>();
        list_of_util.add(uf1);
        list_of_util.add(uf2);
        BindParams bd = new BindParams(list_of_util);
        String query = "INSERT INTO distributors(did, dname) VALUES (DEFAULT, ?);";
        ArrayList<BindParams> bp = new ArrayList<>();
        bp.add(bd);
        QueryDetails qd = new QueryDetails(query, bp);
        ArrayList<QueryDetails> list3 = new ArrayList<>();
        list3.add(qd);
        TransactionDetails td = new TransactionDetails("distributor_query", 100, list3);
        ExecuteRule ob = new ExecuteRule(td);
        ArrayList<ExecuteRule> list4 = new ArrayList<>();
        list4.add(ob);
        return list4;
    }

    @Override
    public void cleanUp(Connection conn) throws SQLException {

        Statement stmtOBj = null;
        stmtOBj = conn.createStatement();

        // DDL Statement - DROP TABLES
        logger.info("\n=======DROP ALL THE TABLES=======");
        stmtOBj.executeUpdate(DROP_TABLE_1);
        logger.info("\n=======TABLES ARE SUCCESSFULLY DROPPED FROM THE DATABASE=======\n");

        try {
            stmtOBj.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
