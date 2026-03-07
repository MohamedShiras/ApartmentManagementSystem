package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ServicesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_services);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. YOU MUST CALL THE METHOD HERE:
        setupQuickActions();

        // You will also need to call your bottom navigation setup here
        // setupBottomNavigation();
    }

    private void setupQuickActions() {
        // 2. USE THE CORRECT ID: cardMaintenance (matches activity_services.xml)
        CardView cardMaintenance = findViewById(R.id.cardComplaint);
        if (cardMaintenance != null) {
            cardMaintenance.setOnClickListener(v -> {
                startActivity(new Intent(this, ComplaintActivity.class));
            });
        }
    }
}