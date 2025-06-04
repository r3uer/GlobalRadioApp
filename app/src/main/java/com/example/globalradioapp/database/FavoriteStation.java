package com.example.globalradioapp.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull; // Import NonNull

@Entity(tableName = "favorite_stations")
public class FavoriteStation {
    @PrimaryKey
    @NonNull // Add this annotation
    private String stationId;
    private String name;
    private String url;
    private String country;
    private String favicon;
    private long dateAdded;

    public FavoriteStation(@NonNull String stationId, String name, String url, String country, String favicon) { // Also consider adding @NonNull to the constructor parameter
        this.stationId = stationId;
        this.name = name;
        this.url = url;
        this.country = country;
        this.favicon = favicon;
        this.dateAdded = System.currentTimeMillis();
    }

    // Getters and Setters
    @NonNull // It's good practice to also annotate the getter
    public String getStationId() { return stationId; }
    public void setStationId(@NonNull String stationId) { this.stationId = stationId; } // And the setter parameter

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getFavicon() { return favicon; }
    public void setFavicon(String favicon) { this.favicon = favicon; }

    public long getDateAdded() { return dateAdded; }
    public void setDateAdded(long dateAdded) { this.dateAdded = dateAdded; }
}