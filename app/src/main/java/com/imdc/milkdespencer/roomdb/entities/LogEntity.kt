package com.imdc.milkdespencer.roomdb.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logs")
class LogEntity(var message: String) {
    @kotlin.jvm.JvmField
    @PrimaryKey(autoGenerate = true)
    var id = 0
    var timestamp: Long

    init {
        timestamp = System.currentTimeMillis()
    }
}