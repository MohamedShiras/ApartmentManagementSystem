package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AdminDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        ImageButton backButton = findViewById(R.id.btn_back);
        LinearLayout maintenanceCard = findViewById(R.id.card_maintenance);
        LinearLayout filesCard = findViewById(R.id.card_files);
        LinearLayout reservationMaintenanceCard = findViewById(R.id.card_reservation_maintenance);

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        maintenanceCard.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminReservationMaintenanceActivity.class);
            startActivity(intent);
        });

        filesCard.setOnClickListener(v ->
                Toast.makeText(this, "User files page will be added soon.", Toast.LENGTH_SHORT).show());

        reservationMaintenanceCard.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminReservationMaintenanceActivity.class);
            startActivity(intent);
        });
    }
}
