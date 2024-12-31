package com.imdc.milkdespencer.network;


import java.util.HashMap;

import io.reactivex.rxjava3.core.Observable;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface ApiService {

    @GET()
    Observable<ResponseBody> doGetRequest(@Url String url);

    @POST()
    Observable<ResponseBody> doPostRequest(@Url String url, @Body RequestBody requestBody, @HeaderMap HashMap<String, String> header_map);

    @FormUrlEncoded
    @POST()
    Observable<ResponseBody> sendOtp(@Url String url, @FieldMap HashMap<String, String> fields, @HeaderMap HashMap<String, String> header_map);

}