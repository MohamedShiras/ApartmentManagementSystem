package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MaintenanceActivity extends AppCompatActivity {

    public static final String EXTRA_SERVICE_TITLE = "extra_service_title";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_maintenance);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_maintenance), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Back button -> FeedActivity
        View btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> ActivityNavigationHelper.navigate(this, FeedActivity.class, true));

        setupServiceNavigation();
        setupViewMoreNavigation();
        setupBottomNavigation();
    }

    private void setupViewMoreNavigation() {
        View tvViewMore = findViewById(R.id.tvViewMore);
        tvViewMore.setOnClickListener(v -> {
            Intent intent = new Intent(this, MaintenanceNavigationActivity.class);
            startActivity(intent);
        });
    }

    private void setupServiceNavigation() {
        findViewById(R.id.servicePlumbing).setOnClickListener(v -> openServiceOptions("Plumbing Maintenance"));
        findViewById(R.id.serviceElectric).setOnClickListener(v -> openServiceOptions("Electric Maintenance"));
        findViewById(R.id.serviceGas).setOnClickListener(v -> openServiceOptions("Gas Maintenance"));
        findViewById(R.id.serviceAC).setOnClickListener(v -> openServiceOptions("AC Maintenance"));
        findViewById(R.id.serviceCarpentry).setOnClickListener(v -> openServiceOptions("Carpentry Maintenance"));
        findViewById(R.id.serviceCleaning).setOnClickListener(v -> openServiceOptions("Cleaning Maintenance"));
    }

    private void openServiceOptions(String serviceTitle) {
        Intent intent = new Intent(this, MaintenanceOptionActivity.class);
        intent.putExtra(EXTRA_SERVICE_TITLE, serviceTitle);
        startActivity(intent);
    }

    private void setupBottomNavigation() {
        FrameLayout navFeed = findViewById(R.id.nav_btn_feed);
        LinearLayout navNotices = findViewById(R.id.nav_btn_notices);
        LinearLayout navChat = findViewById(R.id.nav_btn_chat);
        LinearLayout navServices = findViewById(R.id.nav_btn_services);
        LinearLayout navProfile = findViewById(R.id.nav_btn_profile);

        navFeed.setOnClickListener(v -> ActivityNavigationHelper.navigate(this, FeedActivity.class, true));

        navNotices.setOnClickListener(v -> ActivityNavigationHelper.navigate(this, NoticesActivity.class, true));

        navChat.setOnClickListener(v -> ActivityNavigationHelper.navigate(this, ChatActivity.class, true));

        navServices.setOnClickListener(v -> ActivityNavigationHelper.navigate(this, ServicesActivity.class, true));

        navProfile.setOnClickListener(v -> ActivityNavigationHelper.navigate(this, ProfileActivity.class, true));
    }
}