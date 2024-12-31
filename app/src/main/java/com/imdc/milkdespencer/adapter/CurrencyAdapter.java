package com.imdc.milkdespencer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.imdc.milkdespencer.R;

public class CurrencyAdapter extends BaseAdapter {

    private final Context mContext;

    public CurrencyAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return CurrencyData.currencyValues.length;
    }

    @Override
    public String getItem(int position) {
        return CurrencyData.currencyValues[position];
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

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // Set data for the views
        viewHolder.imageView.setImageResource(CurrencyData.currencyImages[position]);
        viewHolder.textView.setText(CurrencyData.currencyValues[position]);

        return convertView;
    }

    static class ViewHolder {
        ImageView imageView;
        TextView textView;
    }
}
