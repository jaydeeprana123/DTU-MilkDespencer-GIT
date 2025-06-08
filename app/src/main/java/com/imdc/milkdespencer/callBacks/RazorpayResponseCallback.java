package com.imdc.milkdespencer.callBacks;

import com.imdc.milkdespencer.models.Response.RazorpayQrPaymentResponse;

public interface RazorpayResponseCallback {

    void onSuccess(RazorpayQrPaymentResponse response);
    void onError(String error);

}
