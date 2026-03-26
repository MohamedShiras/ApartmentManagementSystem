package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout usernameLayout, passwordLayout;
    private TextInputEditText usernameInput, passwordInput;
    private SwitchCompat rememberMeSwitch;
    private MaterialButton signInButton;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        initializeViews();
        checkRememberedUser();
        setupClickListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // If user is already signed in, go straight to Feed
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            goToFeed();
        }
    }

    private void initializeViews() {
        usernameLayout  = findViewById(R.id.usernameLayout);
        passwordLayout  = findViewById(R.id.passwordLayout);
        usernameInput   = findViewById(R.id.usernameInput);
        passwordInput   = findViewById(R.id.passwordInput);
        rememberMeSwitch = findViewById(R.id.rememberMeSwitch);
        signInButton    = findViewById(R.id.signInButton);
    }

    private void setupClickListeners() {
        signInButton.setOnClickListener(v -> handleSignIn());
    }

    private void handleSignIn() {
        String email    = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        boolean rememberMe = rememberMeSwitch.isChecked();

        // Reset errors
        usernameLayout.setError(null);
        passwordLayout.setError(null);

        // Validate
        if (email.isEmpty()) {
            usernameLayout.setError("Email is required");
            usernameInput.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            usernameLayout.setError("Enter a valid email address");
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

        // Disable button to prevent double-tap
        signInButton.setEnabled(false);
        signInButton.setText("Signing in...");

        // Firebase sign in
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    signInButton.setEnabled(true);
                    signInButton.setText("Sign In");

                    if (task.isSuccessful()) {
                        // Save remember me preference
                        if (rememberMe) {
                            saveCredentials(email, password);
                        } else {
                            clearCredentials();
                        }

                        FirebaseUser user = mAuth.getCurrentUser();
                        String displayName = (user != null && user.getDisplayName() != null)
                                ? user.getDisplayName()
                                : email;

                        Toast.makeText(this, "Welcome, " + displayName + "!", Toast.LENGTH_SHORT).show();
                        goToFeed();

                    } else {
                        // Login failed — show specific error
                        String errorMsg = "Login failed. Check your email and password.";
                        if (task.getException() != null) {
                            String exMsg = task.getException().getMessage();
                            if (exMsg != null) {
                                if (exMsg.contains("password")) {
                                    errorMsg = "Incorrect password.";
                                    passwordLayout.setError(errorMsg);
                                } else if (exMsg.contains("no user") || exMsg.contains("identifier")) {
                                    errorMsg = "No account found with this email.";
                                    usernameLayout.setError(errorMsg);
                                } else if (exMsg.contains("network")) {
                                    errorMsg = "Network error. Check your connection.";
                                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    }
                });
    }

    private void goToFeed() {
        Intent intent = new Intent(LoginActivity.this, FeedActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void saveCredentials(String email, String password) {
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        prefs.edit()
                .putString("email", email)
                .putString("password", password)
                .putBoolean("rememberMe", true)
                .apply();
    }

    private void clearCredentials() {
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    private void checkRememberedUser() {
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        boolean rememberMe = prefs.getBoolean("rememberMe", false);

        if (rememberMe) {
            String savedEmail    = prefs.getString("email", "");
            String savedPassword = prefs.getString("password", "");
            usernameInput.setText(savedEmail);
            passwordInput.setText(savedPassword);
            rememberMeSwitch.setChecked(true);
        }
    }
}