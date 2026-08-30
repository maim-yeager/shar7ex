package com.example.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.R;
import com.example.data.FirebaseManager;
import com.example.ui.auth.AuthActivity;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        animateLogoEntrance();
        new Handler(Looper.getMainLooper()).postDelayed(this::proceed, 1200);
    }

    private void animateLogoEntrance() {
        ImageView logo = findViewById(R.id.ivSplashLogo);
        if (logo == null) return;
        logo.setScaleX(0f);
        logo.setScaleY(0f);
        logo.setAlpha(0f);
        logo.animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(650)
                .setInterpolator(new OvershootInterpolator(2.4f))
                .start();
    }

    private void proceed() {
        FirebaseManager manager = FirebaseManager.getInstance(this);
        if (manager.isLoggedIn()) {
            // Hydrate the real Firestore profile before entering the app so every
            // screen that reads FirebaseManager.getCurrentUser() has real data.
            manager.loadCurrentUser(user -> {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            });
        } else {
            Intent intent = new Intent(SplashActivity.this, AuthActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
