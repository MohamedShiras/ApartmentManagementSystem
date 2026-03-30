package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

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
        if (password.length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters");
            passwordInput.requestFocus();
            return;
        }

        setLoading(true);

        new Thread(() -> {
            try {
                // ── Step 1: Get email from users table by apartment number ──
                String queryUrl = SupabaseClient.SUPABASE_URL
                        + "/rest/v1/users"
                        + "?apartment_number=eq." + apartment
                        + "&select=email,full_name"
                        + "&limit=1";

                HttpURLConnection conn = (HttpURLConnection)
                        new URL(queryUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Content-Type", "application/json");

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
                String fullName = userObj.optString("full_name", "Resident " + apartment);

                // ── Step 2: Sign in with Supabase Auth ──
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
                OutputStream os = authConn.getOutputStream();
                os.write(body.getBytes(StandardCharsets.UTF_8));
                os.close();

                int responseCode = authConn.getResponseCode();

                if (responseCode == 200) {
                    BufferedReader authReader = new BufferedReader(
                            new InputStreamReader(authConn.getInputStream()));
                    StringBuilder authSb = new StringBuilder();
                    while ((line = authReader.readLine()) != null) authSb.append(line);
                    authReader.close();
                    authConn.disconnect();

                    JSONObject authResponse = new JSONObject(authSb.toString());
                    String accessToken = authResponse.getString("access_token");

                    saveToken(accessToken);

                    if (rememberMe) {
                        saveCredentials(apartment, password);
                    } else {
                        clearCredentials();
                    }

                    String welcomeName = fullName;
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(this,
                                "Welcome, " + welcomeName + "!",
                                Toast.LENGTH_SHORT).show();
                        goToFeed();
                    });

                } else {
                    BufferedReader errReader = new BufferedReader(
                            new InputStreamReader(authConn.getErrorStream()));
                    StringBuilder errSb = new StringBuilder();
                    while ((line = errReader.readLine()) != null) errSb.append(line);
                    errReader.close();
                    authConn.disconnect();

                    JSONObject errObj = new JSONObject(errSb.toString());
                    String errMsg = errObj.optString("error_description", "Login failed");

                    runOnUiThread(() -> {
                        setLoading(false);
                        if (errMsg.toLowerCase().contains("invalid")) {
                            passwordLayout.setError("Incorrect password");
                        } else {
                            Toast.makeText(this, errMsg, Toast.LENGTH_LONG).show();
                        }
                    });
                }

            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    String msg = e.getMessage() != null ? e.getMessage() : "";
                    if (msg.contains("network") || msg.contains("Unable to resolve")
                            || msg.contains("timeout")) {
                        Toast.makeText(this,
                                "Network error. Check your connection.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Error: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    private void goToFeed() {
        Intent intent = new Intent(LoginActivity.this, FeedActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        signInButton.setEnabled(!loading);
        signInButton.setText(loading ? "Signing in..." : "Sign In");
    }

    private void saveToken(String token) {
        getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                .edit()
                .putString("access_token", token)
                .apply();
    }

    private void saveCredentials(String apartment, String password) {
        getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                .edit()
                .putString("apartment", apartment)
                .putString("password", password)
                .putBoolean("rememberMe", true)
                .apply();
    }

    private void clearCredentials() {
        getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                .edit()
                .remove("apartment")
                .remove("password")
                .putBoolean("rememberMe", false)
                .apply();
    }

    private void checkRememberedUser() {
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        if (prefs.getBoolean("rememberMe", false)) {
            apartmentInput.setText(prefs.getString("apartment", ""));
            passwordInput.setText(prefs.getString("password", ""));
            rememberMeSwitch.setChecked(true);
        }
    }
}