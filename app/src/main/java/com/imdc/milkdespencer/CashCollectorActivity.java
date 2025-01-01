package com.imdc.milkdespencer;

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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

public class CashCollectorActivity extends AppCompatActivity implements DeviceSetupListener, DeviceEventListener, DeviceFileUpdateListener {

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
    private static final String TAG = CashCollectorActivity.class.getSimpleName();
    static FloatingActionButton fab;
    static LinearLayout bvDisplay;
    static CashCollectorActivity cashCollectorActivity;
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
    private static MenuItem downloadFileSelect = null;
    private static ITLDeviceCom deviceCom;
    private static D2xxManager ftD2xx = null;
    private static FT_Device ftDev = null;
    private static SSPDevice sspDevice = null;
    private static CashCollectorActivity instance = null;
    /**********   USB functions   ******************************************/

//    private static UsbSerialManager usbSerialManager;
    private static UsbSerialCommunication usbSerialCommunication;
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get the current battery status
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

            // Check if the device is charging
            boolean isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL);

            if (isCharging) {


            } else {
                Constants.saveLogs(CashCollectorActivity.this, "Lost Electricity");


                Payment payment = new Gson().fromJson(preferencesManager.get(Constants.PaymentCashReceived, "").toString(), Payment.class);
                if (payment != null && payment.get("amount") != null) {
                    float amount = Float.parseFloat(payment.get("amount").toString());
                    float amt = amount / 100;
                    showFailedProcessDoneDialog(amt);
                }
            }
        }
    };
    LottieAddCashDialog lottieAddCashDialog;
    AlertDialog loadingDialog;
    LottieDialog lottieDialog;
    GridView grdCurrencyView;
    Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
    ApiManager apiManager;
    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                // never come here(when attached, go to onNewIntent)
                openDevice();
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                closeDevice();
                bvDisplay.setVisibility(View.INVISIBLE);
                fab.setVisibility(View.VISIBLE);
                fab.setEnabled(true);
            }
        }
    };
    private SSPUpdate sspUpdate = null;

    public static CashCollectorActivity getInstance() {

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
            AlertDialog.Builder builder = new AlertDialog.Builder(CashCollectorActivity.getInstance());
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

        downloadFileSelect.setEnabled(true);

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
            Toast.makeText(CashCollectorActivity.this, milkSetTemperature + " MilkBase Price " + milkBasePrice, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(CashCollectorActivity.this, "Please Wait initiating the Connection!!! ", Toast.LENGTH_SHORT).show();
            openDevice();
        }
        usbSerialCommunication.connect();
//        UsbSerialCommunication.currentClass = cashCollectorActivity.getClass().getSimpleName();
        usbSerialCommunication.setBaudRate(115200);
    }

    private void sendToMilkDevice(DeviceEvent ev) {
        if (ev.value == Double.parseDouble(selectedCurrency.replace("₹", ""))) {
            Log.e("TAG", "Currency: " + ev.currency + "<C  Vd>" + String.format("%.2f", ev.value) + "\n AMT " + ev.value + " Condition " + (ev.value == Double.parseDouble(selectedCurrency.replace("₹", ""))));
            try {
                ResponseTempStatus responseTempStatus = new Gson().fromJson(preferencesManager.get(Constants.ResponseTempStatus, "").toString(), ResponseTempStatus.class);
                float milkSellingPrice = Float.parseFloat(preferencesManager.get(Constants.MilkBasePrice, "0.0").toString());
                float offSet = Float.parseFloat(preferencesManager.get(Constants.TemperatureOffSet, 0.0).toString());
                float DENSITY_OF_MILK = Float.parseFloat(preferencesManager.get(Constants.MilkDensityPref, "0.0").toString());
                float weight = Float.parseFloat(String.valueOf((ev.value / milkSellingPrice))) * DENSITY_OF_MILK;
//                float weight = Float.parseFloat(String.valueOf((ev.value / milkSellingPrice)));
                double currentSavedTemp = responseTempStatus.getTemperature() / 10;
                float currentTemperature = Float.parseFloat(String.valueOf((currentSavedTemp + offSet)));

                Log.e(TAG, "DisplayEvents: SEND COMMAND " + milkSellingPrice + " " + ev.value);
                SendToDevice sendToDevice = new SendToDevice();
                sendToDevice.setWeight(weight);
                sendToDevice.setStatus(true);
                sendToDevice.setCurtemperature(currentTemperature);
                sendToDevice.setSettemperature(milkSetTemperature);
//                UsbSerialCommunication.currentClass = "CCA";
                Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
                Log.e(TAG, "DisplayEvents: SEND COMMAND " + gson.toJson(sendToDevice));
//                Toast.makeText(CashCollectorActivity.this, "Command SEND TO DEVICE On START ->\n " + new Gson().toJson(sendToDevice), Toast.LENGTH_LONG).show();
                usbSerialCommunication.sendData(gson.toJson(sendToDevice));
                lottieDialog.show();
                usbSerialCommunication.setReadDataListener(new UsbSerialCommunication.ReadDataListener() {
                    @Override
                    public void onReadData(String data) {
                        Log.d(TAG, "DisplayEvents:onReadData: " + data + "\n status " + data.contains("status"));

                        if (data.contains("status")) {
                            ResponseMilkDispense milkDispense = new Gson().fromJson(data, ResponseMilkDispense.class);
                            Log.i(TAG, "run: ==>< onReadData: " + new Gson().toJson(milkDispense));

                            if (milkDispense != null) {
                                double percentage = (milkDispense.getCurTemperature() / milkDispense.getSetTemperature()) * 100;
                                if (lottieDialog != null) {
                                    if (percentage > 0) {
                                        lottieDialog.setPercentage(percentage);
                                    }
                                }
                                if (milkDispense.getStatus()) {
                                    try {
                                        if (lottieDialog.isShowing()) {
                                            lottieDialog.dismiss();
                                        }
                                        preferencesManager.save(Constants.CurrentTemperature, currentSavedTemp);
                                        preferencesManager = SharedPreferencesManager.getInstance(getInstance());
                                        deviceCom.SetEscrowAction(SSPSystem.BillAction.Accept);
                                        /*float offSet = Float.parseFloat(preferencesManager.get(Constants.TemperatureOffSet, 0.0).toString());
                                        double currentSavedTemp = responseTempStatus.getTemperature() / 10;
                                        float currentTemperature = Float.parseFloat(String.valueOf((currentSavedTemp + offSet)));
                                        usbSerialCommunication.fireOnStart(currentTemperature);
                                        usbSerialCommunication.fireOnStart(currentTemperature);*/
//                                        usbSerialCommunication.fireOnStart(currentTemperature);
                                        //                                        bttnAccept.performClick();
                                        showAndProcessDoneDialog(sendToDevice, ev.value);
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }

                                }
                            }
                        }

                    }
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            Constants.showAlertDialog(cashCollectorActivity, "Please insert correct note", "Please insert correct note and selected note didn't match!!! please enter correct note!");
            deviceCom.SetEscrowAction(SSPSystem.BillAction.Reject);
        }
    }

    public void showAndProcessDoneDialog(SendToDevice sendToDevice, double currency) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(CashCollectorActivity.this);
                LayoutInflater inflater = getLayoutInflater();
                View view = inflater.inflate(R.layout.dialog_lottie, null);

                LottieAnimationView lottieAnimationView = view.findViewById(R.id.lottieAnimationView);
                TextView tvProgressDialog = view.findViewById(R.id.tvProgressDialog);
                MaterialButton btnDone = view.findViewById(R.id.doneButton);
                btnDone.setVisibility(View.VISIBLE);
                tvProgressDialog.setVisibility(View.GONE);
                lottieAnimationView.setAnimation(R.raw.process_done);
                lottieAnimationView.setRepeatMode(LottieDrawable.RESTART);
                lottieAnimationView.playAnimation();

                // Customize the LottieAnimationView and TextView here

                builder.setView(view);
                builder.setCancelable(false); // Set to true if you want the dialog to be cancellable

                AlertDialog dialog = builder.create();
                dialog.show();

                btnDone.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        closeDevice();
//                        onDestroy();

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

                                    TransactionDao transactionDao = AppDatabase.getInstance(CashCollectorActivity.this).transactionDao();
                                    assert date != null;
                                    long transactionId = Constants.insertTransaction(CashCollectorActivity.this, transactionDao, "CASH", "", date, time, String.valueOf(currency), "SUCCESS", "");
                                    Log.e(TAG, "onCreate: " + transactionId);
                                    Log.e(TAG, "onCreate: " + new Gson().toJson(transactionDao.getAllTransactions()));
                                    Intent intent = new Intent(CashCollectorActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();

//                doPostTransaction(Constants.PostTransactionURL);
                    }
                });
            }
        });


    }

    private void showFailedProcessDoneDialog(float amt) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                Constants.showAcceptDialog(CashCollectorActivity.this, "Error", "Lost Electricity Connection!! Please Try after sometime.", (dialog1, which) -> {
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

                                TransactionDao transactionDao = AppDatabase.getInstance(CashCollectorActivity.this).transactionDao();
                                assert date != null;
                                long transactionId = Constants.insertTransaction(CashCollectorActivity.this, transactionDao, "CASH", "", date, time, String.valueOf(amt), "FAILED", "");
                                Log.e(TAG, "onCreate: " + transactionId);
                                Log.e(TAG, "onCreate: " + new Gson().toJson(transactionDao.getAllTransactions()));
                                Intent intent = new Intent(CashCollectorActivity.this, MainActivity.class);
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

                                TransactionDao transactionDao = AppDatabase.getInstance(CashCollectorActivity.this).transactionDao();
                                assert date != null;
                                long transactionId = Constants.insertTransaction(CashCollectorActivity.this, transactionDao, "CASH", "", date, time, String.valueOf(amt), "FAILED", "");
                                Log.e(TAG, "onCreate: " + transactionId);
                                Log.e(TAG, "onCreate: " + new Gson().toJson(transactionDao.getAllTransactions()));
                                Intent intent = new Intent(CashCollectorActivity.this, MainActivity.class);
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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        grdCurrencyView = findViewById(R.id.gridViewCurrency);
        bvDisplay = findViewById(R.id.content_bill_validator);
        bvDisplay.setVisibility(View.INVISIBLE);
        cashCollectorActivity = this;
        instance = this;
        preferencesManager = SharedPreferencesManager.getInstance(this);
        lottieDialog = new LottieDialog(getInstance());
        lottieAddCashDialog = new LottieAddCashDialog(this);
        CurrencyAdapter adapter = new CurrencyAdapter(this);
        grdCurrencyView.setAdapter(adapter);

        usbSerialCommunication = new UsbSerialCommunication(getApplicationContext());

        IntentFilter battertyFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, battertyFilter);

        progress = new ProgressDialog(CashCollectorActivity.this);
        /* ask for permission to storeage read  */
        int permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {

                txtConnect.setText("This app requires access to the downloads directory in order to load download files.");
                txtConnect.setVisibility(View.VISIBLE);

                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_STORAGE);


            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_STORAGE);
            }
        }

        setTitle("Pay With Cash");

        listEvents = findViewById(R.id.listEvents);
        listChannels = findViewById(R.id.listChannels);

        eventValues = new String[]{"", ""};
        channelValues = new ArrayList<String>();

        adapterEvents = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, eventValues);
        listEvents.setAdapter(adapterEvents);

        adapterChannels = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, channelValues);
        listChannels.setAdapter(adapterChannels);

        bttnAccept = findViewById(R.id.bttnAccept);
        bttnReject = findViewById(R.id.bttnReject);
        txtFirmware = findViewById(R.id.txtFirmware);
//        txtFirmware.setText(getResources().getString(R.string.firmware_title));
        txtDevice = findViewById(R.id.txtDevice);
//        txtDevice.setText(getResources().getString(R.string.device_title));
        txtDataset = findViewById(R.id.txtDataset);
//        txtDataset.setText(getResources().getString(R.string.dataset_title));
        txtSerial = findViewById(R.id.txtSerialNumber);
//        txtSerial.setText(getResources().getString(R.string.serial_number_title));

        prgConnect = findViewById(R.id.progressBarConnect);
        txtConnect = findViewById(R.id.txtConnection);


        try {
            ftD2xx = D2xxManager.getInstance(this);
        } catch (D2xxManager.D2xxException ex) {
            Log.e("SSP FTmanager", ex.toString());
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.setPriority(500);
        this.registerReceiver(mUsbReceiver, filter);


        deviceCom = new ITLDeviceCom();
        deviceCom.setDeviceSetupListener(this);
        deviceCom.setDeviceEventListener(this);
        deviceCom.setDeviceFileUpdateListener(this);
        fab = findViewById(R.id.fab);
        fab.setVisibility(View.GONE);
        /*fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDevice();
                if (ftDev != null) {
                    prgConnect.setVisibility(View.VISIBLE);
                    txtConnect.setVisibility(View.VISIBLE);
                    fab.setEnabled(false);

                    deviceCom.setup(ftDev, 0x00, false, false, 0);
                    deviceCom.start();
                } else {
                    Toast.makeText(MainActivity.this, "No USB connection detected!", Toast.LENGTH_SHORT).show();
                }
            }
        });*/
        grdCurrencyView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                selectedCurrency = grdCurrencyView.getAdapter().getItem(i).toString();
                milkBasePrice = Float.parseFloat(preferencesManager.get(Constants.MilkBasePrice, "0.0").toString());
                milkSetTemperature = Float.parseFloat(preferencesManager.get(Constants.TemperatureSet, "0.0").toString());
                lottieAddCashDialog.show();
                if (ftDev != null) {
                    Toast.makeText(CashCollectorActivity.this, milkSetTemperature + " MilkBase Price " + milkBasePrice, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CashCollectorActivity.this, "Please Wait initiating the Connection!!! ", Toast.LENGTH_SHORT).show();
                }
                openDevice();
            }
        });


        /**
         * Escrow enable/disable toggle
         */
        swEscrow = findViewById(R.id.swEscrow);
        swEscrow.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                deviceCom.SetEscrowMode(isChecked);
            }
        });
        /**
         * Device enable/disable toggle
         */
        SwitchCompat swDisable = findViewById(R.id.swEnable);
        swDisable.setChecked(true);
        swDisable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                deviceCom.SetDeviceEnable(isChecked);

            }
        });
        /**
         * Accept a bill from escrow button
         */
        bttnAccept = findViewById(R.id.bttnAccept);
        bttnAccept.setVisibility(View.INVISIBLE);
        bttnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                deviceCom.SetEscrowAction(SSPSystem.BillAction.Accept);
                Log.e(TAG, "onClick: clicked!!!");
                bttnReject.setVisibility(View.INVISIBLE);
                bttnAccept.setVisibility(View.INVISIBLE);
                Intent intent = new Intent(CashCollectorActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);


            }
        });
        /**
         * Reject a bill from escrow button
         */
        bttnReject = findViewById(R.id.bttnReject);
        bttnReject.setVisibility(View.INVISIBLE);
        bttnReject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deviceCom.SetEscrowAction(SSPSystem.BillAction.Reject);
                bttnReject.setVisibility(View.INVISIBLE);
                bttnAccept.setVisibility(View.INVISIBLE);
            }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        downloadFileSelect = menu.getItem(0);
        downloadFileSelect.setEnabled(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {


        if (item.getItemId() == R.id.action_home) {
            Intent intent = new Intent(CashCollectorActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
        // Handle item selection
        /*switch (item.getItemId()) {
            case R.id.action_downloadFile:
                openFolder();
                return true;
            case R.id.action_shutdown:
                deviceCom.Stop();
                closeDevice();
                finish();
            default:
                return super.onOptionsItemSelected(item);
        }*/

        return super.onOptionsItemSelected(item);
    }

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


}