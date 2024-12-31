package com.imdc.milkdespencer.common;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.airbnb.lottie.LottieAnimationView;
import com.imdc.milkdespencer.R;

public class AddBeakerLottieDialog extends Dialog {
    TextView tvPercentage;
    LottieAnimationView animationView;

    public AddBeakerLottieDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_lottie);

        animationView = findViewById(R.id.lottieAnimationView);


        tvPercentage = findViewById(R.id.tvProgressDialog);
        tvPercentage.setVisibility(View.VISIBLE);
        animationView.setAnimation(R.raw.close_door);
        animationView.setProgress(0);
        tvPercentage.setText("Filling In Process");
        animationView.playAnimation();
    }

    public void setPercentage(double percentage) {

        String msg = Math.round(percentage) + " (%)";
        Log.e("TAG", " run:>> final data: setPercentage: " + msg);
        tvPercentage.setText("Filling In Process");

//        animationView.setProgress(Math.round(percentage));
//        tvPercentage.setText(msg);
    }

}
