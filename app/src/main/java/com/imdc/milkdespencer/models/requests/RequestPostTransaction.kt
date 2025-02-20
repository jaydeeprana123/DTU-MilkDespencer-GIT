package com.imdc.milkdespencer.models.requests

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class RequestPostTransaction {
    @SerializedName("UserName")
    @Expose
    var userName: String? = null

    @SerializedName("Password")
    @Expose
    var password: String? = null

    @SerializedName("TransactionType")
    @Expose
    var transactionType: String? = null

    @SerializedName("BankTransactionNo")
    @Expose
    var bankTransactionNo: String? = null

    @SerializedName("TransactionDate")
    @Expose
    var transactionDate: String? = null

    @SerializedName("TransactionTime")
    @Expose
    var transactionTime: String? = null

    @SerializedName("Amount")
    @Expose
    var amount: String? = null

    @SerializedName("TransactionStatus")
    @Expose
    var transactionStatus: String? = null

    @SerializedName("UPIID")
    @Expose
    var upiid: String? = null

    @SerializedName("UniqueTransactionId")
    @Expose
    var uniqueTransactionId: String? = null

    @SerializedName("CretedBy")
    @Expose
    var cretedBy: String? = null
}