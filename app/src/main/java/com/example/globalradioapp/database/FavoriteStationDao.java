package com.example.globalradioapp.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FavoriteStationDao {

    @Query("SELECT * FROM favorite_stations ORDER BY dateAdded DESC")
    LiveData<List<FavoriteStation>> getAllFavorites();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FavoriteStation favoriteStation);

    @Delete
    void delete(FavoriteStation favoriteStation);

    @Query("DELETE FROM favorite_stations WHERE stationId = :stationId")
    void deleteByStationId(String stationId);

    @Query("SELECT COUNT(*) FROM favorite_stations WHERE stationId = :stationId")
    int isFavorite(String stationId);
}
