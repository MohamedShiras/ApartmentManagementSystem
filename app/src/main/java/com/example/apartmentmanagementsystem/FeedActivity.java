package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class FeedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setupBottomNavigation();
        setupPostButton();
        setupQuickActions();
    }

    private void setupQuickActions() {
        CardView pillMaintenance = findViewById(R.id.pillMaintenance);
        pillMaintenance.setOnClickListener(v -> {
            startActivity(new Intent(this, ComplaintActivity.class));
        });
    }

    private void setupPostButton() {
        CardView btnPost = findViewById(R.id.btnPost);
        btnPost.setOnClickListener(v -> {
            startActivity(new Intent(this, PostActivity.class));
            overridePendingTransition(0, 0);
        });
    }

    private void setupBottomNavigation() {
        FrameLayout navFeed = findViewById(R.id.nav_btn_feed);
        LinearLayout navNotices = findViewById(R.id.nav_btn_notices);
        LinearLayout navChat = findViewById(R.id.nav_btn_chat);
        LinearLayout navServices = findViewById(R.id.nav_btn_services);
        LinearLayout navProfile = findViewById(R.id.nav_btn_profile);

        // Feed is current page — do nothing on tap
        navFeed.setOnClickListener(v -> {});

        navNotices.setOnClickListener(v -> {
            startActivity(new Intent(this, AnnouncementActivity.class));
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

        navProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
    }
}
