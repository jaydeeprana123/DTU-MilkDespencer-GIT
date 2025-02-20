package com.imdc.milkdespencer.network

import com.google.gson.Gson
import com.imdc.milkdespencer.common.Constants
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private var retrofit: Retrofit? = null
    fun getClient(gson: Gson?): Retrofit? {
        if (retrofit == null) {
            retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .client(OkHttpClient().newBuilder().connectTimeout(5, TimeUnit.MINUTES).build())
                .build()
        }
        return retrofit
    }
}
