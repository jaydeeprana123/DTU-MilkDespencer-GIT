package com.imdc.milkdespencer.adminUi

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.imdc.milkdespencer.R
import com.imdc.milkdespencer.common.Constants
import com.imdc.milkdespencer.common.SharedPreferencesManager
import com.imdc.milkdespencer.enums.UserTypeEnum
import com.imdc.milkdespencer.roomdb.AppDatabase
import com.imdc.milkdespencer.roomdb.entities.User
import java.util.regex.Pattern

//TODO: Add Phone Number Input field
class EndUserRegistrationActivity : AppCompatActivity() {
    var listOfUsers: List<User> = ArrayList()
    private var tilUsername: TextInputLayout? = null
    private var tilPassword: TextInputLayout? = null
    private var tilConfirmPassword: TextInputLayout? = null
    private var tilMachineId: TextInputLayout? = null
    private var tilStripeCustomerId: TextInputLayout? = null
    private var tilFirstName: TextInputLayout? = null
    private var tilLastName: TextInputLayout? = null
    private var tilPhoneNo: TextInputLayout? = null
    private var etUsername: EditText? = null
    private var etPassword: EditText? = null
    private var etConfirmPassword: EditText? = null
    private var etFirstName: EditText? = null
    private var etLastName: EditText? = null
    private var etMachineId: EditText? = null
    private var etStripeCustomerId: EditText? = null
    private var etPhoneNo: EditText? = null
    private var btnRegister: Button? = null
    private var appDatabase: AppDatabase? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_end_user_registration)
        preferencesManager = SharedPreferencesManager.getInstance(this@EndUserRegistrationActivity)
        tilMachineId = findViewById(R.id.tilMachineId)
        tilStripeCustomerId = findViewById(R.id.tilStripeCustomerId)
        tilFirstName = findViewById(R.id.tilFirstName)
        tilLastName = findViewById(R.id.tilLastName)
        tilUsername = findViewById(R.id.tilUsername)
        tilPassword = findViewById(R.id.tilPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        tilPhoneNo = findViewById(R.id.tilMobileNo)
        etUsername = findViewById(R.id.tieUsername)
        etPassword = findViewById(R.id.tiePassword)
        etConfirmPassword = findViewById(R.id.tieConfirmPassword)
        etMachineId = findViewById(R.id.tieMachineId)
        etStripeCustomerId = findViewById(R.id.tieStripeCustomerId)
        etFirstName = findViewById(R.id.tieFirstName)
        etLastName = findViewById(R.id.tieLastName)
        etPhoneNo = findViewById(R.id.tieMobileNo)
        btnRegister = findViewById(R.id.registerButton)
        if (supportActionBar != null) {
            supportActionBar!!.title = "Register End User"
        }
        appDatabase = AppDatabase.getInstance(this)
        endUserDataIfExist
        btnRegister?.setOnClickListener(View.OnClickListener { registerUser() })
    }

    private fun registerUser() {
        val username = etUsername!!.text.toString().trim { it <= ' ' }
        val password = etPassword!!.text.toString().trim { it <= ' ' }
        val confirmPassword = etConfirmPassword!!.text.toString().trim { it <= ' ' }
        val fName = etFirstName!!.text.toString()
        val lName = etLastName!!.text.toString()
        val machineId = etMachineId!!.text.toString()
        val customerId = etStripeCustomerId!!.text.toString()
        val phoneNo = etPhoneNo!!.text.toString()


        // Perform validation
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || machineId.isEmpty() || phoneNo.isEmpty()) {
            if (username.isEmpty()) {
                tilUsername!!.error = "Field Required"
            }
            if (machineId.isEmpty()) {
                tilMachineId!!.error = "Field Required"
            }
            if (customerId.isEmpty()) {
                tilStripeCustomerId!!.error = "Field Required"
            }
            if (password.isEmpty()) {
                tilMachineId!!.error = "Field Required"
            }
            if (confirmPassword.isEmpty()) {
                tilMachineId!!.error = "Field Required"
            }
            if (phoneNo.isEmpty()) {
                tilPhoneNo!!.error = "Field Required"
            }
            return
        }
        if (!TextUtils.isEmpty(customerId) && !Pattern.compile("cust_[A-Za-z0-9]{14}")
                .matcher(customerId).matches()
        ) {
            tilStripeCustomerId!!.error = "Please enter a valid Customer Id"
        }
        if (!TextUtils.isEmpty(username) && Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
            tilUsername!!.error = "Please enter a valid email address"
        }
        if (!TextUtils.isEmpty(phoneNo) && Patterns.PHONE.matcher(username).matches()) {
            tilUsername!!.error = "Please enter a valid Mobile No"
        }
        if (password != confirmPassword) {
            tilConfirmPassword!!.error = "Passwords do not match"
            return
        }

        // Reset error messages
        tilUsername!!.error = null
        tilPassword!!.error = null
        tilConfirmPassword!!.error = null
        Thread {
            /// Here  user type is = 1 for End user
            val user = User(
                username,
                password,
                UserTypeEnum.END_USER.value()
            ) // You can customize userType as needed
            user.first_name = fName
            user.last_name = lName
            user.mobileNo = phoneNo
            user.stripeCustomerId = customerId

            /// Convert into json string
            val userStr = Gson().toJson(user)

            /// Check that end user is available in Database.
            val existUser = appDatabase!!.userDao().getUserByUserType(UserTypeEnum.END_USER.value())

            /// If end user is exist then update the details into database
            if (existUser != null) {
                Log.e("exist user", existUser.username!!)
                appDatabase!!.userDao().update(user)
            } else {

                /// Insert into database
                val userId = appDatabase!!.userDao().insert(user)
            }

            //    String userStr =   new Gson().toJson(user);
            runOnUiThread {
                finish()

                ///Save into shared preference
                preferencesManager!!.save(Constants.MachineId, machineId)
                preferencesManager!!.save(Constants.RazorPayCustomerID, customerId)
                preferencesManager!!.save(Constants.RegisterEndUser, userStr)
                Toast.makeText(
                    this@EndUserRegistrationActivity,
                    getString(R.string.user_added_successfully),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.start()
    }

    val endUserDataIfExist: Unit
        //*If end user data is available in shared preference then get and display on edit text*/
        get() {
            appDatabase = AppDatabase.getInstance(this)
            etMachineId!!.setText(preferencesManager!![Constants.MachineId, ""].toString())
            etStripeCustomerId!!.setText(preferencesManager!![Constants.RazorPayCustomerID, ""].toString())

            ///Convert into json string
            val userStr = preferencesManager!![Constants.RegisterEndUser, ""].toString()

            /// If user data is not empty
            if (!userStr.isEmpty()) {
                val user = Gson().fromJson(userStr, User::class.java)
                Log.e("user first name", user.first_name!!)
                etFirstName!!.setText(user.first_name)
                etLastName!!.setText(user.last_name)
                etPhoneNo!!.setText(user.mobileNo)
                etUsername!!.setText(user.username)
                etPassword!!.setText(user.password)
                etConfirmPassword!!.setText(user.password)
            }
            Thread {
                //                User login = appDatabase.userDao().login("kunjankumarp507@gmail.com", "Xyz@123");
//
//                Log.e(TAG, "onClick: " + new Gson().toJson(login));

//                etFirstName.setText(login.getFirst_name());

//                listOfUsers = appDatabase.userDao().getAllUsers();
//                Log.e("length of list of users", String.valueOf(listOfUsers.size()));
//
//                Log.e("get first user in list", listOfUsers.get(0).getFirst_name());
                runOnUiThread {
                    //                        if(listOfUsers.size() > 0){
////                            etFirstName.setText(listOfUsers.get(0).getFirst_name());
//                            ///Log.e("list length " , listOfUsers)
//                        }
                }
            }.start()
        }

    companion object {
        private val TAG = EndUserRegistrationActivity::class.java.simpleName
        var preferencesManager: SharedPreferencesManager? = null
    }
}
