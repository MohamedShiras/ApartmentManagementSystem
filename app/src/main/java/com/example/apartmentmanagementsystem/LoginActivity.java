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

    private TextInputLayout idLayout, passwordLayout;
    private TextInputEditText idInput, passwordInput;
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
        idLayout         = findViewById(R.id.usernameLayout);
        passwordLayout   = findViewById(R.id.passwordLayout);
        idInput          = findViewById(R.id.usernameInput);
        passwordInput    = findViewById(R.id.passwordInput);
        rememberMeSwitch = findViewById(R.id.rememberMeSwitch);
        signInButton     = findViewById(R.id.signInButton);

        idLayout.setHint("Apartment No / Username");
    }

    private void setupClickListeners() {
        signInButton.setOnClickListener(v -> handleSignIn());
    }

    private void handleSignIn() {
        String inputId   = idInput.getText().toString().trim();
        String password  = passwordInput.getText().toString().trim();
        boolean remember = rememberMeSwitch.isChecked();

        idLayout.setError(null);
        passwordLayout.setError(null);

        if (inputId.isEmpty()) {
            idLayout.setError("ID is required");
            idInput.requestFocus();
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
                // ── Step 1: Check Admin ──────────────────────────────────────
                String adminEmail = lookupEmail("admin_profiles", "username", inputId);
                if (adminEmail != null) {
                    authenticate(adminEmail, password, inputId, "ADMIN", inputId, remember);
                    return;
                }

                // ── Step 2: Check Maintenance Staff ──────────────────────────
                String maintEmail = lookupEmail("maintenance_profiles", "username", inputId);
                if (maintEmail != null) {
                    authenticate(maintEmail, password, inputId, "MAINTENANCE", inputId, remember);
                    return;
                }

                // ── Step 3: Check Resident (apartment_number) ────────────────
                String apartmentUpper = inputId.toUpperCase();
                String[] residentData = lookupResident(apartmentUpper); // [email, full_name]
                if (residentData != null) {
                    authenticate(residentData[0], password, apartmentUpper, "RESIDENT", residentData[1], remember);
                    return;
                }

                // ── Not found in any table ───────────────────────────────────
                runOnUiThread(() -> {
                    setLoading(false);
                    idLayout.setError("ID not found");
                });

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

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Query a table for a single email by column=value. Returns null if not found. */
    private String lookupEmail(String table, String column, String value) throws Exception {
        String url = SupabaseClient.SUPABASE_URL
                + "/rest/v1/" + table
                + "?" + column + "=eq." + value
                + "&select=email&limit=1";

        HttpURLConnection conn = openGet(url);
        String body = readBody(conn);
        conn.disconnect();

        JSONArray arr = new JSONArray(body);
        if (arr.length() == 0) return null;
        return arr.getJSONObject(0).getString("email");
    }

    /** Query users table by apartment_number. Returns [email, full_name] or null. */
    private String[] lookupResident(String apartment) throws Exception {
        String url = SupabaseClient.SUPABASE_URL
                + "/rest/v1/users"
                + "?apartment_number=eq." + apartment
                + "&select=email,full_name&limit=1";

        HttpURLConnection conn = openGet(url);
        String body = readBody(conn);
        conn.disconnect();

        JSONArray arr = new JSONArray(body);
        if (arr.length() == 0) return null;

        JSONObject obj = arr.getJSONObject(0);
        String email    = obj.getString("email");
        String fullName = obj.optString("full_name", "Resident " + apartment);
        return new String[]{email, fullName};
    }

    private void authenticate(String email, String password,
                              String displayId, String role,
                              String fullName, boolean remember) throws Exception {

        String authUrl = SupabaseClient.SUPABASE_URL + "/auth/v1/token?grant_type=password";
        HttpURLConnection authConn = (HttpURLConnection) new URL(authUrl).openConnection();
        authConn.setRequestMethod("POST");
        authConn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
        authConn.setRequestProperty("Content-Type", "application/json");
        authConn.setDoOutput(true);

        String body = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        try (OutputStream os = authConn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int authCode = authConn.getResponseCode();

        if (authCode == 200) {
            String authBody = readBody(authConn);
            authConn.disconnect();

            JSONObject authResponse = new JSONObject(authBody);
            String accessToken = authResponse.getString("access_token");

            String userId = extractUserIdFromToken(accessToken);

            saveSessionData(accessToken, userId, displayId, fullName, role);

            if (remember) {
                savePersistentCredentials(displayId, password);
            } else {
                clearPersistentCredentials();
            }

            runOnUiThread(() -> {
                setLoading(false);
                Toast.makeText(this, "Welcome, " + fullName + "!", Toast.LENGTH_SHORT).show();
                navigateByRole(role);
            });

        } else {
            // Read error body for accurate error message
            String errMsg = "Incorrect password";
            try {
                BufferedReader errReader = new BufferedReader(new InputStreamReader(authConn.getErrorStream()));
                StringBuilder errSb = new StringBuilder();
                String line;
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
    }

    private void navigateByRole(String role) {
        Class<?> target;
        switch (role) {
            case "ADMIN":       target = AdminActivity.class;             break;
            case "MAINTENANCE": target = MaintenanceStaffActivity.class;  break;
            default:            target = FeedActivity.class;              break;
        }
        Intent intent = new Intent(this, target);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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

    // ── SharedPreferences ────────────────────────────────────────────────────

    private void saveSessionData(String token, String userId, String displayId,
                                 String fullName, String role) {
        getSharedPreferences("LoginPrefs", MODE_PRIVATE).edit()
                .putString("access_token", token)
                .putString("user_id",      userId)      // Used for relationships/foreign keys
                .putString("apartment",    displayId)   // Kept for backward compat
                .putString("display_id",   displayId)
                .putString("full_name",    fullName)
                .putString("user_role",    role)
                .apply();
    }

    private void savePersistentCredentials(String id, String password) {
        getSharedPreferences("LoginPrefs", MODE_PRIVATE).edit()
                .putString("saved_apartment", id)
                .putString("saved_password",  password)
                .putBoolean("rememberMe",     true)
                .apply();
    }

    private void clearPersistentCredentials() {
        getSharedPreferences("LoginPrefs", MODE_PRIVATE).edit()
                .remove("saved_apartment")
                .remove("saved_password")
                .putBoolean("rememberMe", false)
                .apply();
    }

    private void checkRememberedUser() {
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        if (prefs.getBoolean("rememberMe", false)) {
            idInput.setText(prefs.getString("saved_apartment", ""));
            passwordInput.setText(prefs.getString("saved_password", ""));
            rememberMeSwitch.setChecked(true);
        }
    }

    // ── Network utils ────────────────────────────────────────────────────────

    private HttpURLConnection openGet(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("apikey",        SupabaseClient.SUPABASE_ANON_KEY);
        conn.setRequestProperty("Authorization", "Bearer " + SupabaseClient.SUPABASE_ANON_KEY);
        return conn;
    }

    private String readBody(HttpURLConnection conn) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        return sb.toString();
    }

    private void setLoading(boolean loading) {
        signInButton.setEnabled(!loading);
        signInButton.setText(loading ? "Signing in..." : "Sign In");
    }
}