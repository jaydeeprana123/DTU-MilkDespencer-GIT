package com.imdc.milkdespencer

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.imdc.milkdespencer.common.Constants
import com.imdc.milkdespencer.common.SharedPreferencesManager
import com.imdc.milkdespencer.common.UsbSerialCommunication
import com.imdc.milkdespencer.common.UsbSerialCommunication.ReadDataListener
import com.imdc.milkdespencer.enums.ScreenEnum
import com.imdc.milkdespencer.models.ResponseTempStatus
import com.imdc.milkdespencer.roomdb.AppDatabase
import com.imdc.milkdespencer.roomdb.entities.User
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), ReadDataListener {
    private var isUsbPermissionGranted = false // Flag for USB permission
    private var getChargingState = false
    private var getUsbShowState = false
    private val handler = Handler()
    var gson = GsonBuilder().serializeSpecialFloatingPointValues().create()
    var filter = IntentFilter(ACTION_USB_PERMISSION)
    var llCash: LinearLayout? = null
    var llQr: LinearLayout? = null
    var lvStatus: LinearLayout? = null
    var llAlert: LinearLayout? = null
    var ivAgitator: ImageView? = null
    var ivCompressor: ImageView? = null
    var alertDialog: AlertDialog? = null
    var isShowError = false

    //    private  UsbSerialManager usbSerialManager;
    private var usbSerialCommunication: UsbSerialCommunication? = null

    ///TODO: 1) Read Continuous data from Serial // { "temperature": "3.04",  should not be more than set Temperature divide by 10 and then add offset value
    //  "compressor": true, green and red indicators
    //  "agitator": false, green and red indicators
    //  "lowlevel": true, if true close the system and show the dialog low on Milk.}
    //  2) Add Agitator and the Compressor, Milk Rate to the right side of the IDMC logo.
    //  3) Admin Screen UI which can be used for the add user and configurations.
    //  4) API Calls ==> Transactions
    val usbPermissionReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent == null || ACTION_USB_PERMISSION != intent.action) {
                return
            }
            val usbDevice = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
            val permissionGranted =
                intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            if (permissionGranted) {
                if (usbDevice != null) {
                    // Set flag to true
                    usbSerialCommunication!!.openConnection(usbDevice)
                } else {
                    Log.e(TAG, "USB device is null.")
                }
            } else {
                Log.e(TAG, "USB permission denied.")
            }
            if (context != null) {
                try {
                    context.unregisterReceiver(this)
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "Receiver already unregistered: " + e.message)
                }
            }
        }
    }
    private var btnPayWithCash: Button? = null
    private var btnPayWithQr: Button? = null
    private var btnStart: Button? = null
    private var btnDone: Button? = null
    private var cvPayWithCash: CardView? = null
    private var cvPayWithQr: CardView? = null
    private var cv_error: CardView? = null
    private var appDatabase: AppDatabase? = null
    private var tvTemperature: TextView? = null
    private var tvMilkBasePrice: TextView? = null
    private var tv_Message: TextView? = null
    private var lvAnimation: LottieAnimationView? = null

    /*
    * Battery Charging Broad Cast Receiver*/
    private val batteryReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent == null) return
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging =
                status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            Log.e("Battery Status", "Charging: $isCharging")
            if (isCharging) {
                getChargingState = true
                checkAndRequestUsbPermission()
            } else {
                getChargingState = false
                getUsbShowState = false
                isUsbPermissionGranted = false
                handleNotChargingState()
            }
        }

        /*
        * If Battery is in Charging Or Fully Charged State*/
        private fun handleChargingState() {
            Log.e("Battery Status", "Device is charging.")
            handler.postDelayed({

                /// If Usb Serial Communication is connected
                if (usbSerialCommunication!!.connected) {
                    /// Charging is not available screen will be hide
                    cv_error!!.visibility = View.GONE
                    getUsbShowState = true
                    /// Here if usb serial connected,
                    // then PAY WITH CASH = Enable && PAY WITH UPI = Enable
                    btnPayWithCash!!.isEnabled = true
                    btnPayWithQr!!.isEnabled = true
                    Log.e("USB Communication", "Connected while charging.")

                    /// If Pay With Cash and Pay With QR Button is Not Visible
                    if (llCash!!.visibility == View.GONE && llQr!!.visibility == View.GONE) {
                        updateUIForChargingState()
                    } else if (llCash!!.visibility == View.VISIBLE && llQr!!.visibility == View.VISIBLE) {
                        Log.e("btnStart", "Gone")
                        /// If Pay With Cash and Pay With QR Button is Visible, Start Button should be gone
                        btnStart!!.visibility = View.GONE
                    }
                } else {
                    tv_Message!!.text = "No USB connection. please try after some time."
                    btnDone!!.text = "Connect"
                    btnDone!!.visibility = View.VISIBLE
                    btnDone!!.setOnClickListener { v: View? ->
                        usbSerialCommunication!!.connect()
                        usbSerialCommunication!!.setBaudRate(115200)
                    }
                    Log.e("USB Communication", "Disconnected while charging.")
                }
            }, DELAY_TIME_MILLIS.toLong())
        }

        private fun checkAndRequestUsbPermission() {
            val usbManager = getSystemService(USB_SERVICE) as UsbManager
            if (usbManager == null) {
                Log.e("USB", "USB Manager is not available.")
                return
            }

            // Get connected USB devices
            val deviceList = usbManager.deviceList
            if (deviceList.isEmpty()) {
                Toast.makeText(this@MainActivity, "No USB devices connected.", Toast.LENGTH_SHORT)
                    .show()
                return
            }
            var allPermissionsGranted = true
            for (device in deviceList.values) {
                if (!usbManager.hasPermission(device)) {
                    allPermissionsGranted = false
                    showPermissionRequestUI(usbManager, device)
                    break // Stop checking further as one permission is not granted
                }
            }
            if (allPermissionsGranted) {
                registerReceiver(usbPermissionReceiver, filter)
                handlePermissionGranted()
            }
        }

        private fun showPermissionRequestUI(usbManager: UsbManager, device: UsbDevice) {
            getUsbShowState = false
            cv_error!!.visibility = View.VISIBLE
            btnStart!!.visibility = View.GONE
            btnDone!!.visibility = View.VISIBLE
            btnDone!!.text = "GRANT PERMISSION"
            tv_Message!!.text = "USB permission is not granted"
            lvAnimation!!.setAnimation(R.raw.no_usb)
            btnDone!!.setOnClickListener { v: View? -> checkAndRequestUsbPermission() }

            // Request USB permission
            val permissionIntent = PendingIntent.getBroadcast(
                this@MainActivity, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
            )
            usbManager.requestPermission(device, permissionIntent)
        }

        private fun handlePermissionGranted() {
            tv_Message!!.text = "Please wait..."
            btnDone!!.visibility = View.GONE
            lvAnimation!!.setAnimation(R.raw.please_wait)
            isUsbPermissionGranted = true
            Log.e("USB", "Permission is granted for all devices.")
            handleChargingState()
        }

        //        private void checkAndRequestUsbPermission() {
        //
        //            UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        //            if (usbManager == null) {
        //                Log.e("USB", "USB Manager is not available.");
        //                return;
        //            }
        //
        //            // Get connected USB devices
        //            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        //            if (deviceList.isEmpty()) {
        //                Toast.makeText(MainActivity.this, "No USB devices connected.", Toast.LENGTH_SHORT).show();
        //                return;
        //            }
        //
        //            boolean isPermissionGive = true;
        //
        //            for (UsbDevice device : deviceList.values()) {
        //                // Check if permission is already granted
        //                if (usbManager.hasPermission(device)) {
        //                    Log.d("USB", "Permission already granted for device: " + device.getDeviceName());
        //                  //  Toast.makeText(MainActivity.this, "Permission already granted.", Toast.LENGTH_SHORT).show();
        //
        //
        //                } else {
        //
        //                    getUsbShowState = false;
        //
        //                    isPermissionGive = false;
        //                    cv_error.setVisibility(View.VISIBLE);
        //                    btnDone.setVisibility(View.VISIBLE);
        //                    btnDone.setText("GRANT PERMISSION");
        //                    tv_Message.setText("USB permission is not granted");
        //                    lvAnimation.setAnimation(R.raw.no_electricity);
        //
        //                    btnDone.setOnClickListener(v -> {
        //                        checkAndRequestUsbPermission();
        //                    });
        //
        //                    // Request permission
        //                    PendingIntent permissionIntent = PendingIntent.getBroadcast(
        //                            MainActivity.this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
        //                    );
        //                    usbManager.requestPermission(device, permissionIntent);
        //                }
        //
        //                /// If permission is granted
        //                if(isPermissionGive){
        //                    tv_Message.setText("Please wait");
        //                    btnDone.setVisibility(View.GONE);
        //                    isUsbPermissionGranted = true;
        //                    Log.e("Here usb permission", " is granted fully");
        //                    handleChargingState();
        //                }
        //
        //            }
        //        }
        /*
         * If Battery is not in Charging State*/
        private fun handleNotChargingState() {
            Log.e("Battery Status", "Device is not charging.")
            runOnUiThread {
                updateUIForNotChargingState()
                if (cv_error!!.visibility != View.VISIBLE) {
                    Log.e("visiblity Visible", "cv_error")
                    cv_error!!.visibility = View.VISIBLE
                    btnDone!!.visibility = View.GONE
                    btnPayWithCash!!.isEnabled = false
                    btnPayWithQr!!.isEnabled = false
                    tv_Message!!.text = "No Electricity please try after some time."
                    lvAnimation!!.setAnimation(R.raw.no_electricity)
                    btnStart!!.visibility = View.GONE
                }
            }
        }

        /*Update the UI when Device is in charging state*/
        private fun updateUIForChargingState() {
            Log.e("updateUIForChargingState", "btnStart")
            btnStart!!.visibility = View.VISIBLE
            llCash!!.visibility = View.GONE
            llQr!!.visibility = View.GONE
            cvPayWithQr!!.visibility = View.GONE
            cvPayWithCash!!.visibility = View.GONE
        }

        /*
        * Buttons visibility should be gone when not in charging*/
        private fun updateUIForNotChargingState() {
            llCash!!.visibility = View.GONE
            llQr!!.visibility = View.GONE
            cvPayWithQr!!.visibility = View.GONE
            cvPayWithCash!!.visibility = View.GONE
            btnStart!!.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        keepScreenOn()
        hideSystemUI()
        setContentView(R.layout.activity_main2)
        initializeDependencies()
        initializeUI()
        setupListeners()
    }

    /*Keep Screen On*/
    private fun keepScreenOn() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /*Hide System UI*/
    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    /*Initialize
    Shared Preference,
    Usb Serial Communication,
    Register Battery Receiver Broadcast,
    SQLite Database,
    Alert Dialog of Electricity*/
    private fun initializeDependencies() {
        preferencesManager = SharedPreferencesManager.getInstance(this)
        usbSerialCommunication = UsbSerialCommunication(this)
        appDatabase = AppDatabase.getInstance(this)
        alertDialog = AlertDialog.Builder(this)
            .setTitle("No Electricity Connections")
            .setMessage("Please check again after sometime. Thank You!")
            .setCancelable(false)
            .create()
    }

    /*Initialize the ui...FInd View By Ids*/
    private fun initializeUI() {
        btnPayWithCash = findViewById(R.id.btnPayWithCash)
        btnPayWithQr = findViewById(R.id.btnPayWithQr)
        btnStart = findViewById(R.id.btnStart)
        btnDone = findViewById(R.id.btnDone)
        ivAgitator = findViewById(R.id.ivAgitator)
        ivCompressor = findViewById(R.id.ivCompressor)
        tvMilkBasePrice = findViewById(R.id.tvMilkBasePrice)
        tvTemperature = findViewById(R.id.tvTemperature)
        tv_Message = findViewById(R.id.tv_Message)
        llCash = findViewById(R.id.llPayCash)
        llQr = findViewById(R.id.llPayQR)
        lvStatus = findViewById(R.id.llStatus)
        llAlert = findViewById(R.id.llAlert)
        cv_error = findViewById(R.id.cv_error)
        lvAnimation = findViewById(R.id.lvAnimation)
        cvPayWithCash = findViewById(R.id.cvPayWithCash)
        cvPayWithQr = findViewById(R.id.cvPayWithQR)
        setupInitialVisibility()
        cv_error?.bringToFront()
    }

    private fun setupInitialVisibility() {
        llCash?.visibility = View.GONE
        llQr?.visibility = View.GONE
        cvPayWithQr?.visibility = View.GONE
        cvPayWithCash?.visibility = View.GONE
        cv_error?.visibility = View.GONE
        Log.e("visiblity Gone", "cv_error")
        lvStatus?.visibility = View.VISIBLE
    }

    /*Listeners*/
    private fun setupListeners() {
        btnStart?.setOnClickListener { v: View? -> showStartDialog() }
        btnPayWithCash?.setOnClickListener { v: View? -> onPayWithCash() }
        btnPayWithQr?.setOnClickListener { v: View? -> onPayWithQr() }
        cvPayWithCash?.setOnClickListener { v: View? -> btnPayWithCash?.performClick() }
        cvPayWithQr?.setOnClickListener { v: View? -> btnPayWithQr?.performClick() }
        usbSerialCommunication?.setReadDataListener(this)
    }

    /*Show Start Button Dialog*/
    private fun showStartDialog() {
        hideSystemUI()
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_beaker, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        val submitBtn = dialogView.findViewById<MaterialButton>(R.id.btnNext)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvTitle)
        val lottieAv = dialogView.findViewById<LottieAnimationView>(R.id.lottiAv)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvMessage)
        tvTitle.text = "Place the pot and close the door."
        lottieAv.setAnimation(R.raw.close_door)
        tvMessage.visibility = View.GONE
        submitBtn.setOnClickListener { v: View? -> handleDialogSubmit(dialog, submitBtn) }
        dialog.show()
    }

    /*
     * Handle Start button dialog's Submit Button Even*/
    private fun handleDialogSubmit(dialog: AlertDialog, submitBtn: MaterialButton) {
        if (preferencesManager!!.hasValue(Constants.ResponseTempStatus)) {
            val responseTempStatus = Gson().fromJson(
                preferencesManager!![Constants.ResponseTempStatus, ""].toString(),
                ResponseTempStatus::class.java
            )
            Log.e(
                "responseTempStatus",
                preferencesManager!![Constants.ResponseTempStatus, ""].toString()
            )
            if (responseTempStatus.connectivity != null) {
                if (!responseTempStatus.connectivity) {
                    Log.e(
                        "responseTempStatus getConnectivity",
                        responseTempStatus.connectivity.toString()
                    )
                    submitBtn.text = getString(R.string.start)
                    dialog.dismiss()
                    startUsbCommunication()
                } else {
                    submitBtn.text = getString(R.string.next)
                }
            }
        }
    }

    /*Start USB Communication*/
    private fun startUsbCommunication() {
        usbSerialCommunication!!.connect()
        usbSerialCommunication!!.setBaudRate(115200)
        if (usbSerialCommunication!!.connected) {
            llCash!!.visibility = View.VISIBLE
            llQr!!.visibility = View.VISIBLE
            cvPayWithQr!!.visibility = View.VISIBLE
            cvPayWithCash!!.visibility = View.VISIBLE
            btnStart!!.visibility = View.GONE
        }
    }

    /*Go To CashCollector Screen*/
    private fun onPayWithCash() {
        if (cv_error!!.visibility == View.VISIBLE) return
        val intent = Intent(this, CashCollectorActivity::class.java)
        startActivityForResult(intent, ScreenEnum.CASH_COLLECTOR.ordinal)
    }

    /*Go To Pay With QR Screen*/
    private fun onPayWithQr() {
        if (cv_error!!.visibility == View.VISIBLE) return
        val intent = Intent(this, PayWithQrActivity::class.java)
        startActivityForResult(intent, ScreenEnum.PAY_WITH_QR.ordinal)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.context_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_Config) {

            //    AppDatabase instance = AppDatabase.getInstance(MainActivity.this);
            Executors.newSingleThreadExecutor().execute {
                val user = appDatabase!!.userDao().getUserByUserType(0)
                if (user == null) {
                    appDatabase!!.userDao().insert(User("admin", "Mvb@idmc123", 0))
                }
            }
            // Handle edit action
            Constants.showLoginDialog(this@MainActivity, appDatabase)
            return true
        }
        return super.onContextItemSelected(item)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onStart() {
        super.onStart()
        hideSystemUI()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        registerReceiver(usbPermissionReceiver, filter)
        //        registerReceiver(usbPermissionReceiver, filter);

//        sendInitialData();
    }

    override fun onStop() {
        super.onStop()


//        unregisterReceiver(batteryReceiver);
    }

    override fun onResume() {
        super.onResume()
        //        registerReceiver(usbPermissionReceiver, filter);
        usbSerialCommunication!!.connect()
        usbSerialCommunication!!.setBaudRate(115200)

//        sendInitialData();
    }

    override fun onRestart() {
        super.onRestart()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        unregisterReceiver(usbPermissionReceiver)
        unregisterReceiver(batteryReceiver)
        usbSerialCommunication!!.disconnect()
        unregisterReceiver(usbPermissionReceiver)
        super.onDestroy()
    }

    override fun onReadData(data: String) {
        if (data == null) {
            Log.e(TAG, "run:>> onReadData: $isShowError")
            return
        }
        if (!data.contains("lowlevel")) return
        isShowError = true
        val responseTempStatus = Gson().fromJson(data, ResponseTempStatus::class.java)
        Log.e(TAG, "run: ==> onReadData: " + Gson().toJson(responseTempStatus))
        if (responseTempStatus == null) return
        runOnUiThread {
            updateTemperatureAndPrice(responseTempStatus)
            updateIndicator(ivAgitator, responseTempStatus.agitator)
            updateIndicator(ivCompressor, responseTempStatus.compressor)

            /// Here check level is normal or not when electricity is available
            if (getUsbShowState && getChargingState) {
                if (java.lang.Boolean.TRUE == responseTempStatus.lowlevel) {
                    handleLowLevel()
                } else {
                    handleNormalLevel()
                }
            } else if (!getChargingState) {
                Log.e("is not ", "charge")
                cv_error!!.visibility = View.VISIBLE
                btnDone!!.visibility = View.GONE
                tv_Message!!.text = "No Electricity please try after some time."
                lvAnimation!!.setAnimation(R.raw.no_electricity)
                btnStart!!.visibility = View.GONE
            } else if (!getUsbShowState && !isUsbPermissionGranted) {
                cv_error!!.visibility = View.VISIBLE
                btnDone!!.visibility = View.VISIBLE
                tv_Message!!.text = "USB permission is not granted"
                lvAnimation!!.setAnimation(R.raw.no_usb)
                btnStart!!.visibility = View.GONE
            } else if (!getUsbShowState && isUsbPermissionGranted) {
                cv_error!!.visibility = View.VISIBLE
                btnDone!!.visibility = View.GONE
                tv_Message!!.text = "Please wait.."
                lvAnimation!!.setAnimation(R.raw.please_wait)
                btnStart!!.visibility = View.GONE
            }
        }
    }

    private fun updateTemperatureAndPrice(responseTempStatus: ResponseTempStatus) {
        val milkBasePrice =
            "₹ " + preferencesManager!![Constants.MilkBasePrice, "0.0"].toString() + "/Ltr"
        val offsetTemp = preferencesManager!![Constants.TemperatureOffSet, "0.0"].toString()
        val cTemp =
            responseTempStatus.temperature.toString().toDouble() / 10 + offsetTemp.toDouble()
        val currentTemp = Constants.df.format(cTemp) + " °C"
        tvTemperature!!.text = currentTemp
        tvMilkBasePrice!!.text = milkBasePrice
    }

    private fun updateIndicator(imageView: ImageView?, status: Boolean?) {
        if (status != null) {
            val drawableId = if (status) R.drawable.red_circle else R.drawable.green_circle
            imageView!!.background = getDrawable(drawableId)
        }
    }

    /*If Level is low then
    * Low Milk level Screen Will be Visible */
    private fun handleLowLevel() {
        Log.e("low level", "True")
        cv_error!!.visibility = View.VISIBLE
        btnDone!!.visibility = View.GONE
        btnStart!!.visibility = View.GONE
        lvAnimation!!.setAnimation(R.raw.milk_loading)
        tv_Message!!.text = "Low Milk level. Please wait till refill."
        lvAnimation!!.repeatMode = LottieDrawable.RESTART
    }

    /*If Level is Normal then
     * Low Milk level Screen Will be Hide And Buttons Will be Visible */
    private fun handleNormalLevel() {
        Log.e("Normal level", "true")
        cv_error!!.visibility = View.GONE
        btnPayWithCash!!.visibility = View.VISIBLE
        btnPayWithQr!!.visibility = View.VISIBLE
        btnDone!!.visibility = View.GONE
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun checkChargingState(context: Context) {
        val batteryManager = context.getSystemService(BATTERY_SERVICE) as BatteryManager
        if (batteryManager != null) {
            val batteryStatus =
                batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
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
    private fun showConnectivityDialog(isConnect: Boolean) {
        if (!isConnect) {
            if (!alertDialog!!.isShowing) {
                alertDialog!!.show()
            } else {
                alertDialog!!.dismiss()
            }
        } else {
            alertDialog!!.dismiss()
        }
    }

    /// When user comes from the screen
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == ScreenEnum.CASH_COLLECTOR.ordinal || requestCode == ScreenEnum.PAY_WITH_QR.ordinal) && resultCode == RESULT_OK) {
            // Retrieve the data from the intent
            Log.e("Here I come", "in Main Activity")
            btnStart!!.visibility = View.VISIBLE
            llCash!!.visibility = View.GONE
            llQr!!.visibility = View.GONE
            cvPayWithQr!!.visibility = View.GONE
            cvPayWithCash!!.visibility = View.GONE


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

    companion object {
        private const val ACTION_USB_PERMISSION = "com.imdc.milkdespencer.USB_PERMISSION"
        private val TAG = MainActivity::class.java.simpleName
        private const val DELAY_TIME_MILLIS = 16000 // 16 seconds
        var preferencesManager: SharedPreferencesManager? = null
    }
}