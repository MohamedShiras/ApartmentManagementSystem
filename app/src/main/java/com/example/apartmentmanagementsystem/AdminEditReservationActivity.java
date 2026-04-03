package com.example.apartmentmanagementsystem;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class AdminEditReservationActivity extends AppCompatActivity {

    private String reservationId;
    private EditText serviceInput;
    private EditText descriptionInput;
    private EditText timePeriodInput;
    private EditText maxGuestsInput;
    private MaterialButton saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_edit_reservation);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        bindViews();
        bindToolbar();
        bindDataFromIntent();

        saveButton.setOnClickListener(v -> updateReservation());
    }

    private void bindViews() {
        serviceInput = findViewById(R.id.editReservationServiceInput);
        descriptionInput = findViewById(R.id.editReservationDescriptionInput);
        timePeriodInput = findViewById(R.id.editReservationTimePeriodInput);
        maxGuestsInput = findViewById(R.id.editReservationMaxGuestsInput);
        saveButton = findViewById(R.id.editReservationSaveButton);
    }

    private void bindToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.adminEditReservationToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void bindDataFromIntent() {
        reservationId = value(getIntent().getStringExtra("reservation_id"));
        serviceInput.setText(value(getIntent().getStringExtra("service_name")));
        descriptionInput.setText(value(getIntent().getStringExtra("description")));
        timePeriodInput.setText(value(getIntent().getStringExtra("time_period")));
        maxGuestsInput.setText(value(getIntent().getStringExtra("max_guests")));
    }

    private void updateReservation() {
        saveButton.setEnabled(false);
        saveButton.setText("Saving...");

        String service = serviceInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String timePeriod = timePeriodInput.getText().toString().trim();
        String maxGuests = maxGuestsInput.getText().toString().trim();

        if (service.isEmpty() || description.isEmpty() || timePeriod.isEmpty() || maxGuests.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            saveButton.setEnabled(true);
            saveButton.setText("Save Changes");
            return;
        }

        new Thread(() -> {
            try {
                String url = SupabaseClient.SUPABASE_URL + "/rest/v1/add_reservation_services";
                String encodedId = URLEncoder.encode(reservationId, "UTF-8");
                url = url + "?id=eq." + encodedId;

                JSONObject body = new JSONObject();
                body.put("service_name", service);
                body.put("description", description);
                body.put("time_period", timePeriod);
                body.put("max_guests", maxGuests);

                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("PATCH");
                connection.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                connection.setRequestProperty("Authorization", "Bearer " + getBearerToken());
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Prefer", "return=minimal");
                connection.setDoOutput(true);

                OutputStream os = connection.getOutputStream();
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                os.close();

                int code = connection.getResponseCode();
                connection.disconnect();

                runOnUiThread(() -> {
                    saveButton.setEnabled(true);
                    saveButton.setText("Save Changes");

                    if (code >= 200 && code < 300) {
                        Toast.makeText(this, "Reservation updated", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(this, "Save failed. Check RLS policy.", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    saveButton.setEnabled(true);
                    saveButton.setText("Save Changes");
                    Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private String getBearerToken() {
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String token = prefs.getString("access_token", "");
        if (token == null || token.trim().isEmpty()) {
            return "";
        }
        return token;
    }

    private String value(String input) {
        return input == null ? "" : input.trim();
    }
}
