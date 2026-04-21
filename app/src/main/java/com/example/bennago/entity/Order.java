package com.example.bennago.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "orders")
public class Order {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int    userId;
    private String userName;
    private String userPhone;
    private String deliveryAddress;
    private String note;
    private double totalPrice;
    private String status;       // "En attente", "Confirmée", "En livraison", "Livrée", "Annulée"
    private long   createdAt;    // timestamp

    public Order(int userId, String userName, String userPhone,
                 String deliveryAddress, String note,
                 double totalPrice, String status, long createdAt) {
        this.userId          = userId;
        this.userName        = userName;
        this.userPhone       = userPhone;
        this.deliveryAddress = deliveryAddress;
        this.note            = note;
        this.totalPrice      = totalPrice;
        this.status          = status;
        this.createdAt       = createdAt;
    }

    // Getters
    public int    getId()              { return id; }
    public int    getUserId()          { return userId; }
    public String getUserName()        { return userName; }
    public String getUserPhone()       { return userPhone; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public String getNote()            { return note; }
    public double getTotalPrice()      { return totalPrice; }
    public String getStatus()          { return status; }
    public long   getCreatedAt()       { return createdAt; }

    // Setters
    public void setId(int id)                         { this.id = id; }
    public void setStatus(String status)              { this.status = status; }
    public void setTotalPrice(double totalPrice)      { this.totalPrice = totalPrice; }
    public void setDeliveryAddress(String address)    { this.deliveryAddress = address; }
    public void setNote(String note)                  { this.note = note; }
    public void setUserPhone(String phone)            { this.userPhone = phone; }
}