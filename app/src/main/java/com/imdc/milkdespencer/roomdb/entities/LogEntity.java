package com.imdc.milkdespencer.roomdb.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "logs")
public class LogEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String message;
    private long timestamp;

    // ✅ New Field
    private int uploadToServer = 0;


    // ✅ New Field
    private String machineId;

    // ✅ New Field
    private String username;

    // ✅ New Field
    private String password;

//    public LogEntity(String message) {
//        this.message = message;
//        this.timestamp = System.currentTimeMillis();
//    }

    public LogEntity(String message, String machineId, String username, String password, int uploadToServer) {
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.machineId = machineId;
        this.username = username;
        this.password = password;
        this.uploadToServer = uploadToServer;
    }


    public int getUploadToServer() {
        return uploadToServer;
    }

    public void setUploadToServer(int uploadToServer) {
        this.uploadToServer = uploadToServer;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
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