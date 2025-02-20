package com.imdc.milkdespencer.roomdb.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
class TransactionEntity {
    @JvmField
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
    @JvmField
    var userName: String? = null
    @JvmField
    var password: String? = null
    @JvmField
    var transactionType: String? = null
    @JvmField
    var bankTransactionNo: String? = null
    @JvmField
    var transactionDate: String? = null
    @JvmField
    var transactionTime: String? = null
    @JvmField
    var amount = 0.0
    @JvmField
    var volume = 0f
    @JvmField
    var milkPrice: String? = null
    @JvmField
    var milkTemperature: String? = null
    @JvmField
    var machineId: String? = null
    @JvmField
    var transactionStatus: String? = null
    @JvmField
    var upiId: String? = null
    @JvmField
    var uniqueTransactionId: String? = null
    @JvmField
    var createdBy: String? = null
}
