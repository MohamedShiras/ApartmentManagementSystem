package com.example.apartmentmanagementsystem.admin.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.apartmentmanagementsystem.R;
import com.example.apartmentmanagementsystem.admin.data.AdminRecordRepository;
import com.example.apartmentmanagementsystem.admin.model.MaintenanceStatus;

public class AdminDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.admin_activity_dashboard);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.adminDashboardRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bindSummary();

        findViewById(R.id.btnManageRecords).setOnClickListener(v ->
                startActivity(new Intent(this, AdminUserRecordsActivity.class))
        );

        findViewById(R.id.btnOpenUserFiles).setOnClickListener(v ->
                startActivity(new Intent(this, AdminUserRecordsActivity.class))
        );

        findViewById(R.id.btnBackToFeed).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindSummary();
    }

    private void bindSummary() {
        AdminRecordRepository repository = AdminRecordRepository.getInstance();

        TextView tvTotal = findViewById(R.id.tvTotalValue);
        TextView tvPending = findViewById(R.id.tvPendingValue);
        TextView tvInProgress = findViewById(R.id.tvInProgressValue);
        TextView tvResolved = findViewById(R.id.tvResolvedValue);

        tvTotal.setText(String.valueOf(repository.getTotalCount()));
        tvPending.setText(String.valueOf(repository.getStatusCount(MaintenanceStatus.PENDING)));
        tvInProgress.setText(String.valueOf(repository.getStatusCount(MaintenanceStatus.IN_PROGRESS)));
        tvResolved.setText(String.valueOf(repository.getStatusCount(MaintenanceStatus.RESOLVED)));
    }
}

