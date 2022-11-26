package com.oltpbenchmark.benchmarks.featurebench.workerhelpers.YamlHelpers;



public class Properties {

    public MicrobenchmarkProperties properties;

    public MicrobenchmarkProperties getProperties() {
        return properties;
    }

    public void setProperties(MicrobenchmarkProperties properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "Properties{" +
            "properties=" + properties +
            '}';
    }

}
