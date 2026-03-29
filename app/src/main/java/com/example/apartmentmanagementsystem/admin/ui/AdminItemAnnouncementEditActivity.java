package com.example.apartmentmanagementsystem.admin.ui;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.apartmentmanagementsystem.R;
import com.google.android.material.textfield.TextInputEditText;

public class AdminItemAnnouncementEditActivity extends AppCompatActivity {

    public static final String EXTRA_SENDER = "extra_sender";
    public static final String EXTRA_TIME = "extra_time";
    public static final String EXTRA_TYPE = "extra_type";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_BODY = "extra_body";
    public static final String EXTRA_AUDIENCE = "extra_audience";
    public static final String EXTRA_LIKES = "extra_likes";
    public static final String EXTRA_COMMENTS = "extra_comments";
    public static final String EXTRA_STATUS = "extra_status";

    private TextInputEditText etNoticeSender;
    private TextInputEditText etNoticeType;
    private TextInputEditText etNoticeTitle;
    private TextInputEditText etNoticeBody;
    private TextInputEditText etNoticeAudience;
    private TextInputEditText etNoticeLikes;
    private TextInputEditText etNoticeComments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.admin_item_announcement_edit);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.adminItemAnnouncementEditRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bindViews();
        bindIntentData();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnUpdateNotice).setOnClickListener(v -> updateAnnouncement());
    }

    private void bindViews() {
        etNoticeSender = findViewById(R.id.etNoticeSender);
        etNoticeType = findViewById(R.id.etNoticeType);
        etNoticeTitle = findViewById(R.id.etNoticeTitle);
        etNoticeBody = findViewById(R.id.etNoticeBody);
        etNoticeAudience = findViewById(R.id.etNoticeAudience);
        etNoticeLikes = findViewById(R.id.etNoticeLikes);
        etNoticeComments = findViewById(R.id.etNoticeComments);
    }

    private void bindIntentData() {
        setTextIfPresent(etNoticeSender, getIntent().getStringExtra(EXTRA_SENDER));
        setTextIfPresent(etNoticeType, getIntent().getStringExtra(EXTRA_TYPE));
        setTextIfPresent(etNoticeTitle, getIntent().getStringExtra(EXTRA_TITLE));
        setTextIfPresent(etNoticeBody, getIntent().getStringExtra(EXTRA_BODY));
        setTextIfPresent(etNoticeAudience, getIntent().getStringExtra(EXTRA_AUDIENCE));
        setTextIfPresent(etNoticeLikes, getIntent().getStringExtra(EXTRA_LIKES));
        setTextIfPresent(etNoticeComments, getIntent().getStringExtra(EXTRA_COMMENTS));
    }

    private void setTextIfPresent(TextInputEditText field, String value) {
        if (value != null && !value.trim().isEmpty()) {
            field.setText(value.trim());
        }
    }

    private void updateAnnouncement() {
        if (textOf(etNoticeSender).isEmpty() || textOf(etNoticeTitle).isEmpty() || textOf(etNoticeBody).isEmpty()) {
            Toast.makeText(this, "Please fill sender, title and body", Toast.LENGTH_SHORT).show();
            return;
        }

        if (textOf(etNoticeLikes).isEmpty()) {
            etNoticeLikes.setText("0");
        }
        if (textOf(etNoticeComments).isEmpty()) {
            etNoticeComments.setText("0");
        }

        Toast.makeText(this, "Selected announcement updated", Toast.LENGTH_SHORT).show();
        finish();
    }

    private String textOf(TextInputEditText field) {
        if (field.getText() == null) {
            return "";
        }
        return field.getText().toString().trim();
    }
}

