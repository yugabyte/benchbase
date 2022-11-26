package com.oltpbenchmark.benchmarks.featurebench.workerhelpers.YamlHelpers;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class Run {
    @JsonProperty("name")
    public String name;

    @JsonProperty("weight")
    public int weight;

    @JsonProperty("queries")
    public List<Query> queries=new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public List<Query> getQueries() {
        return queries;
    }

    public void setQueries(List<Query> queries) {
        this.queries = queries;
    }

    @Override
    public String toString() {
        return "Run{" +
            "name='" + name + '\'' +
            ", weight=" + weight +
            ", queries=" + queries +
            '}';
    }
}
