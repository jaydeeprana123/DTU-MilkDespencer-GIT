package com.imdc.milkdespencer.common;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesManager {

    private static final String PREFERENCES_NAME = "YourSharedPreferencesName";
    private static SharedPreferencesManager instance;
    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    private SharedPreferencesManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public static synchronized SharedPreferencesManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPreferencesManager(context);
        }
        return instance;
    }



    public boolean hasValue(String key) {
        return sharedPreferences.contains(key);
    }

    public void save(String key, Object value) {
        if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } // Add more cases for other data types as needed

        editor.apply();
    }
    public Object get(String key, Object defaultValue) {
        if (defaultValue instanceof String) {
            return sharedPreferences.getString(key, (String) defaultValue);
        } else if (defaultValue instanceof Float) {
            return sharedPreferences.getFloat(key, (Float) defaultValue);
        } else if (defaultValue instanceof Integer) {
            return sharedPreferences.getInt(key, (Integer) defaultValue);
        } else if (defaultValue instanceof Boolean) {
            return sharedPreferences.getBoolean(key, (Boolean) defaultValue);
        }

        return defaultValue;
    }


    /*Delete from shared preference*/
    public void delete(String key) {
        if (sharedPreferences.contains(key)) {
            editor.remove(key);
            editor.apply();
        }
    }


    public void saveUsername(String username) {
        save("username", username);
    }

    public String getUsername() {
        return (String) get("username", "");
    }

    public void saveUserId(int userId) {
        save("user_id", userId);
    }

    public int getUserId() {
        return (int) get("user_id", -1);
    }
}
