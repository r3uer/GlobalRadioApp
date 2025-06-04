package com.example.globalradioapp.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.globalradioapp.fragment.AllStationsFragment;
import com.example.globalradioapp.fragment.FavoritesFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {

    private static final int TAB_COUNT = 2;
    public static final int TAB_ALL_STATIONS = 0;
    public static final int TAB_FAVORITES = 1;

    private AllStationsFragment allStationsFragment;
    private FavoritesFragment favoritesFragment;

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case TAB_ALL_STATIONS:
                allStationsFragment = AllStationsFragment.newInstance();
                return allStationsFragment;
            case TAB_FAVORITES:
                favoritesFragment = FavoritesFragment.newInstance();
                return favoritesFragment;
            default:
                return AllStationsFragment.newInstance();
        }
    }

    @Override
    public int getItemCount() {
        return TAB_COUNT;
    }

    public AllStationsFragment getAllStationsFragment() {
        return allStationsFragment;
    }

    public FavoritesFragment getFavoritesFragment() {
        return favoritesFragment;
    }
}