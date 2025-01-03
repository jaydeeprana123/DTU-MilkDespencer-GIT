package com.imdc.milkdespencer.common;

import static com.imdc.milkdespencer.common.Constants.PREF_PERMISSION_GRANTED;
import static com.imdc.milkdespencer.common.Constants.cipDialog;

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
import com.imdc.milkdespencer.models.ResponseMilkDispense;
import com.imdc.milkdespencer.models.ResponseTempStatus;
import com.imdc.milkdespencer.models.SendToDevice;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UsbSerialCommunication {

    private static final String TAG = "UsbSerialCommunication";
    private static final String ACTION_USB_PERMISSION = "com.imdc.milkdespencer.USB_PERMISSION";
    private static final int TIMEOUT = 1000;
    private final Context context;
    private final UsbManager usbManager;
    private final Handler handler;
    private final ExecutorService executorService;
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
        if (usbManager == null) {
            Log.e(TAG, "UsbManager is null. Make sure USB is supported on this device.");
            return;
        }

        // Find the first available USB device
        UsbDevice device = findAnyUsbDevice();
        if (device == null) {
            Toast.makeText(context, "No USB device found. Please connect the device", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No USB device found.");
            return;
        } else {
            Log.d(TAG, "Connected: " + device.getProductId() + " <---> " + device.getVendorId() + "\n " + device);
//            String temperatureResponse = String.valueOf(preferencesManager.get(Constants.ResponseTempStatus, null));
//            Toast.makeText(context, "TEMPERATURE STATUS " + preferencesManager.hasValue(Constants.ResponseTempStatus), Toast.LENGTH_SHORT).show();

            /*SharedPreferencesManager preferencesManager = SharedPreferencesManager.getInstance(context);
            if (preferencesManager.hasValue(Constants.ResponseTempStatus)) {
                String temperatureResponse = String.valueOf(preferencesManager.get(Constants.ResponseTempStatus, null));
                Log.e(TAG, "connect: " + temperatureResponse);
                if (!temperatureResponse.isEmpty()) {
                    float offSet = Float.parseFloat(preferencesManager.get(Constants.TemperatureOffSet, 0.0).toString());
                    ResponseTempStatus responseTempStatus = new Gson().fromJson(temperatureResponse, ResponseTempStatus.class);
                    double currentSavedTemp = responseTempStatus.getTemperature() / 10;
                    float currentTemperature = Float.parseFloat(String.valueOf((currentSavedTemp + offSet)));
                    fireOnStart(currentTemperature);
                } else {
                    fireOnStart(0);
                }
            } else {
            }*/
            fireOnStart(0);

        }

        // Request permission
        requestPermission(device);
    }

    private UsbDevice findAnyUsbDevice() {
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        for (UsbDevice device : usbDevices.values()) {
            if (device.getVendorId() == 4292 && device.getProductId() == 60000) {
                Log.e(TAG, "findAnyUsbDevice: CONDI " + device);
                return device;
            }
        }

        /*if (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            Log.e(TAG, "findAnyUsbDevice: CONDI " + device);
            Log.e(TAG, usbDevices.values() + " <deviceIteratorSIZE ---- usbDevicesSIZE > " + usbDevices.size());
            if (device.getVendorId() == 4292 && device.getProductId() == 60000) {
                Log.e(TAG, "findAnyUsbDevice: CONDI " + device);
                return device;
            }
        }*/

        return null;
    }

    private void requestPermission(UsbDevice device) {

        SharedPreferencesManager preferencesManager = SharedPreferencesManager.getInstance(context);
        boolean isPermissionGranted = (boolean) preferencesManager.get(PREF_PERMISSION_GRANTED, false);

        if(!isPermissionGranted){
            PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);

            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            context.registerReceiver(usbPermissionReceiver, filter);

            usbManager.requestPermission(device, permissionIntent);
        }


    }

    public void openConnection(UsbDevice device) {
        usbDevice = device;
        usbInterface = device.getInterface(0);
        inEndpoint = usbInterface.getEndpoint(0);
        outEndpoint = usbInterface.getEndpoint(1);

        usbConnection = usbManager.openDevice(device);
        if (usbConnection != null) {
            if (usbConnection.claimInterface(usbInterface, true)) {
                setBaudRateInternal();
                connected = true;
                checkAndStartReadingData();
                SharedPreferencesManager preferencesManager = SharedPreferencesManager.getInstance(context);
                String temperatureResponse = String.valueOf(preferencesManager.get(Constants.ResponseTempStatus, ""));
                if (!temperatureResponse.isEmpty()) {
                    float offSet = Float.parseFloat(preferencesManager.get(Constants.TemperatureOffSet, 0.0).toString());
                    ResponseTempStatus responseTempStatus = new Gson().fromJson(temperatureResponse, ResponseTempStatus.class);
                    double currentSavedTemp = responseTempStatus.getTemperature() / 10;
                    float currentTemperature = Float.parseFloat(String.valueOf((currentSavedTemp + offSet)));
                    fireOnStart(currentTemperature);
                } else {
                    fireOnStart(0);

                }
//                fireOnStart(0);
            } else {
                Log.e(TAG, "Failed to claim interface.");
                disconnect();
            }
        } else {
            Log.e(TAG, "Failed to open USB connection.");
            requestPermission(device);
        }
    }

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

            if (isCipOn) {
                sendToDevice.setWeight(2.0f);
                sendToDevice.setStatus(true);
            } else {
                sendToDevice.setWeight(weight);
                sendToDevice.setStatus(false);
            }


            Log.d(TAG, "Connected:fireOnStart  <---> " + gson.toJson(sendToDevice));
            sendData(gson.toJson(sendToDevice));
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
        Log.e("TAG", "setBaudRate: " + (result >= 0));
    }

    private void startReadingData() {
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
//                        Log.i(TAG, "run: ==> receivedData " + receivedData);
                        boolean icCalibResponse = receivedData.equalsIgnoreCase("1") || receivedData.equalsIgnoreCase("2") || receivedData.equalsIgnoreCase("3") || receivedData.equalsIgnoreCase("4") || receivedData.equalsIgnoreCase("0");
                        accumulatedData.append(receivedData);

                        if (accumulatedData.toString().contains("}") && !icCalibResponse) {
                            String acdStr = accumulatedData.toString();
                            int startIndex = acdStr.indexOf("{");
                            int endIndex = acdStr.indexOf("}", startIndex) + 1;
                            // Process the complete JSON string
                            String completeData = acdStr.substring(startIndex, endIndex);
                            SharedPreferencesManager preferencesManager = SharedPreferencesManager.getInstance(context);
                            Log.d(TAG, " run: ==>< completeData " + completeData);

                            if (fromCalibration) {
                                if (sendToDevice != null) {
//                                    Log.i(TAG, " run: Calibration Send " + gson.toJson(sendToDevice));

                                    sendData(gson.toJson(sendToDevice));
                                    return;
                                }
                            }
                            if (!completeData.matches("^[A-Za-z].*")) {
//                                Log.e(TAG, inEndpoint.getMaxPacketSize() + " run:>><< completeData: receivedData " + completeData);
//                                Log.i(TAG, "run: ==> BOOL " + " CR " + (currencyReceived && !completeData.contains("currentweight") && !completeData.contains("status") && !completeData.contains("setweight")) + " <^^> " + new Gson().toJson(sendToDevice));

                                if (currencyReceived && !completeData.contains("currentweight") && !completeData.contains("status") && !completeData.contains("setweight")) {
                                    if (sendToDevice != null) {
                                        Log.i(TAG, "run: ==> BOOL Resend" + icCalibResponse + " CR " + currencyReceived);

                                        Log.i(TAG, " run: ==> Resend " + gson.toJson(sendToDevice));

                                        sendData(gson.toJson(sendToDevice));
                                    }
                                }
                                if (completeData.contains("lowlevel") && !currencyReceived) {
//                                    Log.d(TAG, " run:>> Condition One: Start " + completeData);

                                    ResponseTempStatus responseTempStatus = new Gson().fromJson(completeData, ResponseTempStatus.class);
                                    preferencesManager.save(Constants.ResponseTempStatus, completeData);
                                    preferencesManager.save(Constants.CurrentTemperature, responseTempStatus.getTemperature().toString());
                                    float offSet = Float.parseFloat(preferencesManager.get(Constants.TemperatureOffSet, 0.0).toString());
                                    double currentSavedTemp = responseTempStatus.getTemperature() / 10;
                                    float currentTemperature = Float.parseFloat(String.valueOf((currentSavedTemp + offSet)));

                                    fireOnStart(currentTemperature);
                                }
                                if (completeData.contains("status")) {

                                    Log.d(TAG, "run: ==> Condition TWO: receivedData " + completeData);

                                    ResponseMilkDispense milkDispense = new Gson().fromJson(completeData, ResponseMilkDispense.class);

                                    /// If status is true cip should be false and dialog will be close
                                    if (milkDispense.getStatus() && isCipOn) {
                                        Log.e("Status is truueeeee", milkDispense.getStatus().toString());

                                        isCipOn = false;
                                        if(cipDialog != null && cipDialog.isShowing()){
                                            cipDialog.dismiss();
                                        }


                                    } else {

                                        Log.e("Status is false", milkDispense.getStatus().toString());
                                    }

                                    ResponseTempStatus responseTempStatus = new Gson().fromJson(preferencesManager.get(Constants.ResponseTempStatus, "").toString(), ResponseTempStatus.class);
                                    float offSet = Float.parseFloat(preferencesManager.get(Constants.TemperatureOffSet, 0.0).toString());
                                    double currentSavedTemp = responseTempStatus.getTemperature() / 10;
                                    float currentTemperature = Float.parseFloat(String.valueOf((currentSavedTemp + offSet)));
                                    preferencesManager.save(Constants.ResponseMilkDispense, completeData);
                                    if (milkDispense.getStatus()) {
                                        fireOnStart(currentTemperature);
                                    }

                                }
                                if (readDataListener != null) {

                                    readDataListener.onReadData(completeData);
                                }
                            }
                            // Reset accumulatedData for the next iteration
                            accumulatedData.setLength(0);
                        } else {
//                            Log.i(TAG, "run:ELSE  receivedData " + receivedData);
                            if (icCalibResponse) {
                                if (readDataListener != null) {
//                                    Log.e(TAG, "run:>><< Calibration RESP " + receivedData);
                                    readDataListener.onReadData(receivedData);
                                    fromCalibration = false;
                                }
                            } else if (fromCalibration) {
                                if (sendToDevice != null) {
                                    Log.i(TAG, " run: Calibration Send " + gson.toJson(sendToDevice));
                                    sendData(gson.toJson(sendToDevice));
                                }
                            }
                        }
                    } else {
                        currencyReceived = false;
                    }
                    try {
                        Thread.sleep(1000); // Adjust the delay as needed
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                readingDataThreadRunning = false;

            }
        });
    }


    public void checkAndStartReadingData() {
        Log.e(TAG, "checkAndStartReadingData: " + readingDataThreadRunning);
        startReadingData();
    }

    private boolean isCompleteJson(String data) {

        return data.startsWith("{") && data.endsWith("}");
    }

    public void sendData(String data) {

        if (connected) {
            byte[] buffer = data.getBytes();
            int bytesSent = usbConnection.bulkTransfer(outEndpoint, buffer, buffer.length, 0);
//            Log.e(TAG, "run:>><< sendData: " + data);
            Log.e(TAG, "run: ==> sendData: " + data);

            if (bytesSent < 0) {
                Log.e(TAG, "Error sending data." + data);
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
        Log.e(TAG, "disconnect run: >>> " + connected);
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
                        openConnection(usbDevice);
                    } else {
                        Log.e(TAG, "USB permission denied.");
                    }

                    context.unregisterReceiver(this);
                }
            }
        }
    };


}
