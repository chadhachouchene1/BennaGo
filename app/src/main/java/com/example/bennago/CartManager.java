package com.example.bennago;

import com.example.bennago.entity.Dish;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton en mémoire qui gère le panier du client.
 * Réinitialisé à chaque connexion/déconnexion.
 */
public class CartManager {

    public static class CartItem {
        public Dish dish;
        public int  quantity;

        public CartItem(Dish dish, int quantity) {
            this.dish     = dish;
            this.quantity = quantity;
        }

        public double getSubtotal() { return dish.getPrice() * quantity; }
    }

    private static CartManager instance;
    private final List<CartItem> items = new ArrayList<>();

    private CartManager() {}

    public static CartManager getInstance() {
        if (instance == null) instance = new CartManager();
        return instance;
    }

    public static void reset() { instance = new CartManager(); }

    // ── API panier ────────────────────────────────────────────────────────────

    public void addDish(Dish dish) {
        for (CartItem item : items) {
            if (item.dish.getId() == dish.getId()) {
                item.quantity++;
                return;
            }
        }
        items.add(new CartItem(dish, 1));
    }

    public void removeDish(int dishId) {
        items.removeIf(item -> item.dish.getId() == dishId);
    }

    public void increaseQty(int dishId) {
        for (CartItem item : items) {
            if (item.dish.getId() == dishId) { item.quantity++; return; }
        }
    }

    public void decreaseQty(int dishId) {
        for (CartItem item : items) {
            if (item.dish.getId() == dishId) {
                item.quantity--;
                if (item.quantity <= 0) removeDish(dishId);
                return;
            }
        }
    }

    public void clear() { items.clear(); }

    public List<CartItem> getItems()  { return items; }
    public boolean        isEmpty()   { return items.isEmpty(); }
    public int            getTotalCount() {
        int n = 0;
        for (CartItem i : items) n += i.quantity;
        return n;
    }

    public double getTotalPrice() {
        double total = 0;
        for (CartItem i : items) total += i.getSubtotal();
        return total;
    }
}