package com.imdc.milkdespencer.common;

import static androidx.core.content.ContextCompat.startActivity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.imdc.milkdespencer.CashCollectorActivity;
import com.imdc.milkdespencer.MainActivity;
import com.imdc.milkdespencer.R;

public class LottieAddCashDialog extends Dialog {
    TextView tvPercentage;
    MaterialButton btnCancel;
    LottieAnimationView animationView;

    public LottieAddCashDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_lottie);

        animationView = findViewById(R.id.lottieAnimationView);
        tvPercentage = findViewById(R.id.tvProgressDialog);
        btnCancel = findViewById(R.id.btnCancel);
        tvPercentage.setVisibility(View.VISIBLE);
        btnCancel.setVisibility(View.VISIBLE);
        animationView.setAnimation(R.raw.add_cash);
        tvPercentage.setText("Add cash once machine starts and show red light!!");
        animationView.playAnimation();

    }



}
