package com.example.globalradioapp.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkCapabilities;
import android.os.Build;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.io.IOException;

public class NetworkUtils {
    
    /**
     * Enhanced network availability check with better API support
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (cm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
                return capabilities != null && 
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                     capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                     capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            } else {
                NetworkInfo info = cm.getActiveNetworkInfo();
                return info != null && info.isConnected();
            }
        }
        return false;
    }
    
    /**
     * Enhanced error message handling with specific error types
     */
    public static String getErrorMessage(Throwable t) {
        if (t == null) return "Unknown error occurred";
        
        if (t instanceof UnknownHostException) {
            return "No internet connection. Please check your network settings.";
        } else if (t instanceof SocketTimeoutException) {
            return "Connection timeout. Please try again.";
        } else if (t instanceof IOException) {
            return "Network error. Please check your connection.";
        } else if (t.getMessage() != null) {
            String message = t.getMessage().toLowerCase();
            if (message.contains("timeout")) {
                return "Request timeout. Please try again.";
            } else if (message.contains("failed to connect")) {
                return "Failed to connect to server. Please try again.";
            } else if (message.contains("unable to resolve host")) {
                return "Cannot reach server. Please check your internet connection.";
            }
            return t.getMessage();
        }
        
        return "An unexpected error occurred. Please try again.";
    }
    
    /**
     * Get network type for analytics or display purposes
     */
    public static String getNetworkType(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (cm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
                if (capabilities != null) {
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        return "WiFi";
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        return "Mobile Data";
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                        return "Ethernet";
                    }
                }
            } else {
                NetworkInfo info = cm.getActiveNetworkInfo();
                if (info != null && info.isConnected()) {
                    return info.getTypeName();
                }
            }
        }
        return "Unknown";
    }
    
    /**
     * Check if connection is metered (useful for data usage warnings)
     */
    public static boolean isMeteredConnection(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (cm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
                return capabilities != null && !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
            } else {
                return cm.isActiveNetworkMetered();
            }
        }
        return true; // Default to metered for safety
    }
}