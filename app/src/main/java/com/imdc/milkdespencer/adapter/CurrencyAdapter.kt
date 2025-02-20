package com.imdc.milkdespencer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.imdc.milkdespencer.R

class CurrencyAdapter(private val mContext: Context) : BaseAdapter() {
    override fun getCount(): Int {
        return CurrencyData.currencyValues.size
    }

    override fun getItem(position: Int): String {
        return CurrencyData.currencyValues[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
        var convertView = convertView
        val viewHolder: ViewHolder
        if (convertView == null) {
            convertView =
                LayoutInflater.from(mContext).inflate(R.layout.gv_currency_item, parent, false)
            viewHolder = ViewHolder()
            viewHolder.imageView = convertView.findViewById(R.id.iv_currencyImage)
            viewHolder.textView = convertView.findViewById(R.id.tvCurrencyAmt)
            convertView.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
        }

        // Set data for the views
        viewHolder.imageView!!.setImageResource(CurrencyData.currencyImages[position])
        viewHolder.textView!!.text = CurrencyData.currencyValues[position]
        return convertView
    }

    internal class ViewHolder {
        var imageView: ImageView? = null
        var textView: TextView? = null
    }
}
