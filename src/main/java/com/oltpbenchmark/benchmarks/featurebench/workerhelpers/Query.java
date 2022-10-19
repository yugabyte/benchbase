package com.oltpbenchmark.benchmarks.featurebench.workerhelpers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oltpbenchmark.benchmarks.featurebench.workerhelpers.Bindings;

public class Query {
    @JsonProperty("query")
    public String query;

    @JsonProperty("bindings")
    public Bindings bindings;

    public Query(String query, Bindings bindings) {
        this.query = query;
        this.bindings = bindings;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Bindings getBindings() {
        return bindings;
    }

    public void setBindings(Bindings bindings) {
        this.bindings = bindings;
    }
}
