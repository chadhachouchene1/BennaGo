package com.example.bennago.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reviews")
public class Review {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int    userId;
    public String clientName;
    public int    rating;       // 1 à 5
    public String comment;
    public long   createdAt;

    public Review(int userId, String clientName, int rating, String comment, long createdAt) {
        this.userId     = userId;
        this.clientName = clientName;
        this.rating     = rating;
        this.comment    = comment;
        this.createdAt  = createdAt;
    }

    public int    getId()         { return id; }
    public int    getUserId()     { return userId; }
    public String getClientName() { return clientName; }
    public int    getRating()     { return rating; }
    public String getComment()    { return comment; }
    public long   getCreatedAt()  { return createdAt; }
}