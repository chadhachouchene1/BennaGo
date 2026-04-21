package com.example.bennago.Adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bennago.CartManager;
import com.example.bennago.R;
import com.example.bennago.entity.Dish;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ClientDishAdapter extends RecyclerView.Adapter<ClientDishAdapter.DishViewHolder> {

    public interface OnCartChangedListener { void onCartChanged(); }

    private List<Dish>            dishes   = new ArrayList<>();
    private OnCartChangedListener listener;

    public void setDishes(List<Dish> newDishes) {
        this.dishes = new ArrayList<>(newDishes);
        notifyDataSetChanged();
    }

    public void setOnCartChangedListener(OnCartChangedListener l) { this.listener = l; }

    @NonNull
    @Override
    public DishViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dish_client, parent, false);
        return new DishViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DishViewHolder h, int position) {
        Dish dish = dishes.get(position);

        h.tvName.setText(dish.getName());
        h.tvCategory.setText(dish.getCategory());
        h.tvPrice.setText(String.format("%.3f TND", dish.getPrice()));

        if (dish.getDescription() != null && !dish.getDescription().isEmpty()) {
            h.tvDescription.setText(dish.getDescription());
            h.tvDescription.setVisibility(View.VISIBLE);
        } else {
            h.tvDescription.setVisibility(View.GONE);
        }

        loadImage(h.ivDish, dish.getImageUri());

        // ✅ Ajouter au panier
        h.btnAdd.setOnClickListener(v -> {
            CartManager.getInstance().addDish(dish);
            Toast.makeText(v.getContext(),
                    dish.getName() + " ajouté au panier 🛒", Toast.LENGTH_SHORT).show();
            if (listener != null) listener.onCartChanged();
        });
    }

    @Override
    public int getItemCount() { return dishes.size(); }

    private void loadImage(ImageView iv, String path) {
        if (path == null || path.isEmpty()) {
            iv.setImageResource(android.R.drawable.ic_menu_gallery);
            return;
        }
        try {
            File f = new File(path);
            if (f.exists()) iv.setImageBitmap(BitmapFactory.decodeFile(path));
            else            iv.setImageResource(android.R.drawable.ic_menu_gallery);
        } catch (Exception e) {
            iv.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    static class DishViewHolder extends RecyclerView.ViewHolder {
        ImageView    ivDish;
        TextView     tvName, tvCategory, tvDescription, tvPrice;
        MaterialButton btnAdd; // ✅ Changé de TextView à MaterialButton

        DishViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDish        = itemView.findViewById(R.id.iv_dish_client);
            tvName        = itemView.findViewById(R.id.tv_client_dish_name);
            tvCategory    = itemView.findViewById(R.id.tv_client_dish_category);
            tvDescription = itemView.findViewById(R.id.tv_client_dish_description);
            tvPrice       = itemView.findViewById(R.id.tv_client_dish_price);
            btnAdd        = itemView.findViewById(R.id.btn_add_to_cart); // ✅
        }
    }
}