package com.example.bennago.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "dishes")
public class Dish {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;        // Nom du plat
    private double price;       // Prix en TND
    private String imageUri;    // URI de l'image (stockée localement)
    private String category;    // Catégorie : Burger, Pizza, Boisson...
    private String description; // Description courte
    private boolean isActive;   // Actif = visible par les clients

    // ── Constructeur ─────────────────────────────────────────────────────────
    public Dish(String name, double price, String imageUri,
                String category, String description, boolean isActive) {
        this.name        = name;
        this.price       = price;
        this.imageUri    = imageUri;
        this.category    = category;
        this.description = description;
        this.isActive    = isActive;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public int     getId()          { return id; }
    public String  getName()        { return name; }
    public double  getPrice()       { return price; }
    public String  getImageUri()    { return imageUri; }
    public String  getCategory()    { return category; }
    public String  getDescription() { return description; }
    public boolean isActive()       { return isActive; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setId(int id)                   { this.id = id; }
    public void setName(String name)            { this.name = name; }
    public void setPrice(double price)          { this.price = price; }
    public void setImageUri(String imageUri)    { this.imageUri = imageUri; }
    public void setCategory(String category)    { this.category = category; }
    public void setDescription(String desc)     { this.description = desc; }
    public void setActive(boolean active)       { this.isActive = active; }
}