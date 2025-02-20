package com.imdc.milkdespencer

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.imdc.milkdespencer.adapter.LogsAdapter
import com.imdc.milkdespencer.adapter.TransactionAdapter
import com.imdc.milkdespencer.common.Constants
import com.imdc.milkdespencer.enums.UserTypeEnum
import com.imdc.milkdespencer.roomdb.AppDatabase
import com.imdc.milkdespencer.roomdb.entities.LogEntity
import com.imdc.milkdespencer.roomdb.entities.TransactionEntity
import com.imdc.milkdespencer.roomdb.entities.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionHistoryActivity : AppCompatActivity() {
    var rvTransactions: RecyclerView? = null
    var tvTitle: TextView? = null
    var tvTodayTotalAmount: TextView? = null
    var tvTodayTotalVolume: TextView? = null
    var btnBackToHome: MaterialButton? = null
    var user: User? = null
    private var transactionAdapter: TransactionAdapter? = null
    private var logsAdapter: LogsAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_history)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        // Retrieve user data from intent
        if (intent != null && intent.hasExtra(Constants.LoginUser)) {
            user = Gson().fromJson(intent.getStringExtra(Constants.LoginUser), User::class.java)
        }
        rvTransactions = findViewById(R.id.rvTransactions)
        tvTitle = findViewById(R.id.tvTitle)
        btnBackToHome = findViewById(R.id.btnBackToHome)
        tvTodayTotalAmount = findViewById(R.id.tvTodayTotalAmount)
        tvTodayTotalVolume = findViewById(R.id.tvTodayTotalVolume)
        rvTransactions?.layoutManager = LinearLayoutManager(this)
        btnBackToHome?.setOnClickListener(View.OnClickListener { view: View? -> finish() })

        // Use a background task to fetch data
        Thread {
            val appDatabase = AppDatabase.getInstance(this@TransactionHistoryActivity)
            Log.e("TAG", "run: " + Gson().toJson(user))
            val isAdmin =
                user!!.userType == UserTypeEnum.ADMIN.value() || user!!.userType == UserTypeEnum.CUSTOMER_ADMIN.value()
            if (isAdmin) {
                updateUI("Logs", appDatabase.logDao().allLogs, true)
                tvTodayTotalAmount?.setVisibility(View.GONE)
                tvTodayTotalVolume?.setVisibility(View.GONE)
            } else {
                updateUI("Transaction History", appDatabase.transactionDao().allTransactions, false)
                tvTodayTotalAmount?.setVisibility(View.VISIBLE)
                tvTodayTotalVolume?.setVisibility(View.VISIBLE)

                /// Get Today date
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                var totalAmount =
                    appDatabase.transactionDao().getTodayAmountSum(todayDate, "SUCCESS").toDouble()
                var totalVolume =
                    appDatabase.transactionDao().getTodayVolumeSum(todayDate, "SUCCESS")
                totalAmount = String.format("%.2f", totalAmount).toDouble()
                totalVolume = String.format("%.2f", totalVolume).toFloat()
                Log.e("totalAmount", totalAmount.toString())
                Log.e("totalVolume", totalVolume.toString())
                tvTodayTotalAmount?.setText("Today's Summary     ₹$totalAmount")
                tvTodayTotalVolume?.setText(totalVolume.toString() + "L")
            }
        }.start()
    }

    // Helper method to update UI with fetched data
    private fun updateUI(title: String, data: List<*>, isLog: Boolean) {
        runOnUiThread {
            if (supportActionBar != null) {
                supportActionBar!!.title = title
            }
            tvTitle!!.text = title
            if (isLog) {
                logsAdapter = LogsAdapter(this@TransactionHistoryActivity, data as List<LogEntity?>)
                rvTransactions!!.adapter = logsAdapter
            } else {
                transactionAdapter = TransactionAdapter(
                    this@TransactionHistoryActivity,
                    data as List<TransactionEntity?>
                )
                rvTransactions!!.adapter = transactionAdapter

                // Add divider to the RecyclerView
                val dividerItemDecoration =
                    DividerItemDecoration(rvTransactions!!.context, DividerItemDecoration.VERTICAL)
                dividerItemDecoration.setDrawable(
                    ContextCompat.getDrawable(
                        this@TransactionHistoryActivity,
                        R.drawable.recycler_view_divider
                    )
                )
                rvTransactions!!.addItemDecoration(dividerItemDecoration)
            }
        }
    }
}