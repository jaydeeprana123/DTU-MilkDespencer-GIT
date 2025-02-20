package com.imdc.milkdespencer

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.os.AsyncTask
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView.OnItemClickListener
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.imdc.milkdespencer.CashCollectorActivityJava.getInstance
import com.imdc.milkdespencer.Workers.PaymentStatusService
import com.imdc.milkdespencer.adapter.SpnCurrencyAdapter
import com.imdc.milkdespencer.adapter.SpnLitersAdapter
import com.imdc.milkdespencer.common.Constants
import com.imdc.milkdespencer.common.LottieDialog
import com.imdc.milkdespencer.common.SharedPreferencesManager
import com.imdc.milkdespencer.common.UsbSerialCommunication
import com.imdc.milkdespencer.enums.ScreenEnum
import com.imdc.milkdespencer.models.ResponseMilkDispense
import com.imdc.milkdespencer.models.ResponseTempStatus
import com.imdc.milkdespencer.models.SendToDevice
import com.imdc.milkdespencer.network.RetrofitClient
import com.imdc.milkdespencer.roomdb.AppDatabase
import com.imdc.milkdespencer.roomdb.interfaces.TransactionDao
import com.razorpay.Payment
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.razorpay.QrCode
import com.razorpay.RazorpayClient
import com.razorpay.RazorpayException
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.concurrent.atomic.AtomicReference
import java.util.regex.Pattern

class PayWithQrActivity : AppCompatActivity(), PaymentResultWithDataListener {
    private val TAG = PayWithQrActivity::class.java.simpleName
    private val previousSelectionAMT = 0
    private val previousSelectionLites = 0
    var isCharging = false
    private var handler: Handler? = Handler() // Create a Handler instance
    private var runnable: Runnable? = null // Declare the Runnable
    private var timeoutHandler: Handler? = null
    private var timeoutRunnable: Runnable? = null
    private val batteryReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Get the current battery status
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

            // Check if the device is charging
            isCharging =
                status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            if (isCharging) {
            } else {
                Constants.saveLogs(this@PayWithQrActivity, "Lost Electricity")
                val payment = Gson().fromJson(
                    preferencesManager!![Constants.PaymentReceived, ""].toString(),
                    Payment::class.java
                )
                if (payment != null && payment.get<Any?>("amount") != null) {
                    val amount = payment.get<Any>("amount").toString().toFloat()
                    val amt = amount / 100

                    /// Here payment is done. And Suddenly electricity lost
                    showElectricityLostAndFailedProcessDialog(amt, payment, 0f)
                }
            }
        }
    }
    var paymentObject = JSONObject()
    var isCommandSent = false
    var dialog = AtomicReference<Dialog?>()
    private val paymentStatusReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "payment_status_action") {
                val paymentStatusJson = intent.getStringExtra("payment_status")
                Log.e("TAG", "onReceive: $paymentStatusJson")
                if (paymentStatusJson != null && !paymentStatusJson.isEmpty()) {
                    val payment = Gson().fromJson(paymentStatusJson, Payment::class.java)
                    Log.e("TAG", "onReceive:payment $paymentStatusJson")
                    if (payment != null) {
                        if (dialog.get() != null) {
                            Log.e("TAG", "onReceive:DIa " + dialog.get()!!.isShowing)
                            dialog.get()!!.dismiss()
                        }
                        if (payment.has("amount")) {
                            val amount = payment.get<Any>("amount").toString().toFloat()
                            val amt = (amount / 100).toDouble()
                            preferencesManager!!.save(
                                Constants.PaymentReceived,
                                Gson().toJson(payment)
                            )
                            preferencesManager!!.save(Constants.PaidAmt, amt)
                            sendForMilkVending(amt, payment)
                        }
                        val serviceIntent =
                            Intent(this@PayWithQrActivity, PaymentStatusService::class.java)
                        stopService(serviceIntent)
                    }
                }
                // Process the payment status JSON received from the service
            }
        }
    }
    var retrofitClient: RetrofitClient? = null
    var razorpay: RazorpayClient? = null
    var lottieDialog: LottieDialog? = null
    var gv_CurrencyLiters: GridView? = null
    var tabLayout: TabLayout? = null
    private var btnGenerateQr: MaterialButton? = null
    private var btnBackToHome: MaterialButton? = null
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_pay_with_qr)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        val battertyFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, battertyFilter)
        screenTimeOut()
        usbSerialCommunication = UsbSerialCommunication(
            applicationContext
        )
        if (!usbSerialCommunication!!.connected) {
            usbSerialCommunication!!.connect()
            usbSerialCommunication!!.setBaudRate(115200)
        }
        btnGenerateQr = findViewById(R.id.btnGenerateQr)
        btnBackToHome = findViewById(R.id.btnBackToHome)
        tabLayout = findViewById(R.id.tabLayout)
        preferencesManager =
            SharedPreferencesManager.getInstance(getInstance())
        /// When user comes first delete the previously saved payment data in shared preference
        preferencesManager?.delete(Constants.PaymentReceived)
        preferencesManager?.delete(Constants.PaidAmt)
        gv_CurrencyLiters = findViewById(R.id.gridViewCurrencyLiters)
        val currencyAdapter = SpnCurrencyAdapter(this)
        val litersAdapter = SpnLitersAdapter(this)
        val filter = IntentFilter("payment_status_action")
        registerReceiver(paymentStatusReceiver, filter)
        if (tabLayout?.getSelectedTabPosition() == 0) {
            gv_CurrencyLiters?.setAdapter(litersAdapter)
            gv_CurrencyLiters?.setNumColumns(4)
        } else {
            gv_CurrencyLiters?.setAdapter(currencyAdapter)
            gv_CurrencyLiters?.setNumColumns(3)
        }
        tabLayout?.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        // Milk in liters selected
                        gv_CurrencyLiters?.setAdapter(litersAdapter)
                        gv_CurrencyLiters?.setNumColumns(4)
                    }

                    1 -> {
                        // Milk in price selected
                        gv_CurrencyLiters?.setAdapter(currencyAdapter)
                        gv_CurrencyLiters?.setNumColumns(3)
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        gv_CurrencyLiters?.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            //   If User clicks on the grid item. Runnable should be close
            // Cancel the delayed task
            if (handler != null && runnable != null) {
                handler!!.removeCallbacks(runnable!!)
            }
            val customerId = preferencesManager?.get(Constants.RazorPayCustomerID, "").toString()
            val machineId = preferencesManager?.get(Constants.MachineId, "").toString()
            if (customerId.isEmpty()) {
                Constants.showAlertDialog(
                    this@PayWithQrActivity,
                    "Error",
                    "Customer Id cannot be empty"
                )
                return@OnItemClickListener
            }
            if (gv_CurrencyLiters?.getAdapter() is SpnLitersAdapter) {
                Log.e(TAG, "onItemSelected: " + gv_CurrencyLiters?.adapter?.getItem(position))
                val numericValueFromString = extractNumericValueFromString(
                    gv_CurrencyLiters?.adapter?.getItem(position).toString()
                )

                // Double numericValueFromString = extractNumericValueFromString(LitersSpinnerData.volumeValuesInLtr[0].toString());
                Log.e("numericValueFromString", numericValueFromString.toString())
                val inputVal = numericValueFromString?.toString() ?: "0.0"
                val ltrs = inputVal.toDouble()
                var amt = Constants.calculateMilkPrice(ltrs, this@PayWithQrActivity)
                amt = String.format("%.2f", amt).toDouble()
                try {
                    paymentObject.put("name", "Milk Vending Machine")
                    paymentObject.put("description", "Payment For Milk")
                    paymentObject.put("currency", "INR")
                    paymentObject.put(
                        "amount",
                        amt * 100
                    ) // Amount in paise (e.g., 10000 paise = INR 100)
                    //                        paymentObject.put("amount", 100); // Amount in paise (e.g., 10000 paise = INR 100)
                    Constants.showAcceptDialog(
                        this@PayWithQrActivity,
                        "Please Confirm",
                        "You need to pay the ₹" + amt + " for " + gv_CurrencyLiters?.adapter?.getItem(position).toString(),
                        DialogInterface.OnClickListener { dialog, which ->
                            //                                Toast.makeText(PayWithQrActivity.this, "YES ", Toast.LENGTH_SHORT).show();
                            dialog.dismiss()
                            val responseTempStatus = Gson().fromJson(
                                preferencesManager?.get(
                                    Constants.ResponseTempStatus,
                                    ""
                                ).toString(), ResponseTempStatus::class.java
                            )

                            /// Here it will check that door is open or close
                            // If door is close then allow to start milking
                            if (!responseTempStatus.connectivity) {
                                executeGenerateQRCodeTask(paymentObject, customerId, machineId)
                            } else {
                                // If door is open then close the cash machine and send to the home page
                                goToHomeScreen()
                            }
                        },
                        object : DialogInterface.OnClickListener {
                            override fun onClick(dialog: DialogInterface, which: Int) {
    //                                Toast.makeText(PayWithQrActivity.this, "NO ", Toast.LENGTH_SHORT).show();
                                dialog.dismiss()
                            }
                        })
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (gv_CurrencyLiters?.adapter is SpnCurrencyAdapter) {
                var cost =
                    gv_CurrencyLiters?.adapter?.getItem(position).toString().replace("₹", "")
                        .toDouble()
                cost = String.format("%.2f", cost).toFloat().toDouble()

                //   Toast.makeText(PayWithQrActivity.this, "COST " + cost, Toast.LENGTH_SHORT).show();
                val weight = Constants.calculateMilkAmount(cost, this@PayWithQrActivity)
                val weightStr = if (weight > 0 && weight < 1) "$weight (Ml)." else "$weight(Ltr)."
                val milkSellingPrice =
                    preferencesManager?.get(Constants.MilkBasePrice, "0.0").toString().toFloat()
                var volumeToDisplay = (cost / milkSellingPrice).toString().toFloat()
                volumeToDisplay = String.format("%.2f", volumeToDisplay).toFloat()
                try {
                    paymentObject.put("name", "Milk Vending Machine")
                    paymentObject.put("description", "Payment For Milk")
                    paymentObject.put("currency", "INR")
                    paymentObject.put(
                        "amount",
                        cost * 100
                    ) // Amount in paise (e.g., 10000 paise = INR 100)
                    //                        paymentObject.put("amount", 100); // Amount in paise (e.g., 10000 paise = INR 100)
                    val finalVolumeToDisplay = volumeToDisplay
                    Constants.showAcceptDialog(
                        this@PayWithQrActivity,
                        "Please Confirm",
                        "You need to pay the ₹" + cost + " for " + volumeToDisplay + "Ltr",
                        { dialog: DialogInterface, which: Int ->
                            dialog.dismiss()

                            /// Check that volume amount is more than 5 lites
                            if (finalVolumeToDisplay > 5) {
                                showAlertExceedLimit()
                            } else {
                                val responseTempStatus = Gson().fromJson(
                                    preferencesManager?.get(
                                        Constants.ResponseTempStatus, ""
                                    ).toString(), ResponseTempStatus::class.java
                                )

                                /// Here it will check that door is open or close
                                // If door is close then allow to start milking
                                if (!responseTempStatus.connectivity) {
                                    executeGenerateQRCodeTask(paymentObject, customerId, machineId)
                                } else {
                                    // If door is open then close the cash machine and send to the home page
                                    goToHomeScreen()
                                }
                            }
                        }) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Log.e(TAG, "onItemSelected: " + gv_CurrencyLiters?.adapter?.getItem(position))
            }
        }
        btnGenerateQr?.setVisibility(View.GONE)
        btnGenerateQr?.setOnClickListener(View.OnClickListener {
            val selectedId = tabLayout?.selectedTabPosition
            //                String inputVal = tieInputVal.getText().toString();
            val numericValueFromString =
                extractNumericValueFromString(gv_CurrencyLiters?.selectedItem.toString())
            val inputVal = numericValueFromString?.toString() ?: "0.0"
            Log.e(TAG, "onClick: $inputVal")
            var weightInLiter = ""
            if (selectedId == 0) {
                val ltrs = inputVal.toDouble()
                val amt = Constants.calculateMilkPrice(ltrs, this@PayWithQrActivity)
                weightInLiter = ltrs.toString()
                try {
                    paymentObject.put("name", "Milk Vending Machine")
                    paymentObject.put("description", "Payment For Milk")
                    paymentObject.put("currency", "INR")
                    paymentObject.put(
                        "amount",
                        amt * 100
                    ) // Amount in paise (e.g., 10000 paise = INR 100)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (selectedId == 1) {
                val cost =
                    gv_CurrencyLiters?.selectedItem.toString().replace("₹", "").toDouble()
                //   Toast.makeText(PayWithQrActivity.this, "COST " + cost, Toast.LENGTH_SHORT).show();
                val weight = Constants.calculateMilkAmount(cost, this@PayWithQrActivity)
                weightInLiter = weight.toString()
                try {
                    paymentObject.put("name", "Milk Vending Machine")
                    paymentObject.put("description", "Payment For Milk")
                    paymentObject.put("currency", "INR")
                    paymentObject.put(
                        "amount",
                        cost * 100
                    ) // Amount in paise (e.g., 10000 paise = INR 100)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val customerId = preferencesManager?.get(Constants.RazorPayCustomerID, "").toString()
            val machineId = preferencesManager?.get(Constants.MachineId, "").toString()
            if (customerId.isEmpty()) {
                Constants.showAlertDialog(
                    this@PayWithQrActivity,
                    "Error",
                    "Customer Id cannot be empty"
                )
                return@OnClickListener
            }
            Log.e("TAG", "onClick: " + Gson().toJson(paymentObject))
            val finalWeightInLiter = weightInLiter
            object : AsyncTask<Void?, Void?, Void?>() {
                override fun doInBackground(vararg p0: Void?): Void? {
                    try {
                        razorpay = RazorpayClient(
                            "rzp_test_bfiWftOYB0MCR7",
                            "VuX6RLVKtB6MBILQKRzcMeZy"
                        ) //TEST
                        //                            razorpay = new RazorpayClient("rzp_live_oTrQqk0HauuUWZ", "7lBcCfNsgl7wKtshFz7QCm8F");//LIVE
                        val qrRequest = JSONObject()
                        qrRequest.put("type", "upi_qr")
                        qrRequest.put("name", "Milk Vending booth")
                        qrRequest.put("usage", "single_use")
                        qrRequest.put("fixed_amount", true)
                        qrRequest.put("payment_amount", paymentObject["amount"])
                        qrRequest.put("description", machineId)
                        //                            qrRequest.put("customer_id", "cust_NQXXhGiitVX9xe"); //Test
//                            qrRequest.put("customer_id", "cust_NWIoi0QrjXC2ez");//LIVE
                        qrRequest.put("customer_id", customerId) //LIVE
                        val currentTime = System.currentTimeMillis()
                        val closeByTime = currentTime + (5 * 60 * 1000)

                        // Check if close_by is within the acceptable range
                        if (closeByTime < 946684800L * 1000 || closeByTime > 4765046400L * 1000) {
                            // Handle the case where close_by is out of range
                            throw IllegalArgumentException("close_by out of acceptable range")
                        }
                        qrRequest.put("close_by", closeByTime / 1000)
                        val notes = JSONObject()
                        notes.put("notes_key_1", "Milk Vending")
                        notes.put("notes_key_2", paymentObject.toString())
                        qrRequest.put("notes", notes)
                        Log.e("TAG", "doInBackground: " + Gson().toJson(qrRequest))
                        val qrcode = razorpay!!.qrCode.create(qrRequest)
                        Log.e("TAG", "doInBackground: " + Gson().toJson(qrcode))
                        if (qrcode != null) {
                            val imageUrl = qrcode.get<Any>("image_url").toString()
                            val qrCodeId = qrcode.get<Any>("id").toString()
                            runOnUiThread {
                                dialog.set(showQRCodeDialog(imageUrl))
                                dialog.get()!!.show()
                                val serviceIntent: Intent =
                                    Intent(this@PayWithQrActivity, PaymentStatusService::class.java)
                                serviceIntent.putExtra("qr_code_id", qrCodeId)
                                startService(serviceIntent)

                                /* Dialog close after 6 minutes*/
                                // Schedule dialog dismissal after 6 minutes (360,000 milliseconds)
                                Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
                                    override fun run() {
                                        val currentDialog: Dialog? = dialog.get()
                                        if (currentDialog != null && currentDialog.isShowing()) {
                                            Log.e("btnGenerateQr", "button CLick")
                                            currentDialog.dismiss()
                                            /// Insert data into database
                                            Thread(object : Runnable {
                                                override fun run() {
                                                    try {
                                                        val dateFormat: String = "yyyy-MM-dd"
                                                        val timeFormat: String = "HH:mm:ss"
                                                        val dateFormatter: SimpleDateFormat =
                                                            SimpleDateFormat(dateFormat)
                                                        val timeFormatter: SimpleDateFormat =
                                                            SimpleDateFormat(timeFormat)
                                                        val date: String? = dateFormatter.format(
                                                            System.currentTimeMillis()
                                                        )
                                                        val time: String = timeFormatter.format(
                                                            System.currentTimeMillis()
                                                        )
                                                        // Print the combined date and time
                                                        if (paymentObject.has("amount")) {
                                                            val amount: Float =
                                                                paymentObject.get("amount")
                                                                    .toString().toFloat()
                                                            val amt: Double =
                                                                (amount / 100).toDouble()
                                                            Log.e(
                                                                "amountttt in string",
                                                                amt.toString()
                                                            )
                                                            val transactionDao: TransactionDao =
                                                                AppDatabase.getInstance(this@PayWithQrActivity)
                                                                    .transactionDao()
                                                            assert(date != null)
                                                            val transactionId: Long =
                                                                Constants.insertTransaction(
                                                                    this@PayWithQrActivity,
                                                                    transactionDao,
                                                                    "ONLINE",
                                                                    "",
                                                                    date,
                                                                    time,
                                                                    amt,
                                                                    "TIME OUT",
                                                                    "",
                                                                    finalWeightInLiter.toFloat(),
                                                                    ""
                                                                )
                                                            Log.e(TAG, "onCreate: " + transactionId)
                                                            Log.e(
                                                                TAG,
                                                                "onCreate: " + Gson().toJson(
                                                                    transactionDao.getAllTransactions()
                                                                )
                                                            )
                                                        }
                                                        goToHomeScreen()
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                            }).start()
                                        }
                                    }
                                }, 360000)
                            }
                        }
                    } catch (e: RazorpayException) {
                        runOnUiThread {
                            Constants.showAlertDialog(
                                this@PayWithQrActivity,
                                "Error",
                                e.message
                            )
                        }
                    } catch (e: JSONException) {
                        runOnUiThread {
                            Constants.showAlertDialog(
                                this@PayWithQrActivity,
                                "Error",
                                e.message
                            )
                        }
                    }
                    return null
                }
            }.execute()
        })
        btnBackToHome?.setOnClickListener(View.OnClickListener { // Cancel the delayed task
            if (handler != null && runnable != null) {
                handler!!.removeCallbacks(runnable!!)
            }

            // Remove the Runnable from the Handler to avoid memory leaks
            if (timeoutHandler != null && timeoutRunnable != null) {
                timeoutHandler!!.removeCallbacks(timeoutRunnable!!)
            }
            goToHomeScreen()
        })

//        checkout.open(this, paymentObject);
    }

    @SuppressLint("StaticFieldLeak")
    fun executeGenerateQRCodeTask(
        paymentObject: JSONObject,
        customerId: String,
        machineId: String
    ) {
        object : AsyncTask<Void?, Void?, Void?>() {
            protected override fun doInBackground(vararg p0: Void?): Void? {
                generateQRCode(paymentObject, customerId, machineId)
                return null
            }
        }.execute()
    }

    /*
     * Generate QR Code*/
    private fun generateQRCode(paymentObject: JSONObject, customerId: String, machineId: String) {
        try {
            val razorpay = RazorpayClient(
                preferencesManager!![Constants.RazorPayKey, "rzp_live_oTrQqk0HauuUWZ"].toString(),
                preferencesManager!![Constants.RazorPaySecretKey, "7lBcCfNsgl7wKtshFz7QCm8F"].toString()
            )

            //  RazorpayClient razorpay = new RazorpayClient("rzp_live_oTrQqk0HauuUWZ", "7lBcCfNsgl7wKtshFz7QCm8F");
            Log.e(
                "api key",
                preferencesManager!![Constants.RazorPayKey, "rzp_live_oTrQqk0HauuUWZ"].toString()
            )
            Log.e(
                "secret key",
                preferencesManager!![Constants.RazorPaySecretKey, "7lBcCfNsgl7wKtshFz7QCm8F"].toString()
            )
            val qrRequest = createQrRequest(paymentObject, customerId, machineId)
            Log.e("TAG", "QR Request: " + Gson().toJson(qrRequest))

            /// Generated QR Code
            val qrcode = razorpay.qrCode.create(qrRequest)
            Log.e("TAG", "QR Code Response: " + Gson().toJson(qrcode))

            /// If QR Code is not null then show QR code in an dialog
            qrcode?.let { handleQrCodeResponse(it, paymentObject) }
        } catch (e: RazorpayException) {
            runOnUiThread { Constants.showAlertDialog(this@PayWithQrActivity, "Error", e.message) }
        } catch (e: JSONException) {
            runOnUiThread { Constants.showAlertDialog(this@PayWithQrActivity, "Error", e.message) }
        }
    }

    /*Create Json Object for request*/
    @Throws(JSONException::class)
    private fun createQrRequest(
        paymentObject: JSONObject,
        customerId: String,
        machineId: String
    ): JSONObject {
        val qrRequest = JSONObject()
        qrRequest.put("type", "upi_qr")
        qrRequest.put("name", "Milk Vending booth")
        qrRequest.put("usage", "single_use")
        qrRequest.put("fixed_amount", true)
        qrRequest.put("payment_amount", paymentObject["amount"])
        qrRequest.put("description", machineId)
        qrRequest.put("customer_id", customerId)
        val closeByTime = System.currentTimeMillis() + 5 * 60 * 1000
        validateCloseByTime(closeByTime)
        qrRequest.put("close_by", closeByTime / 1000)
        val notes = JSONObject()
        notes.put("notes_key_1", "Milk Vending")
        notes.put("notes_key_2", paymentObject.toString())
        qrRequest.put("notes", notes)
        return qrRequest
    }

    private fun validateCloseByTime(closeByTime: Long) {
        val minTime = 946684800L * 1000
        val maxTime = 4765046400L * 1000
        require(!(closeByTime < minTime || closeByTime > maxTime)) { "close_by out of acceptable range" }
    }

    /* Once QR code is generate from Razor pay*/
    private fun handleQrCodeResponse(qrcode: QrCode, paymentObject: JSONObject) {

        /// Image of QR code
        val imageUrl = qrcode.get<Any>("image_url").toString()
        val qrCodeId = qrcode.get<Any>("id").toString()
        runOnUiThread {
            dialog.set(showQRCodeDialog(imageUrl))
            dialog.get()!!.show()
            val serviceIntent: Intent =
                Intent(this@PayWithQrActivity, PaymentStatusService::class.java)
            serviceIntent.putExtra("qr_code_id", qrCodeId)
            startService(serviceIntent)
            scheduleDialogDismissal(paymentObject)
        }
    }

    /*Here if QR code is generate and payment status is not get.
    Then transaction will be added as a TIME OUT and go to the home screen*/
    private fun scheduleDialogDismissal(paymentObject: JSONObject) {
        Handler(Looper.getMainLooper()).postDelayed({
            val currentDialog = dialog.get()
            if (currentDialog != null && currentDialog.isShowing) {
                Log.e("generateQRCode", "Dialog dismissed due to timeout")
                currentDialog.dismiss()
                saveTransactionAsATimeOUt(paymentObject)
                goToHomeScreen()
            }
        }, (6 * 60 * 1000).toLong())
    }

    /*Save transaction if time is out*/
    private fun saveTransactionAsATimeOUt(paymentObject: JSONObject) {
        Thread {
            try {
                val date: String = SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis())
                val time: String = SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())
                if (paymentObject.has("amount")) {
                    val amount: Double = paymentObject.get("amount").toString().toDouble() / 100
                    val transactionDao: TransactionDao =
                        AppDatabase.getInstance(this@PayWithQrActivity).transactionDao()
                    val transactionId: Long = Constants.insertTransaction(
                        this@PayWithQrActivity, transactionDao, "ONLINE", "", date, time,
                        amount, "TIME OUT", "", 0f, ""
                    )
                    Log.e("Transaction", "ID: " + transactionId)
                    Log.e(
                        "Transaction",
                        "All: " + Gson().toJson(transactionDao.getAllTransactions())
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun extractNumericValueFromString(input: String): Double? {
        val pattern = Pattern.compile("(\\d+(\\.\\d+)?)\\s*(\\(.*\\))?")
        val matcher = pattern.matcher(input)
        if (matcher.find()) {
            val numericValueString = matcher.group(1)
            return numericValueString.toDouble()
        } else {
            Constants.showAlertDialog(
                this@PayWithQrActivity,
                "Error",
                "No Valid Selection : $input"
            )
            /*                new IllegalArgumentException("No numeric value found in volume string: " + input);
             new IllegalArgumentException("No numeric value found in volume string: " + input);*/
        }
        /*Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return Double.parseDouble(matcher.group());
        } else {
            throw new IllegalArgumentException("No numeric value found in input string: " + input);
        }*/return null
    }

    /*Show Qr code image in dialog*/
    private fun showQRCodeDialog(imageUrl: String): Dialog {
        // Inflate the dialog layout
        val qrCodeDialog = Dialog(this, android.R.style.Theme_Light_NoTitleBar)
        qrCodeDialog.setContentView(R.layout.dialog_qr_code)

        // Find the ImageView in the layout
        val imageViewQRCode = qrCodeDialog.findViewById<ImageView>(R.id.ivQRCode)

        // Set the QR code bitmap to the ImageView
        Glide.with(this).load(imageUrl).timeout(10000).into(imageViewQRCode)
        qrCodeDialog.setCancelable(true)

        // Show the dialog
        return qrCodeDialog
    }

    //    @Override
    //    public boolean onCreateOptionsMenu(Menu menu) {
    //        getMenuInflater().inflate(R.menu.menu_main, menu);
    //
    //        return true;
    //    }
    //
    //    @Override
    //    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    //        if (item.getItemId() == R.id.action_home) {
    //            Intent intent = new Intent(PayWithQrActivity.this, MainActivity.class);
    //            startActivity(intent);
    //            finish();
    //        }
    //        return super.onOptionsItemSelected(item);
    //    }
    override fun onStart() {
        super.onStart()
        /*Intent serviceIntent = new Intent(this, PaymentStatusService.class);
        startService(serviceIntent);*/
    }

    override fun onStop() {
        super.onStop()
        val serviceIntent = Intent(this, PaymentStatusService::class.java)
        stopService(serviceIntent)
        unregisterReceiver(paymentStatusReceiver)
        unregisterReceiver(batteryReceiver)
    }

    override fun onPaymentSuccess(s: String, paymentData: PaymentData) {
        Log.e("TAG", "onPaymentSuccess: ")
    }

    override fun onPaymentError(i: Int, s: String, paymentData: PaymentData) {
        Log.e("TAG", "onPaymentError: " + paymentData.paymentId)
    }

    /*
     * When payment is done. Send for Vending the milk*/
    fun sendForMilkVending(amt: Double, payment: Payment) {
        val lottieDialog = LottieDialog(this@PayWithQrActivity)
        try {
            val responseTempStatus = Gson().fromJson(
                preferencesManager!![Constants.ResponseTempStatus, ""].toString(),
                ResponseTempStatus::class.java
            )
            val milkSellingPrice =
                preferencesManager!![Constants.MilkBasePrice, "0.0"].toString().toFloat()
            val offSet =
                preferencesManager!![Constants.TemperatureOffSet, "0.0"].toString().toFloat()
            val milkDensity =
                preferencesManager!![Constants.MilkDensityPref, "0.0"].toString().toFloat()
            val weight = (amt / milkSellingPrice * milkDensity).toFloat()
            val milkSetTemperature =
                preferencesManager!![Constants.TemperatureSet, "0.0"].toString().toFloat()
            val currentSavedTemp = responseTempStatus.temperature / 10.0
            val currentTemperature = (currentSavedTemp + offSet).toFloat()
            val sendToDevice = SendToDevice()
            sendToDevice.weight = weight
            sendToDevice.isStatus = true
            sendToDevice.curtemperature = currentTemperature
            Log.e("milkSetTemperature sendForMilkVending", milkSetTemperature.toString())
            sendToDevice.settemperature = milkSetTemperature
            val gson = GsonBuilder().serializeSpecialFloatingPointValues().create()
            Log.e("TAG", "QR_PAYMENT: SEND COMMAND " + gson.toJson(sendToDevice))
            lottieDialog.show()

            /// Here after 15 minute if status is not getting as a true.
            // Dialog will be close and transaction will be add in the database as a TIME OUT
            timeoutHandler = Handler(Looper.getMainLooper())
            timeoutRunnable = Runnable { handleMilkSendingTimeout(lottieDialog, amt, 0f) }

            // Post the Runnable with a delay
            timeoutHandler!!.postDelayed(timeoutRunnable!!, (15 * 60 * 1000).toLong()) // 15 minutes


            /// Send Data to the usb Serial Communication
            usbSerialCommunication!!.sendData(gson.toJson(sendToDevice))
            isCommandSent = true

            /// Read Data of the usb Serial Communication
            usbSerialCommunication!!.setReadDataListener { data: String ->
                handleSerialReadingResponse(
                    data,
                    lottieDialog,
                    amt,
                    payment,
                    timeoutHandler!!,
                    timeoutRunnable!!,
                    milkDensity.toDouble(),
                    currentTemperature
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /*If 15 minutes done and status is not getting as a true.
    Transaction will be added as a FAILED*/
    private fun handleMilkSendingTimeout(lottieDialog: LottieDialog?, amt: Double, volume: Float) {
        if (lottieDialog != null && lottieDialog.isShowing) {
            lottieDialog.dismiss()
            Thread {
                try {
                    val date: String =
                        SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis())
                    val time: String =
                        SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())
                    val transactionDao: TransactionDao =
                        AppDatabase.getInstance(this@PayWithQrActivity).transactionDao()
                    Constants.insertTransaction(
                        this@PayWithQrActivity,
                        transactionDao,
                        "ONLINE",
                        "",
                        date,
                        time,
                        amt,
                        "FAILED",
                        "",
                        volume,
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

    /*Read Listener Response*/
    private fun handleSerialReadingResponse(
        data: String,
        lottieDialog: LottieDialog,
        amt: Double,
        payment: Payment,
        timeoutHandler: Handler,
        timeoutRunnable: Runnable,
        milkDensity: Double,
        milkTemperature: Float
    ) {
        Log.d("TAG", "onReadData: $data")

        /// If it contains status key
        //
        if (data.contains("status")) {
            val milkDispense = Gson().fromJson(data, ResponseMilkDispense::class.java)


            /// If status is true then show success dialog
            // Here if milkDispense.getStatus == true. timeOutHandler will be stop
            /// Here true status getting two times.
            // So put condition that if lottieDialog is showing that time only goes to this condition
            if (milkDispense != null && milkDispense.status && lottieDialog.isShowing) {

                /// Here we calculate volume of milk
                val volumeOfMilk = (milkDispense.currentWeight / milkDensity).toFloat()
                Log.e("volumeOfMilk", volumeOfMilk.toString())


                /// when status get as a true, timeOutHandler removed here
                timeoutHandler.removeCallbacks(timeoutRunnable)
                if (lottieDialog.isShowing) {
                    lottieDialog.dismiss()
                }
                isCommandSent = false
                preferencesManager!!.save(Constants.CurrentTemperature, milkDispense.currentWeight)


                /// Show process done dialog
                showAndProcessDoneDialog(amt, payment, volumeOfMilk, milkTemperature)
            }
        }
    }

    /*
     * If payment is done and electricity is lost. Then Show Fail Dialog*/
    private fun showElectricityLostAndFailedProcessDialog(
        amt: Float,
        payment: Payment,
        volumeOfMilk: Float
    ) {
        runOnUiThread {
            Constants.showAcceptDialog(
                this@PayWithQrActivity,
                "Error",
                "Lost Electricity Connection!! Please Try after sometime.",
                { dialog1: DialogInterface, which: Int ->
                    Thread(object : Runnable {
                        override fun run() {
                            try {
                                val dateFormat = "yyyy-MM-dd"
                                val timeFormat = "HH:mm:ss"
                                val dateFormatter = SimpleDateFormat(dateFormat)
                                val timeFormatter = SimpleDateFormat(timeFormat)
                                val date = dateFormatter.format(System.currentTimeMillis())
                                val time = timeFormatter.format(System.currentTimeMillis())
                                // Print the combined date and time
                                val transactionDao =
                                    AppDatabase.getInstance(this@PayWithQrActivity).transactionDao()
                                assert(date != null)
                                val transactionId = Constants.insertTransaction(
                                    this@PayWithQrActivity,
                                    transactionDao,
                                    "ONLINE",
                                    "",
                                    date,
                                    time,
                                    amt.toDouble(),
                                    "FAILED",
                                    payment.get("vpa"),
                                    volumeOfMilk,
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


//                                Intent intent = new Intent(PayWithQrActivity.this, MainActivity.class);
//                                startActivity(intent);
//                                finish();
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }).start()
                }) { dialog1: DialogInterface, which: Int ->
                Thread(object : Runnable {
                    override fun run() {
                        try {
                            val dateFormat: String = "yyyy-MM-dd"
                            val timeFormat: String = "HH:mm:ss"
                            val dateFormatter: SimpleDateFormat = SimpleDateFormat(dateFormat)
                            val timeFormatter: SimpleDateFormat = SimpleDateFormat(timeFormat)
                            val date: String? = dateFormatter.format(System.currentTimeMillis())
                            val time: String = timeFormatter.format(System.currentTimeMillis())
                            // Print the combined date and time
                            val transactionDao: TransactionDao =
                                AppDatabase.getInstance(this@PayWithQrActivity).transactionDao()
                            assert(date != null)
                            val transactionId: Long = Constants.insertTransaction(
                                this@PayWithQrActivity,
                                transactionDao,
                                "ONLINE",
                                "",
                                date,
                                time,
                                amt.toDouble(),
                                "FAILED",
                                payment.get("vpa"),
                                volumeOfMilk,
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

//                                Intent intent = new Intent(PayWithQrActivity.this, MainActivity.class);
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

    /// If milk is send to the customer. Show process done dialog
    fun showAndProcessDoneDialog(
        amt: Double,
        payment: Payment,
        volumeOfMilk: Float,
        milkTemperature: Float
    ) {
        runOnUiThread {
            val builder = AlertDialog.Builder(this@PayWithQrActivity)
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
                //                        onDestroy();
                insertDataOnProcessDone(amt, payment, volumeOfMilk, milkTemperature)
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
                //                        onDestroy();
                Log.e(TAG, "onClick:Payment " + Gson().toJson(payment))
                insertDataOnProcessDone(amt, payment, volumeOfMilk, milkTemperature)
                /*Intent intent = new Intent(PayWithQrActivity.this, MainActivity.class);
                                        startActivity(intent);
                                        finish();*/


//                doPostTransaction(Constants.PostTransactionURL);
            }
        }
    }

    /*
     * Here we are getting time out from shared preference
     * And after that screen automatically off
     * */
    fun screenTimeOut() {
        preferencesManager =
            SharedPreferencesManager.getInstance(getInstance())
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
        // Simulate finishing and sending data
        val resultIntent = Intent()
        resultIntent.putExtra(Constants.FromScreen, ScreenEnum.PAY_WITH_QR.ordinal)
        setResult(RESULT_OK, resultIntent) // Set the result to be OK
        finish() // Finish the activity

//        Intent intent = new Intent(PayWithQrActivity.this, MainActivity.class);
//        // Clear all previous activities
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        startActivity(intent);
    }

    /// When process is completed. Data will be insert into database
    fun insertDataOnProcessDone(
        amt: Double,
        payment: Payment,
        volumeOfMilk: Float,
        milkTemperature: Float
    ) {
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
                    AppDatabase.getInstance(this@PayWithQrActivity).transactionDao()
                assert(date != null)

                /// Here I convert volume of milk into string and set 3 digits after dot(.)
                val truncatedValueOfMilkVolume = String.format("%.2f", volumeOfMilk).toFloat()

                /// Here I convert temperature of milk into string and set 3 digits after dot(.)
                val strMilkTemperature = String.format("%.3f", milkTemperature)
                val transactionId = Constants.insertTransaction(
                    this@PayWithQrActivity,
                    transactionDao,
                    "ONLINE",
                    "",
                    date,
                    time,
                    amt,
                    "SUCCESS",
                    payment.get("vpa"),
                    truncatedValueOfMilkVolume,
                    strMilkTemperature
                )
                Log.e(TAG, "onCreate: $transactionId")
                Log.e(TAG, "onCreate: " + Gson().toJson(transactionDao.allTransactions))


                /// Go to Home screen
                goToHomeScreen()

//                    Intent intent = new Intent(PayWithQrActivity.this, MainActivity.class);
//                    // Clear all previous activities
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    startActivity(intent);
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun onDestroy() {

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
        var preferencesManager: SharedPreferencesManager? = null
        private var usbSerialCommunication: UsbSerialCommunication? = null
    }
}
