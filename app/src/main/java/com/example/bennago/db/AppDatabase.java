package com.example.bennago.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.bennago.dao.DishDao;
import com.example.bennago.dao.OrderDao;
import com.example.bennago.dao.ReviewDao;
import com.example.bennago.dao.UserDao;
import com.example.bennago.entity.Dish;
import com.example.bennago.entity.Order;
import com.example.bennago.entity.OrderItem;
import com.example.bennago.entity.Review;
import com.example.bennago.entity.User;

// ✅ version 5 : User.fcmToken ajouté
@Database(entities = {User.class, Dish.class, Order.class, OrderItem.class, Review.class},
        version = 6, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract UserDao  userDao();
    public abstract DishDao  dishDao();
    public abstract OrderDao orderDao();

    public abstract ReviewDao reviewDao();
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "bennago_db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}