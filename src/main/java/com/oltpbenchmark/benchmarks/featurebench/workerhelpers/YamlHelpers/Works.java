package com.oltpbenchmark.benchmarks.featurebench.workerhelpers.YamlHelpers;

public class Works {

    public Work work;
    public Work getWork() {
        return work;
    }
    public void setWork(Work work) {
        this.work = work;
    }

    @Override
    public String toString() {
        return "Works{" +
            "work=" + work +
            '}';
    }
}