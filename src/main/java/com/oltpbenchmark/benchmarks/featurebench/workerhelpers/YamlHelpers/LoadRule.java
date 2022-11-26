package com.oltpbenchmark.benchmarks.featurebench.workerhelpers.YamlHelpers;

import com.fasterxml.jackson.annotation.JsonProperty;


import java.util.HashSet;


public class LoadRule {

    @JsonProperty("table")
    public String table;

    @JsonProperty("count")
    public int count = 1;

    @JsonProperty("rows")
    public int rows;

    @JsonProperty("columns")
    public HashSet<Column> columns = new HashSet<>();

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public HashSet<Column> getColumns() {
        return columns;
    }

    public void setColumns(HashSet<Column> columns) {
        this.columns = columns;
    }


    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }


    @Override
    public String toString() {
        return "LoadRule{" +
            "table='" + table + '\'' +
            ", count=" + count +
            ", rows=" + rows +
            ", columns=" + columns +
            '}';
    }
}

