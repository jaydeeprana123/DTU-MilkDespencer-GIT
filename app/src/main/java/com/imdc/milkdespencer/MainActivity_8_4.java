package com.imdc.milkdespencer;

import static com.imdc.milkdespencer.common.Constants.CashTransactionMode;
import static com.imdc.milkdespencer.common.Constants.KEY_ELECTRICITY;
import static com.imdc.milkdespencer.common.Constants.MilkBasePrice;
import static com.imdc.milkdespencer.common.Constants.TemperatureOffSet;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.imdc.milkdespencer.enums.ScreenEnum;
import com.imdc.milkdespencer.models.ResponseTempStatus;
import com.imdc.milkdespencer.roomdb.AppDatabase;
import com.imdc.milkdespencer.roomdb.entities.User;

import java.util.HashMap;
import java.util.concurrent.Executors;

public class MainActivity_8_4 extends AppCompatActivity implements UsbSerialCommunication.ReadDataListener {

 //   private FirebaseAnalytics mFirebaseAnalytics;
    private boolean inMilkDispenseProcessLevel = false;

    private boolean isUsbPermissionGranted = false; // Flag for USB permission
    private boolean getChargingState = false;
    private boolean getUsbShowState = false;

    private boolean isDischargeState = false;


    private boolean isLowMilkLevel = false;


    private static final String ACTION_USB_PERMISSION = "com.imdc.milkdespencer.USB_PERMISSION";
    private static final String TAG = MainActivity_8_4.class.getSimpleName();
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

            final UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            final boolean permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);

            if (permissionGranted) {
                if (usbDevice != null) {
                    // Set flag to true
                    usbSerialCommunication.openConnection(usbDevice);
                } else {
                    Log.e(TAG, "USB device is null.");
                }
            } else {
                Log.e(TAG, "USB permission denied.");
            }

            if (context != null) {
                try {
                    context.unregisterReceiver(this);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Receiver already unregistered: " + e.getMessage());
                }
            }
        }
    };

    private Button btnPayWithCash, btnPayWithQr, btnStart, btnDone;
    private CardView cvPayWithCash, cvPayWithQr, cv_error;

    private TextView tvProcessing;

    private AppDatabase appDatabase;
    private TextView tvTemperature, tvMilkBasePrice, tv_Message;
    private LottieAnimationView lvAnimation;


    /*
     * Battery Charging Broad Cast Receiver*/
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;

            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL);
            Log.e("Battery Status", "Charging: " + isCharging);

            if (isCharging) {
                if (!getChargingState) getChargingState = true;
                if (isDischargeState) isDischargeState = false;

                checkAndRequestUsbPermission();
            } else {
                if (!isDischargeState) {
                    isDischargeState = true;
                    Constants.saveLogs(MainActivity_8_4.this, "Lost Electricity", KEY_ELECTRICITY);
                }

                inMilkDispenseProcessLevel = false;
                getChargingState = false;
                getUsbShowState = false;
                isUsbPermissionGranted = false;
                handleNotChargingState();
            }
        }


        private void checkAndRequestUsbPermission() {

            Log.e("USB", "checkAndRequestUsbPermission");
            UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

            if (usbManager == null) {

                Log.e("USB", "USB Manager is not available.");
                return;
            }

            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

            if (deviceList.isEmpty()) {
                Toast.makeText(MainActivity_8_4.this, "No USB devices connected.", Toast.LENGTH_LONG).show();
                Log.e("USB", "No USB devices connected..");
                Log.e(TAG, "run:>> No USB devices connected..");
                                return;
            }

            Log.e("device list length", String.valueOf(deviceList.size()));

            boolean allPermissionsGranted = true;

            for (UsbDevice device : deviceList.values()) {

                if (device.getVendorId() == 4292 && device.getProductId() == 60000) {
                    if (!usbManager.hasPermission(device)) {
                        allPermissionsGranted = false;
                        showPermissionRequestUI(usbManager, device);
                        return; // Exit early if permission not granted
                    }
                }


            }



            if (allPermissionsGranted) {
                // All permissions granted
                if (getChargingState && !isUsbPermissionGranted) {
                    isUsbPermissionGranted = true;
                    handlePermissionGranted();
                }

                // Register the receiver once all permissions are granted
                registerReceiver(usbPermissionReceiver, filter);
            }


        }


        private void showPermissionRequestUI(UsbManager usbManager, UsbDevice device) {
            getUsbShowState = false;

            cv_error.setVisibility(View.VISIBLE);
            updateUIForUsbPermission(
                    "USB permission is not granted",
                    R.raw.no_usb,
                    "GRANT PERMISSION",
                    v -> checkAndRequestUsbPermission()
            );

            // Request USB permission
            PendingIntent permissionIntent = PendingIntent.getBroadcast(
                    MainActivity_8_4.this, 0,
                    new Intent(ACTION_USB_PERMISSION),
                    PendingIntent.FLAG_IMMUTABLE
            );
            usbManager.requestPermission(device, permissionIntent);
        }

        private void handlePermissionGranted() {
            Log.e(TAG, "Permission is granted for all devices.");
            updateUIForUsbPermission(
                    "Please wait...",
                    R.raw.please_wait,
                    null,
                    null
            );
            btnDone.setVisibility(View.GONE);
        }


        // ✅ Helper to update UI for USB permission states
        private void updateUIForUsbPermission(String message, int animationRes, String btnText, View.OnClickListener clickListener) {
           // cv_error.setVisibility(View.VISIBLE);
            tv_Message.setText(message);
            lvAnimation.setAnimation(animationRes);
            btnStart.setVisibility(View.GONE);

            if (btnText != null && clickListener != null) {
                btnDone.setVisibility(View.VISIBLE);
                btnDone.setText(btnText);
                btnDone.setOnClickListener(clickListener);
            } else {
                btnDone.setVisibility(View.GONE);
            }
        }


        /*
         * If Battery is not in Charging State*/
        private void handleNotChargingState() {
            Log.e("Battery Status", "Device is not charging.");

            runOnUiThread(() -> {

                llCash.setVisibility(View.GONE);
                llQr.setVisibility(View.GONE);
                cvPayWithQr.setVisibility(View.GONE);
                cvPayWithCash.setVisibility(View.GONE);
                btnStart.setVisibility(View.GONE);

                cv_error.setVisibility(View.VISIBLE);
                btnDone.setVisibility(View.GONE);
                btnPayWithCash.setEnabled(false);
                btnPayWithQr.setEnabled(false);
                tv_Message.setText("No Electricity please try after some time.");
                lvAnimation.setAnimation(R.raw.no_electricity);

            });
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // copyDatabase(this);
//        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
//// Initialize Firebase Analytics
//        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        keepScreenOn();
        hideSystemUI();
        setContentView(R.layout.activity_main2);

        initializeDependencies();
        initializeUI();
        setupListeners();
    }


    /*Keep Screen On*/
    private void keepScreenOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


    /*Hide System UI*/
    private void hideSystemUI() {
       // getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }


    /*Initialize
    Shared Preference,
    Usb Serial Communication,
    Register Battery Receiver Broadcast,
    SQLite Database,
    */
    private void initializeDependencies() {
        preferencesManager = SharedPreferencesManager.getInstance(this);
        usbSerialCommunication = new UsbSerialCommunication(this);

        appDatabase = AppDatabase.getInstance(this);
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

        setupInitialVisibility();
        cv_error.bringToFront();
    }

    private void setupInitialVisibility() {
        llCash.setVisibility(View.GONE);
        llQr.setVisibility(View.GONE);
        cvPayWithQr.setVisibility(View.GONE);
        cvPayWithCash.setVisibility(View.GONE);
        cv_error.setVisibility(View.GONE);
        Log.e("visiblity Gone", "cv_error");
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
    }


    /*Show Start Button Dialog*/
    private void showStartDialog() {

        hideSystemUI();
        inMilkDispenseProcessLevel = true;
        btnStart.setVisibility(View.GONE);
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
                Log.e(TAG, "run:>> next button click");

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

            Log.e(TAG, "run:>> responseTempStatus" + preferencesManager.get(Constants.ResponseTempStatus, "").toString());

            if (responseTempStatus.getConnectivity() != null) {
                if (!responseTempStatus.getConnectivity()) {

                    Log.e(TAG, "run:>>" + (responseTempStatus.getConnectivity().toString()));

                    submitBtn.setText(getString(R.string.start));
                    dialog.dismiss();
                    Log.e(TAG, "run:>> handleDialogSubmit: ");
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

            Log.e(TAG, "run:>> usbSerialCommunication is connected: ");
            cvPayWithQr.setVisibility(View.VISIBLE);
            llCash.setVisibility(View.VISIBLE);
            llQr.setVisibility(View.VISIBLE);
            tvProcessing.setVisibility(View.GONE);

            if ((preferencesManager.get(CashTransactionMode, "0")).toString().equals("0")) {

                Log.e(TAG, "run:>> usbSerialCommunication ahi nai aayo: ");

                cvPayWithCash.setVisibility(View.VISIBLE);
            } else {
                Log.e(TAG, "run:>> usbSerialCommunication ahi GONE chhee: ");
                cvPayWithCash.setVisibility(View.GONE);
            }

            btnStart.setVisibility(View.GONE);

            // After 30 second Cash and UPI screen will be gone
            handlerProcessScreen = new Handler(Looper.getMainLooper());
            runnableProcessScreen = () -> {

                btnStart.setVisibility(View.VISIBLE);
                llCash.setVisibility(View.GONE);
                llQr.setVisibility(View.GONE);
                cvPayWithQr.setVisibility(View.GONE);
                cvPayWithCash.setVisibility(View.GONE);
            };

            // Post the Runnable with a delay
            handlerProcessScreen.postDelayed(runnableProcessScreen, 30 * 1000); // 30 minutes


        } else {
            Log.e(TAG, "run:>> usbSerialCommunication not connected: ");
            /// iF USB connection failed... Start button will be visible again
            btnStart.setVisibility(View.VISIBLE);
            tvProcessing.setVisibility(View.GONE);
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
            Constants.showLoginDialog(MainActivity_8_4.this, appDatabase);

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
        hideSystemUI();
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        registerReceiver(usbPermissionReceiver, filter);
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

        // Remove the Runnable from the Handler to avoid memory leaks
        if (handlerProcessScreen != null && runnableProcessScreen != null) {
            handlerProcessScreen.removeCallbacks(runnableProcessScreen);
        }


        try {
            if (usbPermissionReceiver != null) {
                unregisterReceiver(usbPermissionReceiver);
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "usbPermissionReceiver was already unregistered: " + e.getMessage());
        }

        try {
            if (batteryReceiver != null) {
                Log.e("unregisterReceiver", "batteryReceiver");
                unregisterReceiver(batteryReceiver);
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "batteryReceiver was already unregistered: " + e.getMessage());
        }


//        unregisterReceiver(usbPermissionReceiver);
//        unregisterReceiver(batteryReceiver);
        usbSerialCommunication.disconnect();
        super.onDestroy();
    }


    @Override
    public void onReadData(String data) {
        getUsbShowState = true;

        if (data == null || !data.contains("lowlevel")) {
            Log.e(TAG, "onReadData: data is null or does not contain 'lowlevel'");
            return;
        }

        ResponseTempStatus responseTempStatus = parseTempStatus(data);
        if (responseTempStatus == null) return;

        isShowError = true;

        Log.e(TAG, "onReadData: " + new Gson().toJson(responseTempStatus));
        Log.e(TAG, "getChargingState: " + getChargingState);
        Log.e(TAG, "isUsbPermissionGranted: " + isUsbPermissionGranted);

        runOnUiThread(() -> {
            updateTemperatureAndPrice(responseTempStatus);
            updateIndicator(ivAgitator, responseTempStatus.getAgitator());
            updateIndicator(ivCompressor, responseTempStatus.getCompressor());

            if (getChargingState && isUsbPermissionGranted && !inMilkDispenseProcessLevel) {
                if (Boolean.TRUE.equals(responseTempStatus.getLowlevel())) {
                    handleLowLevel();
                } else {
                    handleNormalLevel();
                }
            } else {
                handleErrorStates();
            }
        });
    }


    private void handleErrorStates() {
        cv_error.setVisibility(View.VISIBLE);
        btnStart.setVisibility(View.GONE);

        if (!getChargingState) {
            Log.e(TAG, "Device is not charging");
            btnDone.setVisibility(View.GONE);
            tv_Message.setText("No Electricity, please try after some time.");
            lvAnimation.setAnimation(R.raw.no_electricity);
        } else if (!isUsbPermissionGranted) {
            Log.e(TAG, "USB permission not granted");
            btnDone.setVisibility(View.VISIBLE);
            btnDone.setText("GRANT PERMISSION");
            tv_Message.setText("USB permission is not granted");
            lvAnimation.setAnimation(R.raw.no_usb);
        }
    }


    private ResponseTempStatus parseTempStatus(String data) {
        try {
            return new Gson().fromJson(data, ResponseTempStatus.class);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing ResponseTempStatus", e);
            return null;
        }
    }

    private void updateTemperatureAndPrice(ResponseTempStatus responseTempStatus) {
        String milkBasePrice = "₹ " + preferencesManager.get(MilkBasePrice, "0.0").toString() + "/Ltr";
        String offsetTemp = preferencesManager.get(TemperatureOffSet, "0.0").toString();
        double cTemp = Double.parseDouble(responseTempStatus.getTemperature().toString()) / 10 + Double.parseDouble(offsetTemp);
        String currentTemp = Constants.df.format(cTemp) + " °C";

        tvTemperature.setText(currentTemp);
        tvMilkBasePrice.setText(milkBasePrice);
    }

    private void updateIndicator(ImageView imageView, Boolean status) {
        if (status != null) {
            int drawableId = status ? R.drawable.red_circle : R.drawable.green_circle;
            imageView.setBackground(getDrawable(drawableId));
        }
    }


    /*If Level is low then
     * Low Milk level Screen Will be Visible */
    private void handleLowLevel() {
        isLowMilkLevel = true;
        Log.e(TAG, "Low Milk Level");

        cv_error.setVisibility(View.VISIBLE);
        btnStart.setVisibility(View.GONE);
        btnDone.setVisibility(View.GONE);
        tv_Message.setText("Low Milk level. Please wait till refill.");
        lvAnimation.setAnimation(R.raw.milk_loading);
        lvAnimation.setRepeatMode(LottieDrawable.RESTART);
    }


    /*If Level is Normal then
     * Low Milk level Screen Will be Hide And Buttons Will be Visible */
    private void handleNormalLevel() {
        isLowMilkLevel = false;
        Log.e(TAG, "Normal Milk Level");

        cv_error.setVisibility(View.GONE);
        btnPayWithCash.setVisibility(View.VISIBLE);
        btnPayWithQr.setVisibility(View.VISIBLE);
        btnDone.setVisibility(View.GONE);

        if (cvPayWithQr.getVisibility() == View.GONE) {
            Log.e(TAG, "cvPayWithQr is GONE");
            btnStart.setVisibility(View.VISIBLE);
            llCash.setVisibility(View.GONE);
            llQr.setVisibility(View.GONE);
            cvPayWithQr.setVisibility(View.GONE);
            cvPayWithCash.setVisibility(View.GONE);
        } else {
            Log.e(TAG, "cvPayWithQr is VISIBLE");
            btnStart.setVisibility(View.GONE);
        }
    }

    /// When user comes from the screen
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode == ScreenEnum.CASH_COLLECTOR.ordinal() || requestCode == ScreenEnum.PAY_WITH_QR.ordinal()) && resultCode == RESULT_OK) {
            // Retrieve the data from the intent


            Log.e("Here I come", "in Main Activity");
            btnStart.setVisibility(View.VISIBLE);
            llCash.setVisibility(View.GONE);
            llQr.setVisibility(View.GONE);
            cvPayWithQr.setVisibility(View.GONE);
            cvPayWithCash.setVisibility(View.GONE);

        }
    }


}