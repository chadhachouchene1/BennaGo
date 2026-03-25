package com.example.bennago;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Sécurité : si pas user → retour login
        sessionManager = new SessionManager(this);
        if (!sessionManager.isUserLoggedIn()) {
            redirectToLogin();
            return;
        }

        setContentView(R.layout.activity_main);
        initViews();
        setupBottomNavigation();
    }

    private void initViews() {
        // Nom utilisateur
        TextView tvUserName     = findViewById(R.id.tv_user_name);
        TextView tvAvatarLetter = findViewById(R.id.tv_avatar_letter);
        String name = sessionManager.getUserName();
        tvUserName.setText(name);
        if (name != null && !name.isEmpty()) {
            tvAvatarLetter.setText(String.valueOf(name.charAt(0)).toUpperCase());
        }

        // Avatar → logout
        findViewById(R.id.iv_avatar).setOnClickListener(v -> confirmLogout());
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_home);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_cart) {
                Toast.makeText(this, "🛒 Panier — bientôt disponible", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_orders) {
                Toast.makeText(this, "📋 Commandes — bientôt disponible", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_profile) {
                Toast.makeText(this, "👤 Profil — bientôt disponible", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }

    private void confirmLogout() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Déconnexion")
                .setMessage("Voulez-vous vous déconnecter ?")
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