package com.imdc.milkdespencer.Workers;

import static com.imdc.milkdespencer.common.Constants.KEY_TEMPERATURE;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;
import com.imdc.milkdespencer.MainActivity;
import com.imdc.milkdespencer.PayWithQrActivity;
import com.imdc.milkdespencer.common.Constants;
import com.imdc.milkdespencer.common.SharedPreferencesManager;
import com.imdc.milkdespencer.models.ResponseTempStatus;

public class PeriodicWorkerForTemperatureApiCall extends Worker {

    SharedPreferencesManager preferencesManager;

    public PeriodicWorkerForTemperatureApiCall(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);

        preferencesManager = SharedPreferencesManager.getInstance(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Access SharedPreferences


        // Fetch preferences
        String tempStatusJson = preferencesManager.get(Constants.ResponseTempStatus, "").toString();
        if (!tempStatusJson.isEmpty()) {
            ResponseTempStatus responseTempStatus = new Gson().fromJson(tempStatusJson, ResponseTempStatus.class);

            float offSet = Float.parseFloat(preferencesManager.get(Constants.TemperatureOffSet, "0.0").toString());

            // Calculate current temperature of milk
            double currentSavedTemp = responseTempStatus.getTemperature() / 10.0;
            float currentTemperature = (float) (currentSavedTemp + offSet);

            /// Call api
            try {
                Constants.saveLogs(getApplicationContext(), String.valueOf(currentTemperature),KEY_TEMPERATURE );


                Log.e("save log temprature", String.valueOf(currentTemperature));


            } catch (Exception e) {

                // Don't crash, just log the error silently
            }


        }




        return Result.success();
    }
}
