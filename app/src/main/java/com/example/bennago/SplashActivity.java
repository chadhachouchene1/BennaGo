package com.example.bennago;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2800;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        TextView tvIcon     = findViewById(R.id.tv_logo_icon);
        TextView tvName     = findViewById(R.id.tv_app_name);
        TextView tvTagline  = findViewById(R.id.tv_tagline);
        ProgressBar progressBar = findViewById(R.id.progress_bar);

        animateViews(tvIcon, tvName, tvTagline, progressBar);

        new Handler().postDelayed(() -> {
            SessionManager session = new SessionManager(this);
            Intent intent;

            if (session.isAdminLoggedIn()) {
                intent = new Intent(this, AdminActivity.class);
            } else if (session.isUserLoggedIn()) {
                intent = new Intent(this, MainActivity.class);
            } else {
                intent = new Intent(this, LoginActivity.class);
            }

            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, SPLASH_DURATION);
    }

    private void animateViews(View icon, View name, View tagline, View progress) {
        // Icon: scale + fade in
        ObjectAnimator iconAlpha  = ObjectAnimator.ofFloat(icon, "alpha", 0f, 1f);
        ObjectAnimator iconScaleX = ObjectAnimator.ofFloat(icon, "scaleX", 0.3f, 1f);
        ObjectAnimator iconScaleY = ObjectAnimator.ofFloat(icon, "scaleY", 0.3f, 1f);
        AnimatorSet iconSet = new AnimatorSet();
        iconSet.playTogether(iconAlpha, iconScaleX, iconScaleY);
        iconSet.setDuration(600);
        iconSet.setInterpolator(new DecelerateInterpolator());
        iconSet.setStartDelay(200);

        // Name: slide up + fade in
        name.setTranslationY(40f);
        ObjectAnimator nameAlpha  = ObjectAnimator.ofFloat(name, "alpha", 0f, 1f);
        ObjectAnimator nameTransY = ObjectAnimator.ofFloat(name, "translationY", 40f, 0f);
        AnimatorSet nameSet = new AnimatorSet();
        nameSet.playTogether(nameAlpha, nameTransY);
        nameSet.setDuration(500);
        nameSet.setStartDelay(700);

        // Tagline: fade in
        ObjectAnimator taglineAlpha = ObjectAnimator.ofFloat(tagline, "alpha", 0f, 1f);
        taglineAlpha.setDuration(400);
        taglineAlpha.setStartDelay(1100);

        // Progress: fade in
        ObjectAnimator progressAlpha = ObjectAnimator.ofFloat(progress, "alpha", 0f, 1f);
        progressAlpha.setDuration(400);
        progressAlpha.setStartDelay(1400);

        iconSet.start();
        nameSet.start();
        taglineAlpha.start();
        progressAlpha.start();
    }
}