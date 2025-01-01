package com.imdc.milkdespencer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.imdc.milkdespencer.adapter.LogsAdapter;
import com.imdc.milkdespencer.adapter.TransactionAdapter;
import com.imdc.milkdespencer.common.Constants;
import com.imdc.milkdespencer.models.SendToDevice;
import com.imdc.milkdespencer.roomdb.AppDatabase;
import com.imdc.milkdespencer.roomdb.entities.LogEntity;
import com.imdc.milkdespencer.roomdb.entities.TransactionEntity;
import com.imdc.milkdespencer.roomdb.entities.User;
import com.imdc.milkdespencer.roomdb.interfaces.TransactionDao;

import java.text.SimpleDateFormat;
import java.util.List;

public class TransactionHistoryActivity extends AppCompatActivity {


    RecyclerView rvTransactions;
    TextView tvTitle;
    MaterialButton btnBackToHome;
    User user;
    private TransactionAdapter transactionAdapter;
    private LogsAdapter logsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        if (getIntent() != null) {
            if (getIntent().hasExtra(Constants.LoginUser)) {
                user = new Gson().fromJson(getIntent().getStringExtra(Constants.LoginUser), User.class);
            }
        }

        rvTransactions = findViewById(R.id.rvTransactions);
        tvTitle = findViewById(R.id.tvTitle);
        btnBackToHome = findViewById(R.id.btnBackToHome);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        btnBackToHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                AppDatabase appDatabase = AppDatabase.getInstance(TransactionHistoryActivity.this);
                Log.e("TAG", "run: " + new Gson().toJson(user));
                if (user.getUserType() == 0) {
                    if (getActionBar() != null) {
                        getActionBar().setTitle("Logs");
                    }
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle("Logs");
                    }
                    tvTitle.setText("Logs");
                    List<LogEntity> logs = appDatabase.logDao().getAllLogs();
                    Log.e("TAG", "run:getAllLogs " + new Gson().toJson(logs));
                    logsAdapter = new LogsAdapter(TransactionHistoryActivity.this, logs);
                    rvTransactions.setAdapter(logsAdapter);
                } else {
                    if (getActionBar() != null) {
                        getActionBar().setTitle("Transaction History");
                    }
                    tvTitle.setText("Transaction History");
                    List<TransactionEntity> transactions = appDatabase.transactionDao().getAllTransactions();
                    Log.e("TAG", "run:getAllTransactions " + new Gson().toJson(transactions));
                    transactionAdapter = new TransactionAdapter(TransactionHistoryActivity.this, transactions);
                    // Add divider to the RecyclerView
                    DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvTransactions.getContext(), DividerItemDecoration.VERTICAL);
                    dividerItemDecoration.setDrawable(ContextCompat.getDrawable(TransactionHistoryActivity.this, R.drawable.recycler_view_divider));
                    rvTransactions.addItemDecoration(dividerItemDecoration);
                    rvTransactions.setAdapter(transactionAdapter);
                }
            }
        }).start();

    }


}