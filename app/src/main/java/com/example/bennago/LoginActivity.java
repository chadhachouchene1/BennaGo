package com.example.bennago;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    // ── Identifiants admin statiques ─────────────────────────────────────────
    private static final String ADMIN_EMAIL    = "admin@bennago.com";
    private static final String ADMIN_PASSWORD = "admin123";

    // ── Vues ─────────────────────────────────────────────────────────────────
    private TextInputEditText etEmail, etPassword;
    private TextInputLayout   tilEmail, tilPassword;
    private MaterialButton    btnLogin;
    private TextView          tvRegisterLink, tvErrorMessage;
    private TabLayout         tabLayout;

    // ── Services ─────────────────────────────────────────────────────────────
    private AppDatabase           db;
    private SessionManager        sessionManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (sessionManager.isAdminLoggedIn()) { goTo(AdminActivity.class); return; }
        if (sessionManager.isUserLoggedIn())  { goTo(MainActivity.class);  return; }

        setContentView(R.layout.activity_login);
        db = AppDatabase.getInstance(this);

        initViews();
        setupTabs();
        setupClickListeners();
    }

    // ── Liaison XML ↔ Java ───────────────────────────────────────────────────
    private void initViews() {
        etEmail        = findViewById(R.id.et_email);
        etPassword     = findViewById(R.id.et_password);
        tilEmail       = findViewById(R.id.til_email);
        tilPassword    = findViewById(R.id.til_password);
        btnLogin       = findViewById(R.id.btn_login);
        tvRegisterLink = findViewById(R.id.tv_register_link);
        tvErrorMessage = findViewById(R.id.tv_error_message);
        tabLayout      = findViewById(R.id.tab_layout);
    }

    // ── Tabs : Connexion / Inscription ───────────────────────────────────────
    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Connexion"));
        tabLayout.addTab(tabLayout.newTab().setText("Inscription"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 1) {
                    startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
                    tabLayout.post(() -> tabLayout.selectTab(tabLayout.getTabAt(0)));
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    // ── Listeners ────────────────────────────────────────────────────────────
    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> {
            clearAll();
            handleLogin(); // un seul point d'entrée pour admin ET utilisateur
        });

        tvRegisterLink.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    // ── Logique de connexion unifiée ─────────────────────────────────────────
    private void handleLogin() {
        String email    = getText(etEmail);
        String password = getText(etPassword);

        // Validations communes
        if (TextUtils.isEmpty(email))    { tilEmail.setError("Email requis");           return; }
        if (TextUtils.isEmpty(password)) { tilPassword.setError("Mot de passe requis"); return; }

        // ✅ Vérification admin en priorité (pas de DB nécessaire)
        if (email.equals(ADMIN_EMAIL) && password.equals(ADMIN_PASSWORD)) {
            sessionManager.saveUserSession(-1, "Administrateur", true);
            Toast.makeText(this, "Bienvenue Admin !", Toast.LENGTH_SHORT).show();
            goTo(AdminActivity.class);
            return;
        }

        // ✅ Sinon → vérification utilisateur en DB (thread secondaire)
        setLoading(true);
        executor.execute(() -> {
            User user = db.userDao().getUserByEmail(email);
            runOnUiThread(() -> {
                setLoading(false);
                if (user == null) {
                    showError("Aucun compte trouvé avec cet email");
                } else if (!user.getPassword().equals(password)) {
                    showError("Mot de passe incorrect");
                } else {
                    sessionManager.saveUserSession(user.getId(), user.getName(), false);
                    Toast.makeText(this,
                            "Bienvenue " + user.getName() + " !", Toast.LENGTH_SHORT).show();
                    goTo(MainActivity.class);
                }
            });
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showError(String message) {
        tvErrorMessage.setText(message);
        tvErrorMessage.setVisibility(View.VISIBLE);
    }

    private void clearAll() {
        tilEmail.setError(null);
        tilPassword.setError(null);
        tvErrorMessage.setVisibility(View.GONE);
        tvErrorMessage.setText("");
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Connexion..." : "→  Se connecter");
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void goTo(Class<?> dest) {
        Intent i = new Intent(this, dest);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}