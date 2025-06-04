package com.example.globalradioapp.network;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ApiClient {

    private static Retrofit retrofit = null;

    // DNS Lookup: Fetch available API servers asynchronously
    private static String getAvailableServer() {
        try {
            Future<String[]> future = Executors.newSingleThreadExecutor().submit(() -> {
                InetAddress[] addresses = InetAddress.getAllByName("all.api.radio-browser.info");
                String[] serverList = new String[addresses.length];
                for (int i = 0; i < addresses.length; i++) {
                    serverList[i] = "https://" + addresses[i].getCanonicalHostName() + "/json/";
                }
                return serverList;
            });

            String[] resolvedServers = future.get(); // Wait for the result
            if (resolvedServers.length > 0) {
                List<String> servers = Arrays.asList(resolvedServers);
                Collections.shuffle(servers); // Randomize selection
                return servers.get(0); // Pick a working server
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Fallback to a known working server if DNS lookup fails
        return "https://de1.api.radio-browser.info/json/";
    }

    // Retrofit Client Initialization
    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
            httpClient.addInterceptor(logging);
            httpClient.connectTimeout(30, TimeUnit.SECONDS);
            httpClient.readTimeout(30, TimeUnit.SECONDS);
            httpClient.writeTimeout(30, TimeUnit.SECONDS);

            String baseUrl = getAvailableServer(); // Dynamically fetch API server

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl) // Use the dynamically resolved server
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClient.build())
                    .build();
        }
        return retrofit;
    }

    // Provide API service instance
    public static RadioApiService getRadioApiService() {
        return getClient().create(RadioApiService.class);
    }
}
