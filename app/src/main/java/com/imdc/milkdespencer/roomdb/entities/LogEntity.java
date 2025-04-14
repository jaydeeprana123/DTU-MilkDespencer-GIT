package com.imdc.milkdespencer.roomdb.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "logs")
public class LogEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String message;
    private long timestamp;
    private String machineId;

    private String username;

    private String password;

//    public LogEntity(String message) {
//        this.message = message;
//        this.timestamp = System.currentTimeMillis();
//    }

    public LogEntity(String message, String machineId, String username, String password) {
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.machineId = machineId;
        this.username = username;
        this.password = password;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}