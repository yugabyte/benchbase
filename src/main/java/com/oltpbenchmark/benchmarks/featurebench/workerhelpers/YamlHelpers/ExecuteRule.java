package com.oltpbenchmark.benchmarks.featurebench.workerhelpers.YamlHelpers;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class ExecuteRule {

    @JsonProperty("workload")
    public String workload;

    @JsonProperty("run")
    public List<Run> run=new ArrayList<>();

    public String getWorkload() {
        return workload;
    }

    public void setWorkload(String workload) {
        this.workload = workload;
    }

    public List<Run> getRun() {
        return run;
    }

    public void setRun(List<Run> run) {
        this.run = run;
    }

    @Override
    public String toString() {
        return "ExecuteRule{" +
            "workload='" + workload + '\'' +
            ", run=" + run +
            '}';
    }
}
