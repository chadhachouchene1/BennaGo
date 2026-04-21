package com.example.bennago.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.bennago.entity.Dish;
import java.util.List;

@Dao
public interface DishDao {

    // ── ManageMenuActivity ────────────────────────────────────────────────────

    @Insert
    void insertDish(Dish dish);

    @Update
    void updateDish(Dish dish);

    @Delete
    void deleteDish(Dish dish);

    @Query("SELECT * FROM dishes ORDER BY category, name")
    List<Dish> getAllDishes();

    @Query("UPDATE dishes SET isActive = :active WHERE id = :id")
    void setDishActive(int id, boolean active);

    // ── MainActivity (espace client) ──────────────────────────────────────────

    /** Retourne uniquement les plats actifs — visibles par les clients */
    @Query("SELECT * FROM dishes WHERE isActive = 1 ORDER BY category, name")
    List<Dish> getActiveDishes();

    // ── StatsActivity ─────────────────────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM dishes")
    int getDishCount();

    @Query("SELECT COUNT(*) FROM dishes WHERE isActive = 1")
    int getActiveDishCount();

    @Query("SELECT COUNT(*) FROM dishes WHERE category = :category")
    int getCountByCategory(String category);
}