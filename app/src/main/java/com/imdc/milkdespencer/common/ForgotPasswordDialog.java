package com.imdc.milkdespencer.common;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.textfield.TextInputLayout;
import com.imdc.milkdespencer.R;

public class ForgotPasswordDialog extends Dialog {

    private TextInputLayout tilForgotUsername, tilOtp, tilNewPassword, tilConfirmPassword;
    private EditText tieForgotUsername, tieOtp, tieNewPassword, tieConfirmPassword;
    private Button btnSendOtp, btnResetPassword;
    private TextView tvForgotPasswordTitle;

    public ForgotPasswordDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.forgot_password);

        initializeUI();

        // Add click listeners for buttons

    }

    private void initializeUI() {
        tilForgotUsername = findViewById(R.id.tilForgotUsername);
        tilOtp = findViewById(R.id.tilOtp);
        tilNewPassword = findViewById(R.id.tilNewPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        tieForgotUsername = findViewById(R.id.tiePhoneNo);
        tieOtp = findViewById(R.id.tieOtp);
        tieNewPassword = findViewById(R.id.tieNewPassword);
        tieConfirmPassword = findViewById(R.id.tieConfirmPassword);

        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnResetPassword = findViewById(R.id.btnResetPassword);

        tvForgotPasswordTitle = findViewById(R.id.tvForgotPasswordTitle);

        // Set up any additional UI properties
    }

    // Add any necessary methods for handling button clicks

}