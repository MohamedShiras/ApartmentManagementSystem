package com.example.apartmentmanagementsystem;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import android.view.animation.OvershootInterpolator;

public class PostActivity extends AppCompatActivity {

    // Views
    private EditText       etCaption;
    private TextView       tvCharCount;
    private FrameLayout    frameImagePreview;   // container for image — GONE until image picked
    private ImageView      imgMediaPreview;     // the actual image
    private ImageView      ivPlayOverlay;
    private ImageButton    btnRemoveMedia;
    private MaterialButton btnPost;
    private CardView       cardThumb;
    private ImageView      imgThumb;

    // State
    private Uri     selectedMediaUri = null;
    private boolean isPosting        = false;

    private PostRepository postRepository;

    private TextView       tvUserName, tvUserUnit, tvAvatarInitials;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) handleSelectedImage(uri);
            });

    private final ActivityResultLauncher<String> imagePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) imagePickerLauncher.launch("image/*");
                else Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show();
            });

    private final ActivityResultLauncher<String[]> anyFileLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) handleSelectedFile(uri);
            });

    private final ActivityResultLauncher<String> storagePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) openAddMoreSheet();
                else Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show();
            });

    // ══════════════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_post);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        postRepository = new PostRepository();

        View root = findViewById(R.id.activity_post);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
                return insets;
            });
        }

        bindViews();
        setupCharCounter();
        setupClickListeners();
    }

    private void bindViews() {
        etCaption         = findViewById(R.id.etCaption);
        tvCharCount       = findViewById(R.id.tvCharCount);
        frameImagePreview = findViewById(R.id.frameImagePreview);
        imgMediaPreview   = findViewById(R.id.imgMediaPreview);
        ivPlayOverlay     = findViewById(R.id.ivPlayOverlay);
        btnRemoveMedia    = findViewById(R.id.btnRemoveMedia);
        btnPost           = findViewById(R.id.btnPost);
        tvUserName        = findViewById(R.id.tvUserName);
        tvUserUnit        = findViewById(R.id.tvUserUnit);
        tvAvatarInitials  = findViewById(R.id.tvAvatarInitials);

        loadUserData();

    }

    private void setupCharCounter() {
        if (etCaption == null || tvCharCount == null) return;
        etCaption.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                int n = s.length();
                tvCharCount.setText(n + " / 500");
                tvCharCount.setTextColor(n >= 450
                        ? Color.parseColor("#D32F2F")
                        : Color.parseColor("#C0C8DC"));
            }
        });
    }

    private void setupClickListeners() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> navigateBack());

        LinearLayout optionPhoto = findViewById(R.id.optionPhoto);
        if (optionPhoto != null) optionPhoto.setOnClickListener(v -> requestImagePermissionAndPick());

        MaterialButton btnAddMore = findViewById(R.id.btnAddMore);
        if (btnAddMore != null) btnAddMore.setOnClickListener(v -> requestStoragePermissionAndShowSheet());

        if (btnRemoveMedia != null) btnRemoveMedia.setOnClickListener(v -> clearMediaSelection());
        if (btnPost        != null) btnPost.setOnClickListener(v -> attemptPost());
    }

    // ══════════════════════════════════════════════════════════════════
    //  IMAGE SELECTION — shows inside the write card
    // ══════════════════════════════════════════════════════════════════

    private void handleSelectedImage(Uri uri) {
        selectedMediaUri = uri;

        // Make preview visible with smooth animation
        if (frameImagePreview != null) {
            frameImagePreview.setAlpha(0f);
            frameImagePreview.setScaleY(0.9f);
            frameImagePreview.setVisibility(View.VISIBLE);
            frameImagePreview.animate()
                    .alpha(1f)
                    .scaleY(1f)
                    .setDuration(280)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();
        }

        // Load image into preview
        if (imgMediaPreview != null) {
            Glide.with(this)
                    .load(uri)
                    .centerCrop()
                    .into(imgMediaPreview);
        }

        if (ivPlayOverlay != null) ivPlayOverlay.setVisibility(View.GONE);

        // Bottom bar small thumbnail
        if (cardThumb != null) cardThumb.setVisibility(View.VISIBLE);
        if (imgThumb  != null) Glide.with(this).load(uri).centerCrop().into(imgThumb);
    }

    private void handleSelectedFile(Uri uri) {
        try {
            getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {}

        selectedMediaUri = uri;
        String mimeType = getContentResolver().getType(uri);
        boolean isVideo = mimeType != null && mimeType.startsWith("video");

        if (frameImagePreview != null) {
            frameImagePreview.setAlpha(0f);
            frameImagePreview.setVisibility(View.VISIBLE);
            frameImagePreview.animate().alpha(1f).setDuration(250).start();
        }
        if (imgMediaPreview != null) Glide.with(this).load(uri).centerCrop().into(imgMediaPreview);
        if (ivPlayOverlay   != null) ivPlayOverlay.setVisibility(isVideo ? View.VISIBLE : View.GONE);
        if (cardThumb       != null) cardThumb.setVisibility(View.VISIBLE);
        if (imgThumb        != null) Glide.with(this).load(uri).centerCrop().into(imgThumb);
    }

    private void clearMediaSelection() {
        selectedMediaUri = null;
        if (frameImagePreview != null) {
            frameImagePreview.animate()
                    .alpha(0f).scaleY(0.9f).setDuration(200)
                    .withEndAction(() -> {
                        frameImagePreview.setVisibility(View.GONE);
                        frameImagePreview.setScaleY(1f);
                        if (imgMediaPreview != null) imgMediaPreview.setImageDrawable(null);
                    }).start();
        }
        if (ivPlayOverlay != null) ivPlayOverlay.setVisibility(View.GONE);
        if (cardThumb     != null) cardThumb.setVisibility(View.GONE);
        if (imgThumb      != null) imgThumb.setImageDrawable(null);
    }


    private void attemptPost() {
        if (isPosting) return;

        String caption = etCaption != null ? etCaption.getText().toString().trim() : "";

        android.content.SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String fullName = prefs.getString("temp_full_name", "Resident");
        String unitInfo = prefs.getString("temp_unit_info", "General Unit");

        if (caption.isEmpty() && selectedMediaUri == null) {
            Toast.makeText(this, "Write something or add a photo first.", Toast.LENGTH_SHORT).show();
            return;
        }

        setPostingState(true);

        postRepository.createPost(
                this,
                getUserId(),
                fullName,
                unitInfo,
                caption,
                selectedMediaUri,
                new PostRepository.PostCallback() {
                    @Override
                    public void onSuccess(String postId) {
                        setPostingState(false);
                        showSuccessDialog();
                    }

                    @Override
                    public void onError(String msg) {
                        setPostingState(false);
                        Toast.makeText(PostActivity.this, "Post failed: " + msg, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }


    private void showSuccessDialog() {
        View dv = getLayoutInflater().inflate(R.layout.dialog_post_success, null);

        AlertDialog dialog = new AlertDialog.Builder(this, 0)
                .setView(dv).setCancelable(false).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setGravity(Gravity.CENTER);
        }
        dialog.show();

        View circle = dv.findViewById(R.id.successCircle);
        View check  = dv.findViewById(R.id.successCheck);
        View msg    = dv.findViewById(R.id.tvSuccessMsg);
        View sub    = dv.findViewById(R.id.tvSuccessSub);

        circle.setAlpha(0f); circle.setScaleX(0.3f); circle.setScaleY(0.3f);
        check.setAlpha(0f);  msg.setAlpha(0f);        sub.setAlpha(0f);

        ObjectAnimator sx = ObjectAnimator.ofFloat(circle, "scaleX", 0.3f, 1.15f, 1f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(circle, "scaleY", 0.3f, 1.15f, 1f);
        ObjectAnimator fa = ObjectAnimator.ofFloat(circle, "alpha",  0f,   1f);
        sx.setDuration(450); sy.setDuration(450); fa.setDuration(300);
        sx.setInterpolator(new OvershootInterpolator(2.5f));
        sy.setInterpolator(new OvershootInterpolator(2.5f));

        ObjectAnimator cf = ObjectAnimator.ofFloat(check, "alpha", 0f, 1f);
        cf.setStartDelay(360); cf.setDuration(200);

        ObjectAnimator mf = ObjectAnimator.ofFloat(msg, "alpha", 0f, 1f);
        ObjectAnimator sf = ObjectAnimator.ofFloat(sub, "alpha", 0f, 1f);
        mf.setStartDelay(520); mf.setDuration(280);
        sf.setStartDelay(640); sf.setDuration(280);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(sx, sy, fa, cf, mf, sf);
        set.start();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing()) { dialog.dismiss(); navigateBack(); }
        }, 2200);
    }

    private void requestImagePermissionAndPick() {
        String p = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED)
            imagePickerLauncher.launch("image/*");
        else
            imagePermissionLauncher.launch(p);
    }

    private void requestStoragePermissionAndShowSheet() {
        String p = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED)
            openAddMoreSheet();
        else
            storagePermissionLauncher.launch(p);
    }

    private void openAddMoreSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        sheet.setContentView(R.layout.add_more);

        View img = sheet.findViewById(R.id.sheetOptionImage);
        View doc = sheet.findViewById(R.id.sheetOptionDocument);
        View vid = sheet.findViewById(R.id.sheetOptionVideo);

        if (img != null) img.setOnClickListener(v -> { imagePickerLauncher.launch("image/*"); sheet.dismiss(); });
        if (doc != null) doc.setOnClickListener(v -> {
            anyFileLauncher.launch(new String[]{
                    "application/pdf","application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"});
            sheet.dismiss();
        });
        if (vid != null) vid.setOnClickListener(v -> { anyFileLauncher.launch(new String[]{"video/*"}); sheet.dismiss(); });

        sheet.show();
    }


    private void setPostingState(boolean posting) {
        isPosting = posting;
        if (btnPost == null) return;
        btnPost.setEnabled(!posting);
        btnPost.setText(posting ? "Posting…" : "Post");
        btnPost.setAlpha(posting ? 0.6f : 1f);
    }

    private void navigateBack() {
        Intent i = new Intent(this, FeedActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    private String getUserId() {
        String id = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("user_id", null);
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
            getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putString("user_id", id).apply();
        }
        return id;
    }
    private void loadUserData() {

        String currentUserId = getSharedPreferences("LoginPrefs", MODE_PRIVATE).getString("user_id", null);

        if (currentUserId == null) return;

        new Thread(() -> {
            try {

                String queryUrl = SupabaseClient.SUPABASE_URL
                        + "/rest/v1/users?id=eq." + currentUserId
                        + "&select=full_name,apartment_number,block";

                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(queryUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SupabaseClient.SUPABASE_ANON_KEY);

                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                org.json.JSONArray arr = new org.json.JSONArray(sb.toString());

                if (arr.length() > 0) {
                    org.json.JSONObject userObj = arr.getJSONObject(0);
                    String name = userObj.optString("full_name", "User");
                    String aptNo = userObj.optString("apartment_number", "");
                    String block = userObj.optString("block", "");
                    String unitDisplay = "Unit " + aptNo + " · Block " + block;

                    runOnUiThread(() -> {
                        if (tvUserName != null) tvUserName.setText(name);
                        if (tvUserUnit != null) tvUserUnit.setText(unitDisplay);

                        // Avatar Initials
                        if (tvAvatarInitials != null && !name.isEmpty()) {
                            String initials = "";
                            String[] parts = name.split(" ");
                            if (parts.length > 0) initials += parts[0].toUpperCase().charAt(0);
                            if (parts.length > 1) initials += parts[1].toUpperCase().charAt(0);
                            tvAvatarInitials.setText(initials);
                        }
                    });

                    getSharedPreferences("LoginPrefs", MODE_PRIVATE).edit()
                            .putString("temp_full_name", name)
                            .putString("temp_unit_info", unitDisplay)
                            .apply();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}