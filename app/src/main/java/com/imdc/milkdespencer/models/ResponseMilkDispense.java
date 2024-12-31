package com.imdc.milkdespencer.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


///{"currentweight":0.699044347,"setweight":3,"status":false}

public class ResponseMilkDispense {

    @SerializedName("currentweight")
    @Expose
    private Double curTemperature;
    @SerializedName("setweight")
    @Expose
    private Double setTemperature;
    @SerializedName("status")
    @Expose
    private Boolean status;

    public Double getCurTemperature() {
        return curTemperature;
    }

    public void setCurTemperature(Double curTemperature) {
        this.curTemperature = curTemperature;
    }

    public Double getSetTemperature() {
        return setTemperature;
    }

    public void setSetTemperature(Double setTemperature) {
        this.setTemperature = setTemperature;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }
}


