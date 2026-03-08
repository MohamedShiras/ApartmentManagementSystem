package com.example.apartmentmanagementsystem;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ComplaintDetailActivity extends AppCompatActivity {

    private TextView detailStatus;
    private CardView btnConfirmResolve, btnWithdraw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complaint_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

         ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        displayData();
    }

    private void initViews() {
        CardView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        detailStatus      = findViewById(R.id.detailStatus);
        btnConfirmResolve = findViewById(R.id.btnConfirmResolve);
        btnWithdraw       = findViewById(R.id.btnWithdraw);

        btnWithdraw.setOnClickListener(v -> showWithdrawDialog());
        btnConfirmResolve.setOnClickListener(v -> showResolveDialog());
    }

    private void displayData() {
        String category    = getIntent().getStringExtra("category");
        String subject     = getIntent().getStringExtra("subject");
        String date        = getIntent().getStringExtra("date");
        String description = getIntent().getStringExtra("description");
        String status      = getIntent().getStringExtra("status");

        if (category    != null) ((TextView) findViewById(R.id.detailCategory)).setText(category);
        if (subject     != null) ((TextView) findViewById(R.id.detailSubject)).setText(subject);
        if (date        != null) ((TextView) findViewById(R.id.detailDate)).setText("Submitted on " + date);
        if (description != null) ((TextView) findViewById(R.id.detailDescription)).setText(description);
        if (status      != null) detailStatus.setText(status);

        if ("In Progress".equalsIgnoreCase(status)) {
            btnConfirmResolve.setVisibility(View.VISIBLE);
            btnWithdraw.setVisibility(View.VISIBLE);
        } else if ("Resolved".equalsIgnoreCase(status)) {
            btnConfirmResolve.setVisibility(View.GONE);
            btnWithdraw.setVisibility(View.GONE);
        } else {
            btnConfirmResolve.setVisibility(View.GONE);
            btnWithdraw.setVisibility(View.VISIBLE);
        }
    }

    private void showWithdrawDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Withdraw Complaint")
                .setMessage("Are you sure you want to withdraw this complaint?")
                .setPositiveButton("Withdraw", (dialog, which) -> {
                    Toast.makeText(this, "Complaint withdrawn", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showResolveDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Mark as Resolved")
                .setMessage("Has this issue been fixed to your satisfaction?")
                .setPositiveButton("Yes, it's fixed", (dialog, which) -> {
                    detailStatus.setText("Resolved");
                    btnConfirmResolve.setVisibility(View.GONE);
                    btnWithdraw.setVisibility(View.GONE);
                    Toast.makeText(this, "Thank you for confirming!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Not yet", null)
                .show();
    }
}