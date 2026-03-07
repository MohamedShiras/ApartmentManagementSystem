package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class FeedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setupBottomNavigation();
        setupMaintenanceClick();
    }

    private void setupMaintenanceClick() {
        CardView maintenancePill = findViewById(R.id.pillMaintenance);
        maintenancePill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FeedActivity.this, ComplaintDashboardActivity.class));
            }
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavBar);
        bottomNav.setSelectedItemId(R.id.nav_feed);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_feed) {
                // Already here, do nothing
                return true;

            } else if (id == R.id.nav_announcements) {
                startActivity(new Intent(this, AnnouncementActivity.class));
                overridePendingTransition(0, 0);
                return true;

            } else if (id == R.id.nav_chat) {
                startActivity(new Intent(this, ChatActivity.class));
                overridePendingTransition(0, 0);
                return true;

            } else if (id == R.id.nav_services) {
                startActivity(new Intent(this, ServicesActivity.class));
                overridePendingTransition(0, 0);
                return true;

            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }

            return false;
        });
    }
}