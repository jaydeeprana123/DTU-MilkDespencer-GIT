package com.imdc.milkdespencer.roomdb;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.imdc.milkdespencer.roomdb.entities.LogEntity;
import com.imdc.milkdespencer.roomdb.entities.TransactionEntity;
import com.imdc.milkdespencer.roomdb.entities.User;
import com.imdc.milkdespencer.roomdb.interfaces.LogDao;
import com.imdc.milkdespencer.roomdb.interfaces.TransactionDao;
import com.imdc.milkdespencer.roomdb.interfaces.UserDao;

import java.util.concurrent.Executors;

@Database(entities = {User.class, TransactionEntity.class, LogEntity.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;
    private static final RoomDatabase.Callback roomCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            // Insert initial admin user when the database is created
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    UserDao userDao = instance.userDao();
                    userDao.insert(new User("admin", "Admin@123", 0));
                }
            });
        }
    };

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "IDMC-MilkVending").addCallback(roomCallback).fallbackToDestructiveMigrationFrom(1).enableMultiInstanceInvalidation().build();
        }
        return instance;
    }

    public abstract UserDao userDao();

    public abstract TransactionDao transactionDao();

    public abstract LogDao logDao();
}