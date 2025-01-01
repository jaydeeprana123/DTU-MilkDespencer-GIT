package com.imdc.milkdespencer;

import static com.imdc.milkdespencer.common.Constants.FromScreen;
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
import com.imdc.milkdespencer.enums.ScreenEnum;
import com.imdc.milkdespencer.models.ResponseTempStatus;
import com.imdc.milkdespencer.roomdb.AppDatabase;
import com.imdc.milkdespencer.roomdb.entities.User;

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
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !ACTION_USB_PERMISSION.equals(intent.getAction())) {
                return;
            }

            final UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            final boolean permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);

            if (permissionGranted) {
                if (usbDevice != null) {
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
                handleChargingState();
            } else {
                handleNotChargingState();
            }
        }


        /*
        * If Battery is in Charging Or Fully Charged State*/
        private void handleChargingState() {
            Log.e("Battery Status", "Device is charging.");

            handler.postDelayed(() -> {

                /// Charging is not available screen will be hide
                cv_error.setVisibility(View.GONE);

                /// If Usb Serial Communication is connected
                if (usbSerialCommunication.connected) {
                    Log.e("USB Communication", "Connected while charging.");

                    /// If Pay With Cash and Pay With QR Button is Not Visible
                    if (llCash.getVisibility() == View.GONE && llQr.getVisibility() == View.GONE) {
                        updateUIForChargingState();
                    }else if(llCash.getVisibility() == View.VISIBLE && llQr.getVisibility() == View.VISIBLE){

                        /// If Pay With Cash and Pay With QR Button is Visible, Start Button should be visible
                        btnStart.setVisibility(View.VISIBLE);
                    }
                } else {
                    Log.e("USB Communication", "Disconnected while charging.");
                }
            }, DELAY_TIME_MILLIS);
        }


        /*
         * If Battery is not in Charging State*/
        private void handleNotChargingState() {
            Log.e("Battery Status", "Device is not charging.");

            runOnUiThread(() -> {
                updateUIForNotChargingState();

                if (cv_error.getVisibility() != View.VISIBLE) {
                    cv_error.setVisibility(View.VISIBLE);
                    btnPayWithCash.setEnabled(false);
                    btnPayWithQr.setEnabled(false);
                    tv_Message.setText(getString(R.string.no_electricity));
                    lvAnimation.setAnimation(R.raw.no_electricity);
                    btnDone.setVisibility(View.GONE);

                    btnDone.setOnClickListener(v -> {
                        usbSerialCommunication.connect();
                        usbSerialCommunication.setBaudRate(115200);
                    });
                }
            });
        }

        /*Update the UI when Device is in charging state*/
        private void updateUIForChargingState() {
            btnStart.setVisibility(View.VISIBLE);
            llCash.setVisibility(View.GONE);
            llQr.setVisibility(View.GONE);
            cvPayWithQr.setVisibility(View.GONE);
            cvPayWithCash.setVisibility(View.GONE);
        }


        /*
        * Buttons visibility should be gone when not in charging*/
        private void updateUIForNotChargingState() {
            llCash.setVisibility(View.GONE);
            llQr.setVisibility(View.GONE);
            cvPayWithQr.setVisibility(View.GONE);
            cvPayWithCash.setVisibility(View.GONE);
            btnStart.setVisibility(View.GONE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }


    /*Initialize
    Shared Preference,
    Usb Serial Communication,
    Register Battery Receiver Broadcast,
    SQLite Database,
    Alert Dialog of Electricity*/
    private void initializeDependencies() {
        preferencesManager = SharedPreferencesManager.getInstance(this);
        usbSerialCommunication = new UsbSerialCommunication(this);
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        appDatabase = AppDatabase.getInstance(this);
        alertDialog = new AlertDialog.Builder(this)
                .setTitle("No Electricity Connections")
                .setMessage("Please check again after sometime. Thank You!")
                .setCancelable(false)
                .create();
    }


    /*Initialize the ui...FInd View By Ids*/
    private void initializeUI() {
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
        cvPayWithCash = findViewById(R.id.cvPayWithCash);
        cvPayWithQr = findViewById(R.id.cvPayWithQR);

        setupInitialVisibility();
        cv_error.bringToFront();
    }

    private void setupInitialVisibility() {
        llCash.setVisibility(View.GONE);
        llQr.setVisibility(View.GONE);
        cvPayWithQr.setVisibility(View.GONE);
        cvPayWithCash.setVisibility(View.GONE);
        cv_error.setVisibility(View.GONE);
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

        submitBtn.setOnClickListener(v -> handleDialogSubmit(dialog, submitBtn));
        dialog.show();
    }


    /*
     * Handle Start button dialog's Submit Button Even*/
    private void handleDialogSubmit(AlertDialog dialog, MaterialButton submitBtn) {
        if (preferencesManager.hasValue(Constants.ResponseTempStatus)) {

            ResponseTempStatus responseTempStatus = new Gson().fromJson(preferencesManager.get(Constants.ResponseTempStatus, "").toString(), ResponseTempStatus.class);

            if (responseTempStatus.getConnectivity() != null) {
                if (!responseTempStatus.getConnectivity()) {
                    submitBtn.setText(getString(R.string.start));
                    dialog.dismiss();
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
            llCash.setVisibility(View.VISIBLE);
            llQr.setVisibility(View.VISIBLE);
            cvPayWithQr.setVisibility(View.VISIBLE);
            cvPayWithCash.setVisibility(View.VISIBLE);
            btnStart.setVisibility(View.GONE);
        }
    }


    /*Go To CashCollector Screen*/
    private void onPayWithCash() {
        if (cv_error.getVisibility() == View.VISIBLE) return;
        Intent intent = new Intent(this, CashCollectorActivity.class);
        startActivityForResult(intent, ScreenEnum.CASH_COLLECTOR.ordinal());
    }


    /*Go To Pay With QR Screen*/
    private void onPayWithQr() {
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
        if (data == null) {
            Log.e(TAG, "run:>> onReadData: " + isShowError);
            return;
        }

        if (!data.contains("lowlevel")) return;

        isShowError = true;
        ResponseTempStatus responseTempStatus = new Gson().fromJson(data, ResponseTempStatus.class);
        Log.e(TAG, "run: ==> onReadData: " + new Gson().toJson(responseTempStatus));

        if (responseTempStatus == null) return;

        runOnUiThread(() -> {
            updateTemperatureAndPrice(responseTempStatus);
            updateIndicator(ivAgitator, responseTempStatus.getAgitator());
            updateIndicator(ivCompressor, responseTempStatus.getCompressor());

            if (Boolean.TRUE.equals(responseTempStatus.getLowlevel())) {
                handleLowLevel();
            } else {
                handleNormalLevel();
            }
        });
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
        Log.e("low level", "True");
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
        Log.e("low level", "False");
        cv_error.setVisibility(View.GONE);
        btnPayWithCash.setVisibility(View.VISIBLE);
        btnPayWithQr.setVisibility(View.VISIBLE);
        btnDone.setVisibility(View.GONE);
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


    /// When user comes from the screen
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode == ScreenEnum.CASH_COLLECTOR.ordinal() || requestCode == ScreenEnum.PAY_WITH_QR.ordinal()) && resultCode == RESULT_OK) {
            // Retrieve the data from the intent
            if (data.hasExtra(FromScreen)) {
                btnStart.setVisibility(View.VISIBLE);
                llCash.setVisibility(View.GONE);
                llQr.setVisibility(View.GONE);
                cvPayWithQr.setVisibility(View.GONE);
                cvPayWithCash.setVisibility(View.GONE);
            }
        }
    }


}