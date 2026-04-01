package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class FeedActivity extends AppCompatActivity {

    private LinearLayout feedContainer;
    private TextView greetingText, userNameText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Insets
        CoordinatorLayout root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, 0);
            return insets;
        });

        RelativeLayout bottomNav = findViewById(R.id.bottom_nav_container);
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            CoordinatorLayout.LayoutParams lp =
                    (CoordinatorLayout.LayoutParams) v.getLayoutParams();
            lp.bottomMargin = bars.bottom;
            v.setLayoutParams(lp);
            return insets;
        });

        setupBottomNavigation();
        setupPostButton();
        setupQuickActions();
        loadFeedFromSupabase();
        loadUserGreeting();
    }

    // ── Load greeting from saved prefs / Supabase ───────────────────────────
    private void loadUserGreeting() {
        // Set time-based greeting
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12)      greeting = "Good Morning 👋";
        else if (hour < 17) greeting = "Good Afternoon 👋";
        else                greeting = "Good Evening 👋";

        // Try to find greeting views if they exist in XML
        TextView greetView = findViewById(R.id.greetingText);
        TextView nameView  = findViewById(R.id.userNameText);

        if (greetView != null) greetView.setText(greeting);

        // Load name from Supabase
        if (nameView != null) {
            String token = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                    .getString("access_token", null);
            if (token == null) return;

            new Thread(() -> {
                try {
                    String queryUrl = SupabaseClient.SUPABASE_URL
                            + "/rest/v1/users?select=full_name&limit=1";
                    HttpURLConnection conn = (HttpURLConnection)
                            new URL(queryUrl).openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                    conn.setRequestProperty("Authorization", "Bearer " + token);

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();
                    conn.disconnect();

                    JSONArray arr = new JSONArray(sb.toString());
                    if (arr.length() > 0) {
                        String name = arr.getJSONObject(0).optString("full_name", "Resident");
                        runOnUiThread(() -> nameView.setText(name));
                    }
                } catch (Exception ignored) {}
            }).start();
        }
    }

    // ── Fetch feed posts from Supabase ──────────────────────────────────────
    private void loadFeedFromSupabase() {
        // Find the container where static feed cards are — we'll add dynamic cards after
        feedContainer = findViewById(R.id.feedDynamicContainer);
        if (feedContainer == null) return;

        String token = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                .getString("access_token", null);
        if (token == null) return;

        new Thread(() -> {
            try {
                String queryUrl = SupabaseClient.SUPABASE_URL
                        + "/rest/v1/feed_posts"
                        + "?select=*"
                        + "&order=created_at.desc"
                        + "&limit=20";

                HttpURLConnection conn = (HttpURLConnection)
                        new URL(queryUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "application/json");

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    conn.disconnect();
                    return;
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                conn.disconnect();

                JSONArray posts = new JSONArray(sb.toString());

                runOnUiThread(() -> {
                    feedContainer.removeAllViews();
                    for (int i = 0; i < posts.length(); i++) {
                        try {
                            JSONObject post = posts.getJSONObject(i);
                            buildFeedCard(post);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Failed to load feed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ── Dynamically build a feed card ───────────────────────────────────────
    private void buildFeedCard(JSONObject post) throws Exception {
        String authorName     = post.optString("author_name",    "Unknown");
        String authorInitials = post.optString("author_initials","?");
        String authorUnit     = post.optString("author_unit",    "");
        String postType       = post.optString("post_type",      "notice");
        String badgeLabel     = post.optString("badge_label",    "");
        String title          = post.optString("title",          "");
        String body           = post.optString("body",           "");
        String statusLabel    = post.optString("status_label",   "");
        String requestId      = post.optString("request_id",     "");
        int    likes          = post.optInt("likes_count",        0);
        int    comments       = post.optInt("comments_count",     0);
        String createdAt      = post.optString("created_at",     "");

        int dp4  = dp(4);
        int dp8  = dp(8);
        int dp10 = dp(10);
        int dp12 = dp(12);
        int dp14 = dp(14);
        int dp16 = dp(16);
        int dp20 = dp(20);

        // ── Outer card ──
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(dp16, 0, dp16, dp12);
        card.setLayoutParams(cardParams);
        card.setRadius(dp(20));
        card.setCardElevation(0);
        card.setCardBackgroundColor(Color.WHITE);

        // ── Card inner vertical layout ──
        LinearLayout inner = new LinearLayout(this);
        inner.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp16, dp16, dp16, dp16);

        // ── Header row: avatar + name + badge ──
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headerParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        headerParams.setMargins(0, 0, 0, dp10);
        headerRow.setLayoutParams(headerParams);

        // Avatar card
        CardView avatarCard = new CardView(this);
        LinearLayout.LayoutParams avatarParams =
                new LinearLayout.LayoutParams(dp(40), dp(40));
        avatarCard.setLayoutParams(avatarParams);
        avatarCard.setRadius(dp(20));
        avatarCard.setCardElevation(0);
        avatarCard.setCardBackgroundColor(avatarColorForType(postType));

        TextView avatarTv = new TextView(this);
        avatarTv.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));
        avatarTv.setText(authorInitials);
        avatarTv.setTextColor(Color.WHITE);
        avatarTv.setTextSize(13);
        avatarTv.setTypeface(null, android.graphics.Typeface.BOLD);
        avatarCard.addView(avatarTv);
        headerRow.addView(avatarCard);

        // Name + unit column
        LinearLayout nameCol = new LinearLayout(this);
        LinearLayout.LayoutParams nameColParams =
                new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        nameColParams.setMarginStart(dp12);
        nameCol.setLayoutParams(nameColParams);
        nameCol.setOrientation(LinearLayout.VERTICAL);

        TextView nameTv = new TextView(this);
        nameTv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        nameTv.setText(authorName + (authorUnit.isEmpty() ? "" : " · " + authorUnit));
        nameTv.setTextColor(Color.parseColor("#1E293B"));
        nameTv.setTextSize(13);
        nameTv.setTypeface(null, android.graphics.Typeface.BOLD);
        nameCol.addView(nameTv);

        TextView timeTv = new TextView(this);
        timeTv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        timeTv.setText(formatRelativeTime(createdAt));
        timeTv.setTextColor(Color.parseColor("#94A3B8"));
        timeTv.setTextSize(11);
        nameCol.addView(timeTv);
        headerRow.addView(nameCol);

        // Badge
        if (!badgeLabel.isEmpty()) {
            CardView badgeCard = new CardView(this);
            LinearLayout.LayoutParams badgeParams =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, dp(22));
            badgeCard.setLayoutParams(badgeParams);
            badgeCard.setRadius(dp(11));
            badgeCard.setCardElevation(0);
            badgeCard.setCardBackgroundColor(badgeBgColorForType(postType));

            TextView badgeTv = new TextView(this);
            FrameLayout.LayoutParams badgeTvParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER);
            badgeTv.setLayoutParams(badgeTvParams);
            badgeTv.setPadding(dp10, 0, dp10, 0);
            badgeTv.setText(badgeLabel);
            badgeTv.setTextColor(badgeTextColorForType(postType));
            badgeTv.setTextSize(10);
            badgeTv.setTypeface(null, android.graphics.Typeface.BOLD);
            badgeCard.addView(badgeTv);
            headerRow.addView(badgeCard);
        }

        inner.addView(headerRow);

        // ── Inline status card (for maintenance/complaint/reservation) ──
        boolean hasStatus = !statusLabel.isEmpty() && !requestId.isEmpty();
        if (hasStatus) {
            CardView statusCard = new CardView(this);
            LinearLayout.LayoutParams scParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            scParams.setMargins(0, 0, 0, dp12);
            statusCard.setLayoutParams(scParams);
            statusCard.setRadius(dp14);
            statusCard.setCardElevation(0);
            statusCard.setCardBackgroundColor(Color.parseColor("#F8FAFC"));

            LinearLayout scInner = new LinearLayout(this);
            scInner.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            scInner.setOrientation(LinearLayout.HORIZONTAL);
            scInner.setGravity(Gravity.CENTER_VERTICAL);
            scInner.setPadding(dp12, dp12, dp12, dp12);

            LinearLayout scTextCol = new LinearLayout(this);
            LinearLayout.LayoutParams scTextParams =
                    new LinearLayout.LayoutParams(0,
                            ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            scTextCol.setLayoutParams(scTextParams);
            scTextCol.setOrientation(LinearLayout.VERTICAL);

            TextView scTitle = new TextView(this);
            scTitle.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            scTitle.setText(title);
            scTitle.setTextColor(Color.parseColor("#1E293B"));
            scTitle.setTextSize(13);
            scTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams scTitleParams =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            scTitleParams.setMargins(0, 0, 0, dp(2));
            scTitle.setLayoutParams(scTitleParams);
            scTextCol.addView(scTitle);

            TextView scReqId = new TextView(this);
            scReqId.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            scReqId.setText("Request #" + requestId);
            scReqId.setTextColor(Color.parseColor("#94A3B8"));
            scReqId.setTextSize(11);
            scTextCol.addView(scReqId);
            scInner.addView(scTextCol);

            // Status badge
            CardView statusBadge = new CardView(this);
            statusBadge.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(24)));
            statusBadge.setRadius(dp12);
            statusBadge.setCardElevation(0);
            statusBadge.setCardBackgroundColor(statusBgColor(statusLabel));

            TextView statusTv = new TextView(this);
            statusTv.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER));
            statusTv.setPadding(dp10, 0, dp10, 0);
            statusTv.setText(statusLabel);
            statusTv.setTextColor(statusTextColor(statusLabel));
            statusTv.setTextSize(10);
            statusTv.setTypeface(null, android.graphics.Typeface.BOLD);
            statusBadge.addView(statusTv);
            scInner.addView(statusBadge);

            statusCard.addView(scInner);
            inner.addView(statusCard);

        } else {
            // ── Title + body for announcement/event/update ──
            TextView titleTv = new TextView(this);
            LinearLayout.LayoutParams titleParams =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            titleParams.setMargins(0, 0, 0, dp4);
            titleTv.setLayoutParams(titleParams);
            titleTv.setText(title);
            titleTv.setTextColor(Color.parseColor("#1E293B"));
            titleTv.setTextSize(15);
            titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
            inner.addView(titleTv);

            TextView bodyTv = new TextView(this);
            LinearLayout.LayoutParams bodyParams =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            bodyParams.setMargins(0, 0, 0, dp12);
            bodyTv.setLayoutParams(bodyParams);
            bodyTv.setText(body);
            bodyTv.setTextColor(Color.parseColor("#64748B"));
            bodyTv.setTextSize(13);
            bodyTv.setLineSpacing(dp(3), 1f);
            inner.addView(bodyTv);
        }

        // ── Divider ──
        android.view.View divider = new android.view.View(this);
        LinearLayout.LayoutParams divParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        divParams.setMargins(0, 0, 0, dp10);
        divider.setLayoutParams(divParams);
        divider.setBackgroundColor(Color.parseColor("#F1F5F9"));
        inner.addView(divider);

        // ── Footer row: likes + comments + action ──
        LinearLayout footer = new LinearLayout(this);
        footer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);

        // Likes
        TextView likesTv = new TextView(this);
        LinearLayout.LayoutParams likesParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        likesParams.setMargins(0, 0, dp14, 0);
        likesTv.setLayoutParams(likesParams);
        likesTv.setText("👍 " + likes);
        likesTv.setTextColor(Color.parseColor("#82A6CB"));
        likesTv.setTextSize(12);
        footer.addView(likesTv);

        // Comments
        TextView commentsTv = new TextView(this);
        LinearLayout.LayoutParams commentsParams =
                new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        commentsTv.setLayoutParams(commentsParams);
        commentsTv.setText("💬 " + comments + " comments");
        commentsTv.setTextColor(Color.parseColor("#82A6CB"));
        commentsTv.setTextSize(12);
        footer.addView(commentsTv);

        // Action link
        TextView actionTv = new TextView(this);
        actionTv.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        actionTv.setText(hasStatus ? "Track Request" : "Read more");
        actionTv.setTextColor(Color.parseColor("#3667A6"));
        actionTv.setTextSize(12);
        actionTv.setTypeface(null, android.graphics.Typeface.BOLD);
        footer.addView(actionTv);

        inner.addView(footer);
        card.addView(inner);
        feedContainer.addView(card);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int avatarColorForType(String type) {
        switch (type) {
            case "maintenance":  return Color.parseColor("#3667A6");
            case "complaint":    return Color.parseColor("#A78BFA");
            case "reservation":  return Color.parseColor("#7C3AED");
            case "event":        return Color.parseColor("#82A6CB");
            case "update":       return Color.parseColor("#3667A6");
            default:             return Color.parseColor("#214177");
        }
    }

    private int badgeBgColorForType(String type) {
        switch (type) {
            case "maintenance":  return Color.parseColor("#FEF3C7");
            case "complaint":    return Color.parseColor("#FFE4E6");
            case "reservation":  return Color.parseColor("#EDE9FE");
            case "event":        return Color.parseColor("#DCFCE7");
            case "update":       return Color.parseColor("#FEF3C7");
            default:             return Color.parseColor("#DBEAFE");
        }
    }

    private int badgeTextColorForType(String type) {
        switch (type) {
            case "maintenance":  return Color.parseColor("#D97706");
            case "complaint":    return Color.parseColor("#E11D48");
            case "reservation":  return Color.parseColor("#7C3AED");
            case "event":        return Color.parseColor("#16A34A");
            case "update":       return Color.parseColor("#D97706");
            default:             return Color.parseColor("#214177");
        }
    }

    private int statusBgColor(String status) {
        switch (status.toLowerCase()) {
            case "in progress":   return Color.parseColor("#FEF3C7");
            case "resolved":
            case "approved":      return Color.parseColor("#DCFCE7");
            case "under review":  return Color.parseColor("#DBEAFE");
            default:              return Color.parseColor("#F1F5F9");
        }
    }

    private int statusTextColor(String status) {
        switch (status.toLowerCase()) {
            case "in progress":   return Color.parseColor("#D97706");
            case "resolved":
            case "approved":      return Color.parseColor("#16A34A");
            case "under review":  return Color.parseColor("#3667A6");
            default:              return Color.parseColor("#64748B");
        }
    }

    private String formatRelativeTime(String isoTime) {
        if (isoTime == null || isoTime.isEmpty()) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date postDate = sdf.parse(isoTime.substring(0, 19));
            if (postDate == null) return "";
            long diff = (System.currentTimeMillis() - postDate.getTime()) / 1000;
            if (diff < 60)           return "Just now";
            if (diff < 3600)         return (diff / 60) + " min ago";
            if (diff < 86400)        return (diff / 3600) + " hours ago";
            if (diff < 172800)       return "Yesterday";
            return (diff / 86400) + " days ago";
        } catch (Exception e) {
            return "";
        }
    }

    // ── Navigation & actions ────────────────────────────────────────────────
    private void setupQuickActions() {
        CardView serviceComplaint = findViewById(R.id.serviceComplaint);
        if (serviceComplaint != null)
            serviceComplaint.setOnClickListener(v ->
                    startActivity(new Intent(this, ComplaintActivity.class)));

        CardView serviceMaintenance = findViewById(R.id.serviceMaintenance);
        if (serviceMaintenance != null)
            serviceMaintenance.setOnClickListener(v ->
                    startActivity(new Intent(this, MaintenanceActivity.class)));

        CardView serviceReservation = findViewById(R.id.serviceReservation);
        if (serviceReservation != null)
            serviceReservation.setOnClickListener(v ->
                    startActivity(new Intent(this, ReservationsActivity.class)));
    }

    private void setupPostButton() {
        CardView btnPost = findViewById(R.id.btnPost);
        if (btnPost != null)
            btnPost.setOnClickListener(v -> {
                startActivity(new Intent(this, PostActivity.class));
                overridePendingTransition(0, 0);
            });
    }

    private void setupBottomNavigation() {
        FrameLayout navFeed      = findViewById(R.id.nav_btn_feed);
        LinearLayout navNotices  = findViewById(R.id.nav_btn_notices);
        LinearLayout navChat     = findViewById(R.id.nav_btn_chat);
        LinearLayout navServices = findViewById(R.id.nav_btn_services);
        LinearLayout navProfile  = findViewById(R.id.nav_btn_profile);

        navFeed.setOnClickListener(v -> {});

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
        navProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
    }
}