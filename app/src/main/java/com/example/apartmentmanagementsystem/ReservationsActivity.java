package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class ReservationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        setContentView(R.layout.activity_reservations);

        // ── Toolbar ──
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        // ── History icon ──
        ImageView calendarIcon = findViewById(R.id.btnReservationHistory);
        if (calendarIcon != null) {
            calendarIcon.setOnClickListener(v ->
                    startActivity(new Intent(this, ReservationHistoryActivity.class)));
        }

        // ── Book Pool ──
        MaterialButton btnPool = findViewById(R.id.btnBookPool);
        if (btnPool != null) {
            btnPool.setOnClickListener(v -> openBooking(
                    "Book Swimming Pool",
                    "6AM – 10PM  ·  Max 20 guests",
                    R.drawable.ic_pool,
                    "Pool"
            ));
        }

        // ── Book Gym ──
        MaterialButton btnGym = findViewById(R.id.btnBookGym);
        if (btnGym != null) {
            btnGym.setOnClickListener(v -> openBooking(
                    "Book Fitness Center",
                    "5AM – 11PM  ·  Max 15 guests",
                    R.drawable.ic_fitness_center,
                    "Gym"
            ));
        }

        // ── Book Restaurant ──
        MaterialButton btnRestaurant = findViewById(R.id.btnBookRestaurant);
        if (btnRestaurant != null) {
            btnRestaurant.setOnClickListener(v -> openBooking(
                    "Reserve a Table",
                    "7AM – 11PM  ·  A La Carte",
                    R.drawable.ic_restaurant,
                    "Restaurant"
            ));
        }
    }

    /** Launches BookingActivity as a dialog-style popup */
    private void openBooking(String title, String subtitle, int iconRes, String type) {
        Intent intent = new Intent(this, BookingActivity.class);
        intent.putExtra("amenity_title",    title);
        intent.putExtra("amenity_subtitle", subtitle);
        intent.putExtra("amenity_icon",     iconRes);
        intent.putExtra("amenity_type",     type);
        startActivity(intent);
    }
}