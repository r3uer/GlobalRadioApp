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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import com.example.globalradioapp.adapter.ViewPagerAdapter;
import com.example.globalradioapp.database.FavoriteStation;
import com.example.globalradioapp.dialog.SleepTimerDialog;
import com.example.globalradioapp.fragment.AllStationsFragment;
import com.example.globalradioapp.fragment.FavoritesFragment;
import com.example.globalradioapp.model.RadioStation;
import com.example.globalradioapp.service.RadioPlayerService;
import com.example.globalradioapp.viewmodel.RadioViewModel;

public class MainActivity extends AppCompatActivity
        implements AllStationsFragment.OnStationInteractionListener,
        FavoritesFragment.OnFavoriteInteractionListener {

    private static final String TAG = "MainActivity";
    private static final int SEARCH_DELAY_MS = 500;
    private static final int MAX_VOLUME = 100;
    private static final int VOLUME_STEP = 10;
    private static final int DEFAULT_VOLUME = 70;

    // Views
    private MaterialCardView playerControlsCard;
    private FloatingActionButton fabPlayPause;
    private TextView textViewCurrentStation, textViewCurrentCountry;
    private ImageView imageViewStationLogo;
    private ImageButton buttonFavorite, buttonSleepTimer, buttonVolumeUp, buttonVolumeDown;
    private SearchView searchView;
    private ProgressBar progressBarLoading;

    // Tab components
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ViewPagerAdapter viewPagerAdapter;

    // Service and state
    private RadioPlayerService playerService;
    private boolean isServiceBound = false;
    private RadioStation currentStation;
    private PlayerState playerState = PlayerState.STOPPED;
    private RadioViewModel viewModel;

    // Volume tracking (since we removed the slider)
    private int currentVolume = DEFAULT_VOLUME;

    // Search handling
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private String lastSearchQuery = "";

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeComponents();
    }

    private void initializeComponents() {
        initViews();
        setupViewPager();
        setupViewModelObservers();
        setupClickListeners();
        setupSearch();
        setupFragmentListeners();
    }

    private void initViews() {
        playerControlsCard = findViewById(R.id.playerControlsCard);
        fabPlayPause = findViewById(R.id.fabPlayPause);
        textViewCurrentStation = findViewById(R.id.textViewCurrentStation);
        textViewCurrentCountry = findViewById(R.id.textViewCurrentCountry);
        imageViewStationLogo = findViewById(R.id.imageViewStationLogo);
        buttonFavorite = findViewById(R.id.buttonFavorite);
        buttonSleepTimer = findViewById(R.id.buttonSleepTimer);
        buttonVolumeUp = findViewById(R.id.buttonVolumeUp);
        buttonVolumeDown = findViewById(R.id.buttonVolumeDown);
        searchView = findViewById(R.id.searchView);
        progressBarLoading = findViewById(R.id.progressBarLoading);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        viewModel = new ViewModelProvider(this).get(RadioViewModel.class);

        // Configure search view text color
        EditText searchEditText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (searchEditText != null) {
            searchEditText.setTextColor(getResources().getColor(R.color.white));
            searchEditText.setHintTextColor(getResources().getColor(R.color.gray_300));
        }
        adjustViewPagerForPlayer(false);
    }

    private void setupViewPager() {
        viewPagerAdapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(viewPagerAdapter);

        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case ViewPagerAdapter.TAB_ALL_STATIONS:
                    tab.setText("🌎 All Stations");
                    break;
                case ViewPagerAdapter.TAB_FAVORITES:
                    tab.setText("❤️ Favorites");
                    break;
            }
        }).attach();
    }

    private void setupFragmentListeners() {
        // The fragments will be created by ViewPager, so we need to set listeners when they're available
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                // Set up fragment listeners when fragments are created
                if (viewPagerAdapter.getAllStationsFragment() != null) {
                    viewPagerAdapter.getAllStationsFragment().setOnStationInteractionListener(MainActivity.this);
                }
                if (viewPagerAdapter.getFavoritesFragment() != null) {
                    viewPagerAdapter.getFavoritesFragment().setOnFavoriteInteractionListener(MainActivity.this);
                }
            }
        });
    }

    private void setupViewModelObservers() {
        viewModel.getIsLoading().observe(this, isLoading -> {
            showLoading(isLoading);
        });

        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (!TextUtils.isEmpty(errorMessage)) {
                showSnackbar(errorMessage);
            }
        });
    }

    private void setupClickListeners() {
        fabPlayPause.setOnClickListener(v -> togglePlayPause());
        buttonFavorite.setOnClickListener(v -> toggleFavorite());
        buttonSleepTimer.setOnClickListener(v -> showSleepTimerDialog());
        buttonVolumeUp.setOnClickListener(v -> adjustVolume(true));
        buttonVolumeDown.setOnClickListener(v -> adjustVolume(false));
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
            // Clear search in both fragments
            if (viewPagerAdapter.getAllStationsFragment() != null) {
                viewPagerAdapter.getAllStationsFragment().performSearch("");
            }
            if (viewPagerAdapter.getFavoritesFragment() != null) {
                viewPagerAdapter.getFavoritesFragment().updateSearchQuery("");
            }
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

    private void performSearch(String query) {
        int currentTab = viewPager.getCurrentItem();

        if (currentTab == ViewPagerAdapter.TAB_ALL_STATIONS && viewPagerAdapter.getAllStationsFragment() != null) {
            viewPagerAdapter.getAllStationsFragment().performSearch(query);
        } else if (currentTab == ViewPagerAdapter.TAB_FAVORITES && viewPagerAdapter.getFavoritesFragment() != null) {
            viewPagerAdapter.getFavoritesFragment().updateSearchQuery(query);
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
                        showSnackbar("Playback error: " + error);
                        updatePlayerUI();
                    });
                }
            });
        }
    }

    // AllStationsFragment.OnStationInteractionListener implementation
    @Override
    public void onStationSelected(RadioStation station) {
        selectAndPlayStation(station);
    }

    @Override
    public void onFavoriteToggled(RadioStation station) {
        toggleStationFavorite(station);
    }

    // FavoritesFragment.OnFavoriteInteractionListener implementation
    @Override
    public void onFavoriteStationSelected(RadioStation station) {
        selectAndPlayStation(station);
    }

    @Override
    public void onExplorStationsClicked() {
        // Switch to All Stations tab
        viewPager.setCurrentItem(ViewPagerAdapter.TAB_ALL_STATIONS, true);
    }

    private void selectAndPlayStation(RadioStation station) {
        if (station == null) return;

        Log.d(TAG, "Station selected: " + station.getName());
        currentStation = station;
        playerState = PlayerState.LOADING;
        updatePlayerUI();
        playStation(station);
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

        // Update current station favorite status if it's the same station
        if (currentStation != null && currentStation.getStationuuid().equals(station.getStationuuid())) {
            currentStation.setFavorite(station.isFavorite());
            updateFavoriteButton();
        }
    }

    private void playStation(RadioStation station) {
        if (station == null || TextUtils.isEmpty(station.getUrl())) {
            showSnackbar("Invalid station URL");
            return;
        }

        if (!isServiceBound) {
            startAndBindService();
        }

        if (isServiceBound && playerService != null) {
            playerService.playStation(station.getUrl(), station.getName());
            // Set initial volume
            playerService.setVolume(currentVolume / 100.0f);
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
            showSnackbar("No station selected");
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
        }
    }

    private void showSleepTimerDialog() {
        if (!isServiceBound) {
            showSnackbar("Player not available");
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
        currentVolume = increase ?
                Math.min(currentVolume + VOLUME_STEP, MAX_VOLUME) :
                Math.max(currentVolume - VOLUME_STEP, 0);

        if (isServiceBound && playerService != null) {
            playerService.setVolume(currentVolume / 100.0f);
        }

        // Show volume feedback via snackbar
        showSnackbar("Volume: " + currentVolume + "%");
    }

    private void updatePlayerUI() {
        if (currentStation != null) {
            showPlayerWithAnimation();
            textViewCurrentStation.setText(currentStation.getName());
            textViewCurrentCountry.setText(currentStation.getCountry());

            // Load station logo if available
            loadStationLogo(currentStation.getFavicon());

            updatePlayPauseButton();
            updateFavoriteButton();
        } else {
            hidePlayerWithAnimation();
        }
    }

    private void showPlayerWithAnimation() {
        if (playerControlsCard.getVisibility() != View.VISIBLE) {
            playerControlsCard.setVisibility(View.VISIBLE);
            playerControlsCard.setAlpha(0f);
            playerControlsCard.setTranslationY(playerControlsCard.getHeight());

            playerControlsCard.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .start();

            // Adjust ViewPager for player
            adjustViewPagerForPlayer(true);
        }
    }

    private void hidePlayerWithAnimation() {
        if (playerControlsCard.getVisibility() == View.VISIBLE) {
            playerControlsCard.animate()
                    .alpha(0f)
                    .translationY(playerControlsCard.getHeight())
                    .setDuration(300)
                    .withEndAction(() -> {
                        playerControlsCard.setVisibility(View.GONE);
                    })
                    .start();

            adjustViewPagerForPlayer(false);
        }
    }

    private void adjustViewPagerForPlayer(boolean playerVisible) {
        // Adjust bottom padding dynamically based on player visibility
        int bottomPadding = playerVisible ?
                (int) (100 * getResources().getDisplayMetrics().density) : // 100dp for player + margin
                (int) (16 * getResources().getDisplayMetrics().density);   // 16dp default

        viewPager.setPadding(
                viewPager.getPaddingLeft(),
                viewPager.getPaddingTop(),
                viewPager.getPaddingRight(),
                bottomPadding
        );
    };

    private void loadStationLogo(String logoUrl) {
        // TODO: Implement with Glide or Picasso
        // For now, keeping default radio icon
        imageViewStationLogo.setImageResource(R.drawable.ic_radio);

        // Example with Glide (when you add it to your dependencies):
        /*
        Glide.with(this)
            .load(logoUrl)
            .placeholder(R.drawable.ic_radio)
            .error(R.drawable.ic_radio)
            .circleCrop()
            .into(imageViewStationLogo);
        */
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
                // You could add a loading indicator here
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
    }

    private void showSnackbar(String message) {
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!isServiceBound) {
            Intent intent = new Intent(this, RadioPlayerService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }

        // Set up fragment listeners when activity starts
        if (viewPagerAdapter != null) {
            if (viewPagerAdapter.getAllStationsFragment() != null) {
                viewPagerAdapter.getAllStationsFragment().setOnStationInteractionListener(this);
            }
            if (viewPagerAdapter.getFavoritesFragment() != null) {
                viewPagerAdapter.getFavoritesFragment().setOnFavoriteInteractionListener(this);
            }
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

        // Clean up handlers
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }
}