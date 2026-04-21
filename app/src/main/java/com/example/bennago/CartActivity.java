package com.example.bennago;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class CartActivity extends AppCompatActivity {

    private CartManager          cart;
    private CartItemAdapter      adapter;
    private TextView             tvTotal, tvEmpty, tvItemCount;
    private RecyclerView         rvCart;
    private MaterialButton       btnCheckout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);
        cart = CartManager.getInstance();

        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        tvTotal     = findViewById(R.id.tv_cart_total);
        tvEmpty     = findViewById(R.id.tv_cart_empty);
        tvItemCount = findViewById(R.id.tv_cart_item_count);
        rvCart      = findViewById(R.id.rv_cart);
        btnCheckout = findViewById(R.id.btn_checkout);

        adapter = new CartItemAdapter();
        rvCart.setLayoutManager(new LinearLayoutManager(this));
        rvCart.setAdapter(adapter);

        btnCheckout.setOnClickListener(v -> {
            if (cart.isEmpty()) {
                Toast.makeText(this, "Votre panier est vide", Toast.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(this, CheckoutActivity.class));
            }
        });

        refresh();
    }

    @Override
    protected void onResume() { super.onResume(); refresh(); }

    private void refresh() {
        List<CartManager.CartItem> items = cart.getItems();
        adapter.notifyDataSetChanged();
        tvTotal.setText(String.format("Total : %.3f TND", cart.getTotalPrice()));
        tvItemCount.setText(cart.getTotalCount() + " article(s)");
        tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        rvCart.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        btnCheckout.setEnabled(!items.isEmpty());
    }

    // ── Adapter inline ────────────────────────────────────────────────────────
    class CartItemAdapter extends RecyclerView.Adapter<CartItemAdapter.VH> {

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_cart, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            CartManager.CartItem item = cart.getItems().get(pos);
            h.tvName.setText(item.dish.getName());
            h.tvPrice.setText(String.format("%.3f TND", item.dish.getPrice()));
            h.tvQty.setText(String.valueOf(item.quantity));
            h.tvSubtotal.setText(String.format("%.3f TND", item.getSubtotal()));

            h.btnPlus.setOnClickListener(v -> {
                cart.increaseQty(item.dish.getId());
                refresh();
            });
            h.btnMinus.setOnClickListener(v -> {
                cart.decreaseQty(item.dish.getId());
                refresh();
            });
            h.btnRemove.setOnClickListener(v -> {
                cart.removeDish(item.dish.getId());
                refresh();
            });
        }

        @Override public int getItemCount() { return cart.getItems().size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView    tvName, tvPrice, tvQty, tvSubtotal;
            ImageButton btnPlus, btnMinus, btnRemove;
            VH(View v) {
                super(v);
                tvName    = v.findViewById(R.id.tv_cart_dish_name);
                tvPrice   = v.findViewById(R.id.tv_cart_dish_price);
                tvQty     = v.findViewById(R.id.tv_cart_qty);
                tvSubtotal= v.findViewById(R.id.tv_cart_subtotal);
                btnPlus   = v.findViewById(R.id.btn_qty_plus);
                btnMinus  = v.findViewById(R.id.btn_qty_minus);
                btnRemove = v.findViewById(R.id.btn_remove_item);
            }
        }
    }
}