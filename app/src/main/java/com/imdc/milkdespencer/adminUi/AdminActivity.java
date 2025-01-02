package com.imdc.milkdespencer.adminUi;

import static com.imdc.milkdespencer.common.Constants.showCIPRunningDialog;
import static com.imdc.milkdespencer.common.UsbSerialCommunication.isCipOn;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.imdc.milkdespencer.MainActivity;
import com.imdc.milkdespencer.R;
import com.imdc.milkdespencer.TransactionHistoryActivity;
import com.imdc.milkdespencer.enums.UserTypeEnum;
import com.imdc.milkdespencer.adapter.UserAdapter;
import com.imdc.milkdespencer.common.Constants;
import com.imdc.milkdespencer.roomdb.AppDatabase;
import com.imdc.milkdespencer.roomdb.entities.User;

public class AdminActivity extends AppCompatActivity {


    Button btnSetConfigurations,btnApiConfiguration,btnCIP, btnCustomerAdmin, btnLogs, btnCalibration, btnAddEndUser;
    AppDatabase appDatabase;
    User user;
    private RecyclerView recyclerView;
    private UserAdapter userAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // When user comes first time isCip should be false
        isCipOn = false;

        if (getIntent() != null) {
            if (getIntent().hasExtra(Constants.LoginUser)) {
                String loginExtra = getIntent().getStringExtra(Constants.LoginUser);
                user = new Gson().fromJson(loginExtra, User.class);
                Log.e("TAG", "onCreate: " + new Gson().toJson(user));
                if (getSupportActionBar() != null) {
                    if (user.getUserType() == UserTypeEnum.ADMIN.value()) {
                        getSupportActionBar().setTitle("Admin Panel");
                    }else if (user.getUserType() == UserTypeEnum.CUSTOMER_ADMIN.value()) {
                        getSupportActionBar().setTitle("Customer Admin Panel");
                    } else if (user.getUserType() == UserTypeEnum.END_USER.value()){
                        getSupportActionBar().setTitle("User Panel");
                    }
                }
            }
        }

        appDatabase = AppDatabase.getInstance(this);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this)); // Set the number of columns as needed

        btnSetConfigurations = findViewById(R.id.btnSetConfiguration);
        btnApiConfiguration = findViewById(R.id.btnApiConfiguration);
        btnCIP = findViewById(R.id.btnCIP);
        btnCustomerAdmin = findViewById(R.id.btnAddUser);
        btnAddEndUser = findViewById(R.id.btnAddEndUser);
        btnLogs = findViewById(R.id.btnLogs);
        btnCalibration = findViewById(R.id.btnCalibration);

        if (user.getUserType() == UserTypeEnum.ADMIN.value()) {
            btnLogs.setText("Show Logs");
            btnCalibration.setVisibility(View.VISIBLE);
            btnCustomerAdmin.setVisibility(View.VISIBLE);
            btnAddEndUser.setVisibility(View.VISIBLE);
            btnApiConfiguration.setVisibility(View.VISIBLE);
            btnCIP.setVisibility(View.GONE);
        }else if(user.getUserType() == UserTypeEnum.CUSTOMER_ADMIN.value()){

            btnLogs.setText("Show Logs");
            btnCalibration.setVisibility(View.VISIBLE);
            btnCustomerAdmin.setVisibility(View.GONE);
            btnAddEndUser.setVisibility(View.VISIBLE);
            btnApiConfiguration.setVisibility(View.GONE);
            btnCIP.setVisibility(View.GONE);
        }

        else if(user.getUserType() == UserTypeEnum.END_USER.value()){
            btnCIP.setVisibility(View.VISIBLE);
            btnSetConfigurations.setText("View Configurations");
            btnLogs.setText("Show Transactions");
            btnCalibration.setVisibility(View.GONE);
            btnCustomerAdmin.setVisibility(View.GONE);
            btnAddEndUser.setVisibility(View.GONE);
            btnApiConfiguration.setVisibility(View.GONE);
        }

        btnCIP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.e("btn CIP"," is pressed");
                isCipOn = true;
                showCIPRunningDialog(AdminActivity.this);

            }
        });


        btnCalibration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminActivity.this, CalibrationActivity.class);
                startActivity(intent);
            }
        });



        btnLogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminActivity.this, TransactionHistoryActivity.class);
                intent.putExtra(Constants.LoginUser, new Gson().toJson(user));
                startActivity(intent);
            }
        });


/*
        new Thread(() -> {
            List<User> userList = appDatabase.userDao().getAllUsers();
            userAdapter = new UserAdapter(userList);
            runOnUiThread(() -> recyclerView.setAdapter(userAdapter));
        }).start();
*/

        btnCustomerAdmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminActivity.this, CustomerAdminRegistrationActivity.class);
                startActivity(intent);
            }
        });


        btnAddEndUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminActivity.this, EndUserRegistrationActivity.class);
                startActivity(intent);
            }
        });

        btnSetConfigurations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runOnUiThread(() -> {
                    if (user.getUserType() == UserTypeEnum.ADMIN.value()) {
                        Constants.showAdminConfigDialog(AdminActivity.this,0);
                    }else if (user.getUserType() == UserTypeEnum.CUSTOMER_ADMIN.value()) {
                        Constants.showAdminConfigDialog(AdminActivity.this, 2);
                    } else if (user.getUserType() == UserTypeEnum.END_USER.value()){
                        Constants.showAdminConfigDialog(AdminActivity.this, 1);
                    }
                });
            }
        });

        btnApiConfiguration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                runOnUiThread(() -> {
                    if (user.getUserType() == UserTypeEnum.ADMIN.value()) {
                        Constants.showAPIConfigDialog(AdminActivity.this);
                    }
                });
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_admin, menu);

        MenuItem menuItem = menu.findItem(R.id.action_logout);
        menuItem.setTitle(" LOGOUT");

        Drawable icon = menuItem.getIcon();
        if (icon != null) {
            icon.mutate(); // Ensure the drawable is mutable
            icon.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN);
        }


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.action_logout) {
            finish();
//            Intent intent = new Intent(AdminActivity.this, MainActivity.class);
//            startActivity(intent);
//            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}