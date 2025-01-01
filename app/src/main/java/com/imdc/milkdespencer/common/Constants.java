package com.imdc.milkdespencer.common;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.imdc.milkdespencer.R;
import com.imdc.milkdespencer.adminUi.AdminActivity;
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

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Random;
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

    public static final String TAG = "MilkDespencer";
    public static final String MachineId = "MachineId";
    public static final String RazorPayCustomerID = "RazorPayCustomerID";
    public static final String OwnerName = "OwnerName";
    //    public static final String MachineId = "MachineId";
    public static final String MilkBasePrice = "MilkBasePrice";
    public static final String LoginUser = "LoginUser";
    public static final String MilkDensityPref = "MilkDensity";
    public static final String TemperatureOffSet = "TemperatureOffSet";
    public static final String TemperatureSet = "TemperatureSet";
    public static final String CurrentTemperature = "CurrentTemperature";
    public static final String ResponseTempStatus = "ResponseTempStatus";
    public static final String ResponseMilkDispense = "ResponseMilkDispense";

    public static final String PaymentReceived = "PaymentReceived";
    public static final String PaymentCashReceived = "PaymentCashReceived";
    public static final String PaidAmt = "PaidAmt";

    public static final String BASE_URL = "https://portal.idmc.coop:5151/api/";
    public static final String PostTransactionURL = "/Transaction/PostTransaction";
    public static final String PostMerchantURL = "/Merchant/PostMerchant";
    public static final DecimalFormat df = new DecimalFormat("0.00");
    private static final String OTP = "SentOTP";
    // digit or special character
    private static final String PASSWORD_PATTERN = "^(?!.*\\s)(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9!@#$%]).{8,20}$";
    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);
    static SharedPreferencesManager preferencesManager;

    public static void showAlertDialog(Context context, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title).setMessage(message).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Handle positive button click if needed
                dialog.dismiss();
            }
        }).show();
    }

    public static void showAcceptDialog(Context context, String title, String message, DialogInterface.OnClickListener yesClickListener, DialogInterface.OnClickListener noClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title).setMessage(message).setPositiveButton("Yes", yesClickListener).setNegativeButton("No", noClickListener).show();
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
        TextInputLayout tilMilkDensity = view.findViewById(R.id.tilMilkDensity);
        TextInputEditText tieMilkDensity = view.findViewById(R.id.tieMilkDensity);


        tieMilkBasePrice.setText(preferencesManager.get(MilkBasePrice, "0.0").toString());
        tieMilkDensity.setText(preferencesManager.get(MilkDensityPref, "0.0").toString());


        // Set click listener for OK button
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle OK button click
                String milkBasePrice = tieMilkBasePrice.getText().toString();
                String milkDensity = tieMilkDensity.getText().toString();
                preferencesManager.save(MilkBasePrice, milkBasePrice);
                preferencesManager.save(MilkDensityPref, milkDensity);

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

    public static void showAdminConfigDialog(Context context) {
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

        TextInputEditText tieTemperatureOffset = view.findViewById(R.id.tieTemperatureOffset);
        TextInputEditText tieTemperatureSet = view.findViewById(R.id.tieSetTemperature);
        TextInputEditText tieOwnerNameId = view.findViewById(R.id.tieOwnerNameId);


        TextInputEditText tieMachineId = view.findViewById(R.id.tieMachineId);

        tilMachineId.setErrorEnabled(true);
        tilTemperatureOffset.setErrorEnabled(true);
        tilTemperatureSet.setErrorEnabled(true);

        String machineId = preferencesManager.get(MachineId, "MachineId").toString();

        tilMachineId.setEnabled(machineId.isEmpty() || machineId.equalsIgnoreCase("MachineId"));
        tieMachineId.setText(machineId);
        tieOwnerNameId.setText(preferencesManager.get(OwnerName, 0.0).toString());
        tieTemperatureOffset.setText(preferencesManager.get(TemperatureOffSet, 0.0).toString());
        tieTemperatureSet.setText(preferencesManager.get(TemperatureSet, "0.0").toString());

        // Set click listener for OK button
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle OK button click
                String machineID = tieMachineId.getText().toString();
                String ownerName = tieOwnerNameId.getText().toString();
                String setTemperature = tieTemperatureSet.getText().toString();
                String offSetTemperature = tieTemperatureOffset.getText().toString();

                preferencesManager.save(MachineId, machineID);
                preferencesManager.save(OwnerName, ownerName);
                preferencesManager.save(TemperatureSet, setTemperature);
                preferencesManager.save(TemperatureOffSet, offSetTemperature);

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

    public static void showLoginDialog(Context context, AppDatabase appDatabase) {
        // Create a layout inflater to inflate the custom dialog layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.login_dialog, null);

        preferencesManager = SharedPreferencesManager.getInstance(context);
        // Create the AlertDialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(view);

        // Create the AlertDialog
        AlertDialog dialog = builder.create();

        // Find views in the custom layout
        MaterialButton loginButton = view.findViewById(R.id.loginButton);
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
                        User login = appDatabase.userDao().login(username, password);
                        if (login != null) {
//                            Log.e(TAG, "onClick: " + new Gson().toJson(login));
                            preferencesManager.save(Constants.LoginUser, new Gson().toJson(login));
                            Intent intent = new Intent(context.getApplicationContext(), AdminActivity.class);
                            intent.putExtra(Constants.LoginUser, new Gson().toJson(login));
                            context.startActivity(intent);
                            dialog.dismiss();
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
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

        final boolean[] isOtpSend = {false};
        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://api.kaleyra.io/v1/") // Replace with your base URL
                .addConverterFactory(GsonConverterFactory.create()).addCallAdapterFactory(RxJava3CallAdapterFactory.create()) // Add this line
                .addConverterFactory(GsonConverterFactory.create()).build();

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
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Integer mobileNoExists = appDatabase.userDao().mobileNoExists(phoneNo);
                            if (mobileNoExists <= 0) {
                                Handler handler = new Handler(Looper.getMainLooper());
                                handler.post(() -> Toast.makeText(context, "Please Enter Valid Mobile No!!", Toast.LENGTH_SHORT).show());
                            } else {
                                ProgressDialog pd = new ProgressDialog(context);
                                pd.setTitle("Please Wait...");
                                pd.setCancelable(false);
                                pd.show();

                                String otp = "OTP for MVM password reset is " + generateOtp(6) + ". -IDMC";
                                String content = "to=" + phoneNo + "&type=OTP&sender=IDMCCS&body=" + otp;
                                Log.e(TAG, "onClick: " + content);

                                HashMap<String, String> fields = new HashMap<>();
                                fields.put("to", "+91" + phoneNo);
                                fields.put("type", "OTP");
                                fields.put("sender", "IDMCCS");
                                fields.put("body", otp);
                                fields.put("api-key", "A5b9c8ba406fbc9bf361ffeb8bf6cb120");

                                HashMap<String, String> headers = new HashMap<>();
                                headers.put("Content-Type", "application/x-www-form-urlencoded");
                                headers.put("api-key", "A5b9c8ba406fbc9bf361ffeb8bf6cb120");
                                DisposableObserver<ResponseBody> disposableObserver = new DisposableObserver<ResponseBody>() {
                                    @Override
                                    public void onNext(ResponseBody response) {
                                        pd.dismiss();
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
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        // Handle the error
                                        pd.dismiss();
                                        Utils.handleApiError(context, e, apiManager);
                                    }

                                    @Override
                                    public void onComplete() {
                                        // Handle completion if needed
                                    }
                                };
                                apiManager.makeOTPRequestCall("HXIN1764058706IN/messages/", fields, headers, disposableObserver);
                            }
                            Log.e(TAG, "onClick:mobileNoExists " + mobileNoExists);
                        }
                    }).start();


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
                    // password is valid, proceed with form submission
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

    public static double calculateMilkAmount(double cost, Context context) {
        preferencesManager = SharedPreferencesManager.getInstance(context);

        float milkBasePrice = Float.parseFloat(preferencesManager.get(Constants.MilkBasePrice, "0.0").toString());
        float DENSITY_OF_MILK = Float.parseFloat(preferencesManager.get(Constants.MilkDensityPref, "0.0").toString());

        Log.e(TAG, "calculateMilkAmount: BasePrice " + milkBasePrice + " <+++> " + (cost / milkBasePrice));
        return (cost / milkBasePrice) * DENSITY_OF_MILK;
    }


    public static void saveLogs(Context context, String message) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                AppDatabase database = AppDatabase.getInstance(context);
                LogDao logDao = database.logDao();
                LogEntity logEntity = new LogEntity(message);
                logDao.insert(logEntity);

                Log.e(TAG, "run: saveLogs " + logDao.getAllLogs());

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

    public static long insertTransaction(Activity activity, TransactionDao transactionDao, String transactionType, String bankTransactionNo, String transactionDate, String transactionTime, String amount, String transactionStatus, String upiId) {
        TransactionEntity transaction = new TransactionEntity();
        transaction.setUserName("Admin");
        transaction.setPassword("QWRtaW4=");
        transaction.setTransactionType(transactionType);
        transaction.setBankTransactionNo(bankTransactionNo);
        transaction.setTransactionDate(transactionDate);
        transaction.setTransactionTime(transactionTime);
        transaction.setAmount(amount);
        transaction.setTransactionStatus(transactionStatus);
        transaction.setUpiId(upiId);
        transaction.setUniqueTransactionId(transactionDao.generateUniqueTransactionId());

        long transactionId = transactionDao.insert(transaction);

        doPostTransaction("/api/Transaction/PostTransaction", transaction, activity);

        return transactionId;
    }

    public static void doPostTransaction(String url, TransactionEntity transaction, Activity activity) {


        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://portal.idmc.coop:5151/") // Replace with your base URL
                .addConverterFactory(GsonConverterFactory.create()).addCallAdapterFactory(RxJava3CallAdapterFactory.create()) // Add this line
                .addConverterFactory(GsonConverterFactory.create()).build();

        ApiService apiService = retrofit.create(ApiService.class);

        ApiManager apiManager = new ApiManager(apiService);

        HashMap<String, String> header = new HashMap<>();
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        String request = new Gson().toJson(transaction);
        RequestBody requestBody = RequestBody.create(mediaType, request);

        Log.e(TAG, "doPostTransaction: " + new Gson().toJson(requestBody));
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
                        Log.e(TAG, "onNext: " + json);
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
            apiManager.makePostRequestCall(url, requestBody, header, disposableObserver);
        });


    }

}
