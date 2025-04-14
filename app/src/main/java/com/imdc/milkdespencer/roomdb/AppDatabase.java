package com.imdc.milkdespencer.roomdb;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.imdc.milkdespencer.roomdb.entities.LogEntity;
import com.imdc.milkdespencer.roomdb.entities.TransactionEntity;
import com.imdc.milkdespencer.roomdb.entities.User;
import com.imdc.milkdespencer.roomdb.interfaces.LogDao;
import com.imdc.milkdespencer.roomdb.interfaces.TransactionDao;
import com.imdc.milkdespencer.roomdb.interfaces.UserDao;

import java.util.concurrent.Executors;

/// Change the version 3 to 4
@Database(entities = {User.class, TransactionEntity.class, LogEntity.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    // ✅ Migration: Add uploadToServer to transactions
    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE transactions ADD COLUMN uploadToServer INTEGER NOT NULL DEFAULT 1");
            database.execSQL("ALTER TABLE transactions ADD COLUMN remainingVolume REAL NOT NULL DEFAULT 0");
        }
    };


    private static final RoomDatabase.Callback roomCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {

                    Log.e("Admin ", "inserted");
                }
            });
        }
    };

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                     AppDatabase.class, "IDMC-MilkVending")
                    .addCallback(roomCallback)
                    .addMigrations(MIGRATION_3_4) // ✅ Important for older users
                    .enableMultiInstanceInvalidation()
                    .allowMainThreadQueries()
                    .build();
        }
        return instance;
    }

    public abstract UserDao userDao();

    public abstract TransactionDao transactionDao();

    public abstract LogDao logDao();
}