package com.imdc.milkdespencer.common;

import static com.imdc.milkdespencer.MainActivity.getInstance;
import static com.imdc.milkdespencer.common.Constants.PREF_PERMISSION_GRANTED;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.imdc.milkdespencer.CashCollectorActivity;
import com.imdc.milkdespencer.models.ResponseMilkDispense;
import com.imdc.milkdespencer.models.ResponseTempStatus;
import com.imdc.milkdespencer.models.SendToDevice;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class UsbSerialCommunication {

    private static final String TAG = "UsbSerialCommunication";
    private static final String ACTION_USB_PERMISSION = "com.imdc.milkdespencer.USB_PERMISSION";
    private static final int TIMEOUT = 1000;
    private final Context context;
    private final UsbManager usbManager;
    private final Handler handler;
    private ExecutorService executorService;
    public boolean connected = false;
    public boolean fromCalibration = false;
    public boolean readingDataThreadRunning = false;
    Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
    SendToDevice sendToDevice;
    private ReadDataListener readDataListener;
    private int baudRate = 115200; // Default baud rate, change as needed
    private UsbDevice usbDevice;
    private UsbDeviceConnection usbConnection;
    private UsbInterface usbInterface;
    private UsbEndpoint inEndpoint;
    private UsbEndpoint outEndpoint;
    private boolean currencyReceived = false;

    public static boolean isCipOn = false;

    public static boolean isSendDataStop = false;


    public static boolean isLowLevel = false;


    private boolean isUsbReceiverRegistered = false;


    public UsbSerialCommunication(Context context) {
        this.context = context;
        this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        this.handler = new Handler(Looper.getMainLooper());
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public static boolean isValidJson(String json) {
        try {
            JsonParser parser = new JsonParser();
            parser.parse(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static byte[] intToByteArray(int value) {
        return new byte[]{(byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value};
    }

    public void setReadDataListener(ReadDataListener listener) {
        this.readDataListener = listener;
    }

    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
        // Call this method whenever the baud rate needs to be changed (e.g., after connecting)
        updateBaudRate();
    }

    private void updateBaudRate() {
        // Adjust the baud rate of the USB connection
        if (usbConnection != null && usbInterface != null) {
            usbConnection.releaseInterface(usbInterface);
            usbConnection.close();
            openConnection(usbDevice);

        }
    }


    public void connect() {
        logError("connect ", "method");

        if (usbManager == null) {
            logError(TAG, "UsbManager is null. Make sure USB is supported on this device.");
            return;
        }

        // Find the first available USB device
        UsbDevice device = findAnyUsbDevice();
        if (device == null) {
            logError(TAG, "No USB device found.");
            return;
        } else {
            Log.d(TAG, "Connected: " + device.getProductId() + " <---> " + device.getVendorId() + "\n " + device);

            SharedPreferencesManager preferencesManager = SharedPreferencesManager.getInstance(getInstance());

            if (preferencesManager.hasValue(Constants.ResponseTempStatus)) {
                try {
                    String savedJson = preferencesManager.get(Constants.ResponseTempStatus, "").toString();
                    ResponseTempStatus responseTempStatus = new Gson().fromJson(savedJson, ResponseTempStatus.class);

                    if (responseTempStatus != null && responseTempStatus.getTemperature() != null) {
                        double currentSavedTemp = responseTempStatus.getTemperature() / 10;

                        float offset = 0.0f;
                        try {
                            offset = Float.parseFloat(preferencesManager.get(Constants.TemperatureOffSet, 0.0).toString());
                        } catch (Exception offsetException) {
                            logError(TAG, "Invalid offset value: " + offsetException.getMessage());
                        }

                        float currentTemperature = (float) (currentSavedTemp + offset);
                        fireOnStart(currentTemperature);
                    } else {
                        logError(TAG, "Temperature is null or responseTempStatus is null.");
                        fireOnStart(0);
                    }
                } catch (Exception e) {
                    logError(TAG, "Error parsing ResponseTempStatus: " + e.getMessage());
                    fireOnStart(0);
                }
            } else {
                fireOnStart(0);
            }
        }

        // Request permission
        requestPermission(device);
    }


//    public void connect() {
//
//        logError("connect ", "method");
//
//        if (usbManager == null) {
//            logError(TAG, "UsbManager is null. Make sure USB is supported on this device.");
//            return;
//        }
//
//        // Find the first available USB device
//        UsbDevice device = findAnyUsbDevice();
//        if (device == null) {
//          //  Toast.makeText(context, "No USB device found. Please connect the device", Toast.LENGTH_SHORT).show();
//            logError(TAG, "No USB device found.");
//            return;
//        } else {
//            Log.d(TAG, "Connected: " + device.getProductId() + " <---> " + device.getVendorId() + "\n " + device);
////            String temperatureResponse = String.valueOf(preferencesManager.get(Constants.ResponseTempStatus, null));
////            Toast.makeText(context, "TEMPERATURE STATUS " + preferencesManager.hasValue(Constants.ResponseTempStatus), Toast.LENGTH_SHORT).show();
//
//
//            SharedPreferencesManager preferencesManager = SharedPreferencesManager.getInstance(getInstance());
//            if (preferencesManager.hasValue(Constants.ResponseTempStatus)) {
//                ResponseTempStatus responseTempStatus = new Gson().fromJson(preferencesManager.get(Constants.ResponseTempStatus, "").toString(), ResponseTempStatus.class);
//
//
//                if(responseTempStatus != null){
//                    double currentSavedTemp = responseTempStatus.getTemperature() / 10;
//                    float offSet = Float.parseFloat(preferencesManager.get(Constants.TemperatureOffSet, 0.0).toString());
//                    float currentTemperature = Float.parseFloat(String.valueOf((currentSavedTemp + offSet)));
//                    fireOnStart(currentTemperature);
//                }else {
//                    fireOnStart(0);
//                }
//
//            }else {
//                fireOnStart(0);
//            }
//
//
//
////            SharedPreferencesManager preferencesManager = SharedPreferencesManager.getInstance(context);
////            if (preferencesManager.hasValue(Constants.ResponseTempStatus)) {
////                String temperatureResponse = String.valueOf(preferencesManager.get(Constants.ResponseTempStatus, null));
////                logError(TAG, "connect: " + temperatureResponse);
////                if (!temperatureResponse.isEmpty()) {
////                    float offSet = Float.parseFloat(preferencesManager.get(Constants.TemperatureOffSet, 0.0).toString());
////                    ResponseTempStatus responseTempStatus = new Gson().fromJson(temperatureResponse, ResponseTempStatus.class);
////                    double currentSavedTemp = responseTempStatus.getTemperature() / 10;
////                    float currentTemperature = Float.parseFloat(String.valueOf((currentSavedTemp + offSet)));
////                    fireOnStart(currentTemperature);
////                } else {
////                    fireOnStart(0);
////                }
////            } else {
////            }
//
////            fireOnStart(0);
//
//        }
//
//        // Request permission
//        requestPermission(device);
//    }

    private UsbDevice findAnyUsbDevice() {
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        for (UsbDevice device : usbDevices.values()) {
            if (device.getVendorId() == 4292 && device.getProductId() == 60000) {
                logError(TAG, "findAnyUsbDevice: CONDI " + device);
                return device;
            }
        }

        /*if (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            logError(TAG, "findAnyUsbDevice: CONDI " + device);
            logError(TAG, usbDevices.values() + " <deviceIteratorSIZE ---- usbDevicesSIZE > " + usbDevices.size());
            if (device.getVendorId() == 4292 && device.getProductId() == 60000) {
                logError(TAG, "findAnyUsbDevice: CONDI " + device);
                return device;
            }
        }*/

        return null;
    }

    private void requestPermission(UsbDevice device) {

        SharedPreferencesManager preferencesManager = SharedPreferencesManager.getInstance(context);
        boolean isPermissionGranted = (boolean) preferencesManager.get(PREF_PERMISSION_GRANTED, false);

//        if(!isPermissionGranted){
//            PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
//
//            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
//            context.registerReceiver(usbPermissionReceiver, filter);
//
//            usbManager.requestPermission(device, permissionIntent);
//        }


        PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        context.registerReceiver(usbPermissionReceiver, filter);

        usbManager.requestPermission(device, permissionIntent);

    }


    public void openConnection(UsbDevice device) {
        usbDevice = device;
        usbInterface = device.getInterface(0);
        inEndpoint = usbInterface.getEndpoint(0);
        outEndpoint = usbInterface.getEndpoint(1);

        logError(TAG + "getVendorId", String.valueOf(device.getVendorId()));
        logError(TAG + "getProductId", String.valueOf(device.getProductId()));

        usbConnection = usbManager.openDevice(device);

        logError("openConnection", "method");

        if (usbConnection != null) {
            logError("usbConnection", "not null");

            if (usbConnection.claimInterface(usbInterface, true)) {
                logError("usbConnection", "claimInterface");
                setBaudRateInternal();
                connected = true;
                checkAndStartReadingData();

                SharedPreferencesManager preferencesManager = SharedPreferencesManager.getInstance(context);
                String temperatureResponse = String.valueOf(preferencesManager.get(Constants.ResponseTempStatus, ""));

                if (temperatureResponse != null && !temperatureResponse.isEmpty() && !temperatureResponse.equals("null")) {
                    logError("temperatureResponse", "not empty");

                    Object offsetObj = preferencesManager.get(Constants.TemperatureOffSet, 0.0);
                    float offSet = 0.0f;

                    if (offsetObj != null) {
                        try {
                            offSet = Float.parseFloat(offsetObj.toString());
                        } catch (NumberFormatException e) {
                            logError("TemperatureOffSet", "Invalid format, using default 0.0");
                        }
                    } else {
                        logError("TemperatureOffSet", "was null, using default 0.0");
                    }

                    try {
                        ResponseTempStatus responseTempStatus = new Gson().fromJson(temperatureResponse, ResponseTempStatus.class);
                        if (responseTempStatus != null) {
                            double currentSavedTemp = responseTempStatus.getTemperature() / 10.0;
                            float currentTemperature = (float) (currentSavedTemp + offSet);
                            fireOnStart(currentTemperature);
                        } else {
                            logError("responseTempStatus", "Parsed object was null");
                            fireOnStart(0);
                        }
                    } catch (Exception e) {
                        logError("temperatureResponse", "Failed to parse: " + e.getMessage());
                        fireOnStart(0);
                    }

                } else {
                    logError("temperatureResponse", "empty or null string");
                    fireOnStart(0);
                }

            } else {
                logError("usbConnection", "Failed claimInterface");
                logError(TAG, "Failed to claim interface.");
                disconnect();
            }
        } else {
            logError("Failed to open", "USB connection.");
            logError(TAG, "Failed to open USB connection.");
            requestPermission(device);
        }
    }



//    public void openConnection(UsbDevice device) {
//        usbDevice = device;
//        usbInterface = device.getInterface(0);
//        inEndpoint = usbInterface.getEndpoint(0);
//        outEndpoint = usbInterface.getEndpoint(1);
//
//        logError(TAG + "getVendorId", String.valueOf(device.getVendorId()));
//        logError(TAG + "getProductId", String.valueOf(device.getProductId()));
//
//        usbConnection = usbManager.openDevice(device);
//
//        logError("openConnection", "method");
//
//        if (usbConnection != null) {
//
//            logError("usbConnection", "not null");
//
//            if (usbConnection.claimInterface(usbInterface, true)) {
//                logError("usbConnection", " claimInterface");
//                setBaudRateInternal();
//                connected = true;
//                checkAndStartReadingData();
//                SharedPreferencesManager preferencesManager = SharedPreferencesManager.getInstance(context);
//                String temperatureResponse = String.valueOf(preferencesManager.get(Constants.ResponseTempStatus, ""));
//                if (!temperatureResponse.isEmpty()) {
//                    logError("temperatureResponse ", "not empty");
//
//                    float offSet = Float.parseFloat(preferencesManager.get(Constants.TemperatureOffSet, 0.0).toString());
//                    ResponseTempStatus responseTempStatus = new Gson().fromJson(temperatureResponse, ResponseTempStatus.class);
//                    double currentSavedTemp = responseTempStatus.getTemperature() / 10;
//                    float currentTemperature = Float.parseFloat(String.valueOf((currentSavedTemp + offSet)));
//                    fireOnStart(currentTemperature);
//                } else {
//
//                    logError("temperatureResponse ", "empty");
//                    fireOnStart(0);
//
//                }
////                fireOnStart(0);
//            } else {
//                logError("usbConnection", "Failed claimInterface");
//
//                logError(TAG, "Failed to claim interface.");
//                disconnect();
//            }
//        } else {
//
//            logError("Failed to open", " USB connection.");
//            logError(TAG, "Failed to open USB connection.");
//            requestPermission(device);
//        }
//    }

    public void fireOnStart(float temperature) {
        try {
            SendToDevice sendToDevice = new SendToDevice();
            float offSet = Float.parseFloat(SharedPreferencesManager.getInstance(context).get(Constants.TemperatureOffSet, "0.0").toString());
            float setTemperature = Float.parseFloat(SharedPreferencesManager.getInstance(context).get(Constants.TemperatureSet, "0.0").toString());
            float weight = Float.parseFloat("0");

            sendToDevice.setCurtemperature(temperature + offSet);
            sendToDevice.setSettemperature(setTemperature);

            /// 31-12-2024 add isCIP
            sendToDevice.setCIP(isCipOn);
            sendToDevice.setLowlevel(isLowLevel);

            if (isCipOn) {
                sendToDevice.setWeight(100.0f);
                sendToDevice.setStatus(false);

                if(!isSendDataStop){
                    Log.d(TAG, "Connected:fireOnStart  <---> " + gson.toJson(sendToDevice));
                    sendData(gson.toJson(sendToDevice));
                }


            } else {
                sendToDevice.setWeight(weight);
                sendToDevice.setStatus(false);

                Log.d(TAG, "Connected:fireOnStart  <---> " + gson.toJson(sendToDevice));
                sendData(gson.toJson(sendToDevice));

            }



        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setBaudRateInternal() {
        // USB communication constants
        byte[] data = new byte[4];
        data[0] = (byte) (baudRate & 0xFF);
        data[1] = (byte) ((baudRate >> 8) & 0xFF);
        data[2] = (byte) ((baudRate >> 16) & 0xFF);
        data[3] = (byte) ((baudRate >> 24) & 0xFF);

        int requestType = 0x21; // USB_SETUP_HOST_TO_DEVICE | USB_TYPE_CLASS | USB_RECIP_INTERFACE
        int request = 0x20; // USB_SERIAL_SET_BAUD_RATE request
        int value = 0; // Zero-based interface number
        int index = 0; // Zero-based endpoint number

        int result = usbConnection.controlTransfer(requestType, request, value, index, data, data.length, 5000);
        logError("TAG", "setBaudRate: " + (result >= 0));
    }


    private void startReadingData() {
        if (executorService == null || executorService.isShutdown() || executorService.isTerminated()) {
            executorService = Executors.newSingleThreadExecutor();
        }

        try {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    readingDataThreadRunning = true;
                    StringBuilder accumulatedData = new StringBuilder();

                    while (connected) {
                        byte[] buffer = new byte[1024];
                        int bytesRead = usbConnection.bulkTransfer(inEndpoint, buffer, buffer.length, 0);

                        if (bytesRead > 0) {
                            String receivedData = new String(buffer, 0, bytesRead);
                            boolean icCalibResponse = receivedData.equalsIgnoreCase("1") ||
                                    receivedData.equalsIgnoreCase("2") ||
                                    receivedData.equalsIgnoreCase("3") ||
                                    receivedData.equalsIgnoreCase("4") ||
                                    receivedData.equalsIgnoreCase("0");

                            accumulatedData.append(receivedData);

                            if (accumulatedData.toString().contains("}") && !icCalibResponse) {
                                String acdStr = accumulatedData.toString();
                                int startIndex = acdStr.indexOf("{");
                                int endIndex = acdStr.indexOf("}", startIndex) + 1;

                                if (startIndex != -1 && endIndex > startIndex) {
                                    String completeData = acdStr.substring(startIndex, endIndex);
                                    SharedPreferencesManager preferencesManager = SharedPreferencesManager.getInstance(context);

                                    if (fromCalibration && sendToDevice != null) {
                                        sendData(gson.toJson(sendToDevice));
                                        return;
                                    }

                                    if (!completeData.matches("^[A-Za-z].*")) {
                                        if (currencyReceived && !completeData.contains("currentweight") &&
                                                !completeData.contains("status") &&
                                                !completeData.contains("setweight")) {

                                            if (sendToDevice != null) {
                                                sendData(gson.toJson(sendToDevice));
                                            }
                                        }

                                        if (completeData.contains("lowlevel") && !currencyReceived) {
                                            ResponseTempStatus responseTempStatus = new Gson().fromJson(completeData, ResponseTempStatus.class);
                                            preferencesManager.save(Constants.ResponseTempStatus, completeData);
                                            preferencesManager.save(Constants.CurrentTemperature, responseTempStatus.getTemperature().toString());

                                            float offSet = Float.parseFloat(preferencesManager.get(Constants.TemperatureOffSet, 0.0).toString());
                                            double currentSavedTemp = responseTempStatus.getTemperature() / 10;
                                            float currentTemperature = (float) (currentSavedTemp + offSet);

                                            fireOnStart(currentTemperature);
                                        }

                                        if (completeData.contains("status")) {
                                            ResponseMilkDispense milkDispense = new Gson().fromJson(completeData, ResponseMilkDispense.class);

//                                            if (milkDispense.getStatus() && isCipOn) {
//                                                Constants.saveLogs(context, "CIP Done");
//                                                isCipOn = false;
//                                                if (cipDialog != null && cipDialog.isShowing()) {
//                                                    cipDialog.dismiss();
//                                                }
//                                            }

                                            ResponseTempStatus responseTempStatus = new Gson().fromJson(preferencesManager.get(Constants.ResponseTempStatus, "").toString(), ResponseTempStatus.class);
                                            float offSet = Float.parseFloat(preferencesManager.get(Constants.TemperatureOffSet, 0.0).toString());
                                            double currentSavedTemp = responseTempStatus.getTemperature() / 10;
                                            float currentTemperature = (float) (currentSavedTemp + offSet);
                                            preferencesManager.save(Constants.ResponseMilkDispense, completeData);

                                            if (milkDispense.getStatus()) {
                                                fireOnStart(currentTemperature);
                                            }
                                        }

                                        if (readDataListener != null) {
                                            readDataListener.onReadData(completeData);
                                        }
                                    }

                                    accumulatedData.setLength(0); // Reset
                                }
                            } else {
                                if (icCalibResponse && readDataListener != null) {
                                    readDataListener.onReadData(receivedData);
                                    fromCalibration = false;
                                } else if (fromCalibration && sendToDevice != null) {
                                    sendData(gson.toJson(sendToDevice));
                                }
                            }
                        } else {
                            currencyReceived = false;
                        }

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    readingDataThreadRunning = false;
                }
            });
        } catch (RejectedExecutionException e) {
            Log.e(TAG, "startReadingData: Executor was shut down, restarting...", e);
            executorService = Executors.newSingleThreadExecutor(); // Reinitialize and retry
            startReadingData(); // Retry once
        }
    }



//    private void startReadingData() {
//        executorService.submit(new Runnable() {
//            @Override
//            public void run() {
//                readingDataThreadRunning = true;
//
//                StringBuilder accumulatedData = new StringBuilder();
//
//                while (connected) {
//                    byte[] buffer = new byte[1024];
//                    int bytesRead = usbConnection.bulkTransfer(inEndpoint, buffer, buffer.length, 0);
//
//                    if (bytesRead > 0) {
//                        String receivedData = new String(buffer, 0, bytesRead);
////                        Log.i(TAG, "run: ==> receivedData " + receivedData);
//                        boolean icCalibResponse = receivedData.equalsIgnoreCase("1") || receivedData.equalsIgnoreCase("2") || receivedData.equalsIgnoreCase("3") || receivedData.equalsIgnoreCase("4") || receivedData.equalsIgnoreCase("0");
//                        accumulatedData.append(receivedData);
//
//                        if (accumulatedData.toString().contains("}") && !icCalibResponse) {
//                            String acdStr = accumulatedData.toString();
//                            int startIndex = acdStr.indexOf("{");
//                            int endIndex = acdStr.indexOf("}", startIndex) + 1;
//                            // Process the complete JSON string
//                            String completeData = acdStr.substring(startIndex, endIndex);
//                            SharedPreferencesManager preferencesManager = SharedPreferencesManager.getInstance(context);
//                          //  Log.d(TAG, " run: ==>< completeData " + completeData);
//
//                            if (fromCalibration) {
//                                if (sendToDevice != null) {
////                                    Log.i(TAG, " run: Calibration Send " + gson.toJson(sendToDevice));
//
//                                    sendData(gson.toJson(sendToDevice));
//                                    return;
//                                }
//                            }
//                            if (!completeData.matches("^[A-Za-z].*")) {
////                                logError(TAG, inEndpoint.getMaxPacketSize() + " run:>><< completeData: receivedData " + completeData);
////                                Log.i(TAG, "run: ==> BOOL " + " CR " + (currencyReceived && !completeData.contains("currentweight") && !completeData.contains("status") && !completeData.contains("setweight")) + " <^^> " + new Gson().toJson(sendToDevice));
//
//                                if (currencyReceived && !completeData.contains("currentweight") && !completeData.contains("status") && !completeData.contains("setweight")) {
//                                    if (sendToDevice != null) {
//                                        Log.i(TAG, "run: ==> BOOL Resend" + icCalibResponse + " CR " + currencyReceived);
//
//                                        Log.i(TAG, " run: ==> Resend " + gson.toJson(sendToDevice));
//
//                                        sendData(gson.toJson(sendToDevice));
//                                    }
//                                }
//                                if (completeData.contains("lowlevel") && !currencyReceived) {
////                                    Log.d(TAG, " run:>> Condition One: Start " + completeData);
//
//                                    ResponseTempStatus responseTempStatus = new Gson().fromJson(completeData, ResponseTempStatus.class);
//                                    preferencesManager.save(Constants.ResponseTempStatus, completeData);
//                                    preferencesManager.save(Constants.CurrentTemperature, responseTempStatus.getTemperature().toString());
//                                    float offSet = Float.parseFloat(preferencesManager.get(Constants.TemperatureOffSet, 0.0).toString());
//                                    double currentSavedTemp = responseTempStatus.getTemperature() / 10;
//                                    float currentTemperature = Float.parseFloat(String.valueOf((currentSavedTemp + offSet)));
//
//                                    fireOnStart(currentTemperature);
//                                }
//                                if (completeData.contains("status")) {
//
//                                    Log.d(TAG, "run: ==> Condition TWO: receivedData " + completeData);
//
//                                    ResponseMilkDispense milkDispense = new Gson().fromJson(completeData, ResponseMilkDispense.class);
//
//                                    /// If status is true cip should be false and dialog will be close
////                                    if (milkDispense.getStatus() && isCipOn) {
////                                        logError("Status is truueeeee", milkDispense.getStatus().toString());
////
////                                        Constants.saveLogs(context, "CIP Done");
////
////                                        isCipOn = false;
////                                        if(cipDialog != null && cipDialog.isShowing()){
////                                            cipDialog.dismiss();
////                                        }
////
////
////                                    } else {
////
////                                        logError("Status is false", milkDispense.getStatus().toString());
////                                    }
//
//                                    ResponseTempStatus responseTempStatus = new Gson().fromJson(preferencesManager.get(Constants.ResponseTempStatus, "").toString(), ResponseTempStatus.class);
//                                    float offSet = Float.parseFloat(preferencesManager.get(Constants.TemperatureOffSet, 0.0).toString());
//                                    double currentSavedTemp = responseTempStatus.getTemperature() / 10;
//                                    float currentTemperature = Float.parseFloat(String.valueOf((currentSavedTemp + offSet)));
//                                    preferencesManager.save(Constants.ResponseMilkDispense, completeData);
//                                    if (milkDispense.getStatus()) {
//                                        fireOnStart(currentTemperature);
//                                    }
//
//                                }
//                                if (readDataListener != null) {
//
//                                    readDataListener.onReadData(completeData);
//                                }
//                            }
//                            // Reset accumulatedData for the next iteration
//                            accumulatedData.setLength(0);
//                        } else {
////                            Log.i(TAG, "run:ELSE  receivedData " + receivedData);
//                            if (icCalibResponse) {
//                                if (readDataListener != null) {
////                                    logError(TAG, "run:>><< Calibration RESP " + receivedData);
//                                    readDataListener.onReadData(receivedData);
//                                    fromCalibration = false;
//                                }
//                            } else if (fromCalibration) {
//                                if (sendToDevice != null) {
//                                    Log.i(TAG, " run: Calibration Send " + gson.toJson(sendToDevice));
//                                    sendData(gson.toJson(sendToDevice));
//                                }
//                            }
//                        }
//                    } else {
//                        currencyReceived = false;
//                    }
//                    try {
//                        Thread.sleep(1000); // Adjust the delay as needed
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                readingDataThreadRunning = false;
//
//            }
//        });
//    }


    public void checkAndStartReadingData() {
        logError(TAG, "checkAndStartReadingData: " + readingDataThreadRunning);
        startReadingData();
    }

    private boolean isCompleteJson(String data) {

        return data.startsWith("{") && data.endsWith("}");
    }

    public void sendData(String data) {

        if (connected) {
            byte[] buffer = data.getBytes();
            int bytesSent = usbConnection.bulkTransfer(outEndpoint, buffer, buffer.length, 0);
//            logError(TAG, "run:>><< sendData: " + data);
            logError(TAG, "run: ==> sendData: " + data);

            if (bytesSent < 0) {
                logError(TAG, "Error sending data." + data);
            }

            sendToDevice = new Gson().fromJson(data, SendToDevice.class);
            currencyReceived = sendToDevice.isStatus();
            fromCalibration = sendToDevice.isCalib();
            /*if (sendToDevice.isStatus()) {
            } else if (sendToDevice.isCalib()) {
                startReadingData();
            }*/
            try {
                Thread.sleep(1200);
                startReadingData();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
    }


    public void disconnect() {
        connected = false;
        unregisterPermissionReceiver();
        logError(TAG, "disconnect run: >>> " + connected);
//        Toast.makeText(context, "Disconnected!!!", Toast.LENGTH_SHORT).show();
        if (usbConnection != null) {
            usbConnection.releaseInterface(usbInterface);
            usbConnection.close();

        }

        executorService.shutdown();

    }

    private void unregisterPermissionReceiver() {
        try {

            context.unregisterReceiver(usbPermissionReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver not registered
            e.printStackTrace();
        }
    }

    public interface ReadDataListener {
        void onReadData(String data);
    }

    public final BroadcastReceiver usbPermissionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    boolean permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);

                    if (permissionGranted && usbDevice != null) {
                       /// Here I check that if milk vending device is found, then only open a connection

                        logError(TAG, " permissionGranted usbDevice not null");

                        openConnection(usbDevice);
                    } else {
                        logError(TAG, "USB permission denied.");
                    }

                    // Safe un registration without using a flag
                    try {
                        context.unregisterReceiver(this);
                    } catch (IllegalArgumentException e) {
                        Log.w(TAG, "Receiver already unregistered or not registered", e);
                    }
                }
            }
        }
    };



    private void logError(String tag, String message){
        Log.e(tag, message);
    }

}
