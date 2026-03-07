package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.apartmentmanagementsystem.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.appbar.MaterialToolbar;

public class ReservationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        // Make status bar transparent to blend with dark background
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        setContentView(R.layout.activity_reservations);
        ImageView calendarIcon = findViewById(R.id.btnReservationHistory);
        calendarIcon.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReservationHistoryActivity.class);
            startActivity(intent);
        });
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> onBackPressed());
        // Toolbar back button
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Book Pool
        MaterialButton btnPool = findViewById(R.id.btnBookPool);
        btnPool.setOnClickListener(v -> {
            // TODO: open booking dialog / date picker for Pool
        });

        // Book Gym
        MaterialButton btnGym = findViewById(R.id.btnBookGym);
        btnGym.setOnClickListener(v -> {
            // TODO: open booking dialog / date picker for Gym
        });

        // Book Restaurant
        MaterialButton btnRestaurant = findViewById(R.id.btnBookRestaurant);
        btnRestaurant.setOnClickListener(v -> {
            // TODO: open table reservation dialog for Restaurant
        });
    }
}