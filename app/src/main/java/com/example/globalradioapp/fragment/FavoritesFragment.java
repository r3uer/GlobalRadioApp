package com.example.globalradioapp.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;

import com.example.globalradioapp.R;
import com.example.globalradioapp.SpacingItemDecoration;
import com.example.globalradioapp.adapter.RadioStationAdapter;
import com.example.globalradioapp.database.FavoriteStation;
import com.example.globalradioapp.model.RadioStation;
import com.example.globalradioapp.viewmodel.RadioViewModel;

import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends Fragment implements RadioStationAdapter.OnStationClickListener {

    private static final String TAG = "FavoritesFragment";
    
    // Interface for communicating with MainActivity
    public interface OnFavoriteInteractionListener {
        void onFavoriteStationSelected(RadioStation station);
        void onFavoriteToggled(RadioStation station);
        void onExplorStationsClicked();
    }

    // Views
    private RecyclerView recyclerViewFavorites;
    private RadioStationAdapter adapter;
    private ProgressBar progressBarLoading;
    private TextView textViewFavoritesCount;
    private LinearLayout emptyStateLayout;
    private MaterialButton buttonExplorStations;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Data
    private RadioViewModel viewModel;
    private OnFavoriteInteractionListener listener;
    private List<RadioStation> favoriteStationsList = new ArrayList<>();

    public static FavoritesFragment newInstance() {
        return new FavoritesFragment();
    }

    public void setOnFavoriteInteractionListener(OnFavoriteInteractionListener listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupViewModelObservers();
        setupClickListeners();
        setupSwipeRefresh();
    }

    private void initViews(View view) {
        recyclerViewFavorites = view.findViewById(R.id.recyclerViewFavorites);
        progressBarLoading = view.findViewById(R.id.progressBarLoading);
        textViewFavoritesCount = view.findViewById(R.id.textViewFavoritesCount);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        buttonExplorStations = view.findViewById(R.id.buttonExplorStations);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        viewModel = new ViewModelProvider(requireActivity()).get(RadioViewModel.class);
    }

    private void setupRecyclerView() {
        adapter = new RadioStationAdapter(new ArrayList<>(), this);
        recyclerViewFavorites.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewFavorites.setAdapter(adapter);

        // Add item decoration for better spacing
        int spacing = getResources().getDimensionPixelSize(R.dimen.item_spacing);
        recyclerViewFavorites.addItemDecoration(new SpacingItemDecoration(spacing));
    }

    private void setupViewModelObservers() {
        viewModel.getFavoriteStations().observe(getViewLifecycleOwner(), favoriteStations -> {
            Log.d(TAG, "Favorite stations updated: " + favoriteStations.size());
            updateFavoritesList(favoriteStations);
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            showLoading(isLoading);
        });
    }

    private void setupClickListeners() {
        buttonExplorStations.setOnClickListener(v -> {
            if (listener != null) {
                listener.onExplorStationsClicked();
            }
        });
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::refreshFavorites);
            swipeRefreshLayout.setColorSchemeResources(
                    R.color.primary_500,
                    R.color.primary_600,
                    R.color.primary_700
            );
        }
    }

    private void updateFavoritesList(List<FavoriteStation> favoriteStations) {
        favoriteStationsList.clear();
        
        // Convert FavoriteStation to RadioStation
        for (FavoriteStation favStation : favoriteStations) {
            RadioStation radioStation = new RadioStation();
            radioStation.setStationuuid(favStation.getStationId());
            radioStation.setName(favStation.getName());
            radioStation.setUrl(favStation.getUrl());
            radioStation.setCountry(favStation.getCountry());
            radioStation.setFavicon(favStation.getFavicon());
            radioStation.setFavorite(true); // Mark as favorite
            favoriteStationsList.add(radioStation);
        }

        // Update UI
        updateFavoritesCount(favoriteStations.size());
        
        if (favoriteStations.isEmpty()) {
            showEmptyState();
        } else {
            showFavoritesList();
            adapter.updateStations(favoriteStationsList);
        }

        // Stop refresh animation
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void updateFavoritesCount(int count) {
        if (textViewFavoritesCount != null) {
            String countText = count == 1 ? 
                "1 favorite station" : 
                count + " favorite stations";
            textViewFavoritesCount.setText(countText);
        }
    }

    private void showEmptyState() {
        if (emptyStateLayout != null && recyclerViewFavorites != null) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerViewFavorites.setVisibility(View.GONE);
        }
    }

    private void showFavoritesList() {
        if (emptyStateLayout != null && recyclerViewFavorites != null) {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerViewFavorites.setVisibility(View.VISIBLE);
        }
    }

    private void refreshFavorites() {
        // Favorites are automatically refreshed through ViewModel observer
        // Just stop the refresh animation after a short delay
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.postDelayed(() -> {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }, 500);
        }
    }

    @Override
    public void onStationClick(RadioStation station) {
        if (listener != null) {
            listener.onFavoriteStationSelected(station);
        }
    }

    @Override
    public void onFavoriteClick(RadioStation station) {
        if (listener != null) {
            listener.onFavoriteToggled(station);
        }
    }

    private void showLoading(boolean show) {
        if (progressBarLoading != null) {
            progressBarLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    public void updateSearchQuery(String query) {
        // For favorites, we can implement local filtering
        if (adapter != null && favoriteStationsList != null) {
            if (query == null || query.trim().isEmpty()) {
                adapter.updateStations(favoriteStationsList);
            } else {
                List<RadioStation> filteredStations = new ArrayList<>();
                String searchQuery = query.toLowerCase().trim();
                
                for (RadioStation station : favoriteStationsList) {
                    if (station.getName().toLowerCase().contains(searchQuery) ||
                        station.getCountry().toLowerCase().contains(searchQuery) ||
                        (station.getLanguage() != null && station.getLanguage().toLowerCase().contains(searchQuery)) ||
                        (station.getTags() != null && station.getTags().toLowerCase().contains(searchQuery))) {
                        filteredStations.add(station);
                    }
                }
                
                adapter.updateStations(filteredStations);
                
                if (filteredStations.isEmpty() && !favoriteStationsList.isEmpty()) {
                    // Show a message that no favorites match the search
                    showEmptyState();
                }
            }
        }
    }
}