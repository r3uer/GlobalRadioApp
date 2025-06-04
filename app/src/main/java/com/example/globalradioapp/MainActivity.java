package com.example.globalradioapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import androidx.appcompat.widget.SearchView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import com.example.globalradioapp.adapter.RadioStationAdapter;
import com.example.globalradioapp.database.FavoriteStation;
import com.example.globalradioapp.dialog.SleepTimerDialog;
import com.example.globalradioapp.model.RadioStation;
import com.example.globalradioapp.network.ApiClient;
import com.example.globalradioapp.network.RadioApiService;
import com.example.globalradioapp.service.RadioPlayerService;
import com.example.globalradioapp.network.NetworkUtils;
import com.example.globalradioapp.viewmodel.RadioViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements RadioStationAdapter.OnStationClickListener {

    private static final String TAG = "MainActivity";
    private static final int SEARCH_DELAY_MS = 500;
    private static final int MAX_VOLUME = 100;
    private static final int VOLUME_STEP = 10;

    // Views
    private RecyclerView recyclerViewStations;
    private RadioStationAdapter adapter;
    private ProgressBar progressBarLoading;
    private MaterialCardView playerControlsCard;
    private FloatingActionButton fabPlayPause;
    private TextView textViewCurrentStation, textViewCurrentCountry, textViewError;
    private ImageView imageViewStationLogo;
    private ImageButton buttonFavorite, buttonSleepTimer, buttonVolumeUp, buttonVolumeDown;
    private SeekBar seekBarVolume;
    private SearchView searchView;
    private ChipGroup categoryChipGroup;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Service and state
    private RadioPlayerService playerService;
    private boolean isServiceBound = false;
    private RadioStation currentStation;
    private PlayerState playerState = PlayerState.STOPPED;
    private RadioViewModel viewModel;
    private RadioApiService apiService;

    // Search handling
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private String lastSearchQuery = "";
    private static final String PREFERRED_COUNTRY = "India";

    // Current API call to cancel if needed
    private Call<List<RadioStation>> currentCall;

    public enum PlayerState {
        STOPPED, PLAYING, PAUSED, LOADING, ERROR
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service connected");
            RadioPlayerService.LocalBinder binder = (RadioPlayerService.LocalBinder) service;
            playerService = binder.getService();
            isServiceBound = true;

            // Set up service callbacks
            setupServiceCallbacks();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service disconnected");
            isServiceBound = false;
            playerService = null;
        }
    };
    private List<RadioStation> filterStationsByCountry(List<RadioStation> stations, String country) {
        List<RadioStation> filtered = new ArrayList<>();
        for (RadioStation station : stations) {
            if (country.equalsIgnoreCase(station.getCountry())) {
                filtered.add(station);
            }
        }
        return filtered;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeComponents();
        checkNetworkAndLoad();
    }

    private void initializeComponents() {
        initViews();
        setupRecyclerView();
        setupViewModelObservers();
        setupClickListeners();
        setupSearch();
        setupCategoryChips();
        setupSwipeRefresh();
    }

    private void initViews() {
        recyclerViewStations = findViewById(R.id.recyclerViewStations);
        progressBarLoading = findViewById(R.id.progressBarLoading);
        playerControlsCard = findViewById(R.id.playerControlsCard);
        fabPlayPause = findViewById(R.id.fabPlayPause);
        textViewCurrentStation = findViewById(R.id.textViewCurrentStation);
        textViewCurrentCountry = findViewById(R.id.textViewCurrentCountry);
        textViewError = findViewById(R.id.textViewError);
        imageViewStationLogo = findViewById(R.id.imageViewStationLogo);
        buttonFavorite = findViewById(R.id.buttonFavorite);
        buttonSleepTimer = findViewById(R.id.buttonSleepTimer);
        buttonVolumeUp = findViewById(R.id.buttonVolumeUp);
        buttonVolumeDown = findViewById(R.id.buttonVolumeDown);
        seekBarVolume = findViewById(R.id.seekBarVolume);
        searchView = findViewById(R.id.searchView);
        categoryChipGroup = findViewById(R.id.categoryChipGroup);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        viewModel = new ViewModelProvider(this).get(RadioViewModel.class);
        apiService = ApiClient.getRadioApiService();
        EditText searchEditText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        searchEditText.setTextColor(getResources().getColor(R.color.white)); // Light text
        searchEditText.setHintTextColor(getResources().getColor(R.color.white)); // Light hint, or use a lighter gray if you prefer
        // Initialize volume
        seekBarVolume.setProgress(70); // Default volume
    }

    private void setupRecyclerView() {
        adapter = new RadioStationAdapter(new ArrayList<>(), this);
        recyclerViewStations.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewStations.setAdapter(adapter);

        // Add item decoration for better spacing
        int spacing = getResources().getDimensionPixelSize(R.dimen.item_spacing);
        recyclerViewStations.addItemDecoration(new SpacingItemDecoration(spacing));
    }

    private void setupViewModelObservers() {
        viewModel.getFavoriteStations().observe(this, favoriteStations -> {
            if (adapter != null) {
                adapter.updateFavorites(favoriteStations);
            }
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            showLoading(isLoading);
        });

        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (!TextUtils.isEmpty(errorMessage)) {
                showError(errorMessage);
            }
        });
    }

    private void setupClickListeners() {
        fabPlayPause.setOnClickListener(v -> togglePlayPause());
        buttonFavorite.setOnClickListener(v -> toggleFavorite());
        buttonSleepTimer.setOnClickListener(v -> showSleepTimerDialog());
        buttonVolumeUp.setOnClickListener(v -> adjustVolume(true));
        buttonVolumeDown.setOnClickListener(v -> adjustVolume(false));

        seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && isServiceBound && playerService != null) {
                    playerService.setVolume(progress / 100.0f);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                handleSearchTextChange(newText);
                return true;
            }
        });
    }

    private void handleSearchTextChange(String newText) {
        // Cancel previous search
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }

        if (TextUtils.isEmpty(newText)) {
            loadPopularStations();
            return;
        }

        if (newText.length() < 2) {
            return;
        }

        // Schedule new search with delay
        searchRunnable = () -> {
            if (!newText.equals(lastSearchQuery)) {
                performSearch(newText);
                lastSearchQuery = newText;
            }
        };
        searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
    }

    private void setupCategoryChips() {
        categoryChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            int checkedId = checkedIds.get(0);
            String category = getCategoryFromChipId(checkedId);

            if ("popular".equals(category)) {
                loadPopularStations();
            } else if (!TextUtils.isEmpty(category)) {
                loadStationsByCategory(category);
            }
        });
    }

    private String getCategoryFromChipId(int chipId) {
        if (chipId == R.id.chipPopular) return "popular";
        if (chipId == R.id.chipRock) return "rock";
        if (chipId == R.id.chipPop) return "pop";
        if (chipId == R.id.chipJazz) return "jazz";
        if (chipId == R.id.chipClassical) return "classical";
        if (chipId == R.id.chipNews) return "news";
        return "";
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::refreshCurrentContent);
            swipeRefreshLayout.setColorSchemeResources(
                    android.R.color.holo_blue_bright,
                    android.R.color.holo_green_light,
                    android.R.color.holo_orange_light,
                    android.R.color.holo_red_light
            );
        }
    }

    private void setupServiceCallbacks() {
        if (playerService != null) {
            playerService.setPlaybackCallback(new RadioPlayerService.PlaybackCallback() {
                @Override
                public void onPlaybackStarted() {
                    runOnUiThread(() -> {
                        playerState = PlayerState.PLAYING;
                        updatePlayerUI();
                    });
                }

                @Override
                public void onPlaybackPaused() {
                    runOnUiThread(() -> {
                        playerState = PlayerState.PAUSED;
                        updatePlayerUI();
                    });
                }

                @Override
                public void onPlaybackStopped() {
                    runOnUiThread(() -> {
                        playerState = PlayerState.STOPPED;
                        updatePlayerUI();
                    });
                }

                @Override
                public void onPlaybackError(String error) {
                    runOnUiThread(() -> {
                        playerState = PlayerState.ERROR;
                        showError("Playback error: " + error);
                        updatePlayerUI();
                    });
                }
            });
        }
    }

    private void checkNetworkAndLoad() {
        if (NetworkUtils.isNetworkAvailable(this)) {
            loadPopularStations();
        } else {
            showNetworkError();
        }
    }

    private void refreshCurrentContent() {
        // Get currently selected chip
        int checkedChipId = categoryChipGroup.getCheckedChipId();
        if (checkedChipId != View.NO_ID) {
            String category = getCategoryFromChipId(checkedChipId);
            if ("popular".equals(category)) {
                loadPopularStations();
            } else if (!TextUtils.isEmpty(category)) {
                loadStationsByCategory(category);
            }
        } else {
            loadPopularStations();
        }
    }

    private void loadPopularStations() { //for global
        cancelCurrentCall();

        if (!NetworkUtils.isNetworkAvailable(this)) {
            showNetworkError();
            return;
        }

        showLoading(true);
        hideError();

        Log.d(TAG, "Loading popular stations...");
        currentCall = apiService.getTopStations(20);
//        currentCall = apiService.getStationsByCountry("india");
        currentCall.enqueue(new Callback<List<RadioStation>>() {
            @Override
            public void onResponse(Call<List<RadioStation>> call, Response<List<RadioStation>> response) {
                if (call.isCanceled()) return;

                showLoading(false);
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }

                if (response.isSuccessful() && response.body() != null) {
                    List<RadioStation> stations = response.body();
                    Log.d(TAG, "Loaded " + stations.size() + " popular stations");

                    if (stations.isEmpty()) {
                        showEmptyState();
                    } else {
                        adapter.updateStations(stations);
                        hideError();
                    }
                } else {
                    Log.e(TAG, "Failed to load popular stations: " + response.code());
                    showError("Failed to load stations (Error: " + response.code() + ")");
                }
                currentCall = null;
            }

            @Override
            public void onFailure(Call<List<RadioStation>> call, Throwable t) {
                if (call.isCanceled()) return;

                Log.e(TAG, "Network error loading popular stations", t);
                showLoading(false);
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }

                String errorMessage = NetworkUtils.getErrorMessage(t);
                showError("Network error: " + errorMessage);
                currentCall = null;
            }
        });
    }
//private void loadPopularStations() { //for india
//    cancelCurrentCall();
//
//    if (!NetworkUtils.isNetworkAvailable(this)) {
//        showNetworkError();
//        return;
//    }
//
//    showLoading(true);
//    hideError();
//
//    Log.d(TAG, "Loading popular stations...");
//    currentCall = apiService.getTopStations(10000); // Get more to ensure enough Indian stations
//    currentCall.enqueue(new Callback<List<RadioStation>>() {
//        @Override
//        public void onResponse(Call<List<RadioStation>> call, Response<List<RadioStation>> response) {
//            if (call.isCanceled()) return;
//
//            showLoading(false);
//            if (swipeRefreshLayout != null) {
//                swipeRefreshLayout.setRefreshing(false);
//            }
//
//            if (response.isSuccessful() && response.body() != null) {
//                List<RadioStation> stations = response.body();
//                Log.d(TAG, "Loaded " + stations.size() + " popular stations");
//
//                // Filter for India
//                List<RadioStation> indianStations = new ArrayList<>();
//                for (RadioStation station : stations) {
//                    if ("India".equalsIgnoreCase(station.getCountry())) {
//                        indianStations.add(station);
//                    }
//                }
//
//                if (indianStations.isEmpty()) {
//                    showEmptyState("No popular stations from India found");
//                } else {
//                    adapter.updateStations(indianStations);
//                    hideError();
//                }
//            } else {
//                Log.e(TAG, "Failed to load popular stations: " + response.code());
//                showError("Failed to load stations (Error: " + response.code() + ")");
//            }
//            currentCall = null;
//        }
//
//        @Override
//        public void onFailure(Call<List<RadioStation>> call, Throwable t) {
//            if (call.isCanceled()) return;
//
//            Log.e(TAG, "Network error loading popular stations", t);
//            showLoading(false);
//            if (swipeRefreshLayout != null) {
//                swipeRefreshLayout.setRefreshing(false);
//            }
//
//            String errorMessage = NetworkUtils.getErrorMessage(t);
//            showError("Network error: " + errorMessage);
//            currentCall = null;
//        }
//    });
//}
    private void loadStationsByCategory(String category) {
        cancelCurrentCall();

        if (!NetworkUtils.isNetworkAvailable(this)) {
            showNetworkError();
            return;
        }

        showLoading(true);
        hideError();

        Log.d(TAG, "Loading stations for category: " + category);
        currentCall = apiService.getStationsByCategory(category);
        currentCall.enqueue(new Callback<List<RadioStation>>() {
            @Override
            public void onResponse(Call<List<RadioStation>> call, Response<List<RadioStation>> response) {
                if (call.isCanceled()) return;

                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<RadioStation> stations = response.body();
                    if (stations.isEmpty()) {
                        showEmptyState("No " + category + " stations found");
                    } else {
                        adapter.updateStations(stations);
                        hideError();
                    }
                } else {
                    showError("Failed to load " + category + " stations");
                }
                currentCall = null;
            }

            @Override
            public void onFailure(Call<List<RadioStation>> call, Throwable t) {
                if (call.isCanceled()) return;

                Log.e(TAG, "Error loading category stations", t);
                showLoading(false);
                showError("Failed to load " + category + " stations");
                currentCall = null;
            }
        });
    }

    private void performSearch(String query) {
        if (TextUtils.isEmpty(query.trim())) {
            loadPopularStations();
            return;
        }

        cancelCurrentCall();

        if (!NetworkUtils.isNetworkAvailable(this)) {
            showNetworkError();
            return;
        }

        showLoading(true);
        hideError();

        Log.d(TAG, "Searching for: " + query);
        currentCall = apiService.searchStations(query.trim());
        currentCall.enqueue(new Callback<List<RadioStation>>() {
            @Override
            public void onResponse(Call<List<RadioStation>> call, Response<List<RadioStation>> response) {
                if (call.isCanceled()) return;

                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<RadioStation> stations = response.body();
                    if (stations.isEmpty()) {
                        showEmptyState("No stations found for \"" + query + "\"");
                    } else {
                        adapter.updateStations(stations);
                        hideError();
                    }
                } else {
                    showError("Search failed");
                }
                currentCall = null;
            }

            @Override
            public void onFailure(Call<List<RadioStation>> call, Throwable t) {
                if (call.isCanceled()) return;

                Log.e(TAG, "Search error", t);
                showLoading(false);
                showError("Search failed: " + NetworkUtils.getErrorMessage(t));
                currentCall = null;
            }
        });
    }
//
//private void loadPopularStations() {
//    cancelCurrentCall();
//
//    if (!NetworkUtils.isNetworkAvailable(this)) {
//        showNetworkError();
//        return;
//    }
//
//    showLoading(true);
//    hideError();
//
//    Log.d(TAG, "Loading popular stations...");
//    currentCall = apiService.getTopStations(10000); // get more for better filtering
//    currentCall.enqueue(new Callback<List<RadioStation>>() {
//        @Override
//        public void onResponse(Call<List<RadioStation>> call, Response<List<RadioStation>> response) {
//            if (call.isCanceled()) return;
//
//            showLoading(false);
//            if (swipeRefreshLayout != null) {
//                swipeRefreshLayout.setRefreshing(false);
//            }
//
//            if (response.isSuccessful() && response.body() != null) {
//                List<RadioStation> stations = filterStationsByCountry(response.body(), PREFERRED_COUNTRY);
//                Log.d(TAG, "Loaded " + stations.size() + " popular stations (filtered for " + PREFERRED_COUNTRY + ")");
//
//                if (stations.isEmpty()) {
//                    showEmptyState("No popular stations from " + PREFERRED_COUNTRY + " found");
//                } else {
//                    adapter.updateStations(stations);
//                    hideError();
//                }
//            } else {
//                Log.e(TAG, "Failed to load popular stations: " + response.code());
//                showError("Failed to load stations (Error: " + response.code() + ")");
//            }
//            currentCall = null;
//        }
//
//        @Override
//        public void onFailure(Call<List<RadioStation>> call, Throwable t) {
//            if (call.isCanceled()) return;
//
//            Log.e(TAG, "Network error loading popular stations", t);
//            showLoading(false);
//            if (swipeRefreshLayout != null) {
//                swipeRefreshLayout.setRefreshing(false);
//            }
//
//            String errorMessage = NetworkUtils.getErrorMessage(t);
//            showError("Network error: " + errorMessage);
//            currentCall = null;
//        }
//    });
//}
//
//    private void loadStationsByCategory(String category) {
//        cancelCurrentCall();
//
//        if (!NetworkUtils.isNetworkAvailable(this)) {
//            showNetworkError();
//            return;
//        }
//
//        showLoading(true);
//        hideError();
//
//        Log.d(TAG, "Loading stations for category: " + category);
//        currentCall = apiService.getStationsByCategory(category);
//        currentCall.enqueue(new Callback<List<RadioStation>>() {
//            @Override
//            public void onResponse(Call<List<RadioStation>> call, Response<List<RadioStation>> response) {
//                if (call.isCanceled()) return;
//
//                showLoading(false);
//                if (response.isSuccessful() && response.body() != null) {
//                    List<RadioStation> stations = filterStationsByCountry(response.body(), PREFERRED_COUNTRY);
//                    if (stations.isEmpty()) {
//                        showEmptyState("No " + category + " stations found in " + PREFERRED_COUNTRY);
//                    } else {
//                        adapter.updateStations(stations);
//                        hideError();
//                    }
//                } else {
//                    showError("Failed to load " + category + " stations");
//                }
//                currentCall = null;
//            }
//
//            @Override
//            public void onFailure(Call<List<RadioStation>> call, Throwable t) {
//                if (call.isCanceled()) return;
//
//                Log.e(TAG, "Error loading category stations", t);
//                showLoading(false);
//                showError("Failed to load " + category + " stations");
//                currentCall = null;
//            }
//        });
//    }
//
//    private void performSearch(String query) {
//        if (TextUtils.isEmpty(query.trim())) {
//            loadPopularStations();
//            return;
//        }
//
//        cancelCurrentCall();
//
//        if (!NetworkUtils.isNetworkAvailable(this)) {
//            showNetworkError();
//            return;
//        }
//
//        showLoading(true);
//        hideError();
//
//        Log.d(TAG, "Searching for: " + query);
//        currentCall = apiService.searchStations(query.trim());
//        currentCall.enqueue(new Callback<List<RadioStation>>() {
//            @Override
//            public void onResponse(Call<List<RadioStation>> call, Response<List<RadioStation>> response) {
//                if (call.isCanceled()) return;
//
//                showLoading(false);
//                if (response.isSuccessful() && response.body() != null) {
//                    List<RadioStation> stations = filterStationsByCountry(response.body(), PREFERRED_COUNTRY);
//                    if (stations.isEmpty()) {
//                        showEmptyState("No stations found for \"" + query + "\" in " + PREFERRED_COUNTRY);
//                    } else {
//                        adapter.updateStations(stations);
//                        hideError();
//                    }
//                } else {
//                    showError("Search failed");
//                }
//                currentCall = null;
//            }
//
//            @Override
//            public void onFailure(Call<List<RadioStation>> call, Throwable t) {
//                if (call.isCanceled()) return;
//
//                Log.e(TAG, "Search error", t);
//                showLoading(false);
//                showError("Search failed: " + NetworkUtils.getErrorMessage(t));
//                currentCall = null;
//            }
//        });
//    }
    private void cancelCurrentCall() {
        if (currentCall != null && !currentCall.isCanceled()) {
            currentCall.cancel();
            currentCall = null;
        }
    }

    @Override
    public void onStationClick(RadioStation station) {
        if (station == null) return;

        Log.d(TAG, "Station clicked: " + station.getName());
        currentStation = station;
        playerState = PlayerState.LOADING;
        updatePlayerUI();
        playStation(station);
    }

    @Override
    public void onFavoriteClick(RadioStation station) {
        if (station == null) return;

        toggleStationFavorite(station);
    }

    private void toggleStationFavorite(RadioStation station) {
        if (station.isFavorite()) {
            viewModel.removeFavorite(station.getStationuuid());
            showSnackbar(station.getName() + " removed from favorites");
        } else {
            FavoriteStation favoriteStation = new FavoriteStation(
                    station.getStationuuid(),
                    station.getName(),
                    station.getUrl(),
                    station.getCountry(),
                    station.getFavicon()
            );
            viewModel.addFavorite(favoriteStation);
            showSnackbar(station.getName() + " added to favorites");
        }
        station.setFavorite(!station.isFavorite());
        adapter.notifyDataSetChanged();
    }

    private void playStation(RadioStation station) {
        if (station == null || TextUtils.isEmpty(station.getUrl())) {
            showError("Invalid station URL");
            return;
        }

        if (!isServiceBound) {
            startAndBindService();
        }

        if (isServiceBound && playerService != null) {
            playerService.playStation(station.getUrl(), station.getName());
            playerState = PlayerState.PLAYING;
            updatePlayerUI();
        }
    }

    private void startAndBindService() {
        Intent intent = new Intent(this, RadioPlayerService.class);
        startForegroundService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void togglePlayPause() {
        if (!isServiceBound || playerService == null || currentStation == null) {
            showError("No station selected");
            return;
        }

        switch (playerState) {
            case PLAYING:
                playerService.pause();
                playerState = PlayerState.PAUSED;
                break;
            case PAUSED:
                playerService.resume();
                playerState = PlayerState.PLAYING;
                break;
            case STOPPED:
                playStation(currentStation);
                break;
        }
        updatePlayerUI();
    }

    private void toggleFavorite() {
        if (currentStation != null) {
            toggleStationFavorite(currentStation);
            updateFavoriteButton();
        }
    }

    private void showSleepTimerDialog() {
        if (!isServiceBound) {
            showError("Player not available");
            return;
        }

        SleepTimerDialog dialog = new SleepTimerDialog();
        dialog.setOnTimerSetListener(minutes -> {
            if (isServiceBound && playerService != null) {
                playerService.setSleepTimer(minutes);
                showSnackbar("Sleep timer set for " + minutes + " minutes");
            }
        });
        dialog.show(getSupportFragmentManager(), "SleepTimerDialog");
    }

    private void adjustVolume(boolean increase) {
        int currentVolume = seekBarVolume.getProgress();
        int newVolume = increase ?
                Math.min(currentVolume + VOLUME_STEP, MAX_VOLUME) :
                Math.max(currentVolume - VOLUME_STEP, 0);

        seekBarVolume.setProgress(newVolume);

        if (isServiceBound && playerService != null) {
            playerService.setVolume(newVolume / 100.0f);
        }
    }

    private void updatePlayerUI() {
        if (currentStation != null) {
            playerControlsCard.setVisibility(View.VISIBLE);
            textViewCurrentStation.setText(currentStation.getName());
            textViewCurrentCountry.setText(currentStation.getCountry());

            // Load station logo if available
            loadStationLogo(currentStation.getFavicon());

            updatePlayPauseButton();
            updateFavoriteButton();
        } else {
            playerControlsCard.setVisibility(View.GONE);
        }
    }

    private void loadStationLogo(String logoUrl) {
        // TODO: Implement with Glide or Picasso
        // Glide.with(this)
        //     .load(logoUrl)
        //     .placeholder(R.drawable.ic_radio_default)
        //     .error(R.drawable.ic_radio_default)
        //     .into(imageViewStationLogo);
    }

    private void updatePlayPauseButton() {
        switch (playerState) {
            case PLAYING:
                fabPlayPause.setImageResource(R.drawable.ic_pause);
                fabPlayPause.setEnabled(true);
                break;
            case PAUSED:
            case STOPPED:
                fabPlayPause.setImageResource(R.drawable.ic_play_arrow);
                fabPlayPause.setEnabled(true);
                break;
            case LOADING:
                fabPlayPause.setEnabled(false);
                break;
            case ERROR:
                fabPlayPause.setImageResource(R.drawable.ic_play_arrow);
                fabPlayPause.setEnabled(true);
                break;
        }
    }

    private void updateFavoriteButton() {
        if (currentStation != null && currentStation.isFavorite()) {
            buttonFavorite.setImageResource(R.drawable.ic_favorite);
        } else {
            buttonFavorite.setImageResource(R.drawable.ic_favorite_border);
        }
    }

    private void showLoading(boolean show) {
        if (progressBarLoading != null) {
            progressBarLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (recyclerViewStations != null) {
            recyclerViewStations.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void showError(String message) {
        Log.e(TAG, "Error: " + message);

        if (textViewError != null) {
            textViewError.setText(message);
            textViewError.setVisibility(View.VISIBLE);
        }

        showSnackbar(message);
    }

    private void hideError() {
        if (textViewError != null) {
            textViewError.setVisibility(View.GONE);
        }
    }

    private void showEmptyState() {
        showEmptyState("No stations available");
    }

    private void showEmptyState(String message) {
        adapter.updateStations(new ArrayList<>());
        showError(message);
    }

    private void showNetworkError() {
        showError("No internet connection. Please check your network settings.");
    }

    private void showSnackbar(String message) {
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//        if (!isServiceBound) {
//            Intent intent = new Intent(this, RadioPlayerService.class);
//            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
//        }
//    }
@Override
protected void onStart() {
    super.onStart();
    if (!isServiceBound) {
        Intent intent = new Intent(this, RadioPlayerService.class);
        // Always start service first!
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
}

    @Override
    protected void onStop() {
        super.onStop();
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelCurrentCall();

        // Clean up handlers
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }
}