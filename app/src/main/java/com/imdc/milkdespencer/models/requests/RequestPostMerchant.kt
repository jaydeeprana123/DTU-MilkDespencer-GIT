package com.imdc.milkdespencer.models.requests

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class RequestPostMerchant {
    @SerializedName("UserName")
    @Expose
    var userName: String? = null

    @SerializedName("Password")
    @Expose
    var password: String? = null

    @SerializedName("MerchantName")
    @Expose
    var merchantName: String? = null

    @SerializedName("MerchantId")
    @Expose
    var merchantId: String? = null

    @SerializedName("MerchantQrCode")
    @Expose
    var merchantQrCode: String? = null

    @SerializedName("EmailId")
    @Expose
    var emailId: String? = null

    @SerializedName("ContactNumber")
    @Expose
    var contactNumber: String? = null

    @SerializedName("DeviceId")
    @Expose
    var deviceId: String? = null

    @SerializedName("IsAdmin")
    @Expose
    var isAdmin: String? = null
}