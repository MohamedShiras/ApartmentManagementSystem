package com.example.apartmentmanagementsystem;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ReservationHistoryActivity extends AppCompatActivity {

    private LinearLayout llContainer;
    private LinearLayout layoutEmptyState;
    private ProgressBar progressLoading;

    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation_history);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        llContainer      = findViewById(R.id.llReservationsContainer);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        progressLoading  = findViewById(R.id.progressLoading);

        android.content.SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        currentUserId = prefs.getString("user_id", null);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadReservations();
    }

    // ── Fetch reservations from Supabase ──────────────────────
    private void loadReservations() {
        if (currentUserId == null) {
            showEmpty();
            return;
        }

        new Thread(() -> {
            try {
                String endpoint = SupabaseClient.SUPABASE_URL
                        + "/rest/v1/reservations?user_id=eq." + currentUserId
                        + "&order=created_at.desc&select=*";

                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey",        SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Content-Type",  "application/json");

                int code = conn.getResponseCode();
                InputStream is = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
                String body = new Scanner(is).useDelimiter("\\A").next();
                conn.disconnect();

                if (code == 200) {
                    JSONArray arr = new JSONArray(body);
                    runOnUiThread(() -> {
                        progressLoading.setVisibility(View.GONE);
                        if (arr.length() == 0) {
                            showEmpty();
                        } else {
                            layoutEmptyState.setVisibility(View.GONE);
                            for (int i = 0; i < arr.length(); i++) {
                                try {
                                    addCard(arr.getJSONObject(i));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        progressLoading.setVisibility(View.GONE);
                        Toast.makeText(this, "Error loading: " + body, Toast.LENGTH_LONG).show();
                        showEmpty();
                    });
                }

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "❌ " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showEmpty();
                });
            }
        }).start();
    }

    // ── Build one card from JSON ──────────────────────────────
    private void addCard(JSONObject obj) throws Exception {
        String recordId    = obj.getString("id");
        String amenityType = obj.optString("amenity_type", "");
        String date        = obj.optString("selected_date", "");
        String timeSlot    = obj.optString("time_slot", "");
        int    guests      = obj.optInt("guest_count", 1);

        View card = LayoutInflater.from(this)
                .inflate(R.layout.item_reservation_card, llContainer, false);

        // Image
        ImageView ivImg = card.findViewById(R.id.ivAmenityImage);
        ivImg.setImageResource(getAmenityImage(amenityType));

        // Amenity name
        TextView tvName = card.findViewById(R.id.tvAmenityName);
        tvName.setText(getAmenityEmoji(amenityType) + " " + amenityType);

        // Time slot
        TextView tvTime = card.findViewById(R.id.tvTimeSlot);
        tvTime.setText(timeSlot);

        // Date + guests
        TextView tvDate = card.findViewById(R.id.tvDateGuests);
        tvDate.setText(date + "  ·  " + guests + (guests == 1 ? " Guest" : " Guests"));

        // Cancel button
        MaterialButton btnCancel = card.findViewById(R.id.btnCancelCard);
        btnCancel.setOnClickListener(v ->
                showCancelDialog(amenityType, date, timeSlot, recordId, card));

        llContainer.addView(card);
    }

    // ── Cancel dialog ─────────────────────────────────────────
    private void showCancelDialog(String amenity, String date, String time,
                                  String recordId, View card) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Reservation?")
                .setMessage("Are you sure you want to cancel your "
                        + amenity + " reservation?\n\n📅 " + date + "  ·  " + time)
                .setPositiveButton("Yes, Cancel", (dialog, which) ->
                        deleteReservation(recordId, card, amenity))
                .setNegativeButton("Keep It", null)
                .show();
    }

    // ── DELETE from Supabase ──────────────────────────────────
    private void deleteReservation(String recordId, View card, String amenity) {
        new Thread(() -> {
            try {
                String endpoint = SupabaseClient.SUPABASE_URL
                        + "/rest/v1/reservations?id=eq." + recordId;

                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("apikey",        SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Prefer",        "return=minimal");

                int code = conn.getResponseCode();
                conn.disconnect();

                runOnUiThread(() -> {
                    if (code == 200 || code == 204) {
                        // Remove card from UI
                        llContainer.removeView(card);
                        Toast.makeText(this,
                                "✅ " + amenity + " reservation cancelled.",
                                Toast.LENGTH_SHORT).show();

                        // Show empty state if no cards left
                        if (llContainer.getChildCount() == 0) showEmpty();

                    } else {
                        Toast.makeText(this,
                                "❌ Failed to cancel. Try again.",
                                Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "❌ " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // ── Helpers ───────────────────────────────────────────────
    private void showEmpty() {
        progressLoading.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);
    }

    private int getAmenityImage(String type) {
        if (type == null) return R.drawable.img_pool;
        switch (type) {
            case "Gym":        return R.drawable.img_gym;
            case "Restaurant": return R.drawable.img_restaurant;
            default:           return R.drawable.img_pool;
        }
    }

    private String getAmenityEmoji(String type) {
        if (type == null) return "🏊";
        switch (type) {
            case "Gym":        return "🏋️";
            case "Restaurant": return "🍽️";
            default:           return "🏊";
        }
    }
}