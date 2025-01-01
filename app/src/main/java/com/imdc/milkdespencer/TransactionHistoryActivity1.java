package com.imdc.milkdespencer;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.imdc.milkdespencer.adapter.LogsAdapter;
import com.imdc.milkdespencer.adapter.TransactionAdapter;
import com.imdc.milkdespencer.common.Constants;
import com.imdc.milkdespencer.enums.UserTypeEnum;
import com.imdc.milkdespencer.roomdb.AppDatabase;
import com.imdc.milkdespencer.roomdb.entities.LogEntity;
import com.imdc.milkdespencer.roomdb.entities.TransactionEntity;
import com.imdc.milkdespencer.roomdb.entities.User;

import java.util.List;

public class TransactionHistoryActivity1 extends AppCompatActivity {


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
                AppDatabase appDatabase = AppDatabase.getInstance(TransactionHistoryActivity1.this);
                Log.e("TAG", "run: " + new Gson().toJson(user));
                if (user.getUserType() == UserTypeEnum.ADMIN.value() || user.getUserType() == UserTypeEnum.CUSTOMER_ADMIN.value()) {
                    if (getActionBar() != null) {
                        getActionBar().setTitle("Logs");
                    }
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle("Logs");
                    }
                    tvTitle.setText("Logs");
                    List<LogEntity> logs = appDatabase.logDao().getAllLogs();
                    Log.e("TAG", "run:getAllLogs " + new Gson().toJson(logs));
                    logsAdapter = new LogsAdapter(TransactionHistoryActivity1.this, logs);
                    rvTransactions.setAdapter(logsAdapter);
                } else {
                    if (getActionBar() != null) {
                        getActionBar().setTitle("Transaction History");
                    }
                    tvTitle.setText("Transaction History");
                    List<TransactionEntity> transactions = appDatabase.transactionDao().getAllTransactions();
                    Log.e("TAG", "run:getAllTransactions " + new Gson().toJson(transactions));
                    transactionAdapter = new TransactionAdapter(TransactionHistoryActivity1.this, transactions);
                    // Add divider to the RecyclerView
                    DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvTransactions.getContext(), DividerItemDecoration.VERTICAL);
                    dividerItemDecoration.setDrawable(ContextCompat.getDrawable(TransactionHistoryActivity1.this, R.drawable.recycler_view_divider));
                    rvTransactions.addItemDecoration(dividerItemDecoration);
                    rvTransactions.setAdapter(transactionAdapter);
                }
            }
        }).start();

    }


}