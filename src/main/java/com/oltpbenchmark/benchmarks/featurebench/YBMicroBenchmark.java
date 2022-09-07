package com.oltpbenchmark.benchmarks.featurebench;

import com.oltpbenchmark.benchmarks.featurebench.util.ExecuteRule;
import com.oltpbenchmark.benchmarks.featurebench.util.LoadRule;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

public abstract class YBMicroBenchmark {

    public boolean createDBImplemented = true;
    public boolean cleanUpImplemented = true;
    public boolean loadOnceImplemented = false;
    public boolean executeOnceImplemented = false;

    public boolean afterLoadImplemented = false;
    public HierarchicalConfiguration<ImmutableNode> properties;
    public YBMicroBenchmark(HierarchicalConfiguration<ImmutableNode> properties) {
        this.properties = properties;
    }

    public abstract void createDB(Connection conn) throws SQLException;

    public abstract ArrayList<LoadRule> loadRules();

    public abstract ArrayList<ExecuteRule> executeRules();

    public abstract void cleanUp(Connection conn) throws SQLException;

    public abstract void loadOnce(Connection conn);

    public abstract void executeOnce(Connection conn);

    public void afterLoad(Connection conn) {};
}
