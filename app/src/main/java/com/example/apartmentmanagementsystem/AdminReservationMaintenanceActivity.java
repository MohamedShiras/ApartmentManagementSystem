package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class AdminReservationMaintenanceActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView emptyText;
    private TextView loadingText;
    private AdminReservationMaintenanceAdapter adapter;
    private final List<AdminReservation> reservations = new ArrayList<>();

    private final ActivityResultLauncher<Intent> editLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                fetchReservations();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_reservation_maintenance);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setupToolbar();
        setupList();
        fetchReservations();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.adminReservationToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menuAddReservation) {
                editLauncher.launch(new Intent(this, AdminEditReservationActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.menuRefreshReservations) {
                fetchReservations();
                return true;
            }
            return false;
        });
    }

    private void setupList() {
        recyclerView = findViewById(R.id.adminReservationRecycler);
        emptyText = findViewById(R.id.adminReservationEmptyText);
        loadingText = findViewById(R.id.adminReservationLoadingText);

        adapter = new AdminReservationMaintenanceAdapter(new AdminReservationMaintenanceAdapter.ReservationActionListener() {
            @Override
            public void onEdit(AdminReservation reservation) {
                Intent intent = new Intent(AdminReservationMaintenanceActivity.this, AdminEditReservationActivity.class);
                intent.putExtra("reservation_id", reservation.getId());
                intent.putExtra("service_name", reservation.getServiceName());
                intent.putExtra("description", reservation.getDescription());
                intent.putExtra("reservation_date", reservation.getReservationDate());
                intent.putExtra("reservation_time", reservation.getReservationTime());
                intent.putExtra("duration", reservation.getDuration());
                intent.putExtra("image_url", reservation.getImageUrl());
                intent.putExtra("status", reservation.getStatus());
                intent.putExtra("booked_by", reservation.getBookedBy());
                editLauncher.launch(intent);
            }

            @Override
            public void onDelete(AdminReservation reservation) {
                confirmDelete(reservation);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void fetchReservations() {
        setLoadingState(true);

        new Thread(() -> {
            try {
                String queryUrl = SupabaseClient.SUPABASE_URL
                        + "/rest/v1/reservations"
                        + "?select=*"
                        + "&order=created_at.desc.nullslast";

                HttpURLConnection connection = (HttpURLConnection) new URL(queryUrl).openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                connection.setRequestProperty("Authorization", "Bearer " + getBearerToken());
                connection.setRequestProperty("Content-Type", "application/json");

                int code = connection.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream()
                ));

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                connection.disconnect();

                if (code < 200 || code >= 300) {
                    Log.e("AdminReservationMaint", "Fetch failed: " + sb);
                    showError("Failed to load reservations: " + sb);
                    return;
                }

                JSONArray array = new JSONArray(sb.toString());
                Log.d("AdminReservationMaint", "Loaded " + array.length() + " reservations");

                List<AdminReservation> loaded = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);

                    String imgUrl = valueOf(obj, "image_url", "reservation_image_url", "image", "imageUrl");
                    loaded.add(new AdminReservation(
                            valueOf(obj, "id", "reservation_id"),
                            valueOf(obj, "service_name", "amenity", "title", "service"),
                            valueOf(obj, "description", "special_request", "notes"),
                            valueOf(obj, "reservation_date", "date", "booking_date"),
                            valueOf(obj, "reservation_time", "time_slot", "time"),
                            valueOf(obj, "duration", "duration_text"),
                            imgUrl,
                            valueOf(obj, "status"),
                            valueOf(obj, "booked_by", "resident_name", "full_name", "apartment_number"),
                            valueOf(obj, "capacity", "max_guests", "guest_count")
                    ));
                }

                runOnUiThread(() -> {
                    reservations.clear();
                    reservations.addAll(loaded);
                    adapter.submitList(reservations);
                    setLoadingState(false);
                    showEmptyState(reservations.isEmpty(), getString(R.string.admin_reservation_empty));
                });
            } catch (Exception e) {
                Log.e("AdminReservationMaint", "Exception:", e);
                showError("Error loading reservations: " + e.getMessage());
            }
        }).start();
    }

    private void confirmDelete(AdminReservation reservation) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Reservation")
                .setMessage("Delete this reservation record?")
                .setPositiveButton("Delete", (dialog, which) -> deleteReservation(reservation.getId()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteReservation(String reservationId) {
        setLoadingState(true);

        new Thread(() -> {
            try {
                String encodedId = URLEncoder.encode(reservationId, "UTF-8");
                String url = SupabaseClient.SUPABASE_URL
                        + "/rest/v1/reservations?id=eq." + encodedId;

                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("DELETE");
                connection.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                connection.setRequestProperty("Authorization", "Bearer " + getBearerToken());
                connection.setRequestProperty("Prefer", "return=minimal");

                int code = connection.getResponseCode();
                connection.disconnect();

                runOnUiThread(() -> {
                    setLoadingState(false);
                    if (code >= 200 && code < 300) {
                        Toast.makeText(this, "Reservation deleted", Toast.LENGTH_SHORT).show();
                        fetchReservations();
                    } else {
                        Toast.makeText(this, "Delete failed. Check Supabase RLS policy.", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoadingState(false);
                    Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private String valueOf(JSONObject obj, String... keys) {
        for (String key : keys) {
            if (obj.has(key) && !obj.isNull(key)) {
                String value = obj.optString(key, "").trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return "";
    }

    private String getBearerToken() {
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String token = prefs.getString("access_token", "");
        if (token == null || token.trim().isEmpty()) {
            return SupabaseClient.SUPABASE_ANON_KEY;
        }
        return token;
    }

    private void setLoadingState(boolean loading) {
        runOnUiThread(() -> {
            loadingText.setVisibility(loading ? TextView.VISIBLE : TextView.GONE);
            recyclerView.setVisibility(loading ? TextView.INVISIBLE : TextView.VISIBLE);
            if (loading) {
                emptyText.setVisibility(TextView.GONE);
            }
            recyclerView.setEnabled(!loading);
        });
    }

    private void showEmptyState(boolean show, String message) {
        emptyText.setText(message);
        emptyText.setVisibility(show ? TextView.VISIBLE : TextView.GONE);
        recyclerView.setVisibility(show ? TextView.INVISIBLE : TextView.VISIBLE);
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            setLoadingState(false);
            showEmptyState(true, message);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }
}
