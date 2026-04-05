package com.example.apartmentmanagementsystem;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
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

public class MaintenanceStaffActivity extends AppCompatActivity {

    private LinearLayout requestsContainer;
    private ProgressBar  loadingBar;
    private CardView     emptyCard;
    private LinearLayout tabsRow;

    private String token  = "";
    private String userId = "";
    private String activeFilter = "All";

    private static final String[] STATUSES = {
            "All", "Under Review", "In Progress", "Scheduled", "Completed"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maintenance_staff);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.activity_maintenance_staff), (v, ins) -> {
                    Insets bars = ins.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return ins;
                });

        requestsContainer = findViewById(R.id.staffRequestsContainer);
        loadingBar        = findViewById(R.id.staffLoadingBar);
        emptyCard         = findViewById(R.id.staffEmptyCard);
        tabsRow           = findViewById(R.id.filterTabsRow);


        buildFilterTabs();
        loadSession();
    }

    private void loadSession() {
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        token = prefs.getString("access_token", null);
        if (token == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        userId = extractUserIdFromToken(token);

        String fullName = prefs.getString("full_name", "Maintenance Staff");
        TextView staffName = findViewById(R.id.staffNameText);
        if (staffName != null) staffName.setText(fullName);

        CardView btnLogout = findViewById(R.id.btnLogout);

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                prefs.edit().clear().apply();

                Intent intent = new Intent(MaintenanceStaffActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        loadStats();
        fetchRequests();
    }

    private void loadStats() {
        new Thread(() -> {
            try {
                int pending    = countByStatus("Under Review");
                int inProgress = countByStatus("In Progress");
                int completed  = countByStatus("Completed");
                int total      = pending + inProgress + completed;

                runOnUiThread(() -> {
                    TextView heroTotal = findViewById(R.id.heroTotalCount);
                    TextView heroSub   = findViewById(R.id.heroSubText);
                    TextView tvPending = findViewById(R.id.countPending);
                    TextView tvProgress= findViewById(R.id.countInProgress);
                    TextView tvDone    = findViewById(R.id.countDone);
                    TextView reqLabel  = findViewById(R.id.requestCountLabel);

                    if (heroTotal  != null) heroTotal.setText(total + " Total Requests");
                    if (heroSub    != null) heroSub.setText(
                            pending + " pending · " + inProgress + " active");
                    if (tvPending  != null) tvPending.setText(String.valueOf(pending));
                    if (tvProgress != null) tvProgress.setText(String.valueOf(inProgress));
                    if (tvDone     != null) tvDone.setText(String.valueOf(completed));
                    if (reqLabel   != null) reqLabel.setText(total + " total →");
                });
            } catch (Exception ignored) {}
        }).start();
    }

    private int countByStatus(String status) {
        try {
            String url = SupabaseClient.SUPABASE_URL
                    + "/rest/v1/maintenance_requests"
                    + "?status=eq." + status.replace(" ", "%20")
                    + "&select=id";
            HttpURLConnection conn = openGet(url);
            String body = readBody(conn);
            conn.disconnect();
            return new JSONArray(body).length();
        } catch (Exception e) { return 0; }
    }
    private void buildFilterTabs() {
        tabsRow.removeAllViews();
        for (String s : STATUSES) {
            boolean active = s.equals(activeFilter);

            CardView tab = new CardView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(34));
            lp.setMarginEnd(dp(8));
            tab.setLayoutParams(lp);
            tab.setRadius(dp(17));
            tab.setCardElevation(0);
            tab.setCardBackgroundColor(active
                    ? Color.parseColor("#214177")
                    : Color.parseColor("#EEF4FB"));

            TextView tv = new TextView(this);
            tv.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
            tv.setPadding(dp(14), 0, dp(14), 0);
            tv.setText(s);
            tv.setTextSize(12);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextColor(active
                    ? Color.WHITE
                    : Color.parseColor("#214177"));
            tab.addView(tv);

            tab.setOnClickListener(v -> {
                activeFilter = s;
                buildFilterTabs();
                fetchRequests();
            });

            tabsRow.addView(tab);
        }
    }

    private void fetchRequests() {
        loadingBar.setVisibility(View.VISIBLE);
        emptyCard.setVisibility(View.GONE);
        requestsContainer.removeAllViews();

        new Thread(() -> {
            try {
                StringBuilder urlB = new StringBuilder(
                        SupabaseClient.SUPABASE_URL
                                + "/rest/v1/maintenance_requests"
                                + "?order=created_at.desc");

                if (!activeFilter.equals("All")) {
                    urlB.append("&status=eq.")
                            .append(activeFilter.replace(" ", "%20"));
                }

                HttpURLConnection conn = openGet(urlB.toString());
                String body = readBody(conn);
                conn.disconnect();

                JSONArray arr = new JSONArray(body);

                runOnUiThread(() -> {
                    loadingBar.setVisibility(View.GONE);
                    requestsContainer.removeAllViews();
                    if (arr.length() == 0) {
                        emptyCard.setVisibility(View.VISIBLE);
                        return;
                    }
                    emptyCard.setVisibility(View.GONE);
                    for (int i = 0; i < arr.length(); i++) {
                        buildRequestCard(arr.optJSONObject(i));
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    loadingBar.setVisibility(View.GONE);
                    emptyCard.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void buildRequestCard(JSONObject req) {
        if (req == null) return;
        try {
            String reqId      = req.optString("id", "");
            String serviceType = req.optString("service_type", "Maintenance");
            String title       = req.optString("title", "—");
            String description = req.optString("description", "");
            String status      = req.optString("status", "Under Review");
            String reqNumber   = req.optString("request_number", "MNT-XXXX");
            String priority    = req.optString("priority", "Normal");
            String apartment   = req.optString("apartment_number", "—");
            String createdAt   = req.optString("created_at", "");

            // ── Outer card ────────────────────────────────────────────────────
            CardView card = new CardView(this);
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLp.setMargins(dp(16), 0, dp(16), dp(12));
            card.setLayoutParams(cardLp);
            card.setRadius(dp(20));
            card.setCardElevation(0);
            card.setCardBackgroundColor(Color.WHITE);

            LinearLayout inner = new LinearLayout(this);
            inner.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            inner.setOrientation(LinearLayout.VERTICAL);
            inner.setPadding(dp(16), dp(16), dp(16), dp(16));

            // ── Priority stripe at top ─────────────────────────────────────
            if (!priority.equals("Normal")) {
                View stripe = new View(this);
                LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(3));
                sp.setMargins(-dp(16), -dp(16), -dp(16), dp(12));
                stripe.setLayoutParams(sp);
                stripe.setBackgroundColor(priority.equals("Emergency")
                        ? Color.parseColor("#EF4444")
                        : Color.parseColor("#F59E0B"));
                inner.addView(stripe);
            }

            LinearLayout header = new LinearLayout(this);
            LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            hlp.setMargins(0, 0, 0, dp(12));
            header.setLayoutParams(hlp);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);

            // Emoji icon
            CardView iconCard = new CardView(this);
            iconCard.setLayoutParams(new LinearLayout.LayoutParams(dp(42), dp(42)));
            iconCard.setRadius(dp(14));
            iconCard.setCardElevation(0);
            iconCard.setCardBackgroundColor(serviceIconBg(serviceType));
            TextView iconTv = new TextView(this);
            iconTv.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
            iconTv.setText(serviceEmoji(serviceType));
            iconTv.setTextSize(17);
            iconCard.addView(iconTv);
            header.addView(iconCard);

            LinearLayout nameCol = new LinearLayout(this);
            LinearLayout.LayoutParams ncp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            ncp.setMarginStart(dp(12));
            nameCol.setLayoutParams(ncp);
            nameCol.setOrientation(LinearLayout.VERTICAL);

            TextView svcTv = new TextView(this);
            svcTv.setText(serviceType);
            svcTv.setTextColor(Color.parseColor("#1E293B"));
            svcTv.setTextSize(14);
            svcTv.setTypeface(null, Typeface.BOLD);
            nameCol.addView(svcTv);

            TextView metaTv = new TextView(this);
            metaTv.setText("Unit " + apartment
                    + "  ·  " + formatRelativeTime(createdAt)
                    + (priority.equals("Normal") ? "" : "  ·  ⚠ " + priority));
            metaTv.setTextColor(Color.parseColor("#94A3B8"));
            metaTv.setTextSize(11);
            nameCol.addView(metaTv);
            header.addView(nameCol);

            // Status badge
            CardView statusBadge = new CardView(this);
            statusBadge.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(24)));
            statusBadge.setRadius(dp(12));
            statusBadge.setCardElevation(0);
            statusBadge.setCardBackgroundColor(statusBgColor(status));
            TextView statusTv = new TextView(this);
            statusTv.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
            statusTv.setPadding(dp(10), 0, dp(10), 0);
            statusTv.setText(status);
            statusTv.setTextColor(statusTextColor(status));
            statusTv.setTextSize(10);
            statusTv.setTypeface(null, Typeface.BOLD);
            statusBadge.addView(statusTv);
            header.addView(statusBadge);

            inner.addView(header);

            // ── Info card ─────────────────────────────────────────────────
            CardView infoCard = new CardView(this);
            LinearLayout.LayoutParams icp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            icp.setMargins(0, 0, 0, dp(12));
            infoCard.setLayoutParams(icp);
            infoCard.setRadius(dp(14));
            infoCard.setCardElevation(0);
            infoCard.setCardBackgroundColor(Color.parseColor("#F8FAFC"));

            LinearLayout infoInner = new LinearLayout(this);
            infoInner.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            infoInner.setOrientation(LinearLayout.VERTICAL);
            infoInner.setPadding(dp(12), dp(12), dp(12), dp(12));

            TextView titleTv = new TextView(this);
            LinearLayout.LayoutParams ttp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            ttp.setMargins(0, 0, 0, dp(2));
            titleTv.setLayoutParams(ttp);
            titleTv.setText(title);
            titleTv.setTextColor(Color.parseColor("#1E293B"));
            titleTv.setTextSize(13);
            titleTv.setTypeface(null, Typeface.BOLD);
            infoInner.addView(titleTv);

            TextView reqNumTv = new TextView(this);
            LinearLayout.LayoutParams rnp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            rnp.setMargins(0, 0, 0, dp(6));
            reqNumTv.setLayoutParams(rnp);
            reqNumTv.setText("Request #" + reqNumber);
            reqNumTv.setTextColor(Color.parseColor("#94A3B8"));
            reqNumTv.setTextSize(11);
            infoInner.addView(reqNumTv);

            if (!description.isEmpty()) {
                TextView descTv = new TextView(this);
                descTv.setText(description);
                descTv.setTextColor(Color.parseColor("#64748B"));
                descTv.setTextSize(12);
                descTv.setMaxLines(2);
                descTv.setEllipsize(android.text.TextUtils.TruncateAt.END);
                infoInner.addView(descTv);
            }

            infoCard.addView(infoInner);
            inner.addView(infoCard);

            // ── Divider ───────────────────────────────────────────────────
            View divider = new View(this);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
            dlp.setMargins(0, 0, 0, dp(10));
            divider.setLayoutParams(dlp);
            divider.setBackgroundColor(Color.parseColor("#F1F5F9"));
            inner.addView(divider);

            // ── Action buttons row ────────────────────────────────────────
            LinearLayout actions = new LinearLayout(this);
            actions.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setGravity(Gravity.CENTER_VERTICAL);

            // "Update Status" button
            MaterialButton btnUpdate = new MaterialButton(this);
            LinearLayout.LayoutParams bup = new LinearLayout.LayoutParams(
                    0, dp(36), 1f);
            bup.setMarginEnd(dp(8));
            btnUpdate.setLayoutParams(bup);
            btnUpdate.setText("Update Status");
            btnUpdate.setTextSize(12);
            btnUpdate.setCornerRadius(dp(18));
            btnUpdate.setPadding(dp(12), 0, dp(12), 0);
            btnUpdate.setInsetTop(0);
            btnUpdate.setInsetBottom(0);
            btnUpdate.setBackgroundColor(Color.parseColor("#214177"));
            btnUpdate.setTextColor(Color.WHITE);
            btnUpdate.setOnClickListener(v ->
                    showUpdateStatusDialog(reqId, status));

            // "View Details" button
            MaterialButton btnDetails = new MaterialButton(this,
                    null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btnDetails.setLayoutParams(new LinearLayout.LayoutParams(
                    0, dp(36), 1f));
            btnDetails.setText("Details");
            btnDetails.setTextSize(12);
            btnDetails.setCornerRadius(dp(18));
            btnDetails.setPadding(dp(12), 0, dp(12), 0);
            btnDetails.setInsetTop(0);
            btnDetails.setInsetBottom(0);
            btnDetails.setTextColor(Color.parseColor("#214177"));
            btnDetails.setStrokeColor(android.content.res.ColorStateList.valueOf(
                    Color.parseColor("#CBD5E1")));
            btnDetails.setBackgroundColor(Color.TRANSPARENT);
            btnDetails.setOnClickListener(v ->
                    showDetailsDialog(req));

            actions.addView(btnUpdate);
            actions.addView(btnDetails);
            inner.addView(actions);

            card.addView(inner);
            requestsContainer.addView(card);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showUpdateStatusDialog(String reqId, String currentStatus) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_update_status);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.90),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);

        AutoCompleteTextView dropdown = dialog.findViewById(R.id.statusDropdown);
        MaterialButton btnSave   = dialog.findViewById(R.id.btnSaveStatus);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancelStatus);

        String[] options = {"Under Review", "In Progress", "Scheduled", "Completed"};
        dropdown.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, options));
        dropdown.setText(currentStatus, false);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String newStatus = dropdown.getText().toString().trim();
            if (newStatus.isEmpty()) return;
            btnSave.setEnabled(false);
            btnSave.setText("Saving…");
            updateRequestStatus(reqId, newStatus, dialog, btnSave);
        });

        dialog.show();
    }

    private void showDetailsDialog(JSONObject req) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_request_details);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.92),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);

        try {
            setText(dialog, R.id.detailServiceType,
                    req.optString("service_type", "—"));
            setText(dialog, R.id.detailTitle,
                    req.optString("title", "—"));
            setText(dialog, R.id.detailDescription,
                    req.optString("description", "No description"));
            setText(dialog, R.id.detailApartment,
                    "Unit " + req.optString("apartment_number", "—"));
            setText(dialog, R.id.detailStatus,
                    req.optString("status", "—"));
            setText(dialog, R.id.detailPriority,
                    req.optString("priority", "Normal"));
            setText(dialog, R.id.detailReqNumber,
                    "#" + req.optString("request_number", "MNT-XXXX"));
            setText(dialog, R.id.detailCreatedAt,
                    formatRelativeTime(req.optString("created_at", "")));
        } catch (Exception ignored) {}

        MaterialButton btnClose = dialog.findViewById(R.id.btnCloseDetails);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void setText(Dialog d, int viewId, String text) {
        TextView tv = d.findViewById(viewId);
        if (tv != null) tv.setText(text);
    }

    private void updateRequestStatus(String reqId, String newStatus,
                                     Dialog dialog, MaterialButton btnSave) {
        new Thread(() -> {
            try {
                String url = SupabaseClient.SUPABASE_URL
                        + "/rest/v1/maintenance_requests"
                        + "?id=eq." + reqId;

                JSONObject body = new JSONObject();
                body.put("status", newStatus);

                HttpURLConnection conn =
                        (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("PATCH");
                conn.setRequestProperty("apikey",
                        SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Prefer", "return=minimal");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                conn.disconnect();

                runOnUiThread(() -> {
                    dialog.dismiss();
                    if (code == 204 || code == 200) {
                        Toast.makeText(this,
                                "Status updated to: " + newStatus,
                                Toast.LENGTH_SHORT).show();
                        fetchRequests();
                    } else {
                        btnSave.setEnabled(true);
                        btnSave.setText("Save");
                        Toast.makeText(this,
                                "Update failed (code " + code + ")",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Save");
                    Toast.makeText(this, "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private HttpURLConnection openGet(String urlStr) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        c.setRequestMethod("GET");
        c.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
        c.setRequestProperty("Authorization", "Bearer " + token);
        return c;
    }

    private String readBody(HttpURLConnection c) throws Exception {
        BufferedReader r = new BufferedReader(
                new InputStreamReader(c.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) sb.append(line);
        r.close();
        return sb.toString();
    }

    private String extractUserIdFromToken(String tkn) {
        try {
            String[] p = tkn.split("\\.");
            if (p.length < 2) return "";
            String payload = new String(
                    Base64.decode(p[1], Base64.URL_SAFE),
                    StandardCharsets.UTF_8);
            return new JSONObject(payload).optString("sub", "");
        } catch (Exception e) { return ""; }
    }


    private String formatRelativeTime(String iso) {
        if (iso == null || iso.isEmpty()) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d = sdf.parse(iso.substring(0, 19));
            if (d == null) return "";
            long diff = (System.currentTimeMillis() - d.getTime()) / 1000;
            if (diff < 60)     return "Just now";
            if (diff < 3600)   return (diff / 60) + "m ago";
            if (diff < 86400)  return (diff / 3600) + "h ago";
            if (diff < 172800) return "Yesterday";
            return (diff / 86400) + " days ago";
        } catch (Exception e) { return ""; }
    }

    private int serviceIconBg(String t) {
        switch (t) {
            case "Plumbing":        return Color.parseColor("#DBEAFE");
            case "Electrician":     return Color.parseColor("#FEF3C7");
            case "Gas":             return Color.parseColor("#FFE4E6");
            case "Air Conditioning":return Color.parseColor("#CFFAFE");
            case "Carpentry":       return Color.parseColor("#FED7AA");
            case "Cleaning":        return Color.parseColor("#DCFCE7");
            default:                return Color.parseColor("#EEF4FB");
        }
    }

    private String serviceEmoji(String t) {
        switch (t) {
            case "Plumbing":        return "🚿";
            case "Electrician":     return "⚡";
            case "Gas":             return "🔥";
            case "Air Conditioning":return "❄️";
            case "Carpentry":       return "🪚";
            case "Cleaning":        return "🧹";
            default:                return "🔧";
        }
    }

    private int statusBgColor(String s) {
        switch (s.toLowerCase()) {
            case "in progress":  return Color.parseColor("#FEF3C7");
            case "completed":    return Color.parseColor("#DCFCE7");
            case "scheduled":    return Color.parseColor("#EDE9FE");
            case "under review": return Color.parseColor("#DBEAFE");
            default:             return Color.parseColor("#F1F5F9");
        }
    }

    private int statusTextColor(String s) {
        switch (s.toLowerCase()) {
            case "in progress":  return Color.parseColor("#D97706");
            case "completed":    return Color.parseColor("#16A34A");
            case "scheduled":    return Color.parseColor("#7C3AED");
            case "under review": return Color.parseColor("#1D4ED8");
            default:             return Color.parseColor("#64748B");
        }
    }
}