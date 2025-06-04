package com.example.globalradioapp.viewmodel;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.globalradioapp.database.FavoriteStation;
import com.example.globalradioapp.database.RadioDatabase;
import com.example.globalradioapp.database.FavoriteStationDao;

import java.util.List;

public class RadioViewModel extends AndroidViewModel {

    private FavoriteStationDao favoriteStationDao;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>("");

    private LiveData<List<FavoriteStation>> allFavorites;

    public RadioViewModel(@NonNull Application application) {
        super(application);
        RadioDatabase db = RadioDatabase.getDatabase(application);
        favoriteStationDao = db.favoriteStationDao();
        allFavorites = favoriteStationDao.getAllFavorites();
    }

    public LiveData<List<FavoriteStation>> getFavoriteStations() {
        return allFavorites;
    }

    public void addFavorite(FavoriteStation favoriteStation) {
        RadioDatabase.databaseWriteExecutor.execute(() -> {
            favoriteStationDao.insert(favoriteStation);
        });
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    public void removeFavorite(String stationId) {
        RadioDatabase.databaseWriteExecutor.execute(() -> {
            favoriteStationDao.deleteByStationId(stationId);
        });
    }
}
