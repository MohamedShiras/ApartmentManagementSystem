package com.example.apartmentmanagementsystem;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.util.Calendar;

public class BookingActivity extends AppCompatActivity {

    private final String[] selectedDate = {""};
    private final int[]    guestCount   = {2};
    private final int[]    selectedChip = {-1};

    private int   maxGuests;
    private int[] chipIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_booking);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        if (getWindow() != null) {

            getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            WindowManager.LayoutParams params = getWindow().getAttributes();


            params.gravity = Gravity.CENTER;


            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;


            params.dimAmount = 0.75f;

            getWindow().setAttributes(params);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);


            getWindow().setWindowAnimations(android.R.style.Animation_Dialog);
        }


        String amenityTitle    = getIntent().getStringExtra("amenity_title");
        String amenitySubtitle = getIntent().getStringExtra("amenity_subtitle");
        int    amenityIcon     = getIntent().getIntExtra("amenity_icon", R.drawable.ic_pool);
        String amenityType     = getIntent().getStringExtra("amenity_type");


        if ("Pool".equals(amenityType))       maxGuests = 20;
        else if ("Gym".equals(amenityType))   maxGuests = 15;
        else                                   maxGuests = 10;

        // ── Header ──
        TextView  tvTitle    = findViewById(R.id.dialogTitle);
        TextView  tvSubtitle = findViewById(R.id.dialogSubtitle);
        ImageView ivIcon     = findViewById(R.id.dialogAmenityIcon);

        if (tvTitle    != null) tvTitle.setText(amenityTitle);
        if (tvSubtitle != null) tvSubtitle.setText(amenitySubtitle);
        if (ivIcon     != null) ivIcon.setImageResource(amenityIcon);

        // ── Close / back ──
        ImageView btnClose = findViewById(R.id.dialogBtnClose);
        if (btnClose != null) btnClose.setOnClickListener(v -> finish());

        // ── Sections ──
        setupDatePicker();

        chipIds = new int[]{
                R.id.chipTime1, R.id.chipTime2, R.id.chipTime3,
        };
        setupTimeChips();
        setupGuestCounter();
        setupActionButtons(amenityType);
    }


    private void setupDatePicker() {
        View     layoutDate = findViewById(R.id.layoutSelectDate);
        TextView tvDate     = findViewById(R.id.tvSelectedDate);
        if (layoutDate == null || tvDate == null) return;

        layoutDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            DatePickerDialog picker = new DatePickerDialog(
                    this,
                    (view, year, month, day) -> {
                        String date = String.format("%02d/%02d/%04d", day, month + 1, year);
                        selectedDate[0] = date;
                        tvDate.setText(date);
                        tvDate.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            );
            picker.getDatePicker().setMinDate(cal.getTimeInMillis());
            picker.show();
        });
    }

    // ── Time Chips ───────────────────────────────
    private void setupTimeChips() {
        for (int i = 0; i < chipIds.length; i++) {
            final int index = i;
            TextView chip = findViewById(chipIds[i]);
            if (chip == null) continue;

            // ─ Default state ─
            chip.setBackgroundColor(Color.WHITE);
            chip.setTextColor(Color.parseColor("#2F5F9B")); // Dark Blue

            chip.setOnClickListener(v -> {
                for (int id : chipIds) {
                    TextView c = findViewById(id);
                    if (c != null) {
                        // Reset all chips to default
                        c.setBackgroundColor(Color.WHITE);
                        c.setTextColor(Color.parseColor("#2F5F9B"));
                    }
                }

                // Selected chip
                chip.setBackgroundColor(Color.parseColor("#2F5F9B")); // Dark Blue
                chip.setTextColor(Color.WHITE);
                selectedChip[0] = index;
            });
        }
    }

    // ── Guest Counter ────────────────────────────
    private void setupGuestCounter() {
        TextView  tvGuests = findViewById(R.id.tvGuestCount);
        ImageView btnMinus = findViewById(R.id.btnGuestMinus);
        ImageView btnPlus  = findViewById(R.id.btnGuestPlus);
        if (tvGuests == null || btnMinus == null || btnPlus == null) return;

        updateGuestLabel(tvGuests);

        btnMinus.setOnClickListener(v -> {
            if (guestCount[0] > 1) { guestCount[0]--; updateGuestLabel(tvGuests); }
            else Toast.makeText(this, "Minimum 1 guest required", Toast.LENGTH_SHORT).show();
        });

        btnPlus.setOnClickListener(v -> {
            if (guestCount[0] < maxGuests) { guestCount[0]++; updateGuestLabel(tvGuests); }
            else Toast.makeText(this,
                    "Maximum capacity is " + maxGuests + " guests", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateGuestLabel(TextView tv) {
        tv.setText(guestCount[0] + (guestCount[0] == 1 ? " Guest" : " Guests"));
    }

    // ── Action Buttons ───────────────────────────
    private void setupActionButtons(String amenityType) {
        MaterialButton btnCancel  = findViewById(R.id.dialogBtnCancel);
        MaterialButton btnConfirm = findViewById(R.id.dialogBtnConfirm);

        if (btnCancel != null) btnCancel.setOnClickListener(v -> finish());

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                if (selectedDate[0].isEmpty()) {
                    Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (selectedChip[0] == -1) {
                    Toast.makeText(this, "Please select a time slot", Toast.LENGTH_SHORT).show();
                    return;
                }

                TextView chipView = findViewById(chipIds[selectedChip[0]]);
                String timeSlot   = chipView != null ? chipView.getText().toString() : "";

                EditText etRequest    = findViewById(R.id.etSpecialRequest);
                String specialRequest = etRequest != null
                        ? etRequest.getText().toString().trim() : "";

                // ── TODO: Save to Firebase ──
                // saveReservation(amenityType, selectedDate[0], timeSlot,
                //                 guestCount[0], specialRequest);

                Toast.makeText(this,
                        "✔ " + amenityType + " booked on " + selectedDate[0] + " at " + timeSlot,
                        Toast.LENGTH_LONG).show();

                finish();
            });
        }
    }
}