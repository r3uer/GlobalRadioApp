// com.example.globalradioapp.network.ApiMirrorResolver
package com.example.globalradioapp.network;

import android.util.Log;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class ApiMirrorResolver {

    private static final String[] API_ENDPOINTS = {
            "https://nl1.api.radio-browser.info/json/",
            "https://at1.api.radio-browser.info/json/",
            "https://de1.api.radio-browser.info/json/"
    };

    public static String getWorkingBaseUrl() {
        for (String url : API_ENDPOINTS) {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url + "stations/topvote?limit=1").openConnection();
                connection.setConnectTimeout(3000);
                connection.setReadTimeout(3000);
                connection.setRequestMethod("GET");
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    Log.d("MirrorCheck", "Working mirror: " + url);
                    return url;
                }
            } catch (IOException e) {
                Log.w("MirrorCheck", "Failed: " + url);
            }
        }
        return API_ENDPOINTS[0]; // fallback to first
    }
}
