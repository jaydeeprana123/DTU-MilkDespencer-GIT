package com.imdc.milkdespencer.common;

import android.content.Context;

public class UsbSerialManager {

    private static UsbSerialManager instance;
    private final UsbSerialCommunication usbSerialCommunication;

    private UsbSerialManager(Context context) {
        usbSerialCommunication = new UsbSerialCommunication(context);
    }

    public static UsbSerialManager getInstance(Context context) {
        if (instance == null) {
            instance = new UsbSerialManager(context.getApplicationContext());
        }
        return instance;
    }

    public void setReadDataListener(UsbSerialCommunication.ReadDataListener listener) {
        usbSerialCommunication.setReadDataListener(listener);
    }

    public void setBaudRate(int baudRate) {
        usbSerialCommunication.setBaudRate(baudRate);
    }

    public void connect() {
        usbSerialCommunication.connect();
    }

    public void sendData(String data) {
        usbSerialCommunication.sendData(data);
    }

    // Add other methods as needed

    public void disconnect() {
        usbSerialCommunication.disconnect();
    }
}
