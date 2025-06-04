package com.example.globalradioapp.model;
import com.google.gson.annotations.SerializedName;

public class RadioStation {
    @SerializedName("stationuuid")
    private String stationuuid;

    @SerializedName("name")
    private String name;

    @SerializedName("url_resolved")
    private String url;

    @SerializedName("homepage")
    private String homepage;

    @SerializedName("favicon")
    private String favicon;

    @SerializedName("country")
    private String country;

    @SerializedName("language")
    private String language;

    @SerializedName("tags")
    private String tags;

    @SerializedName("votes")
    private int votes;

    private boolean isFavorite;

    // Constructors
    public RadioStation() {}

    public RadioStation(String stationuuid, String name, String url, String homepage,
                        String favicon, String country, String language, String tags, int votes) {
        this.stationuuid = stationuuid;
        this.name = name;
        this.url = url;
        this.homepage = homepage;
        this.favicon = favicon;
        this.country = country;
        this.language = language;
        this.tags = tags;
        this.votes = votes;
        this.isFavorite = false;
    }

    // Getters and Setters
    public String getStationuuid() { return stationuuid; }
    public void setStationuuid(String stationuuid) { this.stationuuid = stationuuid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getHomepage() { return homepage; }
    public void setHomepage(String homepage) { this.homepage = homepage; }

    public String getFavicon() { return favicon; }
    public void setFavicon(String favicon) { this.favicon = favicon; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public int getVotes() { return votes; }
    public void setVotes(int votes) { this.votes = votes; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    @Override
    public String toString() {
        return "RadioStation{" +
                "name='" + name + '\'' +
                ", country='" + country + '\'' +
                ", language='" + language + '\'' +
                '}';
    }
}
