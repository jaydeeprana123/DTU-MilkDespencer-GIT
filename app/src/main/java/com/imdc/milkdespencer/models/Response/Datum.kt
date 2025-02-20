package com.imdc.milkdespencer.models.Response

import com.google.gson.annotations.SerializedName

class Datum {
    // Getters and Setters
    @SerializedName("SMSConfigurationId")
    var smsConfigurationID: Long = 0

    @SerializedName("SMSApiUrl")
    var smsAPIURL: String? = null

    @SerializedName("SMSSid")
    var smsSid: String? = null

    @SerializedName("SMSApiKey")
    var smsAPIKey: String? = null

    @SerializedName("SMSSender")
    var smsSender: String? = null

    @SerializedName("SMSTemplateId")
    var smsTemplateID: String? = null

    @SerializedName("SMSTemplateContent")
    var smsTemplateContent: String? = null

    @SerializedName("RazorPayKey")
    var razorPayKey: String? = null

    @SerializedName("RazorPaySecretKey")
    var razorPaySecretKey: String? = null
}
