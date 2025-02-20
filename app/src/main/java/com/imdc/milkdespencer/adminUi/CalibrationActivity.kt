package com.imdc.milkdespencer.adminUi

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.GsonBuilder
import com.imdc.milkdespencer.MainActivity
import com.imdc.milkdespencer.R
import com.imdc.milkdespencer.common.UsbSerialCommunication
import com.imdc.milkdespencer.models.SendToDevice

class CalibrationActivity : AppCompatActivity() {
    private val TAG = javaClass.simpleName
    var strMessage = StringBuilder()
    var sendToDevice = SendToDevice()
    var isSent = false
    var knownWeight = 0.0
    var maxWeight = 0.0
    private lateinit var tilKnownWeight: TextInputLayout
    private lateinit var tilMaxWeight: TextInputLayout
    private lateinit var tieKnownWeight: TextInputEditText
    private lateinit var tieMaxWeight: TextInputEditText
    private lateinit var startCalibrationBtn: MaterialButton
    private lateinit var tvMessage: TextView
    private lateinit var tvLblMessage: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calibration)
        usbSerialCommunication = UsbSerialCommunication(
            applicationContext
        )
        if (!usbSerialCommunication!!.connected) {
            usbSerialCommunication!!.connect()
            usbSerialCommunication!!.setBaudRate(115200)
            startCalibration(0f, 0f, false)
        }
        tilKnownWeight = findViewById(R.id.tilKnownWeight)
        tilMaxWeight = findViewById(R.id.tilMaxWeight)
        tieKnownWeight = findViewById(R.id.tieKnownWeight)
        tieMaxWeight = findViewById(R.id.tieMaxWeight)
        startCalibrationBtn = findViewById(R.id.startCalibrationBtn)
        tvMessage = findViewById(R.id.tv_Message)
        tvLblMessage = findViewById(R.id.tv_lbl_Message)
        tilKnownWeight.setEnabled(true)
        tilMaxWeight.setEnabled(true)
        tieKnownWeight = findViewById(R.id.tieKnownWeight)
        tieMaxWeight = findViewById(R.id.tieMaxWeight)
        tieMaxWeight.setText("10000")
        tieKnownWeight.setText("500")
        // Set up input validation for the known weight field
        tilKnownWeight.setEndIconOnClickListener(View.OnClickListener {
            val knownWeight = tieKnownWeight.getText().toString()
            if (TextUtils.isEmpty(knownWeight)) {
                tilKnownWeight.setError("Please enter a known weight")
            } else {
                tilKnownWeight.setError(null)
            }
            if (!isNumeric(knownWeight)) {
                tilKnownWeight.setError("Please enter a numeric value")
            } else {
                tilKnownWeight.setError(null)
            }
        })

        // Set up input validation for the max weight field
        tilMaxWeight.setEndIconOnClickListener(View.OnClickListener {
            val maxWeight = tieMaxWeight.getText().toString()
            if (TextUtils.isEmpty(maxWeight)) {
                tilMaxWeight.setError("Please enter a max weight")
            } else tilMaxWeight.setError(null)
            if (!isNumeric(maxWeight)) {
                tilMaxWeight.setError("Please enter a numeric value")
            } else tilMaxWeight.setError(null)
        })
        startCalibrationBtn.setOnClickListener(View.OnClickListener {
            val knownWeightStr = tieKnownWeight.getText().toString()
            val maxWeightStr = tieMaxWeight.getText().toString()


            // Validate input
            if (TextUtils.isEmpty(knownWeightStr) || TextUtils.isEmpty(maxWeightStr)) {
                if (TextUtils.isEmpty(knownWeightStr)) tilKnownWeight.setError("Please fill in all fields") else tilKnownWeight.setError(
                    null
                )
                if (TextUtils.isEmpty(maxWeightStr)) tilMaxWeight.setError("Please fill in all fields") else tilMaxWeight.setError(
                    null
                )
            } else if (!isNumeric(knownWeightStr) || !isNumeric(maxWeightStr)) {
                if (!isNumeric(knownWeightStr)) tilKnownWeight.setError("Please enter numeric values for known weight and max weight") else tilKnownWeight.setError(
                    null
                )
                if (!isNumeric(maxWeightStr)) tilMaxWeight.setError("Please enter numeric values for known weight and max weight") else tilMaxWeight.setError(
                    null
                )
            } else {
                tilKnownWeight.setHint(getString(R.string.knownWeightInKg))
                tilMaxWeight.setHint(getString(R.string.maxKnownWeightInKg))
                if (knownWeightStr.toDouble() <= 0) {
                    tilKnownWeight.setError("Value cannot be less then zero")
                    return@OnClickListener
                }
                if (maxWeightStr.toDouble() <= 0) {
                    tilMaxWeight.setError("Value cannot be less then zero")
                    return@OnClickListener
                }
                if (!isSent) {
                    // Start calibration process
                    knownWeight = knownWeightStr.toDouble() / 1000
                    maxWeight = maxWeightStr.toDouble() / 1000
                }


                // Display the converted values in a TextView
                @SuppressLint("DefaultLocale") val knownWeightFinalVal =
                    String.format("%.2f", knownWeight)
                tieKnownWeight.setText(knownWeightFinalVal)
                @SuppressLint("DefaultLocale") val maxWeightFinalVal =
                    String.format("%.2f", maxWeight)
                tieMaxWeight.setText(maxWeightFinalVal)
                val lowWeight = knownWeightFinalVal.toFloat()
                val highWeight = maxWeightFinalVal.toFloat()
                tilKnownWeight.setEnabled(false)
                tilMaxWeight.setEnabled(false)
                startCalibration(lowWeight, highWeight, true)
                isSent = true

//                    usbSerialCommunication.sendData("1234", true);
                usbSerialCommunication!!.setReadDataListener { data ->
                    Log.e(TAG, "onReadData: $data")
                    var message = ""
                    when (data) {
                        "0" -> {
                            message = "Calibration Done."
                            startCalibration(0f, 0f, false)
                            showAndProcessDoneDialog()
                            runOnUiThread { /*CalibrationActivity.this.finish();
                                            Intent intent = new Intent(CalibrationActivity.this, MainActivity.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                            startActivity(intent);*/
                            }
                        }

                        "1" -> message = "Calib function ON Tare.Remove any weights from the scale."
                        "2" -> message = "Tare done...Place a low known weight on the scale..."
                        "3" -> message = "Tare Again... remove any weights from the scale."
                        "4" -> message = "Tare done..Place a high known weight on the scale.."
                        else -> message = "Calibration Process"
                    }
                    tvMessage.setText(message)

                    /*strMessage.append("<<<<<<<<<<<< Calibration Start >>>>>>>>>>>>\n");
                                                strMessage.append("\n");
                                                strMessage.append(data);*/
                }
            }
        })
    }

    override fun onStop() {
        super.onStop()
        usbSerialCommunication!!.disconnect()
    }

    private fun startCalibration(lowWeight: Float, highWeight: Float, isCalibMode: Boolean) {
        sendToDevice.isStatus =
            false //TODO ::  Do Not Make this value as true when it's in calibration mode.
        sendToDevice.weight = 0f
        sendToDevice.curtemperature = 0f
        sendToDevice.settemperature = 0f
        sendToDevice.lowweight = lowWeight
        sendToDevice.highweight = highWeight
        sendToDevice.isCalib = isCalibMode //TODO ::  Make this value as true when the.
        //                UsbSerialCommunication.currentClass = "CCA";
        val gson = GsonBuilder().serializeSpecialFloatingPointValues().create()
        Log.e(TAG, "DisplayEvents: SEND COMMAND " + gson.toJson(sendToDevice))
        usbSerialCommunication!!.sendData(gson.toJson(sendToDevice))
        usbSerialCommunication!!.sendData(gson.toJson(sendToDevice))
    }

    private fun isNumeric(str: String): Boolean {
        return try {
            str.toDouble()
            true
        } catch (e: NumberFormatException) {
            false
        }
    }

    fun showAndProcessDoneDialog() {
        runOnUiThread {
            val builder = AlertDialog.Builder(this@CalibrationActivity)
            val inflater = layoutInflater
            val view = inflater.inflate(R.layout.dialog_lottie, null)
            val lottieAnimationView =
                view.findViewById<LottieAnimationView>(R.id.lottieAnimationView)
            val tvProgressDialog = view.findViewById<TextView>(R.id.tvProgressDialog)
            val btnDone = view.findViewById<MaterialButton>(R.id.doneButton)
            btnDone.visibility = View.VISIBLE
            tvProgressDialog.visibility = View.GONE
            lottieAnimationView.setAnimation(R.raw.process_done)
            lottieAnimationView.repeatMode = LottieDrawable.RESTART
            lottieAnimationView.playAnimation()

            // Customize the LottieAnimationView and TextView here
            builder.setView(view)
            builder.setCancelable(false) // Set to true if you want the dialog to be cancellable
            val dialog = builder.create()
            dialog.show()
            btnDone.setOnClickListener {
                dialog.dismiss()
                val intent = Intent(this@CalibrationActivity, MainActivity::class.java)
                startActivity(intent)
                finish()

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
        }
    }

    companion object {
        private var usbSerialCommunication: UsbSerialCommunication? = null
    }
}