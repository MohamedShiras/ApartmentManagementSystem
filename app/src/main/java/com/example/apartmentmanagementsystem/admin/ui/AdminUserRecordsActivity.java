package com.example.apartmentmanagementsystem.admin.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apartmentmanagementsystem.R;
import com.example.apartmentmanagementsystem.admin.adapter.AdminRecordAdapter;
import com.example.apartmentmanagementsystem.admin.data.AdminRecordRepository;

public class AdminUserRecordsActivity extends AppCompatActivity {

    public static final String EXTRA_RECORD_ID = "extra_record_id";

    private AdminRecordRepository repository;
    private AdminRecordAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.admin_activity_user_maintenance_records);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.adminRecordsRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        repository = AdminRecordRepository.getInstance();
        setupRecycler();
        setupSearch();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnRefresh).setOnClickListener(v -> adapter.submitList(repository.getAllRecords()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.submitList(repository.getAllRecords());
    }

    private void setupRecycler() {
        RecyclerView recyclerView = findViewById(R.id.recordsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AdminRecordAdapter(record -> {
            Intent intent = new Intent(this, AdminRecordDetailActivity.class);
            intent.putExtra(EXTRA_RECORD_ID, record.getRecordId());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
        adapter.submitList(repository.getAllRecords());
    }

    private void setupSearch() {
        EditText searchInput = findViewById(R.id.etSearch);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.submitList(repository.search(String.valueOf(s)));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }
}
