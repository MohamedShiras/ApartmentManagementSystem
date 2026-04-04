package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout apartmentLayout, passwordLayout;
    private TextInputEditText apartmentInput, passwordInput;
    private SwitchCompat rememberMeSwitch;
    private MaterialButton signInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initializeViews();
        checkRememberedUser();
        setupClickListeners();
    }

    private void initializeViews() {
        apartmentLayout  = findViewById(R.id.usernameLayout);
        passwordLayout   = findViewById(R.id.passwordLayout);
        apartmentInput   = findViewById(R.id.usernameInput);
        passwordInput    = findViewById(R.id.passwordInput);
        rememberMeSwitch = findViewById(R.id.rememberMeSwitch);
        signInButton     = findViewById(R.id.signInButton);
        apartmentLayout.setHint("Apartment Number");
    }

    private void setupClickListeners() {
        signInButton.setOnClickListener(v -> handleSignIn());
    }

    private void handleSignIn() {
        String apartment   = apartmentInput.getText().toString().trim().toUpperCase();
        String password    = passwordInput.getText().toString().trim();
        boolean rememberMe = rememberMeSwitch.isChecked();

        apartmentLayout.setError(null);
        passwordLayout.setError(null);

        if (apartment.isEmpty()) {
            apartmentLayout.setError("Apartment number is required");
            apartmentInput.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            passwordLayout.setError("Password is required");
            passwordInput.requestFocus();
            return;
        }

        setLoading(true);

        new Thread(() -> {
            try {
                // ── Step 1: Look up email by apartment number ─────────────────
                // Use anon key here — RLS policy allows public lookup by apartment
                String queryUrl = SupabaseClient.SUPABASE_URL
                        + "/rest/v1/users"
                        + "?apartment_number=eq." + apartment
                        + "&select=email,full_name"
                        + "&limit=1";

                HttpURLConnection conn = (HttpURLConnection)
                        new URL(queryUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization",
                        "Bearer " + SupabaseClient.SUPABASE_ANON_KEY);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                conn.disconnect();

                JSONArray arr = new JSONArray(sb.toString());
                if (arr.length() == 0) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        apartmentLayout.setError("Apartment number not found");
                    });
                    return;
                }

                JSONObject userObj = arr.getJSONObject(0);
                String email    = userObj.getString("email");
                String fullName = userObj.optString("full_name", "Resident");

                // ── Step 2: Authenticate with Supabase Auth ───────────────────
                String authUrl = SupabaseClient.SUPABASE_URL
                        + "/auth/v1/token?grant_type=password";

                HttpURLConnection authConn = (HttpURLConnection)
                        new URL(authUrl).openConnection();
                authConn.setRequestMethod("POST");
                authConn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                authConn.setRequestProperty("Content-Type", "application/json");
                authConn.setDoOutput(true);

                String body = "{\"email\":\"" + email
                        + "\",\"password\":\"" + password + "\"}";
                try (OutputStream os = authConn.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }

                int authCode = authConn.getResponseCode();

                if (authCode == 200) {
                    BufferedReader authReader = new BufferedReader(
                            new InputStreamReader(authConn.getInputStream()));
                    StringBuilder authSb = new StringBuilder();
                    while ((line = authReader.readLine()) != null) authSb.append(line);
                    authReader.close();
                    authConn.disconnect();

                    JSONObject authResponse = new JSONObject(authSb.toString());
                    String accessToken = authResponse.getString("access_token");

                    // ── Step 3: Extract user_id from JWT ─────────────────────
                    // This is the UUID stored in auth.users — same as users.id
                    String userId = extractUserIdFromToken(accessToken);

                    // ── Step 4: Save everything to SharedPreferences ──────────
                    saveSessionData(accessToken, userId, apartment, fullName);

                    if (rememberMe) {
                        savePersistentCredentials(apartment, password);
                    } else {
                        clearPersistentCredentials();
                    }

                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(this,
                                "Welcome, " + fullName + "!",
                                Toast.LENGTH_SHORT).show();
                        goToFeed();
                    });

                } else {
                    // Read error body
                    String errMsg = "Incorrect password";
                    try {
                        BufferedReader errReader = new BufferedReader(
                                new InputStreamReader(authConn.getErrorStream()));
                        StringBuilder errSb = new StringBuilder();
                        while ((line = errReader.readLine()) != null) errSb.append(line);
                        errReader.close();
                        JSONObject errObj = new JSONObject(errSb.toString());
                        String desc = errObj.optString("error_description", "");
                        if (!desc.isEmpty()) errMsg = desc;
                    } catch (Exception ignored) {}

                    authConn.disconnect();
                    String finalMsg = errMsg;
                    runOnUiThread(() -> {
                        setLoading(false);
                        passwordLayout.setError(finalMsg);
                    });
                }

            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Network error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // ── Extract UUID (sub) from JWT payload ──────────────────────────────────
    private String extractUserIdFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return "";
            byte[] decoded = Base64.decode(
                    parts[1].replace("-", "+").replace("_", "/"),
                    Base64.DEFAULT);
            JSONObject payload = new JSONObject(new String(decoded, StandardCharsets.UTF_8));
            return payload.optString("sub", "");
        } catch (Exception e) {
            return "";
        }
    }

    // ── SharedPreferences helpers ─────────────────────────────────────────────

    private void saveSessionData(String token, String userId,
                                 String apartment, String fullName) {
        getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                .edit()
                .putString("access_token",   token)
                .putString("user_id",        userId)      // ← critical: used by Feed, Profile, etc.
                .putString("apartment",      apartment)
                .putString("full_name",      fullName)
                .apply();
    }

    private void savePersistentCredentials(String apartment, String password) {
        getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                .edit()
                .putString("saved_apartment", apartment)
                .putString("saved_password",  password)
                .putBoolean("rememberMe",     true)
                .apply();
    }

    private void clearPersistentCredentials() {
        getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                .edit()
                .remove("saved_apartment")
                .remove("saved_password")
                .putBoolean("rememberMe", false)
                .apply();
    }

    private void checkRememberedUser() {
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        if (prefs.getBoolean("rememberMe", false)) {
            apartmentInput.setText(prefs.getString("saved_apartment", ""));
            passwordInput.setText(prefs.getString("saved_password",   ""));
            rememberMeSwitch.setChecked(true);
        }
    }

    private void goToFeed() {
        Intent intent = new Intent(this, FeedActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        signInButton.setEnabled(!loading);
        signInButton.setText(loading ? "Signing in..." : "Sign In");
    }
}