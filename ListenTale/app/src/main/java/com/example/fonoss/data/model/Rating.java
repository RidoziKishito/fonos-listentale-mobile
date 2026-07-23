package com.example.fonoss.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;

public class Rating implements Serializable {
    private String userId;
    private String bookId;
    private double rating;
    
    @ServerTimestamp
    private Date createdAt;
    
    @ServerTimestamp
    private Date updatedAt;

    public Rating() {
    }

    public Rating(String userId, String bookId, double rating) {
        this.userId = userId;
        this.bookId = bookId;
        this.rating = rating;
    }

    public Rating(String userId, String bookId, double rating, Date createdAt, Date updatedAt) {
        this.userId = userId;
        this.bookId = bookId;
        this.rating = rating;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Helper method to construct Document ID: {userId}_{bookId}
    public static String generateDocumentId(String userId, String bookId) {
        return userId + "_" + bookId;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
