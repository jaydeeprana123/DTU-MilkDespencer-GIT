package com.imdc.milkdespencer.models;

public class SendToDevice {

    private float weight;
    private float settemperature;
    private float curtemperature;
    private float lowweight;
    private float highweight;
    private boolean status;
    private boolean calib = false;

    public float getLowweight() {
        return lowweight;
    }

    public void setLowweight(float lowweight) {
        this.lowweight = lowweight;
    }

    public float getHighweight() {
        return highweight;
    }

    public void setHighweight(float highweight) {
        this.highweight = highweight;
    }

    public boolean isCalib() {
        return calib;
    }

    public void setCalib(boolean calib) {
        this.calib = calib;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    public float getSettemperature() {
        return settemperature;
    }

    public void setSettemperature(float settemperature) {
        this.settemperature = settemperature;
    }

    public float getCurtemperature() {
        return curtemperature;
    }

    public void setCurtemperature(float curtemperature) {
        this.curtemperature = curtemperature;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}
