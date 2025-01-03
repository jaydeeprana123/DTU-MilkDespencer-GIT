package com.imdc.milkdespencer;

import static com.imdc.milkdespencer.common.Constants.FromScreen;
import static com.imdc.milkdespencer.common.Constants.ScreenTimeOutPref;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.imdc.milkdespencer.adapter.CurrencyAdapter;
import com.imdc.milkdespencer.common.Constants;
import com.imdc.milkdespencer.common.LottieAddCashDialog;
import com.imdc.milkdespencer.common.LottieDialog;
import com.imdc.milkdespencer.common.SharedPreferencesManager;
import com.imdc.milkdespencer.common.UsbSerialCommunication;
import com.imdc.milkdespencer.enums.ScreenEnum;
import com.imdc.milkdespencer.models.ResponseMilkDispense;
import com.imdc.milkdespencer.models.ResponseTempStatus;
import com.imdc.milkdespencer.models.SendToDevice;
import com.imdc.milkdespencer.network.ApiManager;
import com.imdc.milkdespencer.roomdb.AppDatabase;
import com.imdc.milkdespencer.roomdb.interfaces.TransactionDao;
import com.razorpay.Payment;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import device.itl.sspcoms.BarCodeReader;
import device.itl.sspcoms.DeviceEvent;
import device.itl.sspcoms.DeviceEventListener;
import device.itl.sspcoms.DeviceFileUpdateListener;
import device.itl.sspcoms.DeviceSetupListener;
import device.itl.sspcoms.ItlCurrency;
import device.itl.sspcoms.SSPDevice;
import device.itl.sspcoms.SSPDeviceType;
import device.itl.sspcoms.SSPSystem;
import device.itl.sspcoms.SSPUpdate;

public class CashCollectorActivityBackup extends AppCompatActivity implements DeviceSetupListener, DeviceEventListener, DeviceFileUpdateListener {

    //TODO:
    // 1) Read Continuous data from Serial
    // {
    //  "temperature": "3.04",  should not be more than set Temperature
    //  "compressor": true, green and red indicators
    //  "agitator": false, green and red indicators
    //  "lowlevel": true, if true close the system and show the dialog low on Milk.
    //}

    private static final int MY_PERMISSIONS_REQUEST_READ_STORAGE = 0;
    private static final String ACTION_USB_PERMISSION = "com.imdc.milkdespencer.USB_PERMISSION";
    private static final String TAG = CashCollectorActivityBackup.class.getSimpleName();
    static FloatingActionButton fab;
    static LinearLayout bvDisplay;
    static CashCollectorActivityBackup cashCollectorActivity;
    static ListView listChannels;
    static ListView listEvents;
    static Button bttnAccept;
    static Button bttnReject;
    static SwitchCompat swEscrow;
    static TextView txtFirmware;
    static TextView txtDevice;
    static TextView txtDataset;
    static TextView txtSerial;
    static TextView txtConnect;
    static ProgressBar prgConnect;
    static ProgressDialog progress;
    static List<String> channelValues;
    static String[] eventValues;
    static ArrayAdapter<String> adapterChannels;
    static ArrayAdapter<String> adapterEvents;
    static String selectedCurrency;
    static float milkBasePrice;
    static float milkSetTemperature;
    static SharedPreferencesManager preferencesManager;
//    private static MenuItem downloadFileSelect = null;
    private static ITLDeviceCom deviceCom;
    private static D2xxManager ftD2xx = null;
    private static FT_Device ftDev = null;
    private static SSPDevice sspDevice = null;
    private static CashCollectorActivityBackup instance = null;


    private Handler handler = new Handler(); // Create a Handler instance
    private Runnable runnable; // Declare the Runnable

    /**********   USB functions   ******************************************/

//    private static UsbSerialManager usbSerialManager;
    private static UsbSerialCommunication usbSerialCommunication;


    /*Battery Receiver. In this is battery is not in charging then */
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get the current battery status
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL);

            if (!isCharging) {
                // Log the loss of electricity
                Constants.saveLogs(CashCollectorActivityBackup.this, "Lost Electricity");

                // Retrieve and parse payment details
                String paymentData = preferencesManager.get(Constants.PaymentCashReceived, "").toString();
                if (paymentData != null && !paymentData.isEmpty()) {
                    Payment payment = new Gson().fromJson(paymentData, Payment.class);
                    if (payment != null) {
                        String amountStr = payment.get("amount");
                        if (amountStr != null) {
                            try {
                                float amount = Float.parseFloat(amountStr);
                                float adjustedAmount = amount / 100;


                                /// Here payment is done. And Suddenly electricity lost
                                showElectricityLostAndFailedProcessDialog(adjustedAmount, "");
                            } catch (NumberFormatException e) {
                                Log.e("BatteryReceiver", "Invalid amount format: " + amountStr, e);
                            }
                        }
                    }
                }
            }
        }
    };

    LottieAddCashDialog lottieAddCashDialog;
    AlertDialog loadingDialog;
    LottieDialog lottieDialog;
    GridView grdCurrencyView;

    Button btnBackToHome;

    Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
    ApiManager apiManager;

    /*
    * Broadcast receiver for USB*/
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            switch (action) {
                case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                    // Handle USB device attachment
                    openDevice();
                    break;

                case UsbManager.ACTION_USB_DEVICE_DETACHED:
                    // Handle USB device detachment
                    closeDevice();
                    bvDisplay.setVisibility(View.INVISIBLE);
                    fab.setVisibility(View.VISIBLE);
                    fab.setEnabled(true);
                    break;

                default:
                    // No action needed for other intents
                    break;
            }
        }
    };

    private SSPUpdate sspUpdate = null;

    public static CashCollectorActivityBackup getInstance() {
        return instance;
    }

    public static void DisplaySetUp(SSPDevice dev) {

        sspDevice = dev;

        fab.setVisibility(View.INVISIBLE);
        fab.setVisibility(View.INVISIBLE);
        prgConnect.setVisibility(View.INVISIBLE);
        txtConnect.setVisibility(View.INVISIBLE);
        bvDisplay.setVisibility(View.VISIBLE);

        // check for type comapable
        if (dev.type != SSPDeviceType.BillValidator) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CashCollectorActivityBackup.getInstance());
            // 2. Chain together various setter methods to set the dialog characteristics
            builder.setMessage("Connected device is not BNV (" + dev.type.toString() + ")").setTitle("BNV");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getInstance().finish();
                }
            });

            // 3. Get the AlertDialog from create()
            AlertDialog dialog = builder.create();

            // 4. Show the dialog
            dialog.show();// show error
            return;

        }

//        downloadFileSelect.setEnabled(true);

        /* device details  */
        txtFirmware.append(" " + dev.firmwareVersion);
        txtDevice.append(" " + dev.headerType.toString());
        txtSerial.append(" " + dev.serialNumber);
        txtDataset.append(dev.datasetVersion);

        /* display the channel info */
        channelValues.clear();
        for (ItlCurrency itlCurrency : dev.currency) {
            String v = itlCurrency.country + " " + String.format("%.2f", itlCurrency.realvalue);
            channelValues.add(v);
        }

        adapterChannels.notifyDataSetChanged();


        // if device has barcode hardware
        if (dev.barCodeReader.hardWareConfig != SSPDevice.BarCodeStatus.None) {
            // send new configuration
            BarCodeReader cfg = new BarCodeReader();
            cfg.barcodeReadEnabled = true;
            cfg.billReadEnabled = true;
            cfg.numberOfCharacters = 18;
            cfg.format = SSPDevice.BarCodeFormat.Interleaved2of5;
            cfg.enabledConfig = SSPDevice.BarCodeStatus.Both;
            deviceCom.SetBarcocdeConfig(cfg);
        }
    }

    public static void UpdateFileDownload(SSPUpdate sspUpdate) {


        switch (sspUpdate.UpdateStatus) {
            case dwnInitialise:
                progress.setMessage("Downloading Ram");
                progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progress.setIndeterminate(false);
                progress.setProgress(0);
                progress.setMax(sspUpdate.numberOfRamBlocks);
                progress.setCanceledOnTouchOutside(false);
                progress.show();
                break;
            case dwnRamCode:
                progress.setProgress(sspUpdate.blockIndex);
                break;
            case dwnMainCode:
                progress.setMessage("Downloading flash");
                progress.setMax(sspUpdate.numberOfBlocks);
                progress.setProgress(sspUpdate.blockIndex);
                break;
            case dwnComplete:
                progress.dismiss();
                break;
            case dwnError:
                progress.dismiss();
                break;
        }


    }


    /// If Cash machine device is open then it will be close
    private static void closeDevice() {

        if (ftDev != null) {
            deviceCom.Stop();
            ftDev.close();
        }

    }

    public static void SetConfig(int baud, byte dataBits, byte stopBits, byte parity, byte flowControl) {
        if (!ftDev.isOpen()) {
            return;
        }

        // configure our port
        // reset to UART mode for 232 devices
        ftDev.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);
        ftDev.setBaudRate(baud);

        switch (dataBits) {
            case 7:
                dataBits = D2xxManager.FT_DATA_BITS_7;
                break;
            case 8:
                dataBits = D2xxManager.FT_DATA_BITS_8;
                break;
            default:
                dataBits = D2xxManager.FT_DATA_BITS_8;
                break;
        }

        switch (stopBits) {
            case 1:
                stopBits = D2xxManager.FT_STOP_BITS_1;
                break;
            case 2:
                stopBits = D2xxManager.FT_STOP_BITS_2;
                break;
            default:
                stopBits = D2xxManager.FT_STOP_BITS_1;
                break;
        }

        switch (parity) {
            case 0:
                parity = D2xxManager.FT_PARITY_NONE;
                break;
            case 1:
                parity = D2xxManager.FT_PARITY_ODD;
                break;
            case 2:
                parity = D2xxManager.FT_PARITY_EVEN;
                break;
            case 3:
                parity = D2xxManager.FT_PARITY_MARK;
                break;
            case 4:
                parity = D2xxManager.FT_PARITY_SPACE;
                break;
            default:
                parity = D2xxManager.FT_PARITY_NONE;
                break;
        }

        ftDev.setDataCharacteristics(dataBits, stopBits, parity);

        short flowCtrlSetting;
        switch (flowControl) {
            case 0:
                flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
                break;
            case 1:
                flowCtrlSetting = D2xxManager.FT_FLOW_RTS_CTS;
                break;
            case 2:
                flowCtrlSetting = D2xxManager.FT_FLOW_DTR_DSR;
                break;
            case 3:
                flowCtrlSetting = D2xxManager.FT_FLOW_XON_XOFF;
                break;
            default:
                flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
                break;
        }

        ftDev.setFlowControl(flowCtrlSetting, (byte) 0x0b, (byte) 0x0d);
    }

    public void DeviceDisconnected(SSPDevice dev) {

        eventValues[0] = "DISCONNECTED!!!";
        eventValues[1] = "";
//        Toast.makeText(cashCollectorActivity, "CASH Collector Device " + eventValues[0], Toast.LENGTH_SHORT).show();
        connectToDevices();
        adapterEvents.notifyDataSetChanged();

    }

    private void connectToDevices() {
        if (ftDev != null) {
           // Toast.makeText(CashCollectorActivity.this, milkSetTemperature + " MilkBase Price " + milkBasePrice, Toast.LENGTH_SHORT).show();
        } else {
         //   Toast.makeText(CashCollectorActivity.this, "Please Wait initiating the Connection!!! ", Toast.LENGTH_SHORT).show();
            openDevice();
        }
        usbSerialCommunication.connect();
//        UsbSerialCommunication.currentClass = cashCollectorActivity.getClass().getSimpleName();
        usbSerialCommunication.setBaudRate(115200);
    }



    /* If Submit button is pressed*/
    private void sendToMilkDevice(DeviceEvent ev) {
        double selectedCurrencyValue = Double.parseDouble(selectedCurrency.replace("₹", ""));

        if (ev.value != selectedCurrencyValue) {
            Constants.showAlertDialog(
                    cashCollectorActivity,
                    "Please insert correct note",
                    "Please insert correct note. The selected note didn't match! Please enter the correct note."
            );
            deviceCom.SetEscrowAction(SSPSystem.BillAction.Reject);
            return;
        }

        Log.e("TAG", "Currency: " + ev.currency + "<C  Vd>" + String.format("%.2f", ev.value));

        try {
            ResponseTempStatus responseTempStatus = new Gson().fromJson(
                    preferencesManager.get(Constants.ResponseTempStatus, "").toString(),
                    ResponseTempStatus.class
            );

            float milkSellingPrice = Float.parseFloat(preferencesManager.get(Constants.MilkBasePrice, "0.0").toString());
            float offSet = Float.parseFloat(preferencesManager.get(Constants.TemperatureOffSet, "0.0").toString());
            float milkDensity = Float.parseFloat(preferencesManager.get(Constants.MilkDensityPref, "0.0").toString());

            float weight = (float) ((ev.value / milkSellingPrice) * milkDensity);
            double currentSavedTemp = responseTempStatus.getTemperature() / 10.0;
            float currentTemperature = (float) (currentSavedTemp + offSet);
            float volume = (float) (ev.value) / milkSellingPrice;

            Log.e(TAG, "DisplayEvents: SEND COMMAND " + milkSellingPrice + " " + ev.value);

            SendToDevice sendToDevice = new SendToDevice();
            sendToDevice.setWeight(weight);
            sendToDevice.setStatus(true);
            sendToDevice.setCurtemperature(currentTemperature);
            sendToDevice.setSettemperature(milkSetTemperature);

            Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
            Log.e(TAG, "DisplayEvents: SEND COMMAND " + gson.toJson(sendToDevice));


            /// Show dialog
            lottieDialog.show();

            /// Here after 15 minute if status is not getting as a true.
            // Dialog will be close and transaction will be add in the database as a TIME OUT
            Handler timeoutHandler = new Handler(Looper.getMainLooper());
            Runnable timeoutRunnable = () -> handleMilkSendingTimeout(lottieDialog, (float) ev.value, volume);
            timeoutHandler.postDelayed(timeoutRunnable, 15 * 60 * 1000);



            /// Send Data to the usb Serial Communication
            usbSerialCommunication.sendData(gson.toJson(sendToDevice));


            /// Read Data of the usb Serial Communication
            usbSerialCommunication.setReadDataListener(data -> handleSerialReadingResponse(data, sendToDevice, ev.value, volume, currentSavedTemp, timeoutHandler, timeoutRunnable));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /*If 15 minutes done and status is not getting as a true. Transaction will be added as a TIME OUT*/
    private void handleMilkSendingTimeout(LottieDialog lottieDialog, float amt, float volume) {
        if (lottieDialog != null && lottieDialog.isShowing()) {
            lottieDialog.dismiss();
            new Thread(() -> {
                try {
                    String date = new SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis());
                    String time = new SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis());
                    TransactionDao transactionDao = AppDatabase.getInstance(CashCollectorActivityBackup.this).transactionDao();
                    Constants.insertTransaction(CashCollectorActivityBackup.this, transactionDao, "CASH", "", date, time, String.valueOf(amt), "FAILED", "", String.valueOf(volume));
                    goToHomeScreen();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }



    /*Read Listener Response*/
    private void handleSerialReadingResponse(String data, SendToDevice sendToDevice, double currency, float volume, double currentSavedTemp, Handler timeoutHandler, Runnable timeoutRunnable) {
        Log.d(TAG, "DisplayEvents:onReadData: " + data);

        if (!data.contains("status")) return;

        ResponseMilkDispense milkDispense = new Gson().fromJson(data, ResponseMilkDispense.class);
        Log.i(TAG, "onReadData: " + new Gson().toJson(milkDispense));

        if (milkDispense != null) {
            double percentage = (milkDispense.getCurrentWeight() / milkDispense.getSetWeight()) * 100;
            if (lottieDialog != null && percentage > 0) {
                lottieDialog.setPercentage(percentage);
            }

            /// Here is Milk Dispense is true
            if (milkDispense.getStatus()) {
                handleSuccessfulDispense(sendToDevice, currency,volume, currentSavedTemp);
            } else {
                Log.e("Cashcollector", milkDispense.getStatus().toString());
            }
        }
    }

    private void handleSuccessfulDispense(SendToDevice sendToDevice, double currency,float volume, double currentSavedTemp) {
        Log.e("Cashcollector", "Dispense Successful");

        try {
            if (lottieDialog.isShowing()) {
                lottieDialog.dismiss();
            }

            preferencesManager.save(Constants.CurrentTemperature, currentSavedTemp);
            deviceCom.SetEscrowAction(SSPSystem.BillAction.Accept);

            showAndProcessDoneDialog(sendToDevice, currency,volume);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void showAndProcessDoneDialog(SendToDevice sendToDevice, double currency, float volume) {
        Log.e("showAndProcessDoneDialog", "Show");

        runOnUiThread(() -> {
            AlertDialog dialog = createProcessDoneDialog(sendToDevice, currency, volume);
            dialog.show();
        });
    }

    private AlertDialog createProcessDoneDialog(SendToDevice sendToDevice, double currency, float volume) {
        AlertDialog.Builder builder = new AlertDialog.Builder(CashCollectorActivityBackup.this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_lottie, null);

        setupDialogView(view, sendToDevice, currency, volume);
        builder.setView(view).setCancelable(false);
        return builder.create();
    }

    private void setupDialogView(View view, SendToDevice sendToDevice, double currency, float volume) {
        LottieAnimationView lottieAnimationViewDone = view.findViewById(R.id.lottieAnimationViewDone);
        TextView tvProcessDoneText = view.findViewById(R.id.tvProcessDoneText);
        TextView tvOpenTheDoor = view.findViewById(R.id.tvOpenTheDoor);
        MaterialButton btnDone = view.findViewById(R.id.doneButton);

        btnDone.setVisibility(View.VISIBLE);
        lottieAnimationViewDone.setVisibility(View.VISIBLE);
        tvProcessDoneText.setVisibility(View.VISIBLE);
        tvOpenTheDoor.setVisibility(View.VISIBLE);

        lottieAnimationViewDone.setAnimation(R.raw.process_done);
        lottieAnimationViewDone.setRepeatMode(LottieDrawable.RESTART);
        lottieAnimationViewDone.playAnimation();

        setupDialogHandlers(view, sendToDevice, currency, volume);
    }

    private void setupDialogHandlers(View view, SendToDevice sendToDevice, double currency, float volume) {
        MaterialButton btnDone = view.findViewById(R.id.doneButton);
        long screenTimeout = Long.parseLong(preferencesManager.get(ScreenTimeOutPref, "0").toString()) * 1000;

        Handler handler = new Handler();
        Runnable dismissTask = () -> {
            view.getRootView().setVisibility(View.GONE);
            closeDevice();
            insertDataOnProcessDone(currency, String.valueOf(volume));
        };

        handler.postDelayed(dismissTask, screenTimeout);

        btnDone.setOnClickListener(v -> {
            handler.removeCallbacks(dismissTask);
            dismissTask.run();
        });
    }

    /*
     * If payment is done and electricity is lost. Then Show Fail Dialog*/
    private void showElectricityLostAndFailedProcessDialog(float amt, String volume) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                Constants.showAcceptDialog(CashCollectorActivityBackup.this, "Error", "Lost Electricity Connection!! Please Try after sometime.", (dialog1, which) -> {
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

                                TransactionDao transactionDao = AppDatabase.getInstance(CashCollectorActivityBackup.this).transactionDao();
                                assert date != null;
                                long transactionId = Constants.insertTransaction(CashCollectorActivityBackup.this, transactionDao, "CASH", "", date, time, String.valueOf(amt) ,"FAILED", "",volume);
                                Log.e(TAG, "onCreate: " + transactionId);
                                Log.e(TAG, "onCreate: " + new Gson().toJson(transactionDao.getAllTransactions()));


                                /// Close current dialog
                                dialog1.dismiss();

                                /// Go to Home screen
                                goToHomeScreen();

//                                Intent intent = new Intent(CashCollectorActivity.this, MainActivity.class);
//                                startActivity(intent);
//                                finish();

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

                                TransactionDao transactionDao = AppDatabase.getInstance(CashCollectorActivityBackup.this).transactionDao();
                                assert date != null;
                                long transactionId = Constants.insertTransaction(CashCollectorActivityBackup.this, transactionDao, "CASH", "", date, time, String.valueOf(amt), "FAILED", "", volume);
                                Log.e(TAG, "onCreate: " + transactionId);
                                Log.e(TAG, "onCreate: " + new Gson().toJson(transactionDao.getAllTransactions()));


                                /// Close current dialog
                                dialog1.dismiss();

                                /// Go to Home screen
                                goToHomeScreen();

//                                Intent intent = new Intent(CashCollectorActivity.this, MainActivity.class);
//                                startActivity(intent);
//                                finish();

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                });

            }
        });
    }

    public void DisplayEvents(DeviceEvent ev) {

        switch (ev.event) {
            case CommunicationsFailure:
                break;
            case Ready:
                eventValues[0] = "Ready";
                eventValues[1] = "";
                break;
            case BillRead:
                eventValues[0] = "Reading";
                eventValues[1] = "";
                break;
            case BillEscrow:
                eventValues[0] = "Bill Escrow";
                eventValues[1] = ev.currency + " " + String.format("%.2f", ev.value);

                LayoutInflater inflater = LayoutInflater.from(getInstance());
                View view = inflater.inflate(R.layout.dialog_note_detected, null);

                preferencesManager = SharedPreferencesManager.getInstance(getInstance());

                AlertDialog.Builder builder = new AlertDialog.Builder(getInstance());
                builder.setView(view);

                AlertDialog dialog = builder.create();

                MaterialButton submitBtn = view.findViewById(R.id.btnSubmitToDevice);
                MaterialButton cancelBtn = view.findViewById(R.id.btnCancel);
                TextView tvCurrencyAmt = view.findViewById(R.id.tvCurrencyDetectedAmt);
                ImageView ivCurrency = view.findViewById(R.id.ivLogo);
                TextView tvMessage = view.findViewById(R.id.tvCurrencyMessage);

                Log.e(TAG, "DisplayEvents: " + ev.value);
                if (eventValues != null) {
                    String msg = "";
                    if (eventValues[1] != null) {
                        float milkSellingPrice = Float.parseFloat(String.valueOf(milkBasePrice));
                        float offSet = Float.parseFloat(preferencesManager.get(Constants.TemperatureOffSet, "0.0").toString());
                        float milkDensity = Float.parseFloat(preferencesManager.get(Constants.MilkDensityPref, "0.0").toString());
                        float weight = Float.parseFloat(String.valueOf((ev.value / milkSellingPrice))) * milkDensity; //TODO : multiply with Den.
                        Log.e(TAG, "DisplayEvents: milkDensity " + milkDensity);
                        switch ((int) ev.value) {
                            case 10:
                                ivCurrency.setImageResource(R.drawable.ic_ten);
                                break;
                            case 20:
                                ivCurrency.setImageResource(R.drawable.ic_twenty);
                                break;
                            case 50:
                                ivCurrency.setImageResource(R.drawable.ic_fifty);
                                break;
                            case 100:
                                ivCurrency.setImageResource(R.drawable.ic_hundred);
                                break;
                            case 200:
                                ivCurrency.setImageResource(R.drawable.ic_two_hundred);
                                break;
                            case 500:
                                ivCurrency.setImageResource(R.drawable.ic_five_hundred);
                                break;
                            default:
                                ivCurrency.setImageResource(R.drawable.pay_with_cash);
                        }

                        msg = eventValues[1] + " is detected you will get " + weight + " liters of Milk.\n Please ensure the door is closed starting the dispensation!! Press Start to Confirm!!!";
                        tvMessage.setText(msg);
                        tvCurrencyAmt.setText(eventValues[1]);
                    }

                }


                cancelBtn.setOnClickListener(v -> {
                    deviceCom.SetEscrowAction(SSPSystem.BillAction.Reject);
                    dialog.dismiss();
                });
                submitBtn.setOnClickListener(v -> {
                    sendToMilkDevice(ev);
                    dialog.dismiss();
                });
                // Show the dialog

                if (selectedCurrency != null && !selectedCurrency.isEmpty()) {
                    lottieAddCashDialog.isShowing();
                    lottieAddCashDialog.dismiss();
                    dialog.show();

                } else {
                    Constants.showAlertDialog(cashCollectorActivity, "Please Select the Amount", "Please select the amount before inserting the currency!");
                    deviceCom.SetEscrowAction(SSPSystem.BillAction.Reject);
                }
                break;
            case BillStacked:

                break;
            case BillReject:
                eventValues[0] = "Bill Reject";
                eventValues[1] = "";
                if (swEscrow.isChecked()) {
                    bttnAccept.setVisibility(View.INVISIBLE);
                    bttnReject.setVisibility(View.INVISIBLE);
                }
                break;
            case BillJammed:
                eventValues[0] = "Bill jammed";
                eventValues[1] = "";
                break;
            case BillFraud:
                eventValues[0] = "Bill Fraud";
                eventValues[1] = ev.currency + " " + String.format("%.2f", ev.value);
                break;
            case BillCredit:
                eventValues[0] = "Bill Credit";
                eventValues[1] = ev.currency + " " + String.format("%.2f", ev.value);
                break;
            case Full:
                eventValues[0] = "Bill Cashbox full";
                eventValues[1] = "";
                break;
            case Initialising:

                break;
            case Disabled:
                eventValues[0] = "Disabled";
                eventValues[1] = "";
                break;
            case SoftwareError:
                eventValues[0] = "Software error";
                eventValues[1] = "";
                break;
            case AllDisabled:
                eventValues[0] = "All channels disabled";
                eventValues[1] = "";
                break;
            case CashboxRemoved:
                eventValues[0] = "Cashbox removed";
                eventValues[1] = "";
                break;
            case CashboxReplaced:
                eventValues[0] = "Cashbox replaced";
                eventValues[1] = "";
                break;
            case NotePathOpen:
                eventValues[0] = "Note path open";
                eventValues[1] = "";
                break;
            case BarCodeTicketEscrow:
                eventValues[0] = "Barcode ticket escrow:";
                eventValues[1] = ev.currency;
                if (swEscrow.isChecked()) {
                    bttnAccept.setVisibility(View.VISIBLE);
                    bttnReject.setVisibility(View.VISIBLE);
                }
                break;
            case BarCodeTicketStacked:
                eventValues[0] = "Barcode ticket stacked";
                eventValues[1] = "";
                break;
        }
        Log.e("TAG", "DisplayEvents: " + ev.event);
        adapterEvents.notifyDataSetChanged();
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Keep screen on and hide navigation
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        Log.e("Cash Collector", "Screen Initialized");
        screenTimeOut();

        // Initialize UI components
        initializeUIComponents();

        // Set up permissions
        checkStoragePermission();

        // Set up title
        setTitle("Pay With Cash");

        // Register USB and battery receivers
        registerReceivers();

        // Set up device communication
        setupDeviceCommunication();

        // Handle grid item clicks
        grdCurrencyView.setOnItemClickListener((adapterView, view, position, id) -> handleCurrencyClick(position));

        // Set up toggle switches
        setupSwitchListeners();

        // Set up accept and reject buttons
        setupActionButtons();

        // Configure floating action button (if needed in the future)
        fab.setVisibility(View.GONE);
    }

    // Separate method to initialize UI components
    private void initializeUIComponents() {

        usbSerialCommunication = new UsbSerialCommunication(getApplicationContext());

        grdCurrencyView = findViewById(R.id.gridViewCurrency);
        btnBackToHome = findViewById(R.id.btnBackToHome);
        bvDisplay = findViewById(R.id.content_bill_validator);
        bvDisplay.setVisibility(View.INVISIBLE);
        fab = findViewById(R.id.fab);

        preferencesManager = SharedPreferencesManager.getInstance(this);
        lottieDialog = new LottieDialog(this);
        lottieAddCashDialog = new LottieAddCashDialog(this);

        CurrencyAdapter adapter = new CurrencyAdapter(this);
        grdCurrencyView.setAdapter(adapter);



        txtConnect = findViewById(R.id.txtConnection);
        prgConnect = findViewById(R.id.progressBarConnect);

        listEvents = findViewById(R.id.listEvents);
        listChannels = findViewById(R.id.listChannels);

        eventValues = new String[]{"", ""};
        channelValues = new ArrayList<String>();

        adapterEvents = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new String[]{"", ""});
        listEvents.setAdapter(adapterEvents);

        adapterChannels = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        listChannels.setAdapter(adapterChannels);

        bttnAccept = findViewById(R.id.bttnAccept);
        bttnReject = findViewById(R.id.bttnReject);

        txtFirmware = findViewById(R.id.txtFirmware);
        txtDevice = findViewById(R.id.txtDevice);
        txtDataset = findViewById(R.id.txtDataset);
        txtSerial = findViewById(R.id.txtSerialNumber);
    }

    // Method to check and request storage permissions
    private void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                txtConnect.setText("This app requires access to the downloads directory.");
                txtConnect.setVisibility(View.VISIBLE);
            }
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_STORAGE);
        }
    }

    // Method to register USB and battery receivers
    private void registerReceivers() {
        IntentFilter batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, batteryFilter);

        IntentFilter usbFilter = new IntentFilter();
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        usbFilter.setPriority(500);
        registerReceiver(mUsbReceiver, usbFilter);
    }

    // Method to set up device communication
    private void setupDeviceCommunication() {
        try {
            ftD2xx = D2xxManager.getInstance(this);
        } catch (D2xxManager.D2xxException ex) {
            Log.e("SSP FTManager", ex.toString());
        }

        deviceCom = new ITLDeviceCom();
        deviceCom.setDeviceSetupListener(this);
        deviceCom.setDeviceEventListener(this);
        deviceCom.setDeviceFileUpdateListener(this);
    }

    // Method to handle currency grid item clicks
    private void handleCurrencyClick(int position) {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }

        selectedCurrency = grdCurrencyView.getAdapter().getItem(position).toString();
        milkBasePrice = Float.parseFloat(preferencesManager.get(Constants.MilkBasePrice, "0.0").toString());
        milkSetTemperature = Float.parseFloat(preferencesManager.get(Constants.TemperatureSet, "0.0").toString());

        lottieAddCashDialog.show();
        lottieAddCashDialog.setCancelable(false);

        MaterialButton btnCancel = lottieAddCashDialog.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(view -> {
            lottieAddCashDialog.dismiss();
            goToHomeScreen();
        });

        openDevice();
    }

    // Method to set up toggle switch listeners
    private void setupSwitchListeners() {
        swEscrow = findViewById(R.id.swEscrow);
        swEscrow.setOnCheckedChangeListener((buttonView, isChecked) -> deviceCom.SetEscrowMode(isChecked));

        SwitchCompat swDisable = findViewById(R.id.swEnable);
        swDisable.setChecked(true);
        swDisable.setOnCheckedChangeListener((buttonView, isChecked) -> deviceCom.SetDeviceEnable(isChecked));
    }

    // Method to set up action buttons
    private void setupActionButtons() {
        bttnAccept.setVisibility(View.INVISIBLE);
        bttnAccept.setOnClickListener(v -> {
            bttnReject.setVisibility(View.INVISIBLE);
            bttnAccept.setVisibility(View.INVISIBLE);
            goToHomeScreen();
        });

        bttnReject.setVisibility(View.INVISIBLE);
        bttnReject.setOnClickListener(v -> {
            deviceCom.SetEscrowAction(SSPSystem.BillAction.Reject);
            bttnReject.setVisibility(View.INVISIBLE);
            bttnAccept.setVisibility(View.INVISIBLE);
        });
    }





    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        openDevice();
        connectToDevices();

    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mUsbReceiver);
        unregisterReceiver(batteryReceiver);
//        usbSerialCommunication.disconnect();
        super.onDestroy();
    }

    private void ClearDisplay() {
        progress.setProgress(0);
//        txtFirmware.setText(getResources().getString(R.string.firmware_title));
//        txtDevice.setText(getResources().getString(R.string.device_title));
//        txtDataset.setText(getResources().getString(R.string.dataset_title));
//        txtSerial.setText(getResources().getString(R.string.serial_number_title));

        adapterChannels.clear();
        adapterChannels.notifyDataSetChanged();

        eventValues[0] = "";
        eventValues[1] = "";

        adapterEvents.notifyDataSetChanged();


    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//
//        MenuItem menuItem = menu.findItem(R.id.action_home);
//        // Set the tint color dynamically
//        Drawable icon = menuItem.getIcon();
//        if (icon != null) {
//            icon.mutate(); // Ensure the drawable is mutable
//            icon.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN);
//        }
//
//        downloadFileSelect = menu.getItem(0);
//        downloadFileSelect.setEnabled(false);
//
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//
//
//        if (item.getItemId() == R.id.action_home) {
//            Log.e("Home button", "Pressed");
//            Intent intent = new Intent(CashCollectorActivity.this, MainActivity.class);
//            startActivity(intent);
//            finish();
//        }
//        // Handle item selection
//        /*switch (item.getItemId()) {
//            case R.id.action_downloadFile:
//                openFolder();
//                return true;
//            case R.id.action_shutdown:
//                deviceCom.Stop();
//                closeDevice();
//                finish();
//            default:
//                return super.onOptionsItemSelected(item);
//        }*/
//
//        return super.onOptionsItemSelected(item);
//    }

    public void openFolder() {

        if (deviceCom == null) {
            return;
        }

        int devcode = deviceCom.GetDeviceCode();
        if (devcode < 0) {
        }

        /*Intent intent = new Intent(this, ListFiles.class);
        // send the current device code
        intent.putExtra("deviceCode", (byte) devcode);
        startActivityForResult(intent, 123);*/

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_READ_STORAGE) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // permission was granted, yay! Do the
                // contacts-related task you need to do.

            } else {

                // permission denied, boo! Disable the
                // functionality that depends on this permission.
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 123 && resultCode == RESULT_OK) {
            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();

            path += "/";
            String flname = "";

            if (data.hasExtra("filename")) {
                flname = data.getStringExtra("filename");
                path += flname;

            } else {
                txtDevice.setText(R.string.no_file_data_error);
                return;
            }


            sspUpdate = new SSPUpdate(flname);
            try {
                final File up = new File(path);

                sspUpdate.fileData = new byte[(int) up.length()];
                DataInputStream dis = new DataInputStream(new FileInputStream(up));
                dis.readFully(sspUpdate.fileData);
                dis.close();

                sspUpdate.SetFileData();
                ClearDisplay();
                deviceCom.SetSSPDownload(sspUpdate);


            } catch (IOException e) {
                e.printStackTrace();
                //   txtEvents.append(R.string.unable_to_load + "\r\n");
            }
        }
    }

    private void openDevice() {

        if (ftDev != null) {
            if (ftDev.isOpen()) {
                // if open and run thread is stopped, start thread
                SetConfig(9600, (byte) 8, (byte) 2, (byte) 0, (byte) 0);
                ftDev.purge((byte) (D2xxManager.FT_PURGE_TX | D2xxManager.FT_PURGE_RX));
                ftDev.restartInTask();
                return;
            }
        }

        int devCount = 0;

        if (ftD2xx != null) {
            // Get the connected USB FTDI devoces
            devCount = ftD2xx.createDeviceInfoList(this);
        } else {
            return;
        }

        D2xxManager.FtDeviceInfoListNode[] deviceList = new D2xxManager.FtDeviceInfoListNode[devCount];
        ftD2xx.getDeviceInfoList(devCount, deviceList);
        // none connected
        if (devCount <= 0) {
            return;
        }
        if (ftDev == null) {
//            openDevice();
            ftDev = ftD2xx.openByIndex(this, 0);
        } else {
            synchronized (ftDev) {
                ftDev = ftD2xx.openByIndex(this, 0);
            }
        }
        // run thread
        if (ftDev.isOpen()) {
            SetConfig(9600, (byte) 8, (byte) 2, (byte) 0, (byte) 0);
            ftDev.purge((byte) (D2xxManager.FT_PURGE_TX | D2xxManager.FT_PURGE_RX));
            ftDev.restartInTask();
        }
        if (ftDev != null) {
            deviceCom.setup(ftDev, 0x00, false, false, 0);
            deviceCom.start();
            deviceCom.SetEscrowMode(true);
        }
    }

    @Override
    public void OnDeviceEvent(DeviceEvent deviceEvent) {
        runOnUiThread(() -> DisplayEvents(deviceEvent));
    }

    @Override
    public void OnFileUpdateStatus(SSPUpdate sspUpdate) {
        UpdateFileDownload(sspUpdate);
        /*runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(cashCollectorActivity, " OnFileUpdateStatus ", Toast.LENGTH_SHORT).show();
            }
        });*/
    }

    @Override
    public void OnNewDeviceSetup(SSPDevice sspDevice) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DisplaySetUp(sspDevice);
            }
        });
    }

    @Override
    public void OnDeviceDisconnect(SSPDevice sspDevice) {
        runOnUiThread(() -> DeviceDisconnected(sspDevice));
    }


    /*
    * Here we are getting time out from shared preference
    * And after that screen automatically off
    * */
    void screenTimeOut(){
        preferencesManager = SharedPreferencesManager.getInstance(getInstance());
        Log.e("timeOut", preferencesManager.get(ScreenTimeOutPref, "0").toString());

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
    void goToHomeScreen(){
        closeDevice();
        // Simulate finishing and sending data
        Intent resultIntent = new Intent();
        resultIntent.putExtra(FromScreen, ScreenEnum.CASH_COLLECTOR.ordinal());
        setResult(RESULT_OK, resultIntent); // Set the result to be OK
        finish(); // Finish the activity


//        Intent intent = new Intent(CashCollectorActivity.this, MainActivity.class);
//        // Clear all previous activities
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        startActivity(intent);

    }

    /// When process is completed. Data will be insert into database
    void insertDataOnProcessDone(double currency, String volume){
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

                    TransactionDao transactionDao = AppDatabase.getInstance(CashCollectorActivityBackup.this).transactionDao();
                    assert date != null;
                    long transactionId = Constants.insertTransaction(CashCollectorActivityBackup.this, transactionDao, "CASH", "", date, time, String.valueOf(currency), "SUCCESS", "", volume);
                    Log.e(TAG, "onCreate: " + transactionId);
                    Log.e(TAG, "onCreate: " + new Gson().toJson(transactionDao.getAllTransactions()));


                    /// Go to home page
                    goToHomeScreen();

//                    Intent intent = new Intent(CashCollectorActivity.this, MainActivity.class);
//                    // Clear all previous activities
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    startActivity(intent);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}