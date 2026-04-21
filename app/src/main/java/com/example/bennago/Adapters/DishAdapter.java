package com.example.bennago.Adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bennago.R;
import com.example.bennago.entity.Dish;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DishAdapter extends RecyclerView.Adapter<DishAdapter.DishViewHolder> {

    // ── Interface callbacks ───────────────────────────────────────────────────
    public interface OnDishActionListener {
        void onEdit(Dish dish);
        void onDelete(Dish dish);
        void onToggleActive(Dish dish);
    }

    private List<Dish>                 dishes   = new ArrayList<>();
    private final OnDishActionListener listener;

    public DishAdapter(OnDishActionListener listener) {
        this.listener = listener;
    }

    // ── Mise à jour liste ─────────────────────────────────────────────────────
    public void setDishes(List<Dish> newDishes) {
        this.dishes = new ArrayList<>(newDishes);
        notifyDataSetChanged();
    }

    // ── Création ViewHolder ───────────────────────────────────────────────────
    @NonNull
    @Override
    public DishViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dish, parent, false);
        return new DishViewHolder(v);
    }

    // ── Remplissage données ───────────────────────────────────────────────────
    @Override
    public void onBindViewHolder(@NonNull DishViewHolder h, int position) {
        Dish dish = dishes.get(position);

        h.tvName.setText(dish.getName());
        h.tvPrice.setText(String.format("%.3f TND", dish.getPrice()));
        h.tvCategory.setText(dish.getCategory());

        // Image depuis chemin interne permanent
        loadImage(h.ivDish, dish.getImageUri());

        // Switch : détacher listener AVANT setChecked pour éviter callback involontaire
        h.switchActive.setOnCheckedChangeListener(null);
        h.switchActive.setChecked(dish.isActive());
        h.switchActive.setOnCheckedChangeListener((btn, isChecked) -> {
            dish.setActive(isChecked);
            h.itemView.setAlpha(isChecked ? 1.0f : 0.5f);
            listener.onToggleActive(dish);
        });

        // Plat inactif → semi-transparent
        h.itemView.setAlpha(dish.isActive() ? 1.0f : 0.5f);

        // Boutons actions
        h.btnEdit.setOnClickListener(v   -> listener.onEdit(dish));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(dish));
    }

    @Override
    public int getItemCount() {
        return dishes.size();
    }

    // ── Chargement image depuis chemin interne ────────────────────────────────
    private void loadImage(ImageView iv, String path) {
        if (path == null || path.isEmpty()) {
            iv.setImageResource(android.R.drawable.ic_menu_gallery);
            return;
        }
        try {
            File f = new File(path);
            if (f.exists()) {
                Bitmap bmp = BitmapFactory.decodeFile(path);
                iv.setImageBitmap(bmp);
            } else {
                iv.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } catch (Exception e) {
            iv.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────
    static class DishViewHolder extends RecyclerView.ViewHolder {
        ImageView   ivDish;
        TextView    tvName, tvPrice, tvCategory;
        Switch      switchActive;
        ImageButton btnEdit, btnDelete;

        DishViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDish       = itemView.findViewById(R.id.iv_dish_client);
            tvName       = itemView.findViewById(R.id.tv_dish_name);
            tvPrice      = itemView.findViewById(R.id.tv_dish_price);
            tvCategory   = itemView.findViewById(R.id.tv_dish_category);
            switchActive = itemView.findViewById(R.id.switch_active);
            btnEdit      = itemView.findViewById(R.id.btn_edit_dish);
            btnDelete    = itemView.findViewById(R.id.btn_delete_dish);
        }
    }
}