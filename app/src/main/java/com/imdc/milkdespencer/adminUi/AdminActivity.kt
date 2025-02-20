package com.imdc.milkdespencer.adminUi

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.imdc.milkdespencer.R
import com.imdc.milkdespencer.TransactionHistoryActivity
import com.imdc.milkdespencer.adapter.UserAdapter
import com.imdc.milkdespencer.common.Constants
import com.imdc.milkdespencer.common.UsbSerialCommunication
import com.imdc.milkdespencer.enums.UserTypeEnum
import com.imdc.milkdespencer.roomdb.AppDatabase
import com.imdc.milkdespencer.roomdb.entities.User

class AdminActivity : AppCompatActivity() {
    lateinit var btnSetConfigurations: Button
    lateinit var btnApiConfiguration: Button
    lateinit  var btnCIP: Button
    lateinit var btnCustomerAdmin: Button
    lateinit var btnLogs: Button
   lateinit var btnCalibration: Button
    lateinit var btnAddEndUser: Button
    var appDatabase: AppDatabase? = null
    var user: User = User()
    lateinit var recyclerView: RecyclerView
    private val userAdapter: UserAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)
        Constants.doGetConfigurationData(this@AdminActivity)

        // When user comes first time isCip should be false
        UsbSerialCommunication.isCipOn = false
        if (intent != null) {
            if (intent.hasExtra(Constants.LoginUser)) {
                val loginExtra = intent.getStringExtra(Constants.LoginUser)
                user = Gson().fromJson(loginExtra, User::class.java)
                Log.e("TAG", "onCreate: " + Gson().toJson(user))
                if (supportActionBar != null) {
                    if (user.userType == UserTypeEnum.ADMIN.value()) {
                        supportActionBar!!.setTitle("Admin Panel")
                    } else if (user.userType == UserTypeEnum.CUSTOMER_ADMIN.value()) {
                        supportActionBar!!.setTitle("Customer Admin Panel")
                    } else if (user.userType == UserTypeEnum.END_USER.value()) {
                        supportActionBar!!.setTitle("User Panel")
                    }
                }
            }
        }
        appDatabase = AppDatabase.getInstance(this)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.setLayoutManager(LinearLayoutManager(this)) // Set the number of columns as needed
        btnSetConfigurations = findViewById(R.id.btnSetConfiguration)
        btnApiConfiguration = findViewById(R.id.btnApiConfiguration)
        btnCIP = findViewById(R.id.btnCIP)
        btnCustomerAdmin = findViewById(R.id.btnAddUser)
        btnAddEndUser = findViewById(R.id.btnAddEndUser)
        btnLogs = findViewById(R.id.btnLogs)
        btnCalibration = findViewById(R.id.btnCalibration)
        if (user!!.userType == UserTypeEnum.ADMIN.value()) {
            btnLogs.setText("Show Logs")
            btnCalibration.setVisibility(View.VISIBLE)
            btnCustomerAdmin.setVisibility(View.VISIBLE)
            btnAddEndUser.setVisibility(View.VISIBLE)
            btnApiConfiguration.setVisibility(View.VISIBLE)
            btnCIP.setVisibility(View.GONE)
        } else if (user!!.userType == UserTypeEnum.CUSTOMER_ADMIN.value()) {
            btnLogs.setText("Show Logs")
            btnCalibration.setVisibility(View.VISIBLE)
            btnCustomerAdmin.setVisibility(View.GONE)
            btnAddEndUser.setVisibility(View.VISIBLE)
            btnApiConfiguration.setVisibility(View.GONE)
            btnCIP.setVisibility(View.GONE)
        } else if (user!!.userType == UserTypeEnum.END_USER.value()) {
            btnCIP.setVisibility(View.VISIBLE)
            btnSetConfigurations.setText("View Configurations")
            btnLogs.setText("Show Transactions")
            btnCalibration.setVisibility(View.GONE)
            btnCustomerAdmin.setVisibility(View.GONE)
            btnAddEndUser.setVisibility(View.GONE)
            btnApiConfiguration.setVisibility(View.GONE)
        }
        btnCIP.setOnClickListener(View.OnClickListener {
            Log.e("btn CIP", " is pressed")
            UsbSerialCommunication.isCipOn = true
            Constants.showCIPRunningDialog(this@AdminActivity)
        })
        btnCalibration.setOnClickListener(View.OnClickListener {
            val intent = Intent(this@AdminActivity, CalibrationActivity::class.java)
            startActivity(intent)
        })
        btnLogs.setOnClickListener(View.OnClickListener {
            val intent = Intent(this@AdminActivity, TransactionHistoryActivity::class.java)
            intent.putExtra(Constants.LoginUser, Gson().toJson(user))
            startActivity(intent)
        })


        /*
        new Thread(() -> {
            List<User> userList = appDatabase.userDao().getAllUsers();
            userAdapter = new UserAdapter(userList);
            runOnUiThread(() -> recyclerView.setAdapter(userAdapter));
        }).start();
*/btnCustomerAdmin.setOnClickListener(View.OnClickListener {
            val intent = Intent(this@AdminActivity, CustomerAdminRegistrationActivity::class.java)
            startActivity(intent)
        })
        btnAddEndUser.setOnClickListener(View.OnClickListener {
            val intent = Intent(this@AdminActivity, EndUserRegistrationActivity::class.java)
            startActivity(intent)
        })
        btnSetConfigurations.setOnClickListener(View.OnClickListener {
            runOnUiThread {
                if (user!!.userType == UserTypeEnum.ADMIN.value()) {
                    Constants.showAdminConfigDialog(this@AdminActivity, 0)
                } else if (user!!.userType == UserTypeEnum.CUSTOMER_ADMIN.value()) {
                    Constants.showAdminConfigDialog(this@AdminActivity, 2)
                } else if (user!!.userType == UserTypeEnum.END_USER.value()) {
                    Constants.showAdminConfigDialog(this@AdminActivity, 1)
                }
            }
        })
        btnApiConfiguration.setOnClickListener(View.OnClickListener {
            runOnUiThread {
                if (user!!.userType == UserTypeEnum.ADMIN.value()) {
                    Constants.showAPIConfigDialog(this@AdminActivity)
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_admin, menu)
        val menuItem = menu.findItem(R.id.action_logout)
        menuItem.setTitle(" LOGOUT")
        val icon = menuItem.icon
        if (icon != null) {
            icon.mutate() // Ensure the drawable is mutable
            icon.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_logout) {
            finish()
            //            Intent intent = new Intent(AdminActivity.this, MainActivity.class);
//            startActivity(intent);
//            finish();
        }
        return super.onOptionsItemSelected(item)
    }
}