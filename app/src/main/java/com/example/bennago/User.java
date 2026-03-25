package com.example.bennago;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private String email;
    private String phone;
    private String password;

    public User(String name, String email, String phone, String password) {
        this.name     = name;
        this.email    = email;
        this.phone    = phone;
        this.password = password;
    }

    // Getters & Setters
    public int    getId()       { return id; }
    public void   setId(int id) { this.id = id; }

    public String getName()          { return name; }
    public void   setName(String n)  { this.name = n; }

    public String getEmail()         { return email; }
    public void   setEmail(String e) { this.email = e; }

    public String getPhone()         { return phone; }
    public void   setPhone(String p) { this.phone = p; }

    public String getPassword()          { return password; }
    public void   setPassword(String pw) { this.password = pw; }
}