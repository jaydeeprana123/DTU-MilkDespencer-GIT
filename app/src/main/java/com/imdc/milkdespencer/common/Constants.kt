package com.imdc.milkdespencer.common

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import android.text.InputFilter
import android.text.InputFilter.AllCaps
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.imdc.milkdespencer.R
import com.imdc.milkdespencer.adminUi.AdminActivity
import com.imdc.milkdespencer.enums.UserTypeEnum
import com.imdc.milkdespencer.models.Response.ConfigurationResponse
import com.imdc.milkdespencer.models.Response.ResponseOTP
import com.imdc.milkdespencer.network.ApiManager
import com.imdc.milkdespencer.network.ApiService
import com.imdc.milkdespencer.network.Utils
import com.imdc.milkdespencer.roomdb.AppDatabase
import com.imdc.milkdespencer.roomdb.entities.LogEntity
import com.imdc.milkdespencer.roomdb.entities.TransactionEntity
import com.imdc.milkdespencer.roomdb.interfaces.TransactionDao
import io.reactivex.rxjava3.observers.DisposableObserver
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.text.DecimalFormat
import java.util.Random
import java.util.concurrent.Executors
import java.util.regex.Pattern

class Constants {
    // Save the permission granted state
    private fun handlePermissionGranted() {
        preferencesManager!!.save(PREF_PERMISSION_GRANTED, true)
    }

    // Reset the permission granted state when USB device is disconnected
    private fun handlePermissionRevoked() {
        preferencesManager!!.save(PREF_PERMISSION_GRANTED, false)
    }

    companion object {
        @JvmField
        var cipDialog: AlertDialog? = null
        private const val PREFS_NAME = "usb_permission_prefs"
        const val PREF_PERMISSION_GRANTED = "permission_granted"
        const val TAG = "MilkDespencer"
        const val MachineId = "MachineId"
        const val RazorPayCustomerID = "RazorPayCustomerID"
        const val RegisterEndUser = "RegisterUser"
        const val RegisterCustomerAdmin = "RegisterCustomerAdmin"
        const val OwnerName = "OwnerName"

        //    public static final String MachineId = "MachineId";
        const val MilkBasePrice = "MilkBasePrice"
        const val LoginUser = "LoginUser"
        const val MilkDensityPref = "MilkDensity"
        const val ApiBaseUrl = "ApiBaseUrl"
        const val ScreenTimeOutPref = "ScreenTimeOut"
        const val TemperatureOffSet = "TemperatureOffSet"
        const val TemperatureSet = "TemperatureSet"
        const val CurrentTemperature = "CurrentTemperature"
        const val ResponseTempStatus = "ResponseTempStatus"
        const val ResponseMilkDispense = "ResponseMilkDispense"
        const val PaymentReceived = "PaymentReceived"
        const val PaymentCashReceived = "PaymentCashReceived"
        const val PaidAmt = "PaidAmt"
        const val BASE_URL = "https://portal.idmc.coop:5151/api/"
        const val GetConfigurationUrl = "SMSConfiguration/GetSMSConfiguration"
        const val SMSApiUrl = "SMSApiUrl"
        const val SMSSid = "SMSSid"
        const val SMSApiKey = "SMSApiKey"
        const val SMSSender = "SMSSender"
        const val SMSTemplateId = "SMSTemplateId"
        const val SMSTemplateContent = "SMSTemplateContent"
        const val RazorPayKey = "RazorPayKey"
        const val RazorPaySecretKey = "RazorPaySecretKey"
        const val PostTransactionURL = "/Transaction/PostTransaction"
        const val PostMerchantURL = "/Merchant/PostMerchant"
        @JvmField
        val df = DecimalFormat("0.00")
        private const val OTP = "SentOTP"

        // digit or special character
        private const val PASSWORD_PATTERN =
            "^(?!.*\\s)(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9!@#$%]).{8,20}$"
        private val pattern = Pattern.compile(PASSWORD_PATTERN)
        var preferencesManager: SharedPreferencesManager? = null
        const val FromScreen = "FromScreen"
        @JvmStatic
        fun showAlertDialog(context: Context?, title: String?, message: String?) {
            val builder = AlertDialog.Builder(
                context!!
            )
            builder.setTitle(title).setMessage(message)
                .setPositiveButton("OK") { dialog, which -> // Handle positive button click if needed
                    dialog.dismiss()
                }.show()
        }

        @JvmStatic
        fun showAcceptDialog(
            context: Context?,
            title: String?,
            message: String?,
            yesClickListener: DialogInterface.OnClickListener?,
            noClickListener: DialogInterface.OnClickListener?
        ) {
            val builder = AlertDialog.Builder(
                context!!
            )
            builder.setTitle(title).setMessage(message).setPositiveButton("Yes", yesClickListener)
                .setNegativeButton("No", noClickListener).show()
        }

        fun showConfigDialog(context: Context?) {
            // Create a layout inflater to inflate the custom dialog layout
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.configuration_dialog, null)
            val density = DoubleArray(1)
            preferencesManager = SharedPreferencesManager.getInstance(context)
            // Create the AlertDialog builder
            val builder = AlertDialog.Builder(
                context!!
            )
            builder.setView(view)

            // Create the AlertDialog
            val dialog = builder.create()

            // Find views in the custom layout
            val okButton = view.findViewById<MaterialButton>(R.id.okButton)
            val cancelButton = view.findViewById<MaterialButton>(R.id.cancelButton)
            val tieMilkBasePrice = view.findViewById<TextInputEditText>(R.id.tieMilkBasePrice)
            val tieMilkDensity = view.findViewById<TextInputEditText>(R.id.tieMilkDensity)
            val tieTimeOut = view.findViewById<TextInputEditText>(R.id.tieTimeOut)
            tieMilkBasePrice.setText(preferencesManager?.get(MilkBasePrice, "0.0").toString())
            tieMilkDensity.setText(preferencesManager?.get(MilkDensityPref, "0.0").toString())
            tieTimeOut.setText(preferencesManager?.get(ScreenTimeOutPref, "0").toString())


            // Set click listener for OK button
            okButton.setOnClickListener { // Handle OK button click
                val milkBasePrice = tieMilkBasePrice.text.toString()
                val milkDensity = tieMilkDensity.text.toString()
                val screenTimeOut = tieTimeOut.text.toString()
                preferencesManager?.save(MilkBasePrice, milkBasePrice)
                preferencesManager?.save(MilkDensityPref, milkDensity)
                preferencesManager?.save(ScreenTimeOutPref, screenTimeOut)
                dialog.dismiss()
            }

            // Set click listener for Cancel button
            cancelButton.setOnClickListener { // Handle Cancel button click
                // Dismiss the dialog
                dialog.dismiss()
            }

            // Show the dialog
            dialog.show()
        }

        /**
         * Api configuration dialog
         *
         * @param context
         */
        fun showAPIConfigDialog(context: Context?) {
            // Create a layout inflater to inflate the custom dialog layout
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.api_configuration_dialog, null)
            val density = DoubleArray(1)
            preferencesManager = SharedPreferencesManager.getInstance(context)
            // Create the AlertDialog builder
            val builder = AlertDialog.Builder(
                context!!
            )
            builder.setView(view)

            // Create the AlertDialog
            val dialog = builder.create()

            // Find views in the custom layout
            val okButton = view.findViewById<MaterialButton>(R.id.okButton)
            val cancelButton = view.findViewById<MaterialButton>(R.id.cancelButton)
            val tieApiBaseUrl = view.findViewById<TextInputEditText>(R.id.tieApiBaseUrl)

            /// If url is not set in shared preference.. It will take default base url
            tieApiBaseUrl.setText(
                preferencesManager?.get(
                    ApiBaseUrl,
                    "https://portal.idmc.coop:5151/"
                ).toString()
            )

            // Set click listener for OK button
            okButton.setOnClickListener { // Handle OK button click
                val apiBaseUrl = tieApiBaseUrl.text.toString()
                preferencesManager?.save(apiBaseUrl, apiBaseUrl)
                dialog.dismiss()
            }

            // Set click listener for Cancel button
            cancelButton.setOnClickListener { // Handle Cancel button click
                // Dismiss the dialog
                dialog.dismiss()
            }

            // Show the dialog
            dialog.show()
        }

        /*
    Show admin Config Dialog
     */
        fun showAdminConfigDialog(context: Context, userType: Int) {
            // Create a layout inflater to inflate the custom dialog layout
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.admin_configuration_dialog, null)
            preferencesManager = SharedPreferencesManager.getInstance(context)
            // Create the AlertDialog builder
            val builder = AlertDialog.Builder(context)
            builder.setView(view)

            // Create the AlertDialog
            val dialog = builder.create()

            // Find views in the custom layout
            val okButton = view.findViewById<MaterialButton>(R.id.okButton)
            val cancelButton = view.findViewById<MaterialButton>(R.id.cancelButton)
            val tilMachineId = view.findViewById<TextInputLayout>(R.id.tilMachineId)
            val tilTemperatureOffset = view.findViewById<TextInputLayout>(R.id.tilTemperatureOffset)
            val tilTemperatureSet = view.findViewById<TextInputLayout>(R.id.tilSetTemperature)
            val tilOwnerNameId = view.findViewById<TextInputLayout>(R.id.tilOwnerNameId)
            val tilMilkBasePrice = view.findViewById<TextInputLayout>(R.id.tilMilkBasePrice)
            val tilMilkDensity = view.findViewById<TextInputLayout>(R.id.tilMilkDensity)
            val tilTimeOut = view.findViewById<TextInputLayout>(R.id.tilTimeOut)
            val tieTemperatureOffset =
                view.findViewById<TextInputEditText>(R.id.tieTemperatureOffset)
            val tieTemperatureSet = view.findViewById<TextInputEditText>(R.id.tieSetTemperature)
            val tieOwnerNameId = view.findViewById<TextInputEditText>(R.id.tieOwnerNameId)
            val tieMilkBasePrice = view.findViewById<TextInputEditText>(R.id.tieMilkBasePrice)
            val tieMilkDensity = view.findViewById<TextInputEditText>(R.id.tieMilkDensity)
            val tieTimeOut = view.findViewById<TextInputEditText>(R.id.tieTimeOut)


            /// Machine Id Caps Capital
            val tieMachineId = view.findViewById<TextInputEditText>(R.id.tieMachineId)
            tieMachineId.filters = arrayOf<InputFilter>(AllCaps())
            if (userType == UserTypeEnum.ADMIN.value()) {
                tilMachineId.isEnabled = true
            } else if (userType == UserTypeEnum.CUSTOMER_ADMIN.value()) {
                tieMachineId.isEnabled = false
            } else if (userType == UserTypeEnum.END_USER.value()) {
                tieMachineId.isEnabled = false
                tilTemperatureOffset.isEnabled = false
                tilTemperatureSet.isEnabled = false
                tilMilkBasePrice.isEnabled = false
                tilMilkDensity.isEnabled = false
                tilTimeOut.isEnabled = false
                okButton.text = "Ok"
            }
            tilMachineId.isErrorEnabled = true
            tilTemperatureOffset.isErrorEnabled = true
            tilTemperatureSet.isErrorEnabled = true
            tilMilkBasePrice.isErrorEnabled = true
            tilMilkDensity.isErrorEnabled = true
            tilTimeOut.isErrorEnabled = true
            val machineId = preferencesManager?.get(MachineId, "000000A31122024").toString()

            //  tilMachineId.setEnabled(machineId.isEmpty() || machineId.equalsIgnoreCase("MachineId"));
            tieMachineId.setText(machineId)
            tieOwnerNameId.setText(preferencesManager?.get(OwnerName, "0.0").toString())
            tieTemperatureOffset.setText(
                preferencesManager?.get(TemperatureOffSet, "2.26").toString()
            )
            tieTemperatureSet.setText(preferencesManager?.get(TemperatureSet, "8.0").toString())
            tieMilkBasePrice.setText(preferencesManager?.get(MilkBasePrice, "100.0").toString())
            tieMilkDensity.setText(preferencesManager?.get(MilkDensityPref, "1.0").toString())
            tieTimeOut.setText(preferencesManager?.get(ScreenTimeOutPref, "15").toString())

            // Set click listener for OK button
            okButton.setOnClickListener(View.OnClickListener { // Handle OK button click


                /// If user type is not equal 1. The add into shared preference
                if (userType != 1) {
                    if (tieMachineId.text.toString().length != 15) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.machineId_validation),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@OnClickListener
                    }
                    val machineID = tieMachineId.text.toString()
                    val ownerName = tieOwnerNameId.text.toString()
                    val setTemperature = tieTemperatureSet.text.toString()
                    val offSetTemperature = tieTemperatureOffset.text.toString()
                    val milkBasePrice = tieMilkBasePrice.text.toString()
                    val milkDensity = tieMilkDensity.text.toString()
                    val screenTimeOut = tieTimeOut.text.toString()
                    Log.e("offSetTemperature", offSetTemperature)
                    preferencesManager?.save(MachineId, machineID)
                    preferencesManager?.save(OwnerName, ownerName)
                    preferencesManager?.save(TemperatureSet, setTemperature)
                    preferencesManager?.save(TemperatureOffSet, offSetTemperature)
                    preferencesManager?.save(MilkBasePrice, milkBasePrice)
                    preferencesManager?.save(MilkDensityPref, milkDensity)
                    preferencesManager?.save(ScreenTimeOutPref, screenTimeOut)
                }
                dialog.dismiss()
            })

            // Set click listener for Cancel button
            cancelButton.setOnClickListener { // Handle Cancel button click
                // Dismiss the dialog
                dialog.dismiss()
            }

            // Show the dialog
            dialog.show()
        }

        /*
    * if CIP is true = > Show this dialog
    * */
        fun showCIPRunningDialog(context: Context?) {
            // Create a layout inflater to inflate the custom dialog layout
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.dialog_cip_running, null)

            // Create the AlertDialog builder
            val builder = AlertDialog.Builder(
                context!!
            )
            builder.setView(view)

            // Create the AlertDialog
            cipDialog = builder.create()
            cipDialog!!.setCancelable(false)

            // Show the dialog
            cipDialog!!.show()
        }

        @JvmStatic
        fun showLoginDialog(context: Context, appDatabase: AppDatabase) {
            // Create a layout inflater to inflate the custom dialog layout
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.login_dialog, null)
            preferencesManager = SharedPreferencesManager.getInstance(context)
            // Create the AlertDialog builder
            val builder = AlertDialog.Builder(context)
            builder.setView(view)
            builder.setCancelable(false)
            // Create the AlertDialog
            val dialog = builder.create()

            // Find views in the custom layout
            val loginButton = view.findViewById<MaterialButton>(R.id.loginButton)
            val cancelButton = view.findViewById<MaterialButton>(R.id.cancelButton)
            val tilUserName = view.findViewById<TextInputLayout>(R.id.tilUsername)
            val tilPassword = view.findViewById<TextInputLayout>(R.id.tilPassword)
            val tieUsername = view.findViewById<TextInputEditText>(R.id.tieUsername)
            val tiePassword = view.findViewById<TextInputEditText>(R.id.tiePassword)
            val tvForgotPassword = view.findViewById<TextView>(R.id.tvForgotPassword)
            tilUserName.isErrorEnabled = true
            tilPassword.isErrorEnabled = true
            //        tieUsername.setText("admin");
//        tiePassword.setText("Admin@123");
            // Set click listener for Login button
            cancelButton.setOnClickListener { dialog.cancel() }
            loginButton.setOnClickListener { // Handle Login button click
                val username = tieUsername.text.toString()
                val password = tiePassword.text.toString()
                val handler = Handler(Looper.getMainLooper())
                Thread {
                    val userLIst = appDatabase.userDao().allUsers
                    Log.e("length of user", userLIst.size.toString())
                    for (i in userLIst.indices) {
                        Log.e("email", userLIst[i].username!!)
                        Log.e("email", userLIst[i].password!!)
                    }
                    val login = appDatabase.userDao().login(username, password)
                    if (login != null) {
                        Log.e(TAG, "onClick: " + Gson().toJson(login))
                        preferencesManager?.save(LoginUser, Gson().toJson(login))
                        val intent = Intent(context.applicationContext, AdminActivity::class.java)
                        intent.putExtra(LoginUser, Gson().toJson(login))
                        context.startActivity(intent)
                        dialog.dismiss()
                    } else {
                        handler.post {
                            Log.e(TAG, "onClick: " + Gson().toJson(login))
                            Toast.makeText(
                                context,
                                "Please Enter Valid Username and Password!!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }.start()
            }
            tvForgotPassword.setOnClickListener { // Handle Cancel button click
                // Dismiss the dialog
                dialog.dismiss()
                showForgotPasswordDialog(context, appDatabase)
            }

            // Show the dialog
            dialog.show()
        }

        fun showForgotPasswordDialog(context: Context, appDatabase: AppDatabase) {
            preferencesManager = SharedPreferencesManager.getInstance(context)
            val isOtpSend = booleanArrayOf(false)

            /// Url get from shared preference
            val retrofit = Retrofit.Builder().baseUrl(
                preferencesManager?.get(SMSApiUrl, "https://api.kaleyra.io/v1/").toString()
            ) // Replace with your base URL
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create()) // Add this line
                .addConverterFactory(GsonConverterFactory.create()).build()


//        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://api.kaleyra.io/v1/") // Replace with your base URL
//                .addConverterFactory(GsonConverterFactory.create()).addCallAdapterFactory(RxJava3CallAdapterFactory.create()) // Add this line
//                .addConverterFactory(GsonConverterFactory.create()).build();
            val apiService = retrofit.create(ApiService::class.java)
            val apiManager = ApiManager(apiService)

            // Create a layout inflater to inflate the custom dialog layout
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.forgot_password, null)
            preferencesManager = SharedPreferencesManager.getInstance(context)
            // Create the AlertDialog builder
            val builder = AlertDialog.Builder(context)
            builder.setView(view)

            // Create the AlertDialog
            val dialog = builder.create()

            // Find views in the custom layout
            val tilUsername = view.findViewById<TextInputLayout>(R.id.tilForgotUsername)
            val tilOtp = view.findViewById<TextInputLayout>(R.id.tilOtp)
            val tilNewPassword = view.findViewById<TextInputLayout>(R.id.tilNewPassword)
            val tilConfirmPassword = view.findViewById<TextInputLayout>(R.id.tilConfirmPassword)
            val tieMobileNo = view.findViewById<TextInputEditText>(R.id.tiePhoneNo)
            val tieOtp = view.findViewById<TextInputEditText>(R.id.tieOtp)
            val tieNewPassword = view.findViewById<TextInputEditText>(R.id.tieNewPassword)
            val tieConfirmPassword = view.findViewById<TextInputEditText>(R.id.tieConfirmPassword)
            val btnSendOtp = view.findViewById<MaterialButton>(R.id.btnSendOtp)
            val btnResetPassword = view.findViewById<MaterialButton>(R.id.btnResetPassword)
            tilUsername.isErrorEnabled = true
            tilNewPassword.isErrorEnabled = true
            tilConfirmPassword.isErrorEnabled = true
            tilOtp.isErrorEnabled = true
            tilOtp.visibility = View.GONE
            tilNewPassword.visibility = View.GONE
            tilConfirmPassword.visibility = View.GONE
            btnResetPassword.visibility = View.GONE
            tilOtp.visibility = View.VISIBLE
            tilNewPassword.visibility = View.VISIBLE
            tilConfirmPassword.visibility = View.VISIBLE
            btnResetPassword.visibility = View.VISIBLE

//        btnSendOtp.setText(isOtpSend[0] ? "Verify OTP" : "Send OTP");

            // Set click listener for Login button
            btnSendOtp.setOnClickListener {
                val phoneNo = tieMobileNo.text.toString()
                if (isValidMobileNumber(phoneNo)) {

                    /// New code
                    Thread {
                        val mobileNoExists = appDatabase.userDao().mobileNoExists(phoneNo)
                        if (mobileNoExists <= 0) {
                            val handler = Handler(Looper.getMainLooper())
                            handler.post {
                                Toast.makeText(
                                    context,
                                    "Please Enter Valid Mobile No!!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            // Run UI-related operations on the main thread
                            val handler = Handler(Looper.getMainLooper())
                            handler.post {
                                val pd = ProgressDialog(context)
                                pd.setTitle("Please Wait...")
                                pd.setTitle("Please Wait...")
                                pd.setCancelable(false)
                                pd.show()

                                // Perform network operation in a background thread
                                Thread {
                                    val otp: String =
                                        "OTP for MVM password reset is " + generateOtp(6) + ". -IDMC"
                                    val content: String =
                                        "to=" + phoneNo + "&type=OTP&sender=" + preferencesManager?.get(
                                            SMSSender, "IDMCCS"
                                        ).toString() + "&body=" + otp
                                    Log.e(TAG, "onClick: $content")
                                    val fields: HashMap<String, String> = HashMap()
                                    fields.put("to", "+91$phoneNo")
                                    fields.put("type", "OTP")

                                    /// Get from shared preference
                                    fields.put(
                                        "sender",
                                        preferencesManager?.get(SMSSender, "IDMCCS").toString()
                                    )

//                                        fields.put("sender", "IDMCCS");
                                    fields.put("body", otp)

                                    /// Get from shared preference
                                    fields.put(
                                        "api-key",
                                        preferencesManager?.get(
                                            SMSApiKey,
                                            "Ae0de2903bdeb26110fd03ccab96e92a1"
                                        ).toString()
                                    )
                                    //                                        fields.put("api-key", "Ae0de2903bdeb26110fd03ccab96e92a1");
                                    val headers: HashMap<String, String> = HashMap()
                                    headers.put("Content-Type", "application/x-www-form-urlencoded")
                                    headers.put(
                                        "api-key",
                                        preferencesManager?.get(
                                            SMSApiKey,
                                            "Ae0de2903bdeb26110fd03ccab96e92a1"
                                        ).toString()
                                    )

                                    /// Old API : A5b9c8ba406fbc9bf361ffeb8bf6cb120
                                    val disposableObserver: DisposableObserver<ResponseBody?> =
                                        object : DisposableObserver<ResponseBody?>() {
                                            override fun onNext(response: ResponseBody?) {
                                                handler.post({
                                                    pd.dismiss() // Dismiss the ProgressDialog on the main thread
                                                    if (!response.toString().isEmpty()) {
                                                        isOtpSend[0] = true
                                                        val responseModel: ResponseOTP? =
                                                            Gson().fromJson(
                                                                response?.charStream(),
                                                                ResponseOTP::class.java
                                                            )
                                                        if (responseModel != null) {
                                                            Log.e(
                                                                TAG,
                                                                "onNext: " + Gson().toJson(
                                                                    responseModel
                                                                )
                                                            )
                                                            if (responseModel.error != null) {
                                                                tilOtp.visibility = View.VISIBLE
                                                                tilNewPassword.visibility =
                                                                    View.VISIBLE
                                                                tilConfirmPassword.visibility =
                                                                    View.VISIBLE
                                                                btnResetPassword.visibility =
                                                                    View.VISIBLE
                                                                btnSendOtp.visibility = View.GONE
                                                                tilUsername.visibility = View.GONE
                                                                val OTP: String? =
                                                                    extractOTP(responseModel.body)
                                                                preferencesManager?.save(
                                                                    Companion.OTP,
                                                                    OTP
                                                                )
                                                            }
                                                        }
                                                    }
                                                })
                                            }


                                            override fun onError(e: Throwable) {
                                                handler.post({
                                                    pd.dismiss() // Dismiss the ProgressDialog on the main thread
                                                    Utils.handleApiError(context, e, apiManager)
                                                })
                                            }

                                            override fun onComplete() {
                                                // Handle completion if needed
                                            }
                                        }

                                    /// Get from shared preference
                                    apiManager.makeOTPRequestCall(
                                        ((preferencesManager?.get(
                                            SMSSid,
                                            "https://api.kaleyra.io/v1/"
                                        ).toString())
                                                + "/messages/"), fields, headers, disposableObserver
                                    )
                                }.start()
                            }
                        }
                        Log.e(TAG, "onClick:mobileNoExists $mobileNoExists")
                    }.start()


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
                    tieMobileNo.error = context.getString(R.string.alertForValidMobile)
                }
            }
            btnResetPassword.setOnClickListener {
                // Handle Cancel button click
                // Dismiss the dialog
                val password = tieNewPassword.text.toString()
                val confirmPassword = tieNewPassword.text.toString()
                val otp = tieOtp.text.toString()
                val sentOTP = preferencesManager?.get(OTP, "") as String
                Log.e(TAG, "onClick: $sentOTP")
                if (otp.isEmpty()) {
                    tilOtp.error = "Field Cannot be empty!!"
                } else {
                    if (otp.length < 6) {
                    }
                }
                if (isValid(password) && isValid(confirmPassword) && password.equals(
                        confirmPassword,
                        ignoreCase = true
                    )
                ) {
                    Executors.newSingleThreadExecutor().execute {

                        // Perform the query in the background thread
                        val user =
                            appDatabase.userDao().getUserByMobile(tieMobileNo.text.toString())
                        if (user != null) {
                            // Update the user object
                            user.password = password

                            // Update the user in the database
                            appDatabase.userDao().update(user)

                            /// If user type is end user
                            if (user.userType == UserTypeEnum.END_USER.value()) {
                                preferencesManager?.save(RegisterEndUser, Gson().toJson(user))
                            } else if (user.userType == UserTypeEnum.CUSTOMER_ADMIN.value()) {
                                /// If user type is customer user
                                preferencesManager?.save(RegisterCustomerAdmin, Gson().toJson(user))
                            }

                            // Optionally handle the UI on the main thread
                            Handler(Looper.getMainLooper()).post({
                                Log.e("first name", (user.first_name)!!)
                                Log.e("new password", user.password!!)
                                Toast.makeText(
                                    context,
                                    "Password updated successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                dialog.cancel()
                            })
                        } else {
                            Handler(Looper.getMainLooper()).post({
                                Toast.makeText(
                                    context,
                                    "User not found!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            })
                        }
                    }
                } else {
                    // password is invalid, show error message
                    if (!isValid(password)) {
                        tilNewPassword.error =
                            "Password must contain at least one lowercase character, one uppercase character, one digit, one special character, and is between 8 to 20 characters long."
                    } else if (!isValid(confirmPassword)) {
                        tilConfirmPassword.error =
                            "Password must contain at least one lowercase character, one uppercase character, one digit, one special character, and is between 8 to 20 characters long."
                    } else {
                        if (password.isEmpty()) {
                            tilNewPassword.error = "Field cannot be empty."
                        }
                        if (confirmPassword.isEmpty()) {
                            tilConfirmPassword.error = "Field cannot be empty."
                        }
                        //                        tieConfirmPassword.setError("Confirm Password did not match..");
                    }
                }
                //                dialog.dismiss();
            }

            // Show the dialog
            dialog.show()
        }

        fun isValid(password: String?): Boolean {
            val matcher = pattern.matcher(password)
            return matcher.matches()
        }

        fun generateOtp(length: Int): String {
            var otp = ""
            val characters = "0123456789"
            val random = Random()
            for (i in 0 until length) {
                val randomIndex = random.nextInt(characters.length)
                otp += characters[randomIndex]
            }
            return otp
        }

        fun extractOTP(inputString: String?): String? {
            // The regex pattern to match the OTP format: a 6-digit number
            val regexPattern = "\\b\\d{6}\\b"
            val pattern = Pattern.compile(regexPattern)
            val matcher = pattern.matcher(inputString)
            return if (matcher.find()) {
                val otp = matcher.group()
                Log.d("OTP_TAG", "Extracted OTP: $otp")
                otp
            } else {
                Log.d("OTP_TAG", "No OTP found in the input string")
                null
            }
        }

        fun isValidMobileNumber(mobileNumber: String?): Boolean {
            val regex = "^[0-9]{10}$"
            val pattern = Pattern.compile(regex)
            val matcher = pattern.matcher(mobileNumber)
            return matcher.matches()
        }

        fun calculateMilkWeight(literValue: Double, context: Context?): Double {
            preferencesManager = SharedPreferencesManager.getInstance(context)
            val milkBasePrice = preferencesManager?.get(MilkBasePrice, "0.0").toString().toFloat()
            val DENSITY_OF_MILK =
                preferencesManager?.get(MilkDensityPref, "0.0").toString().toFloat()
            val amountInLiters = milkBasePrice * literValue
            Log.e(TAG, "calculateMilkWeight: BP $milkBasePrice <+++> $amountInLiters")
            return amountInLiters * DENSITY_OF_MILK
        }

        @JvmStatic
        fun calculateMilkPrice(literValue: Double, context: Context?): Double {
            preferencesManager = SharedPreferencesManager.getInstance(context)
            val milkBasePrice = preferencesManager?.get(MilkBasePrice, "0.0").toString().toFloat()
            val amountInLiters = milkBasePrice * literValue
            Log.e(TAG, "calculateMilkWeight: BP $milkBasePrice <+++> $amountInLiters")
            return amountInLiters
        }

        @JvmStatic
        fun calculateMilkAmount(cost: Double, context: Context?): Double {
            preferencesManager = SharedPreferencesManager.getInstance(context)
            val milkBasePrice = preferencesManager?.get(MilkBasePrice, "0.0").toString().toFloat()
            val DENSITY_OF_MILK =
                preferencesManager?.get(MilkDensityPref, "0.0").toString().toFloat()
            Log.e(
                TAG,
                "calculateMilkAmount: BasePrice " + milkBasePrice + " <+++> " + cost / milkBasePrice
            )
            return cost / milkBasePrice * DENSITY_OF_MILK
        }

        @JvmStatic
        fun saveLogs(context: Context?, message: String?) {
            Thread {
                val database = AppDatabase.getInstance(context)
                val logDao = database.logDao()
                val logEntity = LogEntity(message!!)
                logDao.insert(logEntity)
                Log.e(TAG, "run: saveLogs " + logDao.allLogs)
            }.start()
        }

        fun showLottieDialog(context: Context?, percentage: Double): AlertDialog {
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.dialog_lottie, null)
            val builder = AlertDialog.Builder(
                context!!
            )
            builder.setView(view)
            val dialog = builder.create()
            val lottieAnimationView =
                view.findViewById<LottieAnimationView>(R.id.lottieAnimationView)
            val tvProgress = view.findViewById<TextView>(R.id.tvProgressDialog)
            tvProgress.text = "Please Wait " + if (percentage != 0.0) percentage else ""
            return dialog
        }

        fun generateRandomTransactionId(): String {
            val timestamp = System.currentTimeMillis()

            // Generate a random number
            val random = Random()
            val randomNumber = random.nextInt(1000000) // Adjust the range as needed
            return timestamp.toString() + randomNumber
        }

        @JvmStatic
        fun insertTransaction(
            activity: Activity,
            transactionDao: TransactionDao,
            transactionType: String?,
            bankTransactionNo: String?,
            transactionDate: String?,
            transactionTime: String?,
            amount: Double,
            transactionStatus: String?,
            upiId: String?,
            volume: Float,
            milkTemperature: String?
        ): Long {
            val preferencesManager = SharedPreferencesManager.getInstance(activity)
            val transaction = TransactionEntity()
            transaction.userName = "Admin"
            transaction.password = "QWRtaW4="
            transaction.transactionType = transactionType
            transaction.bankTransactionNo = bankTransactionNo
            transaction.transactionDate = transactionDate
            transaction.transactionTime = transactionTime
            transaction.amount = amount
            transaction.volume = volume

            /// Added new on 4-1-2025
            transaction.milkPrice = preferencesManager[MilkBasePrice, ""].toString()
            transaction.milkTemperature = milkTemperature
            transaction.transactionStatus = transactionStatus
            transaction.upiId = upiId
            transaction.uniqueTransactionId = transactionDao.generateUniqueTransactionId()

            /// Added on 1-1 2025
            transaction.machineId = preferencesManager[MachineId, ""].toString()

            /// Insert into Sqlite database
            val transactionId = transactionDao.insert(transaction)
            if (isNetworkAvailable(activity)) {
                doPostTransaction(
                    preferencesManager,
                    "/api/Transaction/PostTransaction",
                    transaction,
                    activity
                )
            } else {
                //   Toast.makeText(activity, "Internet not available", Toast.LENGTH_SHORT).show();
            }
            return transactionId
        }

        fun doPostTransaction(
            preferencesManager: SharedPreferencesManager,
            url: String?,
            transaction: TransactionEntity?,
            activity: Activity?
        ) {
            Log.e(
                "base Url ",
                preferencesManager[ApiBaseUrl, "https://portal.idmc.coop:5151/"].toString()
            )
            val retrofit = Retrofit.Builder()
                .baseUrl(preferencesManager[ApiBaseUrl, "https://portal.idmc.coop:5151/"].toString()) // Replace with your base URL
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create()) // Add this line
                .addConverterFactory(GsonConverterFactory.create()).build()
            val apiService = retrofit.create(ApiService::class.java)
            val apiManager = ApiManager(apiService)
            val header = HashMap<String, String>()
            val mediaType = MediaType.parse("application/json; charset=utf-8")
            val request = Gson().toJson(transaction)
            val requestBody = RequestBody.create(mediaType, request)
            Log.e(TAG, "doPostTransaction: " + Gson().toJson(requestBody))
            val handler = Handler(Looper.getMainLooper())
            handler.post {

//            ProgressDialog pd = new ProgressDialog(activity);
//            pd.setTitle("Please Wait...");
//            pd.setCancelable(false);
//            pd.show();
                val disposableObserver: DisposableObserver<ResponseBody?> =
                    object : DisposableObserver<ResponseBody?>() {
                        override fun onNext(response: ResponseBody?) {
//                    pd.dismiss();
                            if (!response.toString().isEmpty()) {
                                val json = Gson().toJson(
                                    Gson().fromJson(
                                        response?.charStream(),
                                        JsonElement::class.java
                                    )
                                )
                                Log.e(TAG, "onNext: $json")
                            }
                        }

                        override fun onError(e: Throwable) {
                            Log.e(TAG, "onError: $e")

                            // Handle the error
//                    if (pd != null && pd.isShowing()) {
//                        pd.dismiss();
//                    }
                            e.printStackTrace()

                            /// Here I comment because when internet is not available then it will be crash because of this
//                    activity.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Utils.handleApiError(activity, e, apiManager);
//                        }
//                    });
                        }

                        override fun onComplete() {
                            // Handle completion if needed
                        }
                    }
                apiManager.makePostRequestCall(url, requestBody, header, disposableObserver)
            }
        }

        /*Here in this getting the configuration data (SMS, Razorpay). We have to save it into shared preference*/
        fun doGetConfigurationData(activity: Activity) {
            val retrofit = Retrofit.Builder()
                .baseUrl(preferencesManager!![ApiBaseUrl, "https://portal.idmc.coop:5151/api/"].toString()) // Replace with your base URL
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create()) // Add this line
                .addConverterFactory(GsonConverterFactory.create()).build()
            val apiService = retrofit.create(ApiService::class.java)
            val apiManager = ApiManager(apiService)
            val handler = Handler(Looper.getMainLooper())
            handler.post {
                val pd: ProgressDialog = ProgressDialog(activity)
                pd.setTitle("Please Wait...")
                pd.setCancelable(false)
                pd.show()
                val disposableObserver: DisposableObserver<ResponseBody?> =
                    object : DisposableObserver<ResponseBody?>() {
                        override fun onNext(response: ResponseBody?) {
                            pd.dismiss()
                            if (!response.toString().isEmpty()) {
                                val json = Gson().toJson(
                                    Gson().fromJson(
                                        response?.charStream(),
                                        JsonElement::class.java
                                    )
                                )
                                Log.e(TAG, "Configuration Data: $json")
                                preferencesManager = SharedPreferencesManager.getInstance(activity)
                                val configurationResponse =
                                    Gson().fromJson(json, ConfigurationResponse::class.java)


                                /// Save in shared preference
                                Log.e(
                                    TAG,
                                    "RazorPayKey: " + configurationResponse.data[0].razorPayKey
                                )
                                preferencesManager?.save(
                                    SMSApiUrl,
                                    configurationResponse.data[0].smsAPIURL
                                )
                                preferencesManager?.save(
                                    SMSSid,
                                    configurationResponse.data[0].smsSid
                                )
                                preferencesManager?.save(
                                    SMSApiKey,
                                    configurationResponse.data[0].smsAPIKey
                                )
                                preferencesManager?.save(
                                    SMSSender,
                                    configurationResponse.data[0].smsSender
                                )
                                preferencesManager?.save(
                                    SMSTemplateId,
                                    configurationResponse.data[0].smsTemplateID
                                )
                                preferencesManager?.save(
                                    SMSTemplateContent,
                                    configurationResponse.data[0].smsTemplateContent
                                )
                                preferencesManager?.save(
                                    RazorPayKey,
                                    configurationResponse.data[0].razorPayKey
                                )
                                preferencesManager?.save(
                                    RazorPaySecretKey,
                                    configurationResponse.data[0].razorPaySecretKey
                                )
                            }
                        }

                        override fun onError(e: Throwable) {
                            // Handle the error
                            if (pd != null && pd.isShowing) {
                                pd.dismiss()
                            }
                            e.printStackTrace()
                            activity.runOnUiThread(Runnable {
                                Utils.handleApiError(
                                    activity,
                                    e,
                                    apiManager
                                )
                            })
                        }

                        override fun onComplete() {
                            // Handle completion if needed
                        }
                    }
                apiManager.makeGetResponseCall(GetConfigurationUrl, disposableObserver)
            }
        }

        /*Check that internet connection is available or not*/
        private fun isNetworkAvailable(context: Activity): Boolean {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo = connectivityManager?.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
    }
}
