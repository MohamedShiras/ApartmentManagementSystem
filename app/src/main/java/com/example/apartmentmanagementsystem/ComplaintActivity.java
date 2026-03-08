package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

        // ✅ Fixed: matches android:id="@+id/main" in activity_complaint.xml
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupData();
        setupTabs();
        setupStaticCardClicks();
    }

    private void initViews() {
        CardView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        textActiveCount   = findViewById(R.id.textActiveCount);
        textResolvedCount = findViewById(R.id.textResolvedCount);
        textPendingCount  = findViewById(R.id.textPendingCount);
        textListHeader    = findViewById(R.id.textListHeader);

        recyclerView = findViewById(R.id.recyclerViewComplaints);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        CardView fabFileComplaint = findViewById(R.id.fabFileComplaint);
        fabFileComplaint.setOnClickListener(v ->
                startActivity(new Intent(this, FileComplaintActivity.class)));
    }

    private void setupData() {
        allComplaints = new ArrayList<>();
        allComplaints.add(new Complaint("CMP-0018", "Noise",       "Noisy neighbour — late night",    "The tenant in the unit above plays loud music past midnight.", "Under Review", "Oct 24, 2023"));
        allComplaints.add(new Complaint("CMP-0017", "Parking",     "Unauthorized parking — Bay 12",   "Someone is parking in my reserved bay regularly.",            "In Progress",  "Oct 22, 2023"));
        allComplaints.add(new Complaint("CMP-0016", "Cleanliness", "Littering in common corridor",    "Garbage is being left outside unit doors on Floor 3.",        "Pending",      "Oct 20, 2023"));
        allComplaints.add(new Complaint("CMP-0014", "Maintenance", "Lift doors closing too fast",     "The lift doors close before residents can safely enter.",     "Resolved",     "Oct 14, 2023"));

        filteredComplaints = new ArrayList<>(allComplaints);
        adapter = new ComplaintAdapter(filteredComplaints, this::openDetail);
        recyclerView.setAdapter(adapter);
        updateSummary();
    }

    private void setupStaticCardClicks() {
        int[] cardIds = {
                R.id.complaintItem1,
                R.id.complaintItem2,
                R.id.complaintItem3,
                R.id.complaintItem4
        };
        for (int i = 0; i < cardIds.length; i++) {
            final int index = i;
            CardView card = findViewById(cardIds[i]);
            if (card != null && index < allComplaints.size()) {
                card.setOnClickListener(v -> openDetail(allComplaints.get(index)));
            }
        }

        // "View Details" links also navigate to detail
        TextView v1 = findViewById(R.id.viewDetailsComplaint);
        if (v1 != null) v1.setOnClickListener(v -> openDetail(allComplaints.get(0)));

        TextView v2 = findViewById(R.id.viewDetailsComplaint2);
        if (v2 != null) v2.setOnClickListener(v -> openDetail(allComplaints.get(1)));

        TextView v3 = findViewById(R.id.viewDetailsComplaint3);
        if (v3 != null) v3.setOnClickListener(v -> openDetail(allComplaints.get(2)));
    }

    private void openDetail(Complaint complaint) {
        Intent intent = new Intent(this, ComplaintDetailActivity.class);
        intent.putExtra("id",          complaint.getId());
        intent.putExtra("category",    complaint.getCategory());
        intent.putExtra("subject",     complaint.getSubject());
        intent.putExtra("date",        complaint.getDate());
        intent.putExtra("description", complaint.getDescription());
        intent.putExtra("status",      complaint.getStatus());
        startActivity(intent);
    }

    private void setupTabs() {
        TabLayout tabLayout = findViewById(R.id.tabLayoutStatus);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                filterByStatus(tab.getText() != null ? tab.getText().toString() : "All");
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void filterByStatus(String status) {
        filteredComplaints.clear();
        if ("All".equals(status)) {
            filteredComplaints.addAll(allComplaints);
            textListHeader.setText("All Reports");
        } else {
            for (Complaint c : allComplaints) {
                if (c.getStatus().equalsIgnoreCase(status)) filteredComplaints.add(c);
            }
            textListHeader.setText(status + " Reports");
        }
        adapter.notifyDataSetChanged();
    }

    private void updateSummary() {
        int active = 0, resolved = 0, pending = 0;
        for (Complaint c : allComplaints) {
            if ("Resolved".equalsIgnoreCase(c.getStatus())) {
                resolved++;
            } else if ("Pending".equalsIgnoreCase(c.getStatus())) {
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