package com.oltpbenchmark.benchmarks.featurebench.workerhelpers.YamlHelpers;

import com.fasterxml.jackson.annotation.JsonProperty;


import java.util.ArrayList;
import java.util.List;

public class Column {


    @JsonProperty("name")
    public String name;


    @JsonProperty("util")
    public String util;


    @JsonProperty("params")
    public List<Object> params=new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getUtil() {
        return util;
    }

    public void setUtil(String util) {
        this.util = util;
    }

    public List<Object> getParams() {
        return params;
    }

    public void setParams(List<Object> params) {
        this.params = params;
    }


    @Override
    public String toString() {
        return "Column{" +
            "name='" + name + '\'' +
            ", util='" + util + '\'' +
            ", params=" + params +
            '}';
    }
}
