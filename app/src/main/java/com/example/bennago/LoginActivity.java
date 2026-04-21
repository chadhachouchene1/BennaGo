package com.example.bennago;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.example.bennago.db.AppDatabase;
import com.example.bennago.entity.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final String ADMIN_EMAIL    = "admin@bennago.com";
    private static final String ADMIN_PASSWORD = "admin123";

    private TextInputEditText etEmail, etPassword;
    private TextInputLayout   tilEmail, tilPassword;
    private MaterialButton    btnLogin, btnBiometric;
    private TextView          tvRegisterLink, tvErrorMessage;
    private TabLayout         tabLayout;

    private AppDatabase           db;
    private SessionManager        sessionManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private BiometricPrompt            biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

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
        setupBiometric();
        setupClickListeners();
    }

    private void initViews() {
        etEmail        = findViewById(R.id.et_email);
        etPassword     = findViewById(R.id.et_password);
        tilEmail       = findViewById(R.id.til_email);
        tilPassword    = findViewById(R.id.til_password);
        btnLogin       = findViewById(R.id.btn_login);
        btnBiometric   = findViewById(R.id.btn_biometric);
        tvRegisterLink = findViewById(R.id.tv_register_link);
        tvErrorMessage = findViewById(R.id.tv_error_message);
        tabLayout      = findViewById(R.id.tab_layout);
    }

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

    private void setupBiometric() {
        Executor mainExecutor = ContextCompat.getMainExecutor(this);

        biometricPrompt = new BiometricPrompt(this, mainExecutor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        sessionManager.saveUserSession(-1, "Administrateur", true);
                        Toast.makeText(LoginActivity.this,
                                "Empreinte reconnue — Bienvenue Admin !", Toast.LENGTH_SHORT).show();
                        goTo(AdminActivity.class);
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                                errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON)
                            showError("Erreur biométrie : " + errString);
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(LoginActivity.this,
                                "Empreinte non reconnue, réessayez", Toast.LENGTH_SHORT).show();
                    }
                });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Connexion Admin")
                .setSubtitle("Posez votre doigt pour accéder au panneau admin")
                .setNegativeButtonText("Utiliser email / mot de passe")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build();

        BiometricManager bm = BiometricManager.from(this);
        if (bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                == BiometricManager.BIOMETRIC_SUCCESS) {
            btnBiometric.setVisibility(View.VISIBLE);
        } else {
            btnBiometric.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> { clearAll(); handleLogin(); });
        btnBiometric.setOnClickListener(v -> { clearAll(); biometricPrompt.authenticate(promptInfo); });
        tvRegisterLink.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void handleLogin() {
        String email    = getText(etEmail);
        String password = getText(etPassword);

        if (TextUtils.isEmpty(email))    { tilEmail.setError("Email requis");           return; }
        if (TextUtils.isEmpty(password)) { tilPassword.setError("Mot de passe requis"); return; }

        // Admin
        if (email.equals(ADMIN_EMAIL) && password.equals(ADMIN_PASSWORD)) {
            sessionManager.saveUserSession(-1, "Administrateur", true);
            Toast.makeText(this, "Bienvenue Admin !", Toast.LENGTH_SHORT).show();
            goTo(AdminActivity.class);
            return;
        }

        // Client
        setLoading(true);
        executor.execute(() -> {
            User user = db.userDao().getUserByEmail(email);
            runOnUiThread(() -> {
                setLoading(false);
                if (user == null) {
                    showError("Aucun compte trouvé avec cet email");
                } else if (!user.getPassword().equals(password)) {
                    showError("Mot de passe incorrect");
                } else if (user.isBlocked()) {
                    // ✅ Compte bloqué par l'admin
                    showError("Votre compte a été suspendu. Contactez l'administrateur.");
                } else {
                    sessionManager.saveUserSession(user.getId(), user.getName(), false);
                    Toast.makeText(this, "Bienvenue " + user.getName() + " !", Toast.LENGTH_SHORT).show();
                    goTo(MainActivity.class);
                }
            });
        });
    }

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