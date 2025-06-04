package com.example.globalradioapp.network;

import com.example.globalradioapp.model.RadioStation;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Path;

public interface RadioApiService {

    /**
     * Get radio stations by country
     */
    @GET("stations/bycountry/{country}")
    Call<List<RadioStation>> getStationsByCountry(@Path("country") String country);

    /**
     * Get radio stations by language
     */
    @GET("stations/bylanguage/{language}")
    Call<List<RadioStation>> getStationsByLanguage(@Path("language") String language);

    /**
     * Search radio stations by name with enhanced parameters
     */
    @GET("stations/search")
    Call<List<RadioStation>> searchStations(
        @Query("name") String name,
        @Query("limit") int limit
    );

    /**
     * Enhanced search with multiple parameters
     */
    @GET("stations/search")
    Call<List<RadioStation>> searchStationsAdvanced(
        @Query("name") String name,
        @Query("country") String country,
        @Query("language") String language,
        @Query("tag") String tag,
        @Query("limit") int limit
    );

    /**
     * Get top voted stations with limit
     */
    @GET("stations/topvote")
    Call<List<RadioStation>> getTopStations(@Query("limit") int limit);

    /**
     * Get stations by category/tag
     */
    @GET("stations/bytag/{tag}")
    Call<List<RadioStation>> getStationsByCategory(@Path("tag") String tag);

    /**
     * Get stations by category with limit
     */
    @GET("stations/bytag/{tag}")
    Call<List<RadioStation>> getStationsByCategory(
        @Path("tag") String tag, 
        @Query("limit") int limit
    );

    /**
     * Get recently added stations
     */
    @GET("stations/lastchange")
    Call<List<RadioStation>> getRecentStations(@Query("limit") int limit);

    /**
     * Get stations by state/region
     */
    @GET("stations/bystate/{state}")
    Call<List<RadioStation>> getStationsByState(@Path("state") String state);

    /**
     * Get all countries
     */
    @GET("countries")
    Call<List<String>> getAllCountries();

    /**
     * Get all languages
     */
    @GET("languages")
    Call<List<String>> getAllLanguages();

    /**
     * Get all tags/genres
     */
    @GET("tags")
    Call<List<String>> getAllTags();
}