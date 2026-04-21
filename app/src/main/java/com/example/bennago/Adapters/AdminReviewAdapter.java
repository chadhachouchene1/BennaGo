package com.example.bennago.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bennago.R;
import com.example.bennago.entity.Review;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminReviewAdapter extends RecyclerView.Adapter<AdminReviewAdapter.VH> {

    private final List<Review> reviews;

    public AdminReviewAdapter(List<Review> reviews) {
        this.reviews = reviews;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Review r = reviews.get(position);

        // Avatar lettre
        String name = r.getClientName();
        if (name != null && !name.isEmpty())
            h.tvAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());

        h.tvName.setText(name != null ? name : "Anonyme");

        // Date formatée
        String date = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)
                .format(new Date(r.getCreatedAt()));
        h.tvDate.setText(date);

        // Note chiffre
        h.tvRating.setText(String.valueOf(r.getRating()));

        // Étoiles visuelles
        h.tvStars.setText(buildStars(r.getRating()));

        // Commentaire
        h.tvComment.setText(r.getComment());
    }

    @Override public int getItemCount() { return reviews.size(); }

    private String buildStars(int rating) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++)
            sb.append(i <= rating ? "★" : "☆");
        return sb.toString();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvDate, tvRating, tvStars, tvComment;

        VH(View v) {
            super(v);
            tvAvatar  = v.findViewById(R.id.tv_review_avatar);
            tvName    = v.findViewById(R.id.tv_review_client_name);
            tvDate    = v.findViewById(R.id.tv_review_date);
            tvRating  = v.findViewById(R.id.tv_review_rating);
            tvStars   = v.findViewById(R.id.tv_review_stars);
            tvComment = v.findViewById(R.id.tv_review_comment);
        }
    }
}