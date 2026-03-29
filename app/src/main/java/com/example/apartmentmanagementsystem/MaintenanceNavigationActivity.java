package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MaintenanceNavigationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_maintenance_navigation);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_maintenance_navigation), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.iconNotification).setOnClickListener(v ->
                startActivity(new Intent(this, NoticesActivity.class))
        );

        findViewById(R.id.iconProfile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class))
        );

        findViewById(R.id.cardElevator).setOnClickListener(v -> openServiceOptions("Elevator Management"));
        findViewById(R.id.cardGenerator).setOnClickListener(v -> openServiceOptions("Generator Management"));
        findViewById(R.id.cardElectric).setOnClickListener(v -> openServiceOptions("Electric Management"));
    }

    private void openServiceOptions(String serviceTitle) {
        Intent intent = new Intent(this, MaintenanceOptionActivity.class);
        intent.putExtra(MaintenanceActivity.EXTRA_SERVICE_TITLE, serviceTitle);
        startActivity(intent);
    }
}

