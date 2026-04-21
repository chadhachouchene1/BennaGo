package com.example.bennago.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "order_items")
public class OrderItem {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int    orderId;
    private int    dishId;
    private String dishName;
    private double dishPrice;
    private int    quantity;

    public OrderItem(int orderId, int dishId, String dishName, double dishPrice, int quantity) {
        this.orderId   = orderId;
        this.dishId    = dishId;
        this.dishName  = dishName;
        this.dishPrice = dishPrice;
        this.quantity  = quantity;
    }

    // Getters
    public int    getId()        { return id; }
    public int    getOrderId()   { return orderId; }
    public int    getDishId()    { return dishId; }
    public String getDishName()  { return dishName; }
    public double getDishPrice() { return dishPrice; }
    public int    getQuantity()  { return quantity; }

    // Setters
    public void setId(int id)            { this.id = id; }
    public void setQuantity(int qty)     { this.quantity = qty; }

    public double getSubtotal() { return dishPrice * quantity; }
}