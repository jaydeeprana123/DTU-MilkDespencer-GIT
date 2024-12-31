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

    @Query("DELETE FROM users")
    void deleteAllUsers();

    @Query("SELECT * FROM users")
    List<User> getAllUsers();

//    @Query("SELECT * FROM users where username= :username or mobile_no=:username AND password=:password")
//    User login(String username, String password);


    @Query("SELECT * FROM users WHERE (username = :username OR mobile_no = :username) AND password = :password")
    User login(String username, String password);

    @Query("SELECT Count(*) FROM users where mobile_no= :mobileNo")
    Integer mobileNoExists(String mobileNo);


    @Query("SELECT * FROM users where mobile_no=:mobileNo")
    User getUserByMobile(String mobileNo);

    @Query("SELECT * FROM users where user_type=:userType")
    User getUserByUserType(int userType);


}
