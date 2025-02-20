package com.imdc.milkdespencer.common

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.button.MaterialButton
import com.imdc.milkdespencer.R

class LottieAddCashDialog(context: Context) : Dialog(context) {
    var tvPercentage: TextView? = null
    var btnCancel: MaterialButton? = null
    var animationView: LottieAnimationView? = null
    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_lottie)
        animationView = findViewById(R.id.lottieAnimationView)
        tvPercentage = findViewById(R.id.tvProgressDialog)
        btnCancel = findViewById(R.id.btnCancel)
        tvPercentage?.setVisibility(View.VISIBLE)
        btnCancel?.setVisibility(View.VISIBLE)
        animationView?.setAnimation(R.raw.add_cash)
        tvPercentage?.setText("Add cash once machine starts and show red light!!")
        animationView?.playAnimation()
    }
}
