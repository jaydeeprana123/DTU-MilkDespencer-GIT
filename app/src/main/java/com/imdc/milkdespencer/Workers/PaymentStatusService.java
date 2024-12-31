package com.imdc.milkdespencer.Workers;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.razorpay.Payment;
import com.razorpay.QrCode;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import org.json.JSONObject;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class PaymentStatusService extends Service {
    private static final String TAG = "PaymentStatusService";
    private static final String RAZORPAY_KEY_ID = "rzp_live_oTrQqk0HauuUWZ"; //"rzp_test_bfiWftOYB0MCR7";
    private static final String RAZORPAY_KEY_SECRET = "7lBcCfNsgl7wKtshFz7QCm8F"; //"VuX6RLVKtB6MBILQKRzcMeZy";
    private String qrCodeId; // Provide your QR code ID here
    private Timer timer;
    private Handler handler;
    private RazorpayClient razorpayClient;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        timer = new Timer();

        try {
            // Initialize Razorpay client with your key and secret
//            razorpayClient = new RazorpayClient("YOUR_KEY_ID", "YOUR_KEY_SECRET");
            razorpayClient = new RazorpayClient(RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET);

        } catch (RazorpayException e) {
            e.printStackTrace();
        }

        schedulePaymentStatusFetch();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("qr_code_id")) {
            qrCodeId = intent.getStringExtra("qr_code_id");
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void schedulePaymentStatusFetch() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        fetchPaymentStatus();
                    }
                });
            }
        }, 0, 2000); // Fetch payment status every 2 seconds
    }

    @SuppressLint("StaticFieldLeak")
    private void fetchPaymentStatus() {
        try {
            JSONObject params = new JSONObject();
            params.put("count", "1");

            if (qrCodeId == null || qrCodeId.isEmpty()) {
                Log.e(TAG, "QR Code ID is empty");
                return;
            }

            // Fetch payments for the QR code ID
            new AsyncTask<Void, Void, List<QrCode>>() {
                @Override
                protected List<QrCode> doInBackground(Void... voids) {
                    try {
                        return razorpayClient.qrCode.fetchAllPayments(qrCodeId, params);
                    } catch (RazorpayException e) {
                        Log.e(TAG, "RazorpayException: " + e.getMessage());
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(List<QrCode> qrcodePayment) {
                    super.onPostExecute(qrcodePayment);
                    if (qrcodePayment != null) {
                        for (Object qrCode : qrcodePayment) {

                            Payment payment = new Gson().fromJson(new Gson().toJson(qrCode), Payment.class);
                            Log.d(TAG, "QR Code Payment: " + new Gson().toJson(payment));
                            Intent broadcastIntent = new Intent("payment_status_action");
                            broadcastIntent.putExtra("payment_status", new Gson().toJson(payment));
                            sendBroadcast(broadcastIntent);
                            break;
                        }
//                        stopSelf();
                    } else {
                        Log.e(TAG, "Payment list is null");
                    }
                }
            }.execute();


        } catch (Exception e) {
            Log.e(TAG, "Error fetching payment status: " + e.getMessage());
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
    }
}
