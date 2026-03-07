package com.example.apartmentmanagementsystem;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.apartmentmanagementsystem.R;
import com.google.android.material.button.MaterialButton;

public class ReservationHistoryActivity extends AppCompatActivity {

    // Filter chips


    // Cards
    private View cardPoolConfirmed, cardRestaurantConfirmed, cardGymPending;
    private View layoutEmptyState;

    // Cancel buttons
    private MaterialButton btnCancelPool, btnCancelRestaurant, btnCancelGym;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation_history);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        initViews();
        setupBackButton();
        setupCancelButtons();
    }

    private void initViews() {


        cardPoolConfirmed       = findViewById(R.id.cardPoolConfirmed);
        cardRestaurantConfirmed = findViewById(R.id.cardRestaurantConfirmed);
        cardGymPending          = findViewById(R.id.cardGymPending);
        layoutEmptyState        = findViewById(R.id.layoutEmptyState);

        btnCancelPool       = findViewById(R.id.btnCancelPool);
        btnCancelRestaurant = findViewById(R.id.btnCancelRest);
        btnCancelGym        = findViewById(R.id.btnCancelGym);
    }

    private void setupBackButton() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    // ── Filter Chip Logic ──────────────────────────────────────


    private void showFilter(String filter) {
        // Visual state: only "All" chip shows filled for now
        // You can extend this with a proper state drawable selector
        switch (filter) {
            case "all":
                cardPoolConfirmed.setVisibility(View.VISIBLE);
                cardRestaurantConfirmed.setVisibility(View.VISIBLE);
                cardGymPending.setVisibility(View.VISIBLE);
                layoutEmptyState.setVisibility(View.GONE);
                break;
            case "confirmed":
                cardPoolConfirmed.setVisibility(View.VISIBLE);
                cardRestaurantConfirmed.setVisibility(View.VISIBLE);
                cardGymPending.setVisibility(View.GONE);
                layoutEmptyState.setVisibility(View.GONE);
                break;
            case "pending":
                cardPoolConfirmed.setVisibility(View.GONE);
                cardRestaurantConfirmed.setVisibility(View.GONE);
                cardGymPending.setVisibility(View.VISIBLE);
                layoutEmptyState.setVisibility(View.GONE);
                break;
        }
    }

    // ── Cancel Confirmation Dialogs ────────────────────────────
    private void setupCancelButtons() {
        btnCancelPool.setOnClickListener(v ->
                showCancelDialog("Swimming Pool", "Lap Swimming · Today, 07:00 AM"));

        btnCancelRestaurant.setOnClickListener(v ->
                showCancelDialog("Restaurant", "Dinner · Tomorrow, 07:30 PM"));

        btnCancelGym.setOnClickListener(v ->
                showCancelDialog("Gym & Fitness", "Personal Training · Sun, Mar 09, 06:00 AM"));
    }

    private void showCancelDialog(String amenity, String details) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Reservation?")
                .setMessage("Are you sure you want to cancel your " + amenity
                        + " reservation?\n\n" + details)
                .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                    Toast.makeText(this,
                            amenity + " reservation cancelled.", Toast.LENGTH_SHORT).show();
                    // TODO: call your FireStore/backend cancel method here
                    // e.g. viewModel.cancelReservation(reservationId);
                })
                .setNegativeButton("Keep It", null)
                .show();
    }
}