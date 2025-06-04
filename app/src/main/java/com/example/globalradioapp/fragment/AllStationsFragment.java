package com.example.globalradioapp.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

import com.example.globalradioapp.R;
import com.example.globalradioapp.SpacingItemDecoration;
import com.example.globalradioapp.adapter.RadioStationAdapter;
import com.example.globalradioapp.database.FavoriteStation;
import com.example.globalradioapp.model.RadioStation;
import com.example.globalradioapp.network.ApiClient;
import com.example.globalradioapp.network.NetworkUtils;
import com.example.globalradioapp.network.RadioApiService;
import com.example.globalradioapp.viewmodel.RadioViewModel;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AllStationsFragment extends Fragment implements RadioStationAdapter.OnStationClickListener {

    private static final String TAG = "AllStationsFragment";
    
    // Interface for communicating with MainActivity
    public interface OnStationInteractionListener {
        void onStationSelected(RadioStation station);
        void onFavoriteToggled(RadioStation station);
    }

    // Views
    private RecyclerView recyclerViewStations;
    private RadioStationAdapter adapter;
    private ProgressBar progressBarLoading;
    private TextView textViewError;
    private ChipGroup categoryChipGroup;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Data and API
    private RadioViewModel viewModel;
    private RadioApiService apiService;
    private Call<List<RadioStation>> currentCall;
    private OnStationInteractionListener listener;

    // Search and filtering
    private String currentSearchQuery = "";
    private String currentCategory = "popular";

    public static AllStationsFragment newInstance() {
        return new AllStationsFragment();
    }

    public void setOnStationInteractionListener(OnStationInteractionListener listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_all_stations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupViewModelObservers();
        setupClickListeners();
        setupSwipeRefresh();
        
        checkNetworkAndLoad();
    }

    private void initViews(View view) {
        recyclerViewStations = view.findViewById(R.id.recyclerViewStations);
        progressBarLoading = view.findViewById(R.id.progressBarLoading);
        textViewError = view.findViewById(R.id.textViewError);
        categoryChipGroup = view.findViewById(R.id.categoryChipGroup);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        viewModel = new ViewModelProvider(requireActivity()).get(RadioViewModel.class);
        apiService = ApiClient.getRadioApiService();
    }

    private void setupRecyclerView() {
        adapter = new RadioStationAdapter(new ArrayList<>(), this);
        recyclerViewStations.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewStations.setAdapter(adapter);

        // Add item decoration for better spacing
        int spacing = getResources().getDimensionPixelSize(R.dimen.item_spacing);
        recyclerViewStations.addItemDecoration(new SpacingItemDecoration(spacing));
    }

    private void setupViewModelObservers() {
        viewModel.getFavoriteStations().observe(getViewLifecycleOwner(), favoriteStations -> {
            if (adapter != null) {
                adapter.updateFavorites(favoriteStations);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            showLoading(isLoading);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (!TextUtils.isEmpty(errorMessage)) {
                showError(errorMessage);
            }
        });
    }

    private void setupClickListeners() {
        categoryChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            int checkedId = checkedIds.get(0);
            String category = getCategoryFromChipId(checkedId);

            if ("popular".equals(category)) {
                currentCategory = "popular";
                loadPopularStations();
            } else if (!TextUtils.isEmpty(category)) {
                currentCategory = category;
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
                    R.color.primary_500,
                    R.color.primary_600,
                    R.color.primary_700
            );
        }
    }

    private void checkNetworkAndLoad() {
        if (NetworkUtils.isNetworkAvailable(requireContext())) {
            loadPopularStations();
        } else {
            showNetworkError();
        }
    }

    private void refreshCurrentContent() {
        if (!TextUtils.isEmpty(currentSearchQuery)) {
            performSearch(currentSearchQuery);
        } else if ("popular".equals(currentCategory)) {
            loadPopularStations();
        } else {
            loadStationsByCategory(currentCategory);
        }
    }

    public void performSearch(String query) {
        currentSearchQuery = query;
        
        if (TextUtils.isEmpty(query.trim())) {
            loadPopularStations();
            return;
        }

        cancelCurrentCall();

        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            showNetworkError();
            return;
        }

        showLoading(true);
        hideError();

        Log.d(TAG, "Searching for: " + query);
        currentCall = apiService.searchStations(query.trim(), 50);
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

    private void loadPopularStations() {
        cancelCurrentCall();
        currentSearchQuery = ""; // Clear search query

        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            showNetworkError();
            return;
        }

        showLoading(true);
        hideError();

        Log.d(TAG, "Loading popular stations...");
        currentCall = apiService.getTopStations(50);
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
                        showEmptyState("No popular stations found");
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

    private void loadStationsByCategory(String category) {
        cancelCurrentCall();
        currentSearchQuery = ""; // Clear search query

        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            showNetworkError();
            return;
        }

        showLoading(true);
        hideError();

        Log.d(TAG, "Loading stations for category: " + category);
        currentCall = apiService.getStationsByCategory(category, 50);
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
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                showError("Failed to load " + category + " stations");
                currentCall = null;
            }
        });
    }

    private void cancelCurrentCall() {
        if (currentCall != null && !currentCall.isCanceled()) {
            currentCall.cancel();
            currentCall = null;
        }
    }

    @Override
    public void onStationClick(RadioStation station) {
        if (listener != null) {
            listener.onStationSelected(station);
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

    private void showEmptyState(String message) {
        adapter.updateStations(new ArrayList<>());
        showError(message);
    }

    private void showNetworkError() {
        showError("No internet connection. Please check your network settings.");
    }

    private void showSnackbar(String message) {
        View rootView = getView();
        if (rootView != null) {
            Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cancelCurrentCall();
    }
}