package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AdminReservationMaintenanceActivity extends AppCompatActivity {

    public static final String EXTRA_RESERVATION_ID = "extra_reservation_id";
    public static final String EXTRA_SERVICE_NAME = "extra_service_name";
    public static final String EXTRA_BOOKED_BY = "extra_booked_by";
    public static final String EXTRA_DESCRIPTION = "extra_description";
    public static final String EXTRA_DATE_TIME = "extra_date_time";
    public static final String EXTRA_STATUS = "extra_status";
    public static final String EXTRA_IMAGE_RES_ID = "extra_image_res_id";
    public static final String EXTRA_IMAGE_URI = "extra_image_uri";

    private final List<AdminReservation> reservations = new ArrayList<>();
    private TextView tvEmptyAdminBooking;
    private AdminReservationMaintenanceAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_reservation_maintenance);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setupBackButton();
        setupRecyclerView();
        loadMockReservations();
        refreshEmptyState();
    }

    private void setupBackButton() {
        ImageView btnBack = findViewById(R.id.btnBackAdminMaintenance);
        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.rvAdminBookedReservations);
        tvEmptyAdminBooking = findViewById(R.id.tvEmptyAdminBooking);

        adapter = new AdminReservationMaintenanceAdapter(reservations,
                new AdminReservationMaintenanceAdapter.OnReservationActionListener() {
                    @Override
                    public void onEditClick(AdminReservation reservation) {
                        openEditScreen(reservation);
                    }

                    @Override
                    public void onDeleteClick(int position) {
                        if (position != RecyclerView.NO_POSITION && position < reservations.size()) {
                            reservations.remove(position);
                            adapter.notifyItemRemoved(position);
                            refreshEmptyState();
                        }
                    }
                });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void openEditScreen(AdminReservation reservation) {
        Intent intent = new Intent(this, AdminEditReservationActivity.class);
        intent.putExtra(EXTRA_RESERVATION_ID, reservation.getId());
        intent.putExtra(EXTRA_SERVICE_NAME, reservation.getServiceName());
        intent.putExtra(EXTRA_BOOKED_BY, reservation.getBookedBy());
        intent.putExtra(EXTRA_DESCRIPTION, reservation.getDescription());
        intent.putExtra(EXTRA_DATE_TIME, reservation.getDateTime());
        intent.putExtra(EXTRA_STATUS, reservation.getStatus());
        intent.putExtra(EXTRA_IMAGE_RES_ID, reservation.getImageResId());
        if (reservation.getImageUri() != null) {
            intent.putExtra(EXTRA_IMAGE_URI, reservation.getImageUri());
        }
        startActivity(intent);
    }

    private void loadMockReservations() {
        reservations.clear();
        reservations.add(new AdminReservation(
                "R-101",
                "Swimming Pool",
                "John Mathew",
                "Family lane booking with 4 guests.",
                "Apr 20, 2026 | 02:00 PM - 04:00 PM",
                "Confirmed",
                R.drawable.img_pool
        ));

        reservations.add(new AdminReservation(
                "R-102",
                "Restaurant",
                "Sarah Lim",
                "Window-side dining reservation for 3 guests.",
                "Apr 22, 2026 | 07:00 PM - 09:00 PM",
                "Pending",
                R.drawable.img_restaurant
        ));

        reservations.add(new AdminReservation(
                "R-103",
                "Gym & Fitness",
                "Daniel Cruz",
                "Personal training slot and equipment access.",
                "Apr 24, 2026 | 06:30 AM - 08:00 AM",
                "Confirmed",
                R.drawable.img_gym
        ));

        adapter.notifyDataSetChanged();
    }

    private void refreshEmptyState() {
        if (reservations.isEmpty()) {
            tvEmptyAdminBooking.setVisibility(View.VISIBLE);
        } else {
            tvEmptyAdminBooking.setVisibility(View.GONE);
        }
    }
}
