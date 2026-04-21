package com.example.bennago;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bennago.Adapters.DishAdapter;
import com.example.bennago.db.AppDatabase;
import com.example.bennago.entity.Dish;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ManageMenuActivity extends AppCompatActivity
        implements DishAdapter.OnDishActionListener {

    public static final int REQUEST_ADD_EDIT = 100;

    private AppDatabase           db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private RecyclerView         rvDishes;
    private DishAdapter          adapter;
    private TextView             tvDishCount, tvEmpty;
    private FloatingActionButton fabAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_menu);
        db = AppDatabase.getInstance(this);
        initViews();
        loadDishes();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        tvDishCount = findViewById(R.id.tv_dish_count);
        tvEmpty     = findViewById(R.id.tv_empty);
        rvDishes    = findViewById(R.id.rv_dishes);
        fabAdd      = findViewById(R.id.fab_add_dish);

        adapter = new DishAdapter(this);
        rvDishes.setLayoutManager(new LinearLayoutManager(this));
        rvDishes.setAdapter(adapter);

        fabAdd.setOnClickListener(v ->
                startActivityForResult(
                        new Intent(this, AddEditDishActivity.class),
                        REQUEST_ADD_EDIT));
    }

    private void loadDishes() {
        executor.execute(() -> {
            List<Dish> dishes = db.dishDao().getAllDishes();
            runOnUiThread(() -> {
                adapter.setDishes(dishes);
                tvDishCount.setText(dishes.size() + " plats au menu");
                tvEmpty.setVisibility(dishes.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    // ── ✅ FIX Modifier : passe TOUTES les données correctement ───────────────
    @Override
    public void onEdit(Dish dish) {
        Intent i = new Intent(this, AddEditDishActivity.class);
        i.putExtra("dish_id",          dish.getId());
        i.putExtra("dish_name",        dish.getName());
        i.putExtra("dish_price",       dish.getPrice());      // double
        i.putExtra("dish_category",    dish.getCategory());
        i.putExtra("dish_description", dish.getDescription() != null ? dish.getDescription() : "");
        i.putExtra("dish_image",       dish.getImageUri()    != null ? dish.getImageUri()    : "");
        i.putExtra("dish_active",      dish.isActive());
        startActivityForResult(i, REQUEST_ADD_EDIT);
    }

    // ── Supprimer ─────────────────────────────────────────────────────────────
    @Override
    public void onDelete(Dish dish) {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer le plat")
                .setMessage("Supprimer \"" + dish.getName() + "\" du menu ?")
                .setPositiveButton("Supprimer", (d, w) ->
                        executor.execute(() -> {
                            db.dishDao().deleteDish(dish);
                            runOnUiThread(this::loadDishes);
                        }))
                .setNegativeButton("Annuler", null)
                .show();
    }

    // ── Activer / Désactiver ──────────────────────────────────────────────────
    @Override
    public void onToggleActive(Dish dish) {
        executor.execute(() ->
                db.dishDao().setDishActive(dish.getId(), dish.isActive()));
    }

    // ── ✅ Retour depuis formulaire → recharge la liste ───────────────────────
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_EDIT && resultCode == RESULT_OK) {
            loadDishes();
        }
    }
}