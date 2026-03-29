package com.example.apartmentmanagementsystem;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.Locale;

public class CreateMaintenanceIssueActivity extends AppCompatActivity {

    private static final String DEFAULT_CATEGORY = "Electrical";

    private TextInputEditText etTitle;
    private TextInputEditText etLocation;
    private TextInputEditText etDate;
    private TextInputEditText etTime;
    private TextInputEditText etDescription;

    private TextView chipLow;
    private TextView chipMedium;
    private TextView chipHigh;

    private TextView chipElectrical;
    private TextView chipMechanical;
    private TextView chipPlumbing;
    private TextView tvAttachmentStatus;

    private ActivityResultLauncher<Void> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;

    private String selectedPriority;
    private String selectedCategory;

    private Uri selectedAttachmentUri;
    private boolean hasAttachment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_maintenance_issue);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        bindViews();
        setupTopBar();
        setupDateTimePickers();
        setupChipSelection();
        setupAttachmentPickers();
        prefillFromIntent();
        setDefaults();
        setupActions();
    }

    private void bindViews() {
        etTitle = findViewById(R.id.etTitle);
        etLocation = findViewById(R.id.etLocation);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etDescription = findViewById(R.id.etDescription);

        chipLow = findViewById(R.id.chipLow);
        chipMedium = findViewById(R.id.chipMedium);
        chipHigh = findViewById(R.id.chipHigh);

        chipElectrical = findViewById(R.id.chipElectrical);
        chipMechanical = findViewById(R.id.chipMechanical);
        chipPlumbing = findViewById(R.id.chipPlumbing);
        tvAttachmentStatus = findViewById(R.id.tvAttachmentStatus);
    }

    private void setupTopBar() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageButton btnSaveDraft = findViewById(R.id.btnSaveDraft);

        btnBack.setOnClickListener(v -> finish());
        btnSaveDraft.setOnClickListener(v -> Toast.makeText(this, "Draft saved", Toast.LENGTH_SHORT).show());
    }

    private void setupDateTimePickers() {
        etDate.setOnClickListener(v -> showDatePicker());
        etTime.setOnClickListener(v -> showTimePicker());
    }

    private void setupChipSelection() {
        chipLow.setOnClickListener(v -> selectPriority("Low"));
        chipMedium.setOnClickListener(v -> selectPriority("Medium"));
        chipHigh.setOnClickListener(v -> selectPriority("High"));

        chipElectrical.setOnClickListener(v -> selectCategory("Electrical"));
        chipMechanical.setOnClickListener(v -> selectCategory("Mechanical"));
        chipPlumbing.setOnClickListener(v -> selectCategory("Plumbing"));
    }

    private void setupAttachmentPickers() {
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), this::onCameraResult);
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), this::onGalleryResult);
    }

    private void prefillFromIntent() {
        Intent intent = getIntent();
        String serviceTitle = intent.getStringExtra(MaintenanceActivity.EXTRA_SERVICE_TITLE);
        TextView tvSubtitle = findViewById(R.id.tvSubtitle);
        TextView tvCategoryHint = findViewById(R.id.tvCategoryHint);

        if (serviceTitle == null || serviceTitle.trim().isEmpty()) {
            tvSubtitle.setText("Create a new maintenance record");
            tvCategoryHint.setText("Select service type");
            return;
        }

        tvSubtitle.setText(serviceTitle);
        tvCategoryHint.setText("Suggested service: " + serviceTitle);

        String normalizedTitle = serviceTitle.toLowerCase(Locale.US);
        if (normalizedTitle.contains("plumb")) {
            selectCategory("Plumbing");
        } else if (normalizedTitle.contains("mechan") || normalizedTitle.contains("ac") || normalizedTitle.contains("gas")) {
            selectCategory("Mechanical");
        } else {
            selectCategory("Electrical");
        }
    }

    private void setDefaults() {
        if (selectedCategory == null) {
            selectCategory(DEFAULT_CATEGORY);
        }
        selectPriority("Medium");
    }

    private void setupActions() {
        MaterialButton btnSubmit = findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(v -> submitRequest());

        findViewById(R.id.layoutAddAttachment).setOnClickListener(v -> galleryLauncher.launch("image/*"));
        findViewById(R.id.btnCameraOption).setOnClickListener(v -> cameraLauncher.launch(null));
        findViewById(R.id.btnGalleryOption).setOnClickListener(v -> galleryLauncher.launch("image/*"));
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> etDate.setText(String.format(Locale.US, "%02d/%02d/%04d", dayOfMonth, month + 1, year)),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> etTime.setText(String.format(Locale.US, "%02d:%02d", hourOfDay, minute)),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        );
        dialog.show();
    }

    private void selectPriority(String priority) {
        selectedPriority = priority;
        resetPriorityChips();

        if ("Low".equals(priority)) {
            chipLow.setAlpha(1f);
            chipMedium.setAlpha(0.5f);
            chipHigh.setAlpha(0.5f);
        } else if ("Medium".equals(priority)) {
            chipLow.setAlpha(0.5f);
            chipMedium.setAlpha(1f);
            chipHigh.setAlpha(0.5f);
        } else {
            chipLow.setAlpha(0.5f);
            chipMedium.setAlpha(0.5f);
            chipHigh.setAlpha(1f);
        }
    }

    private void resetPriorityChips() {
        chipLow.setAlpha(1f);
        chipMedium.setAlpha(1f);
        chipHigh.setAlpha(1f);
    }

    private void selectCategory(String category) {
        selectedCategory = category;

        chipElectrical.setSelected(false);
        chipMechanical.setSelected(false);
        chipPlumbing.setSelected(false);

        if ("Electrical".equals(category)) {
            chipElectrical.setSelected(true);
        } else if ("Mechanical".equals(category)) {
            chipMechanical.setSelected(true);
        } else {
            chipPlumbing.setSelected(true);
        }

        updateCategoryChipState(chipElectrical);
        updateCategoryChipState(chipMechanical);
        updateCategoryChipState(chipPlumbing);
    }

    private void updateCategoryChipState(TextView chip) {
        if (chip.isSelected()) {
            chip.setBackgroundResource(R.drawable.bg_banner_dark);
            chip.setTextColor(0xFFFFFFFF);
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_outline);
            chip.setTextColor(0xFF0F274A);
        }
    }

    private void submitRequest() {
        String title = valueOrEmpty(etTitle);
        String location = valueOrEmpty(etLocation);
        String date = valueOrEmpty(etDate);
        String time = valueOrEmpty(etTime);
        String description = valueOrEmpty(etDescription);

        if (title.isEmpty() || location.isEmpty() || date.isEmpty() || time.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedPriority == null || selectedCategory == null) {
            Toast.makeText(this, "Please select category and priority", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hasAttachment) {
            tvAttachmentStatus.setText("No media selected (optional)");
        }

        Toast.makeText(this, "Maintenance issue created successfully", Toast.LENGTH_SHORT).show();
        finish();
    }

    private String valueOrEmpty(TextInputEditText field) {
        if (field.getText() == null) {
            return "";
        }
        return field.getText().toString().trim();
    }

    private void onCameraResult(Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        selectedAttachmentUri = null;
        hasAttachment = true;
        tvAttachmentStatus.setText("Camera photo attached");
    }

    private void onGalleryResult(Uri uri) {
        if (uri == null) {
            return;
        }
        selectedAttachmentUri = uri;
        hasAttachment = true;
        tvAttachmentStatus.setText("Gallery image attached");
    }
}
