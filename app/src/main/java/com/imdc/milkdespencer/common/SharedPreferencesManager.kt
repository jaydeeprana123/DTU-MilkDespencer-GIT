package com.imdc.milkdespencer.common

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesManager private constructor(context: Context) {
    private val sharedPreferences: SharedPreferences
    private val editor: SharedPreferences.Editor

    init {
        sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
    }

    fun hasValue(key: String?): Boolean {
        return sharedPreferences.contains(key)
    }

    fun save(key: String?, value: Any?) {
        if (value is String) {
            editor.putString(key, value as String?)
        } else if (value is Float) {
            editor.putFloat(key, (value as Float?)!!)
        } else if (value is Int) {
            editor.putInt(key, (value as Int?)!!)
        } else if (value is Boolean) {
            editor.putBoolean(key, (value as Boolean?)!!)
        } // Add more cases for other data types as needed
        editor.apply()
    }

    operator fun get(key: String?, defaultValue: Any?): Any? {
        if (defaultValue is String) {
            return sharedPreferences.getString(key, defaultValue as String?)
        } else if (defaultValue is Float) {
            return sharedPreferences.getFloat(key, (defaultValue as Float?)!!)
        } else if (defaultValue is Int) {
            return sharedPreferences.getInt(key, (defaultValue as Int?)!!)
        } else if (defaultValue is Boolean) {
            return sharedPreferences.getBoolean(key, (defaultValue as Boolean?)!!)
        }
        return defaultValue
    }

    /*Delete from shared preference*/
    fun delete(key: String?) {
        if (sharedPreferences.contains(key)) {
            editor.remove(key)
            editor.apply()
        }
    }

    fun saveUsername(username: String?) {
        save("username", username)
    }

    val username: String?
        get() = get("username", "") as String?

    fun saveUserId(userId: Int) {
        save("user_id", userId)
    }

    val userId: Int
        get() = get("user_id", -1) as Int

    companion object {
        private const val PREFERENCES_NAME = "YourSharedPreferencesName"
        private var instance: SharedPreferencesManager? = null
        @JvmStatic
        @Synchronized
        fun getInstance(context: Context): SharedPreferencesManager? {
            if (instance == null) {
                instance = SharedPreferencesManager(context)
            }
            return instance
        }
    }
}
