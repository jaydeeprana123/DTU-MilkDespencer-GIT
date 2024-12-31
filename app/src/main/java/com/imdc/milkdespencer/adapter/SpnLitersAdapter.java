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

public class SpnLitersAdapter extends BaseAdapter {

    private final Context mContext;

    public SpnLitersAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return LitersSpinnerData.volumeValues.length;
    }

    @Override
    public String getItem(int position) {
        return LitersSpinnerData.volumeValues[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.spn_currency_item, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.imageView = convertView.findViewById(R.id.iv_currencyImage);
            viewHolder.imageView.setVisibility(View.GONE);
            viewHolder.textView = convertView.findViewById(R.id.tvCurrencyAmt);

            viewHolder.imageView.setBackgroundColor(ContextCompat.getColor(parent.getContext(), android.R.color.transparent));
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // Set data for the views
        viewHolder.textView.setText(LitersSpinnerData.volumeValues[position]);

        return convertView;
    }

    static class ViewHolder {
        ImageView imageView;
        TextView textView;
    }
}
