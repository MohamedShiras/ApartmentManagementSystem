package com.example.apartmentmanagementsystem;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ProfileActivity extends AppCompatActivity {

    private TextView avatarInitialsTop;
    private TextView avatarInitials, residentName, residentUnit, memberSince;
    private TextView statPosts, statRequests, statTenure;
    private TextView phoneValue, emailValue;
    private TextView leaseExpiryValue, rentDueValue;

    private String token  = "";
    private String userId = "";

    // Image Picker Launcher
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // ── Load session ──────────────────────────────────────────────────────
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        token  = prefs.getString("access_token", "");
        userId = prefs.getString("user_id",       "");

        // ── Initialize Image Picker ───────────────────────────────────────────
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            uploadProfilePicture(selectedImageUri);
                        }
                    }
                }
        );

        // ── Insets ────────────────────────────────────────────────────────────
        CoordinatorLayout root = findViewById(R.id.profile);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, 0);
            return insets;
        });

        android.widget.RelativeLayout bottomNav = findViewById(R.id.bottom_nav_container);
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            CoordinatorLayout.LayoutParams lp =
                    (CoordinatorLayout.LayoutParams) v.getLayoutParams();
            lp.bottomMargin = bars.bottom;
            v.setLayoutParams(lp);
            return insets;
        });

        // ── Scroll inset: content clears nav bar ──────────────────────────────
        NestedScrollView scrollView = findViewById(R.id.scrollView);
        ViewCompat.setOnApplyWindowInsetsListener(scrollView, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, bars.bottom + dp(90));
            return insets;
        });

        initViews();
        setupBottomNavigation();
        setupQuickActions();
        loadProfileFromSupabase();
    }

    // ── Bind views ────────────────────────────────────────────────────────────
    private void initViews() {
        avatarInitialsTop = findViewById(R.id.avatarInitialsTop);
        avatarInitials    = findViewById(R.id.avatarInitials);
        residentName      = findViewById(R.id.residentName);
        residentUnit      = findViewById(R.id.residentUnit);
        memberSince       = findViewById(R.id.memberSince);
        statPosts         = findViewById(R.id.statPosts);
        statRequests      = findViewById(R.id.statRequests);
        statTenure        = findViewById(R.id.statTenure);
        phoneValue        = findViewById(R.id.phoneValue);
        emailValue        = findViewById(R.id.emailValue);
        leaseExpiryValue  = findViewById(R.id.leaseExpiryValue);
        rentDueValue      = findViewById(R.id.rentDueValue);

        // ── Avatar Edit Button Listener ───────────────────────────────────────
        CardView editAvatarBtn = findViewById(R.id.editAvatarBtn);
        editAvatarBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });
    }

    // ── Load profile from Supabase ────────────────────────────────────────────
    private void loadProfileFromSupabase() {
        if (token.isEmpty() || userId.isEmpty()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        new Thread(() -> {
            try {
                String queryUrl = SupabaseClient.SUPABASE_URL + "/rest/v1/users"
                        + "?id=eq." + userId
                        + "&select=full_name,email,apartment_number,block,"
                        + "phone,member_since,lease_expiry,rent_amount,"
                        + "posts_count,requests_count,tenure,avatar_url"
                        + "&limit=1";

                HttpURLConnection conn = (HttpURLConnection)
                        new URL(queryUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + token);

                int code = conn.getResponseCode();
                if (code != 200) return;

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                conn.disconnect();

                JSONArray arr = new JSONArray(sb.toString());
                if (arr.length() == 0) return;

                JSONObject u = arr.getJSONObject(0);

                String fullName  = u.optString("full_name",        "Resident");
                String email     = u.optString("email",            "—");
                String aptNumber = u.optString("apartment_number", "—");
                String block     = u.optString("block",            "");
                String phone     = u.optString("phone",            "—");
                String since     = u.optString("member_since",     "—");
                String lease     = u.optString("lease_expiry",     "—");
                String rent      = u.optString("rent_amount",      "—");
                String posts     = String.valueOf(u.optInt("posts_count",    0));
                String requests  = String.valueOf(u.optInt("requests_count", 0));
                String tenure    = u.optString("tenure", "—");
                String avatarUrl = u.optString("avatar_url", "");

                String initials   = buildInitials(fullName);
                String unitLabel  = "Unit " + aptNumber
                        + (block.isEmpty() ? "" : " · Block " + block);
                String sinceLabel = "Member since " + since;
                String rentLabel  = rent + " · Next Due";

                runOnUiThread(() -> {
                    avatarInitialsTop.setText(initials);
                    avatarInitials.setText(initials);
                    residentName.setText(fullName);
                    residentUnit.setText(unitLabel);
                    memberSince.setText(sinceLabel);
                    if (statPosts    != null) statPosts.setText(posts);
                    if (statRequests != null) statRequests.setText(requests);
                    if (statTenure   != null) statTenure.setText(tenure);
                    phoneValue.setText(phone);
                    emailValue.setText(email);
                    leaseExpiryValue.setText(lease);
                    rentDueValue.setText(rentLabel);
                });

                // Load avatar if exists
                if (!avatarUrl.isEmpty() && !avatarUrl.equals("null")) {
                    loadAvatarImage(avatarUrl);
                }

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Failed to load profile", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ── Upload Avatar Image to Supabase Storage ───────────────────────────────
    private void uploadProfilePicture(Uri imageUri) {
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                // 1. Read bytes from Uri
                InputStream is = getContentResolver().openInputStream(imageUri);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                byte[] imageBytes = baos.toByteArray();
                is.close();

                // 2. Upload to Supabase Storage (Bucket: "avatars")
                String fileName = userId + "_" + System.currentTimeMillis() + ".jpg";

                // Safely format the URL to prevent double-slashes
                String baseUrl = SupabaseClient.SUPABASE_URL;
                if (baseUrl.endsWith("/")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                }
                String uploadUrl = baseUrl + "/storage/v1/object/avatars/" + fileName;

                HttpURLConnection conn = (HttpURLConnection) new URL(uploadUrl).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "image/jpeg");
                conn.setDoOutput(true);

                // CRITICAL FIX: Tell the connection exactly how big the file is.
                // This prevents Android from sending a "chunked" request which causes the 400 error.
                conn.setFixedLengthStreamingMode(imageBytes.length);

                OutputStream os = conn.getOutputStream();
                os.write(imageBytes);
                os.flush(); // Ensure all bytes are pushed
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    // 3. Get Public URL
                    String publicUrl = baseUrl + "/storage/v1/object/public/avatars/" + fileName;

                    // 4. Update users table avatar_url
                    updateUserAvatarUrl(publicUrl, imageBytes);
                } else {
                    // CRITICAL FIX: Read the exact error message from Supabase to see what went wrong
                    InputStream errorStream = conn.getErrorStream();
                    if (errorStream != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                        StringBuilder errorSb = new StringBuilder();
                        String errorLine;
                        while ((errorLine = reader.readLine()) != null) {
                            errorSb.append(errorLine);
                        }
                        reader.close();

                        String serverError = errorSb.toString();
                        runOnUiThread(() -> Toast.makeText(this, "Upload failed 400: " + serverError, Toast.LENGTH_LONG).show());
                    } else {
                        runOnUiThread(() -> Toast.makeText(this, "Upload failed: " + responseCode, Toast.LENGTH_SHORT).show());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error uploading: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ── Update users table with new avatar_url ────────────────────────────────
    // ── Update users table with new avatar_url ────────────────────────────────
    // ── Update users table with new avatar_url ────────────────────────────────
    private void updateUserAvatarUrl(String publicUrl, byte[] imageBytes) {
        new Thread(() -> {
            try {
                String updateUrl = SupabaseClient.SUPABASE_URL;
                if (updateUrl.endsWith("/")) updateUrl = updateUrl.substring(0, updateUrl.length() - 1);
                updateUrl += "/rest/v1/users?id=eq." + userId;

                HttpURLConnection conn = (HttpURLConnection) new URL(updateUrl).openConnection();

                // CRITICAL FIX: Send a true PATCH request instead of a POST
                conn.setRequestMethod("PATCH");
                conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Prefer", "return=minimal");
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("avatar_url", publicUrl);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                os.flush(); // Ensure it pushes the data
                os.close();

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Profile picture updated successfully!", Toast.LENGTH_SHORT).show();
                        displayLocalAvatar(imageBytes);
                    });
                } else {
                    InputStream errorStream = conn.getErrorStream();
                    String serverError = "Unknown Error";
                    if (errorStream != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                        StringBuilder errorSb = new StringBuilder();
                        String errorLine;
                        while ((errorLine = reader.readLine()) != null) {
                            errorSb.append(errorLine);
                        }
                        reader.close();
                        serverError = errorSb.toString();
                    }

                    final String finalError = serverError;
                    runOnUiThread(() -> Toast.makeText(this, "DB Error: " + finalError, Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Exception: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // ── Display locally loaded bytes (Instant Update) ─────────────────────────
    private void displayLocalAvatar(byte[] imageBytes) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        injectAvatarIntoCard(bitmap);
    }

    // ── Fetch network URL and inject into CardView ────────────────────────────
    private void loadAvatarImage(String urlString) {
        new Thread(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true);
                conn.connect();
                InputStream input = conn.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                runOnUiThread(() -> injectAvatarIntoCard(bitmap));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // ── Programmatically adding ImageView without changing XML ────────────────
    private void injectAvatarIntoCard(Bitmap bitmap) {
        if (bitmap == null) return;
        CardView avatarCard = findViewById(R.id.avatarCard);
        ImageView iv = avatarCard.findViewWithTag("avatarImage");

        if (iv == null) {
            iv = new ImageView(this);
            iv.setTag("avatarImage");
            iv.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            avatarCard.addView(iv);
        }

        iv.setImageBitmap(bitmap);
        findViewById(R.id.avatarInitials).setVisibility(View.GONE);
    }

    // ── Initials builder ─────────────────────────────────────────────────────
    private String buildInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1)
                + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    // =========================================================================
    // CHANGE PASSWORD DIALOG
    // =========================================================================
    private void showChangePasswordDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_change_password);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);

        TextInputLayout newPassLayout     = dialog.findViewById(R.id.newPasswordLayout);
        TextInputLayout confirmPassLayout = dialog.findViewById(R.id.confirmPasswordLayout);
        TextInputEditText etNew     = dialog.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirm = dialog.findViewById(R.id.etConfirmPassword);
        MaterialButton btnCancel  = dialog.findViewById(R.id.btnCancelPassword);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirmPassword);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String newPass     = etNew.getText()     != null ? etNew.getText().toString().trim()     : "";
            String confirmPass = etConfirm.getText() != null ? etConfirm.getText().toString().trim() : "";

            newPassLayout.setError(null);
            confirmPassLayout.setError(null);

            if (newPass.isEmpty()) {
                newPassLayout.setError("Password is required");
                etNew.requestFocus();
                return;
            }
            if (newPass.length() < 6) {
                newPassLayout.setError("Password must be at least 6 characters");
                etNew.requestFocus();
                return;
            }
            if (!newPass.equals(confirmPass)) {
                confirmPassLayout.setError("Passwords do not match");
                etConfirm.requestFocus();
                return;
            }

            btnConfirm.setEnabled(false);
            btnConfirm.setText("Updating…");

            changePassword(newPass, dialog, btnConfirm);
        });

        dialog.show();
    }

    // ── Call Supabase Auth to update password ─────────────────────────────────
    private void changePassword(String newPassword, Dialog dialog, MaterialButton btnConfirm) {
        new Thread(() -> {
            try {
                String baseUrl = SupabaseClient.SUPABASE_URL;
                if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                String url = baseUrl + "/auth/v1/user";

                HttpURLConnection conn = (HttpURLConnection)
                        new URL(url).openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // Build request body
                JSONObject body = new JSONObject();
                body.put("password", newPassword);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                os.close();

                int responseCode = conn.getResponseCode();
                conn.disconnect();

                if (responseCode == 200) {
                    runOnUiThread(() -> {
                        dialog.dismiss();
                        Toast.makeText(this,
                                "Password updated! Please log in again.",
                                Toast.LENGTH_LONG).show();
                        // Log out after password change for security
                        clearAndGoToLogin();
                    });
                } else {
                    runOnUiThread(() -> {
                        btnConfirm.setEnabled(true);
                        btnConfirm.setText("Update");
                        Toast.makeText(this,
                                "Failed to update password. Try again.",
                                Toast.LENGTH_LONG).show();
                    });
                }

            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnConfirm.setEnabled(true);
                    btnConfirm.setText("Update");
                    Toast.makeText(this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // ── Logout ────────────────────────────────────────────────────────────────
    private void setupQuickActions() {
        MaterialButton logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> handleLogout());

        // ── Wire Change Password row ──────────────────────────────────────────
        LinearLayout rowChangePassword = findViewById(R.id.rowChangePassword);
        if (rowChangePassword != null) {
            rowChangePassword.setClickable(true);
            rowChangePassword.setFocusable(true);
            rowChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        }
    }

    private void handleLogout() {
        new Thread(() -> {
            try {
                String baseUrl = SupabaseClient.SUPABASE_URL;
                if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

                HttpURLConnection conn = (HttpURLConnection)
                        new URL(baseUrl + "/auth/v1/logout")
                                .openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write("{}".getBytes());
                os.close();
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception ignored) {}

            runOnUiThread(this::clearAndGoToLogin);
        }).start();
    }

    private void clearAndGoToLogin() {
        getSharedPreferences("LoginPrefs", MODE_PRIVATE).edit().clear().apply();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ── Bottom navigation ─────────────────────────────────────────────────────
    private void setupBottomNavigation() {
        FrameLayout navFeed      = findViewById(R.id.nav_btn_feed);
        LinearLayout navNotices  = findViewById(R.id.nav_btn_notices);
        LinearLayout navChat     = findViewById(R.id.nav_btn_chat);
        LinearLayout navServices = findViewById(R.id.nav_btn_services);
        LinearLayout navProfile  = findViewById(R.id.nav_btn_profile);

        navProfile.setOnClickListener(v -> {});

        navFeed.setOnClickListener(v -> {
            startActivity(new Intent(this, FeedActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
        navNotices.setOnClickListener(v -> {
            startActivity(new Intent(this, NoticesActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
        navChat.setOnClickListener(v -> {
            startActivity(new Intent(this, ChatActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
        navServices.setOnClickListener(v -> {
            startActivity(new Intent(this, ServicesActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}