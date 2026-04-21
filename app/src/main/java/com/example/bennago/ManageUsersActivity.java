package com.example.bennago;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bennago.db.AppDatabase;
import com.example.bennago.entity.User;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ManageUsersActivity extends AppCompatActivity {

    private AppDatabase           db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private RecyclerView      rvUsers;
    private UserAdapter       adapter;
    private TextView          tvUserCount, tvEmpty;
    private TextInputEditText etSearch;

    private List<User> allUsers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);
        db = AppDatabase.getInstance(this);

        initViews();
        loadUsers();
    }

    @Override
    protected void onResume() { super.onResume(); loadUsers(); }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        tvUserCount = findViewById(R.id.tv_user_count);
        tvEmpty     = findViewById(R.id.tv_users_empty);
        rvUsers     = findViewById(R.id.rv_users);
        etSearch    = findViewById(R.id.et_search_users);

        adapter = new UserAdapter();
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(adapter);

        // Recherche en temps réel
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterUsers(s.toString().trim());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void loadUsers() {
        executor.execute(() -> {
            allUsers = db.userDao().getAllUsers();
            runOnUiThread(() -> {
                filterUsers(etSearch != null && etSearch.getText() != null
                        ? etSearch.getText().toString().trim() : "");
                tvUserCount.setText(allUsers.size() + " client(s)");
            });
        });
    }

    private void filterUsers(String query) {
        List<User> filtered = new ArrayList<>();
        for (User u : allUsers) {
            if (query.isEmpty()
                    || u.getName().toLowerCase().contains(query.toLowerCase())
                    || u.getEmail().toLowerCase().contains(query.toLowerCase())
                    || (u.getPhone() != null && u.getPhone().contains(query))) {
                filtered.add(u);
            }
        }
        adapter.setUsers(filtered);
        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ── Dialog détail client ──────────────────────────────────────────────────
    private void showUserDetail(User user) {
        String info = "📧 " + user.getEmail() + "\n"
                + "📞 " + (user.getPhone() != null ? user.getPhone() : "--") + "\n"
                + "🔒 Statut : " + (user.isBlocked() ? "Bloqué ❌" : "Actif ✅");

        new AlertDialog.Builder(this)
                .setTitle("👤 " + user.getName())
                .setMessage(info)
                .setPositiveButton("Fermer", null)
                .setNeutralButton(user.isBlocked() ? "Débloquer" : "Bloquer", (d, w) ->
                        toggleBlock(user))
                .setNegativeButton("Supprimer", (d, w) ->
                        confirmDelete(user))
                .show();
    }

    // ── Bloquer / Débloquer ───────────────────────────────────────────────────
    private void toggleBlock(User user) {
        boolean newState = !user.isBlocked();
        String msg = newState
                ? "Bloquer le compte de " + user.getName() + " ?"
                : "Débloquer le compte de " + user.getName() + " ?";

        new AlertDialog.Builder(this)
                .setTitle(newState ? "Bloquer le client" : "Débloquer le client")
                .setMessage(msg)
                .setPositiveButton("Confirmer", (d, w) ->
                        executor.execute(() -> {
                            db.userDao().setUserBlocked(user.getId(), newState);
                            runOnUiThread(this::loadUsers);
                        }))
                .setNegativeButton("Annuler", null)
                .show();
    }

    // ── Supprimer ─────────────────────────────────────────────────────────────
    private void confirmDelete(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer le client")
                .setMessage("Supprimer définitivement le compte de " + user.getName() + " ?\nCette action est irréversible.")
                .setPositiveButton("Supprimer", (d, w) ->
                        executor.execute(() -> {
                            db.userDao().deleteUser(user);
                            runOnUiThread(this::loadUsers);
                        }))
                .setNegativeButton("Annuler", null)
                .show();
    }

    // ── Adapter ───────────────────────────────────────────────────────────────
    class UserAdapter extends RecyclerView.Adapter<UserAdapter.VH> {

        private List<User> users = new ArrayList<>();

        void setUsers(List<User> u) { this.users = u; notifyDataSetChanged(); }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_user, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            User user = users.get(pos);

            h.tvName.setText(user.getName());
            h.tvEmail.setText(user.getEmail());
            h.tvPhone.setText(user.getPhone() != null ? user.getPhone() : "--");

            // Avatar lettre
            if (user.getName() != null && !user.getName().isEmpty())
                h.tvAvatar.setText(String.valueOf(user.getName().charAt(0)).toUpperCase());

            // Switch bloqué
            h.switchBlocked.setOnCheckedChangeListener(null);
            h.switchBlocked.setChecked(!user.isBlocked()); // checked = actif
            h.switchBlocked.setOnCheckedChangeListener((btn, isActive) -> {
                user.setBlocked(!isActive);
                h.itemView.setAlpha(isActive ? 1f : 0.5f);
                executor.execute(() ->
                        db.userDao().setUserBlocked(user.getId(), !isActive));
            });

            // Opacité si bloqué
            h.itemView.setAlpha(user.isBlocked() ? 0.5f : 1f);

            // Clic → détail
            h.itemView.setOnClickListener(v -> showUserDetail(user));

            // Bouton supprimer
            h.btnDelete.setOnClickListener(v -> confirmDelete(user));
        }

        @Override public int getItemCount() { return users.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView    tvAvatar, tvName, tvEmail, tvPhone;
            Switch      switchBlocked;
            ImageButton btnDelete;

            VH(View v) {
                super(v);
                tvAvatar      = v.findViewById(R.id.tv_user_avatar);
                tvName        = v.findViewById(R.id.tv_user_name);
                tvEmail       = v.findViewById(R.id.tv_user_email);
                tvPhone       = v.findViewById(R.id.tv_user_phone);
                switchBlocked = v.findViewById(R.id.switch_user_active);
                btnDelete     = v.findViewById(R.id.btn_delete_user);
            }
        }
    }
}