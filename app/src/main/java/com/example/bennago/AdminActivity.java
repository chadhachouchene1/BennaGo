package com.example.bennago;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.bennago.db.AppDatabase;
import com.example.bennago.entity.Order;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdminActivity extends AppCompatActivity {

    private SessionManager        sessionManager;
    private AppDatabase           db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Stats
    private TextView tvTotalUsers, tvTotalOrders, tvTotalDishes;

    // Cartes (sans cardNotifications)
    private CardView cardManageMenu, cardManageOrders,
            cardManageUsers, cardStats, cardReviews;

    // Commandes récentes dynamiques
    private LinearLayout   layoutRecentOrders;
    private TextView       tvEmptyOrders;
    private MaterialButton btnSeeAllOrders;
    private ImageButton    btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isAdminLoggedIn()) { redirectToLogin(); return; }

        setContentView(R.layout.activity_admin);
        db = AppDatabase.getInstance(this);

        FirebaseMessaging.getInstance().subscribeToTopic("admin")
                .addOnCompleteListener(t ->
                        android.util.Log.d("FCM_ADMIN",
                                t.isSuccessful() ? "Abonné ✅" : "Échec"));

        initViews();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStats();
        loadRecentOrders();
    }

    private void initViews() {
        tvTotalUsers   = findViewById(R.id.tv_total_users);
        tvTotalOrders  = findViewById(R.id.tv_total_orders);
        tvTotalDishes  = findViewById(R.id.tv_total_dishes);

        cardManageMenu   = findViewById(R.id.card_manage_menu);
        cardManageOrders = findViewById(R.id.card_manage_orders);
        cardManageUsers  = findViewById(R.id.card_manage_users);
        cardStats        = findViewById(R.id.card_stats);
        cardReviews      = findViewById(R.id.card_reviews);

        layoutRecentOrders = findViewById(R.id.layout_recent_orders_container);
        tvEmptyOrders      = findViewById(R.id.tv_recent_orders_empty);
        btnSeeAllOrders    = findViewById(R.id.btn_see_all_orders);
        btnLogout          = findViewById(R.id.btn_logout_admin);
    }

    // ── Stats réelles ─────────────────────────────────────────────────────────
    private void loadStats() {
        executor.execute(() -> {
            int users  = db.userDao().getUserCount();
            int dishes = db.dishDao().getDishCount();
            int orders = db.orderDao().getOrderCount();
            runOnUiThread(() -> {
                if (tvTotalUsers  != null) tvTotalUsers.setText(String.valueOf(users));
                if (tvTotalDishes != null) tvTotalDishes.setText(String.valueOf(dishes));
                if (tvTotalOrders != null) tvTotalOrders.setText(String.valueOf(orders));
            });
        });
    }

    // ── 5 dernières commandes depuis DB ──────────────────────────────────────
    private void loadRecentOrders() {
        executor.execute(() -> {
            List<Order> all    = db.orderDao().getAllOrders();
            List<Order> recent = all.size() > 5 ? all.subList(0, 5) : all;

            runOnUiThread(() -> {
                if (layoutRecentOrders == null) return;
                layoutRecentOrders.removeAllViews();

                if (recent.isEmpty()) {
                    if (tvEmptyOrders != null) tvEmptyOrders.setVisibility(View.VISIBLE);
                    return;
                }
                if (tvEmptyOrders != null) tvEmptyOrders.setVisibility(View.GONE);

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM  HH:mm", Locale.FRANCE);

                for (int i = 0; i < recent.size(); i++) {
                    Order order = recent.get(i);

                    // Séparateur
                    if (i > 0) {
                        View sep = new View(this);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 1);
                        sep.setLayoutParams(lp);
                        sep.setBackgroundColor(0xFFE8D5C0);
                        layoutRecentOrders.addView(sep);
                    }

                    // Inflater la ligne
                    View row = LayoutInflater.from(this)
                            .inflate(R.layout.item_recent_order_row,
                                    layoutRecentOrders, false);

                    TextView tvId     = row.findViewById(R.id.tv_recent_order_id);
                    TextView tvClient = row.findViewById(R.id.tv_recent_order_client);
                    TextView tvTotal  = row.findViewById(R.id.tv_recent_order_total);
                    TextView tvStatus = row.findViewById(R.id.tv_recent_order_status);
                    TextView tvDate   = row.findViewById(R.id.tv_recent_order_date);

                    tvId.setText("#" + order.getId());
                    tvClient.setText(order.getUserName());
                    tvTotal.setText(String.format("%.3f TND", order.getTotalPrice()));
                    tvStatus.setText(order.getStatus());
                    tvDate.setText(sdf.format(new Date(order.getCreatedAt())));

                    // Couleur selon statut
                    int color;
                    switch (order.getStatus()) {
                        case "Confirmée":    color = 0xFF5A9E6F; break;
                        case "En livraison": color = 0xFF4A90D9; break;
                        case "Livrée":       color = 0xFF2E7D32; break;
                        case "Annulée":      color = 0xFFB00020; break;
                        default:             color = 0xFFD4703A; break;
                    }
                    tvStatus.setTextColor(color);

                    // Clic → ManageOrdersActivity
                    row.setOnClickListener(v -> goTo(ManageOrdersActivity.class));
                    layoutRecentOrders.addView(row);
                }
            });
        });
    }

    private void setupClickListeners() {
        if (btnLogout        != null) btnLogout.setOnClickListener(v -> confirmLogout());
        if (cardManageMenu   != null) cardManageMenu.setOnClickListener(v -> goTo(ManageMenuActivity.class));
        if (cardManageOrders != null) cardManageOrders.setOnClickListener(v -> goTo(ManageOrdersActivity.class));
        if (btnSeeAllOrders  != null) btnSeeAllOrders.setOnClickListener(v -> goTo(ManageOrdersActivity.class));
        if (cardManageUsers  != null) cardManageUsers.setOnClickListener(v -> goTo(ManageUsersActivity.class));
        if (cardStats        != null) cardStats.setOnClickListener(v -> goTo(StatsActivity.class));
        if (cardReviews      != null) cardReviews.setOnClickListener(v -> goTo(AdminReviewsActivity.class));
    }

    private void goTo(Class<?> dest) {
        startActivity(new Intent(this, dest));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void showComingSoon(String f) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(f).setMessage("Bientôt disponible.")
                .setPositiveButton("OK", null).show();
    }

    private void confirmLogout() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Déconnexion Admin")
                .setMessage("Voulez-vous quitter le panneau d'administration ?")
                .setPositiveButton("Oui", (d, w) -> {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic("admin");
                    sessionManager.logout();
                    redirectToLogin();
                })
                .setNegativeButton("Annuler", null).show();
    }

    private void redirectToLogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i); finish();
    }

    @Override public void onBackPressed() { super.onBackPressed(); moveTaskToBack(true); }
}