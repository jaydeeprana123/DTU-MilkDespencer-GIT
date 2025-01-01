package com.imdc.milkdespencer.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.imdc.milkdespencer.R;
import com.imdc.milkdespencer.TransactionHistoryActivity;
import com.imdc.milkdespencer.roomdb.entities.TransactionEntity;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private final List<TransactionEntity> transactions;
    Activity activity;

    public TransactionAdapter(Activity activity, List<TransactionEntity> transactions) {
        this.transactions = transactions;
        this.activity = activity;

    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.transaction_item, parent, false);
        return new TransactionViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        TransactionEntity transaction = transactions.get(position);
        // Bind transaction data to the view holder
        holder.tvTransactionAmount.setText("₹ " + transaction.getAmount() + " (" + transaction.getWeight());
        holder.tvTransactionId.setText(transaction.getUniqueTransactionId());
        holder.tvTransactionType.setText(transaction.getTransactionType());
        holder.tvTransactionDateTime.setText(transaction.getTransactionDate() + " at " + transaction.getTransactionTime());

        /// Add volume on 31-12-2024
//        holder.tvVolume.setText("(" + transaction.get);
        if (transaction.getTransactionStatus().equalsIgnoreCase("Failed") || transaction.getTransactionStatus().equalsIgnoreCase("Time Out")) {
            holder.tvTransactionDateStatus.setTextColor(ContextCompat.getColor(activity, R.color.md_theme_dark_errorContainer));
        } else {
            holder.tvTransactionDateStatus.setTextColor(ContextCompat.getColor(activity, R.color.md_theme_dark_success));
        }
        holder.tvTransactionDateStatus.setText(transaction.getTransactionStatus());

    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public class TransactionViewHolder extends RecyclerView.ViewHolder {

        TextView tvTransactionAmount, tvTransactionDateTime, tvTransactionType, tvTransactionId, tvTransactionDateStatus,tvVolume;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTransactionAmount = itemView.findViewById(R.id.tvTransactionAmount);
            tvTransactionDateTime = itemView.findViewById(R.id.tvTransactionDateTime);
            tvTransactionType = itemView.findViewById(R.id.tvTransactionType);
            tvTransactionId = itemView.findViewById(R.id.tvTransactionId);
            tvTransactionDateStatus = itemView.findViewById(R.id.tvTransactionDateStatus);
            tvVolume= itemView.findViewById(R.id.tvVolume);
        }
    }
}