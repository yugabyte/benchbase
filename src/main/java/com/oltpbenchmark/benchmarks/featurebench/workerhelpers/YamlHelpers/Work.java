package com.oltpbenchmark.benchmarks.featurebench.workerhelpers.YamlHelpers;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Work {
    @JsonProperty("time_secs")
    public int timeSecs;

    public int weights;
    public int rate;
    public int warmup;

    public int getTimeSecs() {
        return timeSecs;
    }
    public void setTimeSecs(Integer timeSecs) {
        this.timeSecs = timeSecs;
    }
    public int getRate() {
        return rate;
    }
    public void setRate(Integer rate) {
        this.rate = rate;
    }

    public int getWarmup() {
        return warmup;
    }

    public void setWarmup(int warmup) {
        this.warmup = warmup;
    }


    public int getWeights() {
        return weights;
    }

    public void setWeights(int weights) {
        this.weights = weights;
    }


    @Override
    public String toString() {
        return "Work{" +
            "timeSecs=" + timeSecs +
            ", weights=" + weights +
            ", rate=" + rate +
            ", warmup=" + warmup +
            '}';
    }
}