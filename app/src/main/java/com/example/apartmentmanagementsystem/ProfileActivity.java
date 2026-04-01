package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ProfileActivity extends AppCompatActivity {
    private TextView avatarInitialsTop;
    private TextView avatarInitials, residentName, residentUnit, memberSince;
    private TextView statPosts, statRequests, statTenure;
    private TextView phoneValue, emailValue;
    private TextView leaseExpiryValue, rentDueValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // System bar insets
        CoordinatorLayout root = findViewById(R.id.profile);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, 0);
            return insets;
        });

        // Fix bottom nav insets same as Feed
        android.widget.RelativeLayout bottomNav = findViewById(R.id.bottom_nav_container);
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams lp =
                    (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) v.getLayoutParams();
            lp.bottomMargin = bars.bottom;
            v.setLayoutParams(lp);
            return insets;
        });

        initViews();
        setupBottomNavigation();
        setupQuickActions();
        loadProfileFromSupabase();
    }

    // ── Bind all views ──────────────────────────────────────────────────────
    private void initViews() {
        avatarInitialsTop  = findViewById(R.id.avatarInitialsTop);
        avatarInitials     = findViewById(R.id.avatarInitials);
        residentName       = findViewById(R.id.residentName);
        residentUnit       = findViewById(R.id.residentUnit);
        memberSince        = findViewById(R.id.memberSince);
        statPosts          = findViewById(R.id.statPosts);
        statRequests       = findViewById(R.id.statRequests);
        statTenure         = findViewById(R.id.statTenure);
        phoneValue         = findViewById(R.id.phoneValue);
        emailValue         = findViewById(R.id.emailValue);
        leaseExpiryValue   = findViewById(R.id.leaseExpiryValue);
        rentDueValue       = findViewById(R.id.rentDueValue);
    }

    // ── Fetch profile data from Supabase ────────────────────────────────────
    private void loadProfileFromSupabase() {
        String token = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                .getString("access_token", null);

        if (token == null) {
            // Not logged in — go back to login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        new Thread(() -> {
            try {
                String queryUrl = SupabaseClient.SUPABASE_URL
                        + "/rest/v1/users"
                        + "?select=full_name,email,apartment_number,block,"
                        + "phone,member_since,lease_expiry,rent_amount,"
                        + "posts_count,requests_count,tenure"
                        + "&limit=1";

                HttpURLConnection conn = (HttpURLConnection)
                        new URL(queryUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "application/json");

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                conn.disconnect();

                JSONArray arr = new JSONArray(sb.toString());
                if (arr.length() == 0) return;

                JSONObject u = arr.getJSONObject(0);

                String fullName    = u.optString("full_name",        "Resident");
                String email       = u.optString("email",            "—");
                String aptNumber   = u.optString("apartment_number", "—");
                String block       = u.optString("block",            "");
                String phone       = u.optString("phone",            "—");
                String since       = u.optString("member_since",     "—");
                String lease       = u.optString("lease_expiry",     "—");
                String rent        = u.optString("rent_amount",      "—");
                String posts       = String.valueOf(u.optInt("posts_count",     0));
                String requests    = String.valueOf(u.optInt("requests_count",  0));
                String tenure      = u.optString("tenure",           "—");

                // Build initials from full name
                String initials = buildInitials(fullName);
                String unitLabel = "Unit " + aptNumber
                        + (block.isEmpty() ? "" : " · Block " + block);
                String sinceLabel = "Member since " + since;
                String rentLabel  = rent + " · Next Due";

                runOnUiThread(() -> {
                    avatarInitialsTop.setText(initials);
                    avatarInitials.setText(initials);
                    residentName.setText(fullName);
                    residentUnit.setText(unitLabel);
                    memberSince.setText(sinceLabel);

                    if (statPosts     != null) statPosts.setText(posts);
                    if (statRequests  != null) statRequests.setText(requests);
                    if (statTenure    != null) statTenure.setText(tenure);

                    phoneValue.setText(phone);
                    emailValue.setText(email);
                    leaseExpiryValue.setText(lease);
                    rentDueValue.setText(rentLabel);
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Failed to load profile: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ── Build initials from name e.g. "Alex Lawson" → "AL" ─────────────────
    private String buildInitials(String fullName) {
        if (fullName == null || fullName.isEmpty()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1))
                .toUpperCase();
    }

    // ── Logout ──────────────────────────────────────────────────────────────
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
            } catch (Exception ignored) {}

            runOnUiThread(this::clearAndGoToLogin);
        }).start();
    }

    private void clearAndGoToLogin() {
        getSharedPreferences("LoginPrefs", MODE_PRIVATE).edit().clear().apply();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ── Bottom Navigation ────────────────────────────────────────────────────
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