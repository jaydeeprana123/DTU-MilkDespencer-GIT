package com.imdc.milkdespencer;

import android.app.DatePickerDialog;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionHistoryByDateActivity extends AppCompatActivity {

    RecyclerView rvTransactions;
    TextView tvTitle;

    TextView tvTodayTotalAmount, tvTodayTotalVolume;


    MaterialButton btnBackToHome;
    User user;
    private TransactionAdapter transactionAdapter;
    private LogsAdapter logsAdapter;

    private EditText fromDateEditText, toDateEditText;

    private Button submitButton;

    private TextView tvNoDataAvailable;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history_by_date);

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

        fromDateEditText= findViewById(R.id.fromDateEditText);
        toDateEditText= findViewById(R.id.toDateEditText);
        submitButton= findViewById(R.id.submitButton);
        tvNoDataAvailable= findViewById(R.id.tvNoDataAvailable);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));

        btnBackToHome.setOnClickListener(view -> finish());

        fromDateEditText.setOnClickListener(v -> showDatePicker(fromDateEditText));
        toDateEditText.setOnClickListener(v -> showDatePicker(toDateEditText));

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String fromDate = fromDateEditText.getText().toString().trim();
                String toDate = toDateEditText.getText().toString().trim();

                if (fromDate.isEmpty()) {
                    Toast.makeText(TransactionHistoryByDateActivity.this, "Please select from date", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (toDate.isEmpty()) {
                    toDate = fromDate; // if toDate not selected, use fromDate
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date from = null;
                Date to = null;

                try {
                    from = sdf.parse(fromDate);
                    to = sdf.parse(toDate);
                } catch (ParseException e) {
                    e.printStackTrace();
                    Toast.makeText(TransactionHistoryByDateActivity.this, "Invalid date format", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (from.after(to)) {
                    Toast.makeText(TransactionHistoryByDateActivity.this, "From Date must be before To Date", Toast.LENGTH_SHORT).show();
                    return;
                }

                String finalToDate = toDate;
                new Thread(() -> {
                    AppDatabase appDatabase = AppDatabase.getInstance(TransactionHistoryByDateActivity.this);

                    boolean isAdmin = user.getUserType() == UserTypeEnum.ADMIN.value()
                            || user.getUserType() == UserTypeEnum.CUSTOMER_ADMIN.value();

                    if (isAdmin) {
                        runOnUiThread(() -> {
                            updateUI("Logs", appDatabase.logDao().getAllLogs(), true);
                            tvTodayTotalAmount.setVisibility(View.GONE);
                            tvTodayTotalVolume.setVisibility(View.GONE);
                        });

                    } else {


                        List<TransactionEntity> transactions = appDatabase.transactionDao().getTransactionsBetweenDates(fromDate, finalToDate);
                        double totalAmount = appDatabase.transactionDao().getTotalAmountBetweenDates(fromDate, finalToDate);
                        float totalVolume = appDatabase.transactionDao().getTotalSuccessVolumeBetweenDates(fromDate, finalToDate);

                        totalAmount = Double.parseDouble(String.format("%.2f", totalAmount));
                        totalVolume = Float.parseFloat(String.format("%.2f", totalVolume));

                        Log.e("totalAmount", String.valueOf(totalAmount));
                        Log.e("totalVolume", String.valueOf(totalVolume));


                        float finalTotalVolume = totalVolume;
                        double finalTotalAmount = totalAmount;
                        runOnUiThread(() -> {
                            updateUI("Transaction Summary", transactions, false);
                            tvTodayTotalAmount.setVisibility(View.VISIBLE);
                            tvTodayTotalVolume.setVisibility(View.VISIBLE);
                            tvTodayTotalAmount.setText("Total Summary     ₹" + finalTotalAmount);
                            tvTodayTotalVolume.setText(finalTotalVolume + "L");
                        });



                    }
                }).start();
            }
        });





    }

    // Helper method to update UI with fetched data
    private void updateUI(String title, List<?> data, boolean isLog) {

            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title);
            }

            tvTitle.setText(title);

            if (isLog) {
                logsAdapter = new LogsAdapter(TransactionHistoryByDateActivity.this, (List<LogEntity>) data);
                rvTransactions.setAdapter(logsAdapter);
            } else {

                if(data.isEmpty()){
                    tvNoDataAvailable.setVisibility(View.VISIBLE);
                }else {

                    tvNoDataAvailable.setVisibility(View.GONE);
                    transactionAdapter = new TransactionAdapter(TransactionHistoryByDateActivity.this, (List<TransactionEntity>) data);
                    rvTransactions.setAdapter(transactionAdapter);

                    // Add divider to the RecyclerView
                    DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvTransactions.getContext(), DividerItemDecoration.VERTICAL);
                    dividerItemDecoration.setDrawable(ContextCompat.getDrawable(TransactionHistoryByDateActivity.this, R.drawable.recycler_view_divider));
                    rvTransactions.addItemDecoration(dividerItemDecoration);
                }


            }

    }


    private void showDatePicker(EditText targetEditText) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
            String selectedDate = String.format("%04d-%02d-%02d", year1, month1 + 1, dayOfMonth);
            targetEditText.setText(selectedDate);
        }, year, month, day);
        datePickerDialog.show();
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