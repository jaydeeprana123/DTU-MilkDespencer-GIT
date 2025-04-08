package com.imdc.milkdespencer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.imdc.milkdespencer.R;

public class SpnLitersAdapter extends BaseAdapter {

    int[] bgColors = {
            R.color.bright_0,
            R.color.bright_1,
            R.color.bright_2,
            R.color.bright_3,
            R.color.bright_4,
            R.color.bright_5,
            R.color.bright_6,
            R.color.bright_7,
            R.color.bright_8
    };

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
            viewHolder.cvPayWithCash = convertView.findViewById(R.id.cvPayWithCash);

            viewHolder.imageView.setBackgroundColor(ContextCompat.getColor(parent.getContext(), android.R.color.transparent));
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // Set data for the views
        viewHolder.textView.setText(LitersSpinnerData.volumeValues[position]);

        if (position < 9) {
            viewHolder.cvPayWithCash.setCardBackgroundColor(ContextCompat.getColor(mContext, bgColors[position]));
        } else {
            viewHolder.cvPayWithCash.setCardBackgroundColor(ContextCompat.getColor(mContext, R.color.white));
        }


        return convertView;
    }

    static class ViewHolder {
        ImageView imageView;
        TextView textView;

        CardView cvPayWithCash;
    }
}
