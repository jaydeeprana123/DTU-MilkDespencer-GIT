package com.imdc.milkdespencer.roomdb.interfaces;

import android.annotation.SuppressLint;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

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

//    default String generateUniqueTransactionId() {
//        long lastTransactionId = getLastTransactionId();
//        @SuppressLint("DefaultLocale") String uniqueTransactionId = "TXN" + String.format("%05d", lastTransactionId + 1);
//        return uniqueTransactionId;
//    }


    default String generateUniqueTransactionId() {
        long lastTransactionId = getLastTransactionId();
        if (lastTransactionId == 0) {
            lastTransactionId = 1; // Start from 1 if no transactions exist
        } else {
            lastTransactionId++; // Increment transaction ID
        }

        String uniqueTransactionId;
        do {
            uniqueTransactionId = "TXN" + String.format("%05d", lastTransactionId);
            lastTransactionId++; // Increment if duplicate is found
        } while (getTransactionByUniqueId(uniqueTransactionId) != null);

        return uniqueTransactionId;
    }



    @Query("SELECT * FROM transactions WHERE transactionDate = :todayDate")
    List<TransactionEntity> getTodayData(String todayDate);


    // Get the sum of the 'volume' column for today's data
    @Query("SELECT SUM(volume) FROM transactions WHERE transactionDate = :todayDate AND transactionStatus = :transactionStatus")
    float getTodayVolumeSum(String todayDate, String transactionStatus);


    // Get the sum of the 'volume' column for today's data
    @Query("SELECT SUM(amount) FROM transactions WHERE transactionDate = :todayDate AND transactionStatus = :transactionStatus")
    float getTodayAmountSum(String todayDate, String transactionStatus);


}