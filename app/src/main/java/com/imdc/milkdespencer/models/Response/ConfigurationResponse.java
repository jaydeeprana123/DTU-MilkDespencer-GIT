package com.imdc.milkdespencer.models.Response;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class ConfigurationResponse {

    @SerializedName("Data")
    private List<Datum> data = new ArrayList<>();

    @SerializedName("JsonRequestBehavior")
    private long jsonRequestBehavior;

    // Getters and Setters
    public List<Datum> getData() {
        return data;
    }

    public void setData(List<Datum> data) {
        this.data = data;
    }

    public long getJsonRequestBehavior() {
        return jsonRequestBehavior;
    }

    public void setJsonRequestBehavior(long jsonRequestBehavior) {
        this.jsonRequestBehavior = jsonRequestBehavior;
    }
}

