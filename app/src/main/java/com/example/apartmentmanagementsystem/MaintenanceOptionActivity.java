package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MaintenanceOptionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maintenance_option);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        String serviceTitle = getIntent().getStringExtra(MaintenanceActivity.EXTRA_SERVICE_TITLE);
        if (serviceTitle == null || serviceTitle.trim().isEmpty()) {
            serviceTitle = "Maintenance";
        }
        final String resolvedServiceTitle = serviceTitle;

        TextView topTitle = findViewById(R.id.tvTopTitle);
        TextView bannerTitle = findViewById(R.id.tvBannerTitle);
        topTitle.setText(resolvedServiceTitle);
        bannerTitle.setText(resolvedServiceTitle);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        View cardComplaint = findViewById(R.id.cardComplaint);
        View cardStatusTracking = findViewById(R.id.cardStatusTracking);
        View cardScheduled = findViewById(R.id.cardScheduled);
        View cardServiceHistory = findViewById(R.id.cardServiceHistory);

        cardComplaint.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateMaintenanceIssueActivity.class);
            intent.putExtra(MaintenanceActivity.EXTRA_SERVICE_TITLE, resolvedServiceTitle);
            startActivity(intent);
        });
        cardStatusTracking.setOnClickListener(v -> startActivity(new Intent(this, MaintenanceStatusTrackingActivity.class)));
        cardScheduled.setOnClickListener(v -> Toast.makeText(this, "Scheduled maintenance coming soon", Toast.LENGTH_SHORT).show());
        cardServiceHistory.setOnClickListener(v -> Toast.makeText(this, "Service history coming soon", Toast.LENGTH_SHORT).show());
    }
}
