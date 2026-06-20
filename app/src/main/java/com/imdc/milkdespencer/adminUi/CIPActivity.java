package com.imdc.milkdespencer.adminUi;

import static com.imdc.milkdespencer.common.Constants.CashTransactionMode;
import static com.imdc.milkdespencer.common.Constants.GetConfigurationUrl;
import static com.imdc.milkdespencer.common.Constants.KEY_API_CALL;
import static com.imdc.milkdespencer.common.Constants.KEY_ELECTRICITY;
import static com.imdc.milkdespencer.common.Constants.KEY_USB;
import static com.imdc.milkdespencer.common.Constants.KEY_WEIGHT;
import static com.imdc.milkdespencer.common.Constants.MachineId;
import static com.imdc.milkdespencer.common.Constants.MilkBasePrice;
import static com.imdc.milkdespencer.common.Constants.RemainingVolumePref;
import static com.imdc.milkdespencer.common.Constants.doPostConfigurationData;
import static com.imdc.milkdespencer.common.Constants.doPostTransaction;
import static com.imdc.milkdespencer.common.Constants.exportTransactionsToCSVAndShare;
import static com.imdc.milkdespencer.common.Constants.generateSafeUniqueTransactionId;
import static com.imdc.milkdespencer.common.Constants.isNetworkAvailable;
import static com.imdc.milkdespencer.common.Constants.remainingVolume;
import static com.imdc.milkdespencer.common.Constants.showCIPRunningDialog;
import static com.imdc.milkdespencer.common.UsbSerialCommunication.isCipOn;
import static com.imdc.milkdespencer.common.UsbSerialCommunication.isLowLevel;
import static com.imdc.milkdespencer.common.UsbSerialCommunication.isSendDataStop;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.imdc.milkdespencer.MainActivity;
import com.imdc.milkdespencer.PayWithQrActivity;
import com.imdc.milkdespencer.R;
import com.imdc.milkdespencer.common.Constants;
import com.imdc.milkdespencer.common.LottieDialog;
import com.imdc.milkdespencer.common.SharedPreferencesManager;
import com.imdc.milkdespencer.common.UsbSerialCommunication;
import com.imdc.milkdespencer.models.ResponseMilkDispense;
import com.imdc.milkdespencer.models.ResponseTempStatus;
import com.imdc.milkdespencer.models.SendToDevice;
import com.imdc.milkdespencer.models.SendToDeviceForCIP;
import com.imdc.milkdespencer.roomdb.AppDatabase;
import com.imdc.milkdespencer.roomdb.entities.TransactionEntity;
import com.imdc.milkdespencer.roomdb.entities.User;
import com.imdc.milkdespencer.roomdb.interfaces.TransactionDao;
import com.razorpay.Payment;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class CIPActivity extends AppCompatActivity implements UsbSerialCommunication.ReadDataListener, UsbSerialCommunication.ReadDataForCIPListener {
    //    private FirebaseAnalytics mFirebaseAnalytics;
    SharedPreferencesManager preferencesManager;
    Button btnCompressorOff, btnAgitatorOff, btnRemoveMilk,
            btnCIP;

    boolean isBackButtonPressed = false;

    private MaterialButton btnBackToHome;
    AppDatabase appDatabase;
    User user;
    boolean isAgitatorOff = false;
    boolean isCompressorOff = false;

    boolean isRemoveMilkOn = false;

    String milkCurrentTemperature = "";

    private UsbSerialCommunication usbSerialCommunication;

    private static final long SEND_INTERVAL_MS = 10000; // Send every 500ms

    private boolean shouldContinueSending = false;

    private boolean isStopWhenCIPEnabled = false;

    private Handler handlerForSendData = new Handler(); // Create a Handler instance
    Runnable sendCommandRunnable;

    ConstraintLayout clProgress, clMain;

    private static final String TAG = CIPActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cip);

        logError(Constants.TAG, "onCreate call");

        usbSerialCommunication = new UsbSerialCommunication(getApplicationContext());
        preferencesManager = SharedPreferencesManager.getInstance(getApplicationContext());
        appDatabase = AppDatabase.getInstance(this);
        btnCIP = findViewById(R.id.btnCIP);
        btnCompressorOff = findViewById(R.id.btnCompressorOff);
        btnAgitatorOff = findViewById(R.id.btnAgitatorOff);
        btnRemoveMilk = findViewById(R.id.btnRemoveMilk);
        btnBackToHome = findViewById(R.id.btnBackToHome);
        clProgress = findViewById(R.id.clProgress);
        clMain = findViewById(R.id.clMain);
        btnAgitatorOff.setEnabled(false);
        btnCIP.setEnabled(false);
        btnRemoveMilk.setEnabled(false);

        if (!usbSerialCommunication.connected) {

            logError(TAG, " usbSerialCommunication not connected");
            usbSerialCommunication.connect();
            usbSerialCommunication.setBaudRate(115200);

            fireOnForCIPOn();
        } else {

            logError(TAG, " usbSerialCommunication already connected");

            fireOnForCIPOn();
        }

        usbSerialCommunication.setReadDataListener(this);
        usbSerialCommunication.setReadDataForCIPListener(this);

        // Fetch preferences
        ResponseTempStatus responseTempStatus = new Gson().fromJson(preferencesManager.get(Constants.ResponseTempStatus, "").toString(), ResponseTempStatus.class);
        float offSet = Float.parseFloat(preferencesManager.get(Constants.TemperatureOffSet, "0.0").toString());
        double currentSavedTemp = responseTempStatus.getTemperature() / 10.0;
        float currentTemperature = (float) (currentSavedTemp + offSet);

        milkCurrentTemperature = String.valueOf(currentTemperature);


        btnCIP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnRemoveMilk.setEnabled(true);
                btnCIP.setEnabled(true);
                // Log.e("btn CIP", " is pressed");

                sendDataForCIP(false, false, true, false);

//                isCipOn = true;
                showCIPRunningDialog(CIPActivity.this, (dialog, which) -> {
                    sendDataForCIP(false, false, false, false);
                    addCIPDataINtoDatabase("CIP");
                    dialog.dismiss();

                }, "CIP IS RUNNING");

            }
        });

        btnRemoveMilk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnRemoveMilk.setEnabled(true);
                btnCIP.setEnabled(true);
                // Log.e("btn btnRemoveMilk", " is pressed");
                sendDataForCIP(false, false, true, false);
                showCIPRunningDialog(CIPActivity.this, (dialog, which) -> {
                    sendDataForCIP(false, false, false, false);

                    addCIPDataINtoDatabase("REMOVE MILK");
                    dialog.dismiss();

                }, "REMOVING MILK");
            }
        });


        btnCompressorOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isStopWhenCIPEnabled = true;
                isSendDataStop = true;
                btnCompressorOff.setEnabled(false);
                btnAgitatorOff.setEnabled(true);
                isCompressorOff = true;
                if (isAgitatorOff) {
                    btnRemoveMilk.setEnabled(true);
                }


                sendDataForCIP(false, true, false, false);

            }
        });


        btnAgitatorOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnAgitatorOff.setEnabled(false);
                btnCIP.setEnabled(true);
                btnRemoveMilk.setEnabled(true);
                isAgitatorOff = true;

                sendDataForCIP(false, false, false, false);
            }
        });

        btnBackToHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                isBackButtonPressed = true;
                sendDataForCIP(false, false, false, true);
                isCipOn = false;

                runOnUiThread(() -> {
                    clProgress.setVisibility(View.VISIBLE);
                    clMain.setVisibility(View.GONE);                          // ✅ End the activity
                });


            }
        });


    }

    /// Send data continues..And get data from on read... It will stop for to enable CIP mode on
    private void sendDataRepeatedly(String commandJson) {
        sendCommandRunnable = new Runnable() {
            @Override
            public void run() {

                logError("sendCommandRunnable", "sendCommandRunnable");

                if (shouldContinueSending) {

                    logError(TAG, " commandJson" + commandJson);

                    usbSerialCommunication.sendData(commandJson);

                    if (isStopWhenCIPEnabled) {
                        shouldContinueSending = false;
                        return;
                    }

                    handlerForSendData.postDelayed(this, SEND_INTERVAL_MS);
                }
            }
        };

        handlerForSendData.postDelayed(sendCommandRunnable, SEND_INTERVAL_MS);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_admin, menu);

        MenuItem menuItem = menu.findItem(R.id.action_logout);
        menuItem.setTitle(" BACK");
        menuItem.setIcon(R.drawable.ic_arrow_back); // Replace with your desired drawable

        Drawable icon = menuItem.getIcon();
        if (icon != null) {
            icon.mutate(); // Ensure the drawable is mutable

            icon.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN);
        }


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.action_logout) {

            runOnUiThread(() -> {
                clProgress.setVisibility(View.VISIBLE);
                clMain.setVisibility(View.GONE);                          // ✅ End the activity
            });

            isBackButtonPressed = true;
            sendDataForCIP(false, false, false, true);
            isCipOn = false;
            return true;       // ✅ consume the event

//            Intent intent = new Intent(AdminActivity.this, MainActivity.class);
//            startActivity(intent);
//            finish();
        }
        return super.onOptionsItemSelected(item);
    }


    /*Continue set as a cip on, If not getting status as a status as a enabled */
    public void fireOnForCIPOn() {

        logError(Constants.TAG, "fireOnForCIPOn");
        shouldContinueSending = true;
        clProgress.setVisibility(View.VISIBLE);

        try {
            SendToDevice sendToDevice = new SendToDevice();

            ResponseTempStatus responseTempStatus = new Gson().fromJson(
                    preferencesManager.get(Constants.ResponseTempStatus, "").toString(),
                    ResponseTempStatus.class
            );

            float setTemperature = Float.parseFloat(preferencesManager.get(Constants.TemperatureSet, "0.0").toString());
            float offSet = Float.parseFloat(preferencesManager.get(Constants.TemperatureOffSet, "0.0").toString());

            // Calculations
            double currentSavedTemp = responseTempStatus.getTemperature() / 10.0;
            float currentTemperature = (float) (currentSavedTemp + offSet);

            sendToDevice.setCurtemperature(currentTemperature + offSet);
            sendToDevice.setSettemperature(setTemperature);

            /// 31-12-2024 add isCIP
            sendToDevice.setCIP(true);
            sendToDevice.setLowlevel(isLowLevel);
            sendToDevice.setWeight(100.0f);
            sendToDevice.setStatus(false);
            Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
            logError(TAG, "Send CIP data" + gson.toJson(sendToDevice));
            /// Check that usb serial is not null
            if (usbSerialCommunication != null) {

                logError(TAG, "Send CIP " + gson.toJson(sendToDevice));

                sendDataRepeatedly(gson.toJson(sendToDevice));


                usbSerialCommunication.setReadDataListener(this);

            } else {

//                try {
//                    Constants.saveLogs(CIPActivity.this, "USB not connected on CIP Screen", KEY_USB);
//                } catch (Exception e) {
//                    logError("USB", "Logging failed: ${e.message}");
//                    // Don't crash, just log the error silently
//                }
//
//                showErrorIfUSBSerialCommunicationLost();

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /*
     * When payment is done. Send for Vending the milk*/
    public void sendDataForCIP(boolean compressorStatus, boolean agitatorStatus, boolean pumpStatus, boolean isCipDone) {
        try {
            // Prepare data to send to device
            SendToDeviceForCIP sendToDevice = new SendToDeviceForCIP();
            sendToDevice.setCompressor(compressorStatus);
            sendToDevice.setAgitator(agitatorStatus);
            sendToDevice.setPump(pumpStatus);
            sendToDevice.setCipdone(isCipDone);
            Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
            logError(TAG, "sendDataForCIP: SEND COMMAND " + gson.toJson(sendToDevice));

            /// Check that usb serial is not null
            if (usbSerialCommunication != null) {
                usbSerialCommunication.sendData(gson.toJson(sendToDevice));

            } else {
                usbSerialCommunication = new UsbSerialCommunication(getApplicationContext()); // replace with actual init logic
                usbSerialCommunication.connect();
                usbSerialCommunication.setBaudRate(115200);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*Read Listener Response*/
    private void handleSerialReadingResponse(String data) {
        logError(TAG, "onReadData: " + data);

        /// If it contains status key
        //
        if (data.contains("CIP")) {

            if (!isStopWhenCIPEnabled) {
                clProgress.setVisibility(View.GONE);
                isStopWhenCIPEnabled = true;
            }

            logError("TAG", "onReadData: if status get" + data);
        }
    }

    private void logError(String tag, String message) {
        Log.e(tag, message);
    }


    private void showErrorIfUSBSerialCommunicationLost() {
        runOnUiThread(() -> Constants.showDispenseErrorMessageDialog(
                CIPActivity.this,
                "Error",
                "Sorry. Something Went wrong!! Please try after some time.",
                (dialog, which) -> finish()
        ));
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        isSendDataStop = false;
        handlerForSendData.removeCallbacks(sendCommandRunnable);
        sendDataForCIP(false, false, false, true);
        isCipOn = false;


    }


    private void addCIPDataINtoDatabase(String transactionStatus) {
        remainingVolume = 0;
        preferencesManager.save(RemainingVolumePref, String.valueOf(remainingVolume));
        new Thread(() -> {
            try {
                String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(System.currentTimeMillis());
                String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis());
                TransactionDao transactionDao = AppDatabase.getInstance(getApplicationContext()).transactionDao();
                TransactionEntity transaction = new TransactionEntity();
                transaction.setUserName("");
                transaction.setPassword("");
                transaction.setTransactionType("");
                transaction.setBankTransactionNo("");
                transaction.setRemainingvolume(0);
                transaction.setTransactionDate(date);
                transaction.setTransactionTime(time);
                transaction.setAmount(0);
                transaction.setUploadToServer(0);
                transaction.setVolume(0);
                transaction.setTransactionStatus(transactionStatus);
                transaction.setUpiId("");

                String uniqueId = generateSafeUniqueTransactionId(transactionDao);
                transaction.setUniqueTransactionId(uniqueId);

                /// Added new on 4-1-2025
                transaction.setMilkPrice(preferencesManager.get(MilkBasePrice, "").toString());
                transaction.setMilkTemperature(milkCurrentTemperature);

                /// Added on 1-1 2025
                transaction.setMachineId(preferencesManager.get(MachineId, "").toString());

                /// Insert into Sqlite database
                long transactionId = transactionDao.insert(transaction);
                transaction.setId(transactionId);

                if (isNetworkAvailable(getApplicationContext())) {

                    try {
                        doPostTransaction(preferencesManager, "/api/Transaction/PostTransaction", transaction, transactionDao);
                        Constants.saveLogs(getApplicationContext(), "CIP Done", "CIP");
                    } catch (Exception e) {

                    }


                } else {

                    try {
                        Constants.saveLogs(getApplicationContext(), "Internet Connection Error", KEY_API_CALL);
                    } catch (Exception e) {

                    }

                    //   Toast.makeText(activity, "Internet not available", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    @Override
    public void onReadData(String data) {
        logError(TAG + " onReadData: ", data);

        // Only proceed if the back button was pressed
        if (isBackButtonPressed) {

            ResponseTempStatus responseTempStatus;
            try {
                // Try parsing the JSON into ResponseTempStatus
                responseTempStatus = new Gson().fromJson(data, ResponseTempStatus.class);

                // If temperature field exists, hide progressBar and finish activity
                if (responseTempStatus.getTemperature() != null) {

                    runOnUiThread(() -> {
                        clProgress.setVisibility(View.GONE);  // ✅ Correct: must run on UI thread
                        finish();                              // ✅ End the activity
                    });

                }

            } catch (Exception e) {
                logError(TAG, "Error parsing responseTempStatus " + e);  // ✅ Good error logging
                return;
            }
        }
    }


    @Override
    public void onReadCIPData(String data) {
        if (data != null && data.contains("Inside CIP loop") && !isBackButtonPressed) {
            logError(TAG + " receivedData INN: ", data + " isSenData " + isSendDataStop);
            shouldContinueSending = false;
            isStopWhenCIPEnabled = true;
            isSendDataStop = true;
//            if (!isSendDataStop) {
//                logError(TAG + " receivedData INN: isSendDataStop", "true When Inside CIP Loop");
//                isSendDataStop = true;
//            }
            runOnUiThread(() -> {
                clProgress.setVisibility(View.GONE);
                clMain.setVisibility(View.VISIBLE);

            });
        }
    }


    @Override
    public void onBackPressed() {

        runOnUiThread(() -> {
            clProgress.setVisibility(View.VISIBLE);
            clMain.setVisibility(View.GONE);                          // ✅ End the activity
        });


        isBackButtonPressed = true;
        sendDataForCIP(false, false, false, true);
        isCipOn = false;


        // Optionally call the default behavior
    }
}