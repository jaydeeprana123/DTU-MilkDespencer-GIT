package com.imdc.milkdespencer.Workers

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.AsyncTask
import android.os.Handler
import android.os.IBinder
import android.util.Log
import com.google.gson.Gson
import com.imdc.milkdespencer.common.Constants
import com.imdc.milkdespencer.common.SharedPreferencesManager
import com.razorpay.Payment
import com.razorpay.QrCode
import com.razorpay.RazorpayClient
import com.razorpay.RazorpayException
import org.json.JSONObject
import java.util.Timer
import java.util.TimerTask

class PaymentStatusService : Service() {
    private var qrCodeId: String? = null // Provide your QR code ID here
    private var timer: Timer? = null
    private var handler: Handler? = null
    private var razorpayClient: RazorpayClient? = null
    override fun onCreate() {
        super.onCreate()
        handler = Handler()
        timer = Timer()
        preferencesManager = SharedPreferencesManager.getInstance(
            applicationContext
        )
        try {
            // Initialize Razorpay client with your key and secret
//            razorpayClient = new RazorpayClient("YOUR_KEY_ID", "YOUR_KEY_SECRET");
            razorpayClient = RazorpayClient(
                preferencesManager?.get(Constants.RazorPayKey, "rzp_live_oTrQqk0HauuUWZ").toString(),
                preferencesManager?.get(
                    Constants.RazorPaySecretKey, "7lBcCfNsgl7wKtshFz7QCm8F"
                ).toString()
            )
        } catch (e: RazorpayException) {
            e.printStackTrace()
        }
        schedulePaymentStatusFetch()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent != null && intent.hasExtra("qr_code_id")) {
            qrCodeId = intent.getStringExtra("qr_code_id")
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun schedulePaymentStatusFetch() {
        timer!!.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                handler!!.post { fetchPaymentStatus() }
            }
        }, 0, 2000) // Fetch payment status every 2 seconds
    }

    @SuppressLint("StaticFieldLeak")
    private fun fetchPaymentStatus() {
        try {
            val params = JSONObject()
            params.put("count", "1")
            if (qrCodeId == null || qrCodeId!!.isEmpty()) {
                Log.e(TAG, "QR Code ID is empty")
                return
            }

            // Fetch payments for the QR code ID
            object : AsyncTask<Void?, Void?, List<QrCode>?>() {
                override fun doInBackground(vararg p0: Void?): List<QrCode>? {
                    return try {
                        razorpayClient!!.qrCode.fetchAllPayments(qrCodeId, params)
                    } catch (e: RazorpayException) {
                        Log.e(TAG, "RazorpayException: " + e.message)
                        null
                    }
                }

                override fun onPostExecute(qrcodePayment: List<QrCode>?) {
                    super.onPostExecute(qrcodePayment)
                    if (qrcodePayment != null) {
                        for (qrCode in qrcodePayment) {
                            val payment =
                                Gson().fromJson(Gson().toJson(qrCode), Payment::class.java)
                            Log.d(TAG, "QR Code Payment: " + Gson().toJson(payment))
                            val broadcastIntent = Intent("payment_status_action")
                            broadcastIntent.putExtra("payment_status", Gson().toJson(payment))
                            sendBroadcast(broadcastIntent)
                            break
                        }
                        //                        stopSelf();
                    } else {
                        Log.e(TAG, "Payment list is null")
                    }
                }


            }.execute()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching payment status: " + e.message)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (timer != null) {
            timer!!.cancel()
        }
    }

    companion object {
        var preferencesManager: SharedPreferencesManager? = null
        private const val TAG = "PaymentStatusService"
        private const val RAZORPAY_KEY_ID = "rzp_live_oTrQqk0HauuUWZ" //"rzp_test_bfiWftOYB0MCR7";
        private const val RAZORPAY_KEY_SECRET =
            "7lBcCfNsgl7wKtshFz7QCm8F" //"VuX6RLVKtB6MBILQKRzcMeZy";
    }
}
