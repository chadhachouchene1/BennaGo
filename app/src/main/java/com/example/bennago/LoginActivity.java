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

    private static final String ADMIN_EMAIL    = "admin@bennago.com";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String ADMIN_CODE     = "BG2024";

    private TextInputEditText etEmail, etPassword, etAdminCode;
    private TextInputLayout   tilEmail, tilPassword, tilAdminCode;
    private MaterialButton    btnLogin;
    private TextView          tvRegisterLink;
    private TabLayout         tabLayout;

    private boolean isAdminMode = false;
    private AppDatabase    db;
    private SessionManager sessionManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (sessionManager.isAdminLoggedIn()) { goTo(AdminActivity.class);  return; }
        if (sessionManager.isUserLoggedIn())  { goTo(MainActivity.class);   return; }

        setContentView(R.layout.activity_login);
        db = AppDatabase.getInstance(this);
        initViews();
        setupTabs();
        setupClickListeners();
    }

    private void initViews() {
        // FIX: IDs now match the corrected activity_login.xml
        etEmail        = findViewById(R.id.et_email);
        etPassword     = findViewById(R.id.et_password);
        etAdminCode    = findViewById(R.id.et_admin_code);      // was et_admin
        tilEmail       = findViewById(R.id.til_email);
        tilPassword    = findViewById(R.id.til_password);
        tilAdminCode   = findViewById(R.id.til_admin_code);     // was til_admin
        btnLogin       = findViewById(R.id.btn_login);
        tvRegisterLink = findViewById(R.id.tv_register_link);   // was missing in XML
        tabLayout      = findViewById(R.id.tab_layout);
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Utilisateur"));
        tabLayout.addTab(tabLayout.newTab().setText("Admin"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isAdminMode = (tab.getPosition() == 1);
                tilAdminCode.setVisibility(isAdminMode ? View.VISIBLE : View.GONE);
                tvRegisterLink.setVisibility(isAdminMode ? View.GONE : View.VISIBLE);
                btnLogin.setText(isAdminMode ? "Connexion Admin" : getString(R.string.btn_login));
                btnLogin.setBackgroundTintList(getColorStateList(
                        isAdminMode ? R.color.brown_dark : R.color.orange_primary));
                clearErrors();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> { if (isAdminMode) loginAdmin(); else loginUser(); });
        tvRegisterLink.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void loginUser() {
        // FIX: null-safe getText() — avoids NullPointerException
        String email    = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        clearErrors();
        if (TextUtils.isEmpty(email))    { tilEmail.setError("Email requis");           return; }
        if (TextUtils.isEmpty(password)) { tilPassword.setError("Mot de passe requis"); return; }

        setLoading(true);
        executor.execute(() -> {
            User user = db.userDao().login(email, password);
            runOnUiThread(() -> {
                setLoading(false);
                if (user != null) {
                    sessionManager.saveUserSession(user.getId(), user.getName(), false);
                    Toast.makeText(this, "Bienvenue " + user.getName() + " !", Toast.LENGTH_SHORT).show();
                    goTo(MainActivity.class);
                } else {
                    tilPassword.setError("Email ou mot de passe incorrect");
                }
            });
        });
    }

    private void loginAdmin() {
        // FIX: null-safe getText() — avoids NullPointerException
        String email    = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String code     = etAdminCode.getText() != null ? etAdminCode.getText().toString().trim() : "";
        clearErrors();
        if (!email.equals(ADMIN_EMAIL))       { tilEmail.setError("Email admin invalide");      return; }
        if (!password.equals(ADMIN_PASSWORD)) { tilPassword.setError("Mot de passe incorrect"); return; }
        if (!code.equals(ADMIN_CODE))         { tilAdminCode.setError("Code secret invalide");  return; }

        sessionManager.saveUserSession(-1, "Administrateur", true);
        Toast.makeText(this, "Bienvenue Admin !", Toast.LENGTH_SHORT).show();
        goTo(AdminActivity.class);
    }

    private void goTo(Class<?> dest) {
        Intent i = new Intent(this, dest);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Connexion..." : (isAdminMode ? "Connexion Admin" : getString(R.string.btn_login)));
    }

    private void clearErrors() {
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilAdminCode.setError(null);
    }
}