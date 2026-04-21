package com.example.bennago.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.bennago.entity.Review;

import java.util.List;

@Dao
public interface ReviewDao {

    @Insert
    void insertReview(Review review);

    // Tous les avis (admin) — du plus récent au plus ancien
    @Query("SELECT * FROM reviews ORDER BY createdAt DESC")
    List<Review> getAllReviews();

    // Avis d'un client spécifique
    @Query("SELECT * FROM reviews WHERE userId = :userId ORDER BY createdAt DESC")
    List<Review> getReviewsByUser(int userId);

    // Nombre total d'avis
    @Query("SELECT COUNT(*) FROM reviews")
    int getReviewCount();

    // Moyenne des notes
    @Query("SELECT AVG(rating) FROM reviews")
    double getAverageRating();

    // Vérifier si un client a déjà laissé un avis aujourd'hui (éviter les doublons)
    @Query("SELECT COUNT(*) FROM reviews WHERE userId = :userId AND createdAt > :since")
    int countRecentByUser(int userId, long since);
}