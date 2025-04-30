package com.imdc.milkdespencer.common;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.FileProvider;


import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.imdc.milkdespencer.CashCollectorActivity;
import com.imdc.milkdespencer.R;
import com.imdc.milkdespencer.enums.UserTypeEnum;
import com.imdc.milkdespencer.adminUi.AdminActivity;
import com.imdc.milkdespencer.models.Response.ConfigurationResponse;
import com.imdc.milkdespencer.models.Response.ResponseOTP;
import com.imdc.milkdespencer.network.ApiManager;
import com.imdc.milkdespencer.network.ApiService;
import com.imdc.milkdespencer.network.Utils;
import com.imdc.milkdespencer.roomdb.AppDatabase;
import com.imdc.milkdespencer.roomdb.entities.LogEntity;
import com.imdc.milkdespencer.roomdb.entities.TransactionEntity;
import com.imdc.milkdespencer.roomdb.entities.User;
import com.imdc.milkdespencer.roomdb.interfaces.LogDao;
import com.imdc.milkdespencer.roomdb.interfaces.TransactionDao;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.observers.DisposableObserver;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class Constants {


    public static final String CashTransactionMode = "CashTransactionMode";

    public static float remainingVolume = 0;

    private static final String PREFS_NAME = "usb_permission_prefs";
    public static final String PREF_PERMISSION_GRANTED = "permission_granted";


    public static final String TAG = "MilkDespencer";
    public static final String MachineId = "MachineId";
    public static final String RazorPayCustomerID = "RazorPayCustomerID";

    public static final String RegisterEndUser = "RegisterUser";

    public static final String RegisterCustomerAdmin = "RegisterCustomerAdmin";

    public static final String OwnerName = "OwnerName";
    //    public static final String MachineId = "MachineId";
    public static final String MilkBasePrice = "MilkBasePrice";
    public static final String LoginUser = "LoginUser";
    public static final String MilkDensityPref = "MilkDensity";

    public static final String ApiBaseUrl = "ApiBaseUrl";

    public static final String ScreenTimeOutPref = "ScreenTimeOut";

    /* This is used for save remaining volume data */
    public static final String RemainingVolumePref = "RemainingVolume";
    public static final String TemperatureOffSet = "TemperatureOffSet";
    public static final String TemperatureSet = "TemperatureSet";
    public static final String CurrentTemperature = "CurrentTemperature";
    public static final String ResponseTempStatus = "ResponseTempStatus";
    public static final String ResponseMilkDispense = "ResponseMilkDispense";

    public static final String PaymentReceived = "PaymentReceived";

    public static final String SavedTransaction = "SavedTransaction";

    public static final String PaymentCashReceived = "PaymentCashReceived";
    public static final String PaidAmt = "PaidAmt";

    public static final String BASE_URL = "https://portal.idmc.coop:5151/api/";


    public static final String GetConfigurationUrl = "api/SMSConfiguration/GetSMSConfiguration";


    public static final String KeyForApi = "Admin";

    public static final String ValueForApi = "Mvb@102405A19022025";

    public static final String SMSApiUrl = "SMSApiUrl";

    public static final String SMSSid = "SMSSid";

    public static final String SMSApiKey = "SMSApiKey";

    public static final String SMSSender = "SMSSender";

    public static final String SMSTemplateId = "SMSTemplateId";

    public static final String SMSTemplateContent = "SMSTemplateContent";

    public static final String RazorPayKey = "RazorPayKey";

    public static final String RazorPaySecretKey = "RazorPaySecretKey";


    public static final String PostTransactionURL = "/Transaction/PostTransaction";
    public static final String PostMerchantURL = "/Merchant/PostMerchant";
    public static final DecimalFormat df = new DecimalFormat("0.00");
    private static final String OTP = "SentOTP";
    // digit or special character
    private static final String PASSWORD_PATTERN = "^(?!.*\\s)(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9!@#$%]).{8,20}$";
    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);
    static SharedPreferencesManager preferencesManager;


    public static final String FromScreen = "FromScreen";

    // Save the permission granted state
    private void handlePermissionGranted() {
        preferencesManager.save(PREF_PERMISSION_GRANTED, true);
    }

    // Reset the permission granted state when USB device is disconnected
    private void handlePermissionRevoked() {
        preferencesManager.save(PREF_PERMISSION_GRANTED, false);
    }


    public static void showAlertDialog(Context context, String title, String message) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        AlertDialog dialog = builder.create();

// Set custom title
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(36); // Increase title font size
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextColor(Color.parseColor("#000000"));
        titleView.setPadding(40, 30, 40, 30);
        titleView.setGravity(Gravity.CENTER);
        dialog.setCustomTitle(titleView);

// Set custom message
        TextView messageView = new TextView(context);
        messageView.setText(message);
        messageView.setTextSize(30); // Increase message font size
        messageView.setPadding(50, 30, 50, 30);
        messageView.setGravity(Gravity.CENTER);
        messageView.setTypeface(null, Typeface.BOLD);
        messageView.setTextColor(Color.parseColor("#000000"));
        ScrollView scrollView = new ScrollView(context); // To handle long messages
        scrollView.addView(messageView);

        dialog.setView(scrollView); // Set the custom message view

// Add a custom positive button
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        dialog.show();

// Set custom width for the dialog
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(dialog.getWindow().getAttributes());
        layoutParams.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.9); // 90% of screen width
        dialog.getWindow().setAttributes(layoutParams);

// Customize buttons after showing the dialog
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

        if (positiveButton != null) {
            positiveButton.setTextSize(26); // Increase button text size
            positiveButton.setPadding(30, 20, 30, 20);
            positiveButton.setTypeface(null, Typeface.BOLD);
            positiveButton.setTextColor(Color.parseColor("#000000"));
        }


//        AlertDialog.Builder builder = new AlertDialog.Builder(context);
//        builder.setTitle(title).setMessage(message).setPositiveButton("OK", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                // Handle positive button click if needed
//                dialog.dismiss();
//            }
//        }).show();
    }

    public static void showAcceptDialog(Context context, String title, String message, DialogInterface.OnClickListener yesClickListener, DialogInterface.OnClickListener noClickListener) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        AlertDialog dialog = builder.create();
// Prevent dismissing on outside touch
        dialog.setCanceledOnTouchOutside(false);
// Set custom title
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(36); // Increase title font size
        titleView.setTypeface(null, Typeface.BOLD); // Bold text
        titleView.setTextColor(Color.parseColor("#000000"));
        titleView.setPadding(20, 20, 20, 20);
        titleView.setGravity(Gravity.CENTER);
        dialog.setCustomTitle(titleView);

// Set custom message
        TextView messageView = new TextView(context);
        messageView.setText(message);
        messageView.setTextSize(32); // Increase message font size
        messageView.setPadding(30, 20, 30, 20);
        messageView.setTypeface(null, Typeface.BOLD); // Bold text
        messageView.setTextColor(Color.parseColor("#000000"));
        messageView.setGravity(Gravity.CENTER);

        ScrollView scrollView = new ScrollView(context); // Optional for long messages
        scrollView.addView(messageView);

        dialog.setView(scrollView); // Set the custom view with increased text size

// Add buttons
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes", yesClickListener);
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No", noClickListener);

        dialog.show();


        // Set custom dialog width
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(dialog.getWindow().getAttributes());
        layoutParams.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.9); // 90% of screen width
        dialog.getWindow().setAttributes(layoutParams);


// Customize buttons
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (positiveButton != null) {
            positiveButton.setTextSize(26); // Increase button text size
            positiveButton.setPadding(20, 20, 20, 20);
            positiveButton.setTypeface(null, Typeface.BOLD); // Bold text
            positiveButton.setTextColor(Color.parseColor("#000000"));
        }

        if (negativeButton != null) {
            negativeButton.setTextSize(26); // Increase button text size
            negativeButton.setPadding(20, 20, 20, 20);
            negativeButton.setTypeface(null, Typeface.BOLD); // Bold text
            negativeButton.setTextColor(Color.parseColor("#000000"));
        }


//        AlertDialog.Builder builder = new AlertDialog.Builder(context);
//        builder.setTitle(title).setMessage(message).setPositiveButton("Yes", yesClickListener).setNegativeButton("No", noClickListener).show();
    }


    public static void showDispenseErrorMessageDialog(Context context, String title, String message, DialogInterface.OnClickListener okClickListener) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        AlertDialog dialog = builder.create();
// Prevent dismissing on outside touch
        dialog.setCanceledOnTouchOutside(false);
// Set custom title
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(36); // Increase title font size
        titleView.setTypeface(null, Typeface.BOLD); // Bold text
        titleView.setTextColor(Color.parseColor("#000000"));
        titleView.setPadding(20, 20, 20, 20);
        titleView.setGravity(Gravity.CENTER);
        dialog.setCustomTitle(titleView);

// Set custom message
        TextView messageView = new TextView(context);
        messageView.setText(message);
        messageView.setTextSize(32); // Increase message font size
        messageView.setPadding(30, 20, 30, 20);
        messageView.setTypeface(null, Typeface.BOLD); // Bold text
        messageView.setTextColor(Color.parseColor("#000000"));
        messageView.setGravity(Gravity.CENTER);

        ScrollView scrollView = new ScrollView(context); // Optional for long messages
        scrollView.addView(messageView);

        dialog.setView(scrollView); // Set the custom view with increased text size

// Add buttons
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Okay", okClickListener);

        dialog.show();


        // Set custom dialog width
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(dialog.getWindow().getAttributes());
        layoutParams.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.9); // 90% of screen width
        dialog.getWindow().setAttributes(layoutParams);


// Customize buttons
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

        if (positiveButton != null) {
            positiveButton.setTextSize(26); // Increase button text size
            positiveButton.setPadding(20, 20, 20, 20);
            positiveButton.setTypeface(null, Typeface.BOLD); // Bold text
            positiveButton.setTextColor(Color.parseColor("#000000"));
        }


//        AlertDialog.Builder builder = new AlertDialog.Builder(context);
//        builder.setTitle(title).setMessage(message).setPositiveButton("Yes", yesClickListener).setNegativeButton("No", noClickListener).show();
    }


    public static void showConfigDialog(Context context) {
        // Create a layout inflater to inflate the custom dialog layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.configuration_dialog, null);
        final double[] density = new double[1];
        preferencesManager = SharedPreferencesManager.getInstance(context);
        // Create the AlertDialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(view);

        // Create the AlertDialog
        AlertDialog dialog = builder.create();

        // Find views in the custom layout
        MaterialButton okButton = view.findViewById(R.id.okButton);
        MaterialButton cancelButton = view.findViewById(R.id.cancelButton);
        TextInputEditText tieMilkBasePrice = view.findViewById(R.id.tieMilkBasePrice);
        TextInputEditText tieMilkDensity = view.findViewById(R.id.tieMilkDensity);
        TextInputEditText tieTimeOut = view.findViewById(R.id.tieTimeOut);


        tieMilkBasePrice.setText(preferencesManager.get(MilkBasePrice, "0.0").toString());
        tieMilkDensity.setText(preferencesManager.get(MilkDensityPref, "0.0").toString());
        tieTimeOut.setText(preferencesManager.get(ScreenTimeOutPref, "0").toString());


        // Set click listener for OK button
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle OK button click
                String milkBasePrice = tieMilkBasePrice.getText().toString();
                String milkDensity = tieMilkDensity.getText().toString();
                String screenTimeOut = tieTimeOut.getText().toString();
                preferencesManager.save(MilkBasePrice, milkBasePrice);
                preferencesManager.save(MilkDensityPref, milkDensity);
                preferencesManager.save(ScreenTimeOutPref, screenTimeOut);

                dialog.dismiss();
            }
        });

        // Set click listener for Cancel button
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle Cancel button click
                // Dismiss the dialog
                dialog.dismiss();
            }
        });

        // Show the dialog
        dialog.show();
    }

    /**
     * Api configuration dialog
     *
     * @param context
     */
    public static void showAPIConfigDialog(Context context) {
        // Create a layout inflater to inflate the custom dialog layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.api_configuration_dialog, null);
        final double[] density = new double[1];
        preferencesManager = SharedPreferencesManager.getInstance(context);
        // Create the AlertDialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(view);

        // Create the AlertDialog
        AlertDialog dialog = builder.create();

        // Find views in the custom layout
        MaterialButton okButton = view.findViewById(R.id.okButton);
        MaterialButton cancelButton = view.findViewById(R.id.cancelButton);
        TextInputEditText tieApiBaseUrl = view.findViewById(R.id.tieApiBaseUrl);

        /// If url is not set in shared preference.. It will take default base url
        tieApiBaseUrl.setText(preferencesManager.get(ApiBaseUrl, "https://portal.idmc.coop:5151/").toString());

        // Set click listener for OK button
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle OK button click
                String apiBaseUrl = tieApiBaseUrl.getText().toString();
                preferencesManager.save(apiBaseUrl, apiBaseUrl);
                dialog.dismiss();
            }
        });

        // Set click listener for Cancel button
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle Cancel button click
                // Dismiss the dialog
                dialog.dismiss();
            }
        });

        // Show the dialog
        dialog.show();
    }


    /*
    Show admin Config Dialog
     */
    public static void showAdminConfigDialog(Context context, int userType) {
        // Create a layout inflater to inflate the custom dialog layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.admin_configuration_dialog, null);

        preferencesManager = SharedPreferencesManager.getInstance(context);
        // Create the AlertDialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(view);

        // Create the AlertDialog
        AlertDialog dialog = builder.create();

        // Find views in the custom layout
        MaterialButton okButton = view.findViewById(R.id.okButton);
        MaterialButton cancelButton = view.findViewById(R.id.cancelButton);

        TextInputLayout tilMachineId = view.findViewById(R.id.tilMachineId);
        TextInputLayout tilTemperatureOffset = view.findViewById(R.id.tilTemperatureOffset);
        TextInputLayout tilTemperatureSet = view.findViewById(R.id.tilSetTemperature);
        TextInputLayout tilOwnerNameId = view.findViewById(R.id.tilOwnerNameId);

        TextInputLayout tilMilkBasePrice = view.findViewById(R.id.tilMilkBasePrice);
        TextInputLayout tilMilkDensity = view.findViewById(R.id.tilMilkDensity);
        TextInputLayout tilTimeOut = view.findViewById(R.id.tilTimeOut);

        TextInputEditText tieTemperatureOffset = view.findViewById(R.id.tieTemperatureOffset);
        TextInputEditText tieTemperatureSet = view.findViewById(R.id.tieSetTemperature);
        TextInputEditText tieOwnerNameId = view.findViewById(R.id.tieOwnerNameId);

        TextInputEditText tieMilkBasePrice = view.findViewById(R.id.tieMilkBasePrice);
        TextInputEditText tieMilkDensity = view.findViewById(R.id.tieMilkDensity);
        TextInputEditText tieTimeOut = view.findViewById(R.id.tieTimeOut);


        /// Machine Id Caps Capital
        TextInputEditText tieMachineId = view.findViewById(R.id.tieMachineId);
        tieMachineId.setFilters(new InputFilter[]{new InputFilter.AllCaps()});

        if (userType == UserTypeEnum.ADMIN.value()) {
            tilMachineId.setEnabled(true);
        } else if (userType == UserTypeEnum.CUSTOMER_ADMIN.value()) {
            tieMachineId.setEnabled(false);
        } else if (userType == UserTypeEnum.END_USER.value()) {
            tieMachineId.setEnabled(false);
            tilTemperatureOffset.setEnabled(false);
            tilTemperatureSet.setEnabled(false);
            tilMilkBasePrice.setEnabled(false);
            tilMilkDensity.setEnabled(false);
            tilTimeOut.setEnabled(false);
            okButton.setText("Ok");

        }

        tilMachineId.setErrorEnabled(true);
        tilTemperatureOffset.setErrorEnabled(true);
        tilTemperatureSet.setErrorEnabled(true);
        tilMilkBasePrice.setErrorEnabled(true);
        tilMilkDensity.setErrorEnabled(true);
        tilTimeOut.setErrorEnabled(true);

        String machineId = preferencesManager.get(MachineId, "000000A31122024").toString();

        //  tilMachineId.setEnabled(machineId.isEmpty() || machineId.equalsIgnoreCase("MachineId"));
        tieMachineId.setText(machineId);
        tieOwnerNameId.setText(preferencesManager.get(OwnerName, "0.0").toString());
        tieTemperatureOffset.setText(preferencesManager.get(TemperatureOffSet, "2.26").toString());
        tieTemperatureSet.setText(preferencesManager.get(TemperatureSet, "8.0").toString());
        tieMilkBasePrice.setText(preferencesManager.get(MilkBasePrice, "100.0").toString());
        tieMilkDensity.setText(preferencesManager.get(MilkDensityPref, "1.0").toString());
        tieTimeOut.setText(preferencesManager.get(ScreenTimeOutPref, "15").toString());

        // Set click listener for OK button
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle OK button click


                /// If user type is not equal 1. The add into shared preference
                if (userType != 1) {
                    if (tieMachineId.getText().toString().length() != 15) {
                        Toast.makeText(context, context.getString(R.string.machineId_validation), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String machineID = tieMachineId.getText().toString();
                    String ownerName = tieOwnerNameId.getText().toString();
                    String setTemperature = tieTemperatureSet.getText().toString();
                    String offSetTemperature = tieTemperatureOffset.getText().toString();
                    String milkBasePrice = tieMilkBasePrice.getText().toString();
                    String milkDensity = tieMilkDensity.getText().toString();
                    String screenTimeOut = tieTimeOut.getText().toString();

                    Log.e("offSetTemperature", offSetTemperature);

                    preferencesManager.save(MachineId, machineID);
                    preferencesManager.save(OwnerName, ownerName);
                    preferencesManager.save(TemperatureSet, setTemperature);
                    preferencesManager.save(TemperatureOffSet, offSetTemperature);
                    preferencesManager.save(MilkBasePrice, milkBasePrice);
                    preferencesManager.save(MilkDensityPref, milkDensity);
                    preferencesManager.save(ScreenTimeOutPref, screenTimeOut);

                }


                dialog.dismiss();
            }
        });

        // Set click listener for Cancel button
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle Cancel button click
                // Dismiss the dialog
                dialog.dismiss();
            }
        });

        // Show the dialog
        dialog.show();
    }

    /*
     * Show Dialog for added volume
     * */
    public static void showAddedVolumeDialog(Activity context) {
        // Create a layout inflater to inflate the custom dialog layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.added_volume_configuration_dialog, null);

        preferencesManager = SharedPreferencesManager.getInstance(context);
        // Create the AlertDialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(view);
        builder.setCancelable(false);

        // Create the AlertDialog
        AlertDialog dialog = builder.create();

        // Find views in the custom layout
        MaterialButton okButton = view.findViewById(R.id.okButton);
        MaterialButton cancelButton = view.findViewById(R.id.cancelButton);

        TextInputEditText tieVolume = view.findViewById(R.id.tieVolume);
        AppCompatTextView tvRemainingVolume = view.findViewById(R.id.tv_remaining_volume);

        remainingVolume = Float.parseFloat(preferencesManager.get(RemainingVolumePref, "0").toString());
        tvRemainingVolume.setText("Remaining Volume : " + String.valueOf(remainingVolume));

        // Set click listener for OK button
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle OK button click

                if (tieVolume.getText().toString().isEmpty()) {
                    Toast.makeText(context, "Please enter volume", Toast.LENGTH_SHORT).show();
                }else if(Float.parseFloat(tieVolume.getText().toString()) > 200 || Float.parseFloat(tieVolume.getText().toString()) < 10){
                    Toast.makeText(context, "Enter Valid Value", Toast.LENGTH_SHORT).show();
                }else {

                    float tempRemainVolume = remainingVolume + Float.parseFloat(tieVolume.getText().toString());
                    if(tempRemainVolume > 200){
                        Toast.makeText(context, "Enter Valid Value", Toast.LENGTH_SHORT).show();
                    }else {
                        remainingVolume += Float.parseFloat(tieVolume.getText().toString());

                        preferencesManager.save(RemainingVolumePref, String.valueOf(remainingVolume));

                        new Thread(() -> {
                            try {
                                String date = new SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis());
                                String time = new SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis());
                                TransactionDao transactionDao = AppDatabase.getInstance(context).transactionDao();
                                TransactionEntity transaction = new TransactionEntity();
                                transaction.setUserName("");
                                transaction.setPassword("");
                                transaction.setTransactionType("");
                                transaction.setBankTransactionNo("");
                                transaction.setRemainingvolume(remainingVolume);
                                transaction.setTransactionDate(date);
                                transaction.setTransactionTime(time);
                                transaction.setAmount(0);
                                transaction.setUploadToServer(0);
                                transaction.setVolume(Float.parseFloat(tieVolume.getText().toString()));
                                transaction.setTransactionStatus("REFILLED");
                                transaction.setUpiId("");

                                String uniqueId = generateSafeUniqueTransactionId(transactionDao);
                                transaction.setUniqueTransactionId(uniqueId);

                                /// Added new on 4-1-2025
                                transaction.setMilkPrice(preferencesManager.get(MilkBasePrice, "").toString());
                                transaction.setMilkTemperature("222");

                                /// Added on 1-1 2025
                                transaction.setMachineId(preferencesManager.get(MachineId, "").toString());

                                /// Insert into Sqlite database
                                long transactionId = transactionDao.insert(transaction);
                                transaction.setId(transactionId);

                                if (isNetworkAvailable(context)) {
                                    doPostTransaction(preferencesManager, "/api/Transaction/PostTransaction", transaction, transactionDao);
                                } else {
                                    Constants.saveLogs(context, "Internet Connection Error");
                                    //   Toast.makeText(activity, "Internet not available", Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();

                        dialog.dismiss();
                    }


                }


            }
        });

        // Set click listener for Cancel button
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle Cancel button click
                // Dismiss the dialog
                dialog.dismiss();
            }
        });

        // Show the dialog
        dialog.show();
    }


    /*
     * if CIP is true = > Show this dialog
     * */
    public static void showCIPRunningDialog(Context context, DialogInterface.OnClickListener stopClickListener, String title) {
        // Create a layout inflater to inflate the custom dialog layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_cip_running, null);
        AlertDialog cipDialog;
        // Create the AlertDialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(view);

        // Create the AlertDialog
        cipDialog = builder.create();
        cipDialog.setCancelable(false);

        TextView tvCIPRunning = view.findViewById(R.id.tvCIPRunning);
        tvCIPRunning.setText(title);
        MaterialButton btnStopCIP = view.findViewById(R.id.btnStopCIP);
        btnStopCIP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (stopClickListener != null) {
                    stopClickListener.onClick(cipDialog, DialogInterface.BUTTON_NEGATIVE);
                    cipDialog.dismiss();
                }
            }
        });


        // Show the dialog
        cipDialog.show();

        // Make dialog larger — for example, 90% of screen width and height
        Window window = cipDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());
            layoutParams.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.9);
            layoutParams.height = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.8);
            window.setAttributes(layoutParams);
        }
    }


    public static void showLoginDialog(Context context, AppDatabase appDatabase) {
        // Create a layout inflater to inflate the custom dialog layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.login_dialog, null);

        preferencesManager = SharedPreferencesManager.getInstance(context);
        // Create the AlertDialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(view);
        builder.setCancelable(false);
        // Create the AlertDialog
        AlertDialog dialog = builder.create();

        // Find views in the custom layout
        MaterialButton loginButton = view.findViewById(R.id.loginButton);
        MaterialButton cancelButton = view.findViewById(R.id.cancelButton);

        TextInputLayout tilUserName = view.findViewById(R.id.tilUsername);
        TextInputLayout tilPassword = view.findViewById(R.id.tilPassword);
        TextInputEditText tieUsername = view.findViewById(R.id.tieUsername);
        TextInputEditText tiePassword = view.findViewById(R.id.tiePassword);
        TextView tvForgotPassword = view.findViewById(R.id.tvForgotPassword);

        tilUserName.setErrorEnabled(true);
        tilPassword.setErrorEnabled(true);
//        tieUsername.setText("admin");
//        tiePassword.setText("Admin@123");
        // Set click listener for Login button

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle Login button click
                String username = tieUsername.getText().toString();
                String password = tiePassword.getText().toString();
                Handler handler = new Handler(Looper.getMainLooper());
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        List<User> userLIst = appDatabase.userDao().getAllUsers();
                        Log.e("length of user", String.valueOf(userLIst.size()));
                        for (int i = 0; i < userLIst.size(); i++) {
                            Log.e("email", userLIst.get(i).getUsername());
                            Log.e("email", userLIst.get(i).getPassword());
                        }

                        User login = appDatabase.userDao().login(username, password);

                        if (login != null) {
                            Log.e(TAG, "onClick: " + new Gson().toJson(login));
                            preferencesManager.save(Constants.LoginUser, new Gson().toJson(login));
                            Intent intent = new Intent(context.getApplicationContext(), AdminActivity.class);
                            intent.putExtra(Constants.LoginUser, new Gson().toJson(login));
                            context.startActivity(intent);
                            dialog.dismiss();
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {

                                    Log.e(TAG, "onClick: " + new Gson().toJson(login));

                                    Toast.makeText(context, "Please Enter Valid Username and Password!!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }).start();
            }
        });

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle Cancel button click
                // Dismiss the dialog
                dialog.dismiss();
                showForgotPasswordDialog(context, appDatabase);


            }
        });

        // Show the dialog
        dialog.show();
    }

    public static void showForgotPasswordDialog(Context context, AppDatabase appDatabase) {

        preferencesManager = SharedPreferencesManager.getInstance(context);

        final boolean[] isOtpSend = {false};

        /// Url get from shared preference
        Retrofit retrofit = new Retrofit.Builder().baseUrl(preferencesManager.get(SMSApiUrl, "https://api.kaleyra.io/v1/").toString()) // Replace with your base URL
                .addConverterFactory(GsonConverterFactory.create()).addCallAdapterFactory(RxJava3CallAdapterFactory.create()) // Add this line
                .addConverterFactory(GsonConverterFactory.create()).build();


        Log.e(TAG + " SMS URL ", preferencesManager.get(SMSApiUrl, "https://api.kaleyra.io/v1/").toString());


//        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://api.kaleyra.io/v1/") // Replace with your base URL
//                .addConverterFactory(GsonConverterFactory.create()).addCallAdapterFactory(RxJava3CallAdapterFactory.create()) // Add this line
//                .addConverterFactory(GsonConverterFactory.create()).build();

        ApiService apiService = retrofit.create(ApiService.class);

        ApiManager apiManager = new ApiManager(apiService);

        // Create a layout inflater to inflate the custom dialog layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.forgot_password, null);

        preferencesManager = SharedPreferencesManager.getInstance(context);
        // Create the AlertDialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(view);

        // Create the AlertDialog
        AlertDialog dialog = builder.create();

        // Find views in the custom layout
        TextInputLayout tilUsername = view.findViewById(R.id.tilForgotUsername);
        TextInputLayout tilOtp = view.findViewById(R.id.tilOtp);
        TextInputLayout tilNewPassword = view.findViewById(R.id.tilNewPassword);
        TextInputLayout tilConfirmPassword = view.findViewById(R.id.tilConfirmPassword);

        TextInputEditText tieMobileNo = view.findViewById(R.id.tiePhoneNo);
        TextInputEditText tieOtp = view.findViewById(R.id.tieOtp);
        TextInputEditText tieNewPassword = view.findViewById(R.id.tieNewPassword);
        TextInputEditText tieConfirmPassword = view.findViewById(R.id.tieConfirmPassword);

        MaterialButton btnSendOtp = view.findViewById(R.id.btnSendOtp);
        MaterialButton btnResetPassword = view.findViewById(R.id.btnResetPassword);

        tilUsername.setErrorEnabled(true);
        tilNewPassword.setErrorEnabled(true);
        tilConfirmPassword.setErrorEnabled(true);
        tilOtp.setErrorEnabled(true);

        tilOtp.setVisibility(View.GONE);
        tilNewPassword.setVisibility(View.GONE);
        tilConfirmPassword.setVisibility(View.GONE);
        btnResetPassword.setVisibility(View.GONE);

        tilOtp.setVisibility(View.VISIBLE);
        tilNewPassword.setVisibility(View.VISIBLE);
        tilConfirmPassword.setVisibility(View.VISIBLE);
        btnResetPassword.setVisibility(View.VISIBLE);

//        btnSendOtp.setText(isOtpSend[0] ? "Verify OTP" : "Send OTP");

        // Set click listener for Login button
        btnSendOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNo = tieMobileNo.getText().toString();
                if (isValidMobileNumber(phoneNo)) {

                    /// New code
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Integer mobileNoExists = appDatabase.userDao().mobileNoExists(phoneNo);
                            if (mobileNoExists <= 0) {
                                Handler handler = new Handler(Looper.getMainLooper());
                                handler.post(() -> Toast.makeText(context, "Please Enter Valid Mobile No!!", Toast.LENGTH_SHORT).show());
                            } else {
                                // Run UI-related operations on the main thread
                                Handler handler = new Handler(Looper.getMainLooper());
                                handler.post(() -> {
                                    ProgressDialog pd = new ProgressDialog(context);
                                    pd.setTitle("Please Wait...");
                                    pd.setTitle("Please Wait...");
                                    pd.setCancelable(false);
                                    pd.show();

                                    // Perform network operation in a background thread
                                    new Thread(() -> {
                                        String otp = "OTP for MVM password reset is " + generateOtp(6) + ". -IDMC";
                                        String content = "to=" + phoneNo + "&type=OTP&sender=" + preferencesManager.get(SMSSender, "IDMCCS").toString() + "&body=" + otp;
                                        Log.e(TAG, "onClick: " + content);

                                        HashMap<String, String> fields = new HashMap<>();
                                        fields.put("to", "+91" + phoneNo);
                                        fields.put("type", "OTP");

                                        /// Get from shared preference
                                        fields.put("sender", preferencesManager.get(SMSSender, "IDMCCS").toString());

//                                        fields.put("sender", "IDMCCS");
                                        fields.put("body", otp);

                                        /// Get from shared preference
                                        fields.put("api-key", preferencesManager.get(SMSApiKey, "Ae0de2903bdeb26110fd03ccab96e92a1").toString());
//                                        fields.put("api-key", "Ae0de2903bdeb26110fd03ccab96e92a1");

                                        Log.e("fields OF SMS ", fields.toString());


                                        HashMap<String, String> headers = new HashMap<>();
                                        headers.put("Content-Type", "application/x-www-form-urlencoded");
                                        headers.put("api-key", preferencesManager.get(SMSApiKey, "Ae0de2903bdeb26110fd03ccab96e92a1").toString());
                                        Log.e("headers OF sms ", headers.toString());
                                        /// Old API : A5b9c8ba406fbc9bf361ffeb8bf6cb120

                                        DisposableObserver<ResponseBody> disposableObserver = new DisposableObserver<ResponseBody>() {
                                            @Override
                                            public void onNext(ResponseBody response) {
                                                handler.post(() -> {
                                                    pd.dismiss(); // Dismiss the ProgressDialog on the main thread
                                                    if (!response.toString().isEmpty()) {
                                                        isOtpSend[0] = true;
                                                        ResponseOTP responseModel = new Gson().fromJson(response.charStream(), ResponseOTP.class);
                                                        if (responseModel != null) {
                                                            Log.e(TAG, "onNext: " + new Gson().toJson(responseModel));
                                                            if (responseModel.getError() != null) {
                                                                tilOtp.setVisibility(View.VISIBLE);
                                                                tilNewPassword.setVisibility(View.VISIBLE);
                                                                tilConfirmPassword.setVisibility(View.VISIBLE);
                                                                btnResetPassword.setVisibility(View.VISIBLE);

                                                                btnSendOtp.setVisibility(View.GONE);
                                                                tilUsername.setVisibility(View.GONE);

                                                                String OTP = extractOTP(responseModel.getBody());
                                                                preferencesManager.save(Constants.OTP, OTP);
                                                            }
                                                        }
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onError(Throwable e) {
                                                handler.post(() -> {

                                                    Log.e("SMS onError", e.toString());

                                                    pd.dismiss(); // Dismiss the ProgressDialog on the main thread
                                                    Utils.handleApiError(context, e, apiManager);
                                                });
                                            }

                                            @Override
                                            public void onComplete() {
                                                // Handle completion if needed
                                            }
                                        };

                                        /// Get from shared preference
                                        apiManager.makeOTPRequestCall("", fields, headers, disposableObserver);


                                        Log.e("SMS URL ", (preferencesManager.get(SMSSid, "").toString()) + (preferencesManager.get(SMSSid, "").toString())
                                                + "/messages/");

                                        //  apiManager.makeOTPRequestCall("HXIN1764058706IN/messages/", fields, headers, disposableObserver);
                                    }).start();
                                });
                            }
                            Log.e(TAG, "onClick:mobileNoExists " + mobileNoExists);
                        }
                    }).start();


//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Integer mobileNoExists = appDatabase.userDao().mobileNoExists(phoneNo);
//                            if (mobileNoExists <= 0) {
//                                Handler handler = new Handler(Looper.getMainLooper());
//                                handler.post(() -> Toast.makeText(context, "Please Enter Valid Mobile No!!", Toast.LENGTH_SHORT).show());
//                            } else {
//
//
//                                ProgressDialog pd = new ProgressDialog(context);
//                                pd.setTitle("Please Wait...");
//                                pd.setCancelable(false);
//                                pd.show();
//
//                                String otp = "OTP for MVM password reset is " + generateOtp(6) + ". -IDMC";
//                                String content = "to=" + phoneNo + "&type=OTP&sender=IDMCCS&body=" + otp;
//                                Log.e(TAG, "onClick: " + content);
//
//                                HashMap<String, String> fields = new HashMap<>();
//                                fields.put("to", "+91" + phoneNo);
//                                fields.put("type", "OTP");
//                                fields.put("sender", "IDMCCS");
//                                fields.put("body", otp);
//                                fields.put("api-key", "A5b9c8ba406fbc9bf361ffeb8bf6cb120");
//
//                                HashMap<String, String> headers = new HashMap<>();
//                                headers.put("Content-Type", "application/x-www-form-urlencoded");
//                                headers.put("api-key", "A5b9c8ba406fbc9bf361ffeb8bf6cb120");
//                                DisposableObserver<ResponseBody> disposableObserver = new DisposableObserver<ResponseBody>() {
//                                    @Override
//                                    public void onNext(ResponseBody response) {
//                                        pd.dismiss();
//                                        if (!response.toString().isEmpty()) {
//                                            isOtpSend[0] = true;
//                                            ResponseOTP responseModel = new Gson().fromJson(response.charStream(), ResponseOTP.class);
//                                            if (responseModel != null) {
//                                                Log.e(TAG, "onNext: " + new Gson().toJson(responseModel));
//                                                if (responseModel.getError() != null) {
//                                                    tilOtp.setVisibility(View.VISIBLE);
//                                                    tilNewPassword.setVisibility(View.VISIBLE);
//                                                    tilConfirmPassword.setVisibility(View.VISIBLE);
//                                                    btnResetPassword.setVisibility(View.VISIBLE);
//
//                                                    btnSendOtp.setVisibility(View.GONE);
//                                                    tilUsername.setVisibility(View.GONE);
//
//                                                    String OTP = extractOTP(responseModel.getBody());
//                                                    preferencesManager.save(Constants.OTP, OTP);
//
//
//                                                }
//                                            }
//                                        }
//                                    }
//
//                                    @Override
//                                    public void onError(Throwable e) {
//                                        // Handle the error
//                                        pd.dismiss();
//                                        Utils.handleApiError(context, e, apiManager);
//                                    }
//
//                                    @Override
//                                    public void onComplete() {
//                                        // Handle completion if needed
//                                    }
//                                };
//                                apiManager.makeOTPRequestCall("HXIN1764058706IN/messages/", fields, headers, disposableObserver);
//                            }
//                            Log.e(TAG, "onClick:mobileNoExists " + mobileNoExists);
//                        }
//                    }).start();


                } else {
                    tieMobileNo.setError(context.getString(R.string.alertForValidMobile));
                }
            }
        });

        btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle Cancel button click
                // Dismiss the dialog

                String password = tieNewPassword.getText().toString();
                String confirmPassword = tieNewPassword.getText().toString();
                String otp = tieOtp.getText().toString();

                String sentOTP = (String) preferencesManager.get(Constants.OTP, "");
                Log.e(TAG, "onClick: " + sentOTP);


                if (otp.isEmpty()) {
                    tilOtp.setError("Field Cannot be empty!!");
                } else {
                    if (otp.length() < 6) {

                    }
                }


                if (isValid(password) && isValid(confirmPassword) && password.equalsIgnoreCase(confirmPassword)) {

                    Executors.newSingleThreadExecutor().execute(() -> {
                        // Perform the query in the background thread
                        User user = appDatabase.userDao().getUserByMobile(tieMobileNo.getText().toString());

                        if (user != null) {
                            // Update the user object
                            user.setPassword(password);

                            // Update the user in the database
                            appDatabase.userDao().update(user);

                            /// If user type is end user
                            if (user.getUserType() == UserTypeEnum.END_USER.value()) {
                                preferencesManager.save(RegisterEndUser, new Gson().toJson(user));

                            } else if (user.getUserType() == UserTypeEnum.CUSTOMER_ADMIN.value()) {
                                /// If user type is customer user
                                preferencesManager.save(RegisterCustomerAdmin, new Gson().toJson(user));
                            }

                            // Optionally handle the UI on the main thread
                            new Handler(Looper.getMainLooper()).post(() -> {
                                Log.e("first name", user.getFirst_name());
                                Log.e("new password", user.getPassword());
                                Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_SHORT).show();

                                dialog.cancel();

                            });
                        } else {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                Toast.makeText(context, "User not found!", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });


                } else {
                    // password is invalid, show error message
                    if (!isValid(password)) {
                        tilNewPassword.setError("Password must contain at least one lowercase character, one uppercase character, one digit, one special character, and is between 8 to 20 characters long.");
                    } else if (!isValid(confirmPassword)) {
                        tilConfirmPassword.setError("Password must contain at least one lowercase character, one uppercase character, one digit, one special character, and is between 8 to 20 characters long.");
                    } else {
                        if (password.isEmpty()) {
                            tilNewPassword.setError("Field cannot be empty.");
                        }
                        if (confirmPassword.isEmpty()) {
                            tilConfirmPassword.setError("Field cannot be empty.");
                        }
//                        tieConfirmPassword.setError("Confirm Password did not match..");

                    }


                }
//                dialog.dismiss();
            }
        });

        // Show the dialog
        dialog.show();
    }

    public static boolean isValid(final String password) {
        Matcher matcher = pattern.matcher(password);
        return matcher.matches();
    }

    public static String generateOtp(int length) {
        String otp = "";
        String characters = "0123456789";
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(characters.length());
            otp += characters.charAt(randomIndex);
        }

        return otp;
    }

    public static String extractOTP(String inputString) {
        // The regex pattern to match the OTP format: a 6-digit number
        String regexPattern = "\\b\\d{6}\\b";

        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher(inputString);

        if (matcher.find()) {
            String otp = matcher.group();
            Log.d("OTP_TAG", "Extracted OTP: " + otp);
            return otp;
        } else {
            Log.d("OTP_TAG", "No OTP found in the input string");
            return null;
        }
    }

    public static boolean isValidMobileNumber(String mobileNumber) {
        String regex = "^[0-9]{10}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(mobileNumber);
        return matcher.matches();
    }


    public static double calculateMilkWeight(double literValue, Context context) {
        preferencesManager = SharedPreferencesManager.getInstance(context);
        float milkBasePrice = Float.parseFloat(preferencesManager.get(Constants.MilkBasePrice, "0.0").toString());

        float DENSITY_OF_MILK = Float.parseFloat(preferencesManager.get(Constants.MilkDensityPref, "0.0").toString());
        double amountInLiters = milkBasePrice * literValue;
        Log.e(TAG, "calculateMilkWeight: BP " + milkBasePrice + " <+++> " + amountInLiters);

        return amountInLiters * DENSITY_OF_MILK;
    }


    public static double calculateMilkPrice(double literValue, Context context) {
        preferencesManager = SharedPreferencesManager.getInstance(context);
        float milkBasePrice = Float.parseFloat(preferencesManager.get(Constants.MilkBasePrice, "0.0").toString());

        double amountInLiters = milkBasePrice * literValue;
        Log.e(TAG, "calculateMilkWeight: BP " + milkBasePrice + " <+++> " + amountInLiters);

        return amountInLiters;
    }


    public static double calculateMilkAmount(double cost, Context context) {
        preferencesManager = SharedPreferencesManager.getInstance(context);

        float milkBasePrice = Float.parseFloat(preferencesManager.get(Constants.MilkBasePrice, "0.0").toString());
        float DENSITY_OF_MILK = Float.parseFloat(preferencesManager.get(Constants.MilkDensityPref, "0.0").toString());

        Log.e(TAG, "calculateMilkAmount: BasePrice " + milkBasePrice + " <+++> " + (cost / milkBasePrice));
        return (cost / milkBasePrice) * DENSITY_OF_MILK;
    }


    public static void saveLogs(Context context, String message) {
        preferencesManager = SharedPreferencesManager.getInstance(context);
        new Thread(new Runnable() {
            @Override
            public void run() {
                AppDatabase database = AppDatabase.getInstance(context);
                LogDao logDao = database.logDao();
                LogEntity logEntity = new LogEntity(message, preferencesManager.get(Constants.MachineId, "000000A31122024").toString(), "", "", 0);
                long logId = logDao.insert(logEntity);
                logEntity.setId((int) logId);

                Log.e(TAG, "run: saveLogs " + logDao.getAllLogs());

                if (logId > 0 && isNetworkAvailable(context)) {
                    doPostLog(preferencesManager, "/api/Log/PostLog", logEntity, logDao);
                }


            }
        }).start();


    }

    public static AlertDialog showLottieDialog(Context context, double percentage) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_lottie, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(view);

        AlertDialog dialog = builder.create();


        LottieAnimationView lottieAnimationView = view.findViewById(R.id.lottieAnimationView);
        TextView tvProgress = view.findViewById(R.id.tvProgressDialog);
        tvProgress.setText("Please Wait " + (percentage != 0 ? percentage : ""));

        return dialog;
    }


    public static String generateRandomTransactionId() {
        long timestamp = System.currentTimeMillis();

        // Generate a random number
        Random random = new Random();
        int randomNumber = random.nextInt(1000000); // Adjust the range as needed

        return String.valueOf(timestamp) + randomNumber;
    }

    public static long insertTransaction(Activity activity, TransactionDao transactionDao, String transactionType, String bankTransactionNo, String transactionDate, String transactionTime, double amount, String transactionStatus, String upiId, float volume, String milkTemperature) {
        preferencesManager = SharedPreferencesManager.getInstance(activity);

        remainingVolume = Float.parseFloat(preferencesManager.get(RemainingVolumePref, "0").toString());
        remainingVolume = remainingVolume - volume;
        preferencesManager.save(RemainingVolumePref, String.valueOf(remainingVolume));

        TransactionEntity transaction = new TransactionEntity();
        transaction.setUserName("");
        transaction.setPassword("");
        transaction.setTransactionType(transactionType);
        transaction.setBankTransactionNo(bankTransactionNo);
        transaction.setTransactionDate(transactionDate);
        transaction.setTransactionTime(transactionTime);
        transaction.setAmount(amount);
        transaction.setVolume(volume);
        transaction.setRemainingvolume(remainingVolume);

        /// Added new on 4-1-2025
        transaction.setMilkPrice(preferencesManager.get(MilkBasePrice, "").toString());
        transaction.setMilkTemperature(milkTemperature);

        transaction.setTransactionStatus(transactionStatus);
        transaction.setUpiId(upiId);
        transaction.setUploadToServer(1);

        try {
            String uniqueId = generateSafeUniqueTransactionId(transactionDao);

            transaction.setUniqueTransactionId(uniqueId);

//        transaction.setUniqueTransactionId(transactionDao.generateUniqueTransactionId());

            /// Added on 1-1 2025
            transaction.setMachineId(preferencesManager.get(MachineId, "").toString());

            // Insert into Sqlite database
            long transactionId = transactionDao.insert(transaction);
            transaction.setId(transactionId);


            if (transactionId > 0 && isNetworkAvailable(activity)) {

                Executors.newSingleThreadExecutor().execute(() -> {
                    doPostTransaction(preferencesManager, "/api/Transaction/PostTransaction", transaction, transactionDao);
                });
                // doPostTransaction(preferencesManager, "/api/Transaction/PostTransaction", transaction, activity);
            } else {

                Constants.saveLogs(activity, "Internet Connection Error");

                //   Toast.makeText(activity, "Internet not available", Toast.LENGTH_SHORT).show();
            }

            return transactionId;
        } catch (Exception e) {
            Log.e("InsertTransaction", "Failed to insert transaction: " + e.getMessage(), e);
            return -1;
        }


    }


    /// Update the transactions
    public static void updateTransaction(Context activity, TransactionDao transactionDao, long transactionId, String transactionStatus, float volume, String milkTemperature, TransactionEntity transaction) {
        preferencesManager = SharedPreferencesManager.getInstance(activity);

        remainingVolume = Float.parseFloat(preferencesManager.get(RemainingVolumePref, "0").toString());
        remainingVolume = remainingVolume - volume;
        preferencesManager.save(RemainingVolumePref, String.valueOf(remainingVolume));

        transactionDao.updateTransactionDetails(
                String.valueOf(transactionId),           // Unique Transaction ID
                volume,                 // volume
                preferencesManager.get(MilkBasePrice, "").toString(),              // milk price
                milkTemperature,
                transactionStatus,// milk temperature,
                remainingVolume

        );

        transaction.setVolume(volume);
        transaction.setMilkPrice(preferencesManager.get(MilkBasePrice, "").toString());
        transaction.setMilkTemperature(milkTemperature);
        transaction.setTransactionStatus(transactionStatus);
        transaction.setRemainingvolume(remainingVolume);

        if (transactionId > 0 && isNetworkAvailable(activity)) {

            Executors.newSingleThreadExecutor().execute(() -> {


//                if (activity != null && !activity.isFinishing()) {
//                    activity.runOnUiThread(() -> Log.e("api call ", "Yes"));
//                }


                doPostTransaction(preferencesManager, "/api/Transaction/PostTransaction", transaction, transactionDao);
            });


        } else {
            Constants.saveLogs(activity, "Internet Connection Error");
            preferencesManager.delete(Constants.PaymentReceived);
            preferencesManager.delete(Constants.PaidAmt);
            preferencesManager.delete(Constants.SavedTransaction);

            //   Toast.makeText(activity, "Internet not available", Toast.LENGTH_SHORT).show();
        }

    }


    public static String generateSafeUniqueTransactionId(TransactionDao transactionDao) {
        long lastId = transactionDao.getLastTransactionId(); // Returns 0 if table is empty
        long nextId = lastId + 1;

        String uniqueId;
        int retryCount = 0;

        do {
            uniqueId = "TXN" + String.format("%05d", nextId);
            nextId++;
            retryCount++;

            // Fail-safe to avoid infinite loops
            if (retryCount > 1000) {
                throw new RuntimeException("Unable to generate unique transaction ID");
            }

        } while (transactionDao.getTransactionByUniqueId(uniqueId) != null);

        return uniqueId;
    }


    public static void doPostTransactionAfterUpdate(SharedPreferencesManager preferencesManager, String url, TransactionEntity transaction, Runnable onComplete) {
        String baseUrl = preferencesManager.get(ApiBaseUrl, "https://portal.idmc.coop:5151/").toString();
        Log.e("Base URL", baseUrl);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        ApiManager apiManager = new ApiManager(apiService);

        String request = new Gson().toJson(transaction);
        Log.e(TAG, "doPostTransaction: " + request);

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), request);

        HashMap<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/json");

        DisposableObserver<ResponseBody> disposableObserver = new DisposableObserver<ResponseBody>() {
            @Override
            public void onNext(ResponseBody response) {
                try {
                    JsonElement jsonElement = JsonParser.parseReader(response.charStream());
                    String json = new Gson().toJson(jsonElement);
                    Log.e(TAG, "onNext: " + json);

                    onComplete.run();

                } catch (Exception e) {
                    Log.e(TAG, "Response parsing error", e);
                }
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError: ", e);
                e.printStackTrace();

                onComplete.run();
            }

            @Override
            public void onComplete() {
                // Completion logic if needed

                onComplete.run();
            }
        };

        apiManager.makePostRequestCall(url, requestBody, header, disposableObserver);
    }


    public static void doPostTransaction(SharedPreferencesManager preferencesManager, String url, TransactionEntity transaction, TransactionDao transactionDao) {
        String baseUrl = preferencesManager.get(ApiBaseUrl, "https://portal.idmc.coop:5151/").toString();
        Log.e("Base URL", baseUrl);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        ApiManager apiManager = new ApiManager(apiService);

        /// Here one transaction add into array
        List<TransactionEntity> transactionList = new ArrayList<>();
        transactionList.add(transaction);

        // Create JSON Array
        // And add Key And Value in the request
        JsonArray jsonArray = new JsonArray();

        for (TransactionEntity transactionEntity : transactionList) {
            JsonObject jsonObject = new Gson().toJsonTree(transactionEntity).getAsJsonObject();
            jsonObject.addProperty("Key", KeyForApi);
            jsonObject.addProperty("value", ValueForApi);
            jsonArray.add(jsonObject);
        }

        String request = new Gson().toJson(jsonArray);
        Log.e(TAG, "doPostTransaction: " + request);

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), request);

        HashMap<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/json");

        DisposableObserver<ResponseBody> disposableObserver = new DisposableObserver<ResponseBody>() {
            @Override
            public void onNext(ResponseBody response) {
                try {
                    JsonElement jsonElement = JsonParser.parseReader(response.charStream());
                    String json = new Gson().toJson(jsonElement);
                    Log.e(TAG, "onNext: " + json);


                    if (transactionDao != null) {
                        new Thread(() -> {
                            transactionDao.updateTransactionUploadToServerStatus(String.valueOf(transaction.getId()), 1);
                        }).start();
                    }


                    if (preferencesManager != null) {
                        /// Delete the saved transactions
                        preferencesManager.delete(Constants.PaymentReceived);
                        preferencesManager.delete(Constants.PaidAmt);
                        preferencesManager.delete(Constants.SavedTransaction);
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Response parsing error", e);
                }
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError: ", e);
                e.printStackTrace();
            }

            @Override
            public void onComplete() {
                // Completion logic if needed
            }
        };

        apiManager.makePostRequestCall(url, requestBody, header, disposableObserver);
    }

    public static void doPostAsyncTransactions(SharedPreferencesManager preferencesManager, String url, ArrayList<TransactionEntity> transactionList, TransactionDao transactionDao) {
        String baseUrl = preferencesManager.get(ApiBaseUrl, "https://portal.idmc.coop:5151/").toString();
        Log.e("Base URL", baseUrl);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        ApiManager apiManager = new ApiManager(apiService);


        // Create JSON Array
        // And add Key And Value in the request
        JsonArray jsonArray = new JsonArray();

        for (TransactionEntity transactionEntity : transactionList) {
            JsonObject jsonObject = new Gson().toJsonTree(transactionEntity).getAsJsonObject();
            jsonObject.addProperty("Key", KeyForApi);
            jsonObject.addProperty("value", ValueForApi);
            jsonArray.add(jsonObject);
        }

        String request = new Gson().toJson(jsonArray);
        Log.e(TAG, "doPostTransactionList: " + request);

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), request);

        HashMap<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/json");

        DisposableObserver<ResponseBody> disposableObserver = new DisposableObserver<ResponseBody>() {
            @Override
            public void onNext(ResponseBody response) {
                try {
                    JsonElement jsonElement = JsonParser.parseReader(response.charStream());
                    String json = new Gson().toJson(jsonElement);
                    Log.e(TAG, "onNext list: " + json);

                    ArrayList idList = new ArrayList();
                    for (int i = 0; i < transactionList.size(); i++) {
                        idList.add(String.valueOf(transactionList.get(i).getId()));
                    }


                    if (transactionDao != null && !idList.isEmpty()) {
                        new Thread(() -> {
                            transactionDao.updateTransactionUploadToServerStatusForIds(1, idList);
                        }).start();
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Response parsing error", e);
                }
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError: ", e);
                e.printStackTrace();
            }

            @Override
            public void onComplete() {
                // Completion logic if needed
            }
        };

        apiManager.makePostRequestCall(url, requestBody, header, disposableObserver);
    }


    public static void doPostLog(SharedPreferencesManager preferencesManager, String url, LogEntity log, LogDao logDao) {
        String baseUrl = preferencesManager.get(ApiBaseUrl, "https://portal.idmc.coop:5151/").toString();
        Log.e("Base URL", baseUrl);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        ApiManager apiManager = new ApiManager(apiService);

        /// Here one transaction add into array
        List<LogEntity> logList = new ArrayList<>();
        logList.add(log);


        // Create JSON Array
        // And add Key And Value in the request
        JsonArray jsonArray = new JsonArray();

        for (LogEntity logEntity : logList) {
            JsonObject jsonObject = new Gson().toJsonTree(logEntity).getAsJsonObject();
            jsonObject.addProperty("Key", KeyForApi);
            jsonObject.addProperty("value", ValueForApi);
            jsonArray.add(jsonObject);
        }

        String request = new Gson().toJson(jsonArray);

        Log.e(TAG, "doPostLog: " + request);

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), request);

        HashMap<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/json");

        DisposableObserver<ResponseBody> disposableObserver = new DisposableObserver<ResponseBody>() {
            @Override
            public void onNext(ResponseBody response) {
                try {
                    JsonElement jsonElement = JsonParser.parseReader(response.charStream());
                    String json = new Gson().toJson(jsonElement);
                    Log.e(TAG, "onNext: " + json);

                    if (log != null && logDao != null) {
                        new Thread(() -> {
                            logDao.updateLogUploadToServerStatus(String.valueOf(log.getId()), 1);
                        }).start();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Response parsing error", e);
                }
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError: ", e);
                e.printStackTrace();
            }

            @Override
            public void onComplete() {
                // Completion logic if needed
            }
        };

        apiManager.makePostRequestCall(url, requestBody, header, disposableObserver);
    }


    public static void doPostAsyncLogs(SharedPreferencesManager preferencesManager, String url, ArrayList<LogEntity> logList, LogDao logDao) {
        String baseUrl = preferencesManager.get(ApiBaseUrl, "https://portal.idmc.coop:5151/").toString();
        Log.e("Base URL", baseUrl);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        ApiManager apiManager = new ApiManager(apiService);


        // Create JSON Array
        // And add Key And Value in the request
        JsonArray jsonArray = new JsonArray();

        for (LogEntity logEntity : logList) {
            JsonObject jsonObject = new Gson().toJsonTree(logEntity).getAsJsonObject();
            jsonObject.addProperty("Key", KeyForApi);
            jsonObject.addProperty("value", ValueForApi);
            jsonArray.add(jsonObject);
        }

        String request = new Gson().toJson(jsonArray);
        Log.e(TAG, "doPostLogList: " + request);

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), request);

        HashMap<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/json");

        DisposableObserver<ResponseBody> disposableObserver = new DisposableObserver<ResponseBody>() {
            @Override
            public void onNext(ResponseBody response) {
                try {
                    JsonElement jsonElement = JsonParser.parseReader(response.charStream());
                    String json = new Gson().toJson(jsonElement);
                    Log.e(TAG, "onNext: " + json);

                    ArrayList idList = new ArrayList();
                    for (int i = 0; i < logList.size(); i++) {
                        idList.add(String.valueOf(logList.get(i).getId()));
                    }


                    // Update database in a background thread to avoid main thread access
                    if (logDao != null && !idList.isEmpty()) {
                        new Thread(() -> {
                            logDao.updateLogsUploadToServerStatusForIds(1, idList);
                        }).start();
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Response parsing error", e);
                }
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError: ", e);
                e.printStackTrace();
            }

            @Override
            public void onComplete() {
                // Completion logic if needed
            }
        };

        apiManager.makePostRequestCall(url, requestBody, header, disposableObserver);
    }




    /*Here in this getting the configuration data (SMS, Razorpay). We have to save it into shared preference*/
//    public static void doGetConfigurationData(Activity activity) {
//
//        Retrofit retrofit = new Retrofit.Builder().baseUrl(preferencesManager.get(ApiBaseUrl, "https://portal.idmc.coop:5151/api/").toString()) // Replace with your base URL
//                .addConverterFactory(GsonConverterFactory.create()).addCallAdapterFactory(RxJava3CallAdapterFactory.create()) // Add this line
//                .addConverterFactory(GsonConverterFactory.create()).build();
//
//        String baseUrl = preferencesManager.get(ApiBaseUrl, "https://portal.idmc.coop:5151/api/").toString();
//
//        Log.d(TAG + "Base URL  run: ==>", baseUrl);
//        Log.d(TAG + "Base URL  run: ==> GetConfigurationUrl",baseUrl +  GetConfigurationUrl);
//
//        ApiService apiService = retrofit.create(ApiService.class);
//
//        ApiManager apiManager = new ApiManager(apiService);
//
//        Handler handler = new Handler(Looper.getMainLooper());
//        handler.post(() -> {
//            ProgressDialog pd = new ProgressDialog(activity);
//            pd.setTitle("Please Wait...");
//            pd.setCancelable(false);
//            pd.show();
//            DisposableObserver<ResponseBody> disposableObserver = new DisposableObserver<ResponseBody>() {
//                @Override
//                public void onNext(ResponseBody response) {
//                    pd.dismiss();
//                    if (!response.toString().isEmpty()) {
//                        String json = new Gson().toJson(new Gson().fromJson(response.charStream(), JsonElement.class));
//                        Log.e(TAG, "Configuration Data: " + json);
//
//                        preferencesManager = SharedPreferencesManager.getInstance(activity);
//                        ConfigurationResponse configurationResponse = new Gson().fromJson(json, ConfigurationResponse.class);
//
//
//                        /// Save in shared preference
//                        Log.e(TAG, "RazorPayKey: " + configurationResponse.getData().get(0).getRazorPayKey());
//                        preferencesManager.save(SMSApiUrl, configurationResponse.getData().get(0).getSmsAPIURL() + "/");
//                        preferencesManager.save(SMSSid, configurationResponse.getData().get(0).getSmsSid());
//                        preferencesManager.save(SMSApiKey, configurationResponse.getData().get(0).getSmsAPIKey());
//                        preferencesManager.save(SMSSender, configurationResponse.getData().get(0).getSmsSender());
//                        preferencesManager.save(SMSTemplateId, configurationResponse.getData().get(0).getSmsTemplateID());
//                        preferencesManager.save(SMSTemplateContent, configurationResponse.getData().get(0).getSmsTemplateContent());
//
//
//                        preferencesManager.save(RazorPayKey, "rzp_live_oTrQqk0HauuUWZ");
//
//                      //  preferencesManager.save(RazorPayKey, configurationResponse.getData().get(0).getRazorPayKey());
//
//                        preferencesManager.save(RazorPaySecretKey, "7lBcCfNsgl7wKtshFz7QCm8F");
//
//
//                        //     preferencesManager.save(RazorPaySecretKey, configurationResponse.getData().get(0).getRazorPaySecretKey());
//                    }
//                }
//
//                @Override
//                public void onError(Throwable e) {
//                    // Handle the error
//                    if (pd != null && pd.isShowing()) {
//                        pd.dismiss();
//                    }
//                    e.printStackTrace();
//                    activity.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Utils.handleApiError(activity, e, apiManager);
//                        }
//                    });
//                }
//
//                @Override
//                public void onComplete() {
//                    // Handle completion if needed
//                }
//            };
//
//            String url = GetConfigurationUrl + "?Key=" + KeyForApi + "&value=" + ValueForApi;
//            apiManager.makeGetResponseCall(url, disposableObserver);
//
//          //  apiManager.makeGetResponseCall(GetConfigurationUrl, disposableObserver);
//        });
//
//
//    }


    public static void doGetConfigurationData(Activity activity) {

        Retrofit retrofit = new Retrofit.Builder().baseUrl(preferencesManager.get(ApiBaseUrl, "https://portal.idmc.coop:5151/api/").toString()) // Replace with your base URL
                .addConverterFactory(GsonConverterFactory.create()).addCallAdapterFactory(RxJava3CallAdapterFactory.create()) // Add this line
                .addConverterFactory(GsonConverterFactory.create()).build();

        String baseUrl = preferencesManager.get(ApiBaseUrl, "https://portal.idmc.coop:5151/api/").toString();

        Log.d(TAG + "Base URL  run: ==>", baseUrl);
        Log.d(TAG + "Base URL  run: ==> GetConfigurationUrl", baseUrl + GetConfigurationUrl);

        ApiService apiService = retrofit.create(ApiService.class);

        ApiManager apiManager = new ApiManager(apiService);

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            ProgressDialog pd = new ProgressDialog(activity);
            pd.setTitle("Please Wait...");
            pd.setCancelable(false);
            pd.show();
            DisposableObserver<ResponseBody> disposableObserver = new DisposableObserver<ResponseBody>() {
                @Override
                public void onNext(ResponseBody response) {
                    pd.dismiss();
                    if (!response.toString().isEmpty()) {
                        String json = new Gson().toJson(new Gson().fromJson(response.charStream(), JsonElement.class));
                        Log.e(TAG, "Configuration Data: " + json);

                        preferencesManager = SharedPreferencesManager.getInstance(activity);
                        ConfigurationResponse configurationResponse = new Gson().fromJson(json, ConfigurationResponse.class);


                        /// Save in shared preference
                        Log.e(TAG, "RazorPayKey: " + configurationResponse.getData().get(0).getRazorPayKey());
                        preferencesManager.save(SMSApiUrl, configurationResponse.getData().get(0).getSmsAPIURL() + "/");
                        preferencesManager.save(SMSSid, configurationResponse.getData().get(0).getSmsSid());
                        preferencesManager.save(SMSApiKey, configurationResponse.getData().get(0).getSmsAPIKey());
                        preferencesManager.save(SMSSender, configurationResponse.getData().get(0).getSmsSender());
                        preferencesManager.save(SMSTemplateId, configurationResponse.getData().get(0).getSmsTemplateID());
                        preferencesManager.save(SMSTemplateContent, configurationResponse.getData().get(0).getSmsTemplateContent());


                        preferencesManager.save(RazorPayKey, "rzp_live_oTrQqk0HauuUWZ");

                        //  preferencesManager.save(RazorPayKey, configurationResponse.getData().get(0).getRazorPayKey());

                        preferencesManager.save(RazorPaySecretKey, "7lBcCfNsgl7wKtshFz7QCm8F");


                        //     preferencesManager.save(RazorPaySecretKey, configurationResponse.getData().get(0).getRazorPaySecretKey());
                    }
                }

                @Override
                public void onError(Throwable e) {
                    // Handle the error
                    if (pd != null && pd.isShowing()) {
                        pd.dismiss();
                    }
                    e.printStackTrace();
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Utils.handleApiError(activity, e, apiManager);
                        }
                    });
                }

                @Override
                public void onComplete() {
                    // Handle completion if needed
                }
            };

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("Key", "Admin");
            jsonObject.addProperty("value", "Mvb@102405A19022025");

            RequestBody requestBody = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    jsonObject.toString()
            );

            apiManager.makeGetResponseCallWithBody(
                    GetConfigurationUrl,
                    requestBody,
                    disposableObserver
            );

            //  apiManager.makeGetResponseCall(GetConfigurationUrl, disposableObserver);
        });


    }


    public static void doPostConfigurationData(Activity activity, String url) {
        String baseUrl = preferencesManager.get(ApiBaseUrl, "https://portal.idmc.coop:5151/").toString();
        Log.e("Base URL", baseUrl + url);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        ApiManager apiManager = new ApiManager(apiService);

        // Create JSON Array and add Key And Value in the request
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("Key", KeyForApi);
        jsonObject.addProperty("value", ValueForApi);

        String request = new Gson().toJson(jsonObject);
        Log.e(TAG, "doPostLog: " + request);

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), request);

        HashMap<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/json");

        // Create a ProgressDialog to show progress while API call is in progress
        ProgressDialog progressDialog = new ProgressDialog(activity);
        progressDialog.setTitle("Loading...");
        progressDialog.setMessage("Please wait while we fetch the configuration...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        DisposableObserver<ResponseBody> disposableObserver = new DisposableObserver<ResponseBody>() {
            @Override
            public void onNext(ResponseBody response) {
                try {
                    // Dismiss the progress dialog once the response is received
                    progressDialog.dismiss();

                    if (response != null && !response.toString().isEmpty()) {
                        String json = new Gson().toJson(new Gson().fromJson(response.charStream(), JsonElement.class));
                        Log.e(TAG, "Configuration Data: " + json);

                        preferencesManager = SharedPreferencesManager.getInstance(activity);
                        ConfigurationResponse configurationResponse = new Gson().fromJson(json, ConfigurationResponse.class);

                        // Save configuration data in SharedPreferences
                        Log.e(TAG, "RazorPayKey: " + configurationResponse.getData().get(0).getRazorPayKey());
                        preferencesManager.save(SMSApiUrl, configurationResponse.getData().get(0).getSmsAPIURL() + "/");
                        preferencesManager.save(SMSSid, configurationResponse.getData().get(0).getSmsSid());
                        preferencesManager.save(SMSApiKey, configurationResponse.getData().get(0).getSmsAPIKey());
                        preferencesManager.save(SMSSender, configurationResponse.getData().get(0).getSmsSender());
                        preferencesManager.save(SMSTemplateId, configurationResponse.getData().get(0).getSmsTemplateID());
                        preferencesManager.save(SMSTemplateContent, configurationResponse.getData().get(0).getSmsTemplateContent());

//                        preferencesManager.save(RazorPayKey, "rzp_live_oTrQqk0HauuUWZ");
//                        preferencesManager.save(RazorPaySecretKey, "7lBcCfNsgl7wKtshFz7QCm8F");

                        //  preferencesManager.save(RazorPayKey, "rzp_live_oTrQqk0HauuUWZ");
                        preferencesManager.save(RazorPayKey, configurationResponse.getData().get(0).getRazorPayKey());
                        //  preferencesManager.save(RazorPaySecretKey, "7lBcCfNsgl7wKtshFz7QCm8F");
                        preferencesManager.save(RazorPaySecretKey, configurationResponse.getData().get(0).getRazorPaySecretKey());


                    }
                } catch (Exception e) {
                    Log.e(TAG, "Response parsing error", e);
                }
            }

            @Override
            public void onError(Throwable e) {
                // Dismiss progress dialog if there is an error
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                Log.e(TAG, "onError: ", e);
                e.printStackTrace();
            }

            @Override
            public void onComplete() {
                // Completion logic if needed
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }
        };

        // Make the API request with the provided URL, request body, and header
        apiManager.makePostRequestCall(url, requestBody, header, disposableObserver);
    }


    /*Check that internet connection is available or not*/
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    public static String exportUsersToCSV(Context context, AppDatabase db) {
        List<TransactionEntity> transactions = db.transactionDao().getAllTransactions();
        File csvFile = null;

        try {
            StringBuilder data = new StringBuilder();
            data.append("ID,UserName,Password,Transaction Type," +
                    "Bank Transaction No,Transaction Date,Transaction Time," +
                    "Amount,Volume,Milk Price,Milk Temperature,Machine Id," +
                    "Transaction Status,UPI Id,Unique Transaction Id,CreatedBy \n");


            for (TransactionEntity transaction : transactions) {
                data.append((transaction.getId())).append(",");
                data.append(transaction.getUserName()).append(",");
                data.append(transaction.getPassword()).append(",");
                data.append(transaction.getTransactionType()).append(",");
                data.append(transaction.getBankTransactionNo()).append(",");
                data.append(transaction.getTransactionDate()).append(",");
                data.append(transaction.getTransactionTime()).append(",");
                data.append(transaction.getAmount()).append(",");
                data.append(transaction.getVolume()).append(",");
                data.append(transaction.getMilkPrice()).append(",");
                data.append(transaction.getMilkTemperature()).append(",");
                data.append(transaction.getMachineId()).append(",");
                data.append(transaction.getTransactionStatus()).append(",");
                data.append(transaction.getUpiId()).append(",");
                data.append(transaction.getUniqueTransactionId()).append(",");
                data.append(transaction.getCreatedBy()).append("\n");
            }

            File dir = new File(context.getExternalFilesDir(null), "exportedCSV");
            if (!dir.exists()) dir.mkdirs();

            csvFile = new File(dir, "Transactions.csv");
            FileWriter writer = new FileWriter(csvFile);
            writer.write(data.toString());
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return csvFile != null ? csvFile.getAbsolutePath() : null;
    }


    public static void sendEmailWithAttachment(Context context, String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"jaideep1210@gmail.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Room Database CSV Export");
        intent.putExtra(Intent.EXTRA_TEXT, "Attached is the exported CSV.");

        Uri uri = FileProvider.getUriForFile(
                context,
                "com.imdc.milkdespencer.provider",
                file
        );
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        context.startActivity(Intent.createChooser(intent, "Send Email..."));
    }


    public static void exportTransactionsToCSVAndShare(Context context, List<TransactionEntity> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            Toast.makeText(context, "No transactions to export", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder csvBuilder = new StringBuilder();

        csvBuilder.append("ID,UserName,Password,Transaction Type," +
                "Bank Transaction No,Transaction Date,Transaction Time," +
                "Amount,Volume,Milk Price,Milk Temperature,Machine Id," +
                "Transaction Status,UPI Id,Unique Transaction Id,CreatedBy \n");

        for (TransactionEntity transaction : transactions) {
            csvBuilder.append((transaction.getId())).append(",");
            csvBuilder.append(transaction.getUserName()).append(",");
            csvBuilder.append(transaction.getPassword()).append(",");
            csvBuilder.append(transaction.getTransactionType()).append(",");
            csvBuilder.append(transaction.getBankTransactionNo()).append(",");
            csvBuilder.append(transaction.getTransactionDate()).append(",");
            csvBuilder.append(transaction.getTransactionTime()).append(",");
            csvBuilder.append(transaction.getAmount()).append(",");
            csvBuilder.append(transaction.getVolume()).append(",");
            csvBuilder.append(transaction.getMilkPrice()).append(",");
            csvBuilder.append(transaction.getMilkTemperature()).append(",");
            csvBuilder.append(transaction.getMachineId()).append(",");
            csvBuilder.append(transaction.getTransactionStatus()).append(",");
            csvBuilder.append(transaction.getUpiId()).append(",");
            csvBuilder.append(transaction.getUniqueTransactionId()).append(",");
            csvBuilder.append(transaction.getCreatedBy()).append("\n");
        }


        String fileName = "Transactions.csv";
        String mimeType = "text/csv";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MilkDispenser");

        ContentResolver resolver = context.getContentResolver();
        Uri uri = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        }

        if (uri != null) {
            try (OutputStream os = resolver.openOutputStream(uri)) {
                os.write(csvBuilder.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();

                // Share
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType(mimeType);
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(Intent.createChooser(intent, "Share CSV via"));
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(context, "Error writing file", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "Error creating file URI", Toast.LENGTH_SHORT).show();
        }
    }


    private String convertTimestamp(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
        return sdf.format(new Date(millis));
    }


}
