package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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

    // Bottom nav
    private LinearLayout navNotices, navChat, navServices, navProfile;
    private FrameLayout navFeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide default ActionBar - use custom header
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_notices);

        // Handle Window Insets for System Navigation adaptation
        CoordinatorLayout root = findViewById(R.id.main);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                // Padding top for status bar, bottom is handled by the nav container logic
                v.setPadding(bars.left, bars.top, bars.right, 0);
                return insets;
            });
        }

        RelativeLayout bottomNav = findViewById(R.id.bottom_nav_container);
        if (bottomNav != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) v.getLayoutParams();
                // Set bottom margin to match system navigation bar height
                lp.bottomMargin = bars.bottom;
                v.setLayoutParams(lp);
                return insets;
            });
        }

        initViews();
        setupBottomNavigation();

        // Fetch data from Supabase
        loadNoticesFromSupabase();
    }

    private void initViews() {
        recyclerNotices = findViewById(R.id.recyclerNotices);
        tvNoticeCount = findViewById(R.id.tvNoticeCount);

        // Note: activity_notices header doesn't have a back button in current layout, 
        // but keeping this for safety or if layout changes.
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Setup RecyclerView
        noticeList = new ArrayList<>();
        noticeAdapter = new Noticeadapter(this, noticeList);
        recyclerNotices.setLayoutManager(new LinearLayoutManager(this));
        recyclerNotices.setAdapter(noticeAdapter);
    }

    private void loadNoticesFromSupabase() {
        String token = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                .getString("access_token", null);

        if (token == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                String queryUrl = SupabaseClient.SUPABASE_URL
                        + "/rest/v1/notices"
                        + "?select=*"
                        + "&order=created_at.desc";

                HttpURLConnection conn = (HttpURLConnection) new URL(queryUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "application/json");

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    runOnUiThread(() -> Toast.makeText(NoticesActivity.this, "Failed to fetch notices", Toast.LENGTH_SHORT).show());
                    conn.disconnect();
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                conn.disconnect();

                JSONArray jsonArray = new JSONArray(sb.toString());
                List<Notice> fetchedNotices = new ArrayList<>();

                for (int i = 0; i < jsonArray.length(); i++) {
                    try {
                        JSONObject obj = jsonArray.getJSONObject(i);

                        String id = obj.optString("id", String.valueOf(i));
                        String title = obj.optString("title", "No Title");
                        String body = obj.optString("body", "");
                        String sender = obj.optString("sender", "Management");
                        String createdAt = obj.optString("created_at", "");
                        String badge = obj.optString("badge_label", "🔔 Notice");
                        int likes = obj.optInt("likes_count", 0);
                        int comments = obj.optInt("comments_count", 0);

                        String timeAgo = formatRelativeTime(createdAt);

                        Notice notice = new Notice(id, title, body, sender, timeAgo, badge, likes, comments);
                        fetchedNotices.add(notice);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                runOnUiThread(() -> {
                    noticeList.clear();
                    noticeList.addAll(fetchedNotices);
                    noticeAdapter.notifyDataSetChanged();
                    updateNoticeCount();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    public void onNewNoticeReceived(String id, String title, String body, String timeAgo, String badge) {
        Notice newNotice = new Notice(id, title, body, "Management", timeAgo, badge, 0, 0);
        noticeAdapter.addNotice(newNotice);
        recyclerNotices.smoothScrollToPosition(0);
        updateNoticeCount();
    }

    private void updateNoticeCount() {
        int count = noticeList.size();
        tvNoticeCount.setText(count + " active notice" + (count != 1 ? "s" : ""));
    }

    private String formatRelativeTime(String isoTime) {
        if (isoTime == null || isoTime.isEmpty()) return "Just now";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date postDate = sdf.parse(isoTime.substring(0, 19));
            if (postDate == null) return "Just now";

            long diff = (System.currentTimeMillis() - postDate.getTime()) / 1000;
            if (diff < 60)           return "Just now";
            if (diff < 3600)         return (diff / 60) + " min ago";
            if (diff < 86400)        return (diff / 3600) + " hours ago";
            if (diff < 172800)       return "Yesterday";
            return (diff / 86400) + " days ago";
        } catch (Exception e) {
            return "Recently";
        }
    }

    private void setupBottomNavigation() {
        navFeed = findViewById(R.id.nav_btn_feed);
        navNotices = findViewById(R.id.nav_btn_notices);
        navChat = findViewById(R.id.nav_btn_chat);
        navServices = findViewById(R.id.nav_btn_services);
        navProfile = findViewById(R.id.nav_btn_profile);

        navNotices.setOnClickListener(v -> {});

        navFeed.setOnClickListener(v -> {
            startActivity(new Intent(this, FeedActivity.class));
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