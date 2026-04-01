package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class NoticesActivity extends AppCompatActivity {

    private RecyclerView recyclerNotices;
    private Noticeadapter noticeAdapter;
    private List<Notice> noticeList;
    private TextView tvNoticeCount;

    private LinearLayout navNotices, navChat, navServices, navProfile;
    private FrameLayout navFeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ ANDROID 15 EDGE-TO-EDGE FIX
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_notices);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // ✅ STATUS BAR INSETS
        CoordinatorLayout root = findViewById(R.id.main);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(bars.left, bars.top, bars.right, 0);
                return insets;
            });
        }

        // ✅ BOTTOM NAVIGATION SAFE AREA
        RelativeLayout bottomNav = findViewById(R.id.bottom_nav_container);
        if (bottomNav != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {

                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

                CoordinatorLayout.LayoutParams lp =
                        (CoordinatorLayout.LayoutParams) v.getLayoutParams();

                lp.bottomMargin = bars.bottom;
                v.setLayoutParams(lp);

                return insets;
            });
        }

        initViews();
        setupBottomNavigation();
        loadNoticesFromSupabase();
    }

    // ───────────────── INIT ─────────────────
    private void initViews() {

        recyclerNotices = findViewById(R.id.recyclerNotices);
        tvNoticeCount = findViewById(R.id.tvNoticeCount);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        noticeList = new ArrayList<>();
        noticeAdapter = new Noticeadapter(this, noticeList);

        recyclerNotices.setLayoutManager(new LinearLayoutManager(this));
        recyclerNotices.setAdapter(noticeAdapter);
    }

    // ───────────────── LOAD FROM SUPABASE ─────────────────
    private void loadNoticesFromSupabase() {

        String token = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                .getString("access_token", null);

        if (token == null) {
            Toast.makeText(this,
                    "Session expired. Please log in again.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {

                String queryUrl =
                        SupabaseClient.SUPABASE_URL +
                                "/rest/v1/notices?select=*&order=created_at.desc";

                HttpURLConnection conn =
                        (HttpURLConnection) new URL(queryUrl).openConnection();

                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "application/json");

                if (conn.getResponseCode() != 200) {
                    runOnUiThread(() ->
                            Toast.makeText(this,
                                    "Failed to fetch notices",
                                    Toast.LENGTH_SHORT).show());
                    conn.disconnect();
                    return;
                }

                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(conn.getInputStream()));

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null)
                    sb.append(line);

                reader.close();
                conn.disconnect();

                JSONArray jsonArray = new JSONArray(sb.toString());

                List<Notice> fetched = new ArrayList<>();

                for (int i = 0; i < jsonArray.length(); i++) {

                    JSONObject obj = jsonArray.getJSONObject(i);

                    Notice notice = new Notice(
                            obj.optString("id", String.valueOf(i)),
                            obj.optString("title", "No Title"),
                            obj.optString("body", ""),
                            obj.optString("sender", "Management"),
                            formatRelativeTime(obj.optString("created_at")),
                            obj.optString("badge_label", "🔔 Notice"),
                            obj.optInt("likes_count", 0),
                            obj.optInt("comments_count", 0)
                    );

                    fetched.add(notice);
                }

                runOnUiThread(() -> {
                    noticeList.clear();
                    noticeList.addAll(fetched);
                    noticeAdapter.notifyDataSetChanged();
                    updateNoticeCount();
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Error loading notices",
                                Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ───────────────── NOTICE COUNT ─────────────────
    private void updateNoticeCount() {
        int count = noticeList.size();
        tvNoticeCount.setText(count + " active notice" + (count != 1 ? "s" : ""));
    }

    // ───────────────── TIME FORMAT ─────────────────
    private String formatRelativeTime(String isoTime) {

        if (isoTime == null || isoTime.isEmpty()) return "Just now";

        try {
            SimpleDateFormat sdf =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

            Date postDate = sdf.parse(isoTime.substring(0, 19));

            long diff = (System.currentTimeMillis() - postDate.getTime()) / 1000;

            if (diff < 60) return "Just now";
            if (diff < 3600) return (diff / 60) + " min ago";
            if (diff < 86400) return (diff / 3600) + " hours ago";
            if (diff < 172800) return "Yesterday";

            return (diff / 86400) + " days ago";

        } catch (Exception e) {
            return "Recently";
        }
    }

    // ───────────────── BOTTOM NAVIGATION ─────────────────
    private void setupBottomNavigation() {

        navFeed = findViewById(R.id.nav_btn_feed);
        navNotices = findViewById(R.id.nav_btn_notices);
        navChat = findViewById(R.id.nav_btn_chat);
        navServices = findViewById(R.id.nav_btn_services);
        navProfile = findViewById(R.id.nav_btn_profile);

        navNotices.setOnClickListener(v -> {});

        navFeed.setOnClickListener(v -> open(FeedActivity.class));
        navChat.setOnClickListener(v -> open(ChatActivity.class));
        navServices.setOnClickListener(v -> open(ServicesActivity.class));
        navProfile.setOnClickListener(v -> open(ProfileActivity.class));
    }

    private void open(Class<?> cls) {
        startActivity(new Intent(this, cls));
        overridePendingTransition(0, 0);
        finish();
    }
}