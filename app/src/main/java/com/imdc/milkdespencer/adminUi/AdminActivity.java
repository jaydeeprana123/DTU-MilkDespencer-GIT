package com.imdc.milkdespencer.adminUi;

import static com.imdc.milkdespencer.common.Constants.CashTransactionMode;
import static com.imdc.milkdespencer.common.Constants.RazorPayCustomerID;
import static com.imdc.milkdespencer.common.Constants.doGetConfigurationData;
import static com.imdc.milkdespencer.common.Constants.exportTransactionsToCSVAndShare;
import static com.imdc.milkdespencer.common.Constants.sendEmailWithAttachment;
import static com.imdc.milkdespencer.common.Constants.showCIPRunningDialog;
import static com.imdc.milkdespencer.common.UsbSerialCommunication.isCipOn;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.imdc.milkdespencer.MainActivity;
import com.imdc.milkdespencer.PayWithQrActivity;
import com.imdc.milkdespencer.R;
import com.imdc.milkdespencer.TransactionHistoryActivity;
import com.imdc.milkdespencer.TransactionHistoryByDateActivity;
import com.imdc.milkdespencer.common.SharedPreferencesManager;
import com.imdc.milkdespencer.enums.UserTypeEnum;
import com.imdc.milkdespencer.adapter.UserAdapter;
import com.imdc.milkdespencer.common.Constants;
import com.imdc.milkdespencer.roomdb.AppDatabase;
import com.imdc.milkdespencer.roomdb.entities.TransactionEntity;
import com.imdc.milkdespencer.roomdb.entities.User;

import java.util.List;

public class AdminActivity extends AppCompatActivity {
    //    private FirebaseAnalytics mFirebaseAnalytics;
    SharedPreferencesManager preferencesManager;
    Button btnSetConfigurations, btnApiConfiguration, btnCIP,
            btnCustomerAdmin, btnLogs, btnCalibration, btnCashButtonOnOff,
            btnAddEndUser, btnHistoryByDate, btnExportTransactions;
    AppDatabase appDatabase;
    User user;
    private RecyclerView recyclerView;
    private UserAdapter userAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
//        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
//        // Log screen view event
//        Bundle bundle = new Bundle();
//        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, "Admin_Screen");
//        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "AdminActivity");
//        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);

        doGetConfigurationData(AdminActivity.this);
        preferencesManager = SharedPreferencesManager.getInstance(this);
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
                    } else if (user.getUserType() == UserTypeEnum.CUSTOMER_ADMIN.value()) {
                        getSupportActionBar().setTitle("Customer Admin Panel");
                    } else if (user.getUserType() == UserTypeEnum.END_USER.value()) {
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
        btnHistoryByDate = findViewById(R.id.btnHistoryByDate);
        btnExportTransactions = findViewById(R.id.btnExportTransactions);
        btnCalibration = findViewById(R.id.btnCalibration);
        btnCashButtonOnOff = findViewById(R.id.btnCashButtonOnOff);


        if (preferencesManager.get(CashTransactionMode, "0").equals("0")) {
            btnCashButtonOnOff.setText(getResources().getString(R.string.btnCashOff));
        } else {
            btnCashButtonOnOff.setText(getResources().getString(R.string.btnCashOnO));
        }


        if (user.getUserType() == UserTypeEnum.ADMIN.value()) {
            btnLogs.setText("Show Logs");
            btnCalibration.setVisibility(View.VISIBLE);
            btnCashButtonOnOff.setVisibility(View.VISIBLE);
            btnCustomerAdmin.setVisibility(View.VISIBLE);
            btnAddEndUser.setVisibility(View.VISIBLE);
            btnApiConfiguration.setVisibility(View.VISIBLE);
            btnCIP.setVisibility(View.GONE);
            btnHistoryByDate.setVisibility(View.GONE);
            btnExportTransactions.setVisibility(View.GONE);
        } else if (user.getUserType() == UserTypeEnum.CUSTOMER_ADMIN.value()) {

            btnLogs.setText("Show Logs");
            btnCalibration.setVisibility(View.VISIBLE);
            btnCashButtonOnOff.setVisibility(View.VISIBLE);
            btnCustomerAdmin.setVisibility(View.GONE);
            btnAddEndUser.setVisibility(View.VISIBLE);
            btnApiConfiguration.setVisibility(View.GONE);
            btnCIP.setVisibility(View.GONE);
            btnHistoryByDate.setVisibility(View.GONE);
            btnExportTransactions.setVisibility(View.GONE);
        } else if (user.getUserType() == UserTypeEnum.END_USER.value()) {
            btnCIP.setVisibility(View.VISIBLE);
            btnSetConfigurations.setText("View Configurations");
            btnLogs.setText("Show Transactions");
            btnHistoryByDate.setVisibility(View.VISIBLE);
            btnCalibration.setVisibility(View.GONE);
            btnCashButtonOnOff.setVisibility(View.GONE);
            btnCustomerAdmin.setVisibility(View.GONE);
            btnAddEndUser.setVisibility(View.GONE);
            btnApiConfiguration.setVisibility(View.GONE);
            btnExportTransactions.setVisibility(View.VISIBLE);
        }

        btnCIP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.e("btn CIP", " is pressed");
                isCipOn = true;
                showCIPRunningDialog(AdminActivity.this);

            }
        });


        btnCashButtonOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (preferencesManager.get(CashTransactionMode, "0").equals("0")) {

                    preferencesManager.save(CashTransactionMode, "1");
                    btnCashButtonOnOff.setText(getResources().getString(R.string.btnCashOnO));
                } else {
                    preferencesManager.save(CashTransactionMode, "0");
                    btnCashButtonOnOff.setText(getResources().getString(R.string.btnCashOff));
                }

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

        btnHistoryByDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(AdminActivity.this, TransactionHistoryByDateActivity.class);
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
                        Constants.showAdminConfigDialog(AdminActivity.this, 0);
                    } else if (user.getUserType() == UserTypeEnum.CUSTOMER_ADMIN.value()) {
                        Constants.showAdminConfigDialog(AdminActivity.this, 2);
                    } else if (user.getUserType() == UserTypeEnum.END_USER.value()) {
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

        btnExportTransactions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                if (ContextCompat.checkSelfPermission(AdminActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                        != PackageManager.PERMISSION_GRANTED) {
//                    ActivityCompat.requestPermissions(AdminActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
//                }


                new Thread(() -> {
                    List<TransactionEntity> transactions = AppDatabase.getInstance(AdminActivity.this).transactionDao().getAllTransactions();

                    // Run export on main thread
                    new Handler(Looper.getMainLooper()).post(() -> {
                        exportTransactionsToCSVAndShare(AdminActivity.this, transactions);
                    });
                }).start();


//                String path = Constants.exportUsersToCSV(AdminActivity.this, AppDatabase.getInstance(AdminActivity.this));
//
//
//                Log.e("path", path);
//
//                if (path != null) {
//                   // sendEmailWithAttachment(AdminActivity.this, path);
//                }

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