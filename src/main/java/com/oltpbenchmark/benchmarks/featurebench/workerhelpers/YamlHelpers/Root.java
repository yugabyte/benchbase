package com.oltpbenchmark.benchmarks.featurebench.workerhelpers.YamlHelpers;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Root {
    private String type;
    private String createdb = null;

    @JsonProperty("collect_pg_stat_statements")
    private boolean pgStatsCollect = false;
    private String driver;
    private String url;
    private String username;
    private String password;
    private int batchsize;

    @JsonProperty("isolation")
    private String isolationMode;
    @JsonProperty("loaderthreads")
    private int loaderthreads;
    private Works works;
    private Microbenchmark microbenchmark;
    private int terminals;


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getBatchsize() {
        return batchsize;
    }

    public void setBatchsize(int batchsize) {
        this.batchsize = batchsize;
    }

    public String getIsolationMode() {
        return isolationMode;
    }

    public void setIsolationMode(String isolationMode) {
        this.isolationMode = isolationMode;
    }

    public int getLoaderThreads() {
        return loaderthreads;
    }

    public void setLoaderThreads(int loaderThreads) {
        this.loaderthreads = loaderThreads;
    }


    public int getTerminals() {
        return terminals;
    }

    public void setTerminals(int terminals) {
        this.terminals = terminals;
    }


    public Works getWorks() {
        return works;
    }

    public void setWorks(Works works) {
        this.works = works;
    }

    public Microbenchmark getMicrobenchmark() {
        return microbenchmark;
    }

    public void setMicrobenchmark(Microbenchmark microbenchmark) {
        this.microbenchmark = microbenchmark;
    }

    public String getCreatedb() {
        return createdb;
    }

    public void setCreatedb(String createdb) {
        this.createdb = createdb;
    }

    public boolean isPgStatsCollect() {
        return pgStatsCollect;
    }

    public void setPgStatsCollect(boolean pgStatsCollect) {
        this.pgStatsCollect = pgStatsCollect;
    }


    @Override
    public String toString() {
        return "Root{" +
            "type='" + type + '\'' +
            ", createdb='" + createdb + '\'' +
            ", pgStatsCollect=" + pgStatsCollect +
            ", driver='" + driver + '\'' +
            ", url='" + url + '\'' +
            ", username='" + username + '\'' +
            ", password='" + password + '\'' +
            ", batchsize=" + batchsize +
            ", isolationMode='" + isolationMode + '\'' +
            ", loaderThreads=" + loaderthreads +
            ", works=" + works +
            ", microbenchmark=" + microbenchmark +
            ", terminals=" + terminals +
            '}';
    }
}
