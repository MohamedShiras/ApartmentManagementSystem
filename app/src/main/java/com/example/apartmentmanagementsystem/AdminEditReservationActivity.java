package com.example.apartmentmanagementsystem;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
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
    private EditText dateInput;
    private EditText timeInput;
    private EditText durationInput;
    private EditText statusInput;
    private EditText bookedByInput;
    private EditText imageUrlInput;
    private ImageView reservationImage;
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
        dateInput = findViewById(R.id.editReservationDateInput);
        timeInput = findViewById(R.id.editReservationTimeInput);
        durationInput = findViewById(R.id.editReservationDurationInput);
        statusInput = findViewById(R.id.editReservationStatusInput);
        bookedByInput = findViewById(R.id.editReservationBookedByInput);
        imageUrlInput = findViewById(R.id.editReservationImageUrlInput);
        reservationImage = findViewById(R.id.editReservationImage);
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
        dateInput.setText(value(getIntent().getStringExtra("reservation_date")));
        timeInput.setText(value(getIntent().getStringExtra("reservation_time")));
        durationInput.setText(value(getIntent().getStringExtra("duration")));
        statusInput.setText(value(getIntent().getStringExtra("status")));
        bookedByInput.setText(value(getIntent().getStringExtra("booked_by")));

        String imageUrl = value(getIntent().getStringExtra("image_url"));
        imageUrlInput.setText(imageUrl);
        loadImageFromUrl(imageUrl);

        imageUrlInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                loadImageFromUrl(value(s.toString()));
            }
        });
    }

    private void loadImageFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            reservationImage.setImageResource(android.R.drawable.ic_menu_gallery);
            return;
        }
        new Thread(() -> {
            try {
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(
                        new java.net.URL(url).openConnection().getInputStream()
                );
                reservationImage.post(() -> reservationImage.setImageBitmap(bitmap));
            } catch (Exception e) {
                reservationImage.post(() -> reservationImage.setImageResource(android.R.drawable.ic_menu_gallery));
            }
        }).start();
    }

    private void updateReservation() {
        saveButton.setEnabled(false);
        saveButton.setText("Saving...");

        new Thread(() -> {
            try {
                boolean isCreate = reservationId.isEmpty();
                String url = SupabaseClient.SUPABASE_URL + "/rest/v1/reservations";
                if (!isCreate) {
                    String encodedId = URLEncoder.encode(reservationId, "UTF-8");
                    url = url + "?id=eq." + encodedId;
                }

                JSONObject body = new JSONObject();
                body.put("service_name", value(serviceInput.getText().toString()));
                body.put("description", value(descriptionInput.getText().toString()));
                body.put("reservation_date", value(dateInput.getText().toString()));
                body.put("reservation_time", value(timeInput.getText().toString()));
                body.put("duration", value(durationInput.getText().toString()));
                body.put("status", value(statusInput.getText().toString()));
                body.put("booked_by", value(bookedByInput.getText().toString()));
                body.put("image_url", value(imageUrlInput.getText().toString()));

                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod(isCreate ? "POST" : "PATCH");
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
                        Toast.makeText(this, isCreate ? "Reservation added" : "Reservation updated", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(this,
                                "Save failed. Check table columns or RLS policy.",
                                Toast.LENGTH_LONG).show();
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
            return SupabaseClient.SUPABASE_ANON_KEY;
        }
        return token;
    }

    private String value(String input) {
        return input == null ? "" : input.trim();
    }
}
