package com.example.bennago.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String  name;
    private String  email;
    private String  phone;
    private String  password;
    private boolean isBlocked;
    private String  fcmToken;   // ✅ Token FCM pour notifications push

    public User(String name, String email, String phone, String password) {
        this.name      = name;
        this.email     = email;
        this.phone     = phone;
        this.password  = password;
        this.isBlocked = false;
        this.fcmToken  = "";
    }

    public int     getId()        { return id; }
    public String  getName()      { return name; }
    public String  getEmail()     { return email; }
    public String  getPhone()     { return phone; }
    public String  getPassword()  { return password; }
    public boolean isBlocked()    { return isBlocked; }
    public String  getFcmToken()  { return fcmToken != null ? fcmToken : ""; }

    public void setId(int id)               { this.id = id; }
    public void setName(String n)           { this.name = n; }
    public void setEmail(String e)          { this.email = e; }
    public void setPhone(String p)          { this.phone = p; }
    public void setPassword(String pw)      { this.password = pw; }
    public void setBlocked(boolean blocked) { this.isBlocked = blocked; }
    public void setFcmToken(String token)   { this.fcmToken = token; }
}