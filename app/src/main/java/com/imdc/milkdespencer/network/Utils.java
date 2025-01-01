package com.imdc.milkdespencer.network;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Patterns;

import androidx.appcompat.app.AlertDialog;

import com.imdc.milkdespencer.R;

import retrofit2.HttpException;

public class Utils {


    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static boolean isValidEmail(CharSequence target) {
        return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }

    public static boolean isValidOTP(CharSequence target) {
        return (!TextUtils.isEmpty(target) && target.length() == 6);
    }

    public static boolean isFieldRequired(CharSequence target) {
        return !TextUtils.isEmpty(target);
    }

    public static String ConvertToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    @SuppressLint("HardwareIds")
    public static String getDeviceId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }


    /*public static void downloadFile(String url, Context context, Activity activity, FirebaseAuth mAuth) {

        try {
            if (NetworkUtils.isNetworkConnected(context) && !url.isEmpty()) {
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReferenceFromUrl(url);

                ProgressDialog pd = new ProgressDialog(context);
                pd.setTitle("Application Update");
                pd.setMessage("Downloading Please Wait!");
                pd.setIndeterminate(true);
                pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                pd.show();
                File file = activity.getBaseContext().getExternalFilesDir("AutoUpdate");
                if (!file.exists()) file.mkdir();
                final File localFile = new File(file, "debug.apk");
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    Log.e(TAG, "downloadFile: " + url);
                    Log.e(TAG, "destinationFile: " + localFile);
                    storageRef.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
                        if (localFile.canRead()) {
                            pd.dismiss();
                            installAPK(localFile, context);
                        }
                        Toast.makeText(context, "Download Complete", Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
                        pd.dismiss();
                        e.printStackTrace();
                    });

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void installAPK(File localFile, Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri contentUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", new File(localFile.getAbsolutePath()));
                Intent install = new Intent(Intent.ACTION_VIEW);
                install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
                install.setData(contentUri);
                context.startActivity(install);
            } else {
                Intent install = new Intent(Intent.ACTION_VIEW);
                install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                install.setDataAndType(Uri.parse(localFile.getAbsolutePath()), "application/vnd.android.package-archive");
                context.startActivity(install);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
*/
    public static void handleApiError(Context context, Throwable e, ApiManager apiManager) {
        e.printStackTrace();
        if (e instanceof HttpException) {
            HttpException httpException = (HttpException) e;
            int statusCode = httpException.code();

            switch (statusCode) {
                case 401:
                    showAlertDialog(context, "Unauthorized Error", "You are not authorized to access this resource.", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Handle the OK button click if needed
//                            doGetTokenApiCall(context, apiManager, memoryKeyValueStore);
                            dialog.dismiss();
                        }
                    });
                    break;
                case 403:
                    showAlertDialog(context, "Forbidden Error", "You do not have permission to access this resource.", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Handle the OK button click if needed
                            dialog.dismiss();
                        }
                    });
                    break;
                default:
                    showAlertDialog(context, "HTTP Error", "An error occurred while processing your request. Please try again later.", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Handle the OK button click if needed
                            dialog.dismiss();
                        }
                    });
                    break;
            }
        } else {
            showAlertDialog(context, "Error", "An unexpected error occurred. Please try again later.", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // Handle the OK button click if needed
                    dialog.dismiss();
                }
            });
        }
    }

    public static void showAlertDialog(Context context, String title, String message, DialogInterface.OnClickListener positiveClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title).setMessage(message).setPositiveButton("OK", positiveClickListener);
        builder.create().show();
    }

    public static void showCustomAlertDialog(String title, String message, Context context) {
        try {
            new AlertDialog.Builder(context).setCancelable(false).setTitle(title).setMessage(message).setIcon(android.R.drawable.ic_dialog_alert).setPositiveButton(context.getString(R.string.ok), null).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*public static void doGetTokenApiCall(Context context, ApiManager apiManager, InMemoryKeyValueStore memoryKeyValueStore) {
        ProgressDialog pd = new ProgressDialog(context);
        pd.setTitle(context.getString(R.string.alerttitle_pleasewait));
        pd.setCancelable(false);
        pd.show();
        Log.e(TAG, "doGetTokenApiCall: " + memoryKeyValueStore.getString(Constants.AuthClientId, ""));
        RequestBody requestBody = new FormBody.Builder().add(context.getString(R.string.grant_type), context.getString(R.string.header_password)).add(context.getString(R.string.username), memoryKeyValueStore.getString(Constants.AuthClientId, "")).add(context.getString(R.string.header_password), context.getString(R.string.auth_password)).build();
        HashMap<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/x-www-form-urlencoded");
//        String url = "https://apps.radiqal.com:9652/token";
        String url = ApiEndpointsConstants.token;
        DisposableObserver<ResponseBody> disposableObserver = new DisposableObserver<ResponseBody>() {
            @Override
            public void onNext(ResponseBody response) {
                // Handle the response
                pd.dismiss();
                if (!response.toString().isEmpty()) {
                    AuthorizationResponse responseAuth = new Gson().fromJson(response.charStream(), AuthorizationResponse.class);
                    memoryKeyValueStore.saveString(Constants.AuthToken, responseAuth.getAccess_token());
                }
                Log.d("MainActivity", "Post request successful: " + memoryKeyValueStore.getString(Constants.AuthToken, ""));
            }

            @Override
            public void onError(Throwable e) {
                Log.e("MainActivity", "Post request failed: " + e.getMessage());
                pd.dismiss();
                handleApiError(context, e, apiManager, memoryKeyValueStore);
            }

            @Override
            public void onComplete() {
                // Handle completion if needed
            }
        };
        apiManager.makePostRequestCall(url, requestBody, header, disposableObserver);
    }*/

    /*public static HashMap<String, String> create_api_request_header(Context context, InMemoryKeyValueStore prefs, String url_type_value) {
        try {
            HashMap<String, String> header = new HashMap<>();
            header.put(context.getString(R.string.authorization), "Bearer " + prefs.getString(Constants.AuthToken, ""));
            header.put(context.getString(R.string.clientid), prefs.getString(Constants.AuthClientId, ""));
            header.put(context.getString(R.string.UrlType), url_type_value);
            header.put("Content-Type", "application/json");
            return header;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }*/


}
