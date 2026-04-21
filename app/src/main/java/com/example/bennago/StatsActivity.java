package com.example.bennago;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bennago.dao.OrderDao;
import com.example.bennago.db.AppDatabase;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatsActivity extends AppCompatActivity {

    // Chiffres clés
    private TextView tvStatUsers, tvStatDishes, tvStatOrders, tvStatRevenue;

    // Top plats
    private TextView tvTop1Name, tvTop1Count;
    private TextView tvTop2Name, tvTop2Count;
    private TextView tvTop3Name, tvTop3Count;

    // Barres catégories
    private View     barBurger,  barPizza,  barBoisson,  barAutre;
    private TextView tvCountBurger, tvCountPizza, tvCountBoisson, tvCountAutre;

    // Commandes en attente
    private TextView tvPendingOrders, tvNewUsersWeek;

    private AppDatabase           db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);
        db = AppDatabase.getInstance(this);
        initViews();
        loadAllStats();
    }

    @Override
    protected void onResume() { super.onResume(); loadAllStats(); }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        tvStatUsers   = findViewById(R.id.tv_stat_users);
        tvStatDishes  = findViewById(R.id.tv_stat_dishes);
        tvStatOrders  = findViewById(R.id.tv_stat_orders);
        tvStatRevenue = findViewById(R.id.tv_stat_revenue);

        tvTop1Name  = findViewById(R.id.tv_top1_name);
        tvTop1Count = findViewById(R.id.tv_top1_count);
        tvTop2Name  = findViewById(R.id.tv_top2_name);
        tvTop2Count = findViewById(R.id.tv_top2_count);
        tvTop3Name  = findViewById(R.id.tv_top3_name);
        tvTop3Count = findViewById(R.id.tv_top3_count);

        barBurger      = findViewById(R.id.bar_burger);
        barPizza       = findViewById(R.id.bar_pizza);
        barBoisson     = findViewById(R.id.bar_boisson);
        barAutre       = findViewById(R.id.bar_autre);
        tvCountBurger  = findViewById(R.id.tv_count_burger);
        tvCountPizza   = findViewById(R.id.tv_count_pizza);
        tvCountBoisson = findViewById(R.id.tv_count_boisson);
        tvCountAutre   = findViewById(R.id.tv_count_autre);

        tvPendingOrders = findViewById(R.id.tv_pending_orders);
        tvNewUsersWeek  = findViewById(R.id.tv_new_users_week);
    }

    private void loadAllStats() {
        executor.execute(() -> {
            // ── Clients ───────────────────────────────────────────────────────
            int totalUsers   = db.userDao().getUserCount();

            // ── Plats ─────────────────────────────────────────────────────────
            int activeDishes = db.dishDao().getActiveDishCount();

            // ── Commandes ─────────────────────────────────────────────────────
            int totalOrders   = db.orderDao().getOrderCount();
            int pendingOrders = db.orderDao().getPendingOrderCount();

            // ✅ Revenus réels des commandes livrées
            double totalRevenue = db.orderDao().getTotalRevenue();

            // ── Plats par catégorie ───────────────────────────────────────────
            int countBurger  = db.dishDao().getCountByCategory("Burger");
            int countPizza   = db.dishDao().getCountByCategory("Pizza");
            int countBoisson = db.dishDao().getCountByCategory("Boisson");
            int countAutre   = Math.max(0,
                    db.dishDao().getDishCount() - countBurger - countPizza - countBoisson);

            // ✅ Top 3 plats depuis la vraie DB (order_items)
            List<OrderDao.TopDishResult> top3 = db.orderDao().getTop3Dishes();

            final String top1Name  = top3.size() > 0 ? top3.get(0).dishName : "--";
            final int    top1Count = top3.size() > 0 ? top3.get(0).totalQty : 0;
            final String top2Name  = top3.size() > 1 ? top3.get(1).dishName : "--";
            final int    top2Count = top3.size() > 1 ? top3.get(1).totalQty : 0;
            final String top3Name  = top3.size() > 2 ? top3.get(2).dishName : "--";
            final int    top3Count = top3.size() > 2 ? top3.get(2).totalQty : 0;

            // Finales
            final int    fUsers    = totalUsers;
            final int    fDishes   = activeDishes;
            final int    fOrders   = totalOrders;
            final int    fPending  = pendingOrders;
            final double fRevenue  = totalRevenue;
            final int    fBurger   = countBurger;
            final int    fPizza    = countPizza;
            final int    fBoisson  = countBoisson;
            final int    fAutre    = countAutre;
            final int    fMax      = Math.max(1,
                    Math.max(fBurger, Math.max(fPizza, Math.max(fBoisson, fAutre))));

            runOnUiThread(() -> {
                // Chiffres clés
                if (tvStatUsers  != null) tvStatUsers.setText(String.valueOf(fUsers));
                if (tvStatDishes != null) tvStatDishes.setText(String.valueOf(fDishes));
                if (tvStatOrders != null) tvStatOrders.setText(String.valueOf(fOrders));
                if (tvStatRevenue!= null)
                    tvStatRevenue.setText(String.format("%.2f", fRevenue));

                // Commandes en attente
                if (tvPendingOrders != null)
                    tvPendingOrders.setText(String.valueOf(fPending));

                // Top 3 plats — données réelles
                if (tvTop1Name  != null) tvTop1Name.setText(top1Name);
                if (tvTop1Count != null) tvTop1Count.setText(top1Count > 0 ? top1Count + " fois" : "--");
                if (tvTop2Name  != null) tvTop2Name.setText(top2Name);
                if (tvTop2Count != null) tvTop2Count.setText(top2Count > 0 ? top2Count + " fois" : "--");
                if (tvTop3Name  != null) tvTop3Name.setText(top3Name);
                if (tvTop3Count != null) tvTop3Count.setText(top3Count > 0 ? top3Count + " fois" : "--");

                // Barres catégories
                if (tvCountBurger  != null) tvCountBurger.setText(String.valueOf(fBurger));
                if (tvCountPizza   != null) tvCountPizza.setText(String.valueOf(fPizza));
                if (tvCountBoisson != null) tvCountBoisson.setText(String.valueOf(fBoisson));
                if (tvCountAutre   != null) tvCountAutre.setText(String.valueOf(fAutre));

                if (barBurger  != null) barBurger.post(() -> setBarWidth(barBurger,  fBurger,  fMax));
                if (barPizza   != null) barPizza.post(()  -> setBarWidth(barPizza,   fPizza,   fMax));
                if (barBoisson != null) barBoisson.post(() -> setBarWidth(barBoisson, fBoisson, fMax));
                if (barAutre   != null) barAutre.post(()  -> setBarWidth(barAutre,   fAutre,   fMax));

                // Nouveaux clients
                if (tvNewUsersWeek != null) tvNewUsersWeek.setText(String.valueOf(fUsers));
            });
        });
    }

    private void setBarWidth(View bar, int value, int max) {
        if (bar == null || bar.getParent() == null) return;
        View parent    = (View) bar.getParent();
        int  available = parent.getWidth() - dpToPx(90 + 36 + 6);
        if (available <= 0) available = dpToPx(150);
        int barWidth = Math.max(dpToPx(4), (int) ((float) value / max * available));
        android.view.ViewGroup.LayoutParams lp = bar.getLayoutParams();
        lp.width = barWidth;
        bar.setLayoutParams(lp);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}