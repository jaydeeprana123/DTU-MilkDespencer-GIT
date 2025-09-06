package com.imdc.milkdespencer.adminUi;

import static com.imdc.milkdespencer.common.Constants.CashTransactionMode;

import static com.imdc.milkdespencer.common.Constants.GetConfigurationUrl;
import static com.imdc.milkdespencer.common.Constants.KEY_ELECTRICITY;
import static com.imdc.milkdespencer.common.Constants.KEY_KIOSK;
import static com.imdc.milkdespencer.common.Constants.KEY_LOGIN_STATUS;
import static com.imdc.milkdespencer.common.Constants.MachineId;
import static com.imdc.milkdespencer.common.Constants.MilkBasePrice;
import static com.imdc.milkdespencer.common.Constants.doPostConfigurationData;
import static com.imdc.milkdespencer.common.Constants.exportTransactionsToCSVAndShare;
import static com.imdc.milkdespencer.common.Constants.generateSafeUniqueTransactionId;
import static com.imdc.milkdespencer.common.Constants.showAddedVolumeDialog;
import static com.imdc.milkdespencer.common.Constants.showCIPRunningDialog;
import static com.imdc.milkdespencer.common.UsbSerialCommunication.isCipOn;
import static com.imdc.milkdespencer.common.UsbSerialCommunication.isSendDataStop;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
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
import com.imdc.milkdespencer.Workers.PaymentStatusService;
import com.imdc.milkdespencer.callBacks.RazorpayResponseCallback;
import com.imdc.milkdespencer.common.SharedPreferencesManager;
import com.imdc.milkdespencer.enums.UserTypeEnum;
import com.imdc.milkdespencer.adapter.UserAdapter;
import com.imdc.milkdespencer.common.Constants;
import com.imdc.milkdespencer.models.Response.RazorpayQrPaymentResponse;
import com.imdc.milkdespencer.roomdb.AppDatabase;
import com.imdc.milkdespencer.roomdb.entities.TransactionEntity;
import com.imdc.milkdespencer.roomdb.entities.User;
import com.imdc.milkdespencer.roomdb.interfaces.TransactionDao;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AdminActivity extends AppCompatActivity {
    //    private FirebaseAnalytics mFirebaseAnalytics;


    private static final String EXIT_PIN = "4432"; // TODO: store securely!
    private static final int REQUIRED_MULTI_TAPS = 5;
    SharedPreferencesManager preferencesManager;
    Button btnSetConfigurations, btnApiConfiguration, btnCIP,
            btnCustomerAdmin, btnLogs, btnCalibration, btnCashButtonOnOff,
            btnAddEndUser, btnHistoryByDate, btnExportTransactions,btnAddedVolume,btnExit;
    AppDatabase appDatabase;
    User user;
    private RecyclerView recyclerView;
    private UserAdapter userAdapter;

    private static final long MULTI_TAP_WINDOW_MS = 3000;

    private ConstraintLayout root;

    private int tapCount = 0;
    private final Handler tapWindowHandler = new Handler();
    private final Runnable resetTaps = () -> tapCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        try {
            Constants.saveLogs(getApplicationContext(), "Login Screen Accessed", KEY_LOGIN_STATUS);
        } catch (Exception e) {
           // Log.e("Login Screen", "Logging failed: ${e.message}");
            // Don't crash, just log the error silently
        }


//        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
//        // Log screen view event
//        Bundle bundle = new Bundle();
//        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, "Admin_Screen");
//        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "AdminActivity");
//        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);


        root = findViewById(R.id.root);
        btnExit= findViewById(R.id.btnExit);

        ///  For kiosk mode

        // Apply immersive sticky immediately
        enterImmersiveSticky();

        // Re-apply immersive when system UI visibility changes (e.g., swipe-in)
        root.setOnSystemUiVisibilityChangeListener(visibility -> {
            // If bars became visible, re-hide them after a tiny delay
            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                root.postDelayed(this::enterImmersiveSticky, 200);
            }
        });

        // Also re-apply when window gains focus
        // (covers cases like dialog dismiss, activity resume, etc.)
        // See onWindowFocusChanged below

        // Hidden exit triggers
        setupHiddenExitTriggers();

        // (Optional) Try to start Lock Task / Screen Pinning
//        tryStartLockTask();


        doPostConfigurationData(AdminActivity.this,GetConfigurationUrl);
        preferencesManager = SharedPreferencesManager.getInstance(getApplicationContext());
        // When user comes first time isCip should be false
        isCipOn = false;

        if (getIntent() != null) {
            if (getIntent().hasExtra(Constants.LoginUser)) {
                String loginExtra = getIntent().getStringExtra(Constants.LoginUser);
                user = new Gson().fromJson(loginExtra, User.class);
              //  Log.e("TAG", "onCreate: " + new Gson().toJson(user));
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
        btnAddedVolume = findViewById(R.id.btnAddedVolume);
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
            btnAddedVolume.setVisibility(View.VISIBLE);
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
            btnAddedVolume.setVisibility(View.VISIBLE);
        } else if (user.getUserType() == UserTypeEnum.END_USER.value()) {
            btnCIP.setVisibility(View.VISIBLE);
            btnAddedVolume.setVisibility(View.VISIBLE);
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


        btnAddedVolume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showAddedVolumeDialog(AdminActivity.this);

            }
        });

        btnCIP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

//                Log.e("btn CIP", " is pressed");
                    isCipOn = true;
                    isSendDataStop = true;
                    Intent intent = new Intent(AdminActivity.this, CIPActivity.class);
                    startActivity(intent);

//                isCipOn = true;
//                showCIPRunningDialog(AdminActivity.this);

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


    private void showPinDialog() {
        // Keep UI hidden behind the dialog as much as possible
        enterImmersiveSticky();

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("Enter PIN");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Secure Exit")
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("Unlock", (d, which) -> {
                    String pin = input.getText().toString().trim();
                    if (EXIT_PIN.equals(pin)) {
                        safeExitKiosk();
                    } else {
                        // Re-hide UI and ignore
                        enterImmersiveSticky();
                    }
                })
                .setNegativeButton("Cancel", (d, which) -> {
                    d.dismiss();
                    enterImmersiveSticky();
                })
                .create();

        // Ensure dialog itself can’t trigger soft buttons revealing too long
        if (dialog.getWindow() != null) {
            dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE);
        }
        dialog.show();
    }


    private void enterImmersiveSticky() {
        int flags =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

        root.setSystemUiVisibility(flags);
    }


    private void setupHiddenExitTriggers() {
        // Long-press on hotspot opens PIN dialog

        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPinDialog();
            }
        });
    }

    private void registerTap() {
        tapCount++;
        tapWindowHandler.removeCallbacks(resetTaps);
        tapWindowHandler.postDelayed(resetTaps, MULTI_TAP_WINDOW_MS);
        if (tapCount >= REQUIRED_MULTI_TAPS) {
            tapCount = 0;
            showPinDialog();
        }
    }


//    private void safeExitKiosk() {
//        // Stop Lock Task if running
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            try { stopLockTask(); } catch (Exception ignored) {}
//        }
//        finishAffinity();   // closes all activities
//        System.exit(0);   // or navigate to an admin screen
//    }


    private void safeExitKiosk() {
        // Stop Lock Task if running
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                stopLockTask();
            } catch (Exception ignored) {}
        }

        // Restore system UI (show back, home, status bar)
        root.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_VISIBLE
        );


        try {
            Constants.saveLogs(getApplicationContext(), "Exit Kisok Mode", KEY_KIOSK);
        } catch (Exception e) {
//            logError("PaymentLog", "Logging failed: ${e.message}");
            // Don't crash, just log the error silently
        }

        // Optionally finish activity if you want to exit app
        // finish();
    }


    private void tryStartLockTask() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                // If your app is device owner or whitelisted, this will silently start.
                // Otherwise Android will show the “Start screen pinning?” prompt to the user.
                startLockTask();
            } catch (Exception ignored) { }
        }
    }


    @Override
    public void onBackPressed() {
        // Block back in kiosk mode
        // super.onBackPressed(); // Intentionally disabled
    }

}