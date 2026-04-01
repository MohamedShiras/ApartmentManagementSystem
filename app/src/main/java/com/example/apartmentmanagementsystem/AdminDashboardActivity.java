package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

public class AdminDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        MaterialToolbar toolbar = findViewById(R.id.adminDashboardToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        MaterialCardView maintenanceCard = findViewById(R.id.adminDashboardReservationsCard);
        maintenanceCard.setOnClickListener(v -> startActivity(
                new Intent(this, AdminReservationMaintenanceActivity.class)
        ));
    }
}

