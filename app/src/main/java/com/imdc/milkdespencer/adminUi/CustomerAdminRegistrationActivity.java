package com.imdc.milkdespencer.adminUi;

import static com.imdc.milkdespencer.common.Constants.MachineId;
import static com.imdc.milkdespencer.common.Constants.RazorPayCustomerID;
import static com.imdc.milkdespencer.common.Constants.RegisterCustomerAdmin;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.imdc.milkdespencer.R;
import com.imdc.milkdespencer.enums.UserTypeEnum;
import com.imdc.milkdespencer.common.SharedPreferencesManager;
import com.imdc.milkdespencer.roomdb.AppDatabase;
import com.imdc.milkdespencer.roomdb.entities.User;

import java.util.regex.Pattern;


//TODO: Add Phone Number Input field
public class CustomerAdminRegistrationActivity extends AppCompatActivity {

    private static final String TAG = CustomerAdminRegistrationActivity.class.getSimpleName();
    static SharedPreferencesManager preferencesManager;
    private TextInputLayout tilUsername, tilPassword, tilConfirmPassword, tilMachineId, tilStripeCustomerId, tilFirstName, tilLastName, tilPhoneNo;
    private EditText etUsername, etPassword, etConfirmPassword, etFirstName, etLastName, etMachineId, etStripeCustomerId, etPhoneNo;
    private Button btnRegister;
    private AppDatabase appDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        preferencesManager = SharedPreferencesManager.getInstance(CustomerAdminRegistrationActivity.this);
        tilMachineId = findViewById(R.id.tilMachineId);
        tilStripeCustomerId = findViewById(R.id.tilStripeCustomerId);
        tilFirstName = findViewById(R.id.tilFirstName);
        tilLastName = findViewById(R.id.tilLastName);
        tilUsername = findViewById(R.id.tilUsername);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        tilPhoneNo = findViewById(R.id.tilMobileNo);

        etUsername = findViewById(R.id.tieUsername);
        etPassword = findViewById(R.id.tiePassword);
        etConfirmPassword = findViewById(R.id.tieConfirmPassword);

        etMachineId = findViewById(R.id.tieMachineId);
        etStripeCustomerId = findViewById(R.id.tieStripeCustomerId);
        etFirstName = findViewById(R.id.tieFirstName);
        etLastName = findViewById(R.id.tieLastName);
        etPhoneNo = findViewById(R.id.tieMobileNo);

        btnRegister = findViewById(R.id.registerButton);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Register Customer Admin");
        }

        appDatabase = AppDatabase.getInstance(this);

        getRegisterUserData();

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerUser();
            }
        });
    }

    private void registerUser() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        String fName = etFirstName.getText().toString();
        String lName = etLastName.getText().toString();
        String machineId = etMachineId.getText().toString();
        String customerId = etStripeCustomerId.getText().toString();
        String phoneNo = etPhoneNo.getText().toString();

        // Perform validation
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || machineId.isEmpty() || phoneNo.isEmpty()) {

            if (username.isEmpty()) {
                tilUsername.setError("Field Required");
            }
            if (machineId.isEmpty()) {
                tilMachineId.setError("Field Required");
            }
            if (customerId.isEmpty()) {
                tilStripeCustomerId.setError("Field Required");
            }
            if (password.isEmpty()) {
                tilMachineId.setError("Field Required");
            }
            if (confirmPassword.isEmpty()) {
                tilMachineId.setError("Field Required");
            }
            if (phoneNo.isEmpty()) {
                tilPhoneNo.setError("Field Required");
            }
            return;
        }

        if (!TextUtils.isEmpty(customerId) && !Pattern.compile("cust_[A-Za-z0-9]{14}").matcher(customerId).matches()) {
            tilStripeCustomerId.setError("Please enter a valid Customer Id");

        }

        if (!TextUtils.isEmpty(username) && Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
            tilUsername.setError("Please enter a valid email address");
        }
        if (!TextUtils.isEmpty(phoneNo) && Patterns.PHONE.matcher(username).matches()) {
            tilUsername.setError("Please enter a valid Mobile No");
        }
        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Passwords do not match");
            return;
        }

        // Reset error messages
        tilUsername.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        // Insert user into the database
        new Thread(new Runnable() {
            @Override
            public void run() {

                /// Make Model
                /// Here  user type is = 2 for customer admin
                User user = new User(username, password, UserTypeEnum.CUSTOMER_ADMIN.value()); // You can customize userType as needed
                user.setFirst_name(fName);
                user.setLast_name(lName);
                user.setMobileNo(phoneNo);
                user.setStripeCustomerId(customerId);

                /// Convert into json string
                String userStr =   new Gson().toJson(user);


                /// Check that Customer Admin is available in Database.
                User existUser = appDatabase.userDao().getUserByUserType(UserTypeEnum.CUSTOMER_ADMIN.value());

                /// If end user is exist then update the details into database
                if(existUser != null){
                    Log.e("exist customer admin", existUser.getUsername());
                    appDatabase.userDao().update(user);
                }else{

                    /// Insert into database
                    long userId = appDatabase.userDao().insert(user);
                }


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        finish();

                        ///Save into shared preference
                        preferencesManager.save(MachineId, machineId);
                        preferencesManager.save(RazorPayCustomerID, customerId);
                        preferencesManager.save(RegisterCustomerAdmin, userStr);

                        Toast.makeText(CustomerAdminRegistrationActivity.this, getString(R.string.user_added_successfully), Toast.LENGTH_SHORT).show();
                    }
                });

            }
        }).start();


    }



    void getRegisterUserData(){
        appDatabase = AppDatabase.getInstance(this);
        etMachineId.setText(preferencesManager.get(MachineId, "").toString());;

        Log.e("Machine Id jhuadhf oyio iu io", etMachineId.getText().toString());
        etStripeCustomerId.setText(preferencesManager.get(RazorPayCustomerID, "").toString());
        String userStr = preferencesManager.get(RegisterCustomerAdmin,"").toString();
        Log.e("userStr jhuadhf oyio iu io", userStr);

        if(!userStr.isEmpty()){
            User user = new Gson().fromJson(userStr, User.class);
            Log.e("user first name", user.getFirst_name());
            etFirstName.setText(user.getFirst_name());
            etLastName.setText(user.getLast_name());
            etPhoneNo.setText(user.getMobileNo());
            etUsername.setText(user.getUsername());
            etPassword.setText(user.getPassword());
            etConfirmPassword.setText(user.getPassword());
        }


        new Thread(new Runnable() {
            @Override
            public void run() {

             //   User login = appDatabase.userDao().login("kunjankumarp507@gmail.com", "Xyz@123");

            //    Log.e(TAG, "onClick: " + new Gson().toJson(login));

//                etFirstName.setText(login.getFirst_name());


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {


                    }
                });




            }
        }).start();



    }


}

