package com.imdc.milkdespencer.adminUi;

import static com.imdc.milkdespencer.common.Constants.KEY_CALIBRATION;
import static com.imdc.milkdespencer.common.Constants.KEY_ELECTRICITY;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.imdc.milkdespencer.MainActivity;
import com.imdc.milkdespencer.R;
import com.imdc.milkdespencer.common.Constants;
import com.imdc.milkdespencer.common.UsbSerialCommunication;
import com.imdc.milkdespencer.models.SendToDevice;

public class CalibrationActivity extends AppCompatActivity {

    private static UsbSerialCommunication usbSerialCommunication;
    private final String TAG = getClass().getSimpleName();
    StringBuilder strMessage = new StringBuilder();
    SendToDevice sendToDevice = new SendToDevice();
    boolean isSent = false;
    double knownWeight = 0.0;
    double maxWeight = 0.0;
    private TextInputLayout tilKnownWeight, tilMaxWeight;
    private TextInputEditText tieKnownWeight, tieMaxWeight;

    private TextView tvInstruction;

    private ImageButton btnMinusMinWeight, btnMinusMaxWeight, btnPlusMinWeight,btnPlusMaxWeight;

    private MaterialButton startCalibrationBtn;
   private TextView tvLblMessage;

    private int minWeightInGram = 500;

    private int maxWeightInGram = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);
        usbSerialCommunication = new UsbSerialCommunication(getApplicationContext());
        if (!usbSerialCommunication.connected) {
            usbSerialCommunication.connect();
            usbSerialCommunication.setBaudRate(115200);
            startCalibration(0, 0, false);
        }

        tilKnownWeight = findViewById(R.id.tilKnownWeight);
        tilMaxWeight = findViewById(R.id.tilMaxWeight);
        tieKnownWeight = findViewById(R.id.tieKnownWeight);

        tvInstruction = findViewById(R.id.tvInstruction);

        btnMinusMinWeight= findViewById(R.id.btnMinusMinWeight);
        btnPlusMinWeight= findViewById(R.id.btnPlusMinWeight);

        btnMinusMaxWeight= findViewById(R.id.btnMinusMaxWeight);
        btnPlusMaxWeight= findViewById(R.id.btnPlusMaxWeight);

        tieMaxWeight = findViewById(R.id.tieMaxWeight);
        startCalibrationBtn = findViewById(R.id.startCalibrationBtn);
//        tvMessage = findViewById(R.id.tv_Message);
        tvLblMessage = findViewById(R.id.tv_lbl_Message);

        tilKnownWeight.setEnabled(true);
        tilMaxWeight.setEnabled(true);
        tieKnownWeight = findViewById(R.id.tieKnownWeight);
        tieMaxWeight = findViewById(R.id.tieMaxWeight);

        tieMaxWeight.setText(String.valueOf(maxWeightInGram));
        tieKnownWeight.setText(String.valueOf(minWeightInGram));

        String instruction = "Please keep <b>" + String.valueOf(minWeightInGram) + " grams</b> and <b>" + String.valueOf(maxWeightInGram/1000) + " kg</b> weighment ready with you,\nboth weights are essential for calibration.";

        tvInstruction.setText(Html.fromHtml(instruction));
        tvLblMessage.setText("Press Calibration button and follow the instructions.");
        clickListener();

        // Set up input validation for the known weight field
        tilKnownWeight.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String knownWeight = tieKnownWeight.getText().toString();
                if (TextUtils.isEmpty(knownWeight)) {
                    tilKnownWeight.setError("Please enter a known weight");
                } else {
                    tilKnownWeight.setError(null);
                }
                if (!isNumeric(knownWeight)) {
                    tilKnownWeight.setError("Please enter a numeric value");
                } else {
                    tilKnownWeight.setError(null);
                }
            }
        });

        // Set up input validation for the max weight field
        tilMaxWeight.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String maxWeight = tieMaxWeight.getText().toString();
                if (TextUtils.isEmpty(maxWeight)) {
                    tilMaxWeight.setError("Please enter a max weight");
                } else tilMaxWeight.setError(null);


                if (!isNumeric(maxWeight)) {
                    tilMaxWeight.setError("Please enter a numeric value");
                } else tilMaxWeight.setError(null);

            }
        });

        startCalibrationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startCalibrationBtn.setVisibility(View.GONE);

                String knownWeightStr = tieKnownWeight.getText().toString();
                String maxWeightStr = tieMaxWeight.getText().toString();

                // Validate input
                if (TextUtils.isEmpty(knownWeightStr) || TextUtils.isEmpty(maxWeightStr)) {
                    if (TextUtils.isEmpty(knownWeightStr))
                        tilKnownWeight.setError("Please fill in all fields");
                    else tilKnownWeight.setError(null);

                    if (TextUtils.isEmpty(maxWeightStr))
                        tilMaxWeight.setError("Please fill in all fields");
                    else tilMaxWeight.setError(null);


                } else if (!isNumeric(knownWeightStr) || !isNumeric(maxWeightStr)) {
                    if (!isNumeric(knownWeightStr))
                        tilKnownWeight.setError("Please enter numeric values for known weight and max weight");
                    else tilKnownWeight.setError(null);

                    if (!isNumeric(maxWeightStr))
                        tilMaxWeight.setError("Please enter numeric values for known weight and max weight");
                    else tilMaxWeight.setError(null);

                } else {


                    tilKnownWeight.setHint(getString(R.string.knownWeightInKg));
                    tilMaxWeight.setHint(getString(R.string.maxKnownWeightInKg));

                    if (Double.parseDouble(knownWeightStr) <= 0) {
                        tilKnownWeight.setError("Value cannot be less then zero");
                        return;
                    }
                    if (Double.parseDouble(maxWeightStr) <= 0) {
                        tilMaxWeight.setError("Value cannot be less then zero");
                        return;
                    }

                    if (!isSent) {
                        // Start calibration process
                        knownWeight = Double.parseDouble(knownWeightStr) / 1000;
                        maxWeight = Double.parseDouble(maxWeightStr) / 1000;
                    }


                    // Display the converted values in a TextView
                    @SuppressLint("DefaultLocale") String knownWeightFinalVal = String.format("%.2f", knownWeight);
                    tieKnownWeight.setText(knownWeightFinalVal);
                    @SuppressLint("DefaultLocale") String maxWeightFinalVal = String.format("%.2f", maxWeight);
                    tieMaxWeight.setText(maxWeightFinalVal);
                    float lowWeight = Float.parseFloat(knownWeightFinalVal);
                    float highWeight = Float.parseFloat(maxWeightFinalVal);


                    tilKnownWeight.setEnabled(false);
                    tilMaxWeight.setEnabled(false);

                    startCalibration(lowWeight, highWeight, true);
                    isSent = true;

//                    usbSerialCommunication.sendData("1234", true);
                    usbSerialCommunication.setReadDataListener(new UsbSerialCommunication.ReadDataListener() {
                        @Override
                        public void onReadData(String data) {

                            Log.e(TAG, "onReadData: " + data);
                            String message = "";
                            String description = "";
                            switch (data) {
                                case "0":
                                    message = "Calibration Done.";
                                    startCalibration(0, 0, false);
                                    showAndProcessDoneDialog();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                            /*CalibrationActivity.this.finish();
                                            Intent intent = new Intent(CalibrationActivity.this, MainActivity.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                            startActivity(intent);*/
                                        }
                                    });

                                    break;
                                case "1":


                                    tvInstruction.setTextColor(Color.parseColor("#000000"));
                                    tvInstruction.setTypeface(null, Typeface.BOLD);

                                    tvLblMessage.setTextColor(Color.parseColor("#ff0000"));
                                    tvLblMessage.setTypeface(null, Typeface.BOLD);

                                    message = "Keep dispenser door open till the calibration process is complete.";
                                    description = "Ensure no any items on plate.";
                                    break;
                                case "2":
                                    tvInstruction.setTextColor(Color.parseColor("#0000ff"));
                                    tvInstruction.setTypeface(null, Typeface.BOLD);

                                    /// Put minimum dynamic value

                                     message = "Put " + minWeightInGram + "grams weight on plate and wait";
                                     description = "";
                                    break;
                                case "3":

                                    tvInstruction.setTextColor(Color.parseColor("#ff0000"));
                                    tvInstruction.setTypeface(null, Typeface.BOLD);

                                    tvLblMessage.setTextColor(Color.parseColor("#ff0000"));
                                    tvLblMessage.setTypeface(null, Typeface.BOLD);

                                    /// Put minimum dynamic value
                                     message = "Tare Done !";
                                     description = "Remove " + minWeightInGram + " grams weight from plate and wait.";
                                    break;
                                case "4":

                                    tvInstruction.setTextColor(Color.parseColor("#0000ff"));
                                    tvInstruction.setTypeface(null, Typeface.BOLD);
                                    /// Put maximum dynamic value
                                     message = "Put " + (maxWeightInGram/1000) + " kg weight on plate and wait.";
                                     description = "";
                                    break;
                                default:
                                    message = "Calibration Process";
                            }

                            tvInstruction.setText(message);
                            tvLblMessage.setText(description);
                            /*strMessage.append("<<<<<<<<<<<< Calibration Start >>>>>>>>>>>>\n");
                            strMessage.append("\n");
                            strMessage.append(data);*/
                        }
                    });




                }
            }
        });
    }

    private void clickListener() {

        /// For Minimum Weight
        btnMinusMinWeight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(minWeightInGram > 0){
                    minWeightInGram = minWeightInGram - 100;
                    tieKnownWeight.setText(String.valueOf(minWeightInGram));

                    String instruction = "Please keep <b>" + String.valueOf(minWeightInGram) + " grams</b> and <b>" + String.valueOf(maxWeightInGram/1000) + " kg</b> weighment ready with you,\nboth weights are essential for calibration.";

                    tvInstruction.setText(Html.fromHtml(instruction));
                    tvLblMessage.setText("Press Calibration button and follow the instructions.");
                }



            }
        });

        btnPlusMinWeight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                minWeightInGram = minWeightInGram + 100;
                tieKnownWeight.setText(String.valueOf(minWeightInGram));

                String instruction = "Please keep <b>" + String.valueOf(minWeightInGram) + " grams</b> and <b>" + String.valueOf(maxWeightInGram/1000) + " kg</b> weighment ready with you,\nboth weights are essential for calibration.";

                tvInstruction.setText(Html.fromHtml(instruction));
                tvLblMessage.setText("Press Calibration button and follow the instructions.");
            }
        });


        /// For Maximum Weight
        btnMinusMaxWeight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(maxWeightInGram > 100){
                    maxWeightInGram = maxWeightInGram - 1000;
                    tieMaxWeight.setText(String.valueOf(maxWeightInGram));

                    String instruction = "Please keep <b>" + String.valueOf(minWeightInGram) + " grams</b> and <b>" + String.valueOf(maxWeightInGram/1000) + " kg</b> weighment ready with you,\nboth weights are essential for calibration.";

                    tvInstruction.setText(Html.fromHtml(instruction));
                    tvLblMessage.setText("Press Calibration button and follow the instructions.");
                }


            }
        });

        btnPlusMaxWeight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                maxWeightInGram = maxWeightInGram + 1000;
                tieMaxWeight.setText(String.valueOf(maxWeightInGram));

                String instruction = "Please keep <b>" + String.valueOf(minWeightInGram) + " grams</b> and <b>" + String.valueOf(maxWeightInGram/1000) + " kg</b> weighment ready with you,\nboth weights are essential for calibration.";

                tvInstruction.setText(Html.fromHtml(instruction));
                tvLblMessage.setText("Press Calibration button and follow the instructions.");


            }
        });

    }

    @Override
    protected void onStop() {
        super.onStop();
        usbSerialCommunication.disconnect();
    }

    private void startCalibration(float lowWeight, float highWeight, boolean isCalibMode) {
        sendToDevice.setStatus(false); //TODO ::  Do Not Make this value as true when it's in calibration mode.
        sendToDevice.setWeight(0f);
        sendToDevice.setCurtemperature(0f);
        sendToDevice.setSettemperature(0f);
        sendToDevice.setLowweight(lowWeight);
        sendToDevice.setHighweight(highWeight);
        sendToDevice.setCalib(isCalibMode); //TODO ::  Make this value as true when the.
//                UsbSerialCommunication.currentClass = "CCA";
        Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
       // Log.e(TAG, "DisplayEvents: SEND COMMAND " + gson.toJson(sendToDevice));

        usbSerialCommunication.sendData(gson.toJson(sendToDevice));
        usbSerialCommunication.sendData(gson.toJson(sendToDevice));
    }

    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void showAndProcessDoneDialog() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(CalibrationActivity.this);
                LayoutInflater inflater = getLayoutInflater();
                View view = inflater.inflate(R.layout.dialog_lottie, null);

                LottieAnimationView lottieAnimationView = view.findViewById(R.id.lottieAnimationView);
                TextView tvProgressDialog = view.findViewById(R.id.tvProgressDialog);
                MaterialButton btnDone = view.findViewById(R.id.doneButton);

                TextView tvProcessDoneText = view.findViewById(R.id.tvProcessDoneText);
                TextView tvOpenTheDoor = view.findViewById(R.id.tvOpenTheDoor);

                btnDone.setVisibility(View.VISIBLE);
                tvProcessDoneText.setVisibility(View.VISIBLE);
                tvOpenTheDoor.setVisibility(View.VISIBLE);
                btnDone.setText("Ok");

                tvProcessDoneText.setText("Calibration done");
                tvOpenTheDoor.setText("Remove all weight and close dispenser door.");
                tvProgressDialog.setVisibility(View.GONE);
                lottieAnimationView.setAnimation(R.raw.process_done);
                lottieAnimationView.setRepeatMode(LottieDrawable.RESTART);
                lottieAnimationView.playAnimation();

                // Customize the LottieAnimationView and TextView here

                builder.setView(view);
                builder.setCancelable(false); // Set to true if you want the dialog to be cancellable

                AlertDialog dialog = builder.create();
                if (!isFinishing() && !isDestroyed()) {
                    dialog.show();
                }


                btnDone.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();

                        try {
                            Constants.saveLogs(getApplicationContext(), "Calibration Done", KEY_CALIBRATION);
                        } catch (Exception e) {

                        }

                        Intent intent = new Intent(CalibrationActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();

                        /*new Thread(new Runnable() {
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
                        }).start();*/

//                doPostTransaction(Constants.PostTransactionURL);
                    }
                });
            }
        });


    }
}