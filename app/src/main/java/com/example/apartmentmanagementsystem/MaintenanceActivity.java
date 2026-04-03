package com.example.apartmentmanagementsystem;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MaintenanceActivity extends AppCompatActivity {

    private static final String SVC_PLUMBING  = "Plumbing";
    private static final String SVC_ELECTRIC  = "Electrician";
    private static final String SVC_GAS       = "Gas";
    private static final String SVC_AC        = "Air Conditioning";
    private static final String SVC_CARPENTRY = "Carpentry";
    private static final String SVC_CLEANING  = "Cleaning";

    private LinearLayout requestsContainer;
    private ProgressBar  requestsLoading;
    private CardView     emptyRequestsCard;

    // ── Session fields (loaded once, reused everywhere) ──────────────────────
    private String token                 = "";
    private String userId                = "";
    private String currentApartmentNumber = "";

    // =========================================================================
    // LIFECYCLE
    // =========================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maintenance);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.activity_maintenance), (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });

        requestsContainer = findViewById(R.id.requestsDynamicContainer);
        requestsLoading   = findViewById(R.id.requestsLoading);
        emptyRequestsCard = findViewById(R.id.emptyRequestsCard);

        setupBackButton();
        setupServiceCards();
        loadSession();
    }

    // =========================================================================
    // SESSION — load token + userId from SharedPreferences
    // =========================================================================
    private void loadSession() {
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        token  = prefs.getString("access_token", null);

        if (token == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Extract userId from JWT payload
        userId = extractUserIdFromToken(token);

        // Load apartment number then fetch requests
        loadApartmentNumber();
    }

    // =========================================================================
    // LOAD APARTMENT NUMBER
    // =========================================================================
    private void loadApartmentNumber() {
        new Thread(() -> {
            try {
                String url = SupabaseClient.SUPABASE_URL
                        + "/rest/v1/users"
                        + "?id=eq." + userId
                        + "&select=apartment_number";

                HttpURLConnection conn = (HttpURLConnection)
                        new URL(url).openConnection();
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
                    currentApartmentNumber = arr.getJSONObject(0)
                            .optString("apartment_number", "");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            runOnUiThread(this::loadMyRequests);
        }).start();
    }

    // =========================================================================
    // LOAD MY REQUESTS (filtered by userId)
    // =========================================================================
    private void loadMyRequests() {
        requestsLoading.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                String queryUrl = SupabaseClient.SUPABASE_URL
                        + "/rest/v1/maintenance_requests"
                        + "?user_id=eq." + userId
                        + "&order=created_at.desc";

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

                JSONArray requests = new JSONArray(sb.toString());

                runOnUiThread(() -> {
                    requestsLoading.setVisibility(View.GONE);
                    requestsContainer.removeAllViews();

                    if (requests.length() == 0) {
                        emptyRequestsCard.setVisibility(View.VISIBLE);
                        return;
                    }

                    emptyRequestsCard.setVisibility(View.GONE);
                    for (int i = 0; i < requests.length(); i++) {
                        buildRequestCard(requests.optJSONObject(i));
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    requestsLoading.setVisibility(View.GONE);
                    emptyRequestsCard.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    // =========================================================================
    // BUILD STYLED REQUEST CARD
    // =========================================================================
    private void buildRequestCard(JSONObject req) {
        if (req == null) return;
        try {
            String serviceType   = req.optString("service_type",   "Maintenance");
            String title         = req.optString("title",          "—");
            String description   = req.optString("description",    "");
            String status        = req.optString("status",         "Under Review");
            String requestNumber = req.optString("request_number", "MNT-XXXX");
            String priority      = req.optString("priority",       "Normal");
            String createdAt     = req.optString("created_at",     "");

            int dp4  = dp(4);
            int dp8  = dp(8);
            int dp10 = dp(10);
            int dp12 = dp(12);
            int dp14 = dp(14);
            int dp16 = dp(16);

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
            card.setClickable(true);
            card.setFocusable(true);

            // ── Inner layout ──
            LinearLayout inner = new LinearLayout(this);
            inner.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            inner.setOrientation(LinearLayout.VERTICAL);
            inner.setPadding(dp16, dp16, dp16, dp16);

            // ── Header: emoji icon + service/unit + badge ──
            LinearLayout headerRow = new LinearLayout(this);
            LinearLayout.LayoutParams hrp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            hrp.setMargins(0, 0, 0, dp12);
            headerRow.setLayoutParams(hrp);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);

            // Emoji icon card
            CardView iconCard = new CardView(this);
            iconCard.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
            iconCard.setRadius(dp(13));
            iconCard.setCardElevation(0);
            iconCard.setCardBackgroundColor(serviceIconBg(serviceType));

            TextView iconTv = new TextView(this);
            iconTv.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
            iconTv.setText(serviceEmoji(serviceType));
            iconTv.setTextSize(16);
            iconCard.addView(iconTv);
            headerRow.addView(iconCard);

            // Name + time
            LinearLayout nameCol = new LinearLayout(this);
            LinearLayout.LayoutParams ncP = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            ncP.setMarginStart(dp12);
            nameCol.setLayoutParams(ncP);
            nameCol.setOrientation(LinearLayout.VERTICAL);

            TextView nameTv = new TextView(this);
            nameTv.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            nameTv.setText(serviceType + " · Unit " + currentApartmentNumber);
            nameTv.setTextColor(Color.parseColor("#1E293B"));
            nameTv.setTextSize(13);
            nameTv.setTypeface(null, android.graphics.Typeface.BOLD);
            nameCol.addView(nameTv);

            TextView timeTv = new TextView(this);
            timeTv.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            timeTv.setText(formatRelativeTime(createdAt)
                    + (priority.equals("Normal") ? "" : " · " + priority));
            timeTv.setTextColor(Color.parseColor("#94A3B8"));
            timeTv.setTextSize(11);
            nameCol.addView(timeTv);
            headerRow.addView(nameCol);

            // 🔧 Maintenance badge
            CardView badgeCard = new CardView(this);
            badgeCard.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(22)));
            badgeCard.setRadius(dp(11));
            badgeCard.setCardElevation(0);
            badgeCard.setCardBackgroundColor(Color.parseColor("#FEF3C7"));

            TextView badgeTv = new TextView(this);
            badgeTv.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
            badgeTv.setPadding(dp10, 0, dp10, 0);
            badgeTv.setText("🔧 Maintenance");
            badgeTv.setTextColor(Color.parseColor("#D97706"));
            badgeTv.setTextSize(10);
            badgeTv.setTypeface(null, android.graphics.Typeface.BOLD);
            badgeCard.addView(badgeTv);
            headerRow.addView(badgeCard);

            inner.addView(headerRow);

            // ── Inline status card ──
            CardView statusCard = new CardView(this);
            LinearLayout.LayoutParams scp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            scp.setMargins(0, 0, 0, dp12);
            statusCard.setLayoutParams(scp);
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

            LinearLayout scText = new LinearLayout(this);
            scText.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            scText.setOrientation(LinearLayout.VERTICAL);

            TextView scTitle = new TextView(this);
            LinearLayout.LayoutParams scTitleP = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            scTitleP.setMargins(0, 0, 0, dp(2));
            scTitle.setLayoutParams(scTitleP);
            scTitle.setText(title);
            scTitle.setTextColor(Color.parseColor("#1E293B"));
            scTitle.setTextSize(13);
            scTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            scText.addView(scTitle);

            TextView scReqId = new TextView(this);
            scReqId.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            scReqId.setText("Request #" + requestNumber);
            scReqId.setTextColor(Color.parseColor("#94A3B8"));
            scReqId.setTextSize(11);
            scText.addView(scReqId);
            scInner.addView(scText);

            // Status badge
            CardView statusBadge = new CardView(this);
            statusBadge.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(24)));
            statusBadge.setRadius(dp12);
            statusBadge.setCardElevation(0);
            statusBadge.setCardBackgroundColor(statusBgColor(status));

            TextView statusTv = new TextView(this);
            statusTv.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
            statusTv.setPadding(dp10, 0, dp10, 0);
            statusTv.setText(status);
            statusTv.setTextColor(statusTextColor(status));
            statusTv.setTextSize(10);
            statusTv.setTypeface(null, android.graphics.Typeface.BOLD);
            statusBadge.addView(statusTv);
            scInner.addView(statusBadge);

            statusCard.addView(scInner);
            inner.addView(statusCard);

            // ── Divider ──
            View divider = new View(this);
            LinearLayout.LayoutParams dp1 = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
            dp1.setMargins(0, 0, 0, dp10);
            divider.setLayoutParams(dp1);
            divider.setBackgroundColor(Color.parseColor("#F1F5F9"));
            inner.addView(divider);

            // ── Footer: description + track ──
            LinearLayout footer = new LinearLayout(this);
            footer.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            footer.setOrientation(LinearLayout.HORIZONTAL);
            footer.setGravity(Gravity.CENTER_VERTICAL);

            TextView descTv = new TextView(this);
            descTv.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            descTv.setText(description.isEmpty() ? "No description" : description);
            descTv.setTextColor(Color.parseColor("#94A3B8"));
            descTv.setTextSize(11);
            descTv.setMaxLines(1);
            descTv.setEllipsize(android.text.TextUtils.TruncateAt.END);
            footer.addView(descTv);

            TextView trackTv = new TextView(this);
            trackTv.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            trackTv.setText("Track Request");
            trackTv.setTextColor(Color.parseColor("#3667A6"));
            trackTv.setTextSize(12);
            trackTv.setTypeface(null, android.graphics.Typeface.BOLD);
            footer.addView(trackTv);

            inner.addView(footer);
            card.addView(inner);
            requestsContainer.addView(card);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    // SERVICE CARD DIALOGS
    // =========================================================================
    private void setupServiceCards() {
        CardView plumbing  = findViewById(R.id.servicePlumbing);
        CardView electric  = findViewById(R.id.serviceElectric);
        CardView gas       = findViewById(R.id.serviceGas);
        CardView ac        = findViewById(R.id.serviceAC);
        CardView carpentry = findViewById(R.id.serviceCarpentry);
        CardView cleaning  = findViewById(R.id.serviceCleaning);

        if (plumbing  != null) plumbing.setOnClickListener(v ->
                showRequestDialog(SVC_PLUMBING, "Pipes, taps & drains",
                        new String[]{"Leaking tap", "Blocked drain", "Burst pipe",
                                "Low water pressure", "Other"}));

        if (electric  != null) electric.setOnClickListener(v ->
                showRequestDialog(SVC_ELECTRIC, "Wiring & power issues",
                        new String[]{"Power trip", "No power in room",
                                "Faulty switch", "Light not working", "Other"}));

        if (gas       != null) gas.setOnClickListener(v ->
                showRequestDialog(SVC_GAS, "Gas leaks & fittings",
                        new String[]{"Gas smell", "Faulty gas fitting",
                                "Gas meter issue", "Stove not igniting", "Other"}));

        if (ac        != null) ac.setOnClickListener(v ->
                showRequestDialog(SVC_AC, "AC service & repair",
                        new String[]{"Not cooling", "Not heating", "Strange noise",
                                "Water leaking from unit", "Remote not working", "Other"}));

        if (carpentry != null) carpentry.setOnClickListener(v ->
                showRequestDialog(SVC_CARPENTRY, "Doors & furniture",
                        new String[]{"Door not closing", "Broken cabinet",
                                "Wardrobe damage", "Window frame issue", "Other"}));

        if (cleaning  != null) cleaning.setOnClickListener(v ->
                showRequestDialog(SVC_CLEANING, "Deep & common area cleaning",
                        new String[]{"Unit deep clean", "Common area",
                                "Post-renovation clean", "Pest cleaning", "Other"}));
    }

    private void showRequestDialog(String serviceType,
                                   String subtitle,
                                   String[] issues) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_maintenance_request);
        dialog.getWindow().setBackgroundDrawable(
                new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);

        TextView tvName = dialog.findViewById(R.id.dialogServiceName);
        TextView tvSub  = dialog.findViewById(R.id.dialogServiceSubtitle);
        AutoCompleteTextView issueDropdown    = dialog.findViewById(R.id.issueDropdown);
        TextInputEditText    etDescription   = dialog.findViewById(R.id.etDescription);
        AutoCompleteTextView priorityDropdown = dialog.findViewById(R.id.priorityDropdown);
        MaterialButton btnSubmit = dialog.findViewById(R.id.btnSubmitRequest);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancelRequest);

        tvName.setText(serviceType);
        tvSub.setText(subtitle);

        issueDropdown.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, issues));
        issueDropdown.setText(issues[0], false);

        priorityDropdown.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line,
                new String[]{"Normal", "Urgent", "Emergency"}));
        priorityDropdown.setText("Normal", false);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            String issue       = issueDropdown.getText().toString().trim();
            String description = etDescription.getText() != null
                    ? etDescription.getText().toString().trim() : "";
            String priority    = priorityDropdown.getText().toString().trim();

            if (description.isEmpty()) {
                etDescription.setError("Please describe the issue");
                etDescription.requestFocus();
                return;
            }

            btnSubmit.setEnabled(false);
            btnSubmit.setText("Submitting…");
            submitRequest(serviceType, issue, description, priority,
                    dialog, btnSubmit);
        });

        dialog.show();
    }

    // =========================================================================
    // SUBMIT REQUEST
    // =========================================================================
    private void submitRequest(String serviceType, String issue,
                               String description, String priority,
                               Dialog dialog, MaterialButton btnSubmit) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("user_id",           userId);
                body.put("apartment_number",  currentApartmentNumber);
                body.put("service_type",      serviceType);
                body.put("title",             issue);
                body.put("description",       description);
                body.put("priority",          priority);
                body.put("status",            "Under Review");

                HttpURLConnection conn = (HttpURLConnection)
                        new URL(SupabaseClient.SUPABASE_URL
                                + "/rest/v1/maintenance_requests")
                                .openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey",
                        SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization",
                        "Bearer " + token);
                conn.setRequestProperty("Content-Type",
                        "application/json");
                conn.setRequestProperty("Prefer",
                        "return=representation");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                os.close();

                int code = conn.getResponseCode();
                String requestNumber = "MNT-XXXX";

                if (code == 201 || code == 200) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();
                    try {
                        JSONArray arr = new JSONArray(sb.toString());
                        if (arr.length() > 0)
                            requestNumber = arr.getJSONObject(0)
                                    .optString("request_number", requestNumber);
                    } catch (Exception ignored) {}
                }

                conn.disconnect();

                String finalReq = requestNumber;
                runOnUiThread(() -> {
                    dialog.dismiss();
                    showSuccessDialog(serviceType, finalReq);
                    loadMyRequests();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Submit Request");
                    Toast.makeText(this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // =========================================================================
    // SUCCESS DIALOG
    // =========================================================================
    private void showSuccessDialog(String serviceType, String requestNumber) {
        Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(R.layout.dialog_maintenance_success);
        d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        d.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.85),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        d.setCancelable(false);

        TextView tvNum     = d.findViewById(R.id.tvRequestNumber);
        TextView tvConfirm = d.findViewById(R.id.tvServiceConfirm);
        MaterialButton btnDone = d.findViewById(R.id.btnDone);

        if (tvNum     != null) tvNum.setText("#" + requestNumber);
        if (tvConfirm != null) tvConfirm.setText(serviceType + " request submitted");
        if (btnDone   != null) btnDone.setOnClickListener(v -> d.dismiss());

        d.show();
    }

    // =========================================================================
    // HELPERS
    // =========================================================================
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int serviceIconBg(String type) {
        switch (type) {
            case SVC_PLUMBING:  return Color.parseColor("#DBEAFE");
            case SVC_ELECTRIC:  return Color.parseColor("#FEF3C7");
            case SVC_GAS:       return Color.parseColor("#FFE4E6");
            case SVC_AC:        return Color.parseColor("#CFFAFE");
            case SVC_CARPENTRY: return Color.parseColor("#FED7AA");
            case SVC_CLEANING:  return Color.parseColor("#DCFCE7");
            default:            return Color.parseColor("#EEF4FB");
        }
    }

    private String serviceEmoji(String type) {
        switch (type) {
            case SVC_PLUMBING:  return "🚿";
            case SVC_ELECTRIC:  return "⚡";
            case SVC_GAS:       return "🔥";
            case SVC_AC:        return "❄️";
            case SVC_CARPENTRY: return "🪚";
            case SVC_CLEANING:  return "🧹";
            default:            return "🔧";
        }
    }

    private int statusBgColor(String status) {
        switch (status.toLowerCase()) {
            case "in progress":  return Color.parseColor("#FEF3C7");
            case "completed":
            case "resolved":     return Color.parseColor("#DCFCE7");
            case "scheduled":
            case "under review": return Color.parseColor("#DBEAFE");
            default:             return Color.parseColor("#F1F5F9");
        }
    }

    private int statusTextColor(String status) {
        switch (status.toLowerCase()) {
            case "in progress":  return Color.parseColor("#D97706");
            case "completed":
            case "resolved":     return Color.parseColor("#16A34A");
            case "scheduled":
            case "under review": return Color.parseColor("#3667A6");
            default:             return Color.parseColor("#64748B");
        }
    }

    private String formatRelativeTime(String isoTime) {
        if (isoTime == null || isoTime.isEmpty()) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d = sdf.parse(isoTime.substring(0, 19));
            if (d == null) return "";
            long diff = (System.currentTimeMillis() - d.getTime()) / 1000;
            if (diff < 60)     return "Just now";
            if (diff < 3600)   return (diff / 60) + " min ago";
            if (diff < 86400)  return (diff / 3600) + " hours ago";
            if (diff < 172800) return "Yesterday";
            return (diff / 86400) + " days ago";
        } catch (Exception e) { return ""; }
    }

    // ── Extract userId from JWT ──────────────────────────────────────────────
    private String extractUserIdFromToken(String tkn) {
        try {
            String[] parts = tkn.split("\\.");
            if (parts.length < 2) return "";
            String payload = new String(
                    Base64.decode(parts[1], Base64.URL_SAFE),
                    StandardCharsets.UTF_8);
            return new JSONObject(payload).optString("sub", "");
        } catch (Exception e) { return ""; }
    }

    private void setupBackButton() {
        CardView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }
}