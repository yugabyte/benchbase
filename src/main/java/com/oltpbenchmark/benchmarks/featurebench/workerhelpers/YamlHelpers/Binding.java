package com.oltpbenchmark.benchmarks.featurebench.workerhelpers.YamlHelpers;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class Binding {

    @JsonProperty("util")
    public String util;

    @JsonProperty("params")
    public List<Object> params=new ArrayList<>();


    public List<Object> getParams() {
        return params;
    }

    public void setParams(List<Object> params) {
        this.params = params;
    }

    public String getUtil() {
        return util;
    }

    public void setUtil(String util) {
        this.util = util;
    }

    @Override
    public String toString() {
        return "Binding{" +
            "util='" + util + '\'' +
            ", params=" + params +
            '}';
    }
}
