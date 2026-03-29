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
        CardView serviceComplaint = findViewById(R.id.serviceComplaint);
        serviceComplaint.setOnClickListener(v -> {
            startActivity(new Intent(this, ComplaintActivity.class));
        });

        CardView serviceMaintenance = findViewById(R.id.serviceMaintenance);
        if (serviceMaintenance != null) {
            serviceMaintenance.setOnClickListener(v -> {
                startActivity(new Intent(this, MaintenanceActivity.class));
            });
        }

        CardView serviceReservation = findViewById(R.id.serviceReservation);
        if (serviceReservation != null) {
            serviceReservation.setOnClickListener(v -> {
                startActivity(new Intent(this, ReservationsActivity.class));
            });
        }
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

        navFeed.setOnClickListener(v -> ActivityNavigationHelper.navigate(this, FeedActivity.class, false));

        navNotices.setOnClickListener(v -> ActivityNavigationHelper.navigate(this, NoticesActivity.class, true));

        navChat.setOnClickListener(v -> ActivityNavigationHelper.navigate(this, ChatActivity.class, true));

        navServices.setOnClickListener(v -> ActivityNavigationHelper.navigate(this, ServicesActivity.class, true));

        navProfile.setOnClickListener(v -> ActivityNavigationHelper.navigate(this, ProfileActivity.class, true));
    }
}
