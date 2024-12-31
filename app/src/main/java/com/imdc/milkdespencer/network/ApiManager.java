package com.imdc.milkdespencer.network;

import java.util.HashMap;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;


public class ApiManager {
    private final ApiService apiService;

    public ApiManager(ApiService apiService) {
        this.apiService = apiService;
    }

    public void makeGetResponseCall(String url, DisposableObserver<ResponseBody> observer) {
        Observable<ResponseBody> observable = apiService.doGetRequest(url);
        makeApiCall(observable, observer);
    }

    public void makePostRequestCall(String url, RequestBody requestBody, HashMap<String, String> headers, DisposableObserver<ResponseBody> observer) {
        Observable<ResponseBody> observable = apiService.doPostRequest(url, requestBody, headers);
        makeApiCall(observable, observer);
    }

    public void makeOTPRequestCall(String url, HashMap<String, String> fields, HashMap<String, String> headers, DisposableObserver<ResponseBody> observer) {
        Observable<ResponseBody> observable = apiService.sendOtp(url, fields, headers);
        makeApiCall(observable, observer);
    }


    private <T> void makeApiCall(Observable<T> observable, DisposableObserver<T> observer) {
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(observer);
    }
}
