package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ServicesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_services);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_services), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;


        });
        setupQuickActions();
        setupBottomNavigation();
    }

    private void setupQuickActions() {
        // 2. USE THE CORRECT ID: cardMaintenance (matches activity_services.xml)
        CardView cardComplaint = findViewById(R.id.cardComplaint);
        if (cardComplaint != null) {
            cardComplaint.setOnClickListener(v -> {
                startActivity(new Intent(this, ComplaintActivity.class));
            });
        }

        CardView cardMaintenance = findViewById(R.id.cardMaintenance);
        if (cardMaintenance != null) {
            cardMaintenance.setOnClickListener(v -> {
                startActivity(new Intent(this, MaintenanceActivity.class));
            });
        }

        CardView cardReservation = findViewById(R.id.cardReservation);
        if (cardReservation != null) {
            cardReservation.setOnClickListener(v -> {
                startActivity(new Intent(this, ReservationsActivity.class));
            });
        }
    }

    private void setupBottomNavigation() {
        FrameLayout navFeed = findViewById(R.id.nav_btn_feed);
        LinearLayout navNotices = findViewById(R.id.nav_btn_notices);
        LinearLayout navChat = findViewById(R.id.nav_btn_chat);
        LinearLayout navServices = findViewById(R.id.nav_btn_services);
        LinearLayout navProfile = findViewById(R.id.nav_btn_profile);

        navServices.setOnClickListener(v -> ActivityNavigationHelper.navigate(this, ServicesActivity.class, false));

        navNotices.setOnClickListener(v -> ActivityNavigationHelper.navigate(this, NoticesActivity.class, true));

        navChat.setOnClickListener(v -> ActivityNavigationHelper.navigate(this, ChatActivity.class, true));

        navFeed.setOnClickListener(v -> ActivityNavigationHelper.navigate(this, FeedActivity.class, true));

        navProfile.setOnClickListener(v -> ActivityNavigationHelper.navigate(this, ProfileActivity.class, true));
    }
}
