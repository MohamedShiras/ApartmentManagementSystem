package com.example.apartmentmanagementsystem;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

public class BookingActivity extends AppCompatActivity {

    private static final String TAG = "BookingActivity";
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
        TextView tvDate = findViewById(R.id.tvBookingDate);
        if (tvDate == null) return;

        tvDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        selectedDate[0] = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                        tvDate.setText(selectedDate[0]);
                        tvDate.setTextColor(ContextCompat.getColor(this, android.R.color.black));
                    },
                    year, month, day
            );
            datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());
            datePickerDialog.show();
        });
    }

    private void setupTimeChips() {
        String[] timeSlots = {"10:00 AM", "2:00 PM", "6:00 PM"};

        for (int i = 0; i < chipIds.length; i++) {
            TextView chip = findViewById(chipIds[i]);
            if (chip == null) continue;

            final int index = i;
            chip.setText(timeSlots[i]);
            chip.setOnClickListener(v -> {
                // Reset all chips
                for (int chipId : chipIds) {
                    TextView c = findViewById(chipId);
                    if (c != null) {
                        c.setBackgroundColor(Color.TRANSPARENT);
                        c.setTextColor(ContextCompat.getColor(BookingActivity.this, android.R.color.darker_gray));
                    }
                }

                // Highlight selected chip
                chip.setBackgroundResource(R.drawable.bg_badge_confirmed);
                chip.setTextColor(Color.WHITE);
                selectedChip[0] = index;
            });
        }
    }

    private void setupGuestCounter() {
        TextView tvMinus = findViewById(R.id.tvGuestMinus);
        TextView tvCount = findViewById(R.id.tvGuestCount);
        TextView tvPlus  = findViewById(R.id.tvGuestPlus);

        if (tvCount != null) {
            tvCount.setText(String.valueOf(guestCount[0]));
        }

        if (tvMinus != null) {
            tvMinus.setOnClickListener(v -> {
                if (guestCount[0] > 1) {
                    guestCount[0]--;
                    if (tvCount != null) {
                        tvCount.setText(String.valueOf(guestCount[0]));
                    }
                }
            });
        }

        if (tvPlus != null) {
            tvPlus.setOnClickListener(v -> {
                if (guestCount[0] < maxGuests) {
                    guestCount[0]++;
                    if (tvCount != null) {
                        tvCount.setText(String.valueOf(guestCount[0]));
                    }
                }
            });
        }
    }

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

                // Save booking to Supabase
                saveBookingToSupabase(amenityType, selectedDate[0], timeSlot, guestCount[0], specialRequest);
            });
        }
    }

    private void saveBookingToSupabase(String serviceName, String bookingDate, String bookingTime,
                                       int numberOfGuests, String specialRequest) {
        new Thread(() -> {
            try {
                // Get logged-in user
                SharedPreferences prefs = getSharedPreferences("ApartmentApp", MODE_PRIVATE);
                String bookedBy = prefs.getString("user_email", "guest");

                // Create JSON payload
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("service_name", serviceName);
                jsonObject.put("booking_date", bookingDate);
                jsonObject.put("booking_time", bookingTime);
                jsonObject.put("number_of_guests", String.valueOf(numberOfGuests));
                jsonObject.put("special_request", specialRequest);
                jsonObject.put("booked_by", bookedBy);
                jsonObject.put("status", "pending");

                // Send to Supabase
                URL url = new URL("https://awznxtzjssdajvgfvjvb.supabase.co/rest/v1/bookings");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImF3em54dHpqc3NkYWp2Z2Z2anZiIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzI0NzA4OTAsImV4cCI6MjA0ODA0Njg5MH0.T-qBMDvxqRKFqfG_LXqMiHPLeI0hf6O39d1c6_KqPpc");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Prefer", "return=representation");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonObject.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                conn.disconnect();

                runOnUiThread(() -> {
                    if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                        Toast.makeText(BookingActivity.this,
                                "✔ " + serviceName + " booked successfully on " + bookingDate + " at " + bookingTime,
                                Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(BookingActivity.this,
                                "Error booking. Please try again. (Code: " + responseCode + ")",
                                Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error saving booking: " + e.getMessage(), e);
                runOnUiThread(() ->
                    Toast.makeText(BookingActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }
}