package com.example.globalradioapp.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkUtils {
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
        return false;
    }

    public static String getErrorMessage(Throwable t) {
        if (t == null) return "Unknown error";
        return t.getMessage() != null ? t.getMessage() : "Unknown network error";
    }
}