package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


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
        logoutButton.setOnClickListener(v -> handleLogout());
    }

    private void handleLogout() {
        String token = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                .getString("access_token", null);

        if (token == null) {
            clearAndGoToLogin();
            return;
        }

        // Sign out from Supabase on background thread
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection)
                        new URL(SupabaseClient.SUPABASE_URL + "/auth/v1/logout")
                                .openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write("{}".getBytes());
                os.close();
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) {
                // Even if the call fails, clear locally and go to login
            }

            runOnUiThread(this::clearAndGoToLogin);
        }).start();
    }

    private void clearAndGoToLogin() {
        getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupBottomNavigation() {
        FrameLayout navFeed      = findViewById(R.id.nav_btn_feed);
        LinearLayout navNotices  = findViewById(R.id.nav_btn_notices);
        LinearLayout navChat     = findViewById(R.id.nav_btn_chat);
        LinearLayout navServices = findViewById(R.id.nav_btn_services);
        LinearLayout navProfile  = findViewById(R.id.nav_btn_profile);

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