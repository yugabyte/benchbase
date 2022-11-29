package com.oltpbenchmark.benchmarks.featurebench.workerhelpers.YamlHelpers;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class MicrobenchmarkProperties {

    @JsonProperty("create")
    public List<String> create = new ArrayList<>();

    @JsonProperty("loadRules")
    public ArrayList<LoadRule> loadRules;

    @JsonProperty("executeRules")
    public ArrayList<ExecuteRule> executeRules;

    @JsonProperty("cleanup")
    public List<String> cleanup = new ArrayList<>();

    @JsonProperty("setAutoCommit")
    public boolean setAutoCommit = false;

    @JsonProperty("execute")
    public boolean execute = false;

    @JsonProperty("afterLoad")
    public List<String> afterLoad = new ArrayList<>();

    @JsonProperty("tableName")
    public String tableName;

    @JsonProperty("columns")
    public int columns;

    @JsonProperty("rows")
    public int rows;

    @JsonProperty("indexes")
    public int indexes;

    @JsonProperty("filePath")
    public String filePath;
    @JsonProperty("rowsPerTransaction")
    public int rowsPerTransaction;

    public int getColumns() {
        return columns;
    }

    public void setColumns(int columns) {
        this.columns = columns;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public int getIndexes() {
        return indexes;
    }

    public void setIndexes(int indexes) {
        this.indexes = indexes;
    }


    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }


    public int getRowsPerTransaction() {
        return rowsPerTransaction;
    }

    public void setRowsPerTransaction(int rowsPerTransaction) {
        this.rowsPerTransaction = rowsPerTransaction;
    }

    public List<String> getAfterLoad() {
        return afterLoad;
    }

    public void setAfterLoad(List<String> afterLoad) {
        this.afterLoad = afterLoad;
    }

    public List<String> getCreate() {
        return create;
    }

    public void setCreate(List<String> create) {
        this.create = create;
    }


    public ArrayList<LoadRule> getLoadRules() {
        return loadRules;
    }

    public void setLoadRules(ArrayList<LoadRule> loadRules) {
        this.loadRules = loadRules;
    }

    public ArrayList<ExecuteRule> getExecuteRules() {
        return executeRules;
    }

    public void setExecuteRules(ArrayList<ExecuteRule> executeRules) {
        this.executeRules = executeRules;
    }

    public List<String> getCleanup() {
        return cleanup;
    }

    public void setCleanup(List<String> cleanup) {
        this.cleanup = cleanup;
    }


    public boolean isSetAutoCommit() {
        return setAutoCommit;
    }

    public void setSetAutoCommit(boolean setAutoCommit) {
        this.setAutoCommit = setAutoCommit;
    }


    public boolean isExecute() {
        return execute;
    }

    public void setExecute(boolean execute) {
        this.execute = execute;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }


    @Override
    public String toString() {
        return "MicrobenchmarkProperties{" +
            "create=" + create +
            ", loadRules=" + loadRules +
            ", executeRules=" + executeRules +
            ", cleanup=" + cleanup +
            ", setAutoCommit=" + setAutoCommit +
            ", execute=" + execute +
            ", afterLoad=" + afterLoad +
            ", tableName='" + tableName + '\'' +
            ", columns=" + columns +
            ", rows=" + rows +
            ", indexes=" + indexes +
            ", filePath='" + filePath + '\'' +
            ", rowsPerTransaction=" + rowsPerTransaction +
            '}';
    }
}
