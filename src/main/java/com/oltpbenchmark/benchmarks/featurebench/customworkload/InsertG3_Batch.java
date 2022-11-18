package com.oltpbenchmark.benchmarks.featurebench.customworkload;

import com.oltpbenchmark.benchmarks.featurebench.YBMicroBenchmark;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;

import java.sql.Connection;

public class InsertG3_Batch extends YBMicroBenchmark {
    public InsertG3_Batch(HierarchicalConfiguration<ImmutableNode> config) {
        super(config);

    }
    @Override
    public void execute(Connection conn){
        String insertStmt= "INSERT INTO index_test_1 (id, col_int_1, col_int_2, col_int_3, col_int_4, col_int_5, col_int_6, col_int_7, col_int_8, col_int_9, col_int_10, col_varchar_1, col_varchar_2, col_varchar_3, col_date, col_boolean) values (?, ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    }
}
