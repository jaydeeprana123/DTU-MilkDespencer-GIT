package com.imdc.milkdespencer.roomdb.interfaces;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.imdc.milkdespencer.roomdb.entities.User;

import java.util.List;

@Dao
public interface UserDao {

    @Insert
    long insert(User user);

    @Update
    void update(User user);

    @Delete
    void delete(User user);

    @Query("SELECT * FROM users")
    List<User> getAllUsers();

    @Query("SELECT * FROM users where username= :username or mobile_no=:username AND password=:password")
    User login(String username, String password);

    @Query("SELECT Count(*) FROM users where mobile_no= :mobileNo")
    Integer mobileNoExists(String mobileNo);
}
