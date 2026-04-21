package com.example.bennago;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bennago.Adapters.AdminReviewAdapter;
import com.example.bennago.db.AppDatabase;
import com.example.bennago.entity.Review;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdminReviewsActivity extends AppCompatActivity {

    private RecyclerView  rvReviews;
    private View          layoutNoReviews;
    private TextView      tvReviewCount, tvAvgRating, tvAvgStars;

    private AppDatabase           db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_reviews);

        db = AppDatabase.getInstance(this);

        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        rvReviews       = findViewById(R.id.rv_reviews);
        layoutNoReviews = findViewById(R.id.layout_no_reviews);
        tvReviewCount   = findViewById(R.id.tv_review_count);
        tvAvgRating     = findViewById(R.id.tv_avg_rating);
        tvAvgStars      = findViewById(R.id.tv_avg_stars);

        if (rvReviews != null)
            rvReviews.setLayoutManager(new LinearLayoutManager(this));

        loadReviews();
    }

    private void loadReviews() {
        executor.execute(() -> {
            List<Review> reviews = db.reviewDao().getAllReviews();
            int    count  = db.reviewDao().getReviewCount();
            double avgRaw = db.reviewDao().getAverageRating();
            double avg    = Math.round(avgRaw * 10.0) / 10.0;

            runOnUiThread(() -> {
                // Compteur
                if (tvReviewCount != null)
                    tvReviewCount.setText(count + " avis");

                // Moyenne
                if (tvAvgRating != null)
                    tvAvgRating.setText(String.format("  %.1f/5", avg));

                // Étoiles visuelles selon la moyenne
                if (tvAvgStars != null)
                    tvAvgStars.setText(buildStars(avg));

                if (reviews.isEmpty()) {
                    if (layoutNoReviews != null) layoutNoReviews.setVisibility(View.VISIBLE);
                    if (rvReviews != null)       rvReviews.setVisibility(View.GONE);
                } else {
                    if (layoutNoReviews != null) layoutNoReviews.setVisibility(View.GONE);
                    if (rvReviews != null) {
                        rvReviews.setVisibility(View.VISIBLE);
                        rvReviews.setAdapter(new AdminReviewAdapter(reviews));
                    }
                }
            });
        });
    }

    // ── Génère les étoiles visuelles (pleine / demi / vide) ──────────────────
    private String buildStars(double avg) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            if (avg >= i)       sb.append("★");
            else if (avg >= i - 0.5) sb.append("⯨");
            else                sb.append("☆");
        }
        return sb.toString();
    }
}