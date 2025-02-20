package com.imdc.milkdespencer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.imdc.milkdespencer.R

class SpnLitersAdapter(private val mContext: Context) : BaseAdapter() {
    override fun getCount(): Int {
        return LitersSpinnerData.volumeValues.size
    }

    override fun getItem(position: Int): String {
        return LitersSpinnerData.volumeValues[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
        var convertView = convertView
        val viewHolder: ViewHolder
        if (convertView == null) {
            convertView =
                LayoutInflater.from(mContext).inflate(R.layout.spn_currency_item, parent, false)
            viewHolder = ViewHolder()
            viewHolder.imageView = convertView.findViewById(R.id.iv_currencyImage)
            viewHolder.imageView?.setVisibility(View.GONE)
            viewHolder.textView = convertView.findViewById(R.id.tvCurrencyAmt)
            viewHolder.imageView?.setBackgroundColor(
                ContextCompat.getColor(
                    parent.context,
                    android.R.color.transparent
                )
            )
            convertView.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
        }

        // Set data for the views
        viewHolder.textView!!.text = LitersSpinnerData.volumeValues[position]
        return convertView
    }

    internal class ViewHolder {
        var imageView: ImageView? = null
        var textView: TextView? = null
    }
}
