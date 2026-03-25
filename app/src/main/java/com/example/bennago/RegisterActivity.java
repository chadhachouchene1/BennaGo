package com.example.bennago;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPhone, etPassword, etConfirmPassword;
    private TextInputLayout   tilName, tilEmail, tilPhone, tilPassword, tilConfirmPassword;
    private MaterialButton    btnRegister;
    private TextView          tvLoginLink;
    private AppDatabase       db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        db = AppDatabase.getInstance(this);
        initViews();
        setupClickListeners();
    }

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

    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> validateAndRegister());
        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void validateAndRegister() {
        String name            = etName.getText().toString().trim();
        String email           = etEmail.getText().toString().trim();
        String phone           = etPhone.getText().toString().trim();
        String password        = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        tilName.setError(null);
        tilEmail.setError(null);
        tilPhone.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        boolean valid = true;
        if (TextUtils.isEmpty(name))  { tilName.setError("Le nom est requis");    valid = false; }
        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email invalide"); valid = false;
        }
        if (TextUtils.isEmpty(phone)) { tilPhone.setError("Le téléphone est requis"); valid = false; }
        if (password.length() < 6)    { tilPassword.setError("Minimum 6 caractères"); valid = false; }
        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Les mots de passe ne correspondent pas"); valid = false;
        }
        if (!valid) return;

        btnRegister.setEnabled(false);
        btnRegister.setText("Création en cours...");

        executor.execute(() -> {
            User existing = db.userDao().getUserByEmail(email);
            runOnUiThread(() -> {
                if (existing != null) {
                    tilEmail.setError("Cet email est déjà utilisé");
                    btnRegister.setEnabled(true);
                    btnRegister.setText(getString(R.string.btn_register));
                    return;
                }
                User newUser = new User(name, email, phone, password);
                executor.execute(() -> {
                    db.userDao().insertUser(newUser);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Compte créé avec succès ! 🎉", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    });
                });
            });
        });
    }
}