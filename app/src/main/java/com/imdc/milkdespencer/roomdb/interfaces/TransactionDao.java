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

    @Query("SELECT * FROM transactions")
    List<TransactionEntity> getAllTransactions();

    @Query("SELECT * FROM transactions WHERE uniqueTransactionId = :uniqueTransactionId")
    TransactionEntity getTransactionByUniqueId(String uniqueTransactionId);

    @Query("DELETE FROM transactions")
    void deleteAll();

    @Query("SELECT MAX(id) FROM transactions")
    long getLastTransactionId();

    default String generateUniqueTransactionId() {
        long lastTransactionId = getLastTransactionId();
        @SuppressLint("DefaultLocale") String uniqueTransactionId = "TXN" + String.format("%05d", lastTransactionId + 1);
        return uniqueTransactionId;
    }
}