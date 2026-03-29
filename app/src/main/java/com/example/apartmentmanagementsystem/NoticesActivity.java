package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class NoticesActivity extends AppCompatActivity {

    private RecyclerView recyclerNotices;
    private Noticeadapter noticeAdapter;
    private List<Notice> noticeList;
    private TextView tvNoticeCount;

    // Bottom nav
    private LinearLayout navNotices, navChat, navFeed, navServices, navProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide default ActionBar - use custom header
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_notices);

        initViews();
        loadDummyNotices();
        setupBottomNavigation();
    }

    private void initViews() {
        recyclerNotices = findViewById(R.id.recyclerNotices);
        tvNoticeCount = findViewById(R.id.tvNoticeCount);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Setup RecyclerView
        noticeList = new ArrayList<>();
        noticeAdapter = new Noticeadapter(this, noticeList);
        recyclerNotices.setLayoutManager(new LinearLayoutManager(this));
        recyclerNotices.setAdapter(noticeAdapter);
    }

    private void loadDummyNotices() {
        // 4 dummy notices — replace with Firebase fetch later
        noticeList.add(new Notice(
                "1",
                "Water supply maintenance on Saturday",
                "Water supply will be interrupted from 9 AM to 1 PM this Saturday for routine maintenance. Please store water accordingly.",
                "Management",
                "2 hours ago",
                "🔔 Notice",
                24, 8
        ));

        noticeList.add(new Notice(
                "2",
                "Parking lot painting — Block B closed",
                "The Block B parking lot will be closed on Monday and Tuesday for repainting. Please use the overflow parking near Gate 2 during this period.",
                "Management",
                "Yesterday",
                "🚗 Parking",
                15, 3
        ));

        noticeList.add(new Notice(
                "3",
                "Elevator inspection — Tower A",
                "The elevator in Tower A will be under scheduled maintenance from 10 AM to 4 PM on Wednesday. Please use the staircase or the Tower B elevator during this time.",
                "Management",
                "2 days ago",
                "⚠️ Urgent",
                31, 12
        ));

        noticeList.add(new Notice(
                "4",
                "Community hall booking — Advance reservation open",
                "Residents can now book the community hall in advance for events in December. A minimum of 3 days notice is required. Contact the admin office to reserve your slot.",
                "Management",
                "3 days ago",
                "🏢 Facility",
                9, 5
        ));

        noticeAdapter.notifyDataSetChanged();
        updateNoticeCount();
    }

    // Call this method when admin sends a new notice (from Firebase listener)
    public void onNewNoticeReceived(String id, String title, String body, String timeAgo, String badge) {
        Notice newNotice = new Notice(id, title, body, "Management", timeAgo, badge, 0, 0);
        noticeAdapter.addNotice(newNotice);
        recyclerNotices.smoothScrollToPosition(0);
        updateNoticeCount();
    }

    private void updateNoticeCount() {
        int count = noticeList.size();
        tvNoticeCount.setText(count + " active notice" + (count != 1 ? "s" : ""));
    }

    private void setupBottomNavigation() {
        FrameLayout navFeed = findViewById(R.id.nav_btn_feed);
        LinearLayout navNotices = findViewById(R.id.nav_btn_notices);
        LinearLayout navChat = findViewById(R.id.nav_btn_chat);
        LinearLayout navServices = findViewById(R.id.nav_btn_services);
        LinearLayout navProfile = findViewById(R.id.nav_btn_profile);

        navFeed.setOnClickListener(v -> ActivityNavigationHelper.navigate(this, FeedActivity.class, true));
        navNotices.setOnClickListener(v -> ActivityNavigationHelper.navigate(this, NoticesActivity.class, false));
        navChat.setOnClickListener(v -> ActivityNavigationHelper.navigate(this, ChatActivity.class, true));
        navServices.setOnClickListener(v -> ActivityNavigationHelper.navigate(this, ServicesActivity.class, true));
        navProfile.setOnClickListener(v -> ActivityNavigationHelper.navigate(this, ProfileActivity.class, true));
    }
}