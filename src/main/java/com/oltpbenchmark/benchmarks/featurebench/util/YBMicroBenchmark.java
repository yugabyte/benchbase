package com.oltpbenchmark.benchmarks.featurebench.util;


import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

public interface YBMicroBenchmark {

    public void createDB(Connection conn) throws SQLException;

    public ArrayList<LoadRule> loadRule(HierarchicalConfiguration<ImmutableNode> properties);

    public ArrayList<ExecuteRule> executeRule(HierarchicalConfiguration<ImmutableNode> properties);


    public void cleanUp(Connection conn) throws SQLException;
}
