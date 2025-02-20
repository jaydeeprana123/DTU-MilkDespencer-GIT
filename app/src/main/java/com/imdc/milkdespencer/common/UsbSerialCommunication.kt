package com.imdc.milkdespencer.common

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.imdc.milkdespencer.CashCollectorActivity.Companion.instance
import com.imdc.milkdespencer.common.Constants.Companion.saveLogs
import com.imdc.milkdespencer.common.SharedPreferencesManager.Companion.getInstance
import com.imdc.milkdespencer.models.ResponseMilkDispense
import com.imdc.milkdespencer.models.ResponseTempStatus
import com.imdc.milkdespencer.models.SendToDevice
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class UsbSerialCommunication(private val context: Context) {
    private val usbManager: UsbManager?
    private val handler: Handler
    private val executorService: ExecutorService
    @JvmField
    var connected = false
    var fromCalibration = false
    var readingDataThreadRunning = false
    var gson = GsonBuilder().serializeSpecialFloatingPointValues().create()
    var sendToDevice: SendToDevice? = null
    private var readDataListener: ReadDataListener? = null
    private var baudRate = 115200 // Default baud rate, change as needed
    private var usbDevice: UsbDevice? = null
    private var usbConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var inEndpoint: UsbEndpoint? = null
    private var outEndpoint: UsbEndpoint? = null
    private var currencyReceived = false
    fun setReadDataListener(listener: ReadDataListener?) {
        readDataListener = listener
    }

    fun setBaudRate(baudRate: Int) {
        this.baudRate = baudRate
        // Call this method whenever the baud rate needs to be changed (e.g., after connecting)
        updateBaudRate()
    }

    private fun updateBaudRate() {
        // Adjust the baud rate of the USB connection
        if (usbConnection != null && usbInterface != null) {
            usbConnection!!.releaseInterface(usbInterface)
            usbConnection!!.close()
            openConnection(usbDevice)
        }
    }

    fun connect() {
        Log.e("connect ", "method")
        if (usbManager == null) {
            Log.e(TAG, "UsbManager is null. Make sure USB is supported on this device.")
            return
        }

        // Find the first available USB device
        val device = findAnyUsbDevice()
        if (device == null) {
            Toast.makeText(
                context,
                "No USB device found. Please connect the device",
                Toast.LENGTH_SHORT
            ).show()
            Log.e(TAG, "No USB device found.")
            return
        } else {
            Log.d(
                TAG, """Connected: ${device.productId} <---> ${device.vendorId}
 $device"""
            )
            //            String temperatureResponse = String.valueOf(preferencesManager.get(Constants.ResponseTempStatus, null));
//            Toast.makeText(context, "TEMPERATURE STATUS " + preferencesManager.hasValue(Constants.ResponseTempStatus), Toast.LENGTH_SHORT).show();
            val preferencesManager = getInstance(
                instance!!
            )
            if (preferencesManager!!.hasValue(Constants.ResponseTempStatus)) {
                val responseTempStatus = Gson().fromJson(
                    preferencesManager[Constants.ResponseTempStatus, ""].toString(),
                    ResponseTempStatus::class.java
                )
                if (responseTempStatus != null) {
                    val currentSavedTemp = responseTempStatus.temperature / 10
                    val offSet =
                        preferencesManager[Constants.TemperatureOffSet, 0.0].toString().toFloat()
                    val currentTemperature = (currentSavedTemp + offSet).toString().toFloat()
                    fireOnStart(currentTemperature)
                } else {
                    fireOnStart(0f)
                }
            } else {
                fireOnStart(0f)
            }


//            SharedPreferencesManager preferencesManager = SharedPreferencesManager.getInstance(context);
//            if (preferencesManager.hasValue(Constants.ResponseTempStatus)) {
//                String temperatureResponse = String.valueOf(preferencesManager.get(Constants.ResponseTempStatus, null));
//                Log.e(TAG, "connect: " + temperatureResponse);
//                if (!temperatureResponse.isEmpty()) {
//                    float offSet = Float.parseFloat(preferencesManager.get(Constants.TemperatureOffSet, 0.0).toString());
//                    ResponseTempStatus responseTempStatus = new Gson().fromJson(temperatureResponse, ResponseTempStatus.class);
//                    double currentSavedTemp = responseTempStatus.getTemperature() / 10;
//                    float currentTemperature = Float.parseFloat(String.valueOf((currentSavedTemp + offSet)));
//                    fireOnStart(currentTemperature);
//                } else {
//                    fireOnStart(0);
//                }
//            } else {
//            }

//            fireOnStart(0);
        }

        // Request permission
        requestPermission(device)
    }

    private fun findAnyUsbDevice(): UsbDevice? {
        val usbDevices = usbManager!!.deviceList
        for (device in usbDevices.values) {
            if (device.vendorId == 4292 && device.productId == 60000) {
                Log.e(TAG, "findAnyUsbDevice: CONDI $device")
                return device
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
        }*/return null
    }

    private fun requestPermission(device: UsbDevice?) {
        val preferencesManager = getInstance(
            context
        )
        val isPermissionGranted =
            preferencesManager!![Constants.PREF_PERMISSION_GRANTED, false] as Boolean

//        if(!isPermissionGranted){
//            PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
//
//            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
//            context.registerReceiver(usbPermissionReceiver, filter);
//
//            usbManager.requestPermission(device, permissionIntent);
//        }
        val permissionIntent =
            PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), 0)
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        context.registerReceiver(usbPermissionReceiver, filter)
        usbManager!!.requestPermission(device, permissionIntent)
    }

    fun openConnection(device: UsbDevice?) {
        usbDevice = device
        usbInterface = device!!.getInterface(0)
        inEndpoint = usbInterface!!.getEndpoint(0)
        outEndpoint = usbInterface!!.getEndpoint(1)
        usbConnection = usbManager!!.openDevice(device)
        Log.e("openConnection", "method")
        if (usbConnection != null) {
            Log.e("usbConnection", "not null")
            if (usbConnection!!.claimInterface(usbInterface, true)) {
                Log.e("usbConnection", " claimInterface")
                setBaudRateInternal()
                connected = true
                checkAndStartReadingData()
                val preferencesManager = getInstance(
                    context
                )
                val temperatureResponse =
                    preferencesManager!![Constants.ResponseTempStatus, ""].toString()
                if (!temperatureResponse.isEmpty()) {
                    Log.e("temperatureResponse ", "not empty")
                    val offSet =
                        preferencesManager[Constants.TemperatureOffSet, 0.0].toString().toFloat()
                    val responseTempStatus =
                        Gson().fromJson(temperatureResponse, ResponseTempStatus::class.java)
                    val currentSavedTemp = responseTempStatus.temperature / 10
                    val currentTemperature = (currentSavedTemp + offSet).toString().toFloat()
                    fireOnStart(currentTemperature)
                } else {
                    Log.e("temperatureResponse ", "empty")
                    fireOnStart(0f)
                }
                //                fireOnStart(0);
            } else {
                Log.e("usbConnection", "Failed claimInterface")
                Log.e(TAG, "Failed to claim interface.")
                disconnect()
            }
        } else {
            Log.e("Failed to open", " USB connection.")
            Log.e(TAG, "Failed to open USB connection.")
            requestPermission(device)
        }
    }

    fun fireOnStart(temperature: Float) {
        try {
            val sendToDevice = SendToDevice()
            val offSet =
                getInstance(context)!![Constants.TemperatureOffSet, "0.0"].toString().toFloat()
            val setTemperature =
                getInstance(context)!![Constants.TemperatureSet, "0.0"].toString().toFloat()
            val weight = "0".toFloat()
            sendToDevice.curtemperature = temperature + offSet
            sendToDevice.settemperature = setTemperature

            /// 31-12-2024 add isCIP
            sendToDevice.isCIP = isCipOn
            if (isCipOn) {
                sendToDevice.weight = 2.0f
                sendToDevice.isStatus = true
            } else {
                sendToDevice.weight = weight
                sendToDevice.isStatus = false
            }
            Log.d(TAG, "Connected:fireOnStart  <---> " + gson.toJson(sendToDevice))
            sendData(gson.toJson(sendToDevice))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setBaudRateInternal() {
        // USB communication constants
        val data = ByteArray(4)
        data[0] = (baudRate and 0xFF).toByte()
        data[1] = (baudRate shr 8 and 0xFF).toByte()
        data[2] = (baudRate shr 16 and 0xFF).toByte()
        data[3] = (baudRate shr 24 and 0xFF).toByte()
        val requestType = 0x21 // USB_SETUP_HOST_TO_DEVICE | USB_TYPE_CLASS | USB_RECIP_INTERFACE
        val request = 0x20 // USB_SERIAL_SET_BAUD_RATE request
        val value = 0 // Zero-based interface number
        val index = 0 // Zero-based endpoint number
        val result = usbConnection!!.controlTransfer(
            requestType,
            request,
            value,
            index,
            data,
            data.size,
            5000
        )
        Log.e("TAG", "setBaudRate: " + (result >= 0))
    }

    private fun startReadingData() {
        executorService.submit(Runnable {
            readingDataThreadRunning = true
            val accumulatedData = StringBuilder()
            while (connected) {
                val buffer = ByteArray(1024)
                val bytesRead = usbConnection!!.bulkTransfer(inEndpoint, buffer, buffer.size, 0)
                if (bytesRead > 0) {
                    val receivedData = String(buffer, 0, bytesRead)
                    //                        Log.i(TAG, "run: ==> receivedData " + receivedData);
                    val icCalibResponse =
                        receivedData.equals("1", ignoreCase = true) || receivedData.equals(
                            "2",
                            ignoreCase = true
                        ) || receivedData.equals("3", ignoreCase = true) || receivedData.equals(
                            "4",
                            ignoreCase = true
                        ) || receivedData.equals("0", ignoreCase = true)
                    accumulatedData.append(receivedData)
                    if (accumulatedData.toString().contains("}") && !icCalibResponse) {
                        val acdStr = accumulatedData.toString()
                        val startIndex = acdStr.indexOf("{")
                        val endIndex = acdStr.indexOf("}", startIndex) + 1
                        // Process the complete JSON string
                        val completeData = acdStr.substring(startIndex, endIndex)
                        val preferencesManager = getInstance(
                            context
                        )
                        Log.d(TAG, " run: ==>< completeData $completeData")
                        if (fromCalibration) {
                            if (sendToDevice != null) {
//                                    Log.i(TAG, " run: Calibration Send " + gson.toJson(sendToDevice));
                                sendData(gson.toJson(sendToDevice))
                                return@Runnable
                            }
                        }
                        if (!completeData.matches("^[A-Za-z].*".toRegex())) {
//                                Log.e(TAG, inEndpoint.getMaxPacketSize() + " run:>><< completeData: receivedData " + completeData);
//                                Log.i(TAG, "run: ==> BOOL " + " CR " + (currencyReceived && !completeData.contains("currentweight") && !completeData.contains("status") && !completeData.contains("setweight")) + " <^^> " + new Gson().toJson(sendToDevice));
                            if (currencyReceived && !completeData.contains("currentweight") && !completeData.contains(
                                    "status"
                                ) && !completeData.contains("setweight")
                            ) {
                                if (sendToDevice != null) {
                                    Log.i(
                                        TAG,
                                        "run: ==> BOOL Resend$icCalibResponse CR $currencyReceived"
                                    )
                                    Log.i(TAG, " run: ==> Resend " + gson.toJson(sendToDevice))
                                    sendData(gson.toJson(sendToDevice))
                                }
                            }
                            if (completeData.contains("lowlevel") && !currencyReceived) {
//                                    Log.d(TAG, " run:>> Condition One: Start " + completeData);
                                val responseTempStatus =
                                    Gson().fromJson(completeData, ResponseTempStatus::class.java)
                                preferencesManager!!.save(
                                    Constants.ResponseTempStatus,
                                    completeData
                                )
                                preferencesManager.save(
                                    Constants.CurrentTemperature,
                                    responseTempStatus.temperature.toString()
                                )
                                val offSet =
                                    preferencesManager[Constants.TemperatureOffSet, 0.0].toString()
                                        .toFloat()
                                val currentSavedTemp = responseTempStatus.temperature / 10
                                val currentTemperature =
                                    (currentSavedTemp + offSet).toString().toFloat()
                                fireOnStart(currentTemperature)
                            }
                            if (completeData.contains("status")) {
                                Log.d(TAG, "run: ==> Condition TWO: receivedData $completeData")
                                val milkDispense =
                                    Gson().fromJson(completeData, ResponseMilkDispense::class.java)

                                /// If status is true cip should be false and dialog will be close
                                if (milkDispense.status && isCipOn) {
                                    Log.e("Status is truueeeee", milkDispense.status.toString())
                                    saveLogs(context, "CIP Done")
                                    isCipOn = false
                                    if (Constants.cipDialog != null && Constants.cipDialog!!.isShowing) {
                                        Constants.cipDialog!!.dismiss()
                                    }
                                } else {
                                    Log.e("Status is false", milkDispense.status.toString())
                                }
                                val responseTempStatus = Gson().fromJson(
                                    preferencesManager!![Constants.ResponseTempStatus, ""].toString(),
                                    ResponseTempStatus::class.java
                                )
                                val offSet =
                                    preferencesManager[Constants.TemperatureOffSet, 0.0].toString()
                                        .toFloat()
                                val currentSavedTemp = responseTempStatus.temperature / 10
                                val currentTemperature =
                                    (currentSavedTemp + offSet).toString().toFloat()
                                preferencesManager.save(
                                    Constants.ResponseMilkDispense,
                                    completeData
                                )
                                if (milkDispense.status) {
                                    fireOnStart(currentTemperature)
                                }
                            }
                            if (readDataListener != null) {
                                readDataListener!!.onReadData(completeData)
                            }
                        }
                        // Reset accumulatedData for the next iteration
                        accumulatedData.setLength(0)
                    } else {
//                            Log.i(TAG, "run:ELSE  receivedData " + receivedData);
                        if (icCalibResponse) {
                            if (readDataListener != null) {
//                                    Log.e(TAG, "run:>><< Calibration RESP " + receivedData);
                                readDataListener!!.onReadData(receivedData)
                                fromCalibration = false
                            }
                        } else if (fromCalibration) {
                            if (sendToDevice != null) {
                                Log.i(TAG, " run: Calibration Send " + gson.toJson(sendToDevice))
                                sendData(gson.toJson(sendToDevice))
                            }
                        }
                    }
                } else {
                    currencyReceived = false
                }
                try {
                    Thread.sleep(1000) // Adjust the delay as needed
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            readingDataThreadRunning = false
        })
    }

    fun checkAndStartReadingData() {
        Log.e(TAG, "checkAndStartReadingData: $readingDataThreadRunning")
        startReadingData()
    }

    private fun isCompleteJson(data: String): Boolean {
        return data.startsWith("{") && data.endsWith("}")
    }

    fun sendData(data: String) {
        if (connected) {
            val buffer = data.toByteArray()
            val bytesSent = usbConnection!!.bulkTransfer(outEndpoint, buffer, buffer.size, 0)
            //            Log.e(TAG, "run:>><< sendData: " + data);
            Log.e(TAG, "run: ==> sendData: $data")
            if (bytesSent < 0) {
                Log.e(TAG, "Error sending data.$data")
            }
            sendToDevice = Gson().fromJson(data, SendToDevice::class.java)

            sendToDevice?.let {
                currencyReceived = it.isStatus
                fromCalibration = it.isCalib
            }


            /*if (sendToDevice.isStatus()) {
            } else if (sendToDevice.isCalib()) {
                startReadingData();
            }*/try {
                Thread.sleep(1200)
                startReadingData()
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
        }
    }

    fun disconnect() {
        connected = false
        unregisterPermissionReceiver()
        Log.e(TAG, "disconnect run: >>> $connected")
        //        Toast.makeText(context, "Disconnected!!!", Toast.LENGTH_SHORT).show();
        if (usbConnection != null) {
            usbConnection!!.releaseInterface(usbInterface)
            usbConnection!!.close()
        }
        executorService.shutdown()
    }

    private fun unregisterPermissionReceiver() {
        try {
            context.unregisterReceiver(usbPermissionReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
            e.printStackTrace()
        }
    }

    interface ReadDataListener {
        fun onReadData(data: String?)
    }

    val usbPermissionReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    val usbDevice = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    val permissionGranted =
                        intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    if (permissionGranted && usbDevice != null) {
                        openConnection(usbDevice)
                    } else {
                        Log.e(TAG, "USB permission denied.")
                    }
                    context.unregisterReceiver(this)
                }
            }
        }
    }

    init {
        usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        handler = Handler(Looper.getMainLooper())
        executorService = Executors.newSingleThreadExecutor()
    }

    companion object {
        private const val TAG = "UsbSerialCommunication"
        private const val ACTION_USB_PERMISSION = "com.imdc.milkdespencer.USB_PERMISSION"
        private const val TIMEOUT = 1000
        var isCipOn = false
        fun isValidJson(json: String?): Boolean {
            return try {
                val parser = JsonParser()
                parser.parse(json)
                true
            } catch (e: Exception) {
                false
            }
        }

        fun intToByteArray(value: Int): ByteArray {
            return byteArrayOf(
                (value ushr 24).toByte(),
                (value ushr 16).toByte(),
                (value ushr 8).toByte(),
                value.toByte()
            )
        }
    }
}
