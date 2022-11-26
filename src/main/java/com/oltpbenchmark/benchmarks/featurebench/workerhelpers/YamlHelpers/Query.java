package com.oltpbenchmark.benchmarks.featurebench.workerhelpers.YamlHelpers;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class Query {

    @JsonProperty("query")
    public String query;

    @JsonProperty("count")
    public int count = 1;

    @JsonProperty("bindings")
    public List<Binding> bindings = new ArrayList<>();

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<Binding> getBindings() {
        return bindings;
    }

    public void setBindings(List<Binding> bindings) {
        this.bindings = bindings;
    }


    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }



    @Override
    public String toString() {
        return "Query{" +
            "query='" + query + '\'' +
            ", count=" + count +
            ", bindings=" + bindings +
            '}';
    }
}
