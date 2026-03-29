package com.example.apartmentmanagementsystem;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class FileComplaintActivity extends AppCompatActivity {

    private AutoCompleteTextView autoCompleteCategory;
    private TextInputEditText editSubject, editDescription;
    private MaterialButton btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_complaint);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        autoCompleteCategory = findViewById(R.id.autoCompleteCategory);
        editSubject = findViewById(R.id.editSubject);
        editDescription = findViewById(R.id.editDescription);
        btnSubmit = findViewById(R.id.btnSubmit);

        // Setup Categories
        String[] categories = {"Plumbing", "Electrical", "Cleaning", "Security", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        autoCompleteCategory.setAdapter(adapter);

        btnSubmit.setOnClickListener(v -> {
            submitComplaint();
        });
    }

    private void submitComplaint() {
        String category = autoCompleteCategory.getText().toString();
        String subject = editSubject.getText().toString().trim();
        String description = editDescription.getText().toString().trim();

        if (category.isEmpty() || subject.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Logic to save the maintenance issue would go here
        Toast.makeText(this, "Maintenance issue created successfully", Toast.LENGTH_SHORT).show();
        finish();
    }
}
