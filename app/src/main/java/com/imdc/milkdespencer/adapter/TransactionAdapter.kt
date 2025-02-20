package com.imdc.milkdespencer.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.imdc.milkdespencer.R
import com.imdc.milkdespencer.adapter.TransactionAdapter.TransactionViewHolder
import com.imdc.milkdespencer.roomdb.entities.TransactionEntity

class TransactionAdapter(
    var activity: Activity,
    private val transactions: List<TransactionEntity>
) : RecyclerView.Adapter<TransactionViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.transaction_item, parent, false)
        return TransactionViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        // Bind transaction data to the view holder
        val transactionTruncet = String.format("%.2f", transaction.amount).toDouble()
        holder.tvTransactionAmount.text = "₹ $transactionTruncet"
        holder.tvVolume.text = transaction.volume.toString() + "L"
        holder.tvTransactionId.text = transaction.uniqueTransactionId
        holder.tvTransactionType.text = transaction.transactionType
        holder.tvTransactionDateTime.text =
            transaction.transactionDate + " at " + transaction.transactionTime


        /// Add volume on 31-12-2024
//        holder.tvVolume.setText("(" + transaction.get);
        if (transaction.transactionStatus.equals(
                "Failed",
                ignoreCase = true
            ) || transaction.transactionStatus.equals("Time Out", ignoreCase = true)
        ) {
            holder.tvTransactionDateStatus.setTextColor(
                ContextCompat.getColor(
                    activity,
                    R.color.md_theme_dark_errorContainer
                )
            )
        } else {
            holder.tvTransactionDateStatus.setTextColor(
                ContextCompat.getColor(
                    activity,
                    R.color.md_theme_dark_success
                )
            )
        }
        holder.tvTransactionDateStatus.text = transaction.transactionStatus
    }

    override fun getItemCount(): Int {
        return transactions.size
    }

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvTransactionAmount: TextView
        var tvTransactionDateTime: TextView
        var tvTransactionType: TextView
        var tvTransactionId: TextView
        var tvTransactionDateStatus: TextView
        var tvVolume: TextView

        init {
            tvTransactionAmount = itemView.findViewById(R.id.tvTransactionAmount)
            tvTransactionDateTime = itemView.findViewById(R.id.tvTransactionDateTime)
            tvTransactionType = itemView.findViewById(R.id.tvTransactionType)
            tvTransactionId = itemView.findViewById(R.id.tvTransactionId)
            tvTransactionDateStatus = itemView.findViewById(R.id.tvTransactionDateStatus)
            tvVolume = itemView.findViewById(R.id.tvVolume)
        }
    }
}