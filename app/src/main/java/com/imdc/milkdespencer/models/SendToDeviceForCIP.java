package com.imdc.milkdespencer.models;

public class SendToDeviceForCIP {


    private boolean compressor = false;
    private boolean agitator = false;
    private boolean pump = false;


    public boolean isCompressor() {
        return compressor;
    }

    public void setCompressor(boolean compressor) {
        this.compressor = compressor;
    }

    public boolean isAgitator() {
        return agitator;
    }

    public void setAgitator(boolean agitator) {
        this.agitator = agitator;
    }

    public boolean isPump() {
        return pump;
    }

    public void setPump(boolean pump) {
        this.pump = pump;
    }
}
