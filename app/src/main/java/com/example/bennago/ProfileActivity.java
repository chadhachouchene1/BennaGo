package com.example.bennago;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
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

public class ProfileActivity extends AppCompatActivity {

    private TextView           tvAvatar, tvNameHeader, tvEmailHeader, tvEditToggle;
    private TextInputEditText  etName, etEmail, etPhone;
    private TextInputEditText  etCurrentPassword, etNewPassword, etConfirmNewPassword;
    private TextInputLayout    tilCurrentPassword, tilNewPassword, tilConfirmNewPassword;
    private MaterialButton     btnSaveProfile, btnChangePassword, btnLogout;
    private MaterialButton     btnMyReviews;  // ✅

    private TextView tvOrderCount, tvTotalSpent;

    private SessionManager        sessionManager;
    private AppDatabase           db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private boolean editMode = false;
    private User    currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager = new SessionManager(this);
        db = AppDatabase.getInstance(this);

        bindViews();
        loadUserData();
        loadStats();

        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        tvEditToggle.setOnClickListener(v -> toggleEditMode());
        btnSaveProfile.setOnClickListener(v -> saveProfile());
        btnChangePassword.setOnClickListener(v -> changePassword());
        btnLogout.setOnClickListener(v -> confirmLogout());

        // ✅ Bouton "Mes avis" → ouvre ReviewsActivity
        btnMyReviews.setOnClickListener(v ->
                startActivity(new Intent(this, ReviewsActivity.class)));
    }

    private void bindViews() {
        tvAvatar              = findViewById(R.id.tv_profile_avatar);
        tvNameHeader          = findViewById(R.id.tv_profile_name_header);
        tvEmailHeader         = findViewById(R.id.tv_profile_email_header);
        tvEditToggle          = findViewById(R.id.tv_edit_toggle);
        etName                = findViewById(R.id.et_profile_name);
        etEmail               = findViewById(R.id.et_profile_email);
        etPhone               = findViewById(R.id.et_profile_phone);
        etCurrentPassword     = findViewById(R.id.et_current_password);
        etNewPassword         = findViewById(R.id.et_new_password);
        etConfirmNewPassword  = findViewById(R.id.et_confirm_new_password);
        tilCurrentPassword    = findViewById(R.id.til_current_password);
        tilNewPassword        = findViewById(R.id.til_new_password);
        tilConfirmNewPassword = findViewById(R.id.til_confirm_new_password);
        btnSaveProfile        = findViewById(R.id.btn_save_profile);
        btnChangePassword     = findViewById(R.id.btn_change_password);
        btnLogout             = findViewById(R.id.btn_logout);
        btnMyReviews          = findViewById(R.id.btn_my_reviews); // ✅
        tvOrderCount          = findViewById(R.id.tv_profile_order_count);
        tvTotalSpent          = findViewById(R.id.tv_profile_total_spent);
    }

    private void loadUserData() {
        int userId = sessionManager.getUserId();
        executor.execute(() -> {
            currentUser = db.userDao().getUserById(userId);
            runOnUiThread(() -> {
                if (currentUser == null) return;
                String name = currentUser.getName();
                if (name != null && !name.isEmpty()) {
                    tvAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());
                    tvNameHeader.setText(name);
                }
                tvEmailHeader.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
                etName.setText(name);
                etEmail.setText(currentUser.getEmail());
                etPhone.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "");
            });
        });
    }

    private void loadStats() {
        int userId = sessionManager.getUserId();
        executor.execute(() -> {
            int    orderCount  = db.orderDao().getOrderCountByUser(userId);
            double totalSpent  = db.orderDao().getTotalSpentByUser(userId);
            runOnUiThread(() -> {
                tvOrderCount.setText(String.valueOf(orderCount));
                tvTotalSpent.setText(String.format("%.3f TND", totalSpent));
            });
        });
    }

    private void toggleEditMode() {
        editMode = !editMode;
        etName.setEnabled(editMode);
        etPhone.setEnabled(editMode);
        etEmail.setEnabled(false); // email non modifiable
        tvEditToggle.setText(editMode ? "Annuler" : "Modifier");
        btnSaveProfile.setVisibility(editMode ? View.VISIBLE : View.GONE);
        if (editMode) etName.requestFocus();
        else loadUserData(); // annulation → recharger
    }

    private void saveProfile() {
        String name  = getText(etName);
        String phone = getText(etPhone);
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Le nom ne peut pas être vide", Toast.LENGTH_SHORT).show();
            return;
        }
        executor.execute(() -> {
            db.userDao().updateNameAndPhone(sessionManager.getUserId(), name, phone);
            sessionManager.updateUserName(name);
            runOnUiThread(() -> {
                Toast.makeText(this, "✅ Profil mis à jour !", Toast.LENGTH_SHORT).show();
                tvNameHeader.setText(name);
                if (!name.isEmpty())
                    tvAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());
                toggleEditMode();
            });
        });
    }

    private void changePassword() {
        String currentPwd = getText(etCurrentPassword);
        String newPwd     = getText(etNewPassword);
        String confirmPwd = getText(etConfirmNewPassword);

        tilCurrentPassword.setError(null);
        tilNewPassword.setError(null);
        tilConfirmNewPassword.setError(null);

        boolean valid = true;
        if (TextUtils.isEmpty(currentPwd)) {
            tilCurrentPassword.setError("Mot de passe actuel requis"); valid = false;
        }
        if (TextUtils.isEmpty(newPwd) || newPwd.length() < 8) {
            tilNewPassword.setError("Minimum 8 caractères"); valid = false;
        }
        if (!newPwd.equals(confirmPwd)) {
            tilConfirmNewPassword.setError("Les mots de passe ne correspondent pas"); valid = false;
        }
        if (!valid) return;

        executor.execute(() -> {
            User user = db.userDao().getUserById(sessionManager.getUserId());
            if (user == null) return;
            if (!user.getPassword().equals(currentPwd)) {
                runOnUiThread(() ->
                        tilCurrentPassword.setError("Mot de passe actuel incorrect"));
                return;
            }
            db.userDao().updatePassword(sessionManager.getUserId(), newPwd);
            runOnUiThread(() -> {
                Toast.makeText(this, "🔐 Mot de passe modifié !", Toast.LENGTH_SHORT).show();
                etCurrentPassword.setText("");
                etNewPassword.setText("");
                etConfirmNewPassword.setText("");
            });
        });
    }

    private void confirmLogout() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Déconnexion")
                .setMessage("Voulez-vous vous déconnecter ?")
                .setPositiveButton("Oui", (d, w) -> {
                    sessionManager.logout();
                    CartManager.reset();
                    Intent i = new Intent(this, LoginActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                })
                .setNegativeButton("Annuler", null).show();
    }

    private String getText(TextInputEditText et) {
        return et != null && et.getText() != null ? et.getText().toString().trim() : "";
    }
}