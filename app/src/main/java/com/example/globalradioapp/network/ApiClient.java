package com.example.globalradioapp.network;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ApiClient {

    private static final String TAG = "ApiClient";
    private static Retrofit retrofit = null;
    private static String currentBaseUrl = null;

    // Enhanced server list with fallbacks
    private static final String[] FALLBACK_SERVERS = {
        "https://de1.api.radio-browser.info/json/",
        "https://nl1.api.radio-browser.info/json/",
        "https://fr1.api.radio-browser.info/json/",
        "https://at1.api.radio-browser.info/json/"
    };

    /**
     * Enhanced DNS lookup with better error handling and fallback mechanisms
     */
    private static String getAvailableServer() {
        try {
            Log.d(TAG, "Resolving radio-browser.info servers...");
            
            Future<String[]> future = Executors.newSingleThreadExecutor().submit(() -> {
                try {
                    InetAddress[] addresses = InetAddress.getAllByName("all.api.radio-browser.info");
                    String[] serverList = new String[addresses.length];
                    for (int i = 0; i < addresses.length; i++) {
                        serverList[i] = "https://" + addresses[i].getCanonicalHostName() + "/json/";
                    }
                    return serverList;
                } catch (UnknownHostException e) {
                    Log.w(TAG, "DNS lookup failed, using fallback servers");
                    return FALLBACK_SERVERS;
                }
            });

            String[] resolvedServers = future.get(5, TimeUnit.SECONDS); // 5 second timeout
            if (resolvedServers.length > 0) {
                List<String> servers = Arrays.asList(resolvedServers);
                Collections.shuffle(servers); // Randomize selection for load balancing
                String selectedServer = servers.get(0);
                Log.d(TAG, "Selected server: " + selectedServer);
                return selectedServer;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during DNS lookup: " + e.getMessage());
        }

        // Ultimate fallback
        String fallbackServer = FALLBACK_SERVERS[0];
        Log.d(TAG, "Using fallback server: " + fallbackServer);
        return fallbackServer;
    }

    /**
     * Enhanced Retrofit client with better configuration
     */
    public static Retrofit getClient() {
        if (retrofit == null || !isCurrentServerWorking()) {
            createNewClient();
        }
        return retrofit;
    }

    private static void createNewClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        
        // Add logging interceptor
        httpClient.addInterceptor(logging);
        
        // Enhanced timeouts
        httpClient.connectTimeout(15, TimeUnit.SECONDS);
        httpClient.readTimeout(30, TimeUnit.SECONDS);
        httpClient.writeTimeout(15, TimeUnit.SECONDS);
        
        // Add retry interceptor
        httpClient.addInterceptor(chain -> {
            Request request = chain.request();
            Response response = null;
            int tryCount = 0;
            int maxTries = 3;

            while (tryCount < maxTries) {
                try {
                    response = chain.proceed(request);
                    if (response.isSuccessful()) {
                        break;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Request failed, attempt " + (tryCount + 1) + "/" + maxTries);
                    if (tryCount == maxTries - 1) {
                        throw e;
                    }
                }
                tryCount++;
                
                // Close the response if it exists
                if (response != null) {
                    response.close();
                }
                
                // Wait before retry
                try {
                    Thread.sleep(1000 * tryCount); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            return response;
        });

        // Add User-Agent header
        httpClient.addInterceptor(chain -> {
            Request original = chain.request();
            Request request = original.newBuilder()
                    .header("User-Agent", "GlobalRadioApp/1.0 (Android)")
                    .build();
            return chain.proceed(request);
        });

        String baseUrl = getAvailableServer();
        currentBaseUrl = baseUrl;

        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build();
        
        Log.i(TAG, "Retrofit client created with base URL: " + baseUrl);
    }

    /**
     * Check if current server is working (basic health check)
     */
    private static boolean isCurrentServerWorking() {
        if (currentBaseUrl == null) return false;
        
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build();
            
            Request request = new Request.Builder()
                    .url(currentBaseUrl + "countries")
                    .build();
            
            Response response = client.newCall(request).execute();
            boolean isWorking = response.isSuccessful();
            response.close();
            return isWorking;
        } catch (Exception e) {
            Log.w(TAG, "Server health check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Force refresh the client (useful when network changes)
     */
    public static void refreshClient() {
        retrofit = null;
        currentBaseUrl = null;
        Log.i(TAG, "API client refreshed");
    }

    /**
     * Get the current base URL being used
     */
    public static String getCurrentBaseUrl() {
        return currentBaseUrl;
    }

    /**
     * Provide API service instance with enhanced error handling
     */
    public static RadioApiService getRadioApiService() {
        return getClient().create(RadioApiService.class);
    }
}