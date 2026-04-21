package com.example.bennago;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bennago.db.AppDatabase;
import com.example.bennago.entity.Order;
import com.example.bennago.entity.OrderItem;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ManageOrdersActivity extends AppCompatActivity {

    private static final String TAG = "ManageOrders";

    private AppDatabase           db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private RecyclerView          rvOrders;
    private OrderAdapter          adapter;
    private TextView              tvOrderCount, tvEmpty;

    private static final String[] STATUSES = {
            "En attente", "Confirmée", "En livraison", "Livrée", "Annulée"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_orders);
        db = AppDatabase.getInstance(this);

        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        tvOrderCount = findViewById(R.id.tv_order_count);
        tvEmpty      = findViewById(R.id.tv_orders_empty);
        rvOrders     = findViewById(R.id.rv_orders);

        adapter = new OrderAdapter();
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        rvOrders.setAdapter(adapter);

        loadOrders();
    }

    @Override
    protected void onResume() { super.onResume(); loadOrders(); }

    private void loadOrders() {
        executor.execute(() -> {
            List<Order> orders = db.orderDao().getAllOrders();
            runOnUiThread(() -> {
                adapter.setOrders(orders);
                tvOrderCount.setText(orders.size() + " commande(s)");
                tvEmpty.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    // ── Changer statut + notifier le client ───────────────────────────────────
    private void showStatusDialog(Order order) {
        new AlertDialog.Builder(this)
                .setTitle("Changer le statut — Commande #" + order.getId())
                .setItems(STATUSES, (d, which) -> {
                    String newStatus = STATUSES[which];
                    executor.execute(() -> {
                        // 1. Mettre à jour le statut en DB
                        db.orderDao().updateStatus(order.getId(), newStatus);

                        // 2. Récupérer le token FCM du client
                        String clientToken = db.userDao().getFcmToken(order.getUserId());

                        // ✅ Log pour déboguer — visible dans Logcat filtré sur "ManageOrders"
                        Log.d(TAG, "UserId=" + order.getUserId()
                                + " | Token=" + (clientToken != null ? clientToken : "NULL ❌"));

                        if (clientToken != null && !clientToken.isEmpty()) {
                            // 3. Envoyer la notification au client
                            NotificationHelper.notifyClientStatusChanged(
                                    clientToken,
                                    order.getId(),
                                    newStatus
                            );
                            Log.d(TAG, "✅ Notification envoyée au client pour commande #"
                                    + order.getId() + " → " + newStatus);
                        } else {
                            Log.w(TAG, "❌ Token vide — le client n'a pas encore "
                                    + "sauvegardé son token FCM (userId=" + order.getUserId() + ")");
                        }

                        runOnUiThread(this::loadOrders);
                    });
                })
                .show();
    }

    // ── Détail commande ───────────────────────────────────────────────────────
    private void showOrderDetail(Order order) {
        executor.execute(() -> {
            List<OrderItem> items = db.orderDao().getItemsByOrder(order.getId());
            runOnUiThread(() -> {
                StringBuilder sb = new StringBuilder();
                sb.append("👤 ").append(order.getUserName()).append("\n");
                sb.append("📞 ").append(order.getUserPhone()).append("\n");
                sb.append("📍 ").append(order.getDeliveryAddress()).append("\n");
                if (order.getNote() != null && !order.getNote().isEmpty())
                    sb.append("📝 ").append(order.getNote()).append("\n");
                sb.append("\n── Articles ──\n");
                for (OrderItem oi : items) {
                    sb.append("• ").append(oi.getDishName())
                            .append(" x").append(oi.getQuantity())
                            .append(" = ").append(String.format("%.3f TND", oi.getSubtotal())).append("\n");
                }
                sb.append("\n💰 Total : ").append(String.format("%.3f TND", order.getTotalPrice()));

                new AlertDialog.Builder(this)
                        .setTitle("Commande #" + order.getId())
                        .setMessage(sb.toString())
                        .setPositiveButton("Fermer", null)
                        .setNeutralButton("Changer statut", (dialog, w) -> showStatusDialog(order))
                        .show();
            });
        });
    }

    // ── Adapter ───────────────────────────────────────────────────────────────
    class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.VH> {

        private List<Order> orders = new ArrayList<>();

        void setOrders(List<Order> o) { this.orders = o; notifyDataSetChanged(); }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_order, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            Order order = orders.get(pos);

            h.tvOrderId.setText("#" + order.getId());
            h.tvClient.setText(order.getUserName());
            h.tvPhone.setText(order.getUserPhone());
            h.tvAddress.setText(order.getDeliveryAddress());
            h.tvTotal.setText(String.format("%.3f TND", order.getTotalPrice()));
            h.tvStatus.setText(order.getStatus());
            h.tvDate.setText(new SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE)
                    .format(new Date(order.getCreatedAt())));

            int color;
            switch (order.getStatus()) {
                case "Confirmée":    color = 0xFF5A9E6F; break;
                case "En livraison": color = 0xFF4A90D9; break;
                case "Livrée":       color = 0xFF2E7D32; break;
                case "Annulée":      color = 0xFFB00020; break;
                default:             color = 0xFFD4703A; break;
            }
            h.tvStatus.setTextColor(color);

            h.itemView.setOnClickListener(v -> showOrderDetail(order));
            h.btnStatus.setOnClickListener(v -> showStatusDialog(order));
        }

        @Override public int getItemCount() { return orders == null ? 0 : orders.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView       tvOrderId, tvClient, tvPhone, tvAddress,
                    tvTotal, tvStatus, tvDate;
            MaterialButton btnStatus;

            VH(View v) {
                super(v);
                tvOrderId = v.findViewById(R.id.tv_order_id);
                tvClient  = v.findViewById(R.id.tv_order_client);
                tvPhone   = v.findViewById(R.id.tv_order_phone);
                tvAddress = v.findViewById(R.id.tv_order_address);
                tvTotal   = v.findViewById(R.id.tv_order_total);
                tvStatus  = v.findViewById(R.id.tv_order_status);
                tvDate    = v.findViewById(R.id.tv_order_date);
                btnStatus = v.findViewById(R.id.btn_change_status);
            }
        }
    }
}