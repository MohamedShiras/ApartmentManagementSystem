package com.example.apartmentmanagementsystem.admin.ui;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.apartmentmanagementsystem.R;

public class AdminCreateAnnouncementActivity extends AppCompatActivity {

    private EditText etTitle;
    private EditText etBody;
    private Spinner spinnerType;
    private Spinner spinnerAudience;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.admin_activity_create_announcement);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.createAnnouncementRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bindViews();
        setupSpinners();
        setupActions();
    }

    private void bindViews() {
        etTitle = findViewById(R.id.etTitle);
        etBody = findViewById(R.id.etBody);
        spinnerType = findViewById(R.id.spinnerType);
        spinnerAudience = findViewById(R.id.spinnerAudience);
    }

    private void setupSpinners() {
        String[] types = {"Notice", "Alert", "Info", "Event", "Update"};
        String[] audiences = {"All Residents", "Tower A", "Tower B", "Maintenance Team", "Owners Only"};

        spinnerType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types));
        spinnerAudience.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, audiences));
    }

    private void setupActions() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnSaveDraft).setOnClickListener(v -> {
            if (!validateFields(false)) {
                return;
            }
            Toast.makeText(this, "Draft saved", Toast.LENGTH_SHORT).show();
            finish();
        });

        findViewById(R.id.btnPublish).setOnClickListener(v -> {
            if (!validateFields(true)) {
                return;
            }
            Toast.makeText(this, "Announcement published", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private boolean validateFields(boolean requireBody) {
        String title = etTitle.getText() == null ? "" : etTitle.getText().toString().trim();
        String body = etBody.getText() == null ? "" : etBody.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (requireBody && body.isEmpty()) {
            Toast.makeText(this, "Please enter a description", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }
}

