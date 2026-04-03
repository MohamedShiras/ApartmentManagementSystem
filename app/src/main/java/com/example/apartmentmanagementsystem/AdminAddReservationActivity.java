package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.UUID;


public class AdminAddReservationActivity extends AppCompatActivity {

    private CardView btnBack;
    private CardView servicePool, serviceGym, serviceRestaurant;
    private EditText descriptionInput, timePeriodInput, maxGuestsInput;
    private CardView submitButton;
    private TextView selectedServiceText;
    private String selectedService = "Swimming Pool";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_reservation);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        servicePool = findViewById(R.id.servicePool);
        serviceGym = findViewById(R.id.serviceGym);
        serviceRestaurant = findViewById(R.id.serviceRestaurant);
        selectedServiceText = findViewById(R.id.selectedServiceText);
        descriptionInput = findViewById(R.id.descriptionInput);
        timePeriodInput = findViewById(R.id.timePeriodInput);
        maxGuestsInput = findViewById(R.id.maxGuestsInput);
        submitButton = findViewById(R.id.submitButton);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Service selection
        servicePool.setOnClickListener(v -> selectService("Swimming Pool"));
        serviceGym.setOnClickListener(v -> selectService("Fitness Center"));
        serviceRestaurant.setOnClickListener(v -> selectService("Restaurant & Dining"));

        // Submit button
        submitButton.setOnClickListener(v -> submitReservation());
    }

    private void selectService(String serviceName) {
        selectedService = serviceName;
        selectedServiceText.setText("Selected: " + serviceName);
        Toast.makeText(this, "Selected: " + serviceName, Toast.LENGTH_SHORT).show();
    }

    private void submitReservation() {
        String description = descriptionInput.getText().toString().trim();
        String timePeriod = timePeriodInput.getText().toString().trim();
        String maxGuests = maxGuestsInput.getText().toString().trim();

        if (description.isEmpty() || timePeriod.isEmpty() || maxGuests.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading indicator
        Toast.makeText(this, "Saving reservation...", Toast.LENGTH_SHORT).show();

        // Create reservation and save
        new Thread(() -> {
            try {
                AdminReservation reservation = new AdminReservation(
                        UUID.randomUUID().toString(),
                        selectedService,
                        description,
                        timePeriod,
                        maxGuests
                );

                addReservationToSupabase(reservation);
            } catch (Exception e) {
                Log.e("AdminAddReservation", "Error:", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void addReservationToSupabase(AdminReservation reservation) {
        new Thread(() -> {
            try {
                // Create JSON payload for Supabase
                JSONObject payload = new JSONObject();
                payload.put("id", reservation.getId());
                payload.put("service_name", reservation.getServiceName());
                payload.put("description", reservation.getDescription());
                payload.put("time_period", reservation.getTimePeriod());
                payload.put("max_guests", reservation.getMaxGuests());
                payload.put("created_at", System.currentTimeMillis());

                // Send to Supabase REST API
                String apiUrl = SupabaseClient.SUPABASE_URL + "/rest/v1/add_reservation_services";

                HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);

                String userToken = getAccessToken();
                if (userToken != null && !userToken.isEmpty()) {
                    connection.setRequestProperty("Authorization", "Bearer " + userToken);
                }

                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                // Write payload
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(payload.toString().getBytes("utf-8"));
                    os.flush();
                }

                // Get response
                int responseCode = connection.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        responseCode >= 200 && responseCode < 300 ?
                        connection.getInputStream() : connection.getErrorStream()
                ));

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                connection.disconnect();

                // Handle response
                if (responseCode >= 200 && responseCode < 300) {
                    Log.d("AdminAddReservation", "Reservation saved successfully");
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Reservation added successfully!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                } else {
                    Log.e("AdminAddReservation", "Error: " + response.toString());
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error: " + response.toString(), Toast.LENGTH_LONG).show();
                    });
                }

            } catch (Exception e) {
                Log.e("AdminAddReservation", "Exception:", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private String getBookedByUser() {
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        return prefs.getString("user_email", "admin");
    }

    /**
     * Get the user's access token from SharedPreferences
     * Returns null/empty if no token is available
     */
    private String getAccessToken() {
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String token = prefs.getString("access_token", "");
        return (token == null || token.trim().isEmpty()) ? null : token;
    }

    /**
     * Legacy method for compatibility - returns Bearer token only if user is authenticated
     * WARNING: Do not use this for apikey header - use getAccessToken() instead
     */
    private String getBearerToken() {
        String token = getAccessToken();
        if (token != null && !token.isEmpty()) {
            return token;
        }
        // Return empty string if no user token (caller should not add Authorization header)
        return "";
    }
}

