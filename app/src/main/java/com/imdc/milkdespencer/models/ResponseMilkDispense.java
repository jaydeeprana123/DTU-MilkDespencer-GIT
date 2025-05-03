package com.imdc.milkdespencer.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


///{"currentweight":0.699044347,"setweight":3,"status":false}

public class ResponseMilkDispense {

    @SerializedName("currentweight")
    @Expose
    private Double currentWeight;
    @SerializedName("setweight")
    @Expose
    private Double setWeight;
    @SerializedName("status")
    @Expose
    private Boolean status;

    @SerializedName("doorstatus")
    @Expose
    private Boolean doorstatus;

    public Boolean getDoorstatus() {
        return doorstatus;
    }

    public void setDoorstatus(Boolean doorstatus) {
        this.doorstatus = doorstatus;
    }

    public Double getCurrentWeight() {
        return currentWeight;
    }

    public void setCurrentWeight(Double currentWeight) {
        this.currentWeight = currentWeight;
    }

    public Double getSetWeight() {
        return setWeight;
    }

    public void setSetWeight(Double setWeight) {
        this.setWeight = setWeight;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }
}


