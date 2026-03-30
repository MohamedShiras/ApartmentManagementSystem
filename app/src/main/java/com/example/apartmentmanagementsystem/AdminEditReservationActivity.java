package com.example.apartmentmanagementsystem;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class AdminEditReservationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_edit_reservation);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setupBackButton();
        bindReservationDetails();
        setupSaveButton();
    }

    private void setupBackButton() {
        ImageView btnBack = findViewById(R.id.btnBackEditReservation);
        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void bindReservationDetails() {
        ImageView ivReservation = findViewById(R.id.ivEditReservationImage);
        TextInputEditText etServiceName = findViewById(R.id.etEditServiceName);
        TextInputEditText etBookedBy = findViewById(R.id.etEditBookedBy);
        TextInputEditText etDescription = findViewById(R.id.etEditDescription);
        TextInputEditText etDateTime = findViewById(R.id.etEditDateTime);
        TextInputEditText etStatus = findViewById(R.id.etEditStatus);

        String imageUri = getIntent().getStringExtra(AdminReservationMaintenanceActivity.EXTRA_IMAGE_URI);
        if (imageUri != null && !imageUri.trim().isEmpty()) {
            Glide.with(this)
                    .load(imageUri)
                    .placeholder(R.drawable.apartment_logo)
                    .error(R.drawable.apartment_logo)
                    .into(ivReservation);
        } else {
            int imageRes = getIntent().getIntExtra(
                    AdminReservationMaintenanceActivity.EXTRA_IMAGE_RES_ID,
                    R.drawable.apartment_logo
            );
            ivReservation.setImageResource(imageRes);
        }

        etServiceName.setText(getIntent().getStringExtra(AdminReservationMaintenanceActivity.EXTRA_SERVICE_NAME));
        etBookedBy.setText(getIntent().getStringExtra(AdminReservationMaintenanceActivity.EXTRA_BOOKED_BY));
        etDescription.setText(getIntent().getStringExtra(AdminReservationMaintenanceActivity.EXTRA_DESCRIPTION));
        etDateTime.setText(getIntent().getStringExtra(AdminReservationMaintenanceActivity.EXTRA_DATE_TIME));
        etStatus.setText(getIntent().getStringExtra(AdminReservationMaintenanceActivity.EXTRA_STATUS));
    }

    private void setupSaveButton() {
        MaterialButton btnSave = findViewById(R.id.btnSaveEditedReservation);
        btnSave.setOnClickListener(v -> {
            Toast.makeText(this, "Reservation updates saved", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
