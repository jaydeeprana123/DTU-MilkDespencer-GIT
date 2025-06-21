package com.imdc.milkdespencer.adminUi;

import static com.imdc.milkdespencer.common.Constants.CashTransactionMode;
import static com.imdc.milkdespencer.common.Constants.GetConfigurationUrl;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.imdc.milkdespencer.R;
import com.imdc.milkdespencer.common.Constants;
import com.imdc.milkdespencer.common.SharedPreferencesManager;
import com.imdc.milkdespencer.common.UsbSerialCommunication;
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

public class CIPActivity extends AppCompatActivity {
    //    private FirebaseAnalytics mFirebaseAnalytics;
    SharedPreferencesManager preferencesManager;
    Button btnCompressorOff, btnAgitatorOff, btnRemoveMilk,
            btnCIP;

    private MaterialButton btnBackToHome;
    AppDatabase appDatabase;
    User user;
    boolean isAgitatorOff = false;
    boolean isCompressorOff = false;

    boolean isRemoveMilkOn = false;

    String milkCurrentTemperature = "";

    private UsbSerialCommunication usbSerialCommunication;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cip);
        usbSerialCommunication = new UsbSerialCommunication(getApplicationContext());

        if (!usbSerialCommunication.connected) {
            usbSerialCommunication.connect();
            usbSerialCommunication.setBaudRate(115200);

        }

        preferencesManager = SharedPreferencesManager.getInstance(this);

        // Fetch preferences
        ResponseTempStatus responseTempStatus = new Gson().fromJson(preferencesManager.get(Constants.ResponseTempStatus, "").toString(), ResponseTempStatus.class);
        float offSet = Float.parseFloat(preferencesManager.get(Constants.TemperatureOffSet, "0.0").toString());
        double currentSavedTemp = responseTempStatus.getTemperature() / 10.0;
        float currentTemperature = (float) (currentSavedTemp + offSet);

        milkCurrentTemperature = String.valueOf(currentTemperature);

        appDatabase = AppDatabase.getInstance(this);
        btnCIP = findViewById(R.id.btnCIP);
        btnCompressorOff = findViewById(R.id.btnCompressorOff);
        btnAgitatorOff = findViewById(R.id.btnAgitatorOff);
        btnRemoveMilk = findViewById(R.id.btnRemoveMilk);
        btnBackToHome = findViewById(R.id.btnBackToHome);
        btnAgitatorOff.setEnabled(false);
        btnCIP.setEnabled(false);
        btnRemoveMilk.setEnabled(false);
        btnCIP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnRemoveMilk.setEnabled(true);
                btnCIP.setEnabled(true);
                Log.e("btn CIP", " is pressed");

                sendDataForCIP(false, false, true, false);

//                isCipOn = true;
                showCIPRunningDialog(CIPActivity.this,(dialog, which) -> {
                    sendDataForCIP(false, false, false, false);

                    addCIPDataINtoDatabase("CIP");

                    }, "CIP IS RUNNING");

            }
        });

        btnRemoveMilk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnRemoveMilk.setEnabled(true);
                btnCIP.setEnabled(true);
                Log.e("btn btnRemoveMilk", " is pressed");
                sendDataForCIP(false, false, true, false);
                showCIPRunningDialog(CIPActivity.this,(dialog, which) -> {
                    sendDataForCIP(false, false, false, false);

                    addCIPDataINtoDatabase("REMOVE MILK");

                }, "REMOVING MILK");
            }
        });


        btnCompressorOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                isSendDataStop = true;

                btnCompressorOff.setEnabled(false);
                btnAgitatorOff.setEnabled(true);
                isCompressorOff = true;
                if(isAgitatorOff){
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
                sendDataForCIP(false, false, false, true);
                isCipOn = false;
                finish();
            }
        });


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
            finish();
//            Intent intent = new Intent(AdminActivity.this, MainActivity.class);
//            startActivity(intent);
//            finish();
        }
        return super.onOptionsItemSelected(item);
    }


    /*
     * When payment is done. Send for Vending the milk*/
    public void sendDataForCIP(boolean compressorStatus,boolean agitatorStatus,boolean pumpStatus, boolean isCipDone) {
        try {
            // Prepare data to send to device
            SendToDeviceForCIP sendToDevice = new SendToDeviceForCIP();
            sendToDevice.setCompressor(compressorStatus);
            sendToDevice.setAgitator(agitatorStatus);
            sendToDevice.setPump(pumpStatus);
            sendToDevice.setCipdone(isCipDone);
            Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
            //  logError(TAG, "QR_PAYMENT: SEND COMMAND " + gson.toJson(sendToDevice));

            /// Check that usb serial is not null
            if (usbSerialCommunication != null) {
                usbSerialCommunication.sendData(gson.toJson(sendToDevice));

            } else {

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();

        sendDataForCIP(false, false, false, true);
        isCipOn = false;
        UsbSerialCommunication.isSendDataStop= false;

    }


   private void addCIPDataINtoDatabase(String transactionStatus){
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
                    doPostTransaction(preferencesManager, "/api/Transaction/PostTransaction", transaction, transactionDao);
                    Constants.saveLogs(getApplicationContext(), "CIP Done");

                } else {
                    Constants.saveLogs(getApplicationContext(), "Internet Connection Error");
                    //   Toast.makeText(activity, "Internet not available", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


}