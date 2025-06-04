package com.example.globalradioapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.globalradioapp.R;
import com.example.globalradioapp.database.FavoriteStation;
import com.example.globalradioapp.model.RadioStation;

import java.util.List;

public class RadioStationAdapter extends RecyclerView.Adapter<RadioStationAdapter.StationViewHolder> {

    private List<RadioStation> stations;
    private List<FavoriteStation> favoriteStations;
    private OnStationClickListener listener;

    public interface OnStationClickListener {
        void onStationClick(RadioStation station);
        void onFavoriteClick(RadioStation station);
    }

    public RadioStationAdapter(List<RadioStation> stations, OnStationClickListener listener) {
        this.stations = stations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_radio_station, parent, false);
        return new StationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StationViewHolder holder, int position) {
        RadioStation station = stations.get(position);
        holder.bind(station, listener);
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    public void updateStations(List<RadioStation> newStations) {
        this.stations = newStations;
        updateFavoriteStatus();
        notifyDataSetChanged();
    }

    public void updateFavorites(List<FavoriteStation> favoriteStations) {
        this.favoriteStations = favoriteStations;
        updateFavoriteStatus();
        notifyDataSetChanged();
    }

    private void updateFavoriteStatus() {
        if (favoriteStations != null) {
            for (RadioStation station : stations) {
                boolean isFavorite = favoriteStations.stream()
                        .anyMatch(fav -> fav.getStationId().equals(station.getStationuuid()));
                station.setFavorite(isFavorite);
            }
        }
    }

    static class StationViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageViewLogo;
        private TextView textViewName, textViewCountry, textViewLanguage, textViewTags;
        private ImageButton buttonFavorite;

        public StationViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewLogo = itemView.findViewById(R.id.imageViewLogo);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewCountry = itemView.findViewById(R.id.textViewCountry);
            textViewLanguage = itemView.findViewById(R.id.textViewLanguage);
            textViewTags = itemView.findViewById(R.id.textViewTags);
            buttonFavorite = itemView.findViewById(R.id.buttonFavorite);
        }

        public void bind(RadioStation station, OnStationClickListener listener) {
            textViewName.setText(station.getName());
            textViewCountry.setText(station.getCountry());
            textViewLanguage.setText(station.getLanguage());
            textViewTags.setText(station.getTags());

            // Update favorite button
            if (station.isFavorite()) {
                buttonFavorite.setImageResource(R.drawable.ic_favorite);
            } else {
                buttonFavorite.setImageResource(R.drawable.ic_favorite_border);
            }

            // Load station logo using Glide
            // Glide.with(itemView.getContext())
            //     .load(station.getFavicon())
            //     .placeholder(R.drawable.ic_radio)
            //     .into(imageViewLogo);

            itemView.setOnClickListener(v -> listener.onStationClick(station));
            buttonFavorite.setOnClickListener(v -> listener.onFavoriteClick(station));
        }
    }
}
