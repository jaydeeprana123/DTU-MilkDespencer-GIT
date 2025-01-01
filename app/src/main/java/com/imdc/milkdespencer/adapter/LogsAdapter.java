package com.imdc.milkdespencer.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.imdc.milkdespencer.R;
import com.imdc.milkdespencer.TransactionHistoryActivity;
import com.imdc.milkdespencer.roomdb.entities.LogEntity;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class LogsAdapter extends RecyclerView.Adapter<LogsAdapter.LogsViewHolder> {

    private final List<LogEntity> logs;
    Activity activity;

    public LogsAdapter(Activity activity, List<LogEntity> logs) {
        this.logs = logs;
        this.activity = activity;

    }

    @NonNull
    @Override
    public LogsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.log_item, parent, false);
        return new LogsViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull LogsViewHolder holder, int position) {
        LogEntity logItem = logs.get(position);
        // Bind transaction data to the view holder
        holder.tvLogText.setText(logItem.getMessage());
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
        String formattedDate = format.format(logItem.getTimestamp());
        holder.tvLogsDateTime.setText(formattedDate);


    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    public class LogsViewHolder extends RecyclerView.ViewHolder {

        TextView tvLogText, tvLogsDateTime;

        public LogsViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLogText = itemView.findViewById(R.id.tvLogText);
            tvLogsDateTime = itemView.findViewById(R.id.tvLogsDateTime);

        }
    }
}