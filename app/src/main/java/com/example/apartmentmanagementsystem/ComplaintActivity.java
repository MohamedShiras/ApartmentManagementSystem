package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ComplaintActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ComplaintAdapter adapter;
    private List<Complaint> allComplaints;
    private List<Complaint> filteredComplaints;
    private TextView textActiveCount, textResolvedCount, textPendingCount, textListHeader;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complaint);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        initViews();
        setupTabs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchComplaints();
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        textActiveCount   = findViewById(R.id.textActiveCount);
        textResolvedCount = findViewById(R.id.textResolvedCount);
        textPendingCount  = findViewById(R.id.textPendingCount);
        textListHeader    = findViewById(R.id.textListHeader);
        progressBar       = findViewById(R.id.progressBar);

        recyclerView = findViewById(R.id.recyclerViewComplaints);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        allComplaints      = new ArrayList<>();
        filteredComplaints = new ArrayList<>();
        adapter = new ComplaintAdapter(filteredComplaints, this::openDetail);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.fabFileComplaint).setOnClickListener(v ->
                startActivity(new Intent(this, FileComplaintActivity.class)));
    }

    private void fetchComplaints() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
                String apartment = prefs.getString("apartment", "").trim();
                String token     = prefs.getString("access_token", "");

                if (apartment.isEmpty()) {
                    runOnUiThread(() -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Session Error: Please login again", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                String encodedApartment = Uri.encode(apartment);
                String queryUrl = SupabaseClient.SUPABASE_URL + "/rest/v1/complaints"
                        + "?apartment_number=eq." + encodedApartment
                        + "&select=*&order=id.desc";

                URL url = new URL(queryUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey",        SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + token);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONArray arr = new JSONArray(sb.toString());
                    allComplaints.clear();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        String id = obj.optString("id", String.valueOf(obj.optInt("id", 0)));

                        allComplaints.add(new Complaint(
                                id,
                                obj.optString("category",    "Other"),
                                obj.optString("subject",     "No Subject"),
                                obj.optString("description", ""),
                                obj.optString("status",      "Pending"),
                                obj.optString("date",        ""),
                                obj.optString("priority",    "Medium")  // NEW
                        ));
                    }

                    runOnUiThread(() -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        applyFilter();
                        updateSummary();
                    });
                } else {
                    runOnUiThread(() -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Server Error: " + responseCode, Toast.LENGTH_SHORT).show();
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.e("ComplaintFetch", "Error", e);
                runOnUiThread(() -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void openDetail(Complaint complaint) {
        Intent intent = new Intent(this, ComplaintDetailActivity.class);
        intent.putExtra("id",          complaint.getId());
        intent.putExtra("category",    complaint.getCategory());
        intent.putExtra("subject",     complaint.getSubject());
        intent.putExtra("date",        complaint.getDate());
        intent.putExtra("description", complaint.getDescription());
        intent.putExtra("status",      complaint.getStatus());
        intent.putExtra("priority",    complaint.getPriority()); // NEW
        startActivity(intent);
    }

    private void setupTabs() {
        TabLayout tabLayout = findViewById(R.id.tabLayoutStatus);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab)   { applyFilter(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void applyFilter() {
        TabLayout tabLayout = findViewById(R.id.tabLayoutStatus);
        int position = tabLayout.getSelectedTabPosition();
        if (position < 0) return;

        TabLayout.Tab tab = tabLayout.getTabAt(position);
        if (tab == null || tab.getText() == null) return;

        String statusFilter = tab.getText().toString();

        filteredComplaints.clear();
        if ("All".equalsIgnoreCase(statusFilter)) {
            filteredComplaints.addAll(allComplaints);
            textListHeader.setText("All Reports (" + allComplaints.size() + ")");
        } else {
            for (Complaint c : allComplaints) {
                if (c.getStatus().equalsIgnoreCase(statusFilter)) {
                    filteredComplaints.add(c);
                }
            }
            textListHeader.setText(statusFilter + " Reports (" + filteredComplaints.size() + ")");
        }
        adapter.notifyDataSetChanged();
    }

    private void updateSummary() {
        int active = 0, resolved = 0, pending = 0;
        for (Complaint c : allComplaints) {
            String s = c.getStatus().toLowerCase();
            if (s.equals("resolved") || s.equals("withdrawn")) {
                resolved++;
            } else if (s.equals("pending")) {
                pending++;
                active++;
            } else {
                active++;
            }
        }
        textActiveCount.setText(String.valueOf(active));
        textResolvedCount.setText(String.valueOf(resolved));
        textPendingCount.setText(String.valueOf(pending));
    }
}