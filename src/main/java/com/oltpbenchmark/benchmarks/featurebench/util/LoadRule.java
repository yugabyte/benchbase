package com.oltpbenchmark.benchmarks.featurebench.util;

public class LoadRule {
    private TableInfo tableInfo;

    private int numOfColumns = 0;
    public LoadRule(TableInfo tableInfo) {
        this.tableInfo = tableInfo;
    }

    public TableInfo getTableInfo() {
        return tableInfo;
    }

    public int getNumOfColumns() {
        return numOfColumns;
    }

    public void setNumOfColumns(int numOfColumns) {
        this.numOfColumns = numOfColumns;
    }
}

