package com.example.apartmentmanagementsystem;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ComplaintDetailActivity extends AppCompatActivity {

    private TextView detailStatus, detailCategory, detailSubject, detailDescription, detailDate, detailRequestId, textUserName;
    private CardView btnConfirmResolve, btnWithdraw;
    private String complaintId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complaint_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        initViews();
        displayData();
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        detailStatus      = findViewById(R.id.detailStatus);
        detailCategory    = findViewById(R.id.detailCategory);
        detailSubject     = findViewById(R.id.detailSubject);
        detailDescription = findViewById(R.id.detailDescription);
        detailDate        = findViewById(R.id.detailDate);
        detailRequestId   = findViewById(R.id.detailRequestId);
        textUserName      = findViewById(R.id.textUserName);
        
        btnConfirmResolve = findViewById(R.id.btnConfirmResolve);
        btnWithdraw       = findViewById(R.id.btnWithdraw);

        btnWithdraw.setOnClickListener(v -> showWithdrawDialog());
        btnConfirmResolve.setOnClickListener(v -> showResolveDialog());
    }

    private void displayData() {
        complaintId        = getIntent().getStringExtra("id");
        String category    = getIntent().getStringExtra("category");
        String subject     = getIntent().getStringExtra("subject");
        String date        = getIntent().getStringExtra("date");
        String description = getIntent().getStringExtra("description");
        String status      = getIntent().getStringExtra("status");

        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String currentName = prefs.getString("full_name", "Resident");
        String apartment = prefs.getString("apartment", "");

        if (textUserName != null) textUserName.setText(currentName + " · Unit " + apartment);
        if (detailCategory != null) detailCategory.setText(category);
        if (detailSubject != null) detailSubject.setText(subject);
        if (detailDate != null) detailDate.setText("Submitted on " + date);
        if (detailDescription != null) detailDescription.setText(description);
        if (detailStatus != null) detailStatus.setText(status);
        if (detailRequestId != null) detailRequestId.setText("Request #" + complaintId);

        updateButtonsVisibility(status);
    }

    private void updateButtonsVisibility(String status) {
        if (status == null) return;
        
        if ("Resolved".equalsIgnoreCase(status) || "Withdrawn".equalsIgnoreCase(status)) {
            btnConfirmResolve.setVisibility(View.GONE);
            btnWithdraw.setVisibility(View.GONE);
        } 
        else if ("In Progress".equalsIgnoreCase(status)) {
            btnConfirmResolve.setVisibility(View.VISIBLE);
            btnWithdraw.setVisibility(View.VISIBLE);
        } 
        else {
            btnConfirmResolve.setVisibility(View.GONE);
            btnWithdraw.setVisibility(View.VISIBLE);
        }
    }

    private void showWithdrawDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Withdraw Complaint")
                .setMessage("Are you sure you want to withdraw this complaint? This will delete the complaint from the system.")
                .setPositiveButton("Withdraw & Delete", (dialog, which) -> deleteComplaint())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showResolveDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Mark as Resolved")
                .setMessage("Has this issue been fixed to your satisfaction?")
                .setPositiveButton("Yes, it's fixed", (dialog, which) -> updateComplaintStatus("Resolved"))
                .setNegativeButton("Not yet", null)
                .show();
    }

    private void deleteComplaint() {
        if (complaintId == null || complaintId.isEmpty()) {
            Toast.makeText(this, "Error: Invalid Complaint ID", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
                String token = prefs.getString("access_token", "");

                String urlStr = SupabaseClient.SUPABASE_URL + "/rest/v1/complaints?id=eq." + complaintId;
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "application/json");

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    runOnUiThread(() -> {
                        Toast.makeText(ComplaintDetailActivity.this, "Complaint deleted successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(ComplaintDetailActivity.this, "Delete Failed (Error " + code + ")", Toast.LENGTH_LONG).show());
                }
                conn.disconnect();
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(ComplaintDetailActivity.this, "Network Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void updateComplaintStatus(String newStatus) {
        if (complaintId == null || complaintId.isEmpty()) {
            Toast.makeText(this, "Error: Invalid Complaint ID", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
                String token = prefs.getString("access_token", "");

                String urlStr = SupabaseClient.SUPABASE_URL + "/rest/v1/complaints?id=eq." + complaintId;
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                
                conn.setRequestMethod("POST");
                conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
                conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Prefer", "return=minimal");
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("status", newStatus);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    runOnUiThread(() -> {
                        detailStatus.setText(newStatus);
                        updateButtonsVisibility(newStatus);
                        Toast.makeText(this, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Update Failed (Error " + code + ")", Toast.LENGTH_LONG).show());
                }
                conn.disconnect();
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Network Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}