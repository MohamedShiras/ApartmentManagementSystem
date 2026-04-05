package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
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

import com.bumptech.glide.Glide;

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

    // ── Session ──────────────────────────────────────────────────────────────
    private String token  = "";
    private String userId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // ── Load session once ─────────────────────────────────────────────────
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        token  = prefs.getString("access_token", "");
        userId = prefs.getString("user_id",       "");

        if (token.isEmpty() || userId.isEmpty()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // ── Insets ────────────────────────────────────────────────────────────
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

        // ── Scroll inset: content clears nav bar ──────────────────────────────
        NestedScrollView scrollView = findViewById(R.id.scrollView);
        ViewCompat.setOnApplyWindowInsetsListener(scrollView, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, bars.bottom + dp(90));
            return insets;
        });

        // ── POST button inset ─────────────────────────────────────────────────
        CardView btnPost = findViewById(R.id.btnPost);
        if (btnPost != null) {
            ViewCompat.setOnApplyWindowInsetsListener(btnPost, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                CoordinatorLayout.LayoutParams lp =
                        (CoordinatorLayout.LayoutParams) v.getLayoutParams();
                lp.bottomMargin = bars.bottom + dp(88);
                v.setLayoutParams(lp);
                return insets;
            });
        }

        feedContainer = findViewById(R.id.feedDynamicContainer);

        setupBottomNavigation();
        setupPostButton();
        setupQuickActions();
        loadUserGreeting();
        loadFeedFromSupabase();
        loadHeroCardData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFeedFromSupabase();
        loadHeroCardData();
    }

    // =========================================================================
    // USER GREETING — uses access_token (not anon key) to pass RLS
    // =========================================================================
    private void loadUserGreeting() {
        int hour = java.util.Calendar.getInstance()
                .get(java.util.Calendar.HOUR_OF_DAY);
        String greeting = hour < 12 ? "Good Morning 👋"
                : hour < 17 ? "Good Afternoon 👋"
                : "Good Evening 👋";

        TextView greetView = findViewById(R.id.greetingText);
        TextView nameView  = findViewById(R.id.userNameText);

        if (greetView != null) greetView.setText(greeting);
        if (nameView  == null) return;

    // =========================================================================
    // HERO CARD — rent due / paid status
    // =========================================================================
    private void loadHeroCardData() {
        new Thread(() -> {
            try {
                // ✅ Use access_token as Bearer — this passes RLS
                String queryUrl = SupabaseClient.SUPABASE_URL
                        + "/rest/v1/users"
                        + "?id=eq." + userId
                        + "&select=full_name"
                        + "&limit=1";

                HttpURLConnection conn = (HttpURLConnection)
                        new URL(queryUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + token); // ← access_token

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
            } catch (Exception e) {
                Log.e("GREETING", "Failed: " + e.getMessage());
            }
        }).start();
    }

    // =========================================================================
    // HERO CARD — rent due / paid status
    // =========================================================================
    private void loadHeroCardData() {
        new Thread(() -> {
            try {
                // ── Load user info ────────────────────────────────────────────
                String userUrl = SupabaseClient.SUPABASE_URL
                        + "/rest/v1/users?id=eq." + userId
                        + "&select=apartment_number,block,rent_amount";

                HttpURLConnection uc = (HttpURLConnection)
                        new URL(userUrl).openConnection();
                uc.setRequestMethod("GET");
                uc.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                uc.setRequestProperty("Authorization", "Bearer " + token); // ← access_token

                BufferedReader ur = new BufferedReader(
                        new InputStreamReader(uc.getInputStream()));
                StringBuilder usb = new StringBuilder();
                String ul;
                while ((ul = ur.readLine()) != null) usb.append(ul);
                ur.close(); uc.disconnect();

                JSONArray userArr = new JSONArray(usb.toString());
                if (userArr.length() == 0) return;

                JSONObject user   = userArr.getJSONObject(0);
                String unit       = user.optString("apartment_number", "--");
                String block      = user.optString("block", "--");
                String rentAmount = user.optString("rent_amount", "N/A");

                // ── Load latest payment ───────────────────────────────────────
                String payUrl = SupabaseClient.SUPABASE_URL
                        + "/rest/v1/payments?user_id=eq." + userId
                        + "&order=payment_date.desc&limit=1";

                HttpURLConnection pc = (HttpURLConnection)
                        new URL(payUrl).openConnection();
                pc.setRequestMethod("GET");
                pc.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                pc.setRequestProperty("Authorization", "Bearer " + token); // ← access_token

                BufferedReader pr = new BufferedReader(
                        new InputStreamReader(pc.getInputStream()));
                StringBuilder psb = new StringBuilder();
                String pl;
                while ((pl = pr.readLine()) != null) psb.append(pl);
                pr.close(); pc.disconnect();

                JSONArray payArr = new JSONArray(psb.toString());

                // ── Calculate next due date (28th of current/next month) ──────
                java.util.Calendar today = java.util.Calendar.getInstance();
                java.util.Calendar due28  = java.util.Calendar.getInstance();
                due28.set(java.util.Calendar.DAY_OF_MONTH, 28);
                due28.set(java.util.Calendar.HOUR_OF_DAY, 0);
                due28.set(java.util.Calendar.MINUTE, 0);
                due28.set(java.util.Calendar.SECOND, 0);
                due28.set(java.util.Calendar.MILLISECOND, 0);
                if (today.get(java.util.Calendar.DAY_OF_MONTH) > 28) {
                    due28.add(java.util.Calendar.MONTH, 1);
                }

                long daysUntilDue = (due28.getTimeInMillis()
                        - System.currentTimeMillis()) / (1000L * 60 * 60 * 24);
                String dueDateFmt = new SimpleDateFormat("dd MMM", Locale.getDefault())
                        .format(due28.getTime());

                // ── Check if paid this month ──────────────────────────────────
                boolean paidThisMonth = false;
                if (payArr.length() > 0) {
                    String payDateStr = payArr.getJSONObject(0)
                            .optString("payment_date", "");
                    Date payDate = parseSupabaseDate(payDateStr);
                    if (payDate != null) {
                        java.util.Calendar payCal = java.util.Calendar.getInstance();
                        payCal.setTime(payDate);
                        paidThisMonth =
                                payCal.get(java.util.Calendar.MONTH)
                                        == due28.get(java.util.Calendar.MONTH)
                                        && payCal.get(java.util.Calendar.YEAR)
                                        == due28.get(java.util.Calendar.YEAR);
                    }
                }

                final boolean paid     = paidThisMonth;
                final long    daysLeft = daysUntilDue;
                final String  dueFmt   = dueDateFmt;
                final String  unitF    = unit;
                final String  blockF   = block;
                final String  rentF    = rentAmount;

                runOnUiThread(() -> {
                    TextView unitBlockTv = findViewById(R.id.heroUnitBlock);
                    TextView titleTv     = findViewById(R.id.heroTitle);
                    TextView subTv       = findViewById(R.id.heroSub);
                    CardView payBtn      = findViewById(R.id.heroBtn);

                    if (unitBlockTv != null)
                        unitBlockTv.setText("Unit " + unitF + " · Block " + blockF
                                + (paid ? " · PAID ✓" : ""));

                    if (paid) {
                        if (titleTv != null) titleTv.setText("Next Rent Due");
                        if (subTv   != null) subTv.setText(rentF
                                + " due on " + dueFmt
                                + "  (" + daysLeft + " days left)");
                        if (payBtn  != null) {
                            payBtn.setAlpha(0.5f);
                            payBtn.setEnabled(false);
                        }
                    } else {
                        if (titleTv != null)
                            titleTv.setText("Rent Due in " + daysLeft + " days");
                        if (subTv   != null)
                            subTv.setText(rentF + " due on " + dueFmt);
                        if (payBtn  != null) {
                            payBtn.setAlpha(1f);
                            payBtn.setEnabled(true);
                            payBtn.setOnClickListener(v ->
                                    startActivity(new Intent(this, PaymentActivity.class)));
                        }
                    }
                });

            } catch (Exception e) {
                Log.e("HERO_CARD", "Error: " + e.getMessage(), e);
            }
        }).start();
    }

    // =========================================================================
    // FEED POSTS — uses access_token
    // =========================================================================
    private void loadFeedFromSupabase() {
        if (feedContainer == null) return;

        new Thread(() -> {
            try {
                String queryUrl = SupabaseClient.SUPABASE_URL
                        + "/rest/v1/posts"
                        + "?select=*"
                        + "&order=created_at.desc"
                        + "&limit=30";

                HttpURLConnection conn = (HttpURLConnection)
                        new URL(queryUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + token); // ← access_token

                int code = conn.getResponseCode();
                if (code != 200) { conn.disconnect(); return; }

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
                    if (posts.length() == 0) { showEmptyState(); return; }
                    for (int i = 0; i < posts.length(); i++) {
                        try { buildFeedCard(posts.getJSONObject(i)); }
                        catch (Exception e) { e.printStackTrace(); }
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Failed to load feed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // =========================================================================
    // BUILD FEED CARD
    // =========================================================================
    private void buildFeedCard(JSONObject post) throws Exception {
        String userName  = post.optString("user_name",  "Resident");
        String userUnit  = post.optString("user_unit",  "");
        String caption   = post.optString("caption",    "");
        String imageUrl  = post.optString("image_url",  "");
        String createdAt = post.optString("created_at", "");
        String initials  = initialsFrom(userName);

        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(dp(16), 0, dp(16), dp(12));
        card.setLayoutParams(cardLp);
        card.setRadius(dp(20));
        card.setCardElevation(0);
        card.setCardBackgroundColor(Color.WHITE);

        LinearLayout inner = new LinearLayout(this);
        inner.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(16), dp(16), dp(16), dp(16));

        // ── Header ──
        LinearLayout header = new LinearLayout(this);
        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerLp.setMargins(0, 0, 0, dp(12));
        header.setLayoutParams(headerLp);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        CardView avatarCard = new CardView(this);
        avatarCard.setLayoutParams(new LinearLayout.LayoutParams(dp(42), dp(42)));
        avatarCard.setRadius(dp(21));
        avatarCard.setCardElevation(0);
        avatarCard.setCardBackgroundColor(Color.parseColor("#214177"));

        TextView avatarTv = new TextView(this);
        avatarTv.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        avatarTv.setText(initials);
        avatarTv.setTextColor(Color.WHITE);
        avatarTv.setTextSize(14);
        avatarTv.setTypeface(null, Typeface.BOLD);
        avatarCard.addView(avatarTv);
        header.addView(avatarCard);

        LinearLayout nameCol = new LinearLayout(this);
        LinearLayout.LayoutParams ncLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        ncLp.setMarginStart(dp(12));
        nameCol.setLayoutParams(ncLp);
        nameCol.setOrientation(LinearLayout.VERTICAL);

        TextView nameTv = new TextView(this);
        nameTv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        nameTv.setText(userName + (userUnit.isEmpty() ? "" : "  ·  " + userUnit));
        nameTv.setTextColor(Color.parseColor("#1E293B"));
        nameTv.setTextSize(13);
        nameTv.setTypeface(null, Typeface.BOLD);
        nameCol.addView(nameTv);

        TextView timeTv = new TextView(this);
        timeTv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        timeTv.setText(formatRelativeTime(createdAt));
        timeTv.setTextColor(Color.parseColor("#94A3B8"));
        timeTv.setTextSize(11);
        nameCol.addView(timeTv);

        header.addView(nameCol);
        inner.addView(header);

        // ── Caption ──
        if (!caption.isEmpty()) {
            TextView capTv = new TextView(this);
            LinearLayout.LayoutParams capLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            capLp.setMargins(0, 0, 0, imageUrl.isEmpty() ? dp(12) : dp(10));
            capTv.setLayoutParams(capLp);
            capTv.setText(caption);
            capTv.setTextColor(Color.parseColor("#334155"));
            capTv.setTextSize(14);
            capTv.setLineSpacing(dp(3), 1f);
            inner.addView(capTv);
        }

        // ── Image ──
        if (!imageUrl.isEmpty()) {
            CardView imgCard = new CardView(this);
            LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            imgLp.setMargins(0, 0, 0, dp(12));
            imgCard.setLayoutParams(imgLp);
            imgCard.setRadius(dp(14));
            imgCard.setCardElevation(0);

            ImageView imgView = new ImageView(this);
            imgView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(220)));
            imgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(new android.graphics.drawable.ColorDrawable(
                            Color.parseColor("#F1F5F9")))
                    .into(imgView);
            imgCard.addView(imgView);
            inner.addView(imgCard);
        }

        // ── Divider ──
        android.view.View divider = new android.view.View(this);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        divLp.setMargins(0, 0, 0, dp(10));
        divider.setLayoutParams(divLp);
        divider.setBackgroundColor(Color.parseColor("#F1F5F9"));
        inner.addView(divider);

        // ── Footer ──
        LinearLayout footer = new LinearLayout(this);
        footer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);

        TextView likeTv = new TextView(this);
        LinearLayout.LayoutParams likeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        likeLp.setMargins(0, 0, dp(16), 0);
        likeTv.setLayoutParams(likeLp);
        likeTv.setText("👍  Like");
        likeTv.setTextColor(Color.parseColor("#82A6CB"));
        likeTv.setTextSize(12);
        footer.addView(likeTv);

        TextView commentTv = new TextView(this);
        commentTv.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        commentTv.setText("💬  Comment");
        commentTv.setTextColor(Color.parseColor("#82A6CB"));
        commentTv.setTextSize(12);
        footer.addView(commentTv);

        inner.addView(footer);
        card.addView(inner);
        feedContainer.addView(card);
    }

    // ── Empty state ───────────────────────────────────────────────────────────
    private void showEmptyState() {
        TextView tv = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(32), dp(48), dp(32), dp(16));
        tv.setLayoutParams(lp);
        tv.setText("No posts yet.\nBe the first to post something! 👋");
        tv.setTextColor(Color.parseColor("#94A3B8"));
        tv.setTextSize(14);
        tv.setGravity(Gravity.CENTER);
        tv.setLineSpacing(dp(4), 1f);
        feedContainer.addView(tv);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================
    private Date parseSupabaseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            String clean = dateStr;
            int plusIdx = clean.indexOf("+");
            if (plusIdx > 10) clean = clean.substring(0, plusIdx);
            if (clean.contains(".")) {
                int dot = clean.indexOf(".");
                String ms = clean.substring(dot + 1);
                if (ms.length() > 3) ms = ms.substring(0, 3);
                clean = clean.substring(0, dot) + "." + ms;
            }
            SimpleDateFormat sdf = clean.contains(".")
                    ? new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
                    : new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.parse(clean);
        } catch (Exception e) { return null; }
    }

    private String initialsFrom(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        if (parts.length > 0 && !parts[0].isEmpty())
            sb.append(Character.toUpperCase(parts[0].charAt(0)));
        if (parts.length > 1 && !parts[1].isEmpty())
            sb.append(Character.toUpperCase(parts[1].charAt(0)));
        return sb.length() > 0 ? sb.toString() : "?";
    }

    private String formatRelativeTime(String isoTime) {
        if (isoTime == null || isoTime.isEmpty()) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d = sdf.parse(isoTime.length() >= 19
                    ? isoTime.substring(0, 19) : isoTime);
            if (d == null) return "";
            long diff = (System.currentTimeMillis() - d.getTime()) / 1000;
            if (diff < 60)     return "Just now";
            if (diff < 3600)   return (diff / 60) + " min ago";
            if (diff < 86400)  return (diff / 3600) + " hr ago";
            if (diff < 172800) return "Yesterday";
            return (diff / 86400) + " days ago";
        } catch (Exception e) { return ""; }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    // =========================================================================
    // NAVIGATION
    // =========================================================================
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

        if (navFeed     != null) navFeed.setOnClickListener(v -> {});
        if (navNotices  != null) navNotices.setOnClickListener(v -> {
            startActivity(new Intent(this, NoticesActivity.class));
            overridePendingTransition(0, 0); finish();
        });
        if (navChat     != null) navChat.setOnClickListener(v -> {
            startActivity(new Intent(this, ChatActivity.class));
            overridePendingTransition(0, 0); finish();
        });
        if (navServices != null) navServices.setOnClickListener(v -> {
            startActivity(new Intent(this, ServicesActivity.class));
            overridePendingTransition(0, 0); finish();
        });
        if (navProfile  != null) navProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            overridePendingTransition(0, 0); finish();
        });
    }
}