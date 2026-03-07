package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class ComplaintActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ComplaintAdapter adapter;
    private List<Complaint> allComplaints;
    private List<Complaint> filteredComplaints;
    private TextView textActiveCount, textResolvedCount, textPendingCount, textListHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complaint);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initViews();
        setupData();
        setupTabs();
    }

    private void initViews() {

        textPendingCount = findViewById(R.id.textPendingCount);
        View btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        textActiveCount = findViewById(R.id.textActiveCount);
        textResolvedCount = findViewById(R.id.textResolvedCount);
        textListHeader = findViewById(R.id.textListHeader);

        recyclerView = findViewById(R.id.recyclerViewComplaints);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 2. Fixed: Changed ExtendedFloatingActionButton to CardView/View
        // to match your activity_complaint.xml
        View fabFileComplaint = findViewById(R.id.fabFileComplaint);
        fabFileComplaint.setOnClickListener(v -> {
            startActivity(new Intent(ComplaintActivity.this, FileComplaintActivity.class));
        });
    }

    private void setupData() {
        allComplaints = new ArrayList<>();
        allComplaints.add(new Complaint("1", "Plumbing", "Leaking pipe in kitchen", "The main pipe under the sink has a small crack and is dripping water continuously.", "In Progress", "Oct 24, 2023"));
        allComplaints.add(new Complaint("2", "Electrical", "Flickering lights in hallway", "The lights in the main hallway are flickering every few seconds, possibly a loose connection.", "Pending", "Oct 22, 2023"));
        allComplaints.add(new Complaint("3", "Cleaning", "Garbage not collected", "The garbage from the 4th floor has not been collected for the last two days.", "Resolved", "Oct 20, 2023"));
        allComplaints.add(new Complaint("4", "Security", "Broken gate lock", "The main entrance gate lock is not clicking into place properly.", "Pending", "Oct 19, 2023"));

        filteredComplaints = new ArrayList<>(allComplaints);
        
        adapter = new ComplaintAdapter(filteredComplaints, complaint -> {
            Intent intent = new Intent(ComplaintActivity.this, ComplaintDetailActivity.class);
            intent.putExtra("category", complaint.getCategory());
            intent.putExtra("subject", complaint.getSubject());
            intent.putExtra("date", complaint.getDate());
            intent.putExtra("description", complaint.getDescription());
            intent.putExtra("status", complaint.getStatus());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        updateSummary();
    }

    private void setupTabs() {
        TabLayout tabLayout = findViewById(R.id.tabLayoutStatus);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterByStatus(tab.getText().toString());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void filterByStatus(String status) {
        filteredComplaints.clear();
        if ("All".equals(status)) {
            filteredComplaints.addAll(allComplaints);
            textListHeader.setText("All Reports");
        } else {
            for (Complaint c : allComplaints) {
                if (c.getStatus().equalsIgnoreCase(status)) {
                    filteredComplaints.add(c);
                }
            }
            textListHeader.setText(status + " Reports");
        }
        adapter.notifyDataSetChanged();
    }

    private void updateSummary() {
        int active = 0;
        int resolved = 0;
        int pending = 0;
        for (Complaint c : allComplaints) {
            if ("Resolved".equalsIgnoreCase(c.getStatus())) {
                resolved++;
            } else if ("Pending".equalsIgnoreCase(c.getStatus())) {
                pending++;
                active++; // Usually pending is considered active
            } else {
                active++;
            }
        }
        textActiveCount.setText(String.valueOf(active));
        textResolvedCount.setText(String.valueOf(resolved));
        textPendingCount.setText(String.valueOf(pending));
    }
}
