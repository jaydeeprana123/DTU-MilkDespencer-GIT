package com.imdc.milkdespencer

import android.Manifest
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CompoundButton
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.ftdi.j2xx.D2xxManager
import com.ftdi.j2xx.D2xxManager.D2xxException
import com.ftdi.j2xx.D2xxManager.FtDeviceInfoListNode
import com.ftdi.j2xx.FT_Device
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.imdc.milkdespencer.adapter.CurrencyAdapter
import com.imdc.milkdespencer.common.Constants
import com.imdc.milkdespencer.common.LottieAddCashDialog
import com.imdc.milkdespencer.common.LottieDialog
import com.imdc.milkdespencer.common.SharedPreferencesManager
import com.imdc.milkdespencer.common.UsbSerialCommunication
import com.imdc.milkdespencer.enums.ScreenEnum
import com.imdc.milkdespencer.models.ResponseMilkDispense
import com.imdc.milkdespencer.models.ResponseTempStatus
import com.imdc.milkdespencer.models.SendToDevice
import com.imdc.milkdespencer.network.ApiManager
import com.imdc.milkdespencer.roomdb.AppDatabase
import com.imdc.milkdespencer.roomdb.interfaces.TransactionDao
import device.itl.sspcoms.BarCodeReader
import device.itl.sspcoms.DeviceEvent
import device.itl.sspcoms.DeviceEventListener
import device.itl.sspcoms.DeviceEventType
import device.itl.sspcoms.DeviceFileUpdateListener
import device.itl.sspcoms.DeviceSetupListener
import device.itl.sspcoms.SSPDevice
import device.itl.sspcoms.SSPDeviceType
import device.itl.sspcoms.SSPSystem
import device.itl.sspcoms.SSPUpdate
import device.itl.sspcoms.SSPUpdate.SSPDownloadStatus
import org.json.JSONException
import org.json.JSONObject
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.text.SimpleDateFormat

class CashCollectorActivity : AppCompatActivity(), DeviceSetupListener, DeviceEventListener,
    DeviceFileUpdateListener {
    var tempIndex = 0
    private var handler: Handler? = Handler() // Create a Handler instance
    private var runnable: Runnable? = null // Declare the Runnable
    private var timeoutHandler: Handler? = null
    private var timeoutRunnable: Runnable? = null
    private val batteryReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Get the current battery status
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

            // Check if the device is charging
            val isCharging =
                status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            if (isCharging) {
            } else {
                Constants.saveLogs(this@CashCollectorActivity, "Lost Electricity")
                var paymentObject: JSONObject? = null
                val paymentJson = preferencesManager!![Constants.PaymentCashReceived, ""].toString()
                if (!paymentJson.isEmpty()) {
                    try {
                        Log.e("paymentJson", "is available")
                        paymentObject = JSONObject(paymentJson)
                        if (paymentObject.has("amount")) {
                            // Safely parse the amount as a float
                            val amount = paymentObject.optDouble("amount", 0.0)
                            Log.e("Amount", "is availableeee")
                            showFailedProcessDoneDialog(amount)
                        } else {
                            Log.e("Amount", "is not available")
                        }
                    } catch (e: JSONException) {
                        Log.e("PaymentError", "Error parsing payment JSON", e)
                        // Handle error (optional: show error dialog or default value)
                    }
                }
            }
        }
    }
    var lottieAddCashDialog: LottieAddCashDialog? = null
    var loadingDialog: AlertDialog? = null
    var lottieDialog: LottieDialog? = null
    var grdCurrencyView: GridView? = null
    var btnBackToHome: Button? = null
    var gson = GsonBuilder().serializeSpecialFloatingPointValues().create()
    var apiManager: ApiManager? = null
    var mUsbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                // never come here(when attached, go to onNewIntent)
                openDevice()
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                closeDevice()
                bvDisplay!!.visibility = View.INVISIBLE
                fab!!.visibility = View.VISIBLE
                fab!!.isEnabled = true
            }
        }
    }
    private var sspUpdate: SSPUpdate? = null
    fun DeviceDisconnected(dev: SSPDevice?) {
        eventValues!![0] = "DISCONNECTED!!!"
        eventValues!![1] = ""
        //        Toast.makeText(cashCollectorActivity, "CASH Collector Device " + eventValues[0], Toast.LENGTH_SHORT).show();
        connectToDevices()
        adapterEvents!!.notifyDataSetChanged()
    }

    private fun connectToDevices() {
        if (ftDev != null) {
            // Toast.makeText(CashCollectorActivity.this, milkSetTemperature + " MilkBase Price " + milkBasePrice, Toast.LENGTH_SHORT).show();
        } else {
            //   Toast.makeText(CashCollectorActivity.this, "Please Wait initiating the Connection!!! ", Toast.LENGTH_SHORT).show();
            openDevice()
        }
        usbSerialCommunication!!.connect()
        //        UsbSerialCommunication.currentClass = cashCollectorActivity.getClass().getSimpleName();
        usbSerialCommunication!!.setBaudRate(115200)
    }

    /*
     * When payment is done. Send for Vending the milk*/
    private fun sendForMilkVending(ev: DeviceEvent) {
        if (ev.value == selectedCurrency!!.replace("₹", "").toDouble()) {
            Log.e(
                "TAG", """Currency: ${ev.currency}<C  Vd>${String.format("%.2f", ev.value)}
 AMT ${ev.value} Condition ${ev.value == selectedCurrency!!.replace("₹", "").toDouble()}"""
            )
            try {
                val responseTempStatus = Gson().fromJson(
                    preferencesManager!![Constants.ResponseTempStatus, ""].toString(),
                    ResponseTempStatus::class.java
                )
                val milkSellingPrice =
                    preferencesManager!![Constants.MilkBasePrice, "0.0"].toString().toFloat()
                val offSet =
                    preferencesManager!![Constants.TemperatureOffSet, 0.0].toString().toFloat()
                val DENSITY_OF_MILK =
                    preferencesManager!![Constants.MilkDensityPref, "0.0"].toString().toFloat()
                val weight = (ev.value / milkSellingPrice).toString().toFloat() * DENSITY_OF_MILK

//                float weight = Float.parseFloat(String.valueOf((ev.value / milkSellingPrice)));
                val currentSavedTemp = responseTempStatus.temperature / 10
                val currentTemperature = (currentSavedTemp + offSet).toString().toFloat()
                Log.e(TAG, "DisplayEvents: SEND COMMAND " + milkSellingPrice + " " + ev.value)
                val sendToDevice = SendToDevice()
                sendToDevice.weight = weight
                sendToDevice.isStatus = true
                sendToDevice.curtemperature = currentTemperature
                Log.e("milkSetTemperature sendForMilkVending", milkSetTemperature.toString())
                sendToDevice.settemperature = milkSetTemperature

//                UsbSerialCommunication.currentClass = "CCA";
                val gson = GsonBuilder().serializeSpecialFloatingPointValues().create()
                Log.e(TAG, "DisplayEvents: SEND COMMAND " + gson.toJson(sendToDevice))
                //                Toast.makeText(CashCollectorActivity.this, "Command SEND TO DEVICE On START ->\n " + new Gson().toJson(sendToDevice), Toast.LENGTH_LONG).show();
                lottieDialog!!.show()

                /// Here after 15 minute if status is not getting as a true.
                // Dialog will be close and transaction will be add in the database as a TIME OUT
                // Initialize the Handler and Runnable
                timeoutHandler = Handler(Looper.getMainLooper())
                timeoutRunnable = Runnable { handleMilkSendingTimeout(lottieDialog, ev.value, 0f) }

                // Post the Runnable with a delay
                timeoutHandler!!.postDelayed(
                    timeoutRunnable!!,
                    (15 * 60 * 1000).toLong()
                ) // 15 minutes


                /// Send Data to the usb Serial Communication
                usbSerialCommunication!!.sendData(gson.toJson(sendToDevice))
                usbSerialCommunication!!.setReadDataListener { data ->
                    Log.d(
                        TAG,
                        "DisplayEvents:onReadData: " + data + "\n status " + data.contains("status")
                    )
                    if (data.contains("status")) {
                        val milkDispense = Gson().fromJson(data, ResponseMilkDispense::class.java)
                        Log.i(TAG, "run: ==>< onReadData: " + Gson().toJson(milkDispense))
                        if (milkDispense != null) {
                            Log.e("Cashcollector status outside", milkDispense.status.toString())

                            //  double percentage = (milkDispense.getCurTemperature() / milkDispense.getSetTemperature()) * 100;
//                                if (lottieDialog != null) {
//                                    if (percentage > 0) {
//                                        lottieDialog.setPercentage(percentage);
//                                    }
//                                }

                            /// Here true status getting two times.
                            // So put condition that if lottieDialog is showing that time only goes to this condition
                            if (milkDispense.status && lottieDialog!!.isShowing) {
                                val volumeOfMilk =
                                    ((milkDispense.currentWeight) / DENSITY_OF_MILK).toFloat()
                                Log.e("VOLUME OF MILK", volumeOfMilk.toString())

                                /// when status get as a true, timeOutHandler removed here
                                timeoutHandler!!.removeCallbacks(timeoutRunnable!!)
                                if (lottieDialog!!.isShowing) {
                                    lottieDialog!!.dismiss()
                                }
                                tempIndex++
                                Log.e("tempIndex ", tempIndex.toString())
                                Log.e("milkDispense status ", milkDispense.status.toString())
                                try {
                                    if (lottieDialog!!.isShowing) {
                                        lottieDialog!!.dismiss()
                                    }
                                    preferencesManager!!.save(
                                        Constants.CurrentTemperature,
                                        currentSavedTemp
                                    )
                                    preferencesManager = SharedPreferencesManager.getInstance(
                                        instance
                                    )
                                    deviceCom!!.SetEscrowAction(SSPSystem.BillAction.Accept)
                                    /*float offSet = Float.parseFloat(preferencesManager.get(Constants.TemperatureOffSet, 0.0).toString());
                                                    double currentSavedTemp = responseTempStatus.getTemperature() / 10;
                                                    float currentTemperature = Float.parseFloat(String.valueOf((currentSavedTemp + offSet)));
                                                    usbSerialCommunication.fireOnStart(currentTemperature);
                                                    usbSerialCommunication.fireOnStart(currentTemperature);*/
//                                        usbSerialCommunication.fireOnStart(currentTemperature);
                                    //                                        bttnAccept.performClick();
                                    showAndProcessDoneDialog(sendToDevice, ev.value, volumeOfMilk)
                                } catch (e: Exception) {
                                    throw RuntimeException(e)
                                }
                            } else {
                                Log.e("milkDispense status ", milkDispense.status.toString())
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        } else {
            Constants.showAlertDialog(
                cashCollectorActivity,
                "Please insert correct note",
                "Please insert correct note and selected note didn't match!!! please enter correct note!"
            )
            deviceCom!!.SetEscrowAction(SSPSystem.BillAction.Reject)
        }
    }

    /*If 15 minutes done and status is not getting as a true.
   Transaction will be added as a FAILED*/
    private fun handleMilkSendingTimeout(
        lottieDialog: LottieDialog?,
        amt: Double,
        volumeOfMilk: Float
    ) {
        if (lottieDialog != null && lottieDialog.isShowing) {
            lottieDialog.dismiss()
            Thread {
                try {
                    val date: String =
                        SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis())
                    val time: String =
                        SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())
                    val transactionDao: TransactionDao =
                        AppDatabase.getInstance(this@CashCollectorActivity).transactionDao()
                    Constants.insertTransaction(
                        this@CashCollectorActivity,
                        transactionDao,
                        "CASH",
                        "",
                        date,
                        time,
                        (amt),
                        "FAILED",
                        "",
                        volumeOfMilk,
                        ""
                    )
                    Log.e("Time is out", "After 15 minutes")
                    goToHomeScreen()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }
    }

    fun showAndProcessDoneDialog(
        sendToDevice: SendToDevice,
        currency: Double,
        volumeOfMilk: Float
    ) {
        Log.e("showAndProcessDoneDialog", "Show")
        runOnUiThread {
            val builder = AlertDialog.Builder(this@CashCollectorActivity)
            val inflater = layoutInflater
            val view = inflater.inflate(R.layout.dialog_lottie, null)
            val lottieAnimationView =
                view.findViewById<LottieAnimationView>(R.id.lottieAnimationView)
            val lottieAnimationViewDone =
                view.findViewById<LottieAnimationView>(R.id.lottieAnimationViewDone)
            val tvProgressDialog = view.findViewById<TextView>(R.id.tvProgressDialog)
            val btnDone = view.findViewById<MaterialButton>(R.id.doneButton)
            val tvProcessDoneText = view.findViewById<TextView>(R.id.tvProcessDoneText)
            val tvDispenseVolume = view.findViewById<TextView>(R.id.tvDispenseVolume)
            val tvOpenTheDoor = view.findViewById<TextView>(R.id.tvOpenTheDoor)
            btnDone.visibility = View.VISIBLE
            lottieAnimationView.visibility = View.GONE
            lottieAnimationViewDone.visibility = View.VISIBLE
            tvProcessDoneText.visibility = View.VISIBLE
            tvOpenTheDoor.visibility = View.VISIBLE
            tvProgressDialog.visibility = View.GONE

            /// Added on 16-1
            tvDispenseVolume.visibility = View.VISIBLE
            /// Here I convert volume of milk into string and set 3 digits after dot(.)
            val truncatedValueOfMilkVolume = String.format("%.2f", volumeOfMilk).toFloat()
            tvDispenseVolume.text =
                getString(R.string.dispense_volume) + " " + truncatedValueOfMilkVolume + " L"
            lottieAnimationViewDone.setAnimation(R.raw.process_done)
            lottieAnimationViewDone.repeatMode = LottieDrawable.RESTART
            lottieAnimationViewDone.playAnimation()

            // Customize the LottieAnimationView and TextView here
            builder.setView(view)
            builder.setCancelable(false) // Set to true if you want the dialog to be cancellable
            val dialog = builder.create()
            dialog.show()


            /// Initialize the handler
            handler = Handler()
            val screenTimeOut =
                preferencesManager!![Constants.ScreenTimeOutPref, "0.0"].toString().toLong()

            // Define the Runnable task
            runnable = Runnable {

                // Task to execute after delay
                dialog.dismiss()
                closeDevice()
                //                        onDestroy();
                insertDataOnProcessDone(currency, volumeOfMilk, sendToDevice.curtemperature)
            }

            // Post the Runnable with a 15-second delay
            handler!!.postDelayed(runnable!!, screenTimeOut * 1000)
            btnDone.setOnClickListener {
                //  If User clicks on the done button. Runnable should be close
                // Cancel the delayed task
                if (handler != null && runnable != null) {
                    handler!!.removeCallbacks(runnable!!)
                }
                dialog.dismiss()
                closeDevice()
                //                        onDestroy();
                insertDataOnProcessDone(currency, volumeOfMilk, sendToDevice.curtemperature)

//                doPostTransaction(Constants.PostTransactionURL);
            }
        }
    }

    private fun showFailedProcessDoneDialog(amt: Double) {
        runOnUiThread {
            Constants.showAcceptDialog(
                this@CashCollectorActivity,
                "Error",
                "Lost Electricity Connection!! Please Try after sometime.",
                { dialog1: DialogInterface, which: Int ->
                    Thread(
                        Runnable {
                            Log.e("Error Electricity", "First")
                            try {
                                val dateFormat = "yyyy-MM-dd"
                                val timeFormat = "HH:mm:ss"
                                val dateFormatter = SimpleDateFormat(dateFormat)
                                val timeFormatter = SimpleDateFormat(timeFormat)
                                val date = dateFormatter.format(System.currentTimeMillis())
                                val time = timeFormatter.format(System.currentTimeMillis())
                                // Print the combined date and time
                                Log.e("date", (date)!!)
                                val transactionDao =
                                    AppDatabase.getInstance(this@CashCollectorActivity)
                                        .transactionDao()
                                assert(date != null)
                                val transactionId = Constants.insertTransaction(
                                    this@CashCollectorActivity,
                                    transactionDao,
                                    "CASH",
                                    "",
                                    date,
                                    time,
                                    amt,
                                    "FAILED",
                                    "",
                                    0f,
                                    ""
                                )
                                Log.e(TAG, "onCreate: $transactionId")
                                Log.e(
                                    TAG,
                                    "onCreate: " + Gson().toJson(transactionDao.allTransactions)
                                )


                                /// Close current dialog
                                dialog1.dismiss()
                                /// Go to Home screen
                                goToHomeScreen()

//                                Intent intent = new Intent(CashCollectorActivity.this, MainActivity.class);
//                                startActivity(intent);
//                                finish();
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }).start()
                }) { dialog1: DialogInterface, which: Int ->
                Thread(object : Runnable {
                    override fun run() {
                        Log.e("Error Electricity", "Second")
                        try {
                            val dateFormat: String = "yyyy-MM-dd"
                            val timeFormat: String = "HH:mm:ss"
                            val dateFormatter: SimpleDateFormat = SimpleDateFormat(dateFormat)
                            val timeFormatter: SimpleDateFormat = SimpleDateFormat(timeFormat)
                            val date: String? = dateFormatter.format(System.currentTimeMillis())
                            val time: String = timeFormatter.format(System.currentTimeMillis())
                            // Print the combined date and time
                            val transactionDao: TransactionDao =
                                AppDatabase.getInstance(this@CashCollectorActivity).transactionDao()
                            assert(date != null)
                            val transactionId: Long = Constants.insertTransaction(
                                this@CashCollectorActivity,
                                transactionDao,
                                "CASH",
                                "",
                                date,
                                time,
                                amt,
                                "FAILED",
                                "",
                                0f,
                                ""
                            )
                            Log.e(TAG, "onCreate: " + transactionId)
                            Log.e(
                                TAG,
                                "onCreate: " + Gson().toJson(transactionDao.getAllTransactions())
                            )


                            /// Close current dialog
                            dialog1.dismiss()

                            /// Go to Home screen
                            goToHomeScreen()

//                                Intent intent = new Intent(CashCollectorActivity.this, MainActivity.class);
//                                startActivity(intent);
//                                finish();
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }).start()
            }
        }
    }

    fun DisplayEvents(ev: DeviceEvent) {
        when (ev.event) {
            DeviceEventType.CommunicationsFailure -> {}
            DeviceEventType.Ready -> {
                eventValues!![0] = "Ready"
                eventValues!![1] = ""
            }

            DeviceEventType.BillRead -> {
                eventValues!![0] = "Reading"
                eventValues!![1] = ""
            }

            DeviceEventType.BillEscrow -> {
                eventValues!![0] = "Bill Escrow"
                eventValues!![1] = ev.currency + " " + String.format("%.2f", ev.value)
                val inflater = LayoutInflater.from(instance)
                val view = inflater.inflate(R.layout.dialog_note_detected, null)
                preferencesManager = SharedPreferencesManager.getInstance(
                    instance
                )
                val builder = AlertDialog.Builder(
                    instance!!
                )
                builder.setView(view)
                val dialog = builder.create()
                val submitBtn = view.findViewById<MaterialButton>(R.id.btnSubmitToDevice)
                val cancelBtn = view.findViewById<MaterialButton>(R.id.btnCancel)
                val tvCurrencyAmt = view.findViewById<TextView>(R.id.tvCurrencyDetectedAmt)
                val ivCurrency = view.findViewById<ImageView>(R.id.ivLogo)
                val tvMessage = view.findViewById<TextView>(R.id.tvCurrencyMessage)
                Log.e(TAG, "DisplayEvents: " + ev.value)
                if (eventValues != null) {
                    var msg = ""
                    if (eventValues!![1] != null) {
                        val milkSellingPrice = milkBasePrice.toString().toFloat()
                        val offSet =
                            preferencesManager?.get(Constants.TemperatureOffSet, "0.0").toString()
                                .toFloat()
                        val milkDensity =
                            preferencesManager?.get(Constants.MilkDensityPref, "0.0").toString()
                                .toFloat()
                        val weight = (ev.value / milkSellingPrice).toString()
                            .toFloat() * milkDensity //TODO : multiply with Den.
                        var volumeToDisplay = (ev.value / milkSellingPrice).toString().toFloat()
                        volumeToDisplay = String.format("%.2f", volumeToDisplay).toFloat()
                        Log.e(TAG, "DisplayEvents: milkDensity $milkDensity")
                        when (ev.value.toInt()) {
                            10 -> ivCurrency.setImageResource(R.drawable.ic_ten)
                            20 -> ivCurrency.setImageResource(R.drawable.ic_twenty)
                            50 -> ivCurrency.setImageResource(R.drawable.ic_fifty)
                            100 -> ivCurrency.setImageResource(R.drawable.ic_hundred)
                            200 -> ivCurrency.setImageResource(R.drawable.ic_two_hundred)
                            500 -> ivCurrency.setImageResource(R.drawable.ic_five_hundred)
                            else -> ivCurrency.setImageResource(R.drawable.pay_with_cash)
                        }

                        //  msg = eventValues[1] + " is detected you will get " + volumeToDisplay + " liters of Milk.\n Please ensure the door is closed starting the dispensation!! Press Start to Confirm!!!";
                        msg = "Ensure the door is closed."
                        tvMessage.text = msg
                        tvCurrencyAmt.text = eventValues!![1] + " (" + volumeToDisplay + "Ltr)"
                    }
                }
                cancelBtn.setOnClickListener { v: View? ->
                    deviceCom!!.SetEscrowAction(SSPSystem.BillAction.Reject)
                    dialog.dismiss()
                }
                submitBtn.setOnClickListener { v: View? ->
                    val milkSellingPrice: Float = milkBasePrice.toString().toFloat()
                    var volumeToDisplay: Float = (ev.value / milkSellingPrice).toString().toFloat()
                    volumeToDisplay = String.format("%.2f", volumeToDisplay).toFloat()

                    /// Check that volume amount is more than 5 lites
                    if (volumeToDisplay > 5) {

                        // If door is open then close the cash machine and send to the home page
                        deviceCom!!.SetEscrowAction(SSPSystem.BillAction.Reject)
                        dialog.dismiss()
                        showAlertExceedLimit()
                    } else {
                        val responseTempStatus: ResponseTempStatus = Gson().fromJson(
                            preferencesManager?.get(
                                Constants.ResponseTempStatus, ""
                            ).toString(), ResponseTempStatus::class.java
                        )

                        /// Here it will check that door is open or close
                        // If door is close then allow to start milking
                        if (!responseTempStatus.connectivity) {

                            /// Here if submit button is pressed,
                            // Payment is set as a received and amount will be save in a shared preference
                            val paymentObject: JSONObject = JSONObject()
                            try {
                                paymentObject.put("name", "Milk Vending Machine")
                                paymentObject.put("description", "Payment For Milk")
                                paymentObject.put("currency", "INR")
                                paymentObject.put("amount", ev.value)
                            } catch (e: JSONException) {
                                throw RuntimeException(e)
                            }

                            // Convert JSONObject to String
                            val paymentObjectString: String = paymentObject.toString()
                            /// Save into shared preference
                            preferencesManager?.save(
                                Constants.PaymentCashReceived,
                                paymentObjectString
                            )
                            sendForMilkVending(ev)
                            dialog.dismiss()
                        } else {

                            // If door is open then close the cash machine and send to the home page
                            deviceCom!!.SetEscrowAction(SSPSystem.BillAction.Reject)
                            dialog.dismiss()
                            goToHomeScreen()
                        }
                    }
                }
                // Show the dialog
                if (selectedCurrency != null && !selectedCurrency!!.isEmpty()) {
                    lottieAddCashDialog!!.isShowing
                    lottieAddCashDialog!!.dismiss()
                    dialog.show()
                } else {
                    Constants.showAlertDialog(
                        cashCollectorActivity,
                        "Please Select the Amount",
                        "Please select the amount before inserting the currency!"
                    )
                    deviceCom!!.SetEscrowAction(SSPSystem.BillAction.Reject)
                }
            }

            DeviceEventType.BillStacked -> {}
            DeviceEventType.BillReject -> {
                eventValues!![0] = "Bill Reject"
                eventValues!![1] = ""
                if (swEscrow!!.isChecked) {
                    bttnAccept!!.visibility = View.INVISIBLE
                    bttnReject!!.visibility = View.INVISIBLE
                }
            }

            DeviceEventType.BillJammed -> {
                eventValues!![0] = "Bill jammed"
                eventValues!![1] = ""
            }

            DeviceEventType.BillFraud -> {
                eventValues!![0] = "Bill Fraud"
                eventValues!![1] = ev.currency + " " + String.format("%.2f", ev.value)
            }

            DeviceEventType.BillCredit -> {
                eventValues!![0] = "Bill Credit"
                eventValues!![1] = ev.currency + " " + String.format("%.2f", ev.value)
            }

            DeviceEventType.Full -> {
                eventValues!![0] = "Bill Cashbox full"
                eventValues!![1] = ""
            }

            DeviceEventType.Initialising -> {}
            DeviceEventType.Disabled -> {
                eventValues!![0] = "Disabled"
                eventValues!![1] = ""
            }

            DeviceEventType.SoftwareError -> {
                eventValues!![0] = "Software error"
                eventValues!![1] = ""
            }

            DeviceEventType.AllDisabled -> {
                eventValues!![0] = "All channels disabled"
                eventValues!![1] = ""
            }

            DeviceEventType.CashboxRemoved -> {
                eventValues!![0] = "Cashbox removed"
                eventValues!![1] = ""
            }

            DeviceEventType.CashboxReplaced -> {
                eventValues!![0] = "Cashbox replaced"
                eventValues!![1] = ""
            }

            DeviceEventType.NotePathOpen -> {
                eventValues!![0] = "Note path open"
                eventValues!![1] = ""
            }

            DeviceEventType.BarCodeTicketEscrow -> {
                eventValues!![0] = "Barcode ticket escrow:"
                eventValues!![1] = ev.currency
                if (swEscrow!!.isChecked) {
                    bttnAccept!!.visibility = View.VISIBLE
                    bttnReject!!.visibility = View.VISIBLE
                }
            }

            DeviceEventType.BarCodeTicketStacked -> {
                eventValues!![0] = "Barcode ticket stacked"
                eventValues!![1] = ""
            }

            else -> {}
        }
        Log.e("TAG", "DisplayEvents: " + ev.event)
        adapterEvents!!.notifyDataSetChanged()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        Log.e("Cash collector ", "Screen")
        screenTimeOut()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        grdCurrencyView = findViewById(R.id.gridViewCurrency)
        btnBackToHome = findViewById(R.id.btnBackToHome)
        bvDisplay = findViewById(R.id.content_bill_validator)
        bvDisplay?.setVisibility(View.INVISIBLE)
        cashCollectorActivity = this
        instance = this
        preferencesManager = SharedPreferencesManager.getInstance(this)
        /// When user comes first delete the previously saved payment data in shared preference
        preferencesManager?.delete(Constants.PaymentCashReceived)
        lottieDialog = LottieDialog(instance!!)
        lottieAddCashDialog = LottieAddCashDialog(this)
        val adapter = CurrencyAdapter(this)
        grdCurrencyView?.adapter = adapter
        usbSerialCommunication = UsbSerialCommunication(
            applicationContext
        )
        val battertyFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, battertyFilter)
        progress = ProgressDialog(this@CashCollectorActivity)
        /* ask for permission to storeage read  */
        val permissionCheck =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                txtConnect!!.text =
                    "This app requires access to the downloads directory in order to load download files."
                txtConnect!!.visibility = View.VISIBLE
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST_READ_STORAGE
                )
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST_READ_STORAGE
                )
            }
        }
        title = "Pay With Cash"
        listEvents = findViewById(R.id.listEvents)
        listChannels = findViewById(R.id.listChannels)
        eventValues = arrayOf("", "")
        channelValues = ArrayList()
        adapterEvents = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            android.R.id.text1,
            eventValues!!
        )
        listEvents?.adapter = adapterEvents
        adapterChannels = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            android.R.id.text1,
            channelValues
        )
        listChannels?.adapter = adapterChannels
        bttnAccept = findViewById(R.id.bttnAccept)
        bttnReject = findViewById(R.id.bttnReject)
        txtFirmware = findViewById(R.id.txtFirmware)
        //        txtFirmware.setText(getResources().getString(R.string.firmware_title));
        txtDevice = findViewById(R.id.txtDevice)
        //        txtDevice.setText(getResources().getString(R.string.device_title));
        txtDataset = findViewById(R.id.txtDataset)
        //        txtDataset.setText(getResources().getString(R.string.dataset_title));
        txtSerial = findViewById(R.id.txtSerialNumber)
        //        txtSerial.setText(getResources().getString(R.string.serial_number_title));
        prgConnect = findViewById(R.id.progressBarConnect)
        txtConnect = findViewById(R.id.txtConnection)
        try {
            ftD2xx = D2xxManager.getInstance(this)
        } catch (ex: D2xxException) {
            Log.e("SSP FTmanager", ex.toString())
        }
        val filter = IntentFilter()
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        filter.priority = 500
        this.registerReceiver(mUsbReceiver, filter)
        deviceCom = ITLDeviceCom()
        deviceCom!!.setDeviceSetupListener(this)
        deviceCom!!.setDeviceEventListener(this)
        deviceCom!!.setDeviceFileUpdateListener(this)
        fab = findViewById(R.id.fab)
        fab?.visibility = View.GONE
        btnBackToHome?.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                if (handler != null && runnable != null) {
                    handler!!.removeCallbacks(runnable!!)
                }

                // Remove the Runnable from the Handler to avoid memory leaks
                if (timeoutHandler != null && timeoutRunnable != null) {
                    timeoutHandler!!.removeCallbacks(timeoutRunnable!!)
                }
                goToHomeScreen()
            }
        })

        /*fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDevice();
                if (ftDev != null) {
                    prgConnect.setVisibility(View.VISIBLE);
                    txtConnect.setVisibility(View.VISIBLE);
                    fab.setEnabled(false);

                    deviceCom.setup(ftDev, 0x00, false, false, 0);
                    deviceCom.start();
                } else {
                    Toast.makeText(MainActivity.this, "No USB connection detected!", Toast.LENGTH_SHORT).show();
                }
            }
        });*/grdCurrencyView?.setOnItemClickListener(object : OnItemClickListener {
            override fun onItemClick(adapterView: AdapterView<*>?, view: View, i: Int, l: Long) {

//                If User clicks on the grid item. Runnable should be close
                // Cancel the delayed task
                if (handler != null && runnable != null) {
                    handler!!.removeCallbacks(runnable!!)
                }
                selectedCurrency = grdCurrencyView?.adapter?.getItem(i).toString()
                milkBasePrice =
                    preferencesManager?.get(Constants.MilkBasePrice, "0.0").toString().toFloat()
                milkSetTemperature =
                    preferencesManager?.get(Constants.TemperatureSet, "0.0").toString().toFloat()
                lottieAddCashDialog!!.show()
                lottieAddCashDialog!!.setCancelable(false)
                val btnCancel = lottieAddCashDialog!!.findViewById<MaterialButton>(R.id.btnCancel)
                btnCancel.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(view: View) {
                        lottieAddCashDialog!!.dismiss()
                        goToHomeScreen()
                    }
                })
                if (ftDev != null) {
                    // Toast.makeText(CashCollectorActivity.this, milkSetTemperature + " MilkBase Price " + milkBasePrice, Toast.LENGTH_SHORT).show();
                } else {
                    //  Toast.makeText(CashCollectorActivity.this, "Please Wait initiating the Connection!!! ", Toast.LENGTH_SHORT).show();
                }
                openDevice()
            }
        })
        /**
         * Escrow enable/disable toggle
         */
        swEscrow = findViewById(R.id.swEscrow)
        swEscrow?.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
                deviceCom!!.SetEscrowMode(isChecked)
            }
        })
        /**
         * Device enable/disable toggle
         */
        val swDisable = findViewById<SwitchCompat>(R.id.swEnable)
        swDisable.isChecked = true
        swDisable.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
                deviceCom!!.SetDeviceEnable(isChecked)
            }
        })
        /**
         * Accept a bill from escrow button
         */
        bttnAccept = findViewById(R.id.bttnAccept)
        bttnAccept?.setVisibility(View.INVISIBLE)
        bttnAccept?.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
//                deviceCom.SetEscrowAction(SSPSystem.BillAction.Accept);
                Log.e(TAG, "onClick: clicked!!!")
                bttnReject?.visibility = View.INVISIBLE
                bttnAccept?.visibility = View.INVISIBLE


                /// Go to Home Page
                goToHomeScreen()
                //                Intent intent = new Intent(CashCollectorActivity.this, MainActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                startActivity(intent);
            }
        })
        /**
         * Reject a bill from escrow button
         */
        bttnReject = findViewById(R.id.bttnReject)
        bttnReject?.setVisibility(View.INVISIBLE)
        bttnReject?.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                deviceCom!!.SetEscrowAction(SSPSystem.BillAction.Reject)
                bttnReject?.setVisibility(View.INVISIBLE)
                bttnAccept?.setVisibility(View.INVISIBLE)
            }
        })
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        //        openDevice();
        connectToDevices()
    }

    override fun onDestroy() {
        unregisterReceiver(mUsbReceiver)
        unregisterReceiver(batteryReceiver)
        //        usbSerialCommunication.disconnect();

        /// Here if handler and runnable remove
        if (handler != null && runnable != null) {
            handler!!.removeCallbacks(runnable!!)
        }

        // Remove the Runnable from the Handler to avoid memory leaks
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler!!.removeCallbacks(timeoutRunnable!!)
        }
        super.onDestroy()
    }

    private fun ClearDisplay() {
        progress!!.progress = 0
        //        txtFirmware.setText(getResources().getString(R.string.firmware_title));
//        txtDevice.setText(getResources().getString(R.string.device_title));
//        txtDataset.setText(getResources().getString(R.string.dataset_title));
//        txtSerial.setText(getResources().getString(R.string.serial_number_title));
        adapterChannels!!.clear()
        adapterChannels!!.notifyDataSetChanged()
        eventValues!![0] = ""
        eventValues!![1] = ""
        adapterEvents!!.notifyDataSetChanged()
    }

    //    @Override
    //    public boolean onCreateOptionsMenu(Menu menu) {
    //        // Inflate the menu; this adds items to the action bar if it is present.
    //        getMenuInflater().inflate(R.menu.menu_main, menu);
    //
    //        MenuItem menuItem = menu.findItem(R.id.action_home);
    //        // Set the tint color dynamically
    //        Drawable icon = menuItem.getIcon();
    //        if (icon != null) {
    //            icon.mutate(); // Ensure the drawable is mutable
    //            icon.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN);
    //        }
    //
    //        downloadFileSelect = menu.getItem(0);
    //        downloadFileSelect.setEnabled(false);
    //
    //        return true;
    //    }
    //
    //    @Override
    //    public boolean onOptionsItemSelected(MenuItem item) {
    //
    //
    //        if (item.getItemId() == R.id.action_home) {
    //            Log.e("Home button", "Pressed");
    //            Intent intent = new Intent(CashCollectorActivity.this, MainActivity.class);
    //            startActivity(intent);
    //            finish();
    //        }
    //        // Handle item selection
    //        /*switch (item.getItemId()) {
    //            case R.id.action_downloadFile:
    //                openFolder();
    //                return true;
    //            case R.id.action_shutdown:
    //                deviceCom.Stop();
    //                closeDevice();
    //                finish();
    //            default:
    //                return super.onOptionsItemSelected(item);
    //        }*/
    //
    //        return super.onOptionsItemSelected(item);
    //    }
    fun openFolder() {
        if (deviceCom == null) {
            return
        }
        val devcode = deviceCom!!.GetDeviceCode()
        if (devcode < 0) {
        }

        /*Intent intent = new Intent(this, ListFiles.class);
        // send the current device code
        intent.putExtra("deviceCode", (byte) devcode);
        startActivityForResult(intent, 123);*/
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_PERMISSIONS_REQUEST_READ_STORAGE) { // If request is cancelled, the result arrays are empty.
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // permission was granted, yay! Do the
                // contacts-related task you need to do.
            } else {

                // permission denied, boo! Disable the
                // functionality that depends on this permission.
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 123 && resultCode == RESULT_OK) {
            var path: String? =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    .toString()
            path += "/"
            var flname: String? = ""
            if (data!!.hasExtra("filename")) {
                flname = data.getStringExtra("filename")
                path += flname
            } else {
                txtDevice!!.setText(R.string.no_file_data_error)
                return
            }
            sspUpdate = SSPUpdate(flname)
            try {
                val up = File(path)
                sspUpdate!!.fileData = ByteArray(up.length().toInt())
                val dis = DataInputStream(FileInputStream(up))
                dis.readFully(sspUpdate!!.fileData)
                dis.close()
                sspUpdate!!.SetFileData()
                ClearDisplay()
                deviceCom!!.SetSSPDownload(sspUpdate)
            } catch (e: IOException) {
                e.printStackTrace()
                //   txtEvents.append(R.string.unable_to_load + "\r\n");
            }
        }
    }

    private fun openDevice() {
        if (ftDev != null) {
            if (ftDev!!.isOpen) {
                // if open and run thread is stopped, start thread
                SetConfig(9600, 8.toByte(), 2.toByte(), 0.toByte(), 0.toByte())
                ftDev!!.purge((D2xxManager.FT_PURGE_TX.toInt() or D2xxManager.FT_PURGE_RX.toInt()).toByte())
                ftDev!!.restartInTask()
                return
            }
        }
        var devCount = 0
        if (ftD2xx != null) {
            // Get the connected USB FTDI devoces
            devCount = ftD2xx!!.createDeviceInfoList(this)
        } else {
            return
        }
        val deviceList = arrayOfNulls<FtDeviceInfoListNode>(devCount)
        ftD2xx!!.getDeviceInfoList(devCount, deviceList)
        // none connected
        if (devCount <= 0) {
            return
        }
        if (ftDev == null) {
//            openDevice();
            ftDev = ftD2xx!!.openByIndex(this, 0)
        } else {
            synchronized(ftDev!!) { ftDev = ftD2xx!!.openByIndex(this, 0) }
        }
        // run thread
        if (ftDev!!.isOpen) {
            SetConfig(9600, 8.toByte(), 2.toByte(), 0.toByte(), 0.toByte())
            ftDev!!.purge((D2xxManager.FT_PURGE_TX.toInt() or D2xxManager.FT_PURGE_RX.toInt()).toByte())
            ftDev!!.restartInTask()
        }
        if (ftDev != null) {
            deviceCom!!.setup(ftDev, 0x00, false, false, 0)
            deviceCom!!.start()
            deviceCom!!.SetEscrowMode(true)
        }
    }

    override fun OnDeviceEvent(deviceEvent: DeviceEvent) {
        runOnUiThread { DisplayEvents(deviceEvent) }
    }

    override fun OnFileUpdateStatus(sspUpdate: SSPUpdate) {
        UpdateFileDownload(sspUpdate)
        /*runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(cashCollectorActivity, " OnFileUpdateStatus ", Toast.LENGTH_SHORT).show();
            }
        });*/
    }

    override fun OnNewDeviceSetup(sspDevice: SSPDevice) {
        runOnUiThread { DisplaySetUp(sspDevice) }
    }

    override fun OnDeviceDisconnect(sspDevice: SSPDevice) {
        runOnUiThread { DeviceDisconnected(sspDevice) }
    }

    /*
     * Here we are getting time out from shared preference
     * And after that screen automatically off
     * */
    fun screenTimeOut() {
        preferencesManager = SharedPreferencesManager.getInstance(
            instance
        )
        Log.e("timeOut", preferencesManager?.get(Constants.ScreenTimeOutPref, "0").toString())
        val screenTimeOut =
            preferencesManager?.get(Constants.ScreenTimeOutPref, "0.0").toString().toLong()

        // Define the Runnable task
        runnable = Runnable {
            // Task to execute after delay
            goToHomeScreen() // Closes the current activity
        }

        // Post the Runnable with a 15-second delay
        handler!!.postDelayed(runnable!!, screenTimeOut * 1000)
    }

    /*
     * It will redirect to the home screen
     * */
    fun goToHomeScreen() {
        closeDevice()
        // Simulate finishing and sending data
        val resultIntent = Intent()
        resultIntent.putExtra("FromScreen", ScreenEnum.CASH_COLLECTOR.ordinal)
        setResult(RESULT_OK, resultIntent) // Set the result to be OK
        finish() // Finish the activity


//        Intent intent = new Intent(CashCollectorActivity.this, MainActivity.class);
//        // Clear all previous activities
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        startActivity(intent);
    }

    /// When process is completed. Data will be insert into database
    fun insertDataOnProcessDone(currency: Double, volumeOfMilk: Float, milkTemperature: Float) {
        Thread {
            try {
                val dateFormat = "yyyy-MM-dd"
                val timeFormat = "HH:mm:ss"
                val dateFormatter = SimpleDateFormat(dateFormat)
                val timeFormatter = SimpleDateFormat(timeFormat)
                val date = dateFormatter.format(System.currentTimeMillis())
                val time = timeFormatter.format(System.currentTimeMillis())
                // Print the combined date and time
                val transactionDao =
                    AppDatabase.getInstance(this@CashCollectorActivity).transactionDao()
                assert(date != null)

                /// Here I convert volume of milk into string and set 3 digits after dot(.)
                val truncatedValueOfMilkVolume = String.format("%.2f", volumeOfMilk).toFloat()

                /// Here I convert temperature of milk into string and set 3 digits after dot(.)
                val strMilkTemperature = String.format("%.3f", milkTemperature)
                val transactionId = Constants.insertTransaction(
                    this@CashCollectorActivity,
                    transactionDao,
                    "CASH",
                    "",
                    date,
                    time,
                    currency,
                    "SUCCESS",
                    "",
                    truncatedValueOfMilkVolume,
                    strMilkTemperature
                )
                Log.e(TAG, "onCreate: $transactionId")
                Log.e(TAG, "onCreate: " + Gson().toJson(transactionDao.allTransactions))


                /// Go to home page
                goToHomeScreen()

//                    Intent intent = new Intent(CashCollectorActivity.this, MainActivity.class);
//                    // Clear all previous activities
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    startActivity(intent);
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    /// If volume amount is more than 5 liters.
    // It will show error tha vending volume can not be more than 5 liters
    private fun showAlertExceedLimit() {
        // Create AlertDialog.Builder instance
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Exceed Limit")
        builder.setMessage("Vending volume can not be more than 5 liters.")

        // Positive button
        builder.setPositiveButton("OK") { dialog, which ->
            dialog.dismiss()
            goToHomeScreen()
        }

        // Show the dialog
        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.show()
    }

    companion object {
        //TODO:
        // 1) Read Continuous data from Serial
        // {
        //  "temperature": "3.04",  should not be more than set Temperature
        //  "compressor": true, green and red indicators
        //  "agitator": false, green and red indicators
        //  "lowlevel": true, if true close the system and show the dialog low on Milk.
        //}
        private const val MY_PERMISSIONS_REQUEST_READ_STORAGE = 0
        private const val ACTION_USB_PERMISSION = "com.imdc.milkdespencer.USB_PERMISSION"
        private val TAG = CashCollectorActivity::class.java.simpleName
        var fab: FloatingActionButton? = null
        var bvDisplay: LinearLayout? = null
        @JvmField
        var cashCollectorActivity: CashCollectorActivity? = null
        var listChannels: ListView? = null
        var listEvents: ListView? = null
        var bttnAccept: Button? = null
        var bttnReject: Button? = null
        var swEscrow: SwitchCompat? = null
        var txtFirmware: TextView? = null
        var txtDevice: TextView? = null
        var txtDataset: TextView? = null
        var txtSerial: TextView? = null
        var txtConnect: TextView? = null
        var prgConnect: ProgressBar? = null
        var progress: ProgressDialog? = null
        var channelValues: MutableList<String> = ArrayList()
        var eventValues: Array<String?>? = null
        var adapterChannels: ArrayAdapter<String>? = null
        var adapterEvents: ArrayAdapter<String?>? = null
        var selectedCurrency: String? = null
        var milkBasePrice = 0f
        @JvmField
        var milkSetTemperature = 0f
        var preferencesManager: SharedPreferencesManager? = null

        //    private static MenuItem downloadFileSelect = null;
        private var deviceCom: ITLDeviceCom? = null
        private var ftD2xx: D2xxManager? = null
        private var ftDev: FT_Device? = null
        private var sspDevice: SSPDevice? = null
        @JvmStatic
        var instance: CashCollectorActivity? = null
            private set

        /**********   USB functions    */ //    private static UsbSerialManager usbSerialManager;
        private var usbSerialCommunication: UsbSerialCommunication? = null
        fun DisplaySetUp(dev: SSPDevice) {
            sspDevice = dev
            fab!!.visibility = View.INVISIBLE
            fab!!.visibility = View.INVISIBLE
            prgConnect!!.visibility = View.INVISIBLE
            txtConnect!!.visibility = View.INVISIBLE
            bvDisplay!!.visibility = View.VISIBLE

            // check for type comapable
            if (dev.type != SSPDeviceType.BillValidator) {
                val builder = AlertDialog.Builder(
                    instance!!
                )
                // 2. Chain together various setter methods to set the dialog characteristics
                builder.setMessage("Connected device is not BNV (" + dev.type.toString() + ")")
                    .setTitle("BNV")
                builder.setPositiveButton("OK") { dialog, which -> instance!!.finish() }

                // 3. Get the AlertDialog from create()
                val dialog = builder.create()

                // 4. Show the dialog
                dialog.show() // show error
                return
            }

//        downloadFileSelect.setEnabled(true);

            /* device details  */txtFirmware!!.append(" " + dev.firmwareVersion)
            txtDevice!!.append(" " + dev.headerType.toString())
            txtSerial!!.append(" " + dev.serialNumber)
            txtDataset!!.append(dev.datasetVersion)

            /* display the channel info */channelValues!!.clear()
            for (itlCurrency in dev.currency) {
                val v = itlCurrency.country + " " + String.format("%.2f", itlCurrency.realvalue)
                channelValues!!.add(v)
            }
            adapterChannels!!.notifyDataSetChanged()


            // if device has barcode hardware
            if (dev.barCodeReader.hardWareConfig != SSPDevice.BarCodeStatus.None) {
                // send new configuration
                val cfg = BarCodeReader()
                cfg.barcodeReadEnabled = true
                cfg.billReadEnabled = true
                cfg.numberOfCharacters = 18
                cfg.format = SSPDevice.BarCodeFormat.Interleaved2of5
                cfg.enabledConfig = SSPDevice.BarCodeStatus.Both
                deviceCom!!.SetBarcocdeConfig(cfg)
            }
        }

        fun UpdateFileDownload(sspUpdate: SSPUpdate) {
            when (sspUpdate.UpdateStatus) {
                SSPDownloadStatus.dwnInitialise -> {
                    progress!!.setMessage("Downloading Ram")
                    progress!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                    progress!!.isIndeterminate = false
                    progress!!.progress = 0
                    progress!!.max = sspUpdate.numberOfRamBlocks
                    progress!!.setCanceledOnTouchOutside(false)
                    progress!!.show()
                }

                SSPDownloadStatus.dwnRamCode -> progress!!.progress = sspUpdate.blockIndex
                SSPDownloadStatus.dwnMainCode -> {
                    progress!!.setMessage("Downloading flash")
                    progress!!.max = sspUpdate.numberOfBlocks
                    progress!!.progress = sspUpdate.blockIndex
                }

                SSPDownloadStatus.dwnComplete -> progress!!.dismiss()
                SSPDownloadStatus.dwnError -> progress!!.dismiss()
                else -> {}
            }
        }

        /// If Cash machine device is open then it will be close
        private fun closeDevice() {
            if (ftDev != null) {
                deviceCom!!.Stop()
                ftDev!!.close()
            }
        }

        @JvmStatic
        fun SetConfig(baud: Int, dataBits: Byte, stopBits: Byte, parity: Byte, flowControl: Byte) {
            var dataBits = dataBits
            var stopBits = stopBits
            var parity = parity
            if (!ftDev!!.isOpen) {
                return
            }

            // configure our port
            // reset to UART mode for 232 devices
            ftDev!!.setBitMode(0.toByte(), D2xxManager.FT_BITMODE_RESET)
            ftDev!!.setBaudRate(baud)
            dataBits = when (dataBits) {
                7.toByte() -> D2xxManager.FT_DATA_BITS_7
                8.toByte() -> D2xxManager.FT_DATA_BITS_8
                else -> D2xxManager.FT_DATA_BITS_8
            }
            stopBits = when (stopBits) {
                1.toByte() -> D2xxManager.FT_STOP_BITS_1
                2.toByte() -> D2xxManager.FT_STOP_BITS_2
                else -> D2xxManager.FT_STOP_BITS_1
            }
            parity = when (parity) {
                0.toByte() -> D2xxManager.FT_PARITY_NONE
                1.toByte() -> D2xxManager.FT_PARITY_ODD
                2.toByte() -> D2xxManager.FT_PARITY_EVEN
                3.toByte() -> D2xxManager.FT_PARITY_MARK
                4.toByte() -> D2xxManager.FT_PARITY_SPACE
                else -> D2xxManager.FT_PARITY_NONE
            }
            ftDev!!.setDataCharacteristics(dataBits, stopBits, parity)
            val flowCtrlSetting: Short
            flowCtrlSetting = when (flowControl) {
                0.toByte() -> D2xxManager.FT_FLOW_NONE
                1.toByte() -> D2xxManager.FT_FLOW_RTS_CTS
                2.toByte() -> D2xxManager.FT_FLOW_DTR_DSR
                3.toByte() -> D2xxManager.FT_FLOW_XON_XOFF
                else -> D2xxManager.FT_FLOW_NONE
            }
            ftDev!!.setFlowControl(flowCtrlSetting, 0x0b.toByte(), 0x0d.toByte())
        }
    }
}