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
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MaintenanceActivity extends AppCompatActivity {

    private LinearLayout requestsContainer;
    private ProgressBar requestsLoading;
    private CardView emptyRequestsCard;

    private String currentApartmentNumber = "";
    private String userId = "";
    private String token = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maintenance);

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.activity_maintenance),
                (v, insets) -> {
                    Insets bars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top,
                            bars.right, bars.bottom);
                    return insets;
                });

        requestsContainer = findViewById(R.id.requestsDynamicContainer);
        requestsLoading = findViewById(R.id.requestsLoading);
        emptyRequestsCard = findViewById(R.id.emptyRequestsCard);

        loadSession();
    }

    // =========================================================
    // SESSION
    // =========================================================
    private void loadSession() {

        SharedPreferences prefs =
                getSharedPreferences("LoginPrefs", MODE_PRIVATE);

        token = prefs.getString("access_token", null);
        userId = prefs.getString("user_id", null);

        if (token == null || userId == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        loadApartmentNumber();
    }

    // =========================================================
    // LOAD APARTMENT NUMBER (CORRECT USER)
    // =========================================================
    private void loadApartmentNumber() {

        new Thread(() -> {
            try {

                String url =
                        SupabaseClient.SUPABASE_URL +
                                "/rest/v1/users" +
                                "?id=eq." + userId +
                                "&select=apartment_number";

                HttpURLConnection conn =
                        (HttpURLConnection) new URL(url).openConnection();

                conn.setRequestProperty("apikey",
                        SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization",
                        "Bearer " + token);

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        conn.getInputStream()));

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null)
                    sb.append(line);

                reader.close();
                conn.disconnect();

                JSONArray arr = new JSONArray(sb.toString());

                if (arr.length() > 0) {
                    currentApartmentNumber =
                            arr.getJSONObject(0)
                                    .optString("apartment_number", "");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            runOnUiThread(this::loadMyRequests);

        }).start();
    }

    // =========================================================
    // LOAD ONLY MY REQUESTS
    // =========================================================
    private void loadMyRequests() {

        requestsLoading.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {

                String queryUrl =
                        SupabaseClient.SUPABASE_URL +
                                "/rest/v1/maintenance_requests" +
                                "?user_id=eq." + userId +
                                "&order=created_at.desc";

                HttpURLConnection conn =
                        (HttpURLConnection) new URL(queryUrl).openConnection();

                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey",
                        SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization",
                        "Bearer " + token);

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        conn.getInputStream()));

                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null)
                    sb.append(line);

                reader.close();
                conn.disconnect();

                JSONArray requests =
                        new JSONArray(sb.toString());

                runOnUiThread(() -> {

                    requestsLoading.setVisibility(View.GONE);
                    requestsContainer.removeAllViews();

                    if (requests.length() == 0) {
                        emptyRequestsCard.setVisibility(View.VISIBLE);
                        return;
                    }

                    emptyRequestsCard.setVisibility(View.GONE);

                    for (int i = 0; i < requests.length(); i++) {
                        buildSimpleCard(requests.optJSONObject(i));
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

    // =========================================================
    // SIMPLE REQUEST CARD
    // =========================================================
    private void buildSimpleCard(JSONObject req) {

        try {

            String title = req.optString("title");
            String status = req.optString("status");
            String desc = req.optString("description");

            CardView card = new CardView(this);
            card.setRadius(20);
            card.setCardBackgroundColor(Color.WHITE);

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);

            params.setMargins(32, 0, 32, 24);
            card.setLayoutParams(params);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(32, 32, 32, 32);

            TextView tvTitle = new TextView(this);
            tvTitle.setText(title);
            tvTitle.setTextSize(16);
            tvTitle.setTextColor(Color.BLACK);

            TextView tvDesc = new TextView(this);
            tvDesc.setText(desc);
            tvDesc.setTextColor(Color.GRAY);

            TextView tvStatus = new TextView(this);
            tvStatus.setText(status);
            tvStatus.setTextColor(Color.parseColor("#3667A6"));

            layout.addView(tvTitle);
            layout.addView(tvDesc);
            layout.addView(tvStatus);

            card.addView(layout);
            requestsContainer.addView(card);

        } catch (Exception ignored) {}
    }

    // =========================================================
    // SUBMIT REQUEST
    // =========================================================
    private void submitRequest(
            String serviceType,
            String issue,
            String description,
            String priority,
            Dialog dialog,
            MaterialButton btnSubmit) {

        new Thread(() -> {
            try {

                JSONObject body = new JSONObject();

                body.put("user_id", userId);
                body.put("apartment_number", currentApartmentNumber);
                body.put("service_type", serviceType);
                body.put("title", issue);
                body.put("description", description);
                body.put("priority", priority);
                body.put("status", "Under Review");

                HttpURLConnection conn =
                        (HttpURLConnection) new URL(
                                SupabaseClient.SUPABASE_URL +
                                        "/rest/v1/maintenance_requests")
                                .openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey",
                        SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization",
                        "Bearer " + token);
                conn.setRequestProperty("Content-Type",
                        "application/json");

                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString()
                        .getBytes(StandardCharsets.UTF_8));
                os.close();

                conn.getResponseCode();
                conn.disconnect();

                runOnUiThread(() -> {
                    dialog.dismiss();
                    loadMyRequests();
                    Toast.makeText(this,
                            "Request submitted",
                            Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {

                runOnUiThread(() -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Submit Request");
                });
            }
        }).start();
    }

    // =========================================================
    // JWT USER ID EXTRACTION (SAFE)
    // =========================================================
    private String extractUserIdFromToken(String token) {

        try {
            String[] parts = token.split("\\.");

            if (parts.length < 2) return "";

            String payload =
                    new String(
                            Base64.decode(parts[1],
                                    Base64.URL_SAFE),
                            StandardCharsets.UTF_8);

            JSONObject json = new JSONObject(payload);

            return json.optString("sub", "");

        } catch (Exception e) {
            return "";
        }
    }
}