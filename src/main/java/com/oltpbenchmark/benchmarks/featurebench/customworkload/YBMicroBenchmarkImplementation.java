package com.oltpbenchmark.benchmarks.featurebench.customworkload;


import com.oltpbenchmark.benchmarks.featurebench.YBMicroBenchmark;
import com.oltpbenchmark.benchmarks.featurebench.helpers.*;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class YBMicroBenchmarkImplementation extends YBMicroBenchmark {


    public final static Logger LOG = Logger.getLogger(YBMicroBenchmarkImplementation.class);

    public YBMicroBenchmarkImplementation(HierarchicalConfiguration<ImmutableNode> config) {
        super(config);
        this.executeOnceImplemented = false;
        this.loadOnceImplemented = false;
        this.afterLoadImplemented = false;
    }


    public void create(Connection conn) throws SQLException {
        try {
            Statement stmtOBj = conn.createStatement();
            LOG.info("Recreating tables if already exists");
            stmtOBj.executeUpdate("DROP TABLE IF EXISTS accounts");
            stmtOBj.execute("CREATE TABLE accounts ("
                + "id int NOT NULL,"
                + "name varchar(64) NOT NULL,"
                + "CONSTRAINT pk_accounts PRIMARY KEY (id)"
                + ");");
            stmtOBj.execute("CREATE INDEX idx_accounts_name ON accounts (name);");
            stmtOBj.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public ArrayList<ExecuteRule> executeRules() {
        int startIndex = 1;
        int endIndex = 10;
        String query = "SELECT * FROM ACCOUNTS WHERE ID > ?";

        // utility parameters for int primary key generation with range
        ParamsForUtilFunc idParams = new ParamsForUtilFunc(new ArrayList<>(Arrays.asList(startIndex, endIndex)));
        UtilityFunc IdUtilityFunc = new UtilityFunc("RandomNoWithinRange", new ArrayList<>(List.of(idParams)));

        BindParams bindParams = new BindParams(new ArrayList<>(List.of(IdUtilityFunc)));
        QueryDetails queryDetails = new QueryDetails(query, new ArrayList<>(List.of(bindParams)));

        // add the transaction details with queryName, weight and queryDetails
        TransactionDetails transactionDetails =
            new TransactionDetails(
                "Account_query", 100,
                new ArrayList<>(List.of(queryDetails)));

        // create and return execute rule with transaction Details
        ExecuteRule executeRule = new ExecuteRule(transactionDetails);
        return new ArrayList<>(List.of(executeRule));
    }

    @Override
    public void loadOnce(Connection conn) {

    }

}
