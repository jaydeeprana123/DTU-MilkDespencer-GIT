package com.imdc.milkdespencer.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ResponseTempStatus {

    @SerializedName("temperature")
    @Expose
    private Double temperature;
    @SerializedName("compressor")
    @Expose
    private Boolean compressor;
    @SerializedName("agitator")
    @Expose
    private Boolean agitator;
    @SerializedName("lowlevel")
    @Expose
    private Boolean lowlevel;
    @SerializedName("connectivity")
    @Expose
    private Boolean connectivity;

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Boolean getCompressor() {
        return compressor;
    }

    public void setCompressor(Boolean compressor) {
        this.compressor = compressor;
    }

    public Boolean getAgitator() {
        return agitator;
    }

    public void setAgitator(Boolean agitator) {
        this.agitator = agitator;
    }

    public Boolean getLowlevel() {
        return lowlevel;
    }

    public void setLowlevel(Boolean lowlevel) {
        this.lowlevel = lowlevel;
    }

    public Boolean getConnectivity() {
        return connectivity;
    }

    public void setConnectivity(Boolean connectivity) {
        this.connectivity = connectivity;
    }
}