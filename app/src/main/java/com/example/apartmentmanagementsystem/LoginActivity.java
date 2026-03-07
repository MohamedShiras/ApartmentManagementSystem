package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout usernameLayout, passwordLayout;
    private TextInputEditText usernameInput, passwordInput;
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
        setupClickListeners();
        checkRememberedUser();
    }

    private void initializeViews() {
        usernameLayout = findViewById(R.id.usernameLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        rememberMeSwitch = findViewById(R.id.rememberMeSwitch);
        signInButton = findViewById(R.id.signInButton);
    }

    private void setupClickListeners() {
        signInButton.setOnClickListener(v -> handleSignIn());
    }

    private void handleSignIn() {
        // Get input values
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        boolean rememberMe = rememberMeSwitch.isChecked();

        // Reset errors
        usernameLayout.setError(null);
        passwordLayout.setError(null);

        // Validate inputs
        if (username.isEmpty()) {
            usernameLayout.setError("Username is required");
            usernameInput.requestFocus();
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

        // Perform login
        performLogin(username, password, rememberMe);
    }

    private void performLogin(String username, String password, boolean rememberMe) {
        // TODO: Add your authentication logic here (Firebase, API, etc.)
        // For now, accepting any valid input

        // Save credentials if Remember Me is checked
        if (rememberMe) {
            saveCredentials(username, password);
        } else {
            clearCredentials();
        }

        // Show success message
        Toast.makeText(this, "Welcome, " + username + "!", Toast.LENGTH_SHORT).show();

        // Navigate to FeedActivity
        Intent intent = new Intent(LoginActivity.this, FeedActivity.class);
        intent.putExtra("username", username);
        startActivity(intent);
        finish(); // Close LoginActivity so user can't go back to it
    }

    private void saveCredentials(String username, String password) {
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        prefs.edit()
                .putString("username", username)
                .putString("password", password)
                .putBoolean("rememberMe", true)
                .apply();
    }

    private void clearCredentials() {
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        prefs.edit()
                .clear()
                .apply();
    }

    private void checkRememberedUser() {
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        boolean rememberMe = prefs.getBoolean("rememberMe", false);

        if (rememberMe) {
            String savedUsername = prefs.getString("username", "");
            String savedPassword = prefs.getString("password", "");

            // Pre-fill the fields
            usernameInput.setText(savedUsername);
            passwordInput.setText(savedPassword);
            rememberMeSwitch.setChecked(true);

            // Optional: Auto-login
            // performLogin(savedUsername, savedPassword, true);
        }
    }
}