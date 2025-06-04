package com.example.globalradioapp.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {FavoriteStation.class}, version = 1, exportSchema = false)
public abstract class RadioDatabase extends RoomDatabase {

    public abstract FavoriteStationDao favoriteStationDao();

    private static volatile RadioDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static RadioDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (RadioDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    RadioDatabase.class, "radio_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
