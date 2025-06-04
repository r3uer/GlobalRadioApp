package com.example.globalradioapp.network;

import com.example.globalradioapp.model.RadioStation;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Path;

public interface RadioApiService {

    @GET("stations/bycountry/{country}")
    Call<List<RadioStation>> getStationsByCountry(@Path("country") String country);


    @GET("stations/bylanguage/{language}")
    Call<List<RadioStation>> getStationsByLanguage(@Path("language") String language);

    @GET("stations/search")
    Call<List<RadioStation>> searchStations(@Query("name") String name);

    @GET("stations/topvote")
    Call<List<RadioStation>> getTopStations(@Query("limit") int limit);

    @GET("stations/bytag/{tag}")
    Call<List<RadioStation>> getStationsByCategory(@Path("tag") String tag);
}
