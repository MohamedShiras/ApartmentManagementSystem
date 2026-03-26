package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setupBottomNavigation();
        setupQuickActions();
    }

    private void setupQuickActions() {

        MaterialButton logoutButton = findViewById(R.id.logoutButton);

        logoutButton.setOnClickListener(v -> {

            // 🔥 SIGN OUT FIREBASE
            FirebaseAuth.getInstance().signOut();

            // Optional: clear remembered credentials
            getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();

            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
        });
    }

    private void setupBottomNavigation() {
        FrameLayout navFeed = findViewById(R.id.nav_btn_feed);
        LinearLayout navNotices = findViewById(R.id.nav_btn_notices);
        LinearLayout navChat = findViewById(R.id.nav_btn_chat);
        LinearLayout navServices = findViewById(R.id.nav_btn_services);
        LinearLayout navProfile = findViewById(R.id.nav_btn_profile);

        // Profile is current page — do nothing on tap
        navProfile.setOnClickListener(v -> {});

        navFeed.setOnClickListener(v -> {
            startActivity(new Intent(this, FeedActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        navNotices.setOnClickListener(v -> {
            startActivity(new Intent(this, NoticesActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        navChat.setOnClickListener(v -> {
            startActivity(new Intent(this, ChatActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        navServices.setOnClickListener(v -> {
            startActivity(new Intent(this, ServicesActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
    }
}