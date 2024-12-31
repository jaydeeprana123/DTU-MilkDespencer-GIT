package com.imdc.milkdespencer.models.requests;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class RequestPostMerchant {

    @SerializedName("UserName")
    @Expose
    private String userName;
    @SerializedName("Password")
    @Expose
    private String password;
    @SerializedName("MerchantName")
    @Expose
    private String merchantName;
    @SerializedName("MerchantId")
    @Expose
    private String merchantId;
    @SerializedName("MerchantQrCode")
    @Expose
    private String merchantQrCode;
    @SerializedName("EmailId")
    @Expose
    private String emailId;
    @SerializedName("ContactNumber")
    @Expose
    private String contactNumber;
    @SerializedName("DeviceId")
    @Expose
    private String deviceId;
    @SerializedName("IsAdmin")
    @Expose
    private String isAdmin;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getMerchantQrCode() {
        return merchantQrCode;
    }

    public void setMerchantQrCode(String merchantQrCode) {
        this.merchantQrCode = merchantQrCode;
    }

    public String getEmailId() {
        return emailId;
    }

    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(String isAdmin) {
        this.isAdmin = isAdmin;
    }

}