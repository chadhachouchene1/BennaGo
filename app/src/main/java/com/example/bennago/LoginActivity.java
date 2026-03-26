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
    private TextView          tvLabelAdminCode;   // label "CODE SECRET" shown/hidden with admin tab
    private TextView          tvErrorMessage;     // global error banner under the button
    private TabLayout         tabLayout;

    private boolean isAdminMode = false;
    private AppDatabase    db;
    private SessionManager sessionManager;
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

    // ── View binding ──────────────────────────────────────────────────────────
    private void initViews() {
        etEmail          = findViewById(R.id.et_email);
        etPassword       = findViewById(R.id.et_password);
        etAdminCode      = findViewById(R.id.et_admin_code);
        tilEmail         = findViewById(R.id.til_email);
        tilPassword      = findViewById(R.id.til_password);
        tilAdminCode     = findViewById(R.id.til_admin_code);
        btnLogin         = findViewById(R.id.btn_login);
        tvRegisterLink   = findViewById(R.id.tv_register_link);
        tvLabelAdminCode = findViewById(R.id.tv_label_admin_code);
        tvErrorMessage   = findViewById(R.id.tv_error_message);
        tabLayout        = findViewById(R.id.tab_layout);
    }

    // ── Tabs setup ────────────────────────────────────────────────────────────
    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Connexion"));
        tabLayout.addTab(tabLayout.newTab().setText("Inscription"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // "Inscription" tab (index 1) = registration mode for users
                // We keep isAdminMode separate; the admin section is toggled via
                // a hidden long-press on the logo (or you can add a 3rd tab).
                // For now tab[0]=Connexion, tab[1]=Inscription (no admin tab shown).
                boolean isRegisterTab = (tab.getPosition() == 1);

                if (isRegisterTab) {
                    // Navigate directly to RegisterActivity
                    startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
                    tabLayout.selectTab(tabLayout.getTabAt(0)); // reset selection
                    return;
                }

                isAdminMode = false;
                updateUiForMode();
                clearAll();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    // ── Click listeners ───────────────────────────────────────────────────────
    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> {
            clearAll();
            if (isAdminMode) loginAdmin(); else loginUser();
        });

        tvRegisterLink.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        // Long-press on the button text area switches to admin mode (hidden shortcut)
        btnLogin.setOnLongClickListener(v -> {
            isAdminMode = !isAdminMode;
            updateUiForMode();
            clearAll();
            return true;
        });
    }

    // ── User login ────────────────────────────────────────────────────────────
    private void loginUser() {
        String email    = getText(etEmail);
        String password = getText(etPassword);

        if (TextUtils.isEmpty(email))    { tilEmail.setError("Email requis");           return; }
        if (TextUtils.isEmpty(password)) { tilPassword.setError("Mot de passe requis"); return; }

        setLoading(true);
        executor.execute(() -> {
            User userByEmail = db.userDao().getUserByEmail(email);
            runOnUiThread(() -> {
                setLoading(false);
                if (userByEmail == null) {
                    showError("Aucun compte trouvé avec cet email");
                } else if (!userByEmail.getPassword().equals(password)) {
                    showError("Mot de passe incorrect");
                } else {
                    sessionManager.saveUserSession(
                            userByEmail.getId(), userByEmail.getName(), false);
                    Toast.makeText(this,
                            "Bienvenue " + userByEmail.getName() + " !", Toast.LENGTH_SHORT).show();
                    goTo(MainActivity.class);
                }
            });
        });
    }

    // ── Admin login ───────────────────────────────────────────────────────────
    private void loginAdmin() {
        String email    = getText(etEmail);
        String password = getText(etPassword);
        String code     = getText(etAdminCode);

        if (!email.equals(ADMIN_EMAIL))       { tilEmail.setError("Email admin invalide");      return; }
        if (!password.equals(ADMIN_PASSWORD)) { tilPassword.setError("Mot de passe incorrect"); return; }
        if (!code.equals(ADMIN_CODE))         { tilAdminCode.setError("Code secret invalide");  return; }

        sessionManager.saveUserSession(-1, "Administrateur", true);
        Toast.makeText(this, "Bienvenue Admin !", Toast.LENGTH_SHORT).show();
        goTo(AdminActivity.class);
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    /** Update fields visibility and button label according to current mode. */
    private void updateUiForMode() {
        int adminVis = isAdminMode ? View.VISIBLE : View.GONE;
        tilAdminCode.setVisibility(adminVis);
        tvLabelAdminCode.setVisibility(adminVis);
        tvRegisterLink.setVisibility(isAdminMode ? View.GONE : View.VISIBLE);

        btnLogin.setText(isAdminMode ? "→  Connexion Admin" : "→  Se connecter");
        btnLogin.setBackgroundTintList(getColorStateList(
                isAdminMode ? R.color.brown_dark : R.color.orange_primary));
    }

    /** Show the red error banner below the button. */
    private void showError(String message) {
        tvErrorMessage.setText(message);
        tvErrorMessage.setVisibility(View.VISIBLE);
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
        btnLogin.setText(loading
                ? "Connexion..."
                : (isAdminMode ? "→  Connexion Admin" : "→  Se connecter"));
    }

    /** Clear all field errors + global error banner. */
    private void clearAll() {
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilAdminCode.setError(null);
        tvErrorMessage.setVisibility(View.GONE);
        tvErrorMessage.setText("");
    }

    /** Null-safe helper to read an EditText. */
    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}