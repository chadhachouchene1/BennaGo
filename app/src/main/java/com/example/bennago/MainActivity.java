package com.example.bennago;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bennago.Adapters.ClientDishAdapter;
import com.example.bennago.db.AppDatabase;
import com.example.bennago.entity.Dish;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private SessionManager        sessionManager;
    private AppDatabase           db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ClientDishAdapter adapter;
    private List<Dish>        allDishes       = new ArrayList<>();
    private String            currentCategory = "Tous";
    private String            currentSearch   = "";   // ✅ recherche en cours

    private TextView tvFilterAll, tvFilterBurger, tvFilterPizza;
    private TextView tvFilterSandwich, tvFilterDessert, tvFilterBoisson;
    private TextView tvEmpty, tvMenuTitle, tvDishCountLabel;
    private TextView tvCategoriesTitle;
    private View     scrollChips;
    private TextView tvCartBadge;
    private TextView btnClearSearch;  // ✅
    private EditText etSearch;        // ✅

    private final ActivityResultLauncher<String> notifPermLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> android.util.Log.d("FCM",
                            granted ? "Permission notifs accordée ✅"
                                    : "Permission notifs refusée ❌"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isUserLoggedIn()) { redirectToLogin(); return; }

        setContentView(R.layout.activity_main);
        db = AppDatabase.getInstance(this);

        NotificationHelper.createClientChannel(this);
        askNotificationPermission();
        saveFcmTokenForCurrentUser();

        initViews();
        setupSearch();       // ✅
        setupBottomNavigation();
        loadDishes();
    }

    // ── Permission notifications Android 13+ ─────────────────────────────────
    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void saveFcmTokenForCurrentUser() {
        int userId = sessionManager.getUserId();
        if (userId == -1) return;
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (token != null)
                        executor.execute(() -> db.userDao().updateFcmToken(userId, token));
                });
    }

    // ── Liaison XML ───────────────────────────────────────────────────────────
    private void initViews() {
        TextView tvUserName     = findViewById(R.id.tv_user_name);
        TextView tvAvatarLetter = findViewById(R.id.tv_avatar_letter);
        String name = sessionManager.getUserName();
        if (tvUserName != null)     tvUserName.setText(name);
        if (tvAvatarLetter != null && name != null && !name.isEmpty())
            tvAvatarLetter.setText(String.valueOf(name.charAt(0)).toUpperCase());

        View avatar = findViewById(R.id.iv_avatar);
        if (avatar != null)
            avatar.setOnClickListener(v ->
                    startActivity(new Intent(this, ProfileActivity.class)));

        tvCartBadge      = findViewById(R.id.tv_cart_badge);
        etSearch         = findViewById(R.id.et_search);         // ✅
        btnClearSearch   = findViewById(R.id.btn_clear_search);  // ✅
        tvCategoriesTitle = findViewById(R.id.tv_categories_title);
        scrollChips      = findViewById(R.id.scroll_chips);
        tvMenuTitle      = findViewById(R.id.tv_menu_title);
        tvDishCountLabel = findViewById(R.id.tv_dish_count_label);
        tvEmpty          = findViewById(R.id.tv_menu_empty);

        RecyclerView rvMenu = findViewById(R.id.rv_menu);
        adapter = new ClientDishAdapter();
        adapter.setOnCartChangedListener(this::updateCartBadge);
        if (rvMenu != null) {
            rvMenu.setLayoutManager(new LinearLayoutManager(this));
            rvMenu.setAdapter(adapter);
        }

        tvFilterAll      = findViewById(R.id.chip_all);
        tvFilterBurger   = findViewById(R.id.chip_burger);
        tvFilterPizza    = findViewById(R.id.chip_pizza);
        tvFilterSandwich = findViewById(R.id.chip_sandwich);
        tvFilterDessert  = findViewById(R.id.chip_dessert);
        tvFilterBoisson  = findViewById(R.id.chip_boisson);

        if (tvFilterAll      != null) tvFilterAll.setOnClickListener(v      -> applyFilter("Tous"));
        if (tvFilterBurger   != null) tvFilterBurger.setOnClickListener(v   -> applyFilter("Burger"));
        if (tvFilterPizza    != null) tvFilterPizza.setOnClickListener(v    -> applyFilter("Pizza"));
        if (tvFilterSandwich != null) tvFilterSandwich.setOnClickListener(v -> applyFilter("Sandwich"));
        if (tvFilterDessert  != null) tvFilterDessert.setOnClickListener(v  -> applyFilter("Dessert"));
        if (tvFilterBoisson  != null) tvFilterBoisson.setOnClickListener(v  -> applyFilter("Boisson"));
    }

    // ── Logique de recherche ─────────────────────────────────────────────────
    private void setupSearch() {
        if (etSearch == null) return;

        // Texte tapé → filtre en temps réel
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                currentSearch = s.toString().trim();
                applySearchAndFilter();

                // Afficher / cacher le bouton ✕
                if (btnClearSearch != null)
                    btnClearSearch.setVisibility(
                            currentSearch.isEmpty() ? View.GONE : View.VISIBLE);

                // Cacher catégories pendant la recherche
                boolean searching = !currentSearch.isEmpty();
                if (tvCategoriesTitle != null)
                    tvCategoriesTitle.setVisibility(searching ? View.GONE : View.VISIBLE);
                if (scrollChips != null)
                    scrollChips.setVisibility(searching ? View.GONE : View.VISIBLE);
                if (tvMenuTitle != null)
                    tvMenuTitle.setText(searching ? "Résultats" : "Notre Menu");
            }
        });

        // Touche "Rechercher" du clavier → ferme le clavier
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                return true;
            }
            return false;
        });

        // Bouton ✕ → efface la recherche
        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                etSearch.setText("");
                currentSearch = "";
                hideKeyboard();
            });
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)
                getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && etSearch != null)
            imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
    }

    // ── Chargement plats ──────────────────────────────────────────────────────
    private void loadDishes() {
        executor.execute(() -> {
            List<Dish> dishes = db.dishDao().getActiveDishes();
            runOnUiThread(() -> {
                allDishes = dishes;
                applySearchAndFilter();
            });
        });
    }

    // ── Filtre par catégorie (chips) ─────────────────────────────────────────
    private void applyFilter(String category) {
        currentCategory = category;
        updateChipStyle(tvFilterAll,      "Tous".equals(category));
        updateChipStyle(tvFilterBurger,   "Burger".equals(category));
        updateChipStyle(tvFilterPizza,    "Pizza".equals(category));
        updateChipStyle(tvFilterSandwich, "Sandwich".equals(category));
        updateChipStyle(tvFilterDessert,  "Dessert".equals(category));
        updateChipStyle(tvFilterBoisson,  "Boisson".equals(category));
        applySearchAndFilter();
    }

    // ── Filtre combiné : catégorie + recherche texte ──────────────────────────
    private void applySearchAndFilter() {
        List<Dish> filtered = new ArrayList<>();
        String query = currentSearch.toLowerCase();

        for (Dish d : allDishes) {
            // Filtre catégorie
            boolean matchCategory = "Tous".equals(currentCategory)
                    || currentCategory.equals(d.getCategory());

            // Filtre recherche : nom OU description OU catégorie
            boolean matchSearch = query.isEmpty()
                    || d.getName().toLowerCase().contains(query)
                    || (d.getDescription() != null
                    && d.getDescription().toLowerCase().contains(query))
                    || (d.getCategory() != null
                    && d.getCategory().toLowerCase().contains(query));

            if (matchCategory && matchSearch) filtered.add(d);
        }

        adapter.setDishes(filtered);

        // Compteur
        if (tvDishCountLabel != null)
            tvDishCountLabel.setText(filtered.isEmpty() ? "" : filtered.size() + " plat(s)");

        // Message vide
        if (tvEmpty != null) {
            if (filtered.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText(currentSearch.isEmpty()
                        ? "Aucun plat disponible dans cette catégorie."
                        : "Aucun plat trouvé pour \"" + currentSearch + "\"");
            } else {
                tvEmpty.setVisibility(View.GONE);
            }
        }
    }

    private void updateChipStyle(TextView chip, boolean selected) {
        if (chip == null) return;
        chip.setBackgroundColor(selected ? 0xFF291C0E : 0xFFEADFD8);
        chip.setTextColor(selected ? 0xFFF2EBE0 : 0xFF6E473B);
    }

    private void updateCartBadge() {
        if (tvCartBadge == null) return;
        int count = CartManager.getInstance().getTotalCount();
        tvCartBadge.setText(String.valueOf(count));
        tvCartBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDishes();
        updateCartBadge();
        // Rafraîchir nom/avatar si modifié dans ProfileActivity
        TextView tvUserName     = findViewById(R.id.tv_user_name);
        TextView tvAvatarLetter = findViewById(R.id.tv_avatar_letter);
        String name = sessionManager.getUserName();
        if (tvUserName != null)     tvUserName.setText(name);
        if (tvAvatarLetter != null && name != null && !name.isEmpty())
            tvAvatarLetter.setText(String.valueOf(name.charAt(0)).toUpperCase());
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav == null) return;
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home)    return true;
            if (id == R.id.nav_cart)    { startActivity(new Intent(this, CartActivity.class));     return true; }
            if (id == R.id.nav_orders)  { startActivity(new Intent(this, MyOrdersActivity.class)); return true; }
            if (id == R.id.nav_profile) { startActivity(new Intent(this, ProfileActivity.class));  return true; }
            return false;
        });
    }

    private void redirectToLogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    @Override public void onBackPressed() { super.onBackPressed(); moveTaskToBack(true); }
}