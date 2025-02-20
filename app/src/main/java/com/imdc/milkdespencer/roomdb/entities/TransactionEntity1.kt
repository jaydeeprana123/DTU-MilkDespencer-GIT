package com.imdc.milkdespencer.roomdb.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
class TransactionEntity1 {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
    var userName: String? = null
    var password: String? = null
    var transactionType: String? = null
    var bankTransactionNo: String? = null
    var transactionDate: String? = null
    var transactionTime: String? = null
    var amount: String? = null
    var transactionStatus: String? = null
    var upiId: String? = null
    var uniqueTransactionId: String? = null
    var createdBy: String? = null
}
