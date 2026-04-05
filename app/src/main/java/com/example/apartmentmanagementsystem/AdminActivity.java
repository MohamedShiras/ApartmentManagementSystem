package com.example.apartmentmanagementsystem;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
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
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

public class AdminActivity extends AppCompatActivity {

    // ── Section keys ──────────────────────────────────────────────────────────
    private static final String SEC_RESERVATIONS = "RESERVATIONS";
    private static final String SEC_COMPLAINTS   = "COMPLAINTS";
    private static final String SEC_NOTICES      = "NOTICES";
    private static final String SEC_USERS        = "USERS";

    // ── Views ─────────────────────────────────────────────────────────────────
    private LinearLayout contentContainer;
    private LinearLayout filterTabsRow;
    private View         filterScrollView;
    private ProgressBar  loadingBar;
    private TextView     sectionLabel;
    private TextView     sectionActionBtn;

    // ── Session ───────────────────────────────────────────────────────────────
    private String token  = "";
    private String userId = "";

    // ── State ─────────────────────────────────────────────────────────────────
    private String activeSection = "";
    private String activeFilter  = "All";

    // ── FIX: Users cache to resolve reservations without DB join ──────────────
    private final HashMap<String, JSONObject> usersMap = new HashMap<>();

    // =========================================================================
    // LIFECYCLE
    // =========================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.activity_admin), (v, ins) -> {
                    Insets bars = ins.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return ins;
                });

        contentContainer  = findViewById(R.id.adminContentContainer);
        filterTabsRow     = findViewById(R.id.adminFilterTabs);
        filterScrollView  = findViewById(R.id.filterScrollView);
        loadingBar        = findViewById(R.id.adminLoadingBar);
        sectionLabel      = findViewById(R.id.sectionLabel);
        sectionActionBtn  = findViewById(R.id.sectionActionBtn);

        loadSession();
        setupNavCards();
        loadDashboardStats();
        openSection(SEC_RESERVATIONS);
    }

    // =========================================================================
    // SESSION
    // =========================================================================
    private void loadSession() {
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        token  = prefs.getString("access_token", "");
        userId = extractUserIdFromToken(token);

        String name = prefs.getString("full_name", "Administrator");
        TextView tv = findViewById(R.id.adminNameText);
        if (tv != null) tv.setText(name);

        CardView btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) btnLogout.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            startActivity(new Intent(this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        });
    }

    // =========================================================================
    // NAV CARDS
    // =========================================================================
    private void setupNavCards() {
        findViewById(R.id.navReservations).setOnClickListener(v -> openSection(SEC_RESERVATIONS));
        findViewById(R.id.navComplaints).setOnClickListener(v   -> openSection(SEC_COMPLAINTS));
        findViewById(R.id.navNotices).setOnClickListener(v      -> openSection(SEC_NOTICES));
        findViewById(R.id.navUsers).setOnClickListener(v        -> openSection(SEC_USERS));
    }

    // =========================================================================
    // DASHBOARD STATS
    // =========================================================================
    private void loadDashboardStats() {
        new Thread(() -> {
            try {
                int users        = countTable("users",        null, null);
                int reservations = countTable("reservations", null, null);
                int complaints   = countTable("complaints",   null, null);
                int notices      = countTable("notices",      null, null);

                runOnUiThread(() -> {
                    setTextSafe(R.id.statUsers,        String.valueOf(users));
                    setTextSafe(R.id.statReservations, String.valueOf(reservations));
                    setTextSafe(R.id.statComplaints,   String.valueOf(complaints));
                    setTextSafe(R.id.statNotices,      String.valueOf(notices));
                    setTextSafe(R.id.adminHeroSub,
                            users + " residents · " + complaints + " open complaints");
                    setTextSafe(R.id.badgeReservations, reservations + " total");
                    setTextSafe(R.id.badgeComplaints,   complaints + " total");
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    // =========================================================================
    // OPEN SECTION
    // =========================================================================
    private void openSection(String section) {
        activeSection = section;
        activeFilter  = "All";
        contentContainer.removeAllViews();
        filterTabsRow.removeAllViews();
        sectionActionBtn.setVisibility(View.GONE);
        filterScrollView.setVisibility(View.GONE);

        switch (section) {
            case SEC_RESERVATIONS:
                sectionLabel.setText("Reservations");
                filterScrollView.setVisibility(View.VISIBLE);
                buildFilterTabs(new String[]{"All", "Pending", "Accepted", "Cancelled"});
                fetchReservations();
                break;
            case SEC_COMPLAINTS:
                sectionLabel.setText("Complaints");
                filterScrollView.setVisibility(View.VISIBLE);
                buildFilterTabs(new String[]{"All", "Pending", "In Progress", "Resolved"});
                fetchComplaints();
                break;
            case SEC_NOTICES:
                sectionLabel.setText("Notices");
                sectionActionBtn.setVisibility(View.VISIBLE);
                sectionActionBtn.setText("+ Add Notice");
                sectionActionBtn.setOnClickListener(v -> showAddNoticeDialog());
                fetchNotices();
                break;
            case SEC_USERS:
                sectionLabel.setText("Users");
                sectionActionBtn.setVisibility(View.VISIBLE);
                sectionActionBtn.setText("+ Add User");
                sectionActionBtn.setOnClickListener(v -> showAddUserDialog());
                fetchUsers();
                break;
        }
    }

    // =========================================================================
    // FILTER TABS — modern pill style
    // =========================================================================
    private void buildFilterTabs(String[] filters) {
        filterTabsRow.removeAllViews();
        for (String f : filters) {
            boolean active = f.equals(activeFilter);

            CardView tab = new CardView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
            lp.setMarginEnd(dp(8));
            tab.setLayoutParams(lp);
            tab.setRadius(dp(18));
            tab.setCardElevation(active ? dp(3) : 0);
            tab.setCardBackgroundColor(active ? Color.parseColor("#214177") : Color.WHITE);

            TextView tv = new TextView(this);
            tv.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
            tv.setPadding(dp(18), 0, dp(18), 0);
            tv.setText(f);
            tv.setTextSize(12);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextColor(active ? Color.WHITE : Color.parseColor("#64748B"));
            tab.addView(tv);

            tab.setOnClickListener(v -> {
                activeFilter = f;
                buildFilterTabs(filters);
                switch (activeSection) {
                    case SEC_RESERVATIONS: fetchReservations(); break;
                    case SEC_COMPLAINTS:   fetchComplaints();   break;
                }
            });
            filterTabsRow.addView(tab);
        }
    }

    // =========================================================================
    // ── RESERVATIONS ──────────────────────────────────────────────────────────
    //    FIX: No join. Preload users into usersMap first, then fetch bookings.
    // =========================================================================

    /**
     * Loads all users into usersMap keyed by their ID, then runs [then] on the
     * UI thread. This avoids the PGRST200 foreign-key join error entirely.
     */
    private void preloadUsers(Runnable then) {
        new Thread(() -> {
            try {
                JSONArray arr = getArray(SupabaseClient.SUPABASE_URL
                        + "/rest/v1/users?select=id,full_name,apartment_number");
                usersMap.clear();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject u = arr.optJSONObject(i);
                    if (u != null) usersMap.put(u.optString("id"), u);
                }
            } catch (Exception e) { e.printStackTrace(); }
            runOnUiThread(then);
        }).start();
    }

    private void fetchReservations() {
        showLoading(true);
        contentContainer.removeAllViews();

        // Step 1 – load users, then step 2 – load reservations
        preloadUsers(() -> new Thread(() -> {
            try {
                StringBuilder url = new StringBuilder(
                        SupabaseClient.SUPABASE_URL
                                + "/rest/v1/reservations?order=created_at.desc&select=*");
                if (!activeFilter.equals("All"))
                    url.append("&status=eq.").append(activeFilter.replace(" ", "%20"));

                JSONArray arr = getArray(url.toString());

                runOnUiThread(() -> {
                    showLoading(false);
                    if (arr.length() == 0) { showEmpty("No reservations found"); return; }
                    for (int i = 0; i < arr.length(); i++)
                        buildReservationCard(arr.optJSONObject(i));
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showEmpty("Error: " + e.getMessage());
                });
            }
        }).start());
    }

    private void buildReservationCard(JSONObject r) {
        if (r == null) return;
        try {
            String id        = r.optString("id", "");
            String amenity   = r.optString("amenity_type", "—");
            String date      = r.optString("selected_date", "—");
            String timeSlot  = r.optString("time_slot", "—");
            String guests    = r.optString("guest_count", "—");
            String status    = r.optString("status", "Pending");
            String special   = r.optString("special_request", "");
            String createdAt = r.optString("created_at", "");
            String uid       = r.optString("user_id", "");

            // Resolve resident name & unit from preloaded map (no DB join needed)
            JSONObject userObj   = usersMap.get(uid);
            String residentName  = userObj != null ? userObj.optString("full_name", "Unknown Resident") : "Unknown Resident";
            String residentUnit  = userObj != null ? userObj.optString("apartment_number", "—") : "—";

            // Check if booking date has already passed
            boolean isPast = isDatePast(date);
            boolean isExpired = isPast && "Pending".equalsIgnoreCase(status);

            CardView card = makeCard();
            if (isExpired) card.setCardBackgroundColor(Color.parseColor("#FFFBEB"));
            LinearLayout inner = makeInner(card);

            // ── Header row ──
            LinearLayout hdr = makeRow(0, dp(10));
            hdr.addView(makeIconCard(amenityEmoji(amenity), amenityBg(amenity)));

            LinearLayout nameCol = makeNameCol(hdr);
            addBoldText(nameCol, residentName, "#1E293B", 13);
            addSmallText(nameCol, "Unit " + residentUnit + "  ·  " + amenity, "#94A3B8");
            hdr.addView(makeStatusBadge(isExpired ? "Expired" : status));
            inner.addView(hdr);

            // ── Info chips ──
            LinearLayout chips = makeRow(0, dp(8));
            chips.setLayoutParams(createMarginLP(0, 0, 0, dp(4)));
            addInfoChip(chips, "📅 " + date);
            addInfoChip(chips, "🕐 " + timeSlot);
            addInfoChip(chips, "👥 " + guests + " guests");
            inner.addView(chips);

            if (!special.isEmpty()) addSmallText(inner, "📝 " + special, "#64748B");

            inner.addView(makeDivider());

            // ── Status display (no buttons) ──
            if ("Accepted".equalsIgnoreCase(status)) {
                TextView timeTv = new TextView(this);
                timeTv.setText("✓ Accepted · " + formatRelativeTime(createdAt));
                timeTv.setTextColor(Color.parseColor("#16A34A"));
                timeTv.setTextSize(11);
                timeTv.setTypeface(null, Typeface.BOLD);
                LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                tlp.setMargins(0, dp(8), 0, 0);
                timeTv.setLayoutParams(tlp);
                inner.addView(timeTv);
            } else {
                // Pending and other statuses - just show status text
                TextView statusTv = new TextView(this);
                statusTv.setText("Status: " + status);
                statusTv.setTextColor(Color.parseColor("#64748B"));
                statusTv.setTextSize(11);
                statusTv.setTypeface(null, Typeface.BOLD);
                LinearLayout.LayoutParams stlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                stlp.setMargins(0, dp(8), 0, 0);
                statusTv.setLayoutParams(stlp);
                inner.addView(statusTv);
            }

            contentContainer.addView(card);
        } catch (Exception e) { e.printStackTrace(); }
    }

    /** Returns true if the booking date (dd/MM/yyyy) is before today */
    private boolean isDatePast(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date d = sdf.parse(dateStr);
            // Give a full day grace before treating as past
            return d != null && System.currentTimeMillis() > (d.getTime() + 86_400_000L);
        } catch (Exception e) { return false; }
    }

    private void confirmUpdateReservation(String id, String status) {
        Log.d("AdminActivity", "✓ confirmUpdateReservation called - ID: " + id + ", Status: " + status);
        Toast.makeText(this, "Confirmation dialog opened", Toast.LENGTH_SHORT).show();
        new android.app.AlertDialog.Builder(this)
                .setTitle("Accept Reservation")
                .setMessage("Accept this reservation? It will be marked as Accepted.")
                .setPositiveButton("Accept", (dlg, w) -> {
                    Log.d("AdminActivity", "✓ Accept button clicked - calling updateReservationStatus");
                    Toast.makeText(this, "Updating status to: " + status, Toast.LENGTH_SHORT).show();
                    updateReservationStatus(id, status);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeleteReservation(String id) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Cancel Reservation")
                .setMessage("Are you sure? This will permanently delete this reservation.")
                .setPositiveButton("Delete", (dlg, w) -> deleteReservation(id))
                .setNegativeButton("No", null)
                .show();
    }

    private void updateReservationStatus(String id, String status) {
        Log.d("AdminActivity", "→ updateReservationStatus started - ID: " + id + ", Status: " + status);
        new Thread(() -> {
            try {
                Log.d("AdminActivity", "→ Creating PATCH request...");
                JSONObject body = new JSONObject();
                body.put("status", status);
                Log.d("AdminActivity", "→ Request body: " + body.toString());

                patch("reservations", id, body);

                Log.d("AdminActivity", "✓ PATCH request successful!");
                runOnUiThread(() -> {
                    Toast.makeText(this, "✓ Reservation " + status + "!", Toast.LENGTH_SHORT).show();
                    Log.d("AdminActivity", "✓ Refreshing reservations list...");
                    fetchReservations();
                    loadDashboardStats();
                });
            } catch (Exception e) {
                Log.e("AdminActivity", "❌ Error updating reservation: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "❌ Update Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                });
            }
        }).start();
    }

    private void deleteReservation(String id) {
        new Thread(() -> {
            try {
                String url = SupabaseClient.SUPABASE_URL + "/rest/v1/reservations?id=eq." + id;
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Prefer", "return=minimal");
                conn.getResponseCode();
                conn.disconnect();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Reservation deleted", Toast.LENGTH_SHORT).show();
                    fetchReservations();
                    loadDashboardStats();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // =========================================================================
    // ── COMPLAINTS ────────────────────────────────────────────────────────────
    // FIX: Now preloads users and displays resident name in complaint cards
    // =========================================================================
    private void fetchComplaints() {
        showLoading(true);
        contentContainer.removeAllViews();

        // Step 1 – load users, then step 2 – load complaints
        preloadUsers(() -> new Thread(() -> {
            try {
                StringBuilder url = new StringBuilder(SupabaseClient.SUPABASE_URL + "/rest/v1/complaints?order=id.desc&select=*");
                if (!activeFilter.equals("All"))
                    url.append("&status=eq.").append(activeFilter.replace(" ", "%20"));

                JSONArray arr = getArray(url.toString());

                runOnUiThread(() -> {
                    showLoading(false);
                    if (arr.length() == 0) { showEmpty("No complaints found"); return; }
                    for (int i = 0; i < arr.length(); i++)
                        buildComplaintCard(arr.optJSONObject(i));
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showEmpty("Error loading complaints: " + e.getMessage());
                });
            }
        }).start());
    }

    private void buildComplaintCard(JSONObject c) {
        if (c == null) return;

        try {
            String id = c.optString("id", "");
            String category = c.optString("category", "Other");
            String subject = c.optString("subject", "No Subject");
            String description = c.optString("description", "");
            String status = c.optString("status", "Pending");
            String apartment = c.optString("apartment_number", "—");
            String date = c.optString("date", "");
            String uid = c.optString("user_id", "");

            // Resolve resident name from preloaded map
            JSONObject userObj = usersMap.get(uid);
            String residentName = userObj != null ? userObj.optString("full_name", "Unknown Resident") : "Unknown Resident";

            CardView card = makeCard();
            LinearLayout inner = makeInner(card);

            LinearLayout hdr = makeRow(0, dp(10));
            hdr.addView(makeIconCard(categoryEmoji(category), Color.parseColor("#FFE4E6")));

            LinearLayout nameCol = makeNameCol(hdr);
            addBoldText(nameCol, subject, "#1E293B", 13);
            addSmallText(nameCol, residentName + "  ·  Unit " + apartment +
                    (category.isEmpty() ? "" : "  ·  " + category) +
                    (date.isEmpty() ? "" : "  ·  " + date), "#94A3B8");
            hdr.addView(makeStatusBadge(status));

            inner.addView(hdr);

            if (!description.isEmpty()) {
                addSmallText(inner, description, "#64748B");
            }

            inner.addView(makeDivider());

            LinearLayout actions = makeRow(dp(8), 0);

            if ("Pending".equalsIgnoreCase(status)) {
                MaterialButton btnProgress = makeActionBtn("⚙ In Progress", "#D97706", false);
                MaterialButton btnResolve = makeActionBtn("✓ Resolve", "#16A34A", false);
                btnProgress.setOnClickListener(v -> confirmUpdateComplaintStatus(id, "In Progress"));
                btnResolve.setOnClickListener(v -> confirmUpdateComplaintStatus(id, "Resolved"));
                actions.addView(btnProgress);
                View gap1 = new View(this);
                gap1.setLayoutParams(new LinearLayout.LayoutParams(dp(8), 0));
                actions.addView(gap1);
                actions.addView(btnResolve);
            } else if ("In Progress".equalsIgnoreCase(status)) {
                MaterialButton btnResolve = makeActionBtn("✓ Resolve", "#16A34A", false);
                btnResolve.setOnClickListener(v -> confirmUpdateComplaintStatus(id, "Resolved"));
                actions.addView(btnResolve);
            } else if ("Resolved".equalsIgnoreCase(status)) {
                MaterialButton btnReopen = makeActionBtn("↻ Reopen", "#D97706", false);
                btnReopen.setOnClickListener(v -> confirmUpdateComplaintStatus(id, "Pending"));
                actions.addView(btnReopen);
            }

            inner.addView(actions);

            contentContainer.addView(card);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void confirmUpdateComplaintStatus(String id, String newStatus) {
        String msg = "Change complaint status to " + newStatus + "?";
        new android.app.AlertDialog.Builder(this)
                .setTitle("Update Status")
                .setMessage(msg)
                .setPositiveButton("Yes", (dlg, w) -> updateComplaintStatus(id, newStatus))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateComplaintStatus(String id, String status) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("status", status);
                patch("complaints", id, body);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Status updated to: " + status, Toast.LENGTH_SHORT).show();
                    fetchComplaints();
                    loadDashboardStats();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // =========================================================================
    // ── NOTICES ───────────────────────────────────────────────────────────────
    // =========================================================================
    private void fetchNotices() {
        showLoading(true);
        contentContainer.removeAllViews();

        new Thread(() -> {
            try {
                JSONArray arr = getArray(SupabaseClient.SUPABASE_URL
                        + "/rest/v1/notices?order=created_at.desc");

                runOnUiThread(() -> {
                    showLoading(false);
                    if (arr.length() == 0) { showEmpty("No notices yet"); return; }
                    for (int i = 0; i < arr.length(); i++)
                        buildNoticeCard(arr.optJSONObject(i));
                });
            } catch (Exception e) {
                runOnUiThread(() -> { showLoading(false); showEmpty("Error: " + e.getMessage()); });
            }
        }).start();
    }

    private void buildNoticeCard(JSONObject n) {
        if (n == null) return;
        try {
            String id        = n.optString("id", "");
            String title     = n.optString("title", "—");
            String body      = n.optString("body", "");
            String badge     = n.optString("badge_label", "🔔 Notice");
            String createdAt = n.optString("created_at", "");

            CardView card = makeCard();
            LinearLayout inner = makeInner(card);

            LinearLayout hdr = makeRow(0, dp(8));
            hdr.addView(makeIconCard("📢", Color.parseColor("#DCFCE7")));
            LinearLayout nameCol = makeNameCol(hdr);
            addBoldText(nameCol, title, "#1E293B", 13);
            addSmallText(nameCol, badge + "  ·  " + formatRelativeTime(createdAt), "#94A3B8");
            inner.addView(hdr);

            if (!body.isEmpty()) addSmallText(inner, body, "#64748B");
            inner.addView(makeDivider());

            LinearLayout actions = makeRow(dp(8), 0);
            MaterialButton btnEdit   = makeActionBtn("✏ Edit",   "#214177", false);
            MaterialButton btnDelete = makeActionBtn("🗑 Delete", "#EF4444", false);
            btnEdit.setOnClickListener(v   -> showEditNoticeDialog(id, title, body, badge));
            btnDelete.setOnClickListener(v -> confirmDeleteNotice(id));
            actions.addView(btnEdit);
            View gap = new View(this);
            gap.setLayoutParams(new LinearLayout.LayoutParams(dp(8), 0));
            actions.addView(gap);
            actions.addView(btnDelete);
            inner.addView(actions);
            contentContainer.addView(card);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAddNoticeDialog() { showNoticeDialog("Add Notice", "", "", "🔔 Notice", null); }
    private void showEditNoticeDialog(String id, String title, String body, String badge) {
        showNoticeDialog("Edit Notice", title, body, badge, id);
    }

    private void showNoticeDialog(String heading, String titleVal,
                                  String bodyVal, String badgeVal, String editId) {
        Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(R.layout.dialog_admin_notice);
        d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        d.getWindow().setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.92),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        d.setCancelable(true);

        setText(d, R.id.dialogNoticeHeading, heading);
        TextInputEditText etTitle = d.findViewById(R.id.etNoticeTitle);
        TextInputEditText etBody  = d.findViewById(R.id.etNoticeBody);
        AutoCompleteTextView ddBadge = d.findViewById(R.id.noticeBadgeDropdown);
        MaterialButton btnSave   = d.findViewById(R.id.btnSaveNotice);
        MaterialButton btnCancel = d.findViewById(R.id.btnCancelNotice);

        if (etTitle != null) etTitle.setText(titleVal);
        if (etBody  != null) etBody.setText(bodyVal);

        String[] badges = {"🔔 Notice", "⚠ Urgent", "📣 Event", "🔧 Maintenance", "💰 Payment"};
        if (ddBadge != null) {
            ddBadge.setAdapter(new ArrayAdapter<>(
                    this, android.R.layout.simple_dropdown_item_1line, badges));
            ddBadge.setText(badgeVal, false);
        }

        if (btnCancel != null) btnCancel.setOnClickListener(v -> d.dismiss());
        if (btnSave   != null) btnSave.setOnClickListener(v -> {
            String t  = etTitle != null ? etTitle.getText().toString().trim() : "";
            String b  = etBody  != null ? etBody.getText().toString().trim()  : "";
            String bg = ddBadge != null ? ddBadge.getText().toString().trim() : "🔔 Notice";

            if (t.isEmpty()) { if (etTitle != null) etTitle.setError("Required"); return; }

            btnSave.setEnabled(false);
            btnSave.setText("Saving…");

            new Thread(() -> {
                try {
                    JSONObject body = new JSONObject();
                    body.put("title",       t);
                    body.put("body",        b);
                    body.put("badge_label", bg);
                    body.put("sender",      "Admin");

                    if (editId != null) patch("notices", editId, body);
                    else                post("notices", body);

                    runOnUiThread(() -> {
                        d.dismiss();
                        Toast.makeText(this, "Notice saved!", Toast.LENGTH_SHORT).show();
                        fetchNotices();
                        loadDashboardStats();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("Save");
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        });

        d.show();
    }

    private void confirmDeleteNotice(String id) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete Notice")
                .setMessage("Are you sure you want to delete this notice?")
                .setPositiveButton("Delete", (dlg, w) -> deleteNotice(id))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteNotice(String id) {
        new Thread(() -> {
            try {
                String url = SupabaseClient.SUPABASE_URL + "/rest/v1/notices?id=eq." + id;
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Prefer", "return=minimal");
                conn.getResponseCode();
                conn.disconnect();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Notice deleted", Toast.LENGTH_SHORT).show();
                    fetchNotices();
                    loadDashboardStats();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // =========================================================================
    // ── USERS ─────────────────────────────────────────────────────────────────
    // =========================================================================
    private void fetchUsers() {
        showLoading(true);
        contentContainer.removeAllViews();

        new Thread(() -> {
            try {
                JSONArray arr = getArray(SupabaseClient.SUPABASE_URL
                        + "/rest/v1/users?order=apartment_number.asc&select=*");

                runOnUiThread(() -> {
                    showLoading(false);
                    if (arr.length() == 0) { showEmpty("No users found"); return; }
                    for (int i = 0; i < arr.length(); i++)
                        buildUserCard(arr.optJSONObject(i));
                });
            } catch (Exception e) {
                runOnUiThread(() -> { showLoading(false); showEmpty("Error: " + e.getMessage()); });
            }
        }).start();
    }

    private void buildUserCard(JSONObject u) {
        if (u == null) return;
        try {
            String id         = u.optString("id", "");
            String fullName   = u.optString("full_name", "—");
            String apartment  = u.optString("apartment_number", "—");
            String email      = u.optString("email", "—");
            String block      = u.optString("block", "");
            String rentAmount = u.optString("rent_amount", "");

            CardView card = makeCard();
            LinearLayout inner = makeInner(card);

            // Header with avatar
            LinearLayout hdr = makeRow(0, dp(12));

            CardView avatarCard = new CardView(this);
            avatarCard.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(44)));
            avatarCard.setRadius(dp(22));
            avatarCard.setCardElevation(0);
            avatarCard.setCardBackgroundColor(Color.parseColor("#214177"));
            TextView avatarTv = new TextView(this);
            avatarTv.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
            avatarTv.setText(initials(fullName));
            avatarTv.setTextColor(Color.WHITE);
            avatarTv.setTextSize(15);
            avatarTv.setTypeface(null, Typeface.BOLD);
            avatarCard.addView(avatarTv);
            hdr.addView(avatarCard);

            LinearLayout nameCol = makeNameCol(hdr);
            addBoldText(nameCol, fullName, "#1E293B", 13);
            addSmallText(nameCol,
                    "Unit " + apartment
                            + (block.isEmpty()      ? "" : "  ·  Block " + block)
                            + (rentAmount.isEmpty() ? "" : "  ·  LKR " + rentAmount),
                    "#94A3B8");

            // Unit badge
            CardView unitBadge = new CardView(this);
            unitBadge.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(26)));
            unitBadge.setRadius(dp(8));
            unitBadge.setCardElevation(0);
            unitBadge.setCardBackgroundColor(Color.parseColor("#EEF4FB"));
            TextView unitTv = new TextView(this);
            unitTv.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
            unitTv.setPadding(dp(8), 0, dp(8), 0);
            unitTv.setText(apartment);
            unitTv.setTextColor(Color.parseColor("#214177"));
            unitTv.setTextSize(10);
            unitTv.setTypeface(null, Typeface.BOLD);
            unitBadge.addView(unitTv);
            hdr.addView(unitBadge);

            inner.addView(hdr);
            addSmallText(inner, "✉  " + email, "#64748B");
            inner.addView(makeDivider());

            LinearLayout actions = makeRow(dp(8), 0);
            MaterialButton btnEdit   = makeActionBtn("✏  Edit",   "#214177", false);
            MaterialButton btnDelete = makeActionBtn("🗑  Delete", "#EF4444", false);
            btnEdit.setOnClickListener(v   -> showEditUserDialog(u));
            btnDelete.setOnClickListener(v -> confirmDeleteUser(id, fullName));
            actions.addView(btnEdit);
            View gap = new View(this);
            gap.setLayoutParams(new LinearLayout.LayoutParams(dp(8), 0));
            actions.addView(gap);
            actions.addView(btnDelete);
            inner.addView(actions);
            contentContainer.addView(card);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAddUserDialog()           { showUserDialog(null); }
    private void showEditUserDialog(JSONObject u) { showUserDialog(u); }

    private void showUserDialog(JSONObject existing) {
        boolean isEdit = existing != null;

        Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(R.layout.dialog_admin_user);
        d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        d.getWindow().setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.92),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        d.setCancelable(true);

        setText(d, R.id.dialogUserHeading, isEdit ? "Edit User" : "Add User");

        TextInputEditText etName  = d.findViewById(R.id.etUserName);
        TextInputEditText etApt   = d.findViewById(R.id.etUserApartment);
        TextInputEditText etBlock = d.findViewById(R.id.etUserBlock);
        TextInputEditText etEmail = d.findViewById(R.id.etUserEmail);
        TextInputEditText etRent  = d.findViewById(R.id.etUserRent);
        TextInputEditText etPwd   = d.findViewById(R.id.etUserPassword);

        View pwdLayout = d.findViewById(R.id.userPasswordLayout);
        if (pwdLayout != null) pwdLayout.setVisibility(isEdit ? View.GONE : View.VISIBLE);

        if (isEdit) {
            if (etName  != null) etName.setText(existing.optString("full_name", ""));
            if (etApt   != null) etApt.setText(existing.optString("apartment_number", ""));
            if (etBlock != null) etBlock.setText(existing.optString("block", ""));
            if (etEmail != null) {
                etEmail.setText(existing.optString("email", ""));
                etEmail.setEnabled(false); // email cannot be changed
            }
            if (etRent  != null) etRent.setText(existing.optString("rent_amount", ""));
        }

        MaterialButton btnSave   = d.findViewById(R.id.btnSaveUser);
        MaterialButton btnCancel = d.findViewById(R.id.btnCancelUser);
        if (btnCancel != null) btnCancel.setOnClickListener(v -> d.dismiss());

        if (btnSave != null) btnSave.setOnClickListener(v -> {
            String name  = etName  != null ? etName.getText().toString().trim()           : "";
            String apt   = etApt   != null ? etApt.getText().toString().trim().toUpperCase() : "";
            String blk   = etBlock != null ? etBlock.getText().toString().trim()          : "";
            String email = etEmail != null ? etEmail.getText().toString().trim()          : "";
            String rent  = etRent  != null ? etRent.getText().toString().trim()           : "0";
            String pwd   = etPwd   != null ? etPwd.getText().toString().trim()            : "";

            if (name.isEmpty())             { if (etName  != null) etName.setError("Required");  return; }
            if (apt.isEmpty())              { if (etApt   != null) etApt.setError("Required");   return; }
            if (!isEdit && email.isEmpty()) { if (etEmail != null) etEmail.setError("Required"); return; }
            if (!isEdit && pwd.isEmpty())   { if (etPwd   != null) etPwd.setError("Required");   return; }

            btnSave.setEnabled(false);
            btnSave.setText("Saving…");

            new Thread(() -> {
                try {
                    if (isEdit) {
                        // ── UPDATE: patch profile fields by user ID ──
                        JSONObject body = new JSONObject();
                        body.put("full_name",        name);
                        body.put("apartment_number", apt);
                        body.put("block",            blk);
                        body.put("rent_amount",      rent.isEmpty() ? 0 : Integer.parseInt(rent));
                        patch("users", existing.optString("id"), body);

                        // Refresh local cache
                        usersMap.remove(existing.optString("id"));

                        runOnUiThread(() -> {
                            d.dismiss();
                            Toast.makeText(this, "User updated!", Toast.LENGTH_SHORT).show();
                            fetchUsers();
                        });

                    } else {
                        // ── CREATE: Supabase Auth signup → insert into users table ──
                        String authUrl = SupabaseClient.SUPABASE_URL + "/auth/v1/signup";
                        JSONObject authBody = new JSONObject();
                        authBody.put("email",    email);
                        authBody.put("password", pwd);

                        HttpURLConnection ac = (HttpURLConnection)
                                new URL(authUrl).openConnection();
                        ac.setRequestMethod("POST");
                        ac.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                        ac.setRequestProperty("Content-Type", "application/json");
                        ac.setDoOutput(true);
                        try (OutputStream os = ac.getOutputStream()) {
                            os.write(authBody.toString().getBytes(StandardCharsets.UTF_8));
                        }

                        int code = ac.getResponseCode();
                        String resp = readBody(ac);
                        ac.disconnect();

                        if (code == 200 || code == 201) {
                            JSONObject respObj = new JSONObject(resp);
                            JSONObject userNode = respObj.optJSONObject("user");
                            String newId = userNode != null
                                    ? userNode.optString("id", "")
                                    : respObj.optString("id", "");

                            JSONObject userBody = new JSONObject();
                            userBody.put("id",               newId);
                            userBody.put("full_name",        name);
                            userBody.put("apartment_number", apt);
                            userBody.put("block",            blk);
                            userBody.put("email",            email);
                            userBody.put("rent_amount",
                                    rent.isEmpty() ? 0 : Integer.parseInt(rent));
                            post("users", userBody);

                            runOnUiThread(() -> {
                                d.dismiss();
                                Toast.makeText(this, "User created!", Toast.LENGTH_SHORT).show();
                                fetchUsers();
                                loadDashboardStats();
                            });
                        } else {
                            JSONObject errObj = new JSONObject(resp);
                            String err = errObj.optString("msg",
                                    errObj.optString("message", "Signup failed"));
                            runOnUiThread(() -> {
                                btnSave.setEnabled(true);
                                btnSave.setText("Save");
                                Toast.makeText(this, "Auth error: " + err, Toast.LENGTH_LONG).show();
                            });
                        }
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("Save");
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        });

        d.show();
    }

    private void confirmDeleteUser(String id, String name) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("Delete " + name + "? This removes them from the system.")
                .setPositiveButton("Delete", (dlg, w) -> deleteUser(id))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUser(String id) {
        new Thread(() -> {
            try {
                String url = SupabaseClient.SUPABASE_URL + "/rest/v1/users?id=eq." + id;
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Prefer", "return=minimal");
                conn.getResponseCode();
                conn.disconnect();
                runOnUiThread(() -> {
                    Toast.makeText(this, "User deleted", Toast.LENGTH_SHORT).show();
                    fetchUsers();
                    loadDashboardStats();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // =========================================================================
    // ── UI BUILDERS ───────────────────────────────────────────────────────────
    // =========================================================================
    private CardView makeCard() {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(16), 0, dp(16), dp(12));
        card.setLayoutParams(lp);
        card.setRadius(dp(20));
        card.setCardElevation(dp(1));
        card.setCardBackgroundColor(Color.WHITE);
        return card;
    }

    private LinearLayout makeInner(CardView card) {
        LinearLayout inner = new LinearLayout(this);
        inner.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.addView(inner);
        return inner;
    }

    private LinearLayout makeRow(int topMargin, int bottomMargin) {
        LinearLayout row = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, topMargin, 0, bottomMargin);
        row.setLayoutParams(lp);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private LinearLayout.LayoutParams createMarginLP(int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(l, t, r, b);
        return lp;
    }

    private CardView makeIconCard(String emoji, int bgColor) {
        CardView c = new CardView(this);
        c.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(44)));
        c.setRadius(dp(14));
        c.setCardElevation(0);
        c.setCardBackgroundColor(bgColor);
        TextView tv = new TextView(this);
        tv.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        tv.setText(emoji);
        tv.setTextSize(18);
        c.addView(tv);
        return c;
    }

    private LinearLayout makeNameCol(LinearLayout parent) {
        LinearLayout col = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMarginStart(dp(12));
        col.setLayoutParams(lp);
        col.setOrientation(LinearLayout.VERTICAL);
        parent.addView(col);
        return col;
    }

    private void addBoldText(LinearLayout parent, String text, String hex, int sp) {
        TextView tv = new TextView(this);
        tv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        tv.setText(text);
        tv.setTextColor(Color.parseColor(hex));
        tv.setTextSize(sp);
        tv.setTypeface(null, Typeface.BOLD);
        parent.addView(tv);
    }

    private void addSmallText(LinearLayout parent, String text, String hex) {
        TextView tv = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(3), 0, 0);
        tv.setLayoutParams(lp);
        tv.setText(text);
        tv.setTextColor(Color.parseColor(hex));
        tv.setTextSize(11);
        tv.setMaxLines(3);
        tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        parent.addView(tv);
    }

    /** Small rounded pill chip — used for date/time/guest info in reservation cards */
    private void addInfoChip(LinearLayout parent, String label) {
        CardView chip = new CardView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(26));
        lp.setMarginEnd(dp(6));
        chip.setLayoutParams(lp);
        chip.setRadius(dp(13));
        chip.setCardElevation(0);
        chip.setCardBackgroundColor(Color.parseColor("#F1F5F9"));
        TextView tv = new TextView(this);
        tv.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        tv.setPadding(dp(10), 0, dp(10), 0);
        tv.setText(label);
        tv.setTextSize(10);
        tv.setTextColor(Color.parseColor("#475569"));
        chip.addView(tv);
        parent.addView(chip);
    }

    private CardView makeStatusBadge(String status) {
        CardView badge = new CardView(this);
        badge.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(26)));
        badge.setRadius(dp(13));
        badge.setCardElevation(0);
        badge.setCardBackgroundColor(statusBg(status));
        TextView tv = new TextView(this);
        tv.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        tv.setPadding(dp(10), 0, dp(10), 0);
        tv.setText(status);
        tv.setTextColor(statusFg(status));
        tv.setTextSize(10);
        tv.setTypeface(null, Typeface.BOLD);
        badge.addView(tv);
        return badge;
    }

    private View makeDivider() {
        View v = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        lp.setMargins(0, dp(10), 0, dp(4));
        v.setLayoutParams(lp);
        v.setBackgroundColor(Color.parseColor("#F1F5F9"));
        return v;
    }

    private MaterialButton makeActionBtn(String label, String hexColor, boolean filled) {
        MaterialButton btn = new MaterialButton(this,
                null, filled
                ? com.google.android.material.R.attr.materialButtonStyle
                : com.google.android.material.R.attr.materialButtonOutlinedStyle);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(36), 1f);
        btn.setLayoutParams(lp);
        btn.setText(label);
        btn.setTextSize(11);
        btn.setCornerRadius(dp(18));
        btn.setPadding(dp(8), 0, dp(8), 0);
        btn.setInsetTop(0);
        btn.setInsetBottom(0);
        if (filled) {
            btn.setBackgroundColor(Color.parseColor(hexColor));
            btn.setTextColor(Color.WHITE);
        } else {
            btn.setBackgroundColor(Color.TRANSPARENT);
            btn.setTextColor(Color.parseColor(hexColor));
            btn.setStrokeColor(android.content.res.ColorStateList.valueOf(
                    Color.parseColor("#E2E8F0")));
        }
        return btn;
    }

    private void showEmpty(String msg) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(16), 0, dp(16), dp(12));
        card.setLayoutParams(lp);
        card.setRadius(dp(20));
        card.setCardElevation(0);
        card.setCardBackgroundColor(Color.WHITE);
        LinearLayout inner = new LinearLayout(this);
        inner.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER);
        inner.setPadding(dp(32), dp(40), dp(32), dp(40));
        TextView tv = new TextView(this);
        tv.setText("📭\n\n" + msg);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(Color.parseColor("#94A3B8"));
        tv.setTextSize(13);
        tv.setLineSpacing(dp(4), 1f);
        inner.addView(tv);
        card.addView(inner);
        contentContainer.addView(card);
    }

    private void showLoading(boolean show) {
        if (loadingBar != null)
            loadingBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // =========================================================================
    // ── SUPABASE HELPERS ──────────────────────────────────────────────────────
    // =========================================================================
    private JSONArray getArray(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("apikey",        SupabaseClient.SUPABASE_ANON_KEY);
        conn.setRequestProperty("Authorization", "Bearer " + token);
        String body = readBody(conn);
        conn.disconnect();
        // Supabase returns either array or error object
        if (body.trim().startsWith("[")) return new JSONArray(body);
        // If it returned an error object, return empty array (avoid crash)
        return new JSONArray();
    }

    private void patch(String table, String id, JSONObject body) throws Exception {
        String url = SupabaseClient.SUPABASE_URL + "/rest/v1/" + table + "?id=eq." + id;
        Log.d("AdminActivity", "PATCH URL: " + url);
        Log.d("AdminActivity", "PATCH Body: " + body.toString());

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("PATCH");
        conn.setRequestProperty("apikey",        SupabaseClient.SUPABASE_ANON_KEY);
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Content-Type",  "application/json");
        conn.setRequestProperty("Prefer",        "return=minimal");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
        int responseCode = conn.getResponseCode();
        Log.d("AdminActivity", "PATCH Response Code: " + responseCode);
        conn.disconnect();
    }

    private void post(String table, JSONObject body) throws Exception {
        String url = SupabaseClient.SUPABASE_URL + "/rest/v1/" + table;
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("apikey",        SupabaseClient.SUPABASE_ANON_KEY);
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Content-Type",  "application/json");
        conn.setRequestProperty("Prefer",        "return=minimal");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
        conn.getResponseCode();
        conn.disconnect();
    }

    private int countTable(String table, String col, String val) {
        try {
            StringBuilder url = new StringBuilder(
                    SupabaseClient.SUPABASE_URL + "/rest/v1/" + table + "?select=id");
            if (col != null && val != null)
                url.append("&").append(col).append("=eq.").append(val);
            return getArray(url.toString()).length();
        } catch (Exception e) { return 0; }
    }

    private String readBody(HttpURLConnection conn) throws Exception {
        java.io.InputStream is;
        try { is = conn.getInputStream(); }
        catch (Exception e) { is = conn.getErrorStream(); }
        if (is == null) return "{}";
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) sb.append(line);
        r.close();
        return sb.toString();
    }

    // =========================================================================
    // ── MISC HELPERS ──────────────────────────────────────────────────────────
    // =========================================================================
    private void setText(Dialog d, int viewId, String text) {
        TextView tv = d.findViewById(viewId);
        if (tv != null) tv.setText(text);
    }

    private void setTextSafe(int viewId, String text) {
        TextView tv = findViewById(viewId);
        if (tv != null) tv.setText(text);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private String extractUserIdFromToken(String tkn) {
        try {
            String[] p = tkn.split("\\.");
            if (p.length < 2) return "";
            String payload = new String(
                    Base64.decode(p[1], Base64.URL_SAFE), StandardCharsets.UTF_8);
            return new JSONObject(payload).optString("sub", "");
        } catch (Exception e) { return ""; }
    }

    private String formatRelativeTime(String iso) {
        if (iso == null || iso.isEmpty()) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d = sdf.parse(iso.length() >= 19 ? iso.substring(0, 19) : iso);
            if (d == null) return "";
            long diff = (System.currentTimeMillis() - d.getTime()) / 1000;
            if (diff < 60)     return "Just now";
            if (diff < 3600)   return (diff / 60) + "m ago";
            if (diff < 86400)  return (diff / 3600) + "h ago";
            if (diff < 172800) return "Yesterday";
            return (diff / 86400) + "d ago";
        } catch (Exception e) { return ""; }
    }

    private String initials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] p = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        if (p.length > 0 && !p[0].isEmpty())
            sb.append(Character.toUpperCase(p[0].charAt(0)));
        if (p.length > 1 && !p[1].isEmpty())
            sb.append(Character.toUpperCase(p[1].charAt(0)));
        return sb.length() > 0 ? sb.toString() : "?";
    }

    private String amenityEmoji(String type) {
        if (type == null) return "📅";
        switch (type) {
            case "Pool":       return "🏊";
            case "Gym":        return "💪";
            case "Restaurant": return "🍽";
            default:           return "📅";
        }
    }

    private int amenityBg(String type) {
        if (type == null) return Color.parseColor("#EDE9FE");
        switch (type) {
            case "Pool":       return Color.parseColor("#DBEAFE");
            case "Gym":        return Color.parseColor("#DCFCE7");
            case "Restaurant": return Color.parseColor("#FEF3C7");
            default:           return Color.parseColor("#EDE9FE");
        }
    }

    private String categoryEmoji(String cat) {
        if (cat == null) return "📋";
        switch (cat.toLowerCase()) {
            case "noise":    return "🔊";
            case "parking":  return "🚗";
            case "cleaning": return "🧹";
            case "security": return "🔒";
            default:         return "📋";
        }
    }

    private int statusBg(String s) {
        if (s == null) return Color.parseColor("#F1F5F9");
        switch (s.toLowerCase()) {
            case "confirmed":
            case "accepted":
            case "resolved":    return Color.parseColor("#DCFCE7");
            case "pending":     return Color.parseColor("#DBEAFE");
            case "in progress": return Color.parseColor("#FEF3C7");
            case "cancelled":   return Color.parseColor("#FFE4E6");
            case "expired":     return Color.parseColor("#F3E8FF");
            default:            return Color.parseColor("#F1F5F9");
        }
    }

    private int statusFg(String s) {
        if (s == null) return Color.parseColor("#64748B");
        switch (s.toLowerCase()) {
            case "confirmed":
            case "accepted":
            case "resolved":    return Color.parseColor("#16A34A");
            case "pending":     return Color.parseColor("#1D4ED8");
            case "in progress": return Color.parseColor("#D97706");
            case "cancelled":   return Color.parseColor("#E11D48");
            case "expired":     return Color.parseColor("#7C3AED");
            default:            return Color.parseColor("#64748B");
        }
    }
}
