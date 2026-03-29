package com.example.apartmentmanagementsystem.admin.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apartmentmanagementsystem.R;
import com.example.apartmentmanagementsystem.admin.adapter.AdminAnnouncementAdapter;
import com.example.apartmentmanagementsystem.admin.model.Announcement;

import java.util.ArrayList;
import java.util.List;

public class AdminAnnouncementActivity extends AppCompatActivity {

    private final List<Announcement> announcementList = new ArrayList<>();
    private AdminAnnouncementAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.admin_activity_announcements);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.adminAnnouncementsRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        seedAnnouncements();
        setupRecycler();
        bindSummary();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnNewAnnouncement).setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminItemAnnouncementActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.submitList(new ArrayList<>(announcementList));
            bindSummary();
        }
    }

    private void setupRecycler() {
        RecyclerView recyclerView = findViewById(R.id.announcementsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AdminAnnouncementAdapter(new AdminAnnouncementAdapter.OnAnnouncementActionListener() {
            @Override
            public void onEdit(Announcement announcement) {
                openItemAnnouncementForm(announcement);
            }

            @Override
            public void onDelete(Announcement announcement) {
                announcementList.remove(announcement);
                adapter.submitList(new ArrayList<>(announcementList));
                bindSummary();
                Toast.makeText(AdminAnnouncementActivity.this, "Announcement deleted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStatusChange(Announcement announcement, Announcement.AnnouncementStatus newStatus) {
                announcement.setStatus(newStatus);
                adapter.notifyDataSetChanged();
                bindSummary();
            }
        });

        recyclerView.setAdapter(adapter);
        adapter.submitList(new ArrayList<>(announcementList));
    }

    private void openItemAnnouncementForm(Announcement announcement) {
        Intent intent = new Intent(this, AdminItemAnnouncementActivity.class);
        intent.putExtra(AdminItemAnnouncementActivity.EXTRA_SENDER, announcement.getSender());
        intent.putExtra(AdminItemAnnouncementActivity.EXTRA_TIME, announcement.getTimestamp());
        intent.putExtra(AdminItemAnnouncementActivity.EXTRA_TYPE, announcement.getType().name());
        intent.putExtra(AdminItemAnnouncementActivity.EXTRA_TITLE, announcement.getTitle());
        intent.putExtra(AdminItemAnnouncementActivity.EXTRA_BODY, announcement.getBody());
        intent.putExtra(AdminItemAnnouncementActivity.EXTRA_LIKES, String.valueOf(announcement.getLikes()));
        intent.putExtra(AdminItemAnnouncementActivity.EXTRA_COMMENTS, String.valueOf(announcement.getComments()));
        startActivity(intent);
    }

    private void bindSummary() {
        int total = announcementList.size();
        int published = 0;
        int scheduled = 0;
        int draft = 0;

        for (Announcement item : announcementList) {
            if (item.getStatus() == Announcement.AnnouncementStatus.PUBLISHED_LIVE) {
                published++;
            } else if (item.getStatus() == Announcement.AnnouncementStatus.SCHEDULED) {
                scheduled++;
            } else if (item.getStatus() == Announcement.AnnouncementStatus.DRAFT) {
                draft++;
            }
        }

        ((TextView) findViewById(R.id.tvTotalCount)).setText(String.valueOf(total));
        ((TextView) findViewById(R.id.tvPublishedCount)).setText(String.valueOf(published));
        ((TextView) findViewById(R.id.tvScheduledCount)).setText(String.valueOf(scheduled));
        ((TextView) findViewById(R.id.tvDraftCount)).setText(String.valueOf(draft));
    }

    private void seedAnnouncements() {
        if (!announcementList.isEmpty()) {
            return;
        }

        announcementList.add(new Announcement(
                "AN-001",
                "Water Tank Cleaning Schedule",
                "Water supply will pause from 10 AM to 2 PM for cleaning.",
                "Admin Office",
                "Today 09:10",
                Announcement.AnnouncementType.NOTICE,
                Announcement.AnnouncementStatus.PUBLISHED_LIVE,
                "All Residents"
        ));

        announcementList.add(new Announcement(
                "AN-002",
                "Lift Maintenance",
                "Tower A lift is under maintenance. Use Lift 2.",
                "Maintenance Team",
                "Tomorrow 08:00",
                Announcement.AnnouncementType.ALERT,
                Announcement.AnnouncementStatus.SCHEDULED,
                "Tower A"
        ));
    }
}
