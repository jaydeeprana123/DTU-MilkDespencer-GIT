package com.imdc.milkdespencer;

import static com.imdc.milkdespencer.common.Constants.MilkBasePrice;
import static com.imdc.milkdespencer.common.Constants.TemperatureOffSet;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.imdc.milkdespencer.common.Constants;
import com.imdc.milkdespencer.common.SharedPreferencesManager;
import com.imdc.milkdespencer.common.UsbSerialCommunication;
import com.imdc.milkdespencer.models.ResponseTempStatus;
import com.imdc.milkdespencer.roomdb.AppDatabase;
import com.imdc.milkdespencer.roomdb.entities.User;
import com.imdc.milkdespencer.roomdb.interfaces.UserDao;

import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements UsbSerialCommunication.ReadDataListener {

    private static final String ACTION_USB_PERMISSION = "com.imdc.milkdespencer.USB_PERMISSION";
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int DELAY_TIME_MILLIS = 16000; // 16 seconds
    static SharedPreferencesManager preferencesManager;
    private final Handler handler = new Handler();
    Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
    IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
    LinearLayout llCash, llQr, lvStatus, llAlert;
    ImageView ivAgitator, ivCompressor;
    AlertDialog alertDialog;
    boolean isShowError = false;
    //    private  UsbSerialManager usbSerialManager;
    private UsbSerialCommunication usbSerialCommunication;
    ///TODO: 1) Read Continuous data from Serial // { "temperature": "3.04",  should not be more than set Temperature divide by 10 and then add offset value
    //  "compressor": true, green and red indicators
    //  "agitator": false, green and red indicators
    //  "lowlevel": true, if true close the system and show the dialog low on Milk.}
    //  2) Add Agitator and the Compressor, Milk Rate to the right side of the IDMC logo.
    //  3) Admin Screen UI which can be used for the add user and configurations.
    //  4) API Calls ==> Transactions
    public final BroadcastReceiver usbPermissionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    boolean permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);

                    if (permissionGranted && usbDevice != null) {
                        usbSerialCommunication.openConnection(usbDevice);
                    } else {
                        Log.e(TAG, "USB permission denied.");
                    }

                    context.unregisterReceiver(this);
                }
            }
        }
    };
    private Button btnPayWithCash, btnPayWithQr, btnStart, btnDone;
    private CardView cvPayWithCash, cvPayWithQr, cv_error;
    private AppDatabase appDatabase;
    private TextView tvTemperature, tvMilkBasePrice, tv_Message;
    private LottieAnimationView lvAnimation;
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get the current battery status
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

            // Check if the device is charging
            boolean isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL);

            Log.e("isCharging boolean", String.valueOf(isCharging));

            if (isCharging) {
                // Device is charging

                Log.e("isCharging if true", String.valueOf(isCharging));

                btnStart.setVisibility(View.VISIBLE);
                llCash.setVisibility(View.GONE);
                llQr.setVisibility(View.GONE);
                cvPayWithQr.setVisibility(View.GONE);
                cvPayWithCash.setVisibility(View.GONE);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        cv_error.setVisibility(View.GONE);
//                        usbSerialCommunication.connect();
                        if (usbSerialCommunication.connected) {

                            Log.e("usbSerialCommunication connected", String.valueOf(isCharging));

                            /// If Cash and Pay with Qr button is not visible. that time btnStart visi
                            if(llCash.getVisibility() == View.GONE){
                                btnStart.setVisibility(View.VISIBLE);
                                llCash.setVisibility(View.GONE);
                                llQr.setVisibility(View.GONE);
                                cvPayWithQr.setVisibility(View.GONE);
                                cvPayWithCash.setVisibility(View.GONE);
                            }


                        } else {
                            llCash.setVisibility(View.GONE);
                            llQr.setVisibility(View.GONE);
                            cvPayWithQr.setVisibility(View.GONE);
                            cvPayWithCash.setVisibility(View.GONE);
                            btnStart.setVisibility(View.VISIBLE);

                            Log.e("usbSerialCommunication disconnected", String.valueOf(isCharging));


                        }
//                        usbSerialCommunication.setBaudRate(115200);
                    }
                }, DELAY_TIME_MILLIS);

            } else {

                Log.e("isCharging if false", String.valueOf(isCharging));

                // Device is not charging
                // Disable the Ui and show the error message.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        llCash.setVisibility(View.GONE);
                        llQr.setVisibility(View.GONE);
                        cvPayWithQr.setVisibility(View.GONE);
                        cvPayWithCash.setVisibility(View.GONE);
                        btnStart.setVisibility(View.GONE);

                        Log.e("isCharging if false runOnUiThread", String.valueOf(isCharging));

                        if (cv_error.getVisibility() != View.VISIBLE) {
                            cv_error.setVisibility(View.VISIBLE);
                            btnPayWithCash.setEnabled(false);
                            btnPayWithQr.setEnabled(false);
                            tv_Message.setText("No Electricity please try after some time.");
                            lvAnimation.setAnimation(R.raw.no_electricity);
                            btnDone.setVisibility(View.GONE);
//                            btnStart.setVisibility(View.GONE);
                            btnDone.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    usbSerialCommunication.connect();
                                    usbSerialCommunication.setBaudRate(115200);

                                }
                            });
                        }
                    }
                });
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main2);


        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        preferencesManager = SharedPreferencesManager.getInstance(this);
        usbSerialCommunication = new UsbSerialCommunication(this);
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);

//        usbSerialCommunication.connect();
//        usbSerialCommunication.setBaudRate(115200);
//        sendInitialData();
//        preferencesManager.save(MilkBasePrice, "1.0");
//        preferencesManager.save(TemperatureOffSet, "0.0");
//        preferencesManager.save(MilkDensityPref, "1.0");
//        preferencesManager.save(TemperatureSet, "4.0");

        appDatabase = AppDatabase.getInstance(this);

        alertDialog = new AlertDialog.Builder(this).setTitle("No Electricity Connections").setMessage("Please check again after sometime. Thank You!").setCancelable(false).create();

        btnPayWithCash = findViewById(R.id.btnPayWithCash);
        btnPayWithQr = findViewById(R.id.btnPayWithQr);
        btnStart = findViewById(R.id.btnStart);
        btnDone = findViewById(R.id.btnDone);
        ivAgitator = findViewById(R.id.ivAgitator);
        ivCompressor = findViewById(R.id.ivCompressor);
        tvMilkBasePrice = findViewById(R.id.tvMilkBasePrice);
        tvTemperature = findViewById(R.id.tvTemperature);
        tv_Message = findViewById(R.id.tv_Message);
        llCash = findViewById(R.id.llPayCash);
        llQr = findViewById(R.id.llPayQR);
        lvStatus = findViewById(R.id.llStatus);
        llAlert = findViewById(R.id.llAlert);
        cv_error = findViewById(R.id.cv_error);
        lvAnimation = findViewById(R.id.lvAnimation);

        lvStatus.setVisibility(View.VISIBLE);
        cvPayWithCash = findViewById(R.id.cvPayWithCash);
        cvPayWithQr = findViewById(R.id.cvPayWithQR);

        cv_error.bringToFront();

       /* findViewById(R.id.ivLogo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PayWithQrActivity.class);
                startActivity(intent);
            }
        });*/

        llCash.setVisibility(View.GONE);
        llQr.setVisibility(View.GONE);
        cvPayWithQr.setVisibility(View.GONE);
        cvPayWithCash.setVisibility(View.GONE);

        cv_error.setVisibility(View.GONE);


        btnStart.setOnClickListener(v -> {

            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

/*
            Intent intent = new Intent(MainActivity.this, CashCollectorActivity.class);
            startActivity(intent);
*/

            final boolean[] isNextClicked = {false};
            LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
            View view = inflater.inflate(R.layout.dialog_add_beaker, null);
            preferencesManager = SharedPreferencesManager.getInstance(MainActivity.this);
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setView(view);

            AlertDialog dialog = builder.create();

            MaterialButton submitBtn = view.findViewById(R.id.btnNext);
            TextView tvTitle = view.findViewById(R.id.tvTitle);
            LottieAnimationView lottieAv = view.findViewById(R.id.lottiAv);
            lottieAv.setAnimation(R.raw.close_door);
            TextView tvMessage = view.findViewById(R.id.tvMessage);
            tvTitle.setText("Place the pot and close the door.");

            tvMessage.setVisibility(View.GONE);
            submitBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (preferencesManager.hasValue(Constants.ResponseTempStatus)) {
                        ResponseTempStatus responseTempStatus = new Gson().fromJson(preferencesManager.get(Constants.ResponseTempStatus, "").toString(), ResponseTempStatus.class);
                        if (responseTempStatus.getConnectivity() != null) {
                            if (!responseTempStatus.getConnectivity()) {
                                submitBtn.setText(getResources().getString(R.string.start));
                                dialog.dismiss();
                                usbSerialCommunication.connect();
                                usbSerialCommunication.setBaudRate(115200);
                                if (usbSerialCommunication.connected) {
                                    llCash.setVisibility(View.VISIBLE);
                                    llQr.setVisibility(View.VISIBLE);
                                    cvPayWithQr.setVisibility(View.VISIBLE);
                                    cvPayWithCash.setVisibility(View.VISIBLE);
                                    btnStart.setVisibility(View.GONE);
                                }
                            } else {
                                submitBtn.setText(getResources().getString(R.string.next));
                                //   Toast.makeText(MainActivity.this, "Please Close the Door " + responseTempStatus.getConnectivity(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }


                }
            });
            dialog.show();
            dialog.setCancelable(false);

            /*else {
                Toast.makeText(MainActivity.this, "Please wait checking for the Door status", Toast.LENGTH_SHORT).show();
                usbSerialCommunication.connect();
                usbSerialCommunication.setBaudRate(115200);
            }*/

        });

        btnPayWithCash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                usbSerialCommunication.fireOnStart(0);
//                usbSerialCommunication.disconnect();
                if (cv_error.getVisibility() == View.VISIBLE) {
                    return;
                }
                Intent intent = new Intent(MainActivity.this, CashCollectorActivity.class);
                startActivity(intent);
            }
        });
        btnPayWithQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cv_error.getVisibility() == View.VISIBLE) {
                    return;
                }

                Intent intent = new Intent(MainActivity.this, PayWithQrActivity.class);
                startActivity(intent);
            }
        });

        cvPayWithCash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnPayWithCash.performClick();
            }
        });

        cvPayWithQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnPayWithQr.performClick();
            }
        });
        usbSerialCommunication.setReadDataListener(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.context_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_Config) {

            //    AppDatabase instance = AppDatabase.getInstance(MainActivity.this);
            Executors.newSingleThreadExecutor().execute(() -> {
                User user = appDatabase.userDao().getUserByUserType(0);

                if (user == null) {
                    appDatabase.userDao().insert(new User("admin", "Mvb@idmc123", 0));

                }

            });
            // Handle edit action
            Constants.showLoginDialog(MainActivity.this, appDatabase);

            return true;
        }
        return super.onContextItemSelected(item);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onStart() {
        super.onStart();

        registerReceiver(usbPermissionReceiver, filter);
//        sendInitialData();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(usbPermissionReceiver);
//        unregisterReceiver(batteryReceiver);

    }

    @Override
    protected void onResume() {
        super.onResume();
//        registerReceiver(usbPermissionReceiver, filter);

        usbSerialCommunication.connect();
        usbSerialCommunication.setBaudRate(115200);

//        sendInitialData();

    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {

        unregisterReceiver(usbPermissionReceiver);
        unregisterReceiver(batteryReceiver);
        usbSerialCommunication.disconnect();
        super.onDestroy();
    }


    @Override
    public void onReadData(String data) {
        if (data != null) {
            if (data.contains("lowlevel")) {
                isShowError = true;
                ResponseTempStatus responseTempStatus = new Gson().fromJson(data, ResponseTempStatus.class);
                Log.e(TAG, "run: ==> onReadData: " + new Gson().toJson(responseTempStatus));
                if (responseTempStatus != null) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            String milkBasePrice = "₹ " + preferencesManager.get(MilkBasePrice, "0.0").toString() + "/Ltr";
                            String offsetTemp = preferencesManager.get(TemperatureOffSet, "0.0").toString();
                            double cTemp = Double.parseDouble(responseTempStatus.getTemperature().toString()) / 10 + Double.parseDouble(offsetTemp);
                            String currentTemp = Constants.df.format(cTemp) + " °C";

                            tvTemperature.setText(currentTemp);
                            tvMilkBasePrice.setText(milkBasePrice);
                            if (responseTempStatus.getAgitator() != null) {
                                ivAgitator.setBackground(responseTempStatus.getAgitator() ? getDrawable(R.drawable.red_circle) : getDrawable(R.drawable.green_circle));
                            }
                            if (responseTempStatus.getCompressor() != null) {
                                ivCompressor.setBackground(responseTempStatus.getCompressor() ? getDrawable(R.drawable.red_circle) : getDrawable(R.drawable.green_circle));
                            }
/*
                            if (responseTempStatus.getConnectivity() != null) {
                                if (responseTempStatus.getConnectivity()) {
                                    cv_error.setVisibility(View.VISIBLE);
                                    btnDone.setVisibility(View.GONE);
                                    btnStart.setVisibility(View.GONE);
                                    lvAnimation.setAnimation(R.raw.close_door);
                                    tv_Message.setText("Place the beaker and close the door.");
                                } else {
                                    cv_error.setVisibility(View.GONE);
                                    btnStart.setVisibility(View.VISIBLE);
                                    btnPayWithCash.setVisibility(View.VISIBLE);
                                    btnPayWithQr.setVisibility(View.VISIBLE);
                                    btnDone.setVisibility(View.GONE);
                                }
                            }
*/
                            if (responseTempStatus.getLowlevel() != null) {
                                if (responseTempStatus.getLowlevel()) {

                                    Log.e("low level", responseTempStatus.getLowlevel().toString());

                                    cv_error.setVisibility(View.VISIBLE);
                                    btnDone.setVisibility(View.GONE);
                                    btnStart.setVisibility(View.GONE);
                                    lvAnimation.setAnimation(R.raw.milk_loading);
                                    tv_Message.setText("Low Milk level Please wait till refill.");
                                    lvAnimation.setRepeatMode(LottieDrawable.RESTART);
//                                        lvAnimation.setProgress(Float.parseFloat("20.0"));


//                                    if (cv_error.getVisibility() != View.VISIBLE) {
//
//                                        Log.e("cv_error", "Not visible");
//
//
//                                        Log.e(TAG, "getLowlevel: True " + cv_error.getVisibility() + " ====> " + View.VISIBLE);
//                                        cv_error.setVisibility(View.VISIBLE);
//                                        btnDone.setVisibility(View.GONE);
//                                        btnStart.setVisibility(View.GONE);
//                                        lvAnimation.setAnimation(R.raw.milk_loading);
//                                        tv_Message.setText("Low Milk level Please wait till refill.");
//                                        lvAnimation.setRepeatMode(LottieDrawable.RESTART);
////                                        lvAnimation.setProgress(Float.parseFloat("20.0"));
//
//                                    } else {
//
//                                        Log.e("cv_error", "issssss visible");
//
//
//                                        cv_error.setVisibility(View.GONE);
//                                        btnStart.setVisibility(View.VISIBLE);
//                                        btnPayWithCash.setVisibility(View.VISIBLE);
//                                        btnPayWithQr.setVisibility(View.VISIBLE);
//                                        btnDone.setVisibility(View.GONE);
//
//                                    }
                                } else {
                                    cv_error.setVisibility(View.GONE);
//                                        btnStart.setVisibility(View.VISIBLE);
                                    btnPayWithCash.setVisibility(View.VISIBLE);
                                    btnPayWithQr.setVisibility(View.VISIBLE);
                                    btnDone.setVisibility(View.GONE);

                                    Log.e("low level is", responseTempStatus.getLowlevel().toString());
                                }
                            }
                        }
                    });

                }
            } /*else if (data.contains("-1")) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnStart.setVisibility(View.VISIBLE);
                        cv_error.setVisibility(View.VISIBLE);
                        tv_Message.setText("No Electricity please wait till 15 second once charger is connect.");
                        lvAnimation.setAnimation(R.raw.no_electricity);
                        btnDone.setVisibility(View.VISIBLE);
                        btnDone.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                usbSerialCommunication.connect();
                                usbSerialCommunication.setBaudRate(115200);
                                btnDone.setVisibility(View.GONE);
                            }
                        });

                    }
                });
            }*/

        } else {
            Log.e(TAG, "run:>> onReadData: " + isShowError);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void checkChargingState(Context context) {
        BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);

        if (batteryManager != null) {
            int batteryStatus = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS);

            if (batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING) {
                // Device is charging
                // Toast.makeText(context, "Charging", Toast.LENGTH_SHORT).show();
            } else {
                // Device is not charging
                // Toast.makeText(context, "Not Charging", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /*@Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }*/

    private void showConnectivityDialog(boolean isConnect) {

        if (!isConnect) {
            if (!alertDialog.isShowing()) {
                alertDialog.show();
            } else {
                alertDialog.dismiss();
            }
        } else {
            alertDialog.dismiss();
        }
    }


}