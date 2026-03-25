package com.example.bennago;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdminActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private AppDatabase    db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private TextView  tvTotalUsers;
    private CardView  cardManageMenu, cardManageOrders, cardManageUsers, cardStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Sécurité : si pas admin → retour login
        sessionManager = new SessionManager(this);
        if (!sessionManager.isAdminLoggedIn()) {
            redirectToLogin();
            return;
        }

        setContentView(R.layout.activity_admin);
        db = AppDatabase.getInstance(this);

        initViews();
        loadStats();
        setupClickListeners();
    }

    private void initViews() {
        TextView tvTitle    = findViewById(R.id.tv_admin_title);
        tvTitle.setText("Tableau de bord Admin 🔧");

        tvTotalUsers     = findViewById(R.id.tv_total_users);
        cardManageMenu   = findViewById(R.id.card_manage_menu);
        cardManageOrders = findViewById(R.id.card_manage_orders);
        cardManageUsers  = findViewById(R.id.card_manage_users);
        cardStats        = findViewById(R.id.card_stats);

        findViewById(R.id.btn_logout_admin).setOnClickListener(v -> confirmLogout());
    }

    private void loadStats() {
        executor.execute(() -> {
            int count = db.userDao().getUserCount();
            runOnUiThread(() -> tvTotalUsers.setText(String.valueOf(count)));
        });
    }

    private void setupClickListeners() {
        cardManageMenu.setOnClickListener(v ->
                Toast.makeText(this, "🍽️ Gestion menu — bientôt disponible", Toast.LENGTH_SHORT).show());
        cardManageOrders.setOnClickListener(v ->
                Toast.makeText(this, "📋 Gestion commandes — bientôt disponible", Toast.LENGTH_SHORT).show());
        cardManageUsers.setOnClickListener(v ->
                Toast.makeText(this, "👥 Gestion utilisateurs — bientôt disponible", Toast.LENGTH_SHORT).show());
        cardStats.setOnClickListener(v ->
                Toast.makeText(this, "📊 Statistiques — bientôt disponible", Toast.LENGTH_SHORT).show());
    }

    private void confirmLogout() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Déconnexion Admin")
                .setMessage("Voulez-vous quitter le panneau d'administration ?")
                .setPositiveButton("Oui", (d, w) -> {
                    sessionManager.logout();
                    redirectToLogin();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void redirectToLogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        moveTaskToBack(true);
    }}