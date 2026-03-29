package com.example.apartmentmanagementsystem.admin.ui;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.apartmentmanagementsystem.R;
import com.example.apartmentmanagementsystem.admin.data.AdminRecordRepository;
import com.example.apartmentmanagementsystem.admin.model.MaintenanceStatus;
import com.example.apartmentmanagementsystem.admin.model.UserMaintenanceRecord;

public class AdminRecordDetailActivity extends AppCompatActivity {

    private String recordId;
    private AdminRecordRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.admin_activity_record_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.adminRecordDetailRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        repository = AdminRecordRepository.getInstance();
        recordId = getIntent().getStringExtra(AdminUserRecordsActivity.EXTRA_RECORD_ID);

        bindRecord();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnPending).setOnClickListener(v -> updateStatus(MaintenanceStatus.PENDING));
        findViewById(R.id.btnInProgress).setOnClickListener(v -> updateStatus(MaintenanceStatus.IN_PROGRESS));
        findViewById(R.id.btnResolved).setOnClickListener(v -> updateStatus(MaintenanceStatus.RESOLVED));
    }

    private void bindRecord() {
        UserMaintenanceRecord record = repository.getById(recordId);
        if (record == null) {
            Toast.makeText(this, "Record not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ((TextView) findViewById(R.id.tvRecordId)).setText(record.getRecordId());
        ((TextView) findViewById(R.id.tvIssueTitle)).setText(record.getIssueSummary());
        ((TextView) findViewById(R.id.tvResident)).setText(record.getResidentName());
        ((TextView) findViewById(R.id.tvUnit)).setText(record.getUnit());
        ((TextView) findViewById(R.id.tvMaintenanceType)).setText(record.getMaintenanceType());
        ((TextView) findViewById(R.id.tvDate)).setText(record.getSubmittedDate());
        ((TextView) findViewById(R.id.tvPriority)).setText(record.getPriority().name());
        ((TextView) findViewById(R.id.tvStatus)).setText(formatStatus(record.getStatus()));
        ((TextView) findViewById(R.id.tvDescription)).setText(record.getDescription());
    }

    private void updateStatus(MaintenanceStatus status) {
        repository.updateStatus(recordId, status);
        bindRecord();
        Toast.makeText(this, "Status updated", Toast.LENGTH_SHORT).show();
    }

    private String formatStatus(MaintenanceStatus status) {
        switch (status) {
            case IN_PROGRESS:
                return "In Progress";
            case RESOLVED:
                return "Resolved";
            case OVERDUE:
                return "Overdue";
            default:
                return "Pending";
        }
    }
}

