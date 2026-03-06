package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;


import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        this.getSupportActionBar().hide();
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavBar);

        // ✅ Set listener FIRST before setSelectedItemId
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_profile) {
                // Already here, do nothing
                return true;

            } else if (id == R.id.nav_feed) {
                startActivity(new Intent(this, FeedActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;

            } else if (id == R.id.nav_announcements) {
                // Uncomment when AnnouncementActivity is ready
                 startActivity(new Intent(this, AnnouncementActivity.class));
                 overridePendingTransition(0, 0);
                 finish();
                return true;

            } else if (id == R.id.nav_chat) {
                // Uncomment when ChatActivity is ready
                 startActivity(new Intent(this, ChatActivity.class));
                 overridePendingTransition(0, 0);
                 finish();
                return true;

            } else if (id == R.id.nav_services) {
                // Uncomment when ServicesActivity is ready
                 startActivity(new Intent(this, ServicesActivity.class));
                 overridePendingTransition(0, 0);
                 finish();
                return true;
            }

            return false;
        });

        // ✅ Set selected item AFTER listener is attached
        bottomNav.setSelectedItemId(R.id.nav_profile);
    }
}