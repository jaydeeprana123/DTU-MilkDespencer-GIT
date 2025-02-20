package com.imdc.milkdespencer.common

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.imdc.milkdespencer.R

class LottieDialog(context: Context) : Dialog(context) {
    var tvPercentage: TextView? = null
    var tvKeepTheDoorClose: TextView? = null
    var animationView: LottieAnimationView? = null
    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_lottie)
        animationView = findViewById(R.id.lottieAnimationView)
        tvKeepTheDoorClose = findViewById(R.id.tvKeepTheDoorClose)
        tvPercentage = findViewById(R.id.tvProgressDialog)
        tvPercentage?.setVisibility(View.VISIBLE)
        tvKeepTheDoorClose?.setVisibility(View.VISIBLE)
        animationView?.setAnimation(R.raw.milk_loading)
        animationView?.setProgress(0f)
        tvPercentage?.setText("Filling In Process")
        animationView?.playAnimation()
    }

    fun setPercentage(percentage: Double) {
        val msg = Math.round(percentage).toString() + " (%)"
        Log.e("TAG", " run:>> final data: setPercentage: $msg")
        tvPercentage!!.text = "Filling In Process"

//        animationView.setProgress(Math.round(percentage));
//        tvPercentage.setText(msg);
    }
}
