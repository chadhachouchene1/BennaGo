package com.example.bennago;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bennago.Adapters.MyReviewAdapter;
import com.example.bennago.db.AppDatabase;
import com.example.bennago.entity.Review;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReviewsActivity extends AppCompatActivity {

    private TextView[]         stars = new TextView[5];
    private TextInputEditText  etComment;
    private MaterialButton     btnSubmit;
    private RecyclerView       rvMyReviews;
    private TextView           tvNoReviews;

    private int selectedRating = 0;

    private SessionManager        sessionManager;
    private AppDatabase           db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviews);

        sessionManager = new SessionManager(this);
        db = AppDatabase.getInstance(this);

        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // ── Étoiles ───────────────────────────────────────────────────────────
        stars[0] = findViewById(R.id.star_1);
        stars[1] = findViewById(R.id.star_2);
        stars[2] = findViewById(R.id.star_3);
        stars[3] = findViewById(R.id.star_4);
        stars[4] = findViewById(R.id.star_5);

        for (int i = 0; i < 5; i++) {
            final int rating = i + 1;
            stars[i].setOnClickListener(v -> setRating(rating));
        }

        etComment   = findViewById(R.id.et_review_comment);
        btnSubmit   = findViewById(R.id.btn_submit_review);
        rvMyReviews = findViewById(R.id.rv_my_reviews);
        tvNoReviews = findViewById(R.id.tv_no_reviews);

        if (rvMyReviews != null)
            rvMyReviews.setLayoutManager(new LinearLayoutManager(this));

        btnSubmit.setOnClickListener(v -> submitReview());

        loadMyReviews();
    }

    // ── Mise à jour visuelle des étoiles ─────────────────────────────────────
    private void setRating(int rating) {
        selectedRating = rating;
        for (int i = 0; i < 5; i++) {
            if (stars[i] == null) continue;
            if (i < rating) {
                stars[i].setText("★");
                stars[i].setTextColor(0xFFD0A040); // étoile pleine dorée
            } else {
                stars[i].setText("☆");
                stars[i].setTextColor(0xFFD0B88A); // étoile vide
            }
        }
    }

    // ── Soumettre l'avis ─────────────────────────────────────────────────────
    private void submitReview() {
        if (selectedRating == 0) {
            Toast.makeText(this, "Veuillez choisir une note ⭐", Toast.LENGTH_SHORT).show();
            return;
        }

        String comment = etComment != null && etComment.getText() != null
                ? etComment.getText().toString().trim() : "";

        if (comment.isEmpty()) {
            Toast.makeText(this, "Veuillez écrire un commentaire", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Envoi...");

        int    userId     = sessionManager.getUserId();
        String clientName = sessionManager.getUserName();
        long   now        = System.currentTimeMillis();

        executor.execute(() -> {
            // Anti-doublon : max 1 avis par 24h par client
            long since = now - 24L * 60 * 60 * 1000;
            int recent = db.reviewDao().countRecentByUser(userId, since);

            if (recent > 0) {
                runOnUiThread(() -> {
                    Toast.makeText(this,
                            "Vous avez déjà donné un avis aujourd'hui 😊",
                            Toast.LENGTH_SHORT).show();
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("⭐  Envoyer mon avis");
                });
                return;
            }

            Review review = new Review(userId, clientName, selectedRating, comment, now);
            db.reviewDao().insertReview(review);

            runOnUiThread(() -> {
                Toast.makeText(this, "Merci pour votre avis ! ⭐", Toast.LENGTH_SHORT).show();
                etComment.setText("");
                setRating(0);
                selectedRating = 0;
                btnSubmit.setEnabled(true);
                btnSubmit.setText("⭐  Envoyer mon avis");
                loadMyReviews();
            });
        });
    }

    // ── Charger les avis du client connecté ──────────────────────────────────
    private void loadMyReviews() {
        executor.execute(() -> {
            List<Review> reviews = db.reviewDao().getReviewsByUser(sessionManager.getUserId());
            runOnUiThread(() -> {
                if (rvMyReviews == null || tvNoReviews == null) return;
                if (reviews.isEmpty()) {
                    tvNoReviews.setVisibility(android.view.View.VISIBLE);
                    rvMyReviews.setVisibility(android.view.View.GONE);
                } else {
                    tvNoReviews.setVisibility(android.view.View.GONE);
                    rvMyReviews.setVisibility(android.view.View.VISIBLE);
                    rvMyReviews.setAdapter(new MyReviewAdapter(reviews));
                }
            });
        });
    }
}