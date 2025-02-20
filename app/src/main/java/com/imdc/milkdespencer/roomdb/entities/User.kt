package com.imdc.milkdespencer.roomdb.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
class User {
    // Getters and Setters for other fields
    @JvmField
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "user_id")
    var userId = 0

    @JvmField
    @ColumnInfo(name = "username")
    var username: String? = null

    @JvmField
    @ColumnInfo(name = "first_name")
    var first_name: String? = null

    @JvmField
    @ColumnInfo(name = "mobile_no")
    var mobileNo: String? = null

    @JvmField
    @ColumnInfo(name = "last_name")
    var last_name: String? = null

    @JvmField
    @ColumnInfo(name = "password")
    var password: String? = null

    @JvmField
    @ColumnInfo(name = "user_type")
    var userType = 0

    @JvmField
    @ColumnInfo(name = "stripe_customer_id")
    var stripeCustomerId: String? = null

    // Other fields, getters, and setters
    // Constructors
    constructor(username: String?, password: String?, userType: Int) {
        this.username = username
        this.password = password
        this.userType = userType
    }

    constructor()
}
