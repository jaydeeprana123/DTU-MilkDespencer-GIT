package com.imdc.milkdespencer;

import android.app.admin.DevicePolicyManager;


import static com.imdc.milkdespencer.common.Constants.KEY_APP_STATUS;
import static com.imdc.milkdespencer.common.Constants.KEY_ELECTRICITY;
import static com.imdc.milkdespencer.common.Constants.KEY_KIOSK;
import static com.imdc.milkdespencer.common.Constants.KEY_LOW_LEVEL;
import static com.imdc.milkdespencer.common.Constants.KEY_TRANSACTION_START_DATE;
import static com.imdc.milkdespencer.common.Constants.KEY_TRANSACTION_START_TIME;
import static com.imdc.milkdespencer.common.Constants.MinimumVolumeLimit;
import static com.imdc.milkdespencer.common.UsbSerialCommunication.isCipOn;
import static com.imdc.milkdespencer.common.UsbSerialCommunication.isLowLevel;

import static com.imdc.milkdespencer.common.Constants.CashTransactionMode;
import static com.imdc.milkdespencer.common.Constants.MilkBasePrice;
import static com.imdc.milkdespencer.common.Constants.RemainingVolumePref;
import static com.imdc.milkdespencer.common.Constants.TemperatureOffSet;
import static com.imdc.milkdespencer.common.Constants.doPostAsyncLogs;
import static com.imdc.milkdespencer.common.Constants.doPostAsyncTransactions;
import static com.imdc.milkdespencer.common.Constants.isNetworkAvailable;
import static com.imdc.milkdespencer.common.Constants.remainingVolume;
import static com.imdc.milkdespencer.common.UsbSerialCommunication.isSendDataStop;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import com.google.android.material.button.MaterialButton;

import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.imdc.milkdespencer.Workers.PeriodicWorkerForTemperatureApiCall;
import com.imdc.milkdespencer.common.Constants;
import com.imdc.milkdespencer.common.SharedPreferencesManager;
import com.imdc.milkdespencer.common.UsbSerialCommunication;
import com.imdc.milkdespencer.enums.ScreenEnum;
import com.imdc.milkdespencer.models.ResponseTempStatus;
import com.imdc.milkdespencer.models.SendToDeviceForCIP;
import com.imdc.milkdespencer.receivers.MyDeviceAdminReceiver;
import com.imdc.milkdespencer.roomdb.AppDatabase;
import com.imdc.milkdespencer.roomdb.entities.LogEntity;
import com.imdc.milkdespencer.roomdb.entities.TransactionEntity;
import com.imdc.milkdespencer.roomdb.entities.User;
import com.imdc.milkdespencer.roomdb.interfaces.LogDao;
import com.imdc.milkdespencer.roomdb.interfaces.TransactionDao;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements UsbSerialCommunication.ReadDataListener, UsbSerialCommunication.ReadDataForCIPListener {

    //    private FirebaseAnalytics mFirebaseAnalytics;

    private static final long MULTI_TAP_WINDOW_MS = 3000;

    private ConstraintLayout root;

    private int tapCount = 0;
    private final Handler tapWindowHandler = new Handler();
    private final Runnable resetTaps = () -> tapCount = 0;


    private boolean inMilkDispenseProcessLevel = false;

    private boolean isUsbPermissionGranted = false; // Flag for USB permission

    private boolean isKioskModeEnable = true; // Flag for USB permission

    private boolean getChargingState = true;

    private boolean getSerialData = false;

    private boolean getUsbShowState = false;

    private boolean isDischargeState = false;

    private boolean isScreenVisible = true;


    private static MainActivity instance = null;
    private boolean isLowMilkLevel = false;

    private static final String ACTION_USB_PERMISSION = "com.imdc.milkdespencer.USB_PERMISSION";
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int DELAY_TIME_MILLIS = 16000; // 16 seconds
    static SharedPreferencesManager preferencesManager;
    private final Handler handler = new Handler();
    Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
    IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
    LinearLayout llCash, llQr, lvStatus, llAlert;
    ImageView ivAgitator, ivCompressor;

    Button btnCrash;

    boolean isShowError = false;


    private Handler handlerProcessScreen = new Handler(); // Create a Handler instance
    private Runnable runnableProcessScreen; // Declare the Runnable

    //    private  UsbSerialManager usbSerialManager;
    private UsbSerialCommunication usbSerialCommunication;

    private String startBtnClickTime;
    private String startBtnClickDate;

    ///TODO: 1) Read Continuous data from Serial // { "temperature": "3.04",  should not be more than set Temperature divide by 10 and then add offset value
    //  "compressor": true, green and red indicators
    //  "agitator": false, green and red indicators
    //  "lowlevel": true, if true close the system and show the dialog low on Milk.}
    //  2) Add Agitator and the Compressor, Milk Rate to the right side of the IDMC logo.
    //  3) Admin Screen UI which can be used for the add user and configurations.
    //  4) API Calls ==> Transactions


    public final BroadcastReceiver usbPermissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !ACTION_USB_PERMISSION.equals(intent.getAction())) {
                return;
            }

            UsbDevice deviceFromIntent = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            boolean permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);

            if (permissionGranted && deviceFromIntent != null) {
                UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
                // Get fresh device reference
                HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
                UsbDevice freshDevice = deviceList.get(deviceFromIntent.getDeviceName());

                if (freshDevice != null && usbManager.hasPermission(freshDevice)) {
                    usbSerialCommunication.openConnection(freshDevice);  // safer to use fresh reference
                } else {
                    logError(TAG, "Device not found or permission missing.");
                }
            } else {
                logError(TAG, "USB permission denied or device is null.");
            }

            if (context != null) {
                try {
                    context.unregisterReceiver(this);
                } catch (IllegalArgumentException e) {
                    logError(TAG, "Receiver already unregistered: " + e.getMessage());
                }
            }
        }
    };


//    public final BroadcastReceiver usbPermissionReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            if (intent == null || !ACTION_USB_PERMISSION.equals(intent.getAction())) {
//                return;
//            }
//
//            final UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
//            final boolean permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
//
//            if (permissionGranted) {
//                if (usbDevice != null ) {
//                    // Set flag to true
//                    usbSerialCommunication.openConnection(usbDevice);
//                } else {
//                    logError(TAG, "USB device is null.");
//                }
//            } else {
//                logError(TAG, "USB permission denied.");
//            }
//
//            if (context != null) {
//                try {
//                    context.unregisterReceiver(this);
//                } catch (IllegalArgumentException e) {
//                    logError(TAG, "Receiver already unregistered: " + e.getMessage());
//
//                }
//            }
//        }
//    };


    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent == null || intent.getAction() == null) return;

            String action = intent.getAction();

            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                long currentTime = System.currentTimeMillis();

                if (isDetachCalled) {
                    return;
                }

                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null && device.getProductName() != null &&
                        device.getProductName().contains("CP2102")) {

                    if (!isDetachCalled) {
                        isDetachCalled = true;
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    logError("USB", "USB connected (Electricity BACK)");
                    // Only reset debounce flag, NOT electricity state
                    // Electricity state should only be reset when actual data with electricity=true is received
                    checkAndRequestUsbPermission(); // see below
                }
            }
        }
    };


    private void checkAndRequestUsbPermission() {
        logError("checkAndRequestUsbPermission", "Method");

        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        if (usbManager == null) {
            logError(TAG, "USB Manager is not available.");
            return;
        }

        // Get connected USB devices
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

        if (deviceList.isEmpty()) {
            Toast.makeText(MainActivity.this, "No USB devices connected.", Toast.LENGTH_SHORT).show();
            enableKioskMode();

            return;
        }


        boolean permissionGrantedForTargetDevice = true;

        for (UsbDevice device : deviceList.values()) {

            logError("device Namee", device.getDeviceName());
            logError("getManufacturerName", device.getManufacturerName());
            logError("getProductName", device.getProductName() + " " + usbManager.hasPermission(device));
            logError("product id", String.valueOf(device.getProductId()) + " " + usbManager.hasPermission(device));

            // FT232R USB UART

            if (!usbManager.hasPermission(device)) {

                safeExitKiosk();

                btnStart.setVisibility(View.GONE);
                getChargingState = false;
                // Register receiver before requesting permission
                IntentFilter filter = new IntentFilter("com.imdc.milkdespencer.USB_PERMISSION");
                try {
                    registerReceiver(usbPermissionReceiver, filter);
                } catch (IllegalArgumentException e) {
                    Log.w("USB", "Receiver was already registered.");
                }

                // Log electricity lost only once
                if (!isElectricityAlreadyLost && isDetachCalled) {
                    logError("ELECTRICITY_LOST", "Electricity lost due to USB detach or permission revoked");
                    isElectricityAlreadyLost = true;

                    markElectricityLost();
                }


                permissionGrantedForTargetDevice = false;
                showPermissionRequestUI(usbManager, device);
                break; // Stop checking further as one permission is not granted
            }
        }


        if (permissionGrantedForTargetDevice) {

            if(!isKioskModeEnable){
                enableKioskMode();
            }


            logError("permissionGrantedForTargetDevice", "true");
            // toastMessage("permissionGrantedForTargetDevice");
            isUsbPermissionGranted = true;

            if (!getSerialData) {
                handlePleaseWaitState();
            } else if (!getChargingState) {
                handleNotChargingState();
            }


        }
    }


    private void handleNotChargingState() {

        logError(TAG + "Battery Status", "Device is not charging.");

        runOnUiThread(() -> {
            updateUIForNotChargingState();
            logError(TAG + "visiblity Visible", "cv_error");

            cv_error.setVisibility(View.VISIBLE);
            btnDone.setVisibility(View.GONE);
//                    btnPayWithCash.setEnabled(false);
//                    btnPayWithQr.setEnabled(false);
            tv_Message.setText("No Electricity please try after some time.");
            lvAnimation.setAnimation(R.raw.no_electricity);
            btnStart.setVisibility(View.GONE);


        });
    }


    private void handlePleaseWaitState() {

        logError(TAG + "handlePleaseWaitState", "Please wait");

        runOnUiThread(() -> {
            updateUIForNotChargingState();
            logError(TAG + "visiblity Visible", "cv_error");
            btnStart.setVisibility(View.GONE);
            cv_error.setVisibility(View.VISIBLE);
            btnDone.setVisibility(View.GONE);
//                    btnPayWithCash.setEnabled(false);
//                    btnPayWithQr.setEnabled(false);
            tv_Message.setText("Please wait...");
            lvAnimation.setAnimation(R.raw.please_wait);
        });


        // Start 10s timeout for no electricity
        if (!isNoDataRunnableScheduled) {
            handlerNoData.postDelayed(noDataRunnable, 10 * 1000);
            isNoDataRunnableScheduled = true;
            logError(TAG, "Started 10s timer to check electricity presence");
        }

    }


    /*
     * Buttons visibility should be gone when not in charging*/
    private void updateUIForNotChargingState() {
//            llCash.setVisibility(View.GONE);
//            llQr.setVisibility(View.GONE);
        cvPayWithQr.setVisibility(View.GONE);
        cvPayWithCash.setVisibility(View.GONE);
        btnStart.setVisibility(View.GONE);
    }


    private void showPermissionRequestUI(UsbManager usbManager, UsbDevice device) {
        getUsbShowState = false;

        cv_error.setVisibility(View.VISIBLE);
        btnStart.setVisibility(View.GONE);
        btnDone.setVisibility(View.VISIBLE);
        btnDone.setText("GRANT PERMISSION");
        tv_Message.setText("USB permission is not granted");
        lvAnimation.setAnimation(R.raw.no_usb);

        btnDone.setOnClickListener(v -> checkAndRequestUsbPermission());

        // Request USB permission
        PendingIntent permissionIntent = PendingIntent.getBroadcast(
                MainActivity.this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
        );
        usbManager.requestPermission(device, permissionIntent);
    }

    private void handlePermissionGranted() {
        logError(TAG, "run:>> ! handlePermissionGranted: Please wait");
        tv_Message.setText("Please wait...");
        btnDone.setVisibility(View.GONE);
        lvAnimation.setAnimation(R.raw.please_wait);
        logError(TAG, "run:>> ! Permission is granted for all devices.");
        //   handleChargingState();
    }


    private Button btnPayWithCash, btnPayWithQr, btnStart, btnDone;
    private CardView cvPayWithCash, cvPayWithQr, cv_error;

    private TextView tvProcessing, tvPleasePutUtensil;

    private AppDatabase appDatabase;
    private TextView tvTemperature, tvMilkBasePrice, tv_Message, tvRemainingVolume;
    private LottieAnimationView lvAnimation;

    private double minimumVolumeLimit = 0.0;


    /*
     * Battery Charging Broad Cast Receiver*/
//    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            if (intent == null) return;
//
//            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
//            boolean isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL);
//            logError(TAG, "Battery Status " + " Charging: " + isCharging);
//            if (isCharging) {
//
//                if (!getChargingState) {
//                    getChargingState = true;
//
//                }
//                checkAndRequestUsbPermission();
//                if (isDischargeState) {
//                    isDischargeState = false;
//                }
//
//
//            } else {
//
//                /// Here only once save log as a lost electricity
//                if (!isDischargeState) {
//                    isDischargeState = true;
//                    Constants.saveLogs(MainActivity.this, "Lost Electricity");
//                }
//
//
//                inMilkDispenseProcessLevel = false;
//                getChargingState = false;
//                getUsbShowState = false;
//                isUsbPermissionGranted = false;
//                handleNotChargingState();
//            }
//        }
//
//
//        private void checkAndRequestUsbPermission() {
//            UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
//
//            if (usbManager == null) {
//                logError(TAG, "USB Manager is not available.");
//                return;
//            }
//
//            // Get connected USB devices
//            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
//
//            if (deviceList.isEmpty()) {
//                Toast.makeText(MainActivity.this, "No USB devices connected.", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//
//            boolean permissionGrantedForTargetDevice = true;
//
//
//            for (UsbDevice device : deviceList.values()) {
//                if (!usbManager.hasPermission(device)) {
//
//                    // Register receiver before requesting permission
//                    IntentFilter filter = new IntentFilter("com.imdc.milkdespencer.USB_PERMISSION");
//                    try {
//                        registerReceiver(usbPermissionReceiver, filter);
//                    } catch (IllegalArgumentException e) {
//                        Log.w("USB", "Receiver was already registered.");
//                    }
//
//
//                    permissionGrantedForTargetDevice = false;
//                    showPermissionRequestUI(usbManager, device);
//                    break; // Stop checking further as one permission is not granted
//                }
//            }
//
//
//            if (permissionGrantedForTargetDevice) {
//
//                logError("permissionGrantedForTargetDevice", "true22");
//
//                if (!isUsbPermissionGranted) {
//                    isUsbPermissionGranted = true;
//                    handleNotChargingState();
//                }
//            }
//        }
//
//        private void showPermissionRequestUI(UsbManager usbManager, UsbDevice device) {
//            getUsbShowState = false;
//
//            cv_error.setVisibility(View.VISIBLE);
//            btnStart.setVisibility(View.GONE);
//            btnDone.setVisibility(View.VISIBLE);
//            btnDone.setText("GRANT PERMISSION");
//            tv_Message.setText("USB permission is not granted");
//            lvAnimation.setAnimation(R.raw.no_usb);
//
//            btnDone.setOnClickListener(v -> checkAndRequestUsbPermission());
//
//            // Request USB permission
//            PendingIntent permissionIntent = PendingIntent.getBroadcast(
//                    MainActivity.this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
//            );
//            usbManager.requestPermission(device, permissionIntent);
//        }
//
//        private void handlePermissionGranted() {
//            logError(TAG, "run:>> ! handlePermissionGranted: Please wait");
//            tv_Message.setText("Please wait...");
//            btnDone.setVisibility(View.GONE);
//            lvAnimation.setAnimation(R.raw.please_wait);
//            logError(TAG, "run:>> ! Permission is granted for all devices.");
//            //   handleChargingState();
//        }
//
//
////        private void checkAndRequestUsbPermission() {
////
////            UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
////            if (usbManager == null) {
////                Log.e("USB", "USB Manager is not available.");
////                return;
////            }
////
////            // Get connected USB devices
////            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
////            if (deviceList.isEmpty()) {
////                Toast.makeText(MainActivity.this, "No USB devices connected.", Toast.LENGTH_SHORT).show();
////                return;
////            }
////
////            boolean isPermissionGive = true;
////
////            for (UsbDevice device : deviceList.values()) {
////                // Check if permission is already granted
////                if (usbManager.hasPermission(device)) {
////                    Log.d("USB", "Permission already granted for device: " + device.getDeviceName());
////                  //  Toast.makeText(MainActivity.this, "Permission already granted.", Toast.LENGTH_SHORT).show();
////
////
////                } else {
////
////                    getUsbShowState = false;
////
////                    isPermissionGive = false;
////                    cv_error.setVisibility(View.VISIBLE);
////                    btnDone.setVisibility(View.VISIBLE);
////                    btnDone.setText("GRANT PERMISSION");
////                    tv_Message.setText("USB permission is not granted");
////                    lvAnimation.setAnimation(R.raw.no_electricity);
////
////                    btnDone.setOnClickListener(v -> {
////                        checkAndRequestUsbPermission();
////                    });
////
////                    // Request permission
////                    PendingIntent permissionIntent = PendingIntent.getBroadcast(
////                            MainActivity.this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
////                    );
////                    usbManager.requestPermission(device, permissionIntent);
////                }
////
////                /// If permission is granted
////                if(isPermissionGive){
////                    tv_Message.setText("Please wait");
////                    btnDone.setVisibility(View.GONE);
////                    isUsbPermissionGranted = true;
////                    Log.e("Here usb permission", " is granted fully");
////                    handleChargingState();
////                }
////
////            }
////        }
//
//
//        /*
//         * If Battery is not in Charging State*/
//        private void handleNotChargingState() {
//            logError(TAG + "Battery Status", "Device is not charging.");
//
//            runOnUiThread(() -> {
//                updateUIForNotChargingState();
//
//                if (cv_error.getVisibility() != View.VISIBLE) {
//
//                    logError(TAG + "visiblity Visible", "cv_error");
//
//                    cv_error.setVisibility(View.VISIBLE);
//                    btnDone.setVisibility(View.GONE);
////                    btnPayWithCash.setEnabled(false);
////                    btnPayWithQr.setEnabled(false);
//                    tv_Message.setText("No Electricity please try after some time.");
//                    lvAnimation.setAnimation(R.raw.no_electricity);
//                    btnStart.setVisibility(View.GONE);
//
//                }
//            });
//        }
//
//        /*Update the UI when Device is in charging state*/
////        private void updateUIForChargingState() {
////
////           Log.e("updateUIForChargingState", "btnStart") ;
////
////           if(!inMilkDispenseProcessLevel){
////               btnStart.setVisibility(View.VISIBLE);
////           }else {
////               btnStart.setVisibility(View.GONE);
////           }
////
////            llCash.setVisibility(View.GONE);
////            llQr.setVisibility(View.GONE);
////            cvPayWithQr.setVisibility(View.GONE);
////            cvPayWithCash.setVisibility(View.GONE);
////
////        }
//
//
//        /*
//         * Buttons visibility should be gone when not in charging*/
//        private void updateUIForNotChargingState() {
////            llCash.setVisibility(View.GONE);
////            llQr.setVisibility(View.GONE);
//            cvPayWithQr.setVisibility(View.GONE);
//            cvPayWithCash.setVisibility(View.GONE);
//            btnStart.setVisibility(View.GONE);
//        }
//    };


    private boolean isDetachCalled = false;
    private boolean isElectricityAlreadyLost = false;


    private void markElectricityLost() {
        logError(TAG, "Electricity lost detected, saving log...");
        isElectricityAlreadyLost = true;
        getChargingState = false;
        isDischargeState = true;
        inMilkDispenseProcessLevel = false;
        getUsbShowState = false;
        isUsbPermissionGranted = false;

        try {
            Constants.saveLogs(getApplicationContext(), "Lost Electricity", KEY_ELECTRICITY);
        } catch (Exception e) {
            logError("LostElectricity", "Logging failed: " + e.getMessage());
        }


    }


    private final Handler handlerNoData = new Handler(Looper.getMainLooper());
    private boolean isNoDataRunnableScheduled = false;

    private final Runnable noDataRunnable = new Runnable() {
        @Override
        public void run() {
            logError(TAG, "No data received within timeout");
            getChargingState = false;
            handleNotChargingState();  // <-- Your custom method
            isNoDataRunnableScheduled = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        copyDatabase(this);


        /// Kiosk mode on
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
//            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//            if (am.getLockTaskModeState() == ActivityManager.LOCK_TASK_MODE_NONE) {
//                startLockTask();
//            }
//        }




        /// Start worker for api call on every 30 minutes for milk temperature send
        startWorkerForApiCallForTemperature();


        FirebaseApp.initializeApp(this);
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
// Initialize Firebase Analytics
        //   mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        keepScreenOn();
       // hideSystemUI();
        setContentView(R.layout.activity_main2);

        root = findViewById(R.id.root);

//        if(savedInstanceState == null){
//            // Apply immersive sticky immediately
//            enterImmersiveSticky();
//
//            // Re-apply immersive when system UI visibility changes (e.g., swipe-in)
//            root.setOnSystemUiVisibilityChangeListener(visibility -> {
//                // If bars became visible, re-hide them after a tiny delay
//                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
//                    root.postDelayed(this::enterImmersiveSticky, 200);
//                }
//            });
//
//
//            // (Optional) Try to start Lock Task / Screen Pinning
//            tryStartLockTask();
//        }


        instance = this;
        initializeDependencies(savedInstanceState);
        initializeUI();
        setupListeners();
    }


    private void safeExitKiosk() {

        isKioskModeEnable = false;

        // Stop Lock Task if running
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                stopLockTask();
            } catch (Exception ignored) {}
        }

        // Restore system UI (show back, home, status bar)
//        root.setSystemUiVisibility(
//                View.SYSTEM_UI_FLAG_VISIBLE
//        );

        // Optionally finish activity if you want to exit app
        // finish();
    }



    /*For to enable kiosk mode*/
    private void kioskModeEnableForDeviceOwner() {

        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, MyDeviceAdminReceiver.class);

        if (dpm.isDeviceOwnerApp(getPackageName())) {
            String[] packages = {getPackageName()};
            dpm.setLockTaskPackages(admin, packages); // whitelist kiosk app
            startLockTask(); // start kiosk mode
        } else {
            logError(TAG, "Not a device owner");
        }

    }


    private void startWorkerForApiCallForTemperature() {
        // 30-minute periodic request
        PeriodicWorkRequest apiWorkRequest =
                new PeriodicWorkRequest.Builder(PeriodicWorkerForTemperatureApiCall.class, 30, TimeUnit.MINUTES)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "CallApiEvery30Min",
                ExistingPeriodicWorkPolicy.KEEP, // Prevent duplicate scheduling
                apiWorkRequest
        );
    }


    /*Keep Screen On*/
    private void keepScreenOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


    /*Hide System UI*/
    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

//    private void hideSystemUI() {
//        View decorView = getWindow().getDecorView();
//        decorView.setSystemUiVisibility(
//                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                       );
//    }


    /*Initialize
    Shared Preference,
    Usb Serial Communication,
    Register Battery Receiver Broadcast,
    SQLite Database,
    Alert Dialog of Electricity*/
    private void initializeDependencies(Bundle savedInstanceState) {
        preferencesManager = SharedPreferencesManager.getInstance(getApplicationContext());
        remainingVolume = Float.parseFloat(preferencesManager.get(RemainingVolumePref, "0").toString());

        usbSerialCommunication = new UsbSerialCommunication(getApplicationContext());

        appDatabase = AppDatabase.getInstance(this);

        if (savedInstanceState == null && isNetworkAvailable(MainActivity.this)) {
            new Thread(() -> {
                try {
                    TransactionDao transactionDao = appDatabase.transactionDao();

                    // Get un-uploaded transactions
                    List<TransactionEntity> unUploadTransactionList = transactionDao.getUnUploadedTransactions();

                    for (TransactionEntity transaction : unUploadTransactionList) {
                        if (transaction.getMilkTemperature() == null || transaction.getMilkTemperature().isEmpty()) {
                            transaction.setMilkTemperature("111");
                        }
                    }

                    if (!unUploadTransactionList.isEmpty()) {
                        // Upload on the server
                        doPostAsyncTransactions(preferencesManager, "/api/Transaction/PostTransaction",
                                new ArrayList<>(unUploadTransactionList), transactionDao);
                    }

                    LogDao logDao = appDatabase.logDao();

                    // Get un-uploaded logs
                    List<LogEntity> unUploadLogsList = logDao.getUnUploadedLogs();

                    if (!unUploadLogsList.isEmpty()) {
                        // Upload on the server
                        doPostAsyncLogs(preferencesManager, "/api/Log/PostLog",
                                new ArrayList<>(unUploadLogsList), logDao);
                    }

                    // ✅ 3. Now save "App Started" log AFTER both APIs
                    Constants.saveLogs(getApplicationContext(), "App Started", KEY_APP_STATUS);
                } catch (Exception e) {
                    logError("App Started", "Exception: " + e.getMessage());
                }
            }).start();
        }
    }


    /*Initialize the ui...FInd View By Ids*/
    private void initializeUI() {
        btnPayWithCash = findViewById(R.id.btnPayWithCash);
        btnPayWithQr = findViewById(R.id.btnPayWithQr);
        btnStart = findViewById(R.id.btnStart);
        btnDone = findViewById(R.id.btnDone);
        ivAgitator = findViewById(R.id.ivAgitator);

        btnCrash = findViewById(R.id.btnCrash);

        btnCrash.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                throw new RuntimeException("Hello Crash"); // Force a crash

            }
        });


        ivCompressor = findViewById(R.id.ivCompressor);
        tvMilkBasePrice = findViewById(R.id.tvMilkBasePrice);
        tvTemperature = findViewById(R.id.tvTemperature);
        tvRemainingVolume = findViewById(R.id.tvRemainingVolume);
        tv_Message = findViewById(R.id.tv_Message);
        llCash = findViewById(R.id.llPayCash);
        llQr = findViewById(R.id.llPayQR);
        lvStatus = findViewById(R.id.llStatus);
        llAlert = findViewById(R.id.llAlert);
        cv_error = findViewById(R.id.cv_error);
        lvAnimation = findViewById(R.id.lvAnimation);
        cvPayWithCash = findViewById(R.id.cvPayWithCash);
        cvPayWithQr = findViewById(R.id.cvPayWithQR);

        tvProcessing = findViewById(R.id.tvProcessing);
        tvPleasePutUtensil = findViewById(R.id.tvPleasePutUtensil);

        setupInitialVisibility();
        cv_error.bringToFront();
    }

    private void setupInitialVisibility() {
//        llCash.setVisibility(View.GONE);
//        llQr.setVisibility(View.GONE);
        cvPayWithQr.setVisibility(View.GONE);
        cvPayWithCash.setVisibility(View.GONE);
        cv_error.setVisibility(View.GONE);
        logError(TAG + "visiblity Gone", "cv_error");
        lvStatus.setVisibility(View.VISIBLE);
    }

    /*Listeners*/
    private void setupListeners() {
        btnStart.setOnClickListener(v -> showStartDialog());
        btnPayWithCash.setOnClickListener(v -> onPayWithCash());
        btnPayWithQr.setOnClickListener(v -> onPayWithQr());
        cvPayWithCash.setOnClickListener(v -> btnPayWithCash.performClick());
        cvPayWithQr.setOnClickListener(v -> btnPayWithQr.performClick());
        usbSerialCommunication.setReadDataListener(this);
        usbSerialCommunication.setReadDataForCIPListener(this);
    }


    /*Show Start Button Dialog*/
    private void showStartDialog() {

        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        startBtnClickDate = dateFormatter.format(System.currentTimeMillis());
        startBtnClickTime = timeFormatter.format(System.currentTimeMillis());

        btnStart.setVisibility(View.GONE);
//        hideSystemUI();
        inMilkDispenseProcessLevel = true;
        tvPleasePutUtensil.setVisibility(View.GONE);
        tvProcessing.setVisibility(View.VISIBLE);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_beaker, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        MaterialButton submitBtn = dialogView.findViewById(R.id.btnNext);
        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        LottieAnimationView lottieAv = dialogView.findViewById(R.id.lottiAv);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);

        tvTitle.setText("Place the pot and close the door.");
        lottieAv.setAnimation(R.raw.close_door);
        tvMessage.setVisibility(View.GONE);

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logError(TAG, "run:>> next button click");

                handleDialogSubmit(dialog, submitBtn);
            }
        });
//        submitBtn.setOnClickListener(v -> handleDialogSubmit(dialog, submitBtn));
        dialog.show();

    }


    /*
     * Handle Start button dialog's Submit Button Even*/
    private void handleDialogSubmit(AlertDialog dialog, MaterialButton submitBtn) {

        /// When press next button visiblity will be off of the start button

        if (preferencesManager.hasValue(Constants.ResponseTempStatus)) {

            ResponseTempStatus responseTempStatus = new Gson().fromJson(preferencesManager.get(Constants.ResponseTempStatus, "").toString(), ResponseTempStatus.class);

            logError(TAG, "run:>> responseTempStatus" + preferencesManager.get(Constants.ResponseTempStatus, "").toString());

            if (responseTempStatus.getConnectivity() != null) {
                if (!responseTempStatus.getConnectivity()) {

                    logError(TAG, "run:>>" + (responseTempStatus.getConnectivity().toString()));

                    submitBtn.setText(getString(R.string.start));
                    dialog.dismiss();
                    logError(TAG, "run:>> handleDialogSubmit: ");
                    /// Here  user start the process of dispense. So start button will be gone
                    inMilkDispenseProcessLevel = true;
                    btnStart.setVisibility(View.GONE);
                    startUsbCommunication();
                } else {
                    submitBtn.setText(getString(R.string.next));
                }
            }
        }

    }

    /*Start USB Communication*/
    private void startUsbCommunication() {
        usbSerialCommunication.connect();
        usbSerialCommunication.setBaudRate(115200);

        if (usbSerialCommunication.connected) {

            logError(TAG, "run:>> usbSerialCommunication is connected: ");
            cvPayWithQr.setVisibility(View.VISIBLE);
//            llCash.setVisibility(View.VISIBLE);
//            llQr.setVisibility(View.VISIBLE);
            tvProcessing.setVisibility(View.GONE);
            tvPleasePutUtensil.setVisibility(View.GONE);
            if ((preferencesManager.get(CashTransactionMode, "0")).toString().equals("0")) {

                logError(TAG, "run:>> usbSerialCommunication ahi nai aayo: ");

                cvPayWithCash.setVisibility(View.VISIBLE);
            } else {
                logError(TAG, "run:>> usbSerialCommunication ahi GONE chhee: ");
                cvPayWithCash.setVisibility(View.GONE);
            }

            btnStart.setVisibility(View.GONE);

            // After 30 second Cash and UPI screen will be gone
            handlerProcessScreen = new Handler(Looper.getMainLooper());
            runnableProcessScreen = () -> {


                if(getChargingState && isUsbPermissionGranted){
                    btnStart.setVisibility(View.VISIBLE);
                }

//                llCash.setVisibility(View.GONE);
//                llQr.setVisibility(View.GONE);
                cvPayWithQr.setVisibility(View.GONE);
                cvPayWithCash.setVisibility(View.GONE);
            };

            // Post the Runnable with a delay
            handlerProcessScreen.postDelayed(runnableProcessScreen, 30 * 1000); // 30 minutes


        } else {
            logError(TAG, "run:>> usbSerialCommunication not connected: ");
            /// iF USB connection failed... Start button will be visible again
            btnStart.setVisibility(View.VISIBLE);
            tvProcessing.setVisibility(View.GONE);
            tvPleasePutUtensil.setVisibility(View.VISIBLE);
            usbSerialCommunication.connect();
            usbSerialCommunication.setBaudRate(115200);

        }
    }


    /*Go To CashCollector Screen*/
    private void onPayWithCash() {

        // Remove the Runnable from the Handler to avoid memory leaks
        if (handlerProcessScreen != null && runnableProcessScreen != null) {
            handlerProcessScreen.removeCallbacks(runnableProcessScreen);
        }

        if (cv_error.getVisibility() == View.VISIBLE) return;
        Intent intent = new Intent(this, CashCollectorActivity.class);
        intent.putExtra(KEY_TRANSACTION_START_DATE, startBtnClickDate);
        intent.putExtra(KEY_TRANSACTION_START_TIME, startBtnClickTime);
        startActivityForResult(intent, ScreenEnum.CASH_COLLECTOR.ordinal());
    }


    /*Go To Pay With QR Screen*/
    private void onPayWithQr() {

        // Remove the Runnable from the Handler to avoid memory leaks
        if (handlerProcessScreen != null && runnableProcessScreen != null) {
            handlerProcessScreen.removeCallbacks(runnableProcessScreen);
        }

        if (cv_error.getVisibility() == View.VISIBLE) return;
        Intent intent = new Intent(this, PayWithQrActivity.class);
        intent.putExtra(KEY_TRANSACTION_START_DATE, startBtnClickDate);
        intent.putExtra(KEY_TRANSACTION_START_TIME, startBtnClickTime);
        startActivityForResult(intent, ScreenEnum.PAY_WITH_QR.ordinal());
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

            Bundle bundle = new Bundle();
            bundle.putString("login_dialog", "open");
            //  mFirebaseAnalytics.logEvent("login_event", bundle);

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

        inMilkDispenseProcessLevel = false;
        tvProcessing.setVisibility(View.GONE);
        tvPleasePutUtensil.setVisibility(View.VISIBLE);
      //  hideSystemUI();
        //  registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        checkAndRequestUsbPermission();

//        registerReceiver(usbPermissionReceiver, filter);
//        registerReceiver(usbPermissionReceiver, filter);

//        sendInitialData();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Remove the Runnable from the Handler to avoid memory leaks
        if (handlerProcessScreen != null && runnableProcessScreen != null) {
            handlerProcessScreen.removeCallbacks(runnableProcessScreen);
        }

        try {
            unregisterReceiver(usbPermissionReceiver);
        } catch (IllegalArgumentException ignored) {}

//        unregisterReceiver(batteryReceiver);

    }


    @Override
    protected void onResume() {
        super.onResume();

        clearAllCache(getApplicationContext());

        isScreenVisible = true;
        isSendDataStop = false;
        isCipOn = false;
        DecimalFormat df = new DecimalFormat("0.00");
        String formattedRemainingVolume = df.format(remainingVolume);
        tvRemainingVolume.setText(formattedRemainingVolume + "L");

        // Ensure preferencesManager is initialized (do this in onCreate if possible)
        if (preferencesManager == null) {
            preferencesManager = SharedPreferencesManager.getInstance(getApplicationContext());
        }

        minimumVolumeLimit = Double.parseDouble(preferencesManager.get(MinimumVolumeLimit, "6.0").toString());

        // Delay the USB check slightly to ensure the device is fully ready
        new Handler(Looper.getMainLooper()).postDelayed(this::checkAndConnectUsbDevice, 700);


        // Register it in onCreate
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        registerReceiver(usbReceiver, filter);

    }

    private void checkAndConnectUsbDevice() {
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            Log.e(TAG, "UsbManager is null");
            return;
        }

        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

        for (UsbDevice device : deviceList.values()) {
            if (device.getVendorId() == 4292 && device.getProductId() == 60000) {
                if (usbManager.hasPermission(device)) {
                    logError(TAG, "USB permission granted, connecting...");
                    try {
                        usbSerialCommunication.connect();

                        if (getChargingState) {
                            usbSerialCommunication.setBaudRate(115200);
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Error connecting to USB device: " + e.getMessage(), e);
                    }
                } else {
                    logError(TAG, "Requesting USB permission...");
                    PendingIntent permissionIntent = PendingIntent.getBroadcast(
                            this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
                    );
                    usbManager.requestPermission(device, permissionIntent);
                }
                break;  // Found the matching device, exit loop
            }
        }
    }


    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onPause() {
        super.onPause();

        isScreenVisible = false;

        try {
            unregisterReceiver(usbReceiver);
        } catch (IllegalArgumentException e) {
            Log.e("usbReceiver", "Receiver not registered or already unregistered");
        }

    }

    @Override
    protected void onDestroy() {

        // Remove the Runnable from the Handler to avoid memory leaks
        if (handlerProcessScreen != null && runnableProcessScreen != null) {
            handlerProcessScreen.removeCallbacks(runnableProcessScreen);
        }


        try {
            if (usbPermissionReceiver != null) {

                logError(TAG, "run:>> ! USB permission unregister");
                unregisterReceiver(usbPermissionReceiver);
            }
        } catch (IllegalArgumentException e) {
            logError(TAG, "usbPermissionReceiver was already unregistered: " + e.getMessage());
        }

//        try {
//            if (batteryReceiver != null) {
//                logError("unregisterReceiver", "batteryReceiver");
//                unregisterReceiver(batteryReceiver);
//            }
//        } catch (IllegalArgumentException e) {
//            logError(TAG, "batteryReceiver was already unregistered: " + e.getMessage());
//        }


//        unregisterReceiver(usbPermissionReceiver);
//        unregisterReceiver(batteryReceiver);
        usbSerialCommunication.disconnect();
        super.onDestroy();
    }


    @Override
    public void onReadData(String data) {

        if (handlerNoData != null && noDataRunnable != null) {
            handlerNoData.removeCallbacks(noDataRunnable);
        }

        isNoDataRunnableScheduled = false;
        getSerialData = true;

        if (!isScreenVisible) {
            return; // Don't update UI or process data
        }

        getUsbShowState = true;

        if (data == null || !data.contains("lowlevel")) {
            logError(TAG, "run:>> onReadData: data is null or does not contain 'lowlevel'");
            return;
        }

        ResponseTempStatus responseTempStatus;
        try {
            responseTempStatus = new Gson().fromJson(data, ResponseTempStatus.class);
        } catch (Exception e) {
            logError(TAG, "Error parsing responseTempStatus " + e);
            return;
        }

        if (responseTempStatus == null) {
            logError(TAG, "responseTempStatus is null after parsing");
            return;
        }

        isShowError = true;

        logError(TAG, "run: ==> onReadData: " + new Gson().toJson(responseTempStatus));
        logError(TAG, "run: ==> getChargingState: " + getChargingState);
        logError(TAG, "run: ==> isUsbPermissionGranted: " + isUsbPermissionGranted);

        runOnUiThread(() -> {

            // If electricity is present, mark flag and cancel electricity lost check
            if (Boolean.TRUE.equals(responseTempStatus.getElectricity())) {
                // If electricity is present, mark flag and cancel electricity lost check

                isDetachCalled = false;
                // Reset electricity lost state when we receive electricity data
                if (isElectricityAlreadyLost) {
                    logError(TAG, "Resetting electricity lost state");
                    isElectricityAlreadyLost = false;
                }

                getChargingState = true;
                isDischargeState = false;
            } else {
                // No electricity in data - but don't immediately mark as lost
                // Let the timeout mechanism handle it
                getChargingState = false;

            }

            updateTemperatureAndPrice(responseTempStatus);
            updateIndicator(ivAgitator, responseTempStatus.getAgitator());
            updateIndicator(ivCompressor, responseTempStatus.getCompressor());

            if (getChargingState && isUsbPermissionGranted && !inMilkDispenseProcessLevel) {


                if (Boolean.TRUE.equals(responseTempStatus.getLowlevel())) {

                    Log.e(TAG, "minimumVolumeLimit: " + minimumVolumeLimit);
                    Log.e(TAG, "remainingVolume: " + remainingVolume);
                    /// Low level will be handle by minimum volume limit
                    if (remainingVolume <= minimumVolumeLimit) {
                        isLowLevel = true;
                        handleLowLevel();

                        if (!isLowMilkLevel) {
                            try {
                                Constants.saveLogs(getApplicationContext(), "Low Level", KEY_LOW_LEVEL);
                            } catch (Exception e) {
                                logError("PaymentLog", "Logging failed: ${e.message}");
                                // Don't crash, just log the error silently
                            }

                            isLowMilkLevel = true;

                        }

                    } else {
                        isLowLevel = false;
                        handleNormalLevel();
                    }

                } else {
                    isLowLevel = false;
                    handleNormalLevel();
                }
            } else if (!getChargingState) {
                logError(TAG, "Device is not charging");
                cv_error.setVisibility(View.VISIBLE);
                btnStart.setVisibility(View.GONE);
                btnDone.setVisibility(View.GONE);
                tv_Message.setText("No Electricity, please try after some time.");
                lvAnimation.setAnimation(R.raw.no_electricity);
            } else if (!isUsbPermissionGranted) {
                logError(TAG, "USB permission not granted");
                cv_error.setVisibility(View.VISIBLE);
                btnStart.setVisibility(View.GONE);
                btnDone.setVisibility(View.VISIBLE);
                btnDone.setText("GRANT PERMISSION");
                tv_Message.setText("USB permission is not granted");
                lvAnimation.setAnimation(R.raw.no_usb);
            } else {
                cv_error.setVisibility(View.GONE);
            }

            // Optional: if you want to handle "please wait" scenario
//        else if (!getUsbShowState && isUsbPermissionGranted) {
//            logError(TAG, "Please wait..");
//            cv_error.setVisibility(View.VISIBLE);
//            btnDone.setVisibility(View.GONE);
//            tv_Message.setText("Please wait..");
//            lvAnimation.setAnimation(R.raw.please_wait);
//            btnStart.setVisibility(View.GONE);
//        }
        });
    }


    @Override
    public void onReadCIPData(String data) {

        if (!isScreenVisible) {
            return; // Don't update UI or process data
        }

        if (data != null && data.contains("Inside CIP loop")) {
            logError(TAG + " receivedData INN: ", data);

            if (!isCipOn) {
                sendDataForCIP(false, false, false, true);
            }


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
            //  logError(TAG, "QR_PAYMENT: SEND COMMAND " + gson.toJson(sendToDevice));

            /// Check that usb serial is not null
            if (usbSerialCommunication != null) {
                usbSerialCommunication.sendData(gson.toJson(sendToDevice));

                usbSerialCommunication.connect();
                usbSerialCommunication.setBaudRate(115200);

            } else {
                usbSerialCommunication = new UsbSerialCommunication(getApplicationContext());
                usbSerialCommunication.connect();
                usbSerialCommunication.setBaudRate(115200);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // Check that temperature value should not null
    private void updateTemperatureAndPrice(ResponseTempStatus responseTempStatus) {
        // Get the base price for milk
        String milkBasePrice = "₹ " + preferencesManager.get(MilkBasePrice, "0.0").toString() + "/Ltr";

        // Get the temperature offset from preferences
        String offsetTemp = preferencesManager.get(TemperatureOffSet, "0.0").toString();

        // Get the temperature value as a String
        String temperatureStr = responseTempStatus.getTemperature() != null ? responseTempStatus.getTemperature().toString() : null;

        // Check if the temperature string is valid and parse it
        double temperature = 0.0; // Default value if invalid or null
        if (temperatureStr != null) {
            try {
                temperature = Double.parseDouble(temperatureStr); // Attempt to parse it as a Double
            } catch (NumberFormatException e) {
                logError(TAG, "Invalid temperature format: " + temperatureStr + ", setting to default 0.0");
                // Handle invalid temperature format
                temperature = 0.0; // Fallback value
            }
        } else {
            logError(TAG, "Temperature is null, setting to default 0.0");
        }

        // Calculate the final temperature after applying the offset
        double offset = 0.0;
        try {
            offset = Double.parseDouble(offsetTemp);
        } catch (NumberFormatException e) {
            logError(TAG, "Invalid offset temperature format: " + offsetTemp);
        }
        double cTemp = temperature / 10 + offset;

        // Format the temperature to a string with the required format
        String currentTemp = Constants.df.format(cTemp) + " °C";

        // Update the UI with the temperature and milk base price
        tvTemperature.setText(currentTemp);
        tvMilkBasePrice.setText(milkBasePrice);
    }


//    private void updateTemperatureAndPrice(ResponseTempStatus responseTempStatus) {
//        String milkBasePrice = "₹ " + preferencesManager.get(MilkBasePrice, "0.0").toString() + "/Ltr";
//        String offsetTemp = preferencesManager.get(TemperatureOffSet, "0.0").toString();
//        double cTemp = Double.parseDouble(responseTempStatus.getTemperature().toString()) / 10 + Double.parseDouble(offsetTemp);
//        String currentTemp = Constants.df.format(cTemp) + " °C";
//
//        tvTemperature.setText(currentTemp);
//        tvMilkBasePrice.setText(milkBasePrice);
//    }

    private void updateIndicator(ImageView imageView, Boolean status) {
        if (status != null) {
            int drawableId = status ? R.drawable.red_circle : R.drawable.green_circle;
            imageView.setBackground(getDrawable(drawableId));
        }
    }


    /*If Level is low then
     * Low Milk level Screen Will be Visible */
    private void handleLowLevel() {
        logError(TAG, "low level");
        cv_error.setVisibility(View.VISIBLE);
        btnDone.setVisibility(View.GONE);
        btnStart.setVisibility(View.GONE);
        lvAnimation.setAnimation(R.raw.milk_loading);
        tv_Message.setText("Low Milk level. Please wait till refill.");
        lvAnimation.setRepeatMode(LottieDrawable.RESTART);
    }


    /*If Level is Normal then
     * Low Milk level Screen Will be Hide And Buttons Will be Visible */
    private void handleNormalLevel() {

        if (isLowMilkLevel) {
            isLowMilkLevel = false;
        }

        // Hide error and show main payment buttons
        cv_error.setVisibility(View.GONE);
        btnPayWithCash.setVisibility(View.VISIBLE);
        btnPayWithQr.setVisibility(View.VISIBLE);
        btnDone.setVisibility(View.GONE);

        logError(TAG, "run: ==> Normal level");

        if (cvPayWithQr.getVisibility() == View.GONE) {
            logError(TAG, "run: ==> cvPayWithQr is GONE");

            // Reset UI to initial state when QR section is hidden
            btnStart.setVisibility(View.VISIBLE);
//            llCash.setVisibility(View.GONE);
//            llQr.setVisibility(View.GONE);
            cvPayWithQr.setVisibility(View.GONE);
            cvPayWithCash.setVisibility(View.GONE);

        } else if (cvPayWithQr.getVisibility() == View.VISIBLE) {
            logError(TAG, "run: ==> cvPayWithQr is VISIBLE");

            // If QR section is visible, don't show Start button again
            btnStart.setVisibility(View.GONE);
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


    /// When user comes from the screen
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode == ScreenEnum.CASH_COLLECTOR.ordinal() || requestCode == ScreenEnum.PAY_WITH_QR.ordinal()) && resultCode == RESULT_OK) {
            // Retrieve the data from the intent

            logError(TAG + " Here I come", "in Main Activity");

            if(getChargingState && isUsbPermissionGranted){
                btnStart.setVisibility(View.VISIBLE);
            }

//            llCash.setVisibility(View.GONE);
//            llQr.setVisibility(View.GONE);
            cvPayWithQr.setVisibility(View.GONE);
            cvPayWithCash.setVisibility(View.GONE);


//            if (data.hasExtra("FromScreen")) {
//
//                Log.e("FromScreen", "FromScreen");
//
//                btnStart.setVisibility(View.VISIBLE);
//                llCash.setVisibility(View.GONE);
//                llQr.setVisibility(View.GONE);
//                cvPayWithQr.setVisibility(View.GONE);
//                cvPayWithCash.setVisibility(View.GONE);
//            }
        }
    }


    private void logError(String tag, String message) {
        // Log.e(tag, message);
    }

    private void toastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }


    public void clearAllCache(Context context) {
        deleteDir(context.getCacheDir()); // Internal cache
        if (context.getExternalCacheDir() != null) {
            deleteDir(context.getExternalCacheDir()); // External cache
        }
    }


    public void clearAppCache(Context context) {
        try {
            File cacheDir = context.getCacheDir();
            deleteDir(cacheDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }


    private void showExitPinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter PIN to Exit");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String enteredPin = input.getText().toString();
            if (enteredPin.equals("1234")) { // Your actual PIN
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    stopLockTask(); // Exit kiosk
                }
                finish(); // Or navigate out
            } else {
                Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }






    private void enterImmersiveSticky() {
        int flags =
                 View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

        root.setSystemUiVisibility(flags);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enterImmersiveSticky();
        }
    }

    @Override
    public void onBackPressed() {
        // Block back in kiosk mode
        // super.onBackPressed(); // Intentionally disabled
    }

    private void tryStartLockTask() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                // If your app is device owner or whitelisted, this will silently start.
                // Otherwise Android will show the “Start screen pinning?” prompt to the user.
                startLockTask();
            } catch (Exception ignored) { }
        }
    }


    private void enableKioskMode(){

        isKioskModeEnable = true;
        // Apply immersive sticky immediately
        enterImmersiveSticky();

        // Re-apply immersive when system UI visibility changes (e.g., swipe-in)
        root.setOnSystemUiVisibilityChangeListener(visibility -> {
            // If bars became visible, re-hide them after a tiny delay
            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                root.postDelayed(this::enterImmersiveSticky, 200);
            }
        });


        // (Optional) Try to start Lock Task / Screen Pinning
        tryStartLockTask();

        try {
            Constants.saveLogs(getApplicationContext(), "Entered Kisok Mode", KEY_KIOSK);
        } catch (Exception e) {
//            logError("PaymentLog", "Logging failed: ${e.message}");
            // Don't crash, just log the error silently
        }

    }


}