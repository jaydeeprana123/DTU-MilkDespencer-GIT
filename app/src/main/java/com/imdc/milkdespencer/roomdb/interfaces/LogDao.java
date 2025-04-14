package com.imdc.milkdespencer.roomdb.interfaces;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.imdc.milkdespencer.roomdb.entities.LogEntity;

import java.util.List;

@Dao
public interface LogDao {

    @Insert
    long insert(LogEntity logEntity);

    @Query("SELECT * FROM logs ORDER BY id DESC")
    List<LogEntity> getAllLogs();

    @Query("DELETE FROM logs")
    void deleteAllLogs();

    @Query("UPDATE logs SET uploadToServer = :uploadToServer WHERE id = :id")
    int updateLogUploadToServerStatus(String id, int uploadToServer);

    @Query("UPDATE logs SET uploadToServer = :uploadToServer WHERE id IN (:idList)")
    void updateLogsUploadToServerStatusForIds(int uploadToServer, List<String> idList);

    @Query("SELECT * FROM logs WHERE uploadToServer = 0")
    List<LogEntity> getUnUploadedLogs();
}