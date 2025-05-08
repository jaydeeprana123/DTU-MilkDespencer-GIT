package com.imdc.milkdespencer.roomdb.interfaces;

import android.annotation.SuppressLint;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.imdc.milkdespencer.roomdb.entities.LogEntity;
import com.imdc.milkdespencer.roomdb.entities.TransactionEntity;

import java.util.List;

@Dao
public interface TransactionDao {

    @Insert
    long insert(TransactionEntity transaction);

    @Query("SELECT * FROM transactions ORDER BY id DESC")
    List<TransactionEntity> getAllTransactions();

    @Query("SELECT * FROM transactions WHERE uniqueTransactionId = :uniqueTransactionId")
    TransactionEntity getTransactionByUniqueId(String uniqueTransactionId);

    @Query("DELETE FROM transactions")
    void deleteAll();

    @Query("SELECT MAX(id) FROM transactions")
    long getLastTransactionId();


    @Query("UPDATE transactions SET volume = :volume, milkPrice = :milkPrice, milkTemperature = :milkTemperature, transactionStatus = :transactionStatus, remainingvolume = :remainingVolume WHERE id = :id")
    int updateTransactionDetails(String id, float volume, String milkPrice, String milkTemperature, String transactionStatus, float remainingVolume);

    @Query("UPDATE transactions SET uploadToServer = :uploadToServer WHERE id = :id")
    int updateTransactionUploadToServerStatus(String id, int uploadToServer);


    @Query("UPDATE transactions SET uploadToServer = :uploadToServer WHERE id IN (:idList)")
    void updateTransactionUploadToServerStatusForIds(int uploadToServer, List<String> idList);


    @Query("SELECT * FROM transactions WHERE transactionDate BETWEEN :startDate AND :endDate ORDER BY id DESC")
    List<TransactionEntity> getTransactionsBetweenDates(String startDate, String endDate);

//    default String generateUniqueTransactionId() {
//        long lastTransactionId = getLastTransactionId();
//        @SuppressLint("DefaultLocale") String uniqueTransactionId = "TXN" + String.format("%05d", lastTransactionId + 1);
//        return uniqueTransactionId;
//    }


//    default String generateUniqueTransactionId() {
//        long lastTransactionId = getLastTransactionId();
//        if (lastTransactionId == 0) {
//            lastTransactionId = 1; // Start from 1 if no transactions exist
//        } else {
//            lastTransactionId++; // Increment transaction ID
//        }
//
//        String uniqueTransactionId;
//        do {
//            uniqueTransactionId = "TXN" + String.format("%05d", lastTransactionId);
//            lastTransactionId++; // Increment if duplicate is found
//        } while (getTransactionByUniqueId(uniqueTransactionId) != null);
//
//        return uniqueTransactionId;
//    }



    @Query("SELECT * FROM transactions WHERE transactionDate = :todayDate")
    List<TransactionEntity> getTodayData(String todayDate);


    // Get the sum of the 'volume' column for today's data
    @Query("SELECT SUM(volume) FROM transactions WHERE transactionDate = :todayDate AND (transactionStatus = 'SUCCESS' OR transactionStatus = 'FAILED' OR transactionStatus = 'DOOR OPEN')")
    float getTodayVolumeSum(String todayDate);


    // Get the sum of the 'volume' column for today's data
//    @Query("SELECT SUM(amount) FROM transactions WHERE transactionDate = :todayDate AND (transactionStatus = 'SUCCESS' OR transactionStatus = 'FAILED')")
//    Double getTodayTotalAmount(String todayDate);


    @Query("SELECT IFNULL(SUM(amount), 0) FROM transactions WHERE transactionDate = :todayDate AND (transactionStatus = 'SUCCESS' OR transactionStatus = 'FAILED' OR transactionStatus = 'DOOR OPEN')")
    double getTodayTotalAmount(String todayDate);


    @Query("SELECT IFNULL(SUM(volume), 0) FROM transactions WHERE transactionDate BETWEEN :startDate AND :endDate AND (transactionStatus = 'SUCCESS' OR transactionStatus = 'FAILED' OR transactionStatus = 'DOOR OPEN')")
    float getTotalSuccessVolumeBetweenDates(String startDate, String endDate);


    @Query("SELECT IFNULL(SUM(amount), 0) FROM transactions WHERE transactionDate BETWEEN :startDate AND :endDate AND transactionStatus IN ('SUCCESS', 'FAILED', 'DOOR OPEN')")
    double getTotalAmountBetweenDates(String startDate, String endDate);



    @Query("SELECT * FROM transactions WHERE uploadToServer = 0")
    List<TransactionEntity> getUnUploadedTransactions();

}