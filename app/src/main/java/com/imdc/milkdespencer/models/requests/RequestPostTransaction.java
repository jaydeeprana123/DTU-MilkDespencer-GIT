package com.imdc.milkdespencer.models.requests;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class RequestPostTransaction {

    @SerializedName("UserName")
    @Expose
    private String userName;
    @SerializedName("Password")
    @Expose
    private String password;
    @SerializedName("TransactionType")
    @Expose
    private String transactionType;
    @SerializedName("BankTransactionNo")
    @Expose
    private String bankTransactionNo;
    @SerializedName("TransactionDate")
    @Expose
    private String transactionDate;
    @SerializedName("TransactionTime")
    @Expose
    private String transactionTime;
    @SerializedName("Amount")
    @Expose
    private String amount;
    @SerializedName("TransactionStatus")
    @Expose
    private String transactionStatus;
    @SerializedName("UPIID")
    @Expose
    private String upiid;
    @SerializedName("UniqueTransactionId")
    @Expose
    private String uniqueTransactionId;
    @SerializedName("CretedBy")
    @Expose
    private String cretedBy;

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

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getBankTransactionNo() {
        return bankTransactionNo;
    }

    public void setBankTransactionNo(String bankTransactionNo) {
        this.bankTransactionNo = bankTransactionNo;
    }

    public String getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(String transactionTime) {
        this.transactionTime = transactionTime;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getTransactionStatus() {
        return transactionStatus;
    }

    public void setTransactionStatus(String transactionStatus) {
        this.transactionStatus = transactionStatus;
    }

    public String getUpiid() {
        return upiid;
    }

    public void setUpiid(String upiid) {
        this.upiid = upiid;
    }

    public String getUniqueTransactionId() {
        return uniqueTransactionId;
    }

    public void setUniqueTransactionId(String uniqueTransactionId) {
        this.uniqueTransactionId = uniqueTransactionId;
    }

    public String getCretedBy() {
        return cretedBy;
    }

    public void setCretedBy(String cretedBy) {
        this.cretedBy = cretedBy;
    }

}