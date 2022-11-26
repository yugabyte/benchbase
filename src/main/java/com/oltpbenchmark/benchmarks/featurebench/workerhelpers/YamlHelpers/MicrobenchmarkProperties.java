package com.oltpbenchmark.benchmarks.featurebench.workerhelpers.YamlHelpers;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class MicrobenchmarkProperties {

    @JsonProperty("create")
    private List<String> create = new ArrayList<String>();

    @JsonProperty("loadRules")
    public ArrayList<LoadRule> loadRules;

    @JsonProperty("executeRules")
    public ArrayList<ExecuteRule> executeRules;

    @JsonProperty("cleanup")
    private List<String> cleanup = new ArrayList<String>();


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


    @Override
    public String toString() {
        return "MicrobenchmarkProperties = {" +
            "create=" + create +
            ", loadRules=" + loadRules +
            ", executeRules=" + executeRules +
            ", cleanup=" + cleanup +
            '}';
    }
}
