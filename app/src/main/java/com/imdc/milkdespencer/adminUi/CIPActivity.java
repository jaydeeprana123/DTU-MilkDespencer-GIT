package com.imdc.milkdespencer.adminUi;

import static com.imdc.milkdespencer.common.Constants.CashTransactionMode;
import static com.imdc.milkdespencer.common.Constants.GetConfigurationUrl;
import static com.imdc.milkdespencer.common.Constants.doPostConfigurationData;
import static com.imdc.milkdespencer.common.Constants.exportTransactionsToCSVAndShare;
import static com.imdc.milkdespencer.common.Constants.showCIPRunningDialog;
import static com.imdc.milkdespencer.common.UsbSerialCommunication.isCipOn;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.imdc.milkdespencer.PayWithQrActivity;
import com.imdc.milkdespencer.R;
import com.imdc.milkdespencer.TransactionHistoryActivity;
import com.imdc.milkdespencer.TransactionHistoryByDateActivity;
import com.imdc.milkdespencer.adapter.UserAdapter;
import com.imdc.milkdespencer.common.Constants;
import com.imdc.milkdespencer.common.LottieDialog;
import com.imdc.milkdespencer.common.SharedPreferencesManager;
import com.imdc.milkdespencer.common.UsbSerialCommunication;
import com.imdc.milkdespencer.enums.UserTypeEnum;
import com.imdc.milkdespencer.models.ResponseTempStatus;
import com.imdc.milkdespencer.models.SendToDevice;
import com.imdc.milkdespencer.models.SendToDeviceForCIP;
import com.imdc.milkdespencer.roomdb.AppDatabase;
import com.imdc.milkdespencer.roomdb.entities.TransactionEntity;
import com.imdc.milkdespencer.roomdb.entities.User;
import com.razorpay.Payment;

import java.util.List;

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

    private UsbSerialCommunication usbSerialCommunication;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cip);
        usbSerialCommunication = new UsbSerialCommunication(getApplicationContext());

        isCipOn = true;

        if (!usbSerialCommunication.connected) {
            usbSerialCommunication.connect();
            usbSerialCommunication.setBaudRate(115200);
        }

        preferencesManager = SharedPreferencesManager.getInstance(this);

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
                btnCIP.setEnabled(false);
                Log.e("btn CIP", " is pressed");

                sendDataForCIP(false, false, true, false);

//                isCipOn = true;
                showCIPRunningDialog(CIPActivity.this,(dialog, which) -> {
                    sendDataForCIP(false, false, false, false);

                    btnCompressorOff.setVisibility(View.GONE);
                    btnAgitatorOff.setVisibility(View.GONE);
                    btnRemoveMilk.setVisibility(View.GONE);
                    btnCIP.setVisibility(View.GONE);
                    btnBackToHome.setVisibility(View.VISIBLE);
                }, "CIP IS RUNNING");

            }
        });

        btnRemoveMilk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnRemoveMilk.setEnabled(false);
                btnCIP.setEnabled(true);
                Log.e("btn btnRemoveMilk", " is pressed");
                sendDataForCIP(false, false, true, false);
                showCIPRunningDialog(CIPActivity.this,(dialog, which) -> {
                    sendDataForCIP(false, false, false, false);
                }, "REMOVING MILK");
            }
        });


        btnCompressorOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

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
                isAgitatorOff = true;
                if(isCompressorOff){

                    btnRemoveMilk.setEnabled(true);
                }

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
        Constants.saveLogs(CIPActivity.this, "CIP Done");
        sendDataForCIP(false, false, false, true);
        isCipOn = false;

    }
}