package com.example.bennago;

import android.os.Bundle;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyOrdersActivity extends AppCompatActivity {

    private AppDatabase           db;
    private SessionManager        sessionManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private RecyclerView   rvOrders;
    private MyOrderAdapter adapter;
    private View           layoutEmpty;   // ✅ FIX : View au lieu de TextView (c'est un LinearLayout dans le XML)
    private TextView       tvOrderCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_orders);

        sessionManager = new SessionManager(this);
        db             = AppDatabase.getInstance(this);

        initViews();
        loadOrders();
    }

    @Override
    protected void onResume() { super.onResume(); loadOrders(); }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        tvOrderCount = findViewById(R.id.tv_order_count);
        layoutEmpty  = findViewById(R.id.layout_orders_empty);  // ✅ FIX : id correspondant au LinearLayout
        rvOrders     = findViewById(R.id.rv_my_orders);

        adapter = new MyOrderAdapter();
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        rvOrders.setAdapter(adapter);
    }

    private void loadOrders() {
        int userId = sessionManager.getUserId();
        executor.execute(() -> {
            List<Order> orders = db.orderDao().getOrdersByUser(userId);
            runOnUiThread(() -> {
                adapter.setOrders(orders);
                if (tvOrderCount != null)
                    tvOrderCount.setText(orders.size() + " commande(s)");

                boolean empty = orders.isEmpty();
                if (layoutEmpty != null) layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                if (rvOrders    != null) rvOrders.setVisibility(empty ? View.GONE : View.VISIBLE);
            });
        });
    }

    // ── Détail commande ───────────────────────────────────────────────────────
    private void showDetail(Order order) {
        executor.execute(() -> {
            List<OrderItem> items = db.orderDao().getItemsByOrder(order.getId());
            runOnUiThread(() -> {
                StringBuilder sb = new StringBuilder();
                sb.append("📅 ").append(new SimpleDateFormat("dd/MM/yyyy HH:mm",
                        Locale.FRANCE).format(new Date(order.getCreatedAt()))).append("\n\n");
                sb.append("── Articles ──\n");
                for (OrderItem oi : items) {
                    sb.append("• ").append(oi.getDishName())
                            .append("  x").append(oi.getQuantity())
                            .append("  → ").append(String.format("%.3f TND", oi.getSubtotal())).append("\n");
                }
                sb.append("\n📍 ").append(order.getDeliveryAddress()).append("\n");
                sb.append("📞 ").append(order.getUserPhone()).append("\n");
                if (order.getNote() != null && !order.getNote().isEmpty())
                    sb.append("📝 ").append(order.getNote()).append("\n");
                sb.append("\n💰 Total : ").append(String.format("%.3f TND", order.getTotalPrice()));

                new AlertDialog.Builder(this)
                        .setTitle("Commande #" + order.getId() + "  —  " + order.getStatus())
                        .setMessage(sb.toString())
                        .setPositiveButton("Fermer", null)
                        .show();
            });
        });
    }

    // ── Adapter ───────────────────────────────────────────────────────────────
    class MyOrderAdapter extends RecyclerView.Adapter<MyOrderAdapter.VH> {

        private List<Order> orders = new ArrayList<>();

        void setOrders(List<Order> o) { this.orders = o; notifyDataSetChanged(); }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_my_order, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            Order order = orders.get(pos);

            h.tvOrderId.setText("Commande #" + order.getId());
            h.tvDate.setText(new SimpleDateFormat("dd/MM/yyyy  HH:mm",
                    Locale.FRANCE).format(new Date(order.getCreatedAt())));
            h.tvTotal.setText(String.format("%.3f TND", order.getTotalPrice()));
            h.tvAddress.setText(order.getDeliveryAddress());
            h.tvStatus.setText(order.getStatus());

            // Couleur statut
            int color;
            switch (order.getStatus()) {
                case "Confirmée":    color = 0xFF5A9E6F; break;
                case "En livraison": color = 0xFF4A90D9; break;
                case "Livrée":       color = 0xFF2E7D32; break;
                case "Annulée":      color = 0xFFB00020; break;
                default:             color = 0xFFD4703A; break;
            }
            h.tvStatus.setTextColor(color);

            // Icône statut
            String icon;
            switch (order.getStatus()) {
                case "Confirmée":    icon = "✅"; break;
                case "En livraison": icon = "🚚"; break;
                case "Livrée":       icon = "🎉"; break;
                case "Annulée":      icon = "❌"; break;
                default:             icon = "⏳"; break;
            }
            h.tvStatusIcon.setText(icon);

            h.itemView.setOnClickListener(v -> showDetail(order));
            h.tvDetail.setOnClickListener(v -> showDetail(order));
        }

        @Override public int getItemCount() { return orders.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvOrderId, tvDate, tvTotal, tvAddress,
                    tvStatus, tvStatusIcon, tvDetail;

            VH(View v) {
                super(v);
                tvOrderId   = v.findViewById(R.id.tv_my_order_id);
                tvDate      = v.findViewById(R.id.tv_my_order_date);
                tvTotal     = v.findViewById(R.id.tv_my_order_total);
                tvAddress   = v.findViewById(R.id.tv_my_order_address);
                tvStatus    = v.findViewById(R.id.tv_my_order_status);
                tvStatusIcon= v.findViewById(R.id.tv_my_order_status_icon);
                tvDetail    = v.findViewById(R.id.tv_my_order_detail);
            }
        }
    }
}