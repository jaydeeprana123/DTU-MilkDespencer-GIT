//package com.imdc.milkdespencer;
//
//import static com.imdc.milkdespencer.common.Constants.ApiBaseUrl;
//
//import android.content.Context;
//
//import androidx.annotation.NonNull;
//import androidx.work.Worker;
//import androidx.work.WorkerParameters;
//
//import com.google.gson.Gson;
//import com.imdc.milkdespencer.common.SharedPreferencesManager;
//import com.imdc.milkdespencer.network.ApiManager;
//import com.imdc.milkdespencer.network.ApiService;
//import com.imdc.milkdespencer.roomdb.entities.TransactionEntity;
//
//import java.util.HashMap;
//import java.util.concurrent.CountDownLatch;
//
//import io.reactivex.rxjava3.observers.DisposableObserver;
//import okhttp3.MediaType;
//import okhttp3.RequestBody;
//import okhttp3.ResponseBody;
//import retrofit2.Retrofit;
//import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
//import retrofit2.converter.gson.GsonConverterFactory;
//
//public class TransactionPostWorker extends Worker {
//
//    public TransactionPostWorker(@NonNull Context context, @NonNull WorkerParameters params) {
//        super(context, params);
//    }
//
//    @NonNull
//    @Override
//    public Result doWork() {
//        try {
//            String transactionJson = getInputData().getString("transaction_json");
//
//            if (transactionJson == null) return Result.failure();
//
//            TransactionEntity transaction = new Gson().fromJson(transactionJson, TransactionEntity.class);
//
//            SharedPreferencesManager preferencesManager = new SharedPreferencesManager(getApplicationContext());
//            String url = "/api/Transaction/PostTransaction";
//            String baseUrl = (preferencesManager.get(ApiBaseUrl, "https://portal.idmc.coop:5151/")).toString();
//
//            Retrofit retrofit = new Retrofit.Builder()
//                    .baseUrl(baseUrl)
//                    .addConverterFactory(GsonConverterFactory.create())
//                    .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
//                    .build();
//
//            ApiService apiService = retrofit.create(ApiService.class);
//            ApiManager apiManager = new ApiManager(apiService);
//
//            String request = new Gson().toJson(transaction);
//            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), request);
//
//            HashMap<String, String> header = new HashMap<>();
//            header.put("Content-Type", "application/json");
//
//            CountDownLatch latch = new CountDownLatch(1);
//
//            apiManager.makePostRequestCall(url, requestBody, header, new DisposableObserver<ResponseBody>() {
//                @Override
//                public void onNext(@NonNull ResponseBody responseBody) {
//                    latch.countDown();
//                }
//
//                @Override
//                public void onError(@NonNull Throwable e) {
//                    latch.countDown(); // still release the lock
//                }
//
//                @Override
//                public void onComplete() {
//                    latch.countDown(); // just in case
//                }
//            });
//
//            latch.await(); // Wait for network response
//
//            return Result.success();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return Result.failure();
//        }
//    }
//}
