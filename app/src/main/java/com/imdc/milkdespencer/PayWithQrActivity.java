package com.imdc.milkdespencer;

import static com.imdc.milkdespencer.CashCollectorActivity.getInstance;
import static com.imdc.milkdespencer.CashCollectorActivity.milkSetTemperature;
import static com.imdc.milkdespencer.common.Constants.ScreenTimeOutPref;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.imdc.milkdespencer.common.Constants;
import com.imdc.milkdespencer.common.LottieDialog;
import com.imdc.milkdespencer.common.SharedPreferencesManager;
import com.imdc.milkdespencer.common.UsbSerialCommunication;
import com.imdc.milkdespencer.models.ResponseMilkDispense;
import com.imdc.milkdespencer.models.ResponseTempStatus;
import com.imdc.milkdespencer.models.SendToDevice;
import com.imdc.milkdespencer.network.RetrofitClient;
import com.imdc.milkdespencer.roomdb.AppDatabase;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PayWithQrActivity extends AppCompatActivity implements PaymentResultWithDataListener {
    static SharedPreferencesManager preferencesManager;
    private static UsbSerialCommunication usbSerialCommunication;
    private final String TAG = PayWithQrActivity.class.getSimpleName();
    private final int previousSelectionAMT = 0;
    private final int previousSelectionLites = 0;
    boolean isCharging;

    private Handler handler = new Handler(); // Create a Handler instance
    private Runnable runnable; // Declare the Runnable

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get the current battery status
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

            // Check if the device is charging
            isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL);

            if (isCharging) {


            } else {
                Constants.saveLogs(PayWithQrActivity.this, "Lost Electricity");


                Payment payment = new Gson().fromJson(preferencesManager.get(Constants.PaymentReceived, "").toString(), Payment.class);
                if (payment != null && payment.get("amount") != null) {
                    float amount = Float.parseFloat(payment.get("amount").toString());
                    float amt = amount / 100;
                    showFailedProcessDoneDialog(amt, payment);
                }
            }
        }
    };
    JSONObject paymentObject = new JSONObject();
    boolean isCommandSent = false;
    AtomicReference<Dialog> dialog = new AtomicReference<>();
    private final BroadcastReceiver paymentStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {


            if (intent.getAction().equals("payment_status_action")) {
                String paymentStatusJson = intent.getStringExtra("payment_status");
                Log.e("TAG", "onReceive: " + paymentStatusJson);

                if (paymentStatusJson != null && !paymentStatusJson.isEmpty()) {
                    Payment payment = new Gson().fromJson(paymentStatusJson, Payment.class);
                    Log.e("TAG", "onReceive:payment " + paymentStatusJson);
                    if (payment != null) {
                        if (dialog.get() != null) {
                            Log.e("TAG", "onReceive:DIa " + dialog.get().isShowing());
                            dialog.get().dismiss();
                        }

                        if (payment.has("amount")) {
                            float amount = Float.parseFloat(payment.get("amount").toString());
                            float amt = amount / 100;
                            preferencesManager.save(Constants.PaymentReceived, new Gson().toJson(payment));
                            preferencesManager.save(Constants.PaidAmt, amt);
                            sendForMilkVending(amt, payment);
                        }
                        Intent serviceIntent = new Intent(PayWithQrActivity.this, PaymentStatusService.class);
                        stopService(serviceIntent);
                    }

                }
                // Process the payment status JSON received from the service
            }
        }
    };
    RetrofitClient retrofitClient;
    RazorpayClient razorpay;
    LottieDialog lottieDialog;
    GridView gv_CurrencyLiters;
    TabLayout tabLayout;

    private MaterialButton btnGenerateQr;
    private MaterialButton btnBackToHome;

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_pay_with_qr);


        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        IntentFilter battertyFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, battertyFilter);

        screenClose();

        usbSerialCommunication = new UsbSerialCommunication(getApplicationContext());
        if (!usbSerialCommunication.connected) {
            usbSerialCommunication.connect();
            usbSerialCommunication.setBaudRate(115200);
        }

        btnGenerateQr = findViewById(R.id.btnGenerateQr);
        btnBackToHome = findViewById(R.id.btnBackToHome);
        tabLayout = findViewById(R.id.tabLayout);

        preferencesManager = SharedPreferencesManager.getInstance(getInstance());

        gv_CurrencyLiters = findViewById(R.id.gridViewCurrencyLiters);

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

                //   If User clicks on the grid item. Runnable should be close
                // Cancel the delayed task
                if (handler != null && runnable != null) {
                    handler.removeCallbacks(runnable);
                }


                String customerId = preferencesManager.get(Constants.RazorPayCustomerID, "").toString();
                if (customerId.isEmpty()) {
                    Constants.showAlertDialog(PayWithQrActivity.this, "Error", "Customer Id cannot be empty");
                    return;
                }


                if (gv_CurrencyLiters.getAdapter() instanceof SpnLitersAdapter) {
                    Log.e(TAG, "onItemSelected: " + gv_CurrencyLiters.getAdapter().getItem(position));
                    Double numericValueFromString = extractNumericValueFromString(gv_CurrencyLiters.getAdapter().getItem(position).toString());
                    String inputVal = numericValueFromString != null ? String.valueOf(numericValueFromString) : "0.0";

                    double ltrs = Double.parseDouble(inputVal);
                    double amt = Constants.calculateMilkWeight(ltrs, PayWithQrActivity.this);

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
                                executeGenerateQRCodeTask(paymentObject, customerId);
                            }
                        }, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
//                                Toast.makeText(PayWithQrActivity.this, "NO ", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (gv_CurrencyLiters.getAdapter() instanceof SpnCurrencyAdapter) {
                    double cost = Double.parseDouble(gv_CurrencyLiters.getAdapter().getItem(position).toString().replace("₹", ""));
                    //   Toast.makeText(PayWithQrActivity.this, "COST " + cost, Toast.LENGTH_SHORT).show();
                    double weight = Constants.calculateMilkAmount(cost, PayWithQrActivity.this);
                    String weightStr = weight > 0 && weight < 1 ? weight + " (Ml)." : weight + "(Ltr).";
                    try {
                        paymentObject.put("name", "Milk Vending Machine");
                        paymentObject.put("description", "Payment For Milk");
                        paymentObject.put("currency", "INR");
                        paymentObject.put("amount", cost * 100); // Amount in paise (e.g., 10000 paise = INR 100)
//                        paymentObject.put("amount", 100); // Amount in paise (e.g., 10000 paise = INR 100)
                        Constants.showAcceptDialog(PayWithQrActivity.this, "Please Confirm", "You need to pay the ₹" + cost + " for " + weightStr, (dialog, which) -> {
                            dialog.dismiss();
                            executeGenerateQRCodeTask(paymentObject, customerId);
                        }, (dialog, which) -> dialog.dismiss());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Log.e(TAG, "onItemSelected: " + gv_CurrencyLiters.getAdapter().getItem(position));
                }
            }
        });


        btnGenerateQr.setVisibility(View.GONE);
        btnGenerateQr.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("StaticFieldLeak")
            @Override
            public void onClick(View v) {
                int selectedId = tabLayout.getSelectedTabPosition();
//                String inputVal = tieInputVal.getText().toString();
                Double numericValueFromString = extractNumericValueFromString(gv_CurrencyLiters.getSelectedItem().toString());
                String inputVal = numericValueFromString != null ? String.valueOf(numericValueFromString) : "0.0";
                Log.e(TAG, "onClick: " + inputVal);
                if (selectedId == 0) {

                    double ltrs = Double.parseDouble(inputVal);
                    double amt = Constants.calculateMilkWeight(ltrs, PayWithQrActivity.this);

                    try {
                        paymentObject.put("name", "Milk Vending Machine");
                        paymentObject.put("description", "Payment For Milk");
                        paymentObject.put("currency", "INR");
                        paymentObject.put("amount", amt * 100); // Amount in paise (e.g., 10000 paise = INR 100)
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (selectedId == 1) {
                    double cost = Double.parseDouble(gv_CurrencyLiters.getSelectedItem().toString().replace("₹", ""));
                    //   Toast.makeText(PayWithQrActivity.this, "COST " + cost, Toast.LENGTH_SHORT).show();
                    double weight = Constants.calculateMilkAmount(cost, PayWithQrActivity.this);

                    try {
                        paymentObject.put("name", "Milk Vending Machine");
                        paymentObject.put("description", "Payment For Milk");
                        paymentObject.put("currency", "INR");
                        paymentObject.put("amount", cost * 100); // Amount in paise (e.g., 10000 paise = INR 100)
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
                String customerId = preferencesManager.get(Constants.RazorPayCustomerID, "").toString();
                if (customerId.isEmpty()) {
                    Constants.showAlertDialog(PayWithQrActivity.this, "Error", "Customer Id cannot be empty");
                    return;
                }

                Log.e("TAG", "onClick: " + new Gson().toJson(paymentObject));
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    public Void doInBackground(Void... voids) {
                        try {
                            razorpay = new RazorpayClient("rzp_test_bfiWftOYB0MCR7", "VuX6RLVKtB6MBILQKRzcMeZy");  //TEST
//                            razorpay = new RazorpayClient("rzp_live_oTrQqk0HauuUWZ", "7lBcCfNsgl7wKtshFz7QCm8F");//LIVE

                            JSONObject qrRequest = new JSONObject();
                            qrRequest.put("type", "upi_qr");
                            qrRequest.put("name", "Milk Vending booth");
                            qrRequest.put("usage", "single_use");
                            qrRequest.put("fixed_amount", true);
                            qrRequest.put("payment_amount", paymentObject.get("amount"));
                            qrRequest.put("description", "For Store 1");
//                            qrRequest.put("customer_id", "cust_NQXXhGiitVX9xe"); //Test
//                            qrRequest.put("customer_id", "cust_NWIoi0QrjXC2ez");//LIVE
                            qrRequest.put("customer_id", customerId);//LIVE
                            long currentTime = System.currentTimeMillis();
                            long closeByTime = currentTime + (5 * 60 * 1000);

                            // Check if close_by is within the acceptable range
                            if (closeByTime < 946684800L * 1000 || closeByTime > 4765046400L * 1000) {
                                // Handle the case where close_by is out of range
                                throw new IllegalArgumentException("close_by out of acceptable range");
                            }

                            qrRequest.put("close_by", closeByTime / 1000);
                            JSONObject notes = new JSONObject();
                            notes.put("notes_key_1", "Milk Vending");
                            notes.put("notes_key_2", String.valueOf(paymentObject));
                            qrRequest.put("notes", notes);

                            Log.e("TAG", "doInBackground: " + new Gson().toJson(qrRequest));

                            QrCode qrcode = razorpay.qrCode.create(qrRequest);

                            Log.e("TAG", "doInBackground: " + new Gson().toJson(qrcode));
                            if (qrcode != null) {
                                String imageUrl = qrcode.get("image_url").toString();
                                String qrCodeId = qrcode.get("id").toString();
                                runOnUiThread(() -> {
                                    dialog.set(showQRCodeDialog(imageUrl));
                                    dialog.get().show();
                                    Intent serviceIntent = new Intent(PayWithQrActivity.this, PaymentStatusService.class);
                                    serviceIntent.putExtra("qr_code_id", qrCodeId);
                                    startService(serviceIntent);

                                    /* Dialog close after 6 minutes*/
                                    // Schedule dialog dismissal after 6 minutes (360,000 milliseconds)
                                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            Dialog currentDialog = dialog.get();
                                            if (currentDialog != null && currentDialog.isShowing()) {

                                                Log.e("btnGenerateQr", "button CLick");
                                                currentDialog.dismiss();
                                                /// Insert data into database
                                                new Thread(new Runnable() {
                                                    @Override
                                                    public void run() {

                                                        try {
                                                            String dateFormat = "yyyy-MM-dd";
                                                            String timeFormat = "HH:mm:ss";
                                                            SimpleDateFormat dateFormatter = new SimpleDateFormat(dateFormat);
                                                            SimpleDateFormat timeFormatter = new SimpleDateFormat(timeFormat);

                                                            String date = dateFormatter.format(System.currentTimeMillis());
                                                            String time = timeFormatter.format(System.currentTimeMillis());
                                                            // Print the combined date and time

                                                            if (paymentObject.has("amount")) {
                                                                float amount = Float.parseFloat(paymentObject.get("amount").toString());
                                                                float amt = amount / 100;

                                                                Log.e("amountttt in string", String.valueOf(amt));

                                                                TransactionDao transactionDao = AppDatabase.getInstance(PayWithQrActivity.this).transactionDao();
                                                                assert date != null;
                                                                long transactionId = Constants.insertTransaction(PayWithQrActivity.this, transactionDao, "ONLINE", "", date, time, String.valueOf(amt), "TIME OUT", "");
                                                                Log.e(TAG, "onCreate: " + transactionId);
                                                                Log.e(TAG, "onCreate: " + new Gson().toJson(transactionDao.getAllTransactions()));


                                                            }

                                                            goToHome();

                                                        } catch (Exception e) {
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                }).start();
                                            }
                                        }
                                    }, 360000);

                                });
                            }

                        } catch (RazorpayException | JSONException e) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Constants.showAlertDialog(PayWithQrActivity.this, "Error", e.getMessage());
                                }
                            });
                        }
                        return null;
                    }
                }.execute();

            }
        });

        btnBackToHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Cancel the delayed task
                if (handler != null && runnable != null) {
                    handler.removeCallbacks(runnable);
                }
                goToHome();
            }
        });

//        checkout.open(this, paymentObject);
    }


    @SuppressLint("StaticFieldLeak")
    public void executeGenerateQRCodeTask(JSONObject paymentObject, String customerId) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                generateQRCode(paymentObject, customerId);
                return null;
            }
        }.execute();
    }

    private void generateQRCode(JSONObject paymentObject, String customerId) {
        try {


//            razorpay = new RazorpayClient("rzp_test_bfiWftOYB0MCR7", "VuX6RLVKtB6MBILQKRzcMeZy");  //TEST
            razorpay = new RazorpayClient("rzp_live_oTrQqk0HauuUWZ", "7lBcCfNsgl7wKtshFz7QCm8F");//LIVE

            JSONObject qrRequest = new JSONObject();
            qrRequest.put("type", "upi_qr");
            qrRequest.put("name", "Milk Vending booth");
            qrRequest.put("usage", "single_use");
            qrRequest.put("fixed_amount", true);
            qrRequest.put("payment_amount", paymentObject.get("amount"));
            qrRequest.put("description", "For Store 1");
            qrRequest.put("customer_id", customerId);
            long currentTime = System.currentTimeMillis();
            long closeByTime = currentTime + (5 * 60 * 1000);

            // Check if close_by is within the acceptable range
            if (closeByTime < 946684800L * 1000 || closeByTime > 4765046400L * 1000) {
                // Handle the case where close_by is out of range
                throw new IllegalArgumentException("close_by out of acceptable range");
            }

            qrRequest.put("close_by", closeByTime / 1000);
            JSONObject notes = new JSONObject();
            notes.put("notes_key_1", "Milk Vending");
            notes.put("notes_key_2", String.valueOf(paymentObject));
            qrRequest.put("notes", notes);

            Log.e("TAG", "doInBackground: " + new Gson().toJson(qrRequest));

            QrCode qrcode = razorpay.qrCode.create(qrRequest);

            Log.e("TAG", "doInBackground: " + new Gson().toJson(qrcode));
            if (qrcode != null) {
                String imageUrl = qrcode.get("image_url").toString();
                String qrCodeId = qrcode.get("id").toString();
                runOnUiThread(() -> {
                    dialog.set(showQRCodeDialog(imageUrl));
                    dialog.get().show();
                    Intent serviceIntent = new Intent(PayWithQrActivity.this, PaymentStatusService.class);
                    serviceIntent.putExtra("qr_code_id", qrCodeId);
                    startService(serviceIntent);

                    // Schedule dialog dismissal after 6 minutes (360,000 milliseconds)
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Dialog currentDialog = dialog.get();
                            if (currentDialog != null && currentDialog.isShowing()) {
                                Log.e("generateQRCode", "Method called");
                                currentDialog.dismiss();


                                /// Insert data into database
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {

                                        try {
                                            String dateFormat = "yyyy-MM-dd";
                                            String timeFormat = "HH:mm:ss";
                                            SimpleDateFormat dateFormatter = new SimpleDateFormat(dateFormat);
                                            SimpleDateFormat timeFormatter = new SimpleDateFormat(timeFormat);

                                            String date = dateFormatter.format(System.currentTimeMillis());
                                            String time = timeFormatter.format(System.currentTimeMillis());
                                            // Print the combined date and time

                                            if (paymentObject.has("amount")) {
                                                float amount = Float.parseFloat(paymentObject.get("amount").toString());
                                                float amt = amount / 100;

                                                Log.e("amountttt in string", String.valueOf(amt));

                                                TransactionDao transactionDao = AppDatabase.getInstance(PayWithQrActivity.this).transactionDao();
                                                assert date != null;
                                                long transactionId = Constants.insertTransaction(PayWithQrActivity.this, transactionDao, "ONLINE", "", date, time, String.valueOf(amt), "TIME OUT", "");
                                                Log.e(TAG, "onCreate: " + transactionId);
                                                Log.e(TAG, "onCreate: " + new Gson().toJson(transactionDao.getAllTransactions()));


                                            }

                                            goToHome();

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }).start();


                            }
                        }
                    }, 360000);

                });
            }

        } catch (RazorpayException | JSONException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Constants.showAlertDialog(PayWithQrActivity.this, "Error", e.getMessage());
                }
            });
        }
    }

    public Double extractNumericValueFromString(String input) {
        Pattern pattern = Pattern.compile("(\\d+(\\.\\d+)?)\\s*(\\(.*\\))?");
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            String numericValueString = matcher.group(1);
            double numericValue = Double.parseDouble(numericValueString);
            return numericValue;
        } else {

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


    private Dialog showQRCodeDialog(String imageUrl) {
        // Inflate the dialog layout
        Dialog qrCodeDialog = new Dialog(this, android.R.style.Theme_Light_NoTitleBar);
        qrCodeDialog.setContentView(R.layout.dialog_qr_code);

        // Find the ImageView in the layout
        ImageView imageViewQRCode = qrCodeDialog.findViewById(R.id.ivQRCode);

        // Set the QR code bitmap to the ImageView
        Glide.with(this).load(imageUrl).timeout(10000).into(imageViewQRCode);

        qrCodeDialog.setCancelable(true);

        // Show the dialog

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
        Intent serviceIntent = new Intent(this, PaymentStatusService.class);
        stopService(serviceIntent);
        unregisterReceiver(paymentStatusReceiver);
        unregisterReceiver(batteryReceiver);

    }

    @Override
    public void onPaymentSuccess(String s, PaymentData paymentData) {
        Log.e("TAG", "onPaymentSuccess: ");
    }

    @Override
    public void onPaymentError(int i, String s, PaymentData paymentData) {
        Log.e("TAG", "onPaymentError: " + paymentData.getPaymentId());
    }


    public void sendForMilkVending(float amt, Payment payment) {
        LottieDialog lottieDialog = new LottieDialog(PayWithQrActivity.this);
        try {
            ResponseTempStatus responseTempStatus = new Gson().fromJson(preferencesManager.get(Constants.ResponseTempStatus, "").toString(), ResponseTempStatus.class);
            float milkSellingPrice = Float.parseFloat(preferencesManager.get(Constants.MilkBasePrice, "0.0").toString());
            float offSet = Float.parseFloat(preferencesManager.get(Constants.TemperatureOffSet, 0.0).toString());

            float DENSITY_OF_MILK = Float.parseFloat(preferencesManager.get(Constants.MilkDensityPref, "0.0").toString());
            float weight = Float.parseFloat(String.valueOf((amt / milkSellingPrice))) * DENSITY_OF_MILK;

            double currentSavedTemp = responseTempStatus.getTemperature() / 10;
            float currentTemperature = Float.parseFloat(String.valueOf((currentSavedTemp + offSet)));

            SendToDevice sendToDevice = new SendToDevice();
            sendToDevice.setWeight(weight);
            sendToDevice.setStatus(true);
            sendToDevice.setCurtemperature(currentTemperature);
            sendToDevice.setSettemperature(milkSetTemperature);

            Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
            Log.e("TAG", "QR_PAYMENT: MSP" + milkSellingPrice);
            Log.e("TAG", "QR_PAYMENT: SEND COMMAND " + gson.toJson(sendToDevice));

            lottieDialog.show();
            usbSerialCommunication.sendData(gson.toJson(sendToDevice));
            isCommandSent = true;
            usbSerialCommunication.setReadDataListener(new UsbSerialCommunication.ReadDataListener() {
                @Override
                public void onReadData(String data) {
                    Log.d("TAG", "onReadData: " + data + "\n status " + data.contains("status"));

                    if (data.contains("status")) {

                        /*if (!isCharging) {
                            Constants.saveLogs(PayWithQrActivity.this, "Lost Electricity");
                            showFailedProcessDoneDialog(amt, payment);
                        }*/
                        ResponseMilkDispense milkDispense = new Gson().fromJson(data, ResponseMilkDispense.class);
                        if (milkDispense != null) {
                            double percentage = (milkDispense.getCurTemperature() / milkDispense.getSetTemperature()) * 100;
                            /*if (lottieDialog != null) {
                                if (percentage > 0) {
                                    lottieDialog.setPercentage(percentage);
                                }
                            }*/
                            if (milkDispense.getStatus()) {

                                // Cancel the delayed task
                                if (handler != null && runnable != null) {
                                    handler.removeCallbacks(runnable);
                                }

                                try {
                                    if (lottieDialog.isShowing()) {
                                        lottieDialog.dismiss();
                                    }
                                    isCommandSent = false;
                                    preferencesManager.save(Constants.CurrentTemperature, currentSavedTemp);
                                    preferencesManager = SharedPreferencesManager.getInstance(getInstance());
                                    float offSet = Float.parseFloat(preferencesManager.get(Constants.TemperatureOffSet, 0.0).toString());
                                    double currentSavedTemp = responseTempStatus.getTemperature() / 10;
                                    float currentTemperature = Float.parseFloat(String.valueOf((currentSavedTemp + offSet)));
                                    usbSerialCommunication.fireOnStart(currentTemperature);
                                    showAndProcessDoneDialog(amt, payment);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }

                            }


//
                        }
                    }
                }
            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void showFailedProcessDoneDialog(float amt, Payment payment) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                Constants.showAcceptDialog(PayWithQrActivity.this, "Error", "Lost Electricity Connection!! Please Try after sometime.", (dialog1, which) -> {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            try {
                                String dateFormat = "yyyy-MM-dd";
                                String timeFormat = "HH:mm:ss";
                                SimpleDateFormat dateFormatter = new SimpleDateFormat(dateFormat);
                                SimpleDateFormat timeFormatter = new SimpleDateFormat(timeFormat);

                                String date = dateFormatter.format(System.currentTimeMillis());
                                String time = timeFormatter.format(System.currentTimeMillis());
                                // Print the combined date and time

                                TransactionDao transactionDao = AppDatabase.getInstance(PayWithQrActivity.this).transactionDao();
                                assert date != null;
                                long transactionId = Constants.insertTransaction(PayWithQrActivity.this, transactionDao, "ONLINE", "", date, time, String.valueOf(amt), "FAILED", payment.get("vpa"));
                                Log.e(TAG, "onCreate: " + transactionId);
                                Log.e(TAG, "onCreate: " + new Gson().toJson(transactionDao.getAllTransactions()));
                                Intent intent = new Intent(PayWithQrActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }, (dialog1, which) -> {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            try {
                                String dateFormat = "yyyy-MM-dd";
                                String timeFormat = "HH:mm:ss";
                                SimpleDateFormat dateFormatter = new SimpleDateFormat(dateFormat);
                                SimpleDateFormat timeFormatter = new SimpleDateFormat(timeFormat);

                                String date = dateFormatter.format(System.currentTimeMillis());
                                String time = timeFormatter.format(System.currentTimeMillis());
                                // Print the combined date and time

                                TransactionDao transactionDao = AppDatabase.getInstance(PayWithQrActivity.this).transactionDao();
                                assert date != null;
                                long transactionId = Constants.insertTransaction(PayWithQrActivity.this, transactionDao, "ONLINE", "", date, time, String.valueOf(amt), "FAILED", payment.get("vpa"));
                                Log.e(TAG, "onCreate: " + transactionId);
                                Log.e(TAG, "onCreate: " + new Gson().toJson(transactionDao.getAllTransactions()));
                                Intent intent = new Intent(PayWithQrActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                });

            }
        });
    }

    public void showAndProcessDoneDialog(float amt, Payment payment) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(PayWithQrActivity.this);
                LayoutInflater inflater = getLayoutInflater();
                View view = inflater.inflate(R.layout.dialog_lottie, null);

                LottieAnimationView lottieAnimationView = view.findViewById(R.id.lottieAnimationView);
                LottieAnimationView lottieAnimationViewDone = view.findViewById(R.id.lottieAnimationViewDone);
                TextView tvProgressDialog = view.findViewById(R.id.tvProgressDialog);
                MaterialButton btnDone = view.findViewById(R.id.doneButton);
                TextView tvProcessDoneText = view.findViewById(R.id.tvProcessDoneText);
                TextView tvOpenTheDoor = view.findViewById(R.id.tvOpenTheDoor);
                btnDone.setVisibility(View.VISIBLE);
                lottieAnimationView.setVisibility(View.GONE);
                lottieAnimationViewDone.setVisibility(View.VISIBLE);
                tvProcessDoneText.setVisibility(View.VISIBLE);
                tvOpenTheDoor.setVisibility(View.VISIBLE);
                tvProgressDialog.setVisibility(View.GONE);
                lottieAnimationViewDone.setAnimation(R.raw.process_done);
                lottieAnimationViewDone.setRepeatMode(LottieDrawable.RESTART);
                lottieAnimationViewDone.playAnimation();

                // Customize the LottieAnimationView and TextView here

                builder.setView(view);
                builder.setCancelable(false); // Set to true if you want the dialog to be cancellable

                AlertDialog dialog = builder.create();
                dialog.show();


                /// Initialize the handler
                handler = new Handler();
                Long screenTimeOut = Long.parseLong(preferencesManager.get(ScreenTimeOutPref, "0.0").toString());

                // Define the Runnable task
                runnable = () -> {
                    // Task to execute after delay
                    dialog.dismiss();
//                        onDestroy();

                    insertDataOnProcessDone(amt, payment);
                };

                // Post the Runnable with a 15-second delay
                handler.postDelayed(runnable, screenTimeOut * 1000);



                btnDone.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        //  If User clicks on the done button. Runnable should be close
                        // Cancel the delayed task
                        if (handler != null && runnable != null) {
                            handler.removeCallbacks(runnable);
                        }

                        dialog.dismiss();
//                        onDestroy();
                        Log.e(TAG, "onClick:Payment " + new Gson().toJson(payment));

                        insertDataOnProcessDone(amt, payment);
                        /*Intent intent = new Intent(PayWithQrActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();*/
//                doPostTransaction(Constants.PostTransactionURL);
                    }
                });
            }
        });


    }

    /*
     * Here we are getting time out from shared preference
     * And after that screen automatically off
     * */
    void screenClose() {
        preferencesManager = SharedPreferencesManager.getInstance(getInstance());
        Log.e("timeOut", preferencesManager.get(ScreenTimeOutPref, "0").toString());

        Long screenTimeOut = Long.parseLong(preferencesManager.get(ScreenTimeOutPref, "0.0").toString());

        // Define the Runnable task
        runnable = () -> {
            // Task to execute after delay
            goToHome(); // Closes the current activity
        };

        // Post the Runnable with a 15-second delay
        handler.postDelayed(runnable, screenTimeOut * 1000);
    }


    void goToHome() {

        finish();

//        Intent intent = new Intent(PayWithQrActivity.this, MainActivity.class);
//        // Clear all previous activities
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        startActivity(intent);

    }

    /// When process is completed. Data will be insert into database
    void insertDataOnProcessDone(float amt, Payment payment){
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    String dateFormat = "yyyy-MM-dd";
                    String timeFormat = "HH:mm:ss";
                    SimpleDateFormat dateFormatter = new SimpleDateFormat(dateFormat);
                    SimpleDateFormat timeFormatter = new SimpleDateFormat(timeFormat);

                    String date = dateFormatter.format(System.currentTimeMillis());
                    String time = timeFormatter.format(System.currentTimeMillis());
                    // Print the combined date and time

                    TransactionDao transactionDao = AppDatabase.getInstance(PayWithQrActivity.this).transactionDao();
                    assert date != null;
                    long transactionId = Constants.insertTransaction(PayWithQrActivity.this, transactionDao, "ONLINE", "", date, time, String.valueOf(amt), "SUCCESS", payment.get("vpa"));
                    Log.e(TAG, "onCreate: " + transactionId);
                    Log.e(TAG, "onCreate: " + new Gson().toJson(transactionDao.getAllTransactions()));
                    Intent intent = new Intent(PayWithQrActivity.this, MainActivity.class);
                    // Clear all previous activities
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
