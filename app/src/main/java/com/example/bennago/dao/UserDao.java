package com.example.bennago.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.bennago.entity.User;
import java.util.List;

@Dao
public interface UserDao {

    @Insert
    void insertUser(User user);

    @Update
    void updateUser(User user);

    @Delete
    void deleteUser(User user);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getUserByEmail(String email);

    @Query("SELECT * FROM users WHERE email = :email AND password = :password LIMIT 1")
    User login(String email, String password);

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    User getUserById(int id);

    @Query("SELECT COUNT(*) FROM users")
    int getUserCount();

    @Query("SELECT * FROM users ORDER BY name ASC")
    List<User> getAllUsers();

    @Query("UPDATE users SET isBlocked = :blocked WHERE id = :userId")
    void setUserBlocked(int userId, boolean blocked);

    // ✅ Sauvegarder le token FCM du client
    @Query("UPDATE users SET fcmToken = :token WHERE id = :userId")
    void updateFcmToken(int userId, String token);

    // ✅ Récupérer le token FCM d'un client pour lui envoyer une notif
    @Query("SELECT fcmToken FROM users WHERE id = :userId LIMIT 1")
    String getFcmToken(int userId);

    // ✅ Mettre à jour le nom et le téléphone (ProfileActivity)
    @Query("UPDATE users SET name = :name, phone = :phone WHERE id = :userId")
    void updateNameAndPhone(int userId, String name, String phone);

    // ✅ Mettre à jour le mot de passe (ProfileActivity)
    @Query("UPDATE users SET password = :newPassword WHERE id = :userId")
    void updatePassword(int userId, String newPassword);
}