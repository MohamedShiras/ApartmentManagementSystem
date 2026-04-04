package com.example.apartmentmanagementsystem;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileComplaintActivity extends AppCompatActivity {

    private AutoCompleteTextView autoCompleteCategory;
    private TextInputEditText editSubject, editDescription;
    private MaterialButton btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_complaint);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        autoCompleteCategory = findViewById(R.id.autoCompleteCategory);
        editSubject = findViewById(R.id.editSubject);
        editDescription = findViewById(R.id.editDescription);
        btnSubmit = findViewById(R.id.btnSubmit);

        // Setup Categories
        String[] categories = {"Plumbing", "Electrical", "Cleaning", "Security", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        autoCompleteCategory.setAdapter(adapter);

        btnSubmit.setOnClickListener(v -> {
            submitComplaint();
        });
    }

    private void submitComplaint() {
        if (autoCompleteCategory.getText() == null || editSubject.getText() == null || editDescription.getText() == null) return;

        String category = autoCompleteCategory.getText().toString();
        String subject = editSubject.getText().toString().trim();
        String description = editDescription.getText().toString().trim();

        if (category.isEmpty() || subject.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        new Thread(() -> {
            try {
                // 1. Get Session Data
                SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
                String apartment = prefs.getString("apartment", "");
                String token = prefs.getString("access_token", "");

                if (apartment.isEmpty() || token.isEmpty()) {
                    runOnUiThread(() -> {
                        Toast.makeText(FileComplaintActivity.this, "Session expired. Please Login again.", Toast.LENGTH_LONG).show();
                        setLoading(false);
                    });
                    return;
                }

                // 2. Prepare Data
                String dateStr = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("apartment_number", apartment);
                jsonBody.put("category", category);
                jsonBody.put("subject", subject);
                jsonBody.put("description", description);
                jsonBody.put("status", "Pending");
                jsonBody.put("date", dateStr);

                // 3. POST to Supabase
                URL url = new URL(SupabaseClient.SUPABASE_URL + "/rest/v1/complaints");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Prefer", "return=minimal");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonBody.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 201 || responseCode == 200 || responseCode == 204) {
                    runOnUiThread(() -> {
                        Toast.makeText(FileComplaintActivity.this, "Complaint submitted successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(FileComplaintActivity.this, "Submission failed: " + responseCode, Toast.LENGTH_LONG).show();
                        setLoading(false);
                    });
                }
                conn.disconnect();

            } catch (Exception e) {
                Log.e("FileComplaint", "Error", e);
                runOnUiThread(() -> {
                    Toast.makeText(FileComplaintActivity.this, "Network Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    setLoading(false);
                });
            }
        }).start();
    }

    private void setLoading(boolean loading) {
        btnSubmit.setEnabled(!loading);
        btnSubmit.setText(loading ? "Submitting..." : "Submit Complaint");
    }
}
