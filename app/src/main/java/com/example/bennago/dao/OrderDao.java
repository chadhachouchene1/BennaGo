package com.example.bennago.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.bennago.entity.Order;
import com.example.bennago.entity.OrderItem;

import java.util.List;

@Dao
public interface OrderDao {

    @Insert
    long insertOrder(Order order);

    @Update
    void updateOrder(Order order);

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    List<Order> getAllOrders();

    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY createdAt DESC")
    List<Order> getOrdersByUser(int userId);

    @Query("SELECT COUNT(*) FROM orders")
    int getOrderCount();

    @Query("SELECT COUNT(*) FROM orders WHERE status = 'En attente'")
    int getPendingOrderCount();

    // ✅ Revenus des commandes livrées uniquement
    @Query("SELECT COALESCE(SUM(totalPrice), 0) FROM orders WHERE status = 'Livrée'")
    double getTotalRevenue();

    // ✅ Revenus de toutes les commandes
    @Query("SELECT COALESCE(SUM(totalPrice), 0) FROM orders")
    double getAllRevenue();

    @Query("UPDATE orders SET status = :status WHERE id = :orderId")
    void updateStatus(int orderId, String status);

    @Insert
    void insertOrderItem(OrderItem item);

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    List<OrderItem> getItemsByOrder(int orderId);

    // ✅ Top 3 plats les plus commandés depuis la vraie DB
    @Query("SELECT dishName, SUM(quantity) as totalQty " +
            "FROM order_items " +
            "GROUP BY dishName " +
            "ORDER BY totalQty DESC " +
            "LIMIT 3")
    List<TopDishResult> getTop3Dishes();

    // ✅ Nombre de commandes d'un client (ProfileActivity)
    @Query("SELECT COUNT(*) FROM orders WHERE userId = :userId")
    int getOrderCountByUser(int userId);

    // ✅ Total dépensé par un client (ProfileActivity)
    @Query("SELECT COALESCE(SUM(totalPrice), 0) FROM orders WHERE userId = :userId")
    double getTotalSpentByUser(int userId);

    // Résultat du top 3
    class TopDishResult {
        public String dishName;
        public int    totalQty;
    }
}