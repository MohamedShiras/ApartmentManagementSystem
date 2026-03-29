package com.example.apartmentmanagementsystem.admin.ui;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.apartmentmanagementsystem.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class AdminItemAnnouncementActivity extends AppCompatActivity {

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
    private TextInputEditText etNoticeBadge;
    private TextInputEditText etNoticeTitle;
    private TextInputEditText etNoticeBody;
    private TextInputEditText etNoticeComments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.admin_item_announcement);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.adminItemAnnouncementFormRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bindViews();
        bindIntentData();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        MaterialButton btnSaveNotice = findViewById(R.id.btnSaveNotice);
        MaterialButton btnResetNotice = findViewById(R.id.btnResetNotice);

        btnSaveNotice.setOnClickListener(v -> saveNoticeItem());
        btnResetNotice.setOnClickListener(v -> resetForm());
    }

    private void bindViews() {
        etNoticeSender = findViewById(R.id.etNoticeSender);
        etNoticeBadge = findViewById(R.id.etNoticeBadge);
        etNoticeTitle = findViewById(R.id.etNoticeTitle);
        etNoticeBody = findViewById(R.id.etNoticeBody);
        etNoticeComments = findViewById(R.id.etNoticeComments);
    }

    private void bindIntentData() {
        setTextIfPresent(etNoticeSender, getIntent().getStringExtra(EXTRA_SENDER));
        setTextIfPresent(etNoticeTitle, getIntent().getStringExtra(EXTRA_TITLE));
        setTextIfPresent(etNoticeBody, getIntent().getStringExtra(EXTRA_BODY));

        String type = getIntent().getStringExtra(EXTRA_TYPE);
        if (type != null && !type.trim().isEmpty()) {
            etNoticeBadge.setText(type + " Notice");
        }

        setNumericIfPresent(etNoticeComments, getIntent().getStringExtra(EXTRA_COMMENTS));
    }

    private void setTextIfPresent(TextInputEditText field, String value) {
        if (value != null && !value.trim().isEmpty()) {
            field.setText(value.trim());
        }
    }

    private void setNumericIfPresent(TextInputEditText field, String value) {
        if (value == null) {
            return;
        }

        String numeric = value.replaceAll("[^0-9]", "");
        if (!numeric.isEmpty()) {
            field.setText(numeric);
        }
    }

    private void saveNoticeItem() {
        String sender = textOf(etNoticeSender);
        String badge = textOf(etNoticeBadge);
        String title = textOf(etNoticeTitle);
        String body = textOf(etNoticeBody);

        if (sender.isEmpty() || badge.isEmpty() || title.isEmpty() || body.isEmpty()) {
            Toast.makeText(this, "Please fill sender, badge, title and body", Toast.LENGTH_SHORT).show();
            return;
        }

        if (textOf(etNoticeComments).isEmpty()) {
            etNoticeComments.setText("0");
        }

        Toast.makeText(this, "Notice item details saved", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void resetForm() {
        etNoticeSender.setText("");
        etNoticeBadge.setText("");
        etNoticeTitle.setText("");
        etNoticeBody.setText("");
        etNoticeComments.setText("");
    }

    private String textOf(TextInputEditText field) {
        if (field.getText() == null) {
            return "";
        }
        return field.getText().toString().trim();
    }
}
