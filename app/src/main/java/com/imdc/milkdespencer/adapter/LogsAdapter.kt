package com.imdc.milkdespencer.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.imdc.milkdespencer.R
import com.imdc.milkdespencer.adapter.LogsAdapter.LogsViewHolder
import com.imdc.milkdespencer.roomdb.entities.LogEntity
import java.text.SimpleDateFormat
import java.util.Locale

class LogsAdapter(var activity: Activity, private val logs: List<LogEntity>) :
    RecyclerView.Adapter<LogsViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.log_item, parent, false)
        return LogsViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: LogsViewHolder, position: Int) {
        val logItem = logs[position]
        // Bind transaction data to the view holder
        holder.tvLogText.text = logItem.message
        val format = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val formattedDate = format.format(logItem.timestamp)
        holder.tvLogsDateTime.text = formattedDate
    }

    override fun getItemCount(): Int {
        return logs.size
    }

    inner class LogsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvLogText: TextView
        var tvLogsDateTime: TextView

        init {
            tvLogText = itemView.findViewById(R.id.tvLogText)
            tvLogsDateTime = itemView.findViewById(R.id.tvLogsDateTime)
        }
    }
}