package com.imdc.milkdespencer.roomdb.interfaces;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.imdc.milkdespencer.roomdb.entities.LogEntity;

import java.util.List;

@Dao
public interface LogDao {

    @Insert
    void insert(LogEntity logEntity);

    @Query("SELECT * FROM logs")
    List<LogEntity> getAllLogs();

    @Query("DELETE FROM logs")
    void deleteAllLogs();
}