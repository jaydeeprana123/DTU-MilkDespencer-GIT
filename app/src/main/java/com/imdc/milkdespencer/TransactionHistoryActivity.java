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

public class TransactionHistoryActivity extends AppCompatActivity {


    RecyclerView rvTransactions;
    TextView tvTitle;
    MaterialButton btnBackToHome;
    User user;
    private TransactionAdapter transactionAdapter;
    private LogsAdapter logsAdapter;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        // Retrieve user data from intent
        if (getIntent() != null && getIntent().hasExtra(Constants.LoginUser)) {
            user = new Gson().fromJson(getIntent().getStringExtra(Constants.LoginUser), User.class);
        }

        rvTransactions = findViewById(R.id.rvTransactions);
        tvTitle = findViewById(R.id.tvTitle);
        btnBackToHome = findViewById(R.id.btnBackToHome);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));

        btnBackToHome.setOnClickListener(view -> finish());

        // Use a background task to fetch data
        new Thread(() -> {
            AppDatabase appDatabase = AppDatabase.getInstance(TransactionHistoryActivity.this);
            Log.e("TAG", "run: " + new Gson().toJson(user));

            boolean isAdmin = user.getUserType() == UserTypeEnum.ADMIN.value() || user.getUserType() == UserTypeEnum.CUSTOMER_ADMIN.value();
            if (isAdmin) {
                updateUI("Logs", appDatabase.logDao().getAllLogs(), true);
            } else {
                updateUI("Transaction History", appDatabase.transactionDao().getAllTransactions(), false);
            }
        }).start();
    }

    // Helper method to update UI with fetched data
    private void updateUI(String title, List<?> data, boolean isLog) {
        runOnUiThread(() -> {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title);
            }

            tvTitle.setText(title);

            if (isLog) {
                logsAdapter = new LogsAdapter(TransactionHistoryActivity.this, (List<LogEntity>) data);
                rvTransactions.setAdapter(logsAdapter);
            } else {
                transactionAdapter = new TransactionAdapter(TransactionHistoryActivity.this, (List<TransactionEntity>) data);
                rvTransactions.setAdapter(transactionAdapter);

                // Add divider to the RecyclerView
                DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvTransactions.getContext(), DividerItemDecoration.VERTICAL);
                dividerItemDecoration.setDrawable(ContextCompat.getDrawable(TransactionHistoryActivity.this, R.drawable.recycler_view_divider));
                rvTransactions.addItemDecoration(dividerItemDecoration);
            }
        });
    }



}