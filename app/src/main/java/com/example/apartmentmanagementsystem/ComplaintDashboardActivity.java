package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class ComplaintDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complaint_dashboard);
        
        // Hide the default ActionBar (which shows the App Name)
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Setup Back Button
        ImageView backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // Setup "New Complaint" FAB
        ExtendedFloatingActionButton addComplaintFab = findViewById(R.id.addComplaintFab);
        addComplaintFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ComplaintDashboardActivity.this, CreateComplaintActivity.class);
                startActivity(intent);
            }
        });
    }
}