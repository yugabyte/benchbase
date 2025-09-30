package com.oltpbenchmark.benchmarks.featurebench;

import org.json.JSONObject;
import java.util.List;


public class FeaturebenchAdditionalResults {
    List<JSONObject> jsonResultsList;
    JSONObject metaDataJson = new JSONObject();

    public List<JSONObject> getJsonResultsList() {
        return jsonResultsList;
    }

    public void setJsonResultsList(List<JSONObject> jsonResultsList) {
        this.jsonResultsList = jsonResultsList;

    }

    public JSONObject getMetaDataJson() {
        return metaDataJson;
    }

    public void setMetaDataJson(JSONObject metaDataJson) {
        this.metaDataJson = metaDataJson;
    }

}
