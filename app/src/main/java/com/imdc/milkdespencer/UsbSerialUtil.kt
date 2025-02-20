package com.imdc.milkdespencer

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbManager
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class UsbSerialUtil(context: Context, private val dataReceivedCallback: DataReceivedCallback?) {
    private val usbManager: UsbManager
    private var usbDevice: UsbDevice? = null
    private var usbConnection: UsbDeviceConnection? = null
    private var inEndpoint: UsbEndpoint? = null
    private var outEndpoint: UsbEndpoint? = null
    private var isReading = false

    init {
        usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    }

    fun connectToDevice(device: UsbDevice?): Boolean {
        if (usbConnection != null) {
            disconnectDevice()
        }
        usbDevice = device
        val usbInterface = usbDevice!!.getInterface(0)
        usbConnection = usbManager.openDevice(usbDevice)
        if (usbConnection != null) {
            if (usbConnection!!.claimInterface(usbInterface, true)) {
                if (setBaudRate(115200)) {
                    // Find the IN and OUT endpoints
                    inEndpoint = usbInterface.getEndpoint(0)
                    outEndpoint = usbInterface.getEndpoint(1)
                    return true
                } else {
                    disconnectDevice()
                }
            } else {
                Log.e("TAG", "Not connectedToDevice:  ")
            }
        }
        return false
    }

    fun disconnectDevice() {
        if (usbConnection != null) {
            usbConnection!!.releaseInterface(usbDevice!!.getInterface(0))
            usbConnection!!.close()
            usbConnection = null
        }
        usbDevice = null
        inEndpoint = null
        outEndpoint = null
    }

    @Throws(IOException::class)
    fun sendData(json: JSONObject): Int {
        if (usbConnection != null && outEndpoint != null) {
            val jsonString = json.toString()
            val data = jsonString.toByteArray()
            return usbConnection!!.bulkTransfer(outEndpoint, data, data.size, TIMEOUT)
        }
        throw IOException("USB device is not connected")
        /* if (usbConnection != null && outEndpoint != null) {
            return usbConnection.bulkTransfer(outEndpoint, data, data.length, TIMEOUT);
        }
        throw new IOException("USB device is not connected");*/
    }

    @Throws(IOException::class, JSONException::class)
    fun receiveData(): String {
        if (usbConnection != null && inEndpoint != null) {
            val receivedDataBuilder = StringBuilder()
            val lineSeparator = System.getProperty("line.separator")
            var startTime = System.currentTimeMillis()
            var elapsedTime: Long
            while (true) {
                val buffer = ByteArray(10000) // Adjust the buffer size as needed
                val bytesRead =
                    usbConnection!!.bulkTransfer(inEndpoint, buffer, buffer.size, TIMEOUT)
                if (bytesRead <= 0) {
                    elapsedTime = System.currentTimeMillis() - startTime
                    if (elapsedTime > TIMEOUT) {
                        // Break the loop if the elapsed time exceeds TIMEOUT
                        break
                    }
                    // Wait for a short duration to avoid busy waiting
                    try {
                        Thread.sleep(10)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    continue  // Continue the loop to check for more data after a short delay
                }
                val receivedData = String(buffer, 0, bytesRead)

                // Append the received data to the StringBuilder
                receivedDataBuilder.append(receivedData)

                // Check if a complete JSON object has been received
                val receivedDataString = receivedDataBuilder.toString()
                if (receivedDataString.contains("}")) {
                    // Parse the JSON object
                    val startIndex = receivedDataString.indexOf("{")
                    val endIndex = receivedDataString.indexOf("}") + 1
                    if (startIndex >= 0 && endIndex > startIndex) {
                        return receivedDataString.substring(startIndex, endIndex)
                    }
                }

                // Reset the start time for the timeout check
                startTime = System.currentTimeMillis()
            }
        }
        throw IOException("USB device is not connected")
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
    private fun setBaudRate(baudRate: Int): Boolean {
        // Example code to set the baud rate, assuming the device supports it
        // This code is fictional and device-specific, and you'll need to consult your device's documentation
        // or driver to configure the baud rate.

        // For example, if the device uses xcontrol requests to set baud rate:
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
            TIMEOUT
        )
        Log.e("TAG", "setBaudRate: " + (result >= 0))
        return result >= 0 // Return true if successful
    }

    val isDataAvailable: Boolean
        get() {
            if (usbConnection != null && inEndpoint != null) {
                val buffer = ByteArray(1)
                val bytesRead = usbConnection!!.bulkTransfer(
                    inEndpoint,
                    buffer,
                    buffer.size,
                    0
                ) // Non-blocking check
                return bytesRead > 0
            }
            return false
        }

    fun startReadingData() {
        isReading = true
        Thread {
            while (isReading) {
                var receivedData = ""
                try {
                    receivedData = receiveData()
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                if (dataReceivedCallback != null && !receivedData.isEmpty()) {
                    dataReceivedCallback.onDataReceived(receivedData)
                }
            }
        }.start()
    }

    fun stopReadingData() {
        isReading = false
    }

    interface DataReceivedCallback {
        fun onDataReceived(data: String?)
    }

    companion object {
        const val ACTION_USB_PERMISSION = "com.imdc.milkdespencer.ACTION_USB_PERMISSION"
        private const val TIMEOUT = 1000
    }
}
