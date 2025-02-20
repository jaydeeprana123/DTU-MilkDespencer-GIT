package com.imdc.milkdespencer.roomdb

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.imdc.milkdespencer.roomdb.entities.LogEntity
import com.imdc.milkdespencer.roomdb.entities.TransactionEntity
import com.imdc.milkdespencer.roomdb.entities.User
import com.imdc.milkdespencer.roomdb.interfaces.LogDao
import com.imdc.milkdespencer.roomdb.interfaces.TransactionDao
import com.imdc.milkdespencer.roomdb.interfaces.UserDao
import java.util.concurrent.Executors

@Database(
    entities = [User::class, TransactionEntity::class, LogEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao?
    abstract fun transactionDao(): TransactionDao?
    abstract fun logDao(): LogDao?

    companion object {
        private var instance: AppDatabase? = null
        private val roomCallback: Callback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Insert initial admin user when the database is created
                Executors.newSingleThreadExecutor().execute {
                    Log.e("Admin ", "inserted")

//                    UserDao userDao = instance.userDao();
//                    long userId = userDao.insert(new User("admin", "Mvb@idmc123", 0));
//                    User existUser = userDao.getUserByUserType(0);
//                    if(existUser == null){
//                        long userId = userDao.insert(new User("admin", "Mvb@idmc123", 0));
//                    }
//                    UserDao userDao = instance.userDao();
//                    userDao.insert(new User("admin", "Mvb@idmc123", 0));
                }
            }
        }

        @JvmStatic
        @Synchronized
        fun getInstance(context: Context): AppDatabase? {
            if (instance == null) {
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "IDMC-MilkVending"
                ).addCallback(
                    roomCallback
                ).fallbackToDestructiveMigrationFrom(1).enableMultiInstanceInvalidation().build()
            }
            return instance
        }
    }
}