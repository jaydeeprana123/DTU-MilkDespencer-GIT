package com.imdc.milkdespencer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.imdc.milkdespencer.R;

public class SpnCurrencyAdapter extends BaseAdapter {

    private final Context mContext;

    public SpnCurrencyAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return CurrencyData.currencySpmValues.length;
    }

    @Override
    public String getItem(int position) {
        return CurrencyData.currencySpmValues[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.gv_currency_item, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.imageView = convertView.findViewById(R.id.iv_currencyImage);
            viewHolder.textView = convertView.findViewById(R.id.tvCurrencyAmt);

            viewHolder.imageView.setBackgroundColor(ContextCompat.getColor(parent.getContext(), android.R.color.transparent));
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // Set data for the views
//        viewHolder.imageView.setImageResource(CurrencyData.currencyImages[position]);
        viewHolder.textView.setText(CurrencyData.currencySpmValues[position]);

        return convertView;
    }

    static class ViewHolder {
        ImageView imageView;
        TextView textView;
    }
}
