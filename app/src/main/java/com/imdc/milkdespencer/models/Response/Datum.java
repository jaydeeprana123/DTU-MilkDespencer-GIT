package com.imdc.milkdespencer.models.Response;

import com.google.gson.annotations.SerializedName;

public class Datum {
    @SerializedName("SMSConfigurationId")
    private long smsConfigurationID;

    @SerializedName("SMSApiUrl")
    private String smsAPIURL;

    @SerializedName("SMSSid")
    private String smsSid;

    @SerializedName("SMSApiKey")
    private String smsAPIKey;

    @SerializedName("SMSSender")
    private String smsSender;

    @SerializedName("SMSTemplateId")
    private String smsTemplateID;

    @SerializedName("SMSTemplateContent")
    private String smsTemplateContent;

    @SerializedName("RazorPayKey")
    private String razorPayKey;

    @SerializedName("RazorPaySecretKey")
    private String razorPaySecretKey;

    // Getters and Setters
    public long getSmsConfigurationID() {
        return smsConfigurationID;
    }

    public void setSmsConfigurationID(long smsConfigurationID) {
        this.smsConfigurationID = smsConfigurationID;
    }

    public String getSmsAPIURL() {
        return smsAPIURL;
    }

    public void setSmsAPIURL(String smsAPIURL) {
        this.smsAPIURL = smsAPIURL;
    }

    public String getSmsSid() {
        return smsSid;
    }

    public void setSmsSid(String smsSid) {
        this.smsSid = smsSid;
    }

    public String getSmsAPIKey() {
        return smsAPIKey;
    }

    public void setSmsAPIKey(String smsAPIKey) {
        this.smsAPIKey = smsAPIKey;
    }

    public String getSmsSender() {
        return smsSender;
    }

    public void setSmsSender(String smsSender) {
        this.smsSender = smsSender;
    }

    public String getSmsTemplateID() {
        return smsTemplateID;
    }

    public void setSmsTemplateID(String smsTemplateID) {
        this.smsTemplateID = smsTemplateID;
    }

    public String getSmsTemplateContent() {
        return smsTemplateContent;
    }

    public void setSmsTemplateContent(String smsTemplateContent) {
        this.smsTemplateContent = smsTemplateContent;
    }

    public String getRazorPayKey() {
        return razorPayKey;
    }

    public void setRazorPayKey(String razorPayKey) {
        this.razorPayKey = razorPayKey;
    }

    public String getRazorPaySecretKey() {
        return razorPaySecretKey;
    }

    public void setRazorPaySecretKey(String razorPaySecretKey) {
        this.razorPaySecretKey = razorPaySecretKey;
    }
}
