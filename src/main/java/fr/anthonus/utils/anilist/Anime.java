package fr.anthonus.utils.anilist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Anime {
    private final int id;
    private final String title;
    private final String imageLink;
    private final String link;
    private int lastEpisode;
    private int lastCheckedEpisode;
    public static final Map<Integer, Anime> animes = new HashMap<>();

    public Anime(int id, String title, String imageLink, String link, int lastEpisode) {
        this.id = id;
        this.title = title;
        this.imageLink = imageLink;
        this.link = link;
        this.lastEpisode = lastEpisode;
    }

    public int getId() {
        return id;
    }
    public String getTitle() {
        return title;
    }
    public String getImageLink() {
        return imageLink;
    }
    public String getLink() {
        return link;
    }
    public int getLastEpisode() {
        return lastEpisode;
    }
    public void setLastEpisode(int lastEpisode) {
        this.lastEpisode = lastEpisode;
    }
    public int getLastCheckedEpisode() {
        return lastCheckedEpisode;
    }
    public void setLastCheckedEpisode(int lastCheckedEpisode) {
        this.lastCheckedEpisode = lastCheckedEpisode;
    }
}
