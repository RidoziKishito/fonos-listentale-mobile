package com.example.fonoss;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Book implements Serializable {
    private String id;
    private String title;
    private String author;
    private String genre;
    private String description;
    private String duration;
    private String pages;
    private double rating;
    private String coverUrl;
    private transient List<String> chapters;

    public Book() {
        this.chapters = new ArrayList<>();
    }

    public Book(String id, String title, String author, String genre, String description, String duration, String pages, double rating, String coverUrl, List<String> chapters) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.genre = genre;
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
    public String getDescription() { return description; }
    public String getDuration() { return duration; }
    public String getPages() { return pages; }
    public double getRating() { return rating; }
    public String getCoverUrl() { return coverUrl; }
    public List<String> getChapters() { return chapters; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setGenre(String genre) { this.genre = genre; }
    
    @com.google.firebase.firestore.PropertyName("gender")
    public void setGender(String gender) { this.genre = gender; }

    public void setDescription(String description) { this.description = description; }
    public void setDuration(String duration) { this.duration = duration; }
    public void setPages(String pages) { this.pages = pages; }
    public void setRating(double rating) { this.rating = rating; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public void setChapters(List<String> chapters) { this.chapters = chapters; }
}
