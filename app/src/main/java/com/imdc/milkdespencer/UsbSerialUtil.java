package com.imdc.milkdespencer;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class UsbSerialUtil {
    public static final String ACTION_USB_PERMISSION = "com.imdc.milkdespencer.ACTION_USB_PERMISSION";
    private static final int TIMEOUT = 1000;
    private final UsbManager usbManager;
    private final DataReceivedCallback dataReceivedCallback;
    private UsbDevice usbDevice;
    private UsbDeviceConnection usbConnection;
    private UsbEndpoint inEndpoint;
    private UsbEndpoint outEndpoint;
    private boolean isReading = false;

    public UsbSerialUtil(Context context, DataReceivedCallback callback) {
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        dataReceivedCallback = callback;
    }

    public boolean connectToDevice(UsbDevice device) {
        if (usbConnection != null) {
            disconnectDevice();
        }

        usbDevice = device;
        UsbInterface usbInterface = usbDevice.getInterface(0);
        usbConnection = usbManager.openDevice(usbDevice);

        if (usbConnection != null) {
            if (usbConnection.claimInterface(usbInterface, true)) {
                if (setBaudRate(115200)) {
                    // Find the IN and OUT endpoints
                    inEndpoint = usbInterface.getEndpoint(0);
                    outEndpoint = usbInterface.getEndpoint(1);
                    return true;
                } else {
                    disconnectDevice();
                }

            } else {

                Log.e("TAG", "Not connectedToDevice:  ");
            }
        }
        return false;
    }

    public void disconnectDevice() {
        if (usbConnection != null) {
            usbConnection.releaseInterface(usbDevice.getInterface(0));
            usbConnection.close();
            usbConnection = null;
        }
        usbDevice = null;
        inEndpoint = null;
        outEndpoint = null;
    }

    public int sendData(JSONObject json) throws IOException {
        if (usbConnection != null && outEndpoint != null) {
            String jsonString = json.toString();
            byte[] data = jsonString.getBytes();
            return usbConnection.bulkTransfer(outEndpoint, data, data.length, TIMEOUT);
        }
        throw new IOException("USB device is not connected");
        /* if (usbConnection != null && outEndpoint != null) {
            return usbConnection.bulkTransfer(outEndpoint, data, data.length, TIMEOUT);
        }
        throw new IOException("USB device is not connected");*/
    }

    public String receiveData() throws IOException, JSONException {

        if (usbConnection != null && inEndpoint != null) {
            StringBuilder receivedDataBuilder = new StringBuilder();
            String lineSeparator = System.getProperty("line.separator");

            long startTime = System.currentTimeMillis();
            long elapsedTime;

            while (true) {
                byte[] buffer = new byte[10000]; // Adjust the buffer size as needed
                int bytesRead = usbConnection.bulkTransfer(inEndpoint, buffer, buffer.length, TIMEOUT);

                if (bytesRead <= 0) {
                    elapsedTime = System.currentTimeMillis() - startTime;
                    if (elapsedTime > TIMEOUT) {
                        // Break the loop if the elapsed time exceeds TIMEOUT
                        break;
                    }
                    // Wait for a short duration to avoid busy waiting
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue; // Continue the loop to check for more data after a short delay
                }

                String receivedData = new String(buffer, 0, bytesRead);

                // Append the received data to the StringBuilder
                receivedDataBuilder.append(receivedData);

                // Check if a complete JSON object has been received
                String receivedDataString = receivedDataBuilder.toString();
                if (receivedDataString.contains("}")) {
                    // Parse the JSON object
                    int startIndex = receivedDataString.indexOf("{");
                    int endIndex = receivedDataString.indexOf("}") + 1;
                    if (startIndex >= 0 && endIndex > startIndex) {
                        return receivedDataString.substring(startIndex, endIndex);
                    }
                }

                // Reset the start time for the timeout check
                startTime = System.currentTimeMillis();
            }
        }
        throw new IOException("USB device is not connected");
    }

    /*public String receiveData() throws IOException, JSONException {
        if (usbConnection != null && inEndpoint != null) {
            StringBuilder receivedDataBuilder = new StringBuilder();
            String lineSeparator = System.getProperty("line.separator");
            while (true) {
                byte[] buffer = new byte[10000]; // Adjust the buffer size as needed
                int bytesRead = usbConnection.bulkTransfer(inEndpoint, buffer, buffer.length, TIMEOUT);
                if (bytesRead <= 0) {
                    break; // No more data to read
                }

                String receivedData = new String(buffer, 0, bytesRead);

                // Append the received data to the StringBuilder
                if (receivedData.isEmpty()) {
                    return "";
                }
                receivedDataBuilder.append(receivedData);

                // Check if a complete JSON object has been received
                String receivedDataString = receivedDataBuilder.toString();
                if (receivedDataString.contains("}")) {
                    // Parse the JSON object
                    int startIndex = receivedDataString.indexOf("{");
                    int endIndex = receivedDataString.indexOf("}") + 1;
                    if (startIndex >= 0 && endIndex > startIndex) {
                        return receivedDataString.substring(startIndex, endIndex);
                    }
                }else return "";
            }
        }
        throw new IOException("USB device is not connected");
    }*/

    private boolean setBaudRate(int baudRate) {
        // Example code to set the baud rate, assuming the device supports it
        // This code is fictional and device-specific, and you'll need to consult your device's documentation
        // or driver to configure the baud rate.

        // For example, if the device uses xcontrol requests to set baud rate:
        byte[] data = new byte[4];
        data[0] = (byte) (baudRate & 0xFF);
        data[1] = (byte) ((baudRate >> 8) & 0xFF);
        data[2] = (byte) ((baudRate >> 16) & 0xFF);
        data[3] = (byte) ((baudRate >> 24) & 0xFF);

        int requestType = 0x21; // USB_SETUP_HOST_TO_DEVICE | USB_TYPE_CLASS | USB_RECIP_INTERFACE
        int request = 0x20; // USB_SERIAL_SET_BAUD_RATE request
        int value = 0; // Zero-based interface number
        int index = 0; // Zero-based endpoint number

        int result = usbConnection.controlTransfer(requestType, request, value, index, data, data.length, TIMEOUT);
        Log.e("TAG", "setBaudRate: " + (result >= 0));
        return result >= 0; // Return true if successful
    }

    public boolean isDataAvailable() {
        if (usbConnection != null && inEndpoint != null) {
            byte[] buffer = new byte[1];
            int bytesRead = usbConnection.bulkTransfer(inEndpoint, buffer, buffer.length, 0); // Non-blocking check
            return bytesRead > 0;
        }
        return false;
    }

    public void startReadingData() {
        isReading = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isReading) {
                    String receivedData = "";
                    try {
                        receivedData = receiveData();
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }

                    if (dataReceivedCallback != null && !receivedData.isEmpty()) {
                        dataReceivedCallback.onDataReceived(receivedData);
                    }
                }
            }
        }).start();
    }

    public void stopReadingData() {
        isReading = false;
    }

    public interface DataReceivedCallback {
        void onDataReceived(String data);
    }
}
