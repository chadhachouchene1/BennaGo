package com.example.bennago;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bennago.db.AppDatabase;
import com.example.bennago.entity.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    // ── Vues ─────────────────────────────────────────────────────────────────
    private TextInputEditText etName, etEmail, etPhone, etPassword, etConfirmPassword;
    private TextInputLayout   tilName, tilEmail, tilPhone, tilPassword, tilConfirmPassword;
    private MaterialButton    btnRegister;
    private TextView          tvLoginLink;

    // ── Services ─────────────────────────────────────────────────────────────
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        db = AppDatabase.getInstance(this);
        initViews();
        setupClickListeners();
    }

    // ── Liaison XML ↔ Java ───────────────────────────────────────────────────
    private void initViews() {
        etName            = findViewById(R.id.et_name);
        etEmail           = findViewById(R.id.et_email);
        etPhone           = findViewById(R.id.et_phone);
        etPassword        = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        tilName           = findViewById(R.id.til_name);
        tilEmail          = findViewById(R.id.til_email);
        tilPhone          = findViewById(R.id.til_phone);
        tilPassword       = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        btnRegister       = findViewById(R.id.btn_register);
        tvLoginLink       = findViewById(R.id.tv_login_link);
    }

    // ── Listeners ────────────────────────────────────────────────────────────
    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> validateAndRegister());
        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    // ── Validation + Inscription ──────────────────────────────────────────────
    private void validateAndRegister() {
        String name            = getText(etName);
        String email           = getText(etEmail);
        String phone           = getText(etPhone);
        String password        = getText(etPassword);
        String confirmPassword = getText(etConfirmPassword);

        // Effacer toutes les erreurs précédentes
        clearErrors();

        // Validation champ par champ
        boolean valid = true;

        if (TextUtils.isEmpty(name)) {
            tilName.setError("Le nom est requis");
            valid = false;
        }
        if (TextUtils.isEmpty(email) ||
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email invalide");
            valid = false;
        }
        if (TextUtils.isEmpty(phone)) {
            tilPhone.setError("Le téléphone est requis");
            valid = false;
        }
        if (password.length() < 6) {
            tilPassword.setError("Minimum 6 caractères");
            valid = false;
        }
        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Les mots de passe ne correspondent pas");
            valid = false;
        }

        if (!valid) return; // stoppe si un champ est invalide

        // Passe en état chargement
        setLoading(true);

        // Tout se passe dans un seul thread séquentiel
        executor.execute(() -> {

            // 1. Vérifier si l'email existe déjà en DB
            User existing = db.userDao().getUserByEmail(email);

            if (existing != null) {
                // Email déjà pris → retour sur le thread principal pour afficher l'erreur
                runOnUiThread(() -> {
                    tilEmail.setError("Cet email est déjà utilisé");
                    setLoading(false);
                });
                return; // stoppe ici, pas d'insertion
            }

            // 2. Email libre → insertion du nouvel utilisateur
            User newUser = new User(name, email, phone, password);
            db.userDao().insertUser(newUser);

            // 3. Retour sur le thread principal → message + redirection
            runOnUiThread(() -> {
                Toast.makeText(this,
                        "Compte créé avec succès !", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            });
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void clearErrors() {
        tilName.setError(null);
        tilEmail.setError(null);
        tilPhone.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
    }

    private void setLoading(boolean loading) {
        btnRegister.setEnabled(!loading);
        btnRegister.setText(loading ? "Création en cours..." : "→  S'inscrire");
    }

    /** Lecture null-safe d'un TextInputEditText */
    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}