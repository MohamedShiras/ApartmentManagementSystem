package com.example.apartmentmanagementsystem;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class CreateComplaintActivity extends AppCompatActivity {

    private AutoCompleteTextView categoryDropdown;
    private TextInputEditText complaintTitle;
    private TextInputEditText complaintDescription;
    private TextInputLayout categoryLayout, titleLayout, descriptionLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_complaint);

        // Hide the default ActionBar (which shows the App Name)
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize Views
        categoryDropdown = findViewById(R.id.categoryDropdown);
        complaintTitle = findViewById(R.id.complaintTitle);
        complaintDescription = findViewById(R.id.complaintDescription);
        
        categoryLayout = findViewById(R.id.categoryInputLayout);
        titleLayout = findViewById(R.id.titleInputLayout);
        descriptionLayout = findViewById(R.id.descriptionInputLayout);

        // Setup Back Button
        ImageView backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> onBackPressed());

        // Setup Category Dropdown
        String[] categories = {"Plumbing", "Electrical", "Cleaning", "Security", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, categories);
        categoryDropdown.setAdapter(adapter);

        // Setup Submit Button
        MaterialButton submitBtn = findViewById(R.id.submitComplaintBtn);
        submitBtn.setOnClickListener(v -> {
            if (validateInputs()) {
                showSuccessDialog();
            }
        });
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // Reset errors
        categoryLayout.setError(null);
        titleLayout.setError(null);
        descriptionLayout.setError(null);

        if (TextUtils.isEmpty(categoryDropdown.getText())) {
            categoryLayout.setError("Please select a category");
            isValid = false;
        }

        if (TextUtils.isEmpty(complaintTitle.getText())) {
            titleLayout.setError("Please enter a title");
            isValid = false;
        }

        if (TextUtils.isEmpty(complaintDescription.getText())) {
            descriptionLayout.setError("Please enter a description");
            isValid = false;
        }

        return isValid;
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Submission Successful")
                .setMessage("Your complaint has been registered. Our maintenance team will look into it shortly.")
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    finish(); // Go back to Dashboard
                })
                .setCancelable(false)
                .show();
    }
}