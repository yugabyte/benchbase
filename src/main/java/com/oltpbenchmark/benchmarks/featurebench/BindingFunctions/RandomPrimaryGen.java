package com.oltpbenchmark.benchmarks.featurebench.BindingFunctions;


import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RandomPrimaryGen {

    protected static int upperRange;
    protected static int lowerRange;

    protected int counterInsert;

    protected int counterQuery;

    protected PreparedStatement stmt;

    public final static org.apache.log4j.Logger logger = Logger.getLogger(RandomPrimaryGen.class);

    RandomPrimaryGen(int upper, int lower) {
        lowerRange = lower;
        upperRange = upper;
        counterInsert = lowerRange;
    }

    void setCounterQuery(int lower) {
        counterQuery = lower;
    }

    int getNextValInsertion(Connection conn, String columnName, String TableName) throws SQLException {
        String query1 = "SELECT count(" + columnName + ")" + "FROM" + TableName;
        stmt = conn.prepareStatement(query1);
        int count = stmt.executeQuery().getInt(1);
        if (count == 0) {
            counterInsert = lowerRange;
        } else {
            String query2 = "SELECT " + columnName + " FROM " + TableName + " ORDER BY " + columnName + " DESC limit 1";
            stmt = conn.prepareStatement(query2);
            ResultSet rs = stmt.executeQuery();
            int lastCounter = rs.getInt(1);
            if (lastCounter < upperRange) {
                counterInsert++;
                return counterInsert;
            } else {
                throw new RuntimeException("Insertion completed primary key out of bounds");
            }
        }
        return 0;
    }

    int getNextValExecution(Connection conn, String columnName, String TableName, int startRange, int endRange) throws SQLException {
        if (endRange > upperRange || startRange < lowerRange) {
            throw new RuntimeException("Incorrect bounds for query");
        }
        String query1 = "SELECT count(" + columnName + ")" + "FROM" + TableName;
        stmt = conn.prepareStatement(query1);
        int count = stmt.executeQuery().getInt(1);
        if (count == 0) {
            throw new RuntimeException("No values in table for query");
        } else {
            if (counterQuery < endRange) {
                counterQuery++;
                return counterQuery;
            } else {
                logger.info("Finished executing for queries");
            }
        }
        return 0;
    }

}






