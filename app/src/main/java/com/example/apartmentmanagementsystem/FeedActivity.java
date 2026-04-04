package com.example.apartmentmanagementsystem;

import android.content.Intent;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        CoordinatorLayout root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, 0);
            return insets;
        });

        RelativeLayout bottomNav = findViewById(R.id.bottom_nav_container);
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) v.getLayoutParams();
            lp.bottomMargin = bars.bottom;
            v.setLayoutParams(lp);
            return insets;
        });

        feedContainer = findViewById(R.id.feedDynamicContainer);

        setupBottomNavigation();
        setupPostButton();
        setupQuickActions();  // This now checks payment status
        loadUserGreeting();
        loadFeedFromSupabase(); // Kept from main
        loadHeroCardData(); // Added from feature branch
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh feed and payment status when returning from other activities
        loadFeedFromSupabase();
        loadHeroCardData();
        CardView payNowBtn = findViewById(R.id.heroBtn);
        if (payNowBtn != null) {
            checkAndBlockPaymentButton(payNowBtn);
        }
    }

    private void loadHeroCardData() {
        String userId = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                .getString("user_id", null);
        if (userId == null) return;

        new Thread(() -> {
            try {
                // ── Step 1: Load user info ─────────────────────────────────────
                String userUrl = SupabaseClient.SUPABASE_URL
                        + "/rest/v1/users?id=eq." + userId
                        + "&select=full_name,apartment_number,block,rent_amount";

                HttpURLConnection uc = (HttpURLConnection) new URL(userUrl).openConnection();
                uc.setRequestMethod("GET");
                uc.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                uc.setRequestProperty("Authorization", "Bearer " + SupabaseClient.SUPABASE_ANON_KEY);

                BufferedReader ur = new BufferedReader(new InputStreamReader(uc.getInputStream()));
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
                String rentDisplay = rentAmount;

                // ── Step 2: Load latest payment ────────────────────────────────
                String payUrl = SupabaseClient.SUPABASE_URL
                        + "/rest/v1/payments?user_id=eq." + userId
                        + "&order=payment_date.desc&limit=1";

                HttpURLConnection pc = (HttpURLConnection) new URL(payUrl).openConnection();
                pc.setRequestMethod("GET");
                pc.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                pc.setRequestProperty("Authorization", "Bearer " + SupabaseClient.SUPABASE_ANON_KEY);

                BufferedReader pr = new BufferedReader(new InputStreamReader(pc.getInputStream()));
                StringBuilder psb = new StringBuilder();
                String pl;
                while ((pl = pr.readLine()) != null) psb.append(pl);
                pr.close(); pc.disconnect();

                JSONArray payArr = new JSONArray(psb.toString());

                // ── Step 3: Calculate next due date (28th of current/next month) ─
                java.util.Calendar today = java.util.Calendar.getInstance();
                java.util.Calendar due28  = java.util.Calendar.getInstance();
                due28.set(java.util.Calendar.DAY_OF_MONTH, 28);
                due28.set(java.util.Calendar.HOUR_OF_DAY, 0);
                due28.set(java.util.Calendar.MINUTE, 0);
                due28.set(java.util.Calendar.SECOND, 0);
                due28.set(java.util.Calendar.MILLISECOND, 0);

                // If today is past the 28th, move to next month's 28th
                if (today.get(java.util.Calendar.DAY_OF_MONTH) > 28) {
                    due28.add(java.util.Calendar.MONTH, 1);
                }

                long daysUntilDue = (due28.getTimeInMillis() - System.currentTimeMillis())
                        / (1000L * 60 * 60 * 24);
                String dueDateFmt = new SimpleDateFormat("dd MMM", Locale.getDefault())
                        .format(due28.getTime());

                // ── Step 4: Check if this month's 28th is already paid ─────────
                boolean paidThisMonth = false;

                if (payArr.length() > 0) {
                    String payDateStr = payArr.getJSONObject(0).optString("payment_date", "");
                    Date payDate      = parseSupabaseDate(payDateStr);

                    if (payDate != null) {
                        java.util.Calendar payCal = java.util.Calendar.getInstance();
                        payCal.setTime(payDate);

                        // Paid in same month & year as due28 → paid this month
                        paidThisMonth =
                                payCal.get(java.util.Calendar.MONTH) == due28.get(java.util.Calendar.MONTH)
                                        && payCal.get(java.util.Calendar.YEAR) == due28.get(java.util.Calendar.YEAR);
                    }
                }

                final boolean paid        = paidThisMonth;
                final long    daysLeft    = daysUntilDue;
                final String  dueFmt      = dueDateFmt;
                final String  unitF       = unit;
                final String  blockF      = block;
                final String  rentF       = rentDisplay;

                runOnUiThread(() -> {
                    TextView unitBlockTv = findViewById(R.id.heroUnitBlock);
                    TextView titleTv     = findViewById(R.id.heroTitle);
                    TextView subTv       = findViewById(R.id.heroSub);

                    if (paid) {
                        // ✅ PAID — show next due
                        if (unitBlockTv != null)
                            unitBlockTv.setText("Unit " + unitF + " · Block " + blockF + " · PAID ✓");
                        if (titleTv != null)
                            titleTv.setText("Next Rent Due");
                        if (subTv != null)
                            subTv.setText(rentF + " due on " + dueFmt + "  (" + daysLeft + " days left)");

                    } else {
                        // ❌ NOT PAID — show countdown to 28th
                        if (unitBlockTv != null)
                            unitBlockTv.setText("Unit " + unitF + " · Block " + blockF);
                        if (titleTv != null)
                            titleTv.setText("Rent Due in " + daysLeft + " days");
                        if (subTv != null)
                            subTv.setText(rentF + " due on " + dueFmt);
                    }
                });

            } catch (Exception e) {
                Log.e("HERO_CARD", "Error: " + e.getMessage(), e);
            }
        }).start();
    }

    private void updateHeroUnpaid(String unit, String block, String rentDisplay, String subtitle) {
        TextView unitBlockTv = findViewById(R.id.heroUnitBlock);
        TextView titleTv     = findViewById(R.id.heroTitle);
        TextView subTv       = findViewById(R.id.heroSub);

        if (unitBlockTv != null) unitBlockTv.setText("Unit " + unit + " · Block " + block);
        if (titleTv != null)     titleTv.setText("Rent Due");
        if (subTv != null)       subTv.setText(rentDisplay + " · " + subtitle);
    }

    // ── User greeting — mirrors PostActivity.loadUserData() exactly ─────────
    private void loadUserGreeting() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String greeting = hour < 12 ? "Good Morning 👋" : hour < 17 ? "Good Afternoon 👋" : "Good Evening 👋";

        TextView greetView = findViewById(R.id.greetingText);
        TextView nameView  = findViewById(R.id.userNameText);

        if (greetView != null) greetView.setText(greeting);

        // Use same prefs key as PostActivity / LoginActivity
        String userId = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                .getString("user_id", null);
        if (userId == null || nameView == null) return;

        new Thread(() -> {
            try {
                String queryUrl = SupabaseClient.SUPABASE_URL
                        + "/rest/v1/users?id=eq." + userId
                        + "&select=full_name,apartment_number,block";

                HttpURLConnection conn = (HttpURLConnection) new URL(queryUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SupabaseClient.SUPABASE_ANON_KEY);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                conn.disconnect();

                JSONArray arr = new JSONArray(sb.toString());
                if (arr.length() > 0) {
                    JSONObject obj = arr.getJSONObject(0);
                    String name = obj.optString("full_name", "Resident");
                    runOnUiThread(() -> nameView.setText(name));
                }
            } catch (Exception ignored) {}
        }).start();
    }

    // ── Fetch posts from the same `posts` table PostRepository writes to ────
    private void loadFeedFromSupabase() {
        if (feedContainer == null) return;

        String userId = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                .getString("user_id", null);
        if (userId == null) return;

        new Thread(() -> {
            try {
                // Query the `posts` table — same table PostRepository inserts into
                String queryUrl = SupabaseClient.SUPABASE_URL
                        + "/rest/v1/posts"
                        + "?select=*"
                        + "&order=created_at.desc"
                        + "&limit=30";

                HttpURLConnection conn = (HttpURLConnection) new URL(queryUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Content-Type", "application/json");

                int code = conn.getResponseCode();
                if (code != 200) { conn.disconnect(); return; }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                conn.disconnect();

                JSONArray posts = new JSONArray(sb.toString());

                runOnUiThread(() -> {
                    feedContainer.removeAllViews();
                    if (posts.length() == 0) {
                        showEmptyState();
                        return;
                    }
                    for (int i = 0; i < posts.length(); i++) {
                        try {
                            buildFeedCard(posts.getJSONObject(i));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Failed to load feed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ── Empty state ──────────────────────────────────────────────────────────
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

    // ── Build one feed card from a `posts` row ───────────────────────────────
    private void buildFeedCard(JSONObject post) throws Exception {
        // Columns written by PostRepository.insertPost()
        String userName  = post.optString("user_name",  "Resident");
        String userUnit  = post.optString("user_unit",  "");
        String caption   = post.optString("caption",    "");
        String imageUrl  = post.optString("image_url",  "");
        String createdAt = post.optString("created_at", "");

        // Derive initials from user_name
        String initials = initialsFrom(userName);

        // ── Outer card ──────────────────────────────────────────────────────
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(dp(16), 0, dp(16), dp(12));
        card.setLayoutParams(cardLp);
        card.setRadius(dp(20));
        card.setCardElevation(0);
        card.setCardBackgroundColor(Color.WHITE);

        // ── Card inner layout ───────────────────────────────────────────────
        LinearLayout inner = new LinearLayout(this);
        inner.setLayoutParams(wrapWrap());
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(16), dp(16), dp(16), dp(16));

        // ── Header: avatar + name/unit + time ──────────────────────────────
        LinearLayout header = new LinearLayout(this);
        LinearLayout.LayoutParams headerLp = matchWrap();
        headerLp.setMargins(0, 0, 0, dp(12));
        header.setLayoutParams(headerLp);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        // Avatar
        CardView avatarCard = new CardView(this);
        avatarCard.setLayoutParams(new LinearLayout.LayoutParams(dp(42), dp(42)));
        avatarCard.setRadius(dp(21));
        avatarCard.setCardElevation(0);
        avatarCard.setCardBackgroundColor(Color.parseColor("#214177"));

        TextView avatarTv = new TextView(this);
        avatarTv.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        avatarTv.setText(initials);
        avatarTv.setTextColor(Color.WHITE);
        avatarTv.setTextSize(14);
        avatarTv.setTypeface(null, Typeface.BOLD);
        avatarCard.addView(avatarTv);
        header.addView(avatarCard);

        // Name + unit + time column
        LinearLayout nameCol = new LinearLayout(this);
        LinearLayout.LayoutParams nameColLp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        nameColLp.setMarginStart(dp(12));
        nameCol.setLayoutParams(nameColLp);
        nameCol.setOrientation(LinearLayout.VERTICAL);

        TextView nameTv = new TextView(this);
        nameTv.setLayoutParams(wrapWrap());
        nameTv.setText(userName + (userUnit.isEmpty() ? "" : "  ·  " + userUnit));
        nameTv.setTextColor(Color.parseColor("#1E293B"));
        nameTv.setTextSize(13);
        nameTv.setTypeface(null, Typeface.BOLD);
        nameCol.addView(nameTv);

        TextView timeTv = new TextView(this);
        timeTv.setLayoutParams(wrapWrap());
        timeTv.setText(formatRelativeTime(createdAt));
        timeTv.setTextColor(Color.parseColor("#94A3B8"));
        timeTv.setTextSize(11);
        nameCol.addView(timeTv);

        header.addView(nameCol);
        inner.addView(header);

        // ── Caption ─────────────────────────────────────────────────────────
        if (!caption.isEmpty()) {
            TextView capTv = new TextView(this);
            LinearLayout.LayoutParams capLp = matchWrap();
            capLp.setMargins(0, 0, 0, imageUrl.isEmpty() ? dp(12) : dp(10));
            capTv.setLayoutParams(capLp);
            capTv.setText(caption);
            capTv.setTextColor(Color.parseColor("#334155"));
            capTv.setTextSize(14);
            capTv.setLineSpacing(dp(3), 1f);
            inner.addView(capTv);
        }

        // ── Image preview (if any) ───────────────────────────────────────────
        if (!imageUrl.isEmpty()) {
            CardView imgCard = new CardView(this);
            LinearLayout.LayoutParams imgLp = matchWrap();
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
                    .placeholder(new android.graphics.drawable.ColorDrawable(Color.parseColor("#F1F5F9")))
                    .into(imgView);

            imgCard.addView(imgView);
            inner.addView(imgCard);
        }

        // ── Divider ──────────────────────────────────────────────────────────
        android.view.View divider = new android.view.View(this);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        divLp.setMargins(0, 0, 0, dp(10));
        divider.setLayoutParams(divLp);
        divider.setBackgroundColor(Color.parseColor("#F1F5F9"));
        inner.addView(divider);

        // ── Footer: like + comment placeholders ──────────────────────────────
        LinearLayout footer = new LinearLayout(this);
        footer.setLayoutParams(matchWrap());
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);

        TextView likeTv = new TextView(this);
        LinearLayout.LayoutParams likeLp = wrapWrap();
        likeLp.setMargins(0, 0, dp(16), 0);
        likeTv.setLayoutParams(likeLp);
        likeTv.setText("👍  Like");
        likeTv.setTextColor(Color.parseColor("#82A6CB"));
        likeTv.setTextSize(12);
        footer.addView(likeTv);

        TextView commentTv = new TextView(this);
        commentTv.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        commentTv.setText("💬  Comment");
        commentTv.setTextColor(Color.parseColor("#82A6CB"));
        commentTv.setTextSize(12);
        footer.addView(commentTv);

        inner.addView(footer);
        card.addView(inner);
        feedContainer.addView(card);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String initialsFrom(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        if (parts.length > 0 && !parts[0].isEmpty()) sb.append(Character.toUpperCase(parts[0].charAt(0)));
        if (parts.length > 1 && !parts[1].isEmpty()) sb.append(Character.toUpperCase(parts[1].charAt(0)));
        return sb.length() > 0 ? sb.toString() : "?";
    }

    private String formatRelativeTime(String isoTime) {
        if (isoTime == null || isoTime.isEmpty()) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d = sdf.parse(isoTime.length() >= 19 ? isoTime.substring(0, 19) : isoTime);
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

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams wrapWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    private void setupQuickActions() {
        CardView serviceComplaint = findViewById(R.id.serviceComplaint);
        if (serviceComplaint != null)
            serviceComplaint.setOnClickListener(v -> startActivity(new Intent(this, ComplaintActivity.class)));
        CardView serviceMaintenance = findViewById(R.id.serviceMaintenance);
        if (serviceMaintenance != null)
            serviceMaintenance.setOnClickListener(v -> startActivity(new Intent(this, MaintenanceActivity.class)));
        CardView serviceReservation = findViewById(R.id.serviceReservation);
        if (serviceReservation != null)
            serviceReservation.setOnClickListener(v -> startActivity(new Intent(this, ReservationsActivity.class)));

        // Check payment status and update Pay Now button
        CardView payNowBtn = findViewById(R.id.heroBtn);
        if (payNowBtn != null) {
            checkAndBlockPaymentButton(payNowBtn);
        }
    }

    // Check payment status from Supabase payments table
    private void checkAndBlockPaymentButton(CardView payNowBtn) {
        String userId = getSharedPreferences("LoginPrefs", MODE_PRIVATE).getString("user_id", "");

        Log.d("PAYMENT_CHECK", "Checking payment for userId: " + userId);

        if (userId.isEmpty()) {
            Log.w("PAYMENT_CHECK", "userId is empty, enabling button");
            enablePaymentButton(payNowBtn);
            return;
        }

        new Thread(() -> {
            try {
                // Query Supabase payments table
                String queryUrl = SupabaseClient.SUPABASE_URL + "/rest/v1/payments"
                        + "?user_id=eq." + userId
                        + "&order=payment_date.desc"
                        + "&limit=1";

                Log.d("PAYMENT_CHECK", "Query URL: " + queryUrl);

                URL url = new URL(queryUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SupabaseClient.SUPABASE_ANON_KEY);

                int responseCode = conn.getResponseCode();
                Log.d("PAYMENT_CHECK", "Response Code: " + responseCode);

                BufferedReader reader;
                if (responseCode == 200 || responseCode == 201) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    // Read error stream if response code is not 200/201
                    reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    Log.e("PAYMENT_CHECK", "Error Response: " + errorResponse.toString());
                    reader.close();
                    conn.disconnect();
                    runOnUiThread(() -> enablePaymentButton(payNowBtn));
                    return;
                }

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                conn.disconnect();

                String jsonResponse = response.toString();
                Log.d("PAYMENT_CHECK", "Response: " + jsonResponse);

                JSONArray result = new JSONArray(jsonResponse);

                if (result.length() > 0) {
                    JSONObject lastPayment = result.getJSONObject(0);
                    String createdAt = lastPayment.optString("payment_date", "");

                    Log.d("PAYMENT_CHECK", "Last payment date: " + createdAt);

                    if (isPaymentWithin28Days(createdAt)) {
                        Log.d("PAYMENT_CHECK", "Payment is within 28 days - BLOCKING button");
                        runOnUiThread(() -> blockPaymentButton(payNowBtn, createdAt));
                    } else {
                        Log.d("PAYMENT_CHECK", "Payment is older than 28 days - ENABLING button");
                        runOnUiThread(() -> enablePaymentButton(payNowBtn));
                    }
                } else {
                    Log.d("PAYMENT_CHECK", "No payment found - ENABLING button");
                    runOnUiThread(() -> enablePaymentButton(payNowBtn));
                }
            } catch (Exception e) {
                Log.e("PAYMENT_CHECK", "Error checking payment: " + e.getMessage(), e);
                runOnUiThread(() -> enablePaymentButton(payNowBtn));
            }
        }).start();
    }

    private Date parseSupabaseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            String clean = dateStr;

            // Remove timezone offset (+00:00 / +05:30)
            int plusIdx = clean.indexOf("+");
            if (plusIdx > 10) clean = clean.substring(0, plusIdx);
            int lastMinus = clean.lastIndexOf("-");
            if (lastMinus > 10) clean = clean.substring(0, lastMinus);

            // Trim microseconds to milliseconds
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
        } catch (Exception e) {
            Log.e("DATE_PARSE", "Failed: " + e.getMessage());
            return null;
        }
    }

    // Check if payment date is within 28 days
    private boolean isPaymentWithin28Days(String dateStr) {
        try {
            if (dateStr == null || dateStr.isEmpty()) return false;

            // Strip timezone offset (+00:00 or +05:30 etc.)
            String cleanDate = dateStr;
            if (cleanDate.contains("+")) {
                cleanDate = cleanDate.substring(0, cleanDate.indexOf("+"));
            } else if (cleanDate.lastIndexOf("-") > 10) {
                // handles negative offset like -05:00
                cleanDate = cleanDate.substring(0, cleanDate.lastIndexOf("-"));
            }

            // Trim microseconds to milliseconds (keep only 3 decimal digits)
            if (cleanDate.contains(".")) {
                int dotIndex = cleanDate.indexOf(".");
                String beforeDot = cleanDate.substring(0, dotIndex);
                String afterDot  = cleanDate.substring(dotIndex + 1);
                if (afterDot.length() > 3) afterDot = afterDot.substring(0, 3);
                cleanDate = beforeDot + "." + afterDot;
            }

            Log.d("DATE_CHECK", "Cleaned date string: " + cleanDate);

            SimpleDateFormat sdf;
            if (cleanDate.contains(".")) {
                sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
            } else {
                sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            }
            sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // ← important!

            Date lastPaymentDate = sdf.parse(cleanDate);
            if (lastPaymentDate == null) {
                Log.w("DATE_CHECK", "Could not parse date: " + cleanDate);
                return false;
            }

            long diffInMillis = System.currentTimeMillis() - lastPaymentDate.getTime();
            long diffInDays   = diffInMillis / (1000L * 60 * 60 * 24);

            Log.d("DATE_CHECK", "Days since payment: " + diffInDays);
            return diffInDays <= 28;

        } catch (Exception e) {
            Log.e("DATE_CHECK", "Error: " + e.getMessage(), e);
            return false;
        }
    }

    // Block the Pay Now button
    private void blockPaymentButton(CardView payNowBtn, String paymentDate) {
        Log.d("PAYMENT_UI", "Blocking payment button");

        payNowBtn.setEnabled(false);
        payNowBtn.setAlpha(0.5f);

        TextView btnText = payNowBtn.findViewById(R.id.heroBtnText);
        if (btnText != null) {
            btnText.setText("PAID");
            btnText.setTextColor(Color.parseColor("#999999"));
        }

        payNowBtn.setOnClickListener(v -> {
            int daysRemaining = getDaysRemaining(paymentDate);
            String nextDueDate = getNextDueDate(paymentDate);
            Toast.makeText(FeedActivity.this,
                    "✓ Payment already made!\nNext payment due: " + nextDueDate + " (" + daysRemaining + " days)",
                    Toast.LENGTH_LONG).show();
        });
    }

    // Enable the Pay Now button
    private void enablePaymentButton(CardView payNowBtn) {
        Log.d("PAYMENT_UI", "Enabling payment button");

        // Enable button
        payNowBtn.setEnabled(true);
        payNowBtn.setAlpha(1f);

        // Update text
        TextView btnText = payNowBtn.findViewById(R.id.heroBtnText);
        if (btnText != null) {
            btnText.setText("Pay Now");
            btnText.setTextColor(Color.parseColor("#214177"));
        }

        // Set click listener to open PaymentActivity
        payNowBtn.setOnClickListener(v -> {
            startActivity(new Intent(FeedActivity.this, PaymentActivity.class));
        });
    }

    // Get days remaining until next payment
    private int getDaysRemaining(String paymentDateStr) {
        try {
            SimpleDateFormat[] formats = {
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault()),
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
            };

            Date paymentDate = null;
            for (SimpleDateFormat format : formats) {
                try {
                    String cleanDate = paymentDateStr;
                    if (paymentDateStr.contains("+")) {
                        cleanDate = paymentDateStr.substring(0, paymentDateStr.indexOf("+"));
                    }
                    paymentDate = format.parse(cleanDate);
                    if (paymentDate != null) break;
                } catch (Exception e) {
                    continue;
                }
            }

            if (paymentDate == null) return 28;

            Date nextDueDate = new Date(paymentDate.getTime() + (28L * 24 * 60 * 60 * 1000));
            Date today = new Date();

            long diffInMillis = nextDueDate.getTime() - today.getTime();
            long daysRemaining = diffInMillis / (1000 * 60 * 60 * 24);

            return (int) daysRemaining;
        } catch (Exception e) {
            Log.e("DAYS_CALC", "Error: " + e.getMessage());
            return 28;
        }
    }

    // Get next due date
    private String getNextDueDate(String paymentDateStr) {
        try {
            SimpleDateFormat[] formats = {
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault()),
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
            };

            Date paymentDate = null;
            for (SimpleDateFormat format : formats) {
                try {
                    String cleanDate = paymentDateStr;
                    if (paymentDateStr.contains("+")) {
                        cleanDate = paymentDateStr.substring(0, paymentDateStr.indexOf("+"));
                    }
                    paymentDate = format.parse(cleanDate);
                    if (paymentDate != null) break;
                } catch (Exception e) {
                    continue;
                }
            }

            if (paymentDate == null) return "N/A";

            Date nextDueDate = new Date(paymentDate.getTime() + (28L * 24 * 60 * 60 * 1000));
            SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return displayFormat.format(nextDueDate);
        } catch (Exception e) {
            Log.e("DATE_FORMAT", "Error: " + e.getMessage());
            return "N/A";
        }
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