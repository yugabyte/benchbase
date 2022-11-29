package com.oltpbenchmark.benchmarks.featurebench.workerhelpers.YamlHelpers;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Microbenchmark {

    @JsonProperty("class")
    public String classpath;


    @JsonProperty("properties")
    public MicrobenchmarkProperties properties;

    public MicrobenchmarkProperties getProperties() {
        return properties;
    }

    public void setProperties(MicrobenchmarkProperties properties) {
        this.properties = properties;
    }

    public String getClasspath() {
        return classpath;
    }

    public void setClasspath(String classpath) {
        this.classpath = classpath;
    }


    @Override
    public String toString() {
        return "Microbenchmark{" +
            "class='" + classpath + '\'' +
            ", properties=" + properties +
            '}';
    }
}
