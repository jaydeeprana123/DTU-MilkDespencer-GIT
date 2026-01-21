package com.imdc.milkdespencer;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionHistoryActivity extends AppCompatActivity {

    RecyclerView rvTransactions;
    TextView tvTitle;

    TextView tvTodayTotalAmount, tvTodayTotalVolume;


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
        tvTodayTotalAmount= findViewById(R.id.tvTodayTotalAmount);
        tvTodayTotalVolume= findViewById(R.id.tvTodayTotalVolume);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));

        btnBackToHome.setOnClickListener(view -> finish());

        // Use a background task to fetch data
        new Thread(() -> {
            AppDatabase appDatabase = AppDatabase.getInstance(TransactionHistoryActivity.this);
            Log.e("TAG", "run: " + new Gson().toJson(user));

            boolean isAdmin = user.getUserType() == UserTypeEnum.ADMIN.value()
                    || user.getUserType() == UserTypeEnum.CUSTOMER_ADMIN.value();

            if (isAdmin) {
                List<LogEntity> logs = appDatabase.logDao().getAllLogs();

                runOnUiThread(() -> {
                    updateUI("Logs", logs, true);
                    tvTodayTotalAmount.setVisibility(View.GONE);
                    tvTodayTotalVolume.setVisibility(View.GONE);
                });

            } else {
                List<TransactionEntity> transactions = appDatabase.transactionDao().getAllTransactions();

                // Get Today's date
                String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                double totalAmount = appDatabase.transactionDao().getTodayTotalAmount(todayDate);
                float totalVolume = appDatabase.transactionDao().getTodayVolumeSum(todayDate);

                totalAmount = Double.parseDouble(String.format("%.2f", totalAmount));
                totalVolume = Float.parseFloat(String.format("%.2f", totalVolume));

//                Log.e("totalAmount", String.valueOf(totalAmount));
//                Log.e("totalVolume", String.valueOf(totalVolume));

                float finalTotalVolume = totalVolume;
                double finalTotalAmount = totalAmount;
                runOnUiThread(() -> {
                    updateUI("Transaction Summary", transactions, false);
                    tvTodayTotalAmount.setVisibility(View.VISIBLE);
                    tvTodayTotalVolume.setVisibility(View.VISIBLE);
                    tvTodayTotalAmount.setText("Today's Summary     ₹" + finalTotalAmount);
                    tvTodayTotalVolume.setText(finalTotalVolume + "L");
                });
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_admin, menu);

        MenuItem menuItem = menu.findItem(R.id.action_logout);
        menuItem.setTitle(" BACK");
        menuItem.setIcon(R.drawable.ic_arrow_back); // Replace with your desired drawable

        Drawable icon = menuItem.getIcon();
        if (icon != null) {
            icon.mutate(); // Ensure the drawable is mutable

            icon.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN);
        }


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_logout) {
            finish();
            return true; // 👈 stop further processing
        }
        return super.onOptionsItemSelected(item);
    }

}