package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout usernameLayout, passwordLayout;
    private TextInputEditText usernameInput, passwordInput;
    private SwitchCompat rememberMeSwitch;
    private MaterialButton signInButton;

    private FirebaseAuth mAuth;
    private DatabaseReference adminCredentialsRef;
    private static final String ADMIN_DB_URL = "https://apartment-management-sys-ff2de-default-rtdb.asia-southeast1.firebasedatabase.app";
    private static final long ADMIN_SIGN_IN_TIMEOUT_MS = 15000L;
    private final Handler adminTimeoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable adminTimeoutRunnable = () -> {
        setSigningState(false);
        Toast.makeText(this, "Admin login timed out. Please try again.", Toast.LENGTH_LONG).show();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize Firebase clients
        mAuth = FirebaseAuth.getInstance();
        adminCredentialsRef = FirebaseDatabase.getInstance(ADMIN_DB_URL).getReference("adminCredentials");

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

    @Override
    protected void onDestroy() {
        adminTimeoutHandler.removeCallbacks(adminTimeoutRunnable);
        super.onDestroy();
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
        String identifier = usernameInput.getText() != null
                ? usernameInput.getText().toString().trim()
                : "";
        String password = passwordInput.getText() != null
                ? passwordInput.getText().toString().trim()
                : "";
        boolean rememberMe = rememberMeSwitch.isChecked();

        // Reset errors
        usernameLayout.setError(null);
        passwordLayout.setError(null);

        if (identifier.isEmpty()) {
            usernameLayout.setError("Email or admin username is required");
            usernameInput.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordLayout.setError("Password is required");
            passwordInput.requestFocus();
            return;
        }

        setSigningState(true);

        // Users keep using Firebase Auth by email; non-email identifiers are treated as admin usernames.
        if (android.util.Patterns.EMAIL_ADDRESS.matcher(identifier).matches()) {
            signInAsUser(identifier, password, rememberMe);
        } else {
            signInAsAdmin(identifier, password, rememberMe);
        }
    }

    private void signInAsAdmin(String adminUsername, String password, boolean rememberMe) {
        adminTimeoutHandler.removeCallbacks(adminTimeoutRunnable);
        adminTimeoutHandler.postDelayed(adminTimeoutRunnable, ADMIN_SIGN_IN_TIMEOUT_MS);

        adminCredentialsRef.child(adminUsername).get().addOnCompleteListener(task -> {
            adminTimeoutHandler.removeCallbacks(adminTimeoutRunnable);

            if (!task.isSuccessful()) {
                setSigningState(false);
                String message = "Unable to verify admin now. Try again.";
                if (task.getException() != null && task.getException().getMessage() != null) {
                    message = "Admin login failed: " + task.getException().getMessage();
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                return;
            }

            DataSnapshot snapshot = task.getResult();
            if (!snapshot.exists()) {
                setSigningState(false);
                usernameLayout.setError("Admin account not found");
                usernameInput.requestFocus();
                return;
            }

            String firebasePassword = snapshot.child("password").getValue(String.class);
            Boolean enabled = snapshot.child("enabled").getValue(Boolean.class);
            boolean adminEnabled = enabled == null || enabled;

            if (!adminEnabled) {
                setSigningState(false);
                usernameLayout.setError("Admin account is disabled");
                return;
            }

            if (firebasePassword == null || !firebasePassword.equals(password)) {
                setSigningState(false);
                passwordLayout.setError("Incorrect admin password");
                passwordInput.requestFocus();
                return;
            }

            if (rememberMe) {
                saveCredentials(adminUsername, password);
            } else {
                clearCredentials();
            }

            setSigningState(false);
            Toast.makeText(this, "Welcome, Admin!", Toast.LENGTH_SHORT).show();
            goToAdminPanel();
        });
    }

    private void signInAsUser(String email, String password, boolean rememberMe) {
        if (password.length() < 6) {
            setSigningState(false);
            passwordLayout.setError("Password must be at least 6 characters");
            passwordInput.requestFocus();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setSigningState(false);

                    if (task.isSuccessful()) {
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
                        String errorMsg = "Login failed. Check your email and password.";
                        if (task.getException() != null && task.getException().getMessage() != null) {
                            String exMsg = task.getException().getMessage();
                            if (exMsg.contains("password")) {
                                errorMsg = "Incorrect password.";
                                passwordLayout.setError(errorMsg);
                            } else if (exMsg.contains("no user") || exMsg.contains("identifier")) {
                                errorMsg = "No account found with this email.";
                                usernameLayout.setError(errorMsg);
                            } else if (exMsg.contains("network")) {
                                errorMsg = "Network error. Check your connection.";
                            }
                        }
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setSigningState(boolean isSigningIn) {
        signInButton.setEnabled(!isSigningIn);
        signInButton.setText(isSigningIn ? "Signing in..." : "Sign In");
    }

    private void goToFeed() {
        Intent intent = new Intent(LoginActivity.this, FeedActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void goToAdminPanel() {
        Intent intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
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
            String savedEmail = prefs.getString("email", "");
            String savedPassword = prefs.getString("password", "");
            usernameInput.setText(savedEmail);
            passwordInput.setText(savedPassword);
            rememberMeSwitch.setChecked(true);
        }
    }
}