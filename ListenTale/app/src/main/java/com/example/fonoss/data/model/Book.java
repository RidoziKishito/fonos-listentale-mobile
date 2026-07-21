package com.example.fonoss.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Book implements Serializable {
    private String id;
    private String title;
    private String author;
    private String genre;
    private List<String> genres;
    private String description;
    private String duration;
    private String pages;
    private double rating;
    private String coverUrl;
    private String series;
    private transient List<String> chapters;
    private String audio_link;
    private long views;
    private long publishDate;

    public Book() {
        this.chapters = new ArrayList<>();
        this.genres = new ArrayList<>();
    }

    public Book(String id, String title, String author, String genre, List<String> genres, String description, String duration, String pages, double rating, String coverUrl, List<String> chapters) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.genres = genres != null ? genres : new ArrayList<>();
        this.description = description;
        this.duration = duration;
        this.pages = pages;
        this.rating = rating;
        this.coverUrl = coverUrl;
        this.chapters = chapters;
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getGenre() { return genre; }
    public List<String> getGenres() { return genres; }
    public String getDescription() { return description; }
    public String getDuration() { return duration; }
    public String getPages() { return pages; }
    public double getRating() { return rating; }
    public String getCoverUrl() { return coverUrl; }
    public String getSeries() { return series; }
    public List<String> getChapters() { return chapters; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setGenre(String genre) { this.genre = genre; }
    public void setGenres(List<String> genres) { this.genres = genres; }
    
    @com.google.firebase.firestore.PropertyName("gender")
    public void setGender(String gender) { this.genre = gender; }

    public void setDescription(String description) { this.description = description; }
    public void setDuration(String duration) { this.duration = duration; }
    public void setPages(String pages) { this.pages = pages; }
    public void setRating(double rating) { this.rating = rating; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    @com.google.firebase.firestore.PropertyName("cover_url")
    public void setLegacyCoverUrl(String coverUrl) { setCoverUrlIfMissing(coverUrl); }

    @com.google.firebase.firestore.PropertyName("imageUrl")
    public void setLegacyImageUrl(String imageUrl) { setCoverUrlIfMissing(imageUrl); }

    @com.google.firebase.firestore.PropertyName("image_url")
    public void setLegacyImageUrlSnakeCase(String imageUrl) { setCoverUrlIfMissing(imageUrl); }

    @com.google.firebase.firestore.PropertyName("cover")
    public void setLegacyCover(String coverUrl) { setCoverUrlIfMissing(coverUrl); }

    public void setSeries(String series) { this.series = series; }
    public void setChapters(List<String> chapters) { this.chapters = chapters; }

    public String getAudio_link() { return audio_link; }
    public void setAudio_link(String audio_link) { this.audio_link = audio_link; }

    @com.google.firebase.firestore.PropertyName("audio_url")
    public void setLegacyAudioUrl(String audio_url) { setAudioLinkIfMissing(audio_url); }

    @com.google.firebase.firestore.PropertyName("audioUrl")
    public void setLegacyAudioUrlCamelCase(String audioUrl) { setAudioLinkIfMissing(audioUrl); }

    private void setAudioLinkIfMissing(String value) {
        if ((audio_link == null || audio_link.trim().isEmpty())
                && value != null && !value.trim().isEmpty()) {
            audio_link = value.trim();
        }
    }

    public long getViews() { return views; }
    public void setViews(long views) { this.views = views; }

    public long getPublishDate() { return publishDate; }
    public void setPublishDate(long publishDate) { this.publishDate = publishDate; }

    @com.google.firebase.firestore.PropertyName("view_count")
    public void setViewCount(long views) { this.views = views; }

    @com.google.firebase.firestore.PropertyName("viewCount")
    public void setViewCountCamelCase(long views) { this.views = views; }

    @com.google.firebase.firestore.PropertyName("publish_date")
    public void setPublishDateSnakeCase(Object publishDate) { setPublishDateValue(publishDate); }

    @com.google.firebase.firestore.PropertyName("createdAt")
    public void setCreatedAt(Object createdAt) { setPublishDateValue(createdAt); }

    @com.google.firebase.firestore.PropertyName("created_at")
    public void setCreatedAtSnakeCase(Object createdAt) { setPublishDateValue(createdAt); }

    private void setCoverUrlIfMissing(String value) {
        if ((coverUrl == null || coverUrl.trim().isEmpty())
                && value != null && !value.trim().isEmpty()) {
            coverUrl = value.trim();
        }
    }

    private void setPublishDateValue(Object value) {
        if (value instanceof Number) {
            publishDate = ((Number) value).longValue();
        } else if (value instanceof com.google.firebase.Timestamp) {
            publishDate = ((com.google.firebase.Timestamp) value).toDate().getTime();
        } else if (value instanceof Date) {
            publishDate = ((Date) value).getTime();
        }
    }
}

