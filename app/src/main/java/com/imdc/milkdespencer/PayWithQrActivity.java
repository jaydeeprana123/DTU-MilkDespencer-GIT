package com.imdc.milkdespencer;


import static com.imdc.milkdespencer.common.Constants.FromScreen;
import static com.imdc.milkdespencer.common.Constants.KEY_API_CALL;
import static com.imdc.milkdespencer.common.Constants.KEY_ELECTRICITY;
import static com.imdc.milkdespencer.common.Constants.KEY_QR_GENERATE;
import static com.imdc.milkdespencer.common.Constants.KEY_TRANSACTION_START_DATE;
import static com.imdc.milkdespencer.common.Constants.KEY_TRANSACTION_START_TIME;
import static com.imdc.milkdespencer.common.Constants.KEY_USB;
import static com.imdc.milkdespencer.common.Constants.KEY_WEIGHT;
import static com.imdc.milkdespencer.common.Constants.MachineId;
import static com.imdc.milkdespencer.common.Constants.MilkBasePrice;
import static com.imdc.milkdespencer.common.Constants.ScreenTimeOutPref;
import static com.imdc.milkdespencer.common.Constants.generateSafeUniqueTransactionId;
import static com.imdc.milkdespencer.common.Constants.isNetworkAvailable;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.imdc.milkdespencer.Workers.PaymentStatusService;
import com.imdc.milkdespencer.adapter.SpnCurrencyAdapter;
import com.imdc.milkdespencer.adapter.SpnLitersAdapter;
import com.imdc.milkdespencer.callBacks.RazorpayResponseCallback;
import com.imdc.milkdespencer.common.Constants;
import com.imdc.milkdespencer.common.LottieDialog;
import com.imdc.milkdespencer.common.SharedPreferencesManager;
import com.imdc.milkdespencer.common.UsbSerialCommunication;
import com.imdc.milkdespencer.enums.ScreenEnum;
import com.imdc.milkdespencer.models.Response.RazorpayQrPaymentResponse;
import com.imdc.milkdespencer.models.ResponseMilkDispense;
import com.imdc.milkdespencer.models.ResponseTempStatus;
import com.imdc.milkdespencer.models.SendToDevice;
import com.imdc.milkdespencer.network.RetrofitClient;
import com.imdc.milkdespencer.roomdb.AppDatabase;
import com.imdc.milkdespencer.roomdb.entities.TransactionEntity;
import com.imdc.milkdespencer.roomdb.interfaces.TransactionDao;
import com.razorpay.Payment;
import com.razorpay.PaymentData;
import com.razorpay.PaymentResultWithDataListener;
import com.razorpay.QrCode;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PayWithQrActivity extends AppCompatActivity implements PaymentResultWithDataListener {


    private boolean isTransactionInsertionStarted = false;

    private boolean isMilkVendingStarted = false;

    private boolean isDatabaseOperationStarted = false;

    // Add this with your other class variables
    private volatile boolean transactionCompleted = false;

    private String qrCodeId = "";

    private LottieDialog milkDispensingDialog;

    static SharedPreferencesManager preferencesManager;
    private UsbSerialCommunication usbSerialCommunication;
    private final String TAG = PayWithQrActivity.class.getSimpleName();
    private final int previousSelectionAMT = 0;

    boolean isCharging;

    private Handler handlerForSendData = new Handler(); // Create a Handler instance

    private Handler handler = new Handler(); // Create a Handler instance
    private Runnable runnable; // Declare the Runnable

    private boolean isElectricityLost = false;

    private Handler timeoutHandler;
    private Runnable timeoutRunnable;

    private Handler qrCodeTimeoutHandler = new Handler(Looper.getMainLooper());

    private Runnable qrCodeTimeoutRunnable;

    private Handler retryHandler = new Handler(Looper.getMainLooper());
    private Runnable retryRunnable;
    private int retryCount = 0;
    private final int MAX_RETRIES = 7; // At 2, 3, 4, 5 minutes
    private boolean hasValidTransaction = false;

    private String transactionStartTime;
    private String transactionStartDate;


    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent == null || intent.getAction() == null) return;

            String action = intent.getAction();

            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null && device.getProductName() != null && device.getProductName().contains("CP2102")) {

                    UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
                    // ✅ Check if permission is actually revoked
                    if(usbManager != null && !usbManager.hasPermission(device)) {

                        logError("USB", "USB disconnected (Electricity GONE)");
                        // Stop communication, update UI

                        if (!isElectricityLost) {
                            isElectricityLost = true;

                            /// If QR code dialog is showing or null.. Dismiss the dialog
                            if (dialog.get() != null && dialog.get().isShowing()) {
                                dialog.get().dismiss();
                            }

                            if (milkDispensingDialog != null && milkDispensingDialog.isShowing()) {
                                milkDispensingDialog.dismiss();
                            }


                            try {
                                Constants.saveLogs(getApplicationContext(), "Lost Electricity", KEY_ELECTRICITY);
                            } catch (Exception e) {
                                logError("PaymentLog", "Logging failed: ${e.message}");
                                // Don't crash, just log the error silently
                            }

                            tvProcessing.setText("Sorry. No Electricity, please try after some time!");

                            String transactionJson = (preferencesManager.get(Constants.SavedTransaction, "")).toString();
                            String paymentJson = (preferencesManager.get(Constants.PaymentReceived, "")).toString();

                            if (!transactionJson.isEmpty()) {
                                TransactionEntity transactionEntity = new Gson().fromJson(transactionJson, TransactionEntity.class);

                                logError("ElectricityLost transactionJson ", transactionJson);

                                updateTransactionIfElectricityLost(transactionEntity);
                            } else if (!paymentJson.isEmpty()) {
                                Payment payment = new Gson().fromJson(paymentJson, Payment.class);
                                if (payment != null && payment.get("amount") != null) {
                                    try {
                                        float amount = Float.parseFloat(payment.get("amount").toString());
                                        float amt = amount / 100;
                                        insertTransactionIfElectricityLost(amt, payment, 0);
                                    } catch (NumberFormatException e) {
                                        e.printStackTrace();
                                        // Optional: log or handle parse error
                                    }
                                }
                            } else {
                                goToHomeScreen();
                            }
                        }
                    }


                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    logError("USB", "USB connected (Electricity BACK)");

                }
            }
        }
    };


//    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
//
//            boolean isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING
//                    || status == BatteryManager.BATTERY_STATUS_FULL);
//
//            if (isCharging) {
//                if (isElectricityLost) {
//                    isElectricityLost = false;
//                }
//            } else {
//                if (!isElectricityLost) {
//                    isElectricityLost = true;
//
//                    /// If QR code dialog is showing or null.. Dismiss the dialog
//                    if (dialog.get() != null && dialog.get().isShowing()) {
//                        dialog.get().dismiss();
//                    }
//
//                    if (milkDispensingDialog != null && milkDispensingDialog.isShowing()) {
//                        milkDispensingDialog.dismiss();
//                    }
//
//                    Constants.saveLogs(PayWithQrActivity.this, "Lost Electricity");
//                    tvProcessing.setText("Sorry. No Electricity, please try after some time!");
//
//                    String transactionJson = (preferencesManager.get(Constants.SavedTransaction, "")).toString();
//                    String paymentJson = (preferencesManager.get(Constants.PaymentReceived, "")).toString();
//
//                    if (!transactionJson.isEmpty()) {
//                        TransactionEntity transactionEntity = new Gson().fromJson(transactionJson, TransactionEntity.class);
//
//                        logError("ElectricityLost transactionJson ", transactionJson);
//
//                        updateTransactionIfElectricityLost(transactionEntity);
//                    } else if (!paymentJson.isEmpty()) {
//                        Payment payment = new Gson().fromJson(paymentJson, Payment.class);
//                        if (payment != null && payment.get("amount") != null) {
//                            try {
//                                float amount = Float.parseFloat(payment.get("amount").toString());
//                                float amt = amount / 100;
//                                insertTransactionIfElectricityLost(amt, payment, 0);
//                            } catch (NumberFormatException e) {
//                                e.printStackTrace();
//                                // Optional: log or handle parse error
//                            }
//                        }
//                    } else {
//                        btnBackToHome.setVisibility(View.VISIBLE);
//                    }
//                }
//            }
//        }
//    };

    JSONObject paymentObject = new JSONObject();
    boolean isCommandSent = false;
    boolean isDataSent = false;

    AtomicReference<Dialog> dialog = new AtomicReference<>();
    private final BroadcastReceiver paymentStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if ("payment_status_action".equals(intent.getAction())) {

                // Cancel QR code timeout
                if (qrCodeTimeoutHandler != null && qrCodeTimeoutRunnable != null) {
                    qrCodeTimeoutHandler.removeCallbacks(qrCodeTimeoutRunnable);
                }

                String paymentStatusJson = intent.getStringExtra("payment_status");
                logError(TAG, "onReceive: " + paymentStatusJson);

                if (paymentStatusJson != null && !paymentStatusJson.isEmpty() && !isTransactionCompleted) {
                    isTransactionCompleted = true;

                    Payment payment = new Gson().fromJson(paymentStatusJson, Payment.class);
                    logError(TAG, "onReceive:payment " + paymentStatusJson);

                    if (payment != null && payment.has("amount")) {

                        if (dialog.get() != null && dialog.get().isShowing()) {
                            dialog.get().dismiss();
                        }

                        String payCodeId = payment.get("id").toString();
                        float amount = Float.parseFloat(payment.get("amount").toString());
                        double amt = amount / 100;

                        preferencesManager.save(Constants.PaymentReceived, new Gson().toJson(payment));
                        preferencesManager.save(Constants.PaidAmt, amt);

                        /// Get transaction json from shared preference
                        String transactionJson = (preferencesManager.get(Constants.SavedTransaction, "")).toString();

                        if (!transactionJson.isEmpty()) {

                            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

                            String transactionDate = dateFormatter.format(System.currentTimeMillis());
                            String transactionTime = timeFormatter.format(System.currentTimeMillis());

                            TransactionEntity transactionEntity = new Gson().fromJson(transactionJson, TransactionEntity.class);
                            logError(TAG + "PAYMENT DONE transactionJson ", transactionJson);
                            transactionEntity.setTransactionDate(transactionDate);
                            transactionEntity.setTransactionTime(transactionTime);
                            transactionEntity.setAmount(amt);
                            transactionEntity.setBankTransactionNo(payCodeId);

                            if (isTransactionHandled.getAndSet(true)) return;

                            updateIntoDatabaseAfterPaymentDone(transactionEntity);
                        } else {

                          //  insertIntoDatabaseAfterPaymentDone(payCodeId, amt);

                        }


                        // Stop service after handling payment
                        Intent serviceIntent = new Intent(PayWithQrActivity.this, PaymentStatusService.class);
                        stopService(serviceIntent);
                    }
                }
            }
        }
    };

    RetrofitClient retrofitClient;
    RazorpayClient razorpay;
    GridView gv_CurrencyLiters;

    private TextView tvProcessing;

    TabLayout tabLayout;

    private MaterialButton btnGenerateQr;
    private MaterialButton btnBackToHome;

    private boolean shouldContinueSending = false;

    private boolean isStopConditionMet = false;

    private static final long SEND_INTERVAL_MS = 500; // Send every 500ms

    private boolean isTransactionCompleted = false;

    private final AtomicBoolean isTransactionHandled = new AtomicBoolean(false);

    private boolean isNavigated = false;


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_pay_with_qr);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

//        IntentFilter battertyFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
//        registerReceiver(batteryReceiver, battertyFilter);

        transactionStartDate = getIntent().getStringExtra(KEY_TRANSACTION_START_DATE);
        transactionStartTime = getIntent().getStringExtra(KEY_TRANSACTION_START_TIME);
        screenTimeOut();

        usbSerialCommunication = new UsbSerialCommunication(getApplicationContext());

        if (!usbSerialCommunication.connected) {
            usbSerialCommunication.connect();
            usbSerialCommunication.setBaudRate(115200);
        }

        btnGenerateQr = findViewById(R.id.btnGenerateQr);
        btnBackToHome = findViewById(R.id.btnBackToHome);
        tabLayout = findViewById(R.id.tabLayout);

        preferencesManager = SharedPreferencesManager.getInstance(getApplicationContext());
        /// When user comes first delete the previously saved payment data in shared preference
        preferencesManager.delete(Constants.PaymentReceived);
        preferencesManager.delete(Constants.PaidAmt);
        preferencesManager.delete(Constants.SavedTransaction);

        logError(TAG + "api key", preferencesManager.get(Constants.RazorPayKey, "rzp_live_oTrQqk0HauuUWZ").toString());

        gv_CurrencyLiters = findViewById(R.id.gridViewCurrencyLiters);
        tvProcessing = findViewById(R.id.tvProcessing);
        SpnCurrencyAdapter currencyAdapter = new SpnCurrencyAdapter(this);
        SpnLitersAdapter litersAdapter = new SpnLitersAdapter(this);

        IntentFilter filter = new IntentFilter("payment_status_action");
        registerReceiver(paymentStatusReceiver, filter);

        if (tabLayout.getSelectedTabPosition() == 0) {
            gv_CurrencyLiters.setAdapter(litersAdapter);
            gv_CurrencyLiters.setNumColumns(4);
        } else {
            gv_CurrencyLiters.setAdapter(currencyAdapter);
            gv_CurrencyLiters.setNumColumns(3);
        }
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        // Milk in liters selected
                        gv_CurrencyLiters.setAdapter(litersAdapter);
                        gv_CurrencyLiters.setNumColumns(4);
                        break;
                    case 1:
                        // Milk in price selected
                        gv_CurrencyLiters.setAdapter(currencyAdapter);
                        gv_CurrencyLiters.setNumColumns(3);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });


        gv_CurrencyLiters.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                gv_CurrencyLiters.setVisibility(View.GONE);
                btnBackToHome.setVisibility(View.GONE);

                //   If User clicks on the grid item. Runnable should be close
                // Cancel the delayed task
                if (handler != null && runnable != null) {
                    handler.removeCallbacks(runnable);
                }


                if (!usbSerialCommunication.connected) {
                    try {
                        Constants.saveLogs(getApplicationContext(), "USB Not Connected", KEY_USB);
                    } catch (Exception e) {
                        logError("PaymentLog", "Logging failed: ${e.message}");
                        // Don't crash, just log the error silently
                    }

                    Constants.showUSBConnectionErrorMessageDialog(PayWithQrActivity.this, "Alert", "Usb is not connected properly!", (dialog1, which) -> {
                        goToHomeScreen();
                    });
                    return;
                }


                String customerId = preferencesManager.get(Constants.RazorPayCustomerID, "").toString();
                String machineId = preferencesManager.get(Constants.MachineId, "").toString();

                if (customerId.isEmpty()) {

                    gv_CurrencyLiters.setVisibility(View.VISIBLE);
                    btnBackToHome.setVisibility(View.VISIBLE);

                    Constants.showAlertDialog(PayWithQrActivity.this, "Error", "Customer Id cannot be empty");
                    return;
                }


                if (gv_CurrencyLiters.getAdapter() instanceof SpnLitersAdapter) {
                    logError(TAG, "onItemSelected: " + gv_CurrencyLiters.getAdapter().getItem(position));
                    Double numericValueFromString = extractNumericValueFromString(gv_CurrencyLiters.getAdapter().getItem(position).toString());

                    // Double numericValueFromString = extractNumericValueFromString(LitersSpinnerData.volumeValuesInLtr[0].toString());

                    logError(TAG + "numericValueFromString", numericValueFromString.toString());

                    String inputVal = numericValueFromString != null ? String.valueOf(numericValueFromString) : "0.0";

                    double ltrs = Double.parseDouble(inputVal);
                    double amt = Constants.calculateMilkPrice(ltrs, PayWithQrActivity.this);
                    amt = Double.parseDouble(String.format("%.2f", amt));

                    try {
                        paymentObject.put("name", "Milk Vending Machine");
                        paymentObject.put("description", "Payment For Milk");
                        paymentObject.put("currency", "INR");
                        paymentObject.put("amount", amt * 100); // Amount in paise (e.g., 10000 paise = INR 100)
//                        paymentObject.put("amount", 100); // Amount in paise (e.g., 10000 paise = INR 100)
                        Constants.showAcceptDialog(PayWithQrActivity.this, "Please Confirm", "You need to pay the ₹" + amt + " for " + gv_CurrencyLiters.getAdapter().getItem(position).toString(), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
//                                Toast.makeText(PayWithQrActivity.this, "YES ", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();

                                gv_CurrencyLiters.setVisibility(View.GONE);
                                btnBackToHome.setVisibility(View.GONE);
                                tvProcessing.setVisibility(View.VISIBLE);

                                ResponseTempStatus responseTempStatus = new Gson().fromJson(preferencesManager.get(Constants.ResponseTempStatus, "").toString(), ResponseTempStatus.class);

                                /// Here it will check that door is open or close
                                // If door is close then allow to start milking
                                if (!responseTempStatus.getConnectivity()) {

                                    if (isNetworkAvailable(PayWithQrActivity.this)) {
                                        executeGenerateQRCodeTask(paymentObject, customerId, machineId);
                                    } else {
                                        tvProcessing.setText("Sorry. Internet is not available!");
                                        btnBackToHome.setVisibility(View.VISIBLE);
                                        //  Toast.makeText(PayWithQrActivity.this, "Sorry Network is not available", Toast.LENGTH_SHORT).show();
                                    }


                                } else {
                                    // If door is open then close the cash machine and send to the home page
                                    goToHomeScreen();
                                }

                            }
                        }, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
//                                Toast.makeText(PayWithQrActivity.this, "NO ", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();

                                gv_CurrencyLiters.setVisibility(View.VISIBLE);
                                btnBackToHome.setVisibility(View.VISIBLE);

                            }
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (gv_CurrencyLiters.getAdapter() instanceof SpnCurrencyAdapter) {
                    double cost = Double.parseDouble(gv_CurrencyLiters.getAdapter().getItem(position).toString().replace("₹", ""));
                    cost = Float.parseFloat(String.format("%.2f", cost));

                    //   Toast.makeText(PayWithQrActivity.this, "COST " + cost, Toast.LENGTH_SHORT).show();
                    double weight = Constants.calculateMilkAmount(cost, PayWithQrActivity.this);
                    String weightStr = weight > 0 && weight < 1 ? weight + " (Ml)." : weight + "(Ltr).";
                    float milkSellingPrice = Float.parseFloat(preferencesManager.get(Constants.MilkBasePrice, "0.0").toString());
                    float volumeToDisplay = Float.parseFloat(String.valueOf((cost / milkSellingPrice)));
                    volumeToDisplay = Float.parseFloat(String.format("%.2f", volumeToDisplay));


                    try {
                        paymentObject.put("name", "Milk Vending Machine");
                        paymentObject.put("description", "Payment For Milk");
                        paymentObject.put("currency", "INR");
                        paymentObject.put("amount", cost * 100); // Amount in paise (e.g., 10000 paise = INR 100)
//                        paymentObject.put("amount", 100); // Amount in paise (e.g., 10000 paise = INR 100)
                        float finalVolumeToDisplay = volumeToDisplay;
                        Constants.showAcceptDialog(PayWithQrActivity.this, "Please Confirm", "You need to pay the ₹" + cost + " for " + volumeToDisplay + "Ltr", (dialog, which) -> {
                            dialog.dismiss();

                            /// Check that volume amount is more than 5 lites
                            if (finalVolumeToDisplay > 5) {
                                showAlertExceedLimit();
                            } else {

                                gv_CurrencyLiters.setVisibility(View.GONE);
                                btnBackToHome.setVisibility(View.GONE);
                                tvProcessing.setVisibility(View.VISIBLE);

                                ResponseTempStatus responseTempStatus = new Gson().fromJson(preferencesManager.get(Constants.ResponseTempStatus, "").toString(), ResponseTempStatus.class);

                                /// Here it will check that door is open or close
                                // If door is close then allow to start milking
                                if (!responseTempStatus.getConnectivity()) {

                                    if (isNetworkAvailable(PayWithQrActivity.this)) {
                                        executeGenerateQRCodeTask(paymentObject, customerId, machineId);
                                    } else {

                                        tvProcessing.setText("Sorry. Internet is not available!");
                                        btnBackToHome.setVisibility(View.VISIBLE);
                                        //   Toast.makeText(PayWithQrActivity.this, "Sorry Network is not available", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    // If door is open then close the cash machine and send to the home page
                                    goToHomeScreen();
                                }
                            }


                        }, (dialog, which) -> {
                            dialog.dismiss();

                            gv_CurrencyLiters.setVisibility(View.VISIBLE);
                            btnBackToHome.setVisibility(View.VISIBLE);

                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    logError(TAG, "onItemSelected: " + gv_CurrencyLiters.getAdapter().getItem(position));
                }
            }
        });


        btnGenerateQr.setVisibility(View.GONE);
//        btnGenerateQr.setOnClickListener(new View.OnClickListener() {
//            @SuppressLint("StaticFieldLeak")
//            @Override
//            public void onClick(View v) {
//                int selectedId = tabLayout.getSelectedTabPosition();
////                String inputVal = tieInputVal.getText().toString();
//                Double numericValueFromString = extractNumericValueFromString(gv_CurrencyLiters.getSelectedItem().toString());
//                String inputVal = numericValueFromString != null ? String.valueOf(numericValueFromString) : "0.0";
//                logError(TAG, "onClick: " + inputVal);
//
//                String weightInLiter = "";
//
//                if (selectedId == 0) {
//
//                    double ltrs = Double.parseDouble(inputVal);
//                    double amt = Constants.calculateMilkPrice(ltrs, PayWithQrActivity.this);
//                    weightInLiter = String.valueOf(ltrs);
//                    try {
//                        paymentObject.put("name", "Milk Vending Machine");
//                        paymentObject.put("description", "Payment For Milk");
//                        paymentObject.put("currency", "INR");
//                        paymentObject.put("amount", amt * 100); // Amount in paise (e.g., 10000 paise = INR 100)
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                } else if (selectedId == 1) {
//                    double cost = Double.parseDouble(gv_CurrencyLiters.getSelectedItem().toString().replace("₹", ""));
//                    //   Toast.makeText(PayWithQrActivity.this, "COST " + cost, Toast.LENGTH_SHORT).show();
//                    double weight = Constants.calculateMilkAmount(cost, PayWithQrActivity.this);
//                    weightInLiter = String.valueOf(weight);
//                    try {
//                        paymentObject.put("name", "Milk Vending Machine");
//                        paymentObject.put("description", "Payment For Milk");
//                        paymentObject.put("currency", "INR");
//                        paymentObject.put("amount", cost * 100); // Amount in paise (e.g., 10000 paise = INR 100)
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//
//                }
//                String customerId = preferencesManager.get(Constants.RazorPayCustomerID, "").toString();
//                String machineId = preferencesManager.get(Constants.MachineId, "").toString();
//                if (customerId.isEmpty()) {
//                    Constants.showAlertDialog(PayWithQrActivity.this, "Error", "Customer Id cannot be empty");
//                    return;
//                }
//
//                logError(TAG, "onClick: " + new Gson().toJson(paymentObject));
//                String finalWeightInLiter = weightInLiter;
//                new AsyncTask<Void, Void, Void>() {
//                    @Override
//                    public Void doInBackground(Void... voids) {
//                        try {
//                            razorpay = new RazorpayClient("rzp_test_bfiWftOYB0MCR7", "VuX6RLVKtB6MBILQKRzcMeZy");  //TEST
////                            razorpay = new RazorpayClient("rzp_live_oTrQqk0HauuUWZ", "7lBcCfNsgl7wKtshFz7QCm8F");//LIVE
//
//                            JSONObject qrRequest = new JSONObject();
//                            qrRequest.put("type", "upi_qr");
//                            qrRequest.put("name", "Milk Vending booth");
//                            qrRequest.put("usage", "single_use");
//                            qrRequest.put("fixed_amount", true);
//                            qrRequest.put("payment_amount", paymentObject.get("amount"));
//                            qrRequest.put("description", machineId);
////                            qrRequest.put("customer_id", "cust_NQXXhGiitVX9xe"); //Test
////                            qrRequest.put("customer_id", "cust_NWIoi0QrjXC2ez");//LIVE
//                            qrRequest.put("customer_id", customerId);//LIVE
//                            long currentTime = System.currentTimeMillis();
//                            long closeByTime = currentTime + (5 * 60 * 1000);
//
//                            // Check if close_by is within the acceptable range
//                            if (closeByTime < 946684800L * 1000 || closeByTime > 4765046400L * 1000) {
//                                // Handle the case where close_by is out of range
//                                throw new IllegalArgumentException("close_by out of acceptable range");
//                            }
//
//                            qrRequest.put("close_by", closeByTime / 1000);
//                            JSONObject notes = new JSONObject();
//                            notes.put("notes_key_1", "Milk Vending");
//                            notes.put("notes_key_2", String.valueOf(paymentObject));
//                            qrRequest.put("notes", notes);
//
//                            logError(TAG, "doInBackground: " + new Gson().toJson(qrRequest));
//
//                            QrCode qrcode = razorpay.qrCode.create(qrRequest);
//
//                            logError(TAG, "doInBackground: " + new Gson().toJson(qrcode));
//                            if (qrcode != null) {
//                                String imageUrl = qrcode.get("image_url").toString();
//                                String qrCodeId = qrcode.get("id").toString();
//                                runOnUiThread(() -> {
//                                    dialog.set(showQRCodeDialog(imageUrl));
//                                    dialog.get().show();
//                                    Intent serviceIntent = new Intent(PayWithQrActivity.this, PaymentStatusService.class);
//                                    serviceIntent.putExtra("qr_code_id", qrCodeId);
//                                    startService(serviceIntent);
//
//                                    /* Dialog close after 6 minutes*/
//                                    // Schedule dialog dismissal after 6 minutes (360,000 milliseconds)
//                                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            Dialog currentDialog = dialog.get();
//                                            if (currentDialog != null && currentDialog.isShowing()) {
//
//                                                logError(TAG + "btnGenerateQr", "button CLick");
//                                                currentDialog.dismiss();
//                                                /// Insert data into database
//                                                new Thread(new Runnable() {
//                                                    @Override
//                                                    public void run() {
//
//                                                        try {
//                                                            String dateFormat = "yyyy-MM-dd";
//                                                            String timeFormat = "HH:mm:ss";
//                                                            SimpleDateFormat dateFormatter = new SimpleDateFormat(dateFormat);
//                                                            SimpleDateFormat timeFormatter = new SimpleDateFormat(timeFormat);
//
//                                                            String date = dateFormatter.format(System.currentTimeMillis());
//                                                            String time = timeFormatter.format(System.currentTimeMillis());
//                                                            // Print the combined date and time
//
//                                                            if (paymentObject.has("amount")) {
//                                                                float amount = Float.parseFloat(paymentObject.get("amount").toString());
//                                                                double amt = amount / 100;
//
//                                                                logError(TAG + "amountttt in string", String.valueOf(amt));
//
//                                                                TransactionDao transactionDao = AppDatabase.getInstance(PayWithQrActivity.this).transactionDao();
//                                                                assert date != null;
//                                                                long transactionId = Constants.insertTransaction(PayWithQrActivity.this, transactionDao, "ONLINE", "", date, time, amt, "TIME OUT", "", Float.parseFloat(finalWeightInLiter), "");
//                                                                logError(TAG, "onCreate: " + transactionId);
//                                                                logError(TAG, "onCreate: " + new Gson().toJson(transactionDao.getAllTransactions()));
//
//
//                                                            }
//
//                                                            goToHomeScreen();
//
//                                                        } catch (Exception e) {
//                                                            e.printStackTrace();
//                                                        }
//                                                    }
//                                                }).start();
//                                            }
//                                        }
//                                    }, 360000);
//
//                                });
//                            }
//
//                        } catch (RazorpayException | JSONException e) {
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    Constants.showAlertDialog(PayWithQrActivity.this, "Error", e.getMessage());
//                                }
//                            });
//                        }
//                        return null;
//                    }
//                }.execute();
//
//            }
//        });

        btnBackToHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Cancel the delayed task
                if (handler != null && runnable != null) {
                    handler.removeCallbacks(runnable);
                }

                // Remove the Runnable from the Handler to avoid memory leaks
                if (timeoutHandler != null && timeoutRunnable != null) {
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                }


                // Remove the Runnable from the Handler to avoid memory leaks
                if (qrCodeTimeoutHandler != null && qrCodeTimeoutRunnable != null) {
                    qrCodeTimeoutHandler.removeCallbacks(qrCodeTimeoutRunnable);
                }

                goToHomeScreen();
            }
        });

//        checkout.open(this, paymentObject);
    }


    @SuppressLint("StaticFieldLeak")
    public void executeGenerateQRCodeTask(JSONObject paymentObject, String customerId, String machineId) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            try {
                generateQRCode(paymentObject, customerId, machineId);
            } finally {
                executor.shutdown(); // Shutdown after task finishes, whether successful or failed
            }
        });
    }



    /*
     * Generate QR Code*/
    private void generateQRCode(JSONObject paymentObject, String customerId, String machineId) {
        try {
            RazorpayClient razorpay = new RazorpayClient(preferencesManager.get(Constants.RazorPayKey, "rzp_live_oTrQqk0HauuUWZ").toString(), preferencesManager.get(Constants.RazorPaySecretKey, "7lBcCfNsgl7wKtshFz7QCm8F").toString());

            //  RazorpayClient razorpay = new RazorpayClient("rzp_live_oTrQqk0HauuUWZ", "7lBcCfNsgl7wKtshFz7QCm8F");


            logError(TAG + "api key", preferencesManager.get(Constants.RazorPayKey, "rzp_live_oTrQqk0HauuUWZ").toString());
            logError(TAG + "secret key", preferencesManager.get(Constants.RazorPaySecretKey, "7lBcCfNsgl7wKtshFz7QCm8F").toString());

            JSONObject qrRequest = createQrRequest(paymentObject, customerId, machineId);
            logError(TAG, "QR Request: " + new Gson().toJson(qrRequest));

            /// Generated QR Code
            QrCode qrcode = razorpay.qrCode.create(qrRequest);
            logError(TAG, "QR Code Response: " + new Gson().toJson(qrcode));

            /// If QR Code is not null then show QR code in an dialog
            if (qrcode != null) {
                handleQrCodeResponse(qrcode, paymentObject);
            }
        } catch (RazorpayException | JSONException e) {

            runOnUiThread(() -> {

                logError("Error razor", e.getMessage());


                try {
                    Constants.saveLogs(getApplicationContext(), " - Error in QR Code Generation",KEY_QR_GENERATE);
                } catch (Exception exc) {
                    logError("PaymentLog", "Logging failed: ${e.message}");
                    // Don't crash, just log the error silently
                }

                btnBackToHome.setVisibility(View.VISIBLE);
                tvProcessing.setText("We're sorry! Please try again after a while.");
                Constants.showAlertDialog(PayWithQrActivity.this, "Error", e.getMessage());
            });

        }
    }


    /*Create Json Object for request*/
    private JSONObject createQrRequest(JSONObject paymentObject, String customerId, String machineId) throws JSONException {
        JSONObject qrRequest = new JSONObject();
        qrRequest.put("type", "upi_qr");
        qrRequest.put("name", "Milk Vending booth");
        qrRequest.put("usage", "single_use");
        qrRequest.put("fixed_amount", true);
        qrRequest.put("payment_amount", paymentObject.get("amount"));
        qrRequest.put("description", machineId);
        qrRequest.put("customer_id", customerId);

        long closeByTime = System.currentTimeMillis() + (5 * 60 * 1000);
        validateCloseByTime(closeByTime);
        qrRequest.put("close_by", closeByTime / 1000);

        JSONObject notes = new JSONObject();
        notes.put("notes_key_1", "Milk Vending");
        notes.put("notes_key_2", paymentObject.toString());
        qrRequest.put("notes", notes);

        return qrRequest;
    }

    private void validateCloseByTime(long closeByTime) {
        long minTime = 946684800L * 1000;
        long maxTime = 4765046400L * 1000;
        if (closeByTime < minTime || closeByTime > maxTime) {
            throw new IllegalArgumentException("close_by out of acceptable range");
        }
    }


    /* Once QR code is generate from Razor pay*/
    private void handleQrCodeResponse(QrCode qrcode, JSONObject paymentObject) {
        logError(TAG, "handleQrCodeResponse : handleQrCodeResponse ");
        /// Image of QR code
        String imageUrl = qrcode.get("image_url").toString();
        qrCodeId = qrcode.get("id").toString();

        logError(TAG , "qrCode generate response" + qrcode.toJson().toString());
        logError(TAG + "qrCode generate response", qrcode.toString());
        logError(TAG , "qrCodeId GOT IT" + qrCodeId);



        runOnUiThread(() -> {
            dialog.set(showQRCodeDialog(imageUrl));
            dialog.get().show();

            // save transaction once Qr code is generated
            if(!isTransactionInsertionStarted){
                isTransactionInsertionStarted = true;
                saveTransactionAsAQRCodeGenerate(paymentObject);
            }

            Intent serviceIntent = new Intent(PayWithQrActivity.this, PaymentStatusService.class);
            serviceIntent.putExtra("qr_code_id", qrCodeId);
            startService(serviceIntent);

            scheduleDialogDismissal(paymentObject, qrCodeId);
        });
    }

    /*Here if QR code is generate and payment status is not get.
    Then transaction will be added as a TIME OUT and go to the home screen*/
    private void scheduleDialogDismissal(JSONObject paymentObject, String qrCodeId) {
        logError(TAG, "scheduleDialogDismissal : Method call ");

        qrCodeTimeoutRunnable = () -> {
            Dialog currentDialog = dialog.get();
            if (currentDialog != null && currentDialog.isShowing()) {
                logError(TAG + "generateQRCode", "Stop payment receiver");


                // Stop background polling
                Intent serviceIntent = new Intent(PayWithQrActivity.this, PaymentStatusService.class);
                stopService(serviceIntent);


                // Unregister dynamic broadcast receivers
                try {
                    if (paymentStatusReceiver != null) {
                        unregisterReceiver(paymentStatusReceiver);
                    }
                } catch (IllegalArgumentException e) {
                    logError(TAG, "usbPermissionReceiver was already unregistered: " + e.getMessage());
                }

                // ✅ Immediately start retry check
                startRetryPaymentCheck(qrCodeId, paymentObject, currentDialog);
            }
        };

        // 🔁 Trigger timeout + retry at 2 minutes
        qrCodeTimeoutHandler.postDelayed(qrCodeTimeoutRunnable, 2 * 60 * 1000);
    }


    interface OnRetryCompleteCallback {
        void onComplete();
    }

    /*Check in razor pay on every 30 seconds, after 2 minutes complete.*/
    private void startRetryPaymentCheck(String qrCodeId, JSONObject paymentObject, Dialog qrCodeDialog) {
        logError(TAG, "Start retry check for RazorPay response");

        retryCount = 0;
        hasValidTransaction = false;

        retryRunnable = new Runnable() {
            @Override
            public void run() {
                if (hasValidTransaction || retryCount >= MAX_RETRIES) {
                    logError(TAG, "Stopping retries. Either success or max retries reached.");
                    return;
                }

                retryCount++;
                logError(TAG, "Checking RazorPay payment, attempt: " + retryCount);

                // ✅ Next retry only fires AFTER API responds
                getRazorPayResponseByQRCodeId(qrCodeId, paymentObject, qrCodeDialog, () -> {
                    if (!hasValidTransaction && retryCount < MAX_RETRIES) {
                        retryHandler.postDelayed(retryRunnable, 30 * 1000);
                    } else if (!hasValidTransaction && retryCount >= MAX_RETRIES) {
                        logError(TAG, "Max retries reached. Timeout.");
                        handleTransactionTimeout(paymentObject);
                    }
                });
            }
        };

        retryHandler.post(retryRunnable); // Start immediately
    }

    /// When there is no response from qr code scan and 5 minutes is done.
    // Then get response from the qrcode
    private void getRazorPayResponseByQRCodeId(String qrCodeId, JSONObject paymentObject, Dialog qrCodeDialog, OnRetryCompleteCallback callback) {

        Constants.getRazorPayResponse(this, qrCodeId, new RazorpayResponseCallback() {
            @Override
            public void onSuccess(RazorpayQrPaymentResponse response) {
                if (isTransactionCompleted) {
                    callback.onComplete(); // ← always call callback
                    return;
                }

                if (response.getItems().isEmpty()) {
                    logError(TAG, "No transaction found. Retry count: " + retryCount);
                    callback.onComplete(); // ← call so next retry schedules
                    return;
                }

                if (response.getItems().get(0).getId() != null && !response.getItems().get(0).getId().isEmpty()) {
                    hasValidTransaction = true;
                    isTransactionCompleted = true;

                    retryHandler.removeCallbacks(retryRunnable);

                    if (qrCodeDialog != null && qrCodeDialog.isShowing()) {
                        qrCodeDialog.dismiss();
                    }

                    try {
                        Constants.saveLogs(getApplicationContext(), qrCodeId + " - Success By Razorpay API Call", KEY_API_CALL);
                    } catch (Exception e) {
                        logError("PaymentLog", "Logging failed: " + e.getMessage());
                    }

                    processValidTransactionByCheckRazorpayAPICall(response, qrCodeId);
                    callback.onComplete(); // ← call after success too
                } else {
                    logError(TAG, "Transaction item is invalid. Will retry.");
                    callback.onComplete(); // ← call so retry can continue
                }
            }

            @Override
            public void onError(String error) {
                logError(TAG, "API error. Retry count: " + retryCount);
                callback.onComplete(); // ← always call so retry chain continues
            }
        });
    }


    /*If payment data is not received from razor pay api then After 5 minute update transaction as a time out*/
    private void handleTransactionTimeout(JSONObject paymentObject) {
        logError(TAG, "Handling transaction timeout...");

        retryHandler.removeCallbacks(retryRunnable);
        isTransactionCompleted = true;

        runOnUiThread(() ->
                Toast.makeText(PayWithQrActivity.this, "We didn't receive a payment", Toast.LENGTH_LONG).show()
        );

        saveTransactionAsATimeOut(paymentObject);
        goToHomeScreen();
    }


    /// Once confirmed by razor pay...Update into database as Failed and start for milking
    private void processValidTransactionByCheckRazorpayAPICall(RazorpayQrPaymentResponse response, String qrCodeId) {
        if (qrCodeTimeoutHandler != null && qrCodeTimeoutRunnable != null) {
            qrCodeTimeoutHandler.removeCallbacks(qrCodeTimeoutRunnable);
        }

        if (dialog.get() != null && dialog.get().isShowing()) {
            dialog.get().dismiss();
        }

        if (response.getItems().get(0).getAmount() != null) {
            String payCodeId = response.getItems().get(0).getId();
            float amount = Float.parseFloat(response.getItems().get(0).getAmount().toString());
            double amt = amount / 100;

            preferencesManager.save(Constants.PaidAmt, amt);

            /// Get transaction json from shared preference
            String transactionJson = (preferencesManager.get(Constants.SavedTransaction, "")).toString();

            if (!transactionJson.isEmpty()) {

                SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

                String transactionDate = dateFormatter.format(System.currentTimeMillis());
                String transactionTime = timeFormatter.format(System.currentTimeMillis());

                TransactionEntity transactionEntity = new Gson().fromJson(transactionJson, TransactionEntity.class);
                logError(TAG + "PAYMENT DONE transactionJson ", transactionJson);

                transactionEntity.setTransactionDate(transactionDate);
                transactionEntity.setTransactionTime(transactionTime);
                transactionEntity.setAmount(amt);
                transactionEntity.setBankTransactionNo(payCodeId);
                updateIntoDatabaseAfterPaymentDone(transactionEntity);
            } else {
               // insertIntoDatabaseAfterPaymentDone(payCodeId, amt);
            }


        }

        stopService(new Intent(PayWithQrActivity.this, PaymentStatusService.class));
    }


    /*Save transaction after QR Code generate*/
    private void saveTransactionAsAQRCodeGenerate(JSONObject paymentObject) {

        new Thread(() -> {
            try {
                SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

                String date = dateFormatter.format(System.currentTimeMillis());
                String time = timeFormatter.format(System.currentTimeMillis());
                double amount = Double.parseDouble(paymentObject.get("amount").toString()) / 100;

                TransactionDao transactionDao = AppDatabase.getInstance(PayWithQrActivity.this).transactionDao();
                SharedPreferencesManager preferencesManager = SharedPreferencesManager.getInstance(getApplicationContext());

                TransactionEntity transaction = new TransactionEntity();
                transaction.setUserName("");
                transaction.setPassword(""); // base64 for 'Admin'
                transaction.setTransactionType("ONLINE");
                transaction.setBankTransactionNo("");
                transaction.setTransactionDate(date);
                transaction.setTransactionTime(time);

                transaction.setAmount(amount);
                transaction.setVolume(0);


                // Set extra fields
                transaction.setMilkPrice((preferencesManager.get(MilkBasePrice, "")).toString());
                transaction.setMilkTemperature("111");
                transaction.setTransactionStatus("QR GENERATED");
                transaction.setUpiId(qrCodeId);
                transaction.setMachineId((preferencesManager.get(MachineId, "")).toString());

                /// Yet uploaded to server flag set.. Need to change. Only it will insert into Local DB
                transaction.setUploadToServer(0);

                // Add field on 28-6-2025
                transaction.setTransactionStartTime(transactionStartDate +" || "+transactionStartTime);
                /// 28-6-2025 - in created by...We save current time. So we can get QR Code generated time
                transaction.setQrCreatedOn(date +" || "+time);

                String uniqueId = generateSafeUniqueTransactionId(transactionDao);
                transaction.setUniqueTransactionId(uniqueId);

                // Insert into database
                long transactionId = transactionDao.insert(transaction);
                transaction.setId(transactionId);

                logError(TAG, "Insert Transaction qr code" + new Gson().toJson(transaction));

                preferencesManager.save(Constants.SavedTransaction, new Gson().toJson(transaction));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

    }


    /*Save transaction if time is out*/
    private void saveTransactionAsATimeOut(JSONObject paymentObject) {
        new Thread(() -> {
            try {
                String date = new SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis());
                String time = new SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis());

                TransactionDao transactionDao = AppDatabase.getInstance(PayWithQrActivity.this).transactionDao();

                /// Get transaction json from shared preference
                String transactionJson = (preferencesManager.get(Constants.SavedTransaction, "")).toString();

                if (!transactionJson.isEmpty()) {
                    TransactionEntity transactionEntity = new Gson().fromJson(transactionJson, TransactionEntity.class);
                    Constants.updateTransaction(
                            getApplicationContext(),
                            transactionDao,
                            transactionEntity.getId(),
                            "TIME OUT",
                            0,
                            transactionEntity.getMilkTemperature(),
                            transactionEntity
                    );
                } else if (paymentObject.has("amount")) {
                    double amount = Double.parseDouble(paymentObject.get("amount").toString()) / 100;
                    long transactionId = Constants.insertTransaction(
                            getApplicationContext(), transactionDao, "ONLINE", "", date, time,
                            amount, "TIME OUT", qrCodeId, 0, "111",
                            transactionStartDate,
                            transactionStartTime
                    );
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    public Double extractNumericValueFromString(String input) {
        Pattern pattern = Pattern.compile("(\\d+(\\.\\d+)?)\\s*(\\(.*\\))?");
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            String numericValueString = matcher.group(1);
            double numericValue = Double.parseDouble(numericValueString);
            return numericValue;
        } else {

            btnBackToHome.setVisibility(View.VISIBLE);
            gv_CurrencyLiters.setVisibility(View.VISIBLE);
            Constants.showAlertDialog(PayWithQrActivity.this, "Error", "No Valid Selection : " + input);
/*                new IllegalArgumentException("No numeric value found in volume string: " + input);
             new IllegalArgumentException("No numeric value found in volume string: " + input);*/

        }
        /*Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return Double.parseDouble(matcher.group());
        } else {
            throw new IllegalArgumentException("No numeric value found in input string: " + input);
        }*/
        return null;
    }


    /*Show Qr code image in dialog*/
    private Dialog showQRCodeDialog(String imageUrl) {
        // Inflate the dialog layout
        Dialog qrCodeDialog = new Dialog(this, android.R.style.Theme_Light_NoTitleBar);
        qrCodeDialog.setContentView(R.layout.dialog_qr_code);

        // Find the ImageView in the layout
        ImageView imageViewQRCode = qrCodeDialog.findViewById(R.id.ivQRCode);

        // Set the QR code bitmap to the ImageView
        Glide.with(this).load(imageUrl).timeout(10000).into(imageViewQRCode);

        qrCodeDialog.setCancelable(false);

        // Hide system UI (navigation + status bars)
        Window window = qrCodeDialog.getWindow();
        if (window != null) {
            View decorView = window.getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }

        return qrCodeDialog;
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

    @Override
    protected void onStart() {
        super.onStart();



        /*Intent serviceIntent = new Intent(this, PaymentStatusService.class);
        startService(serviceIntent);*/
    }

    @Override
    protected void onStop() {
        super.onStop();
//        Intent serviceIntent = new Intent(this, PaymentStatusService.class);
//        stopService(serviceIntent);
//
//        try {
//            if (paymentStatusReceiver != null) {
//                unregisterReceiver(paymentStatusReceiver);
//            }
//        } catch (IllegalArgumentException e) {
//            logError(TAG, "usbPermissionReceiver was already unregistered: " + e.getMessage());
//        }

//        try {
//            if (batteryReceiver != null) {
//                logError(TAG + "unregisterReceiver", "batteryReceiver");
//                unregisterReceiver(batteryReceiver);
//            }
//        } catch (IllegalArgumentException e) {
//            logError(TAG, "batteryReceiver was already unregistered: " + e.getMessage());
//        }


//        unregisterReceiver(paymentStatusReceiver);
//        unregisterReceiver(batteryReceiver);


    }

    @Override
    public void onPaymentSuccess(String s, PaymentData paymentData) {
        logError(TAG, "onPaymentSuccess: ");
    }

    @Override
    public void onPaymentError(int i, String s, PaymentData paymentData) {
        logError(TAG, "onPaymentError: " + paymentData.getPaymentId());
    }


    /*
     * When payment is done. Send for Vending the milk*/
    public void sendForMilkVending(double amt, String payCodeId, TransactionEntity transaction) {
        milkDispensingDialog = new LottieDialog(PayWithQrActivity.this);
        try {

            logError("Milk base price ", preferencesManager.get(Constants.MilkBasePrice, "0.0").toString());

            if (!isFinishing() && !isDestroyed() && milkDispensingDialog != null) {
                milkDispensingDialog.show();
            }

            // Fetch preferences
            ResponseTempStatus responseTempStatus = new Gson().fromJson(preferencesManager.get(Constants.ResponseTempStatus, "").toString(), ResponseTempStatus.class);
            float milkSellingPrice = Float.parseFloat(preferencesManager.get(Constants.MilkBasePrice, "0.0").toString());
            float offSet = Float.parseFloat(preferencesManager.get(Constants.TemperatureOffSet, "0.0").toString());
            float milkDensity = Float.parseFloat(preferencesManager.get(Constants.MilkDensityPref, "0.0").toString());
            float milkSetTemperature = Float.parseFloat(preferencesManager.get(Constants.TemperatureSet, "0.0").toString());

            // Calculate weight and current temperature
            float weight = (float) ((amt / milkSellingPrice) * milkDensity);
            double currentSavedTemp = responseTempStatus.getTemperature() / 10.0;
            float currentTemperature = (float) (currentSavedTemp + offSet);

            transaction.setMilkTemperature(String.valueOf(currentTemperature));
            preferencesManager.save(Constants.SavedTransaction, new Gson().toJson(transaction));

            // Prepare data to send to device
            SendToDevice sendToDevice = new SendToDevice();
            sendToDevice.setWeight(weight);
            sendToDevice.setStatus(true);
            sendToDevice.setCurtemperature(currentTemperature);
            sendToDevice.setSettemperature(milkSetTemperature);
            //  logError(TAG + " milkSetTemperature sendForMilkVending", String.valueOf(milkSetTemperature));

            Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
            //  logError(TAG, "QR_PAYMENT: SEND COMMAND " + gson.toJson(sendToDevice));

            /// Check that usb serial is not null
            if (usbSerialCommunication != null && usbSerialCommunication.connected) {

                shouldContinueSending = true;
//                usbSerialCommunication.sendData(commandJson);

                if (!isDataSent) {
                    isDataSent = true;

                    try {
                        Constants.saveLogs(getApplicationContext(),
                                "Sent Weight - " + sendToDevice.getWeight() + " TransactionId: " + transaction.getUniqueTransactionId(),
                                KEY_WEIGHT

                        );
                    } catch (Exception e) {
                        logError("PaymentLog", "Logging failed: ${e.message}");
                        // Don't crash, just log the error silently
                    }


                }

                sendDataRepeatedly(gson.toJson(sendToDevice));

                isCommandSent = true;

                usbSerialCommunication.setReadDataListener(data -> {
                    handleSerialReadingResponse(
                            data, milkDispensingDialog, amt, payCodeId,
                            timeoutHandler, timeoutRunnable,
                            milkDensity, currentTemperature, transaction,
                            weight
                    );
                });
            } else {
                logError(TAG + "UsbSerialCommunication", "usbSerialCommunication is null");

                /// Here if usbSerialCommunication getting null then dismiss the dialog.
                /// And open error message that Something went wrong. Please try again letter
                if (milkDispensingDialog != null && milkDispensingDialog.isShowing()) {
                    milkDispensingDialog.dismiss();
                }

                /// Here if database operation is not started then start.
                // So api will not call again and again
                if (!isDatabaseOperationStarted) {
                    isDatabaseOperationStarted = true;

                    try {
                        Constants.saveLogs(getApplicationContext(), "Dispensation Not Started", KEY_USB);
                    } catch (Exception e) {
                        logError("PaymentLog", "Logging failed: ${e.message}");
                        // Don't crash, just log the error silently
                    }


                    updateTransactionIfUSBSerialCommunicationLost(transaction);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /// Send data continues..And get data from on read... It will stop
    private void sendDataRepeatedly(String commandJson) {

        Runnable sendCommandRunnable = new Runnable() {
            @Override
            public void run() {

                logError("sendCommandRunnable", "sendCommandRunnable");

                if (shouldContinueSending) {
                    usbSerialCommunication.sendData(commandJson);

                    if (isStopConditionMet) {
                        shouldContinueSending = false;
                        return;
                    }

                    handlerForSendData.postDelayed(this, SEND_INTERVAL_MS);
                }
            }
        };

        handlerForSendData.postDelayed(sendCommandRunnable, SEND_INTERVAL_MS);
    }


    /*If 5 minutes done and status is not getting as a true.
    Transaction will be added as a FAILED*/
    private void handleMilkSendingTimeout(LottieDialog lottieDialog, double amt, float volume, TransactionEntity transaction) {
        if (PayWithQrActivity.this.isFinishing() || PayWithQrActivity.this.isDestroyed()) {
            return; // Activity is no longer valid, skip dismiss
        }

        shouldContinueSending = false;

        if (handlerForSendData != null) {
            handlerForSendData.removeCallbacksAndMessages(null);
        }

        if (lottieDialog != null && lottieDialog.isShowing()) {
            try {
                lottieDialog.dismiss();
            } catch (Exception e) {
                e.printStackTrace(); // This is a last-resort guard
            }
        }

        new Thread(() -> {
            try {
                TransactionDao transactionDao = AppDatabase.getInstance(PayWithQrActivity.this).transactionDao();
                Constants.updateTransaction(getApplicationContext(), transactionDao, transaction.getId(), "FAILED", 0, transaction.getMilkTemperature(), transaction);

                logError(TAG + "Time is out", "After 5 minutes");

                if (!isFinishing() && !isDestroyed()) {
                    runOnUiThread(this::goToHomeScreen);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    /*Read Listener Response*/
    private void handleSerialReadingResponse(String data, LottieDialog lottieDialog, double amt, String payCodeId, Handler timeoutHandler, Runnable timeoutRunnable, double milkDensity, float milkTemperature, TransactionEntity transaction, float setWeight) {
        logError("TAG", "onReadData: " + data);

        /// If it contains status key
        //
        if (data.contains("status")) {

            if (!isStopConditionMet) {

                try {
                    Constants.saveLogs(getApplicationContext(),
                            "Pump Started - " + "  TransactionId: " + transaction.getUniqueTransactionId(),
                            KEY_WEIGHT
                    );
                } catch (Exception e) {
                    logError("PaymentLog", "Logging failed: ${e.message}");
                    // Don't crash, just log the error silently
                }


                isStopConditionMet = true;
            }


            logError("TAG", "onReadData: if status get" + data);

            ResponseMilkDispense milkDispense = new Gson().fromJson(data, ResponseMilkDispense.class);
            logError(TAG + " data after get status ", data);
            /// If status is true then show success dialog
            // Here if milkDispense.getStatus == true. timeOutHandler will be stop
            /// Here true status getting two times.
            // So put condition that if lottieDialog is showing that time only goes to this condition
            if (milkDispense != null && milkDispense.getStatus() && lottieDialog.isShowing() && !transactionCompleted) {

                // Set the flag immediately to prevent any race conditions
                transactionCompleted = true;

                /// Here we calculate volume of milk
                float volumeOfMilk = (float) ((milkDispense.getCurrentWeight()) / milkDensity);

                logError(TAG + " volumeOfMilk", String.valueOf(volumeOfMilk));

                /// Here check that if volume is negative then, get weight as a set weight
                if (volumeOfMilk < 0) {

                    logError("VOLUME OF MILK", "IS MINUS");

                    volumeOfMilk = (float) (setWeight / milkDensity);
                }

                /// when status get as a true, timeOutHandler removed here
                if (timeoutHandler != null && timeoutRunnable != null) {
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                }

                isCommandSent = false;

//                tvProcessing.setText("Thank You..");

                /// Here if database operation is not started then start.
                // So api will not call again and again
                if (!isDatabaseOperationStarted) {
                    isDatabaseOperationStarted = true;

                    try {
                        Constants.saveLogs(getApplicationContext(),
                                "Get Weight - " + milkDispense.getCurrentWeight() + " TransactionId: " + transaction.getUniqueTransactionId(),
                                KEY_WEIGHT
                        );
                    } catch (Exception e) {
                        logError("PaymentLog", "Logging failed: ${e.message}");
                        // Don't crash, just log the error silently
                    }

                    updateDataInDatabaseWhenProcessDone(amt, payCodeId, volumeOfMilk, milkTemperature, transaction, milkDispense.getDoorstatus());
                }

            }
        }
    }


    /**
     * If payment is done and electricity is lost, show a failure dialog.
     */
    private void insertTransactionIfElectricityLost(float amt, Payment payment, float volumeOfMilk) {
        runOnUiThread(() -> Constants.showAcceptDialog(
                PayWithQrActivity.this,
                "Error",
                "Lost Electricity Connection!! Please try after some time.",
                (dialog, which) -> handleTransactionInsert(dialog, amt, volumeOfMilk),
                (dialog, which) -> handleTransactionInsert(dialog, amt, volumeOfMilk)
        ));
    }


    private void handleTransactionInsert(DialogInterface dialog, float amt, float volumeOfMilk) {
        new Thread(() -> {
            try {
                String dateFormat = "yyyy-MM-dd";
                String timeFormat = "HH:mm:ss";
                SimpleDateFormat dateFormatter = new SimpleDateFormat(dateFormat, Locale.getDefault());
                SimpleDateFormat timeFormatter = new SimpleDateFormat(timeFormat, Locale.getDefault());

                String date = dateFormatter.format(System.currentTimeMillis());
                String time = timeFormatter.format(System.currentTimeMillis());

                TransactionDao transactionDao = AppDatabase.getInstance(PayWithQrActivity.this).transactionDao();
                long transactionId = Constants.insertTransaction(
                        getApplicationContext(),
                        transactionDao,
                        "ONLINE",
                        "",
                        date,
                        time,
                        amt,
                        "FAILED",
                        qrCodeId,
                        volumeOfMilk,
                        "111",
                        transactionStartDate,
                        transactionStartTime
                );

                runOnUiThread(() -> {
                    logError(TAG, "Transaction ID: " + transactionId);
                    logError(TAG, "All Transactions: " + new Gson().toJson(transactionDao.getAllTransactions()));
                    dialog.dismiss();
                    goToHomeScreen();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    private void handleTransactionInsertOnBackPressed(float amt, float volumeOfMilk) {
        new Thread(() -> {
            try {
                String dateFormat = "yyyy-MM-dd";
                String timeFormat = "HH:mm:ss";
                SimpleDateFormat dateFormatter = new SimpleDateFormat(dateFormat, Locale.getDefault());
                SimpleDateFormat timeFormatter = new SimpleDateFormat(timeFormat, Locale.getDefault());

                String date = dateFormatter.format(System.currentTimeMillis());
                String time = timeFormatter.format(System.currentTimeMillis());

                TransactionDao transactionDao = AppDatabase.getInstance(PayWithQrActivity.this).transactionDao();
                long transactionId = Constants.insertTransaction(
                        getApplicationContext(),
                        transactionDao,
                        "ONLINE",
                        "",
                        date,
                        time,
                        amt,
                        "FAILED",
                        qrCodeId,
                        volumeOfMilk,
                        "111",
                        transactionStartDate,
                        transactionStartTime
                );


            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    /**
     * If payment is done and electricity is lost after inserting the transaction,
     * show a failure dialog and update the transaction accordingly.
     */
    private void updateTransactionIfElectricityLost(TransactionEntity transactionEntity) {
        runOnUiThread(() -> Constants.showAcceptDialog(
                PayWithQrActivity.this,
                "Error",
                "Lost Electricity Connection!! Please try after some time.",
                (dialog, which) -> handleFailedUpdateInDatabase(dialog, transactionEntity),
                (dialog, which) -> handleFailedUpdateInDatabase(dialog, transactionEntity)
        ));
    }


    /**
     * If payment is done and Milk dispense not initiate due to serial communication lost,
     * show a failure dialog and update the transaction accordingly.
     */
    private void updateTransactionIfUSBSerialCommunicationLost(TransactionEntity transactionEntity) {
        runOnUiThread(() -> Constants.showDispenseErrorMessageDialog(
                PayWithQrActivity.this,
                "Error",
                "Sorry. Something Went wrong!! Please try after some time.",
                (dialog, which) -> handleFailedUpdateInDatabase(dialog, transactionEntity)
        ));
    }

    /*After payment done insert transaction as a failed..If transaction is not inserted.
       And then send for milking*/
    private void insertIntoDatabaseAfterPaymentDone(String payCodeId, Double amt) {
        new Thread(() -> {
            try {
                SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

                String date = dateFormatter.format(System.currentTimeMillis());
                String time = timeFormatter.format(System.currentTimeMillis());

                TransactionDao transactionDao = AppDatabase.getInstance(PayWithQrActivity.this).transactionDao();
                SharedPreferencesManager preferencesManager = SharedPreferencesManager.getInstance(getApplicationContext());

                TransactionEntity transaction = new TransactionEntity();
                transaction.setUserName("");
                transaction.setPassword(""); // base64 for 'Admin'
                transaction.setTransactionType("ONLINE");
                transaction.setBankTransactionNo(payCodeId);
                transaction.setTransactionDate(date);
                transaction.setTransactionTime(time);
                transaction.setAmount(amt);
                transaction.setVolume(0);

                // Set extra fields
                transaction.setMilkPrice((preferencesManager.get(MilkBasePrice, "")).toString());
                transaction.setMilkTemperature("111");
                transaction.setTransactionStatus("FAILED");
                transaction.setUpiId(qrCodeId);
                transaction.setMachineId((preferencesManager.get(MachineId, "")).toString());

                /// Yet uploaded to server flag set.. Need to change. Only it will insert into Local DB
                transaction.setUploadToServer(0);

                String uniqueId = generateSafeUniqueTransactionId(transactionDao);
                transaction.setUniqueTransactionId(uniqueId);

                // Insert into database
                long transactionId = transactionDao.insert(transaction);
                transaction.setId(transactionId);

                logError("save karti ", new Gson().toJson(transaction));

                preferencesManager.save(Constants.SavedTransaction, new Gson().toJson(transaction));

                runOnUiThread(() -> {

                    //    Toast.makeText(PayWithQrActivity.this, "Transaction id : " + (String.valueOf(transactionId)), Toast.LENGTH_SHORT).show();

                    if (!isMilkVendingStarted) {
                        isMilkVendingStarted = true;

                        /// Here after 3 minute if status is not getting as a true.
                        // Dialog will be close and transaction will be add in the database as a TIME OUT
                        timeoutHandler = new Handler(Looper.getMainLooper());
                        timeoutRunnable = () -> handleMilkSendingTimeout(milkDispensingDialog, amt, 0, transaction);

                        // Post the Runnable with a delay
                        timeoutHandler.postDelayed(timeoutRunnable, 3 * 60 * 1000); // 3 minutes

                        sendForMilkVending(amt, payCodeId, transaction);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    /*After payment done update transaction done.
    And then send for milking*/
    private void updateIntoDatabaseAfterPaymentDone(TransactionEntity transactionEntity) {
        new Thread(() -> {
            try {
                TransactionDao transactionDao = AppDatabase.getInstance(PayWithQrActivity.this).transactionDao();

                Constants.updateTransactionAfterPaymentDone(
                        getApplicationContext(),
                        transactionDao,
                        transactionEntity.getId(),
                        "FAILED",
                        0,
                        transactionEntity.getMilkTemperature(),
                        transactionEntity
                );

                preferencesManager.save(Constants.SavedTransaction, new Gson().toJson(transactionEntity));

                runOnUiThread(() -> {

                    //    Toast.makeText(PayWithQrActivity.this, "Transaction id : " + (String.valueOf(transactionId)), Toast.LENGTH_SHORT).show();

                    if (!isMilkVendingStarted) {
                        isMilkVendingStarted = true;

                        /// Here after 3 minute if status is not getting as a true.
                        // Dialog will be close and transaction will be add in the database as a TIME OUT
                        timeoutHandler = new Handler(Looper.getMainLooper());
                        timeoutRunnable = () -> handleMilkSendingTimeout(milkDispensingDialog, transactionEntity.getAmount(), 0, transactionEntity);

                        // Post the Runnable with a delay
                        timeoutHandler.postDelayed(timeoutRunnable, 3 * 60 * 1000); // 3 minutes

                        sendForMilkVending(transactionEntity.getAmount(), transactionEntity.getUpiId(), transactionEntity);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleFailedUpdateInDatabase(DialogInterface dialog, TransactionEntity transactionEntity) {
        new Thread(() -> {
            try {
                TransactionDao transactionDao = AppDatabase.getInstance(PayWithQrActivity.this).transactionDao();

                Constants.updateTransaction(
                        getApplicationContext(),
                        transactionDao,
                        transactionEntity.getId(),
                        "FAILED",
                        0,
                        transactionEntity.getMilkTemperature(),
                        transactionEntity
                );

                runOnUiThread(() -> {
                    logError(TAG, "Updated Transaction: " + new Gson().toJson(transactionDao.getAllTransactions()));
                    dialog.dismiss();
                    goToHomeScreen();
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    private void handleFailedUpdateInDatabaseOnBackPressed(TransactionEntity transactionEntity) {
        new Thread(() -> {
            try {
                TransactionDao transactionDao = AppDatabase.getInstance(PayWithQrActivity.this).transactionDao();
                Constants.updateTransaction(
                        getApplicationContext(),
                        transactionDao,
                        transactionEntity.getId(),
                        "FAILED",
                        0,
                        transactionEntity.getMilkTemperature(),
                        transactionEntity
                );

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    /// If milk is send to the customer. Show process done dialog
    public void showAndProcessDoneDialog(double amt, float volumeOfMilk, float milkTemperature, String payCodeId) {

        if (isFinishing() || isDestroyed()) return; // Prevent dialog if activity is finishing

        AlertDialog.Builder builder = new AlertDialog.Builder(PayWithQrActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_lottie, null);

        LottieAnimationView lottieAnimationView = view.findViewById(R.id.lottieAnimationView);
        LottieAnimationView lottieAnimationViewDone = view.findViewById(R.id.lottieAnimationViewDone);
        TextView tvProgressDialog = view.findViewById(R.id.tvProgressDialog);
        MaterialButton btnDone = view.findViewById(R.id.doneButton);
        TextView tvProcessDoneText = view.findViewById(R.id.tvProcessDoneText);
        TextView tvDispenseVolume = view.findViewById(R.id.tvDispenseVolume);
        TextView tvOpenTheDoor = view.findViewById(R.id.tvOpenTheDoor);

        btnDone.setVisibility(View.VISIBLE);
        lottieAnimationView.setVisibility(View.GONE);
        lottieAnimationViewDone.setVisibility(View.VISIBLE);
        tvProcessDoneText.setVisibility(View.VISIBLE);
        tvOpenTheDoor.setVisibility(View.VISIBLE);
        tvProgressDialog.setVisibility(View.GONE);
        tvDispenseVolume.setVisibility(View.GONE);

        float truncatedValueOfMilkVolume = Float.parseFloat(String.format("%.2f", volumeOfMilk));
        tvDispenseVolume.setText(getString(R.string.dispense_volume) + " " + truncatedValueOfMilkVolume + " L");

        lottieAnimationViewDone.setAnimation(R.raw.process_done);
        lottieAnimationViewDone.setRepeatMode(LottieDrawable.RESTART);
        lottieAnimationViewDone.playAnimation();

        builder.setView(view);
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();

        handler = new Handler();
        Long screenTimeOut = Long.parseLong(preferencesManager.get(ScreenTimeOutPref, "0.0").toString());

        runnable = () -> {
            dialog.dismiss();

            runOnUiThread(() -> goToHomeScreen());
        };

        handler.postDelayed(runnable, screenTimeOut * 1000);

        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (handler != null && runnable != null) {
                    handler.removeCallbacks(runnable);
                }

                dialog.dismiss();
                runOnUiThread(() -> goToHomeScreen());


            }
        });
    }


    /*
     * Here we are getting time out from shared preference
     * And after that screen automatically off
     * */
    void screenTimeOut() {
        preferencesManager = SharedPreferencesManager.getInstance(getApplicationContext());
        logError("timeOut", preferencesManager.get(ScreenTimeOutPref, "0").toString());

        Long screenTimeOut = Long.parseLong(preferencesManager.get(ScreenTimeOutPref, "0.0").toString());

        // Define the Runnable task
        runnable = () -> {
            // Task to execute after delay
            goToHomeScreen(); // Closes the current activity
        };

        // Post the Runnable with a 15-second delay
        handler.postDelayed(runnable, screenTimeOut * 1000);
    }


    /*
     * It will redirect to the home screen
     * */
    void goToHomeScreen() {

        /// Here if function is already called then do not call again
        if (isNavigated) return; // Prevent double execution
        isNavigated = true;

        /// Here if handler and runnable remove
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }


        // Remove the Runnable from the Handler to avoid memory leaks
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }


        // Remove the Runnable from the Handler to avoid memory leaks
        if (qrCodeTimeoutHandler != null && qrCodeTimeoutRunnable != null) {
            qrCodeTimeoutHandler.removeCallbacks(qrCodeTimeoutRunnable);
        }


        // Simulate finishing and sending data
        Intent resultIntent = new Intent();
        resultIntent.putExtra(FromScreen, ScreenEnum.PAY_WITH_QR.ordinal());
        setResult(RESULT_OK, resultIntent); // Set the result to be OK
        finish(); // Finish the activity

//        Intent intent = new Intent(PayWithQrActivity.this, MainActivity.class);
//        // Clear all previous activities
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        startActivity(intent);

    }

    /// When process is completed. Data will be updated into database
    void updateDataInDatabaseWhenProcessDone(double amt, String payCodeId, float volumeOfMilk, float milkTemperature, TransactionEntity transaction, Boolean doorStatus) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    TransactionDao transactionDao = AppDatabase.getInstance(PayWithQrActivity.this).transactionDao();
                    /// Here I convert volume of milk into string and set 3 digits after dot(.)
                    float truncatedValueOfMilkVolume = Float.parseFloat(String.format("%.2f", volumeOfMilk));

                    /// Here I convert temperature of milk into string and set 3 digits after dot(.)
                    String strMilkTemperature = String.format("%.3f", milkTemperature);


                    Constants.updateTransaction(getApplicationContext(), transactionDao, transaction.getId(), doorStatus ? "DOOR OPEN" : "SUCCESS", truncatedValueOfMilkVolume, strMilkTemperature, transaction);

                    // Now show dialog on UI thread
                    runOnUiThread(() -> {
                        logError(TAG, "onCreate: " + new Gson().toJson(transactionDao.getAllTransactions()));
                        tvProcessing.setText("PROCESSING...");


                        // Delay the call to show dialog by 5 seconds
                        /// Discussed on 21-6-2025
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {

                            /// Here dismiss milk dispense lottie dialog after 5 seconds
                            if (milkDispensingDialog != null && milkDispensingDialog.isShowing()) {
                                milkDispensingDialog.dismiss();
                            }

                            showAndProcessDoneDialog(amt, volumeOfMilk, milkTemperature, payCodeId);
                        }, 5000); // 5000 ms = 5 seconds
                    });


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    @Override
    protected void onDestroy() {


        // Move this here from onStop()
        try {
            if (paymentStatusReceiver != null) {
                unregisterReceiver(paymentStatusReceiver);
            }
        } catch (IllegalArgumentException e) {
            logError(TAG, "Already unregistered: " + e.getMessage());
        }


        shouldContinueSending = false;

        /// Here if handler and runnable remove
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }

        if (handlerForSendData != null) {
            handlerForSendData.removeCallbacksAndMessages(null);
        }

        // Remove the Runnable from the Handler to avoid memory leaks
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }


        // Remove the Runnable from the Handler to avoid memory leaks
        if (qrCodeTimeoutHandler != null && qrCodeTimeoutRunnable != null) {
            qrCodeTimeoutHandler.removeCallbacks(qrCodeTimeoutRunnable);
        }

        if (retryHandler != null && retryRunnable != null) {
            retryHandler.removeCallbacks(retryRunnable);
        }


        // Also clear dialog safely
        if (dialog.get() != null && dialog.get().isShowing()) {
            try {
                dialog.get().dismiss();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        super.onDestroy();
    }


    /// If volume amount is more than 5 liters.
    // It will show error tha vending volume can not be more than 5 liters
    private void showAlertExceedLimit() {
        // Create AlertDialog.Builder instance
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Exceed Limit");
        builder.setMessage("Vending volume can not be more than 5 liters.");

        // Positive button
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                goToHomeScreen();
            }
        });

        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.show();
    }


    private void logError(String tag, String message) {
        // Log.e(tag, message);
    }


    @Override
    public void onBackPressed() {
        Log.e("BackButton", "User pressed the back button!");
        Constants.saveLogs(getApplicationContext(), "Back Pressed", "Back Pressed");

        Object transactionObj = preferencesManager.get(Constants.SavedTransaction, "");
        String transactionJson = transactionObj != null ? transactionObj.toString() : "";

        Object paymentObj = preferencesManager.get(Constants.PaymentReceived, "");
        String paymentJson = paymentObj != null ? paymentObj.toString() : "";

        if (!transactionJson.isEmpty()) {
            TransactionEntity transactionEntity = new Gson().fromJson(transactionJson, TransactionEntity.class);
            logError("ElectricityLost transactionJson ", transactionJson);
            handleFailedUpdateInDatabaseOnBackPressed(transactionEntity);

        } else if (!paymentJson.isEmpty()) {
            Payment payment = new Gson().fromJson(paymentJson, Payment.class);
            if (payment != null && payment.get("amount") != null) {
                try {
                    float amount = Float.parseFloat(payment.get("amount").toString());
                    float amt = amount / 100;
                    handleTransactionInsertOnBackPressed(amt, 0);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }


        Intent resultIntent = new Intent();
        resultIntent.putExtra(FromScreen, ScreenEnum.PAY_WITH_QR.ordinal());
        setResult(RESULT_OK, resultIntent); // Set the result to be OK

        super.onBackPressed();  // Call this only after your logic is done
    }


    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        registerReceiver(usbReceiver, filter);

        logError(TAG, "on resume called");

    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(usbReceiver);
    }
}
