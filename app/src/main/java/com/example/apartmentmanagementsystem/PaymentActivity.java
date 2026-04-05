package com.example.apartmentmanagementsystem;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PaymentActivity extends AppCompatActivity {

    private String currentUserName = "N/A";
    private String currentUserEmail = "";
    private String currentUserUnit = "N/A";
    private String currentUserRent = "0.00";
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_combined_payment);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }


        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.gravity = Gravity.CENTER;
        params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92);
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.dimAmount = 0.65f;
        getWindow().setAttributes(params);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        }

        getWindow().setWindowAnimations(android.R.style.Animation_Dialog);

        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        userId = prefs.getString("user_id", "");

        if (userId.isEmpty()) {
            Toast.makeText(this, "Login session expired!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fetchUserDataAndStartFlow();
    }

    private void fetchUserDataAndStartFlow() {
        new Thread(() -> {
            try {
                String urlStr = SupabaseClient.SUPABASE_URL + "/rest/v1/users?id=eq." + userId + "&select=*";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SupabaseClient.SUPABASE_ANON_KEY);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                JSONArray jsonArray = new JSONArray(response.toString());
                if (jsonArray.length() > 0) {
                    JSONObject user = jsonArray.getJSONObject(0);
                    currentUserName = user.optString("full_name", "Valued Resident");
                    currentUserEmail = user.optString("email", "");
                    currentUserUnit = user.optString("apartment_number", "N/A");
                    currentUserRent = user.optString("rent_amount", "0.00");
                    runOnUiThread(this::showUnifiedPaymentPopup);
                }
            } catch (Exception e) {
                Log.e("FETCH_ERROR", e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(this, "Connection Error!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }

    private void showUnifiedPaymentPopup() {
        EditText etCard  = findViewById(R.id.etCardNum);
        EditText etExp   = findViewById(R.id.etExp);
        EditText etCvc   = findViewById(R.id.etCvc);
        TextView tvName  = findViewById(R.id.tvUserDetailName);
        TextView tvUnit  = findViewById(R.id.tvUserDetailUnit);
        TextView tvAmount= findViewById(R.id.tvAmountDue);
        Button btnPay    = findViewById(R.id.btnFinalizePayment);
        Button btnCancel = findViewById(R.id.btnCancel);

        tvName.setText("Resident: " + currentUserName);
        tvUnit.setText("Apartment: " + currentUserUnit);
        tvAmount.setText(currentUserRent);

        checkPaymentStatus(etCard, etExp, etCvc, btnPay);

        etCard.addTextChangedListener(new TextWatcher() {
            private boolean isUpdating = false;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) return;
                isUpdating = true;
                String originalText = s.toString().replace(" ", "");
                String cleanedText = originalText.length() > 16 ? originalText.substring(0, 16) : originalText;
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < cleanedText.length(); i++) {
                    formatted.append(cleanedText.charAt(i));
                    if ((i + 1) % 4 == 0 && (i + 1) < cleanedText.length()) formatted.append(" ");
                }
                s.replace(0, s.length(), formatted.toString());
                isUpdating = false;
            }
        });

        etExp.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count == 1 && start == 1 && !s.toString().contains("/")) {
                    etExp.setText(s + "/");
                    etExp.setSelection(etExp.getText().length());
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnCancel.setOnClickListener(v -> finish());

        btnPay.setOnClickListener(v -> {
            String cardNum = etCard.getText().toString().trim();
            String expDate = etExp.getText().toString().trim();
            String cvc = etCvc.getText().toString().trim();


            if ( cardNum.length() < 16) {
                etCard.setError("Please enter a valid 16-digit card number");
                etCard.requestFocus();
                return;
            }

            if (expDate.isEmpty() || expDate.length() < 5 || !expDate.contains("/")) {
                etExp.setError("Enter expiration date (MM/YY)");
                etExp.requestFocus();
                return;
            }


            String[] parts = expDate.split("/");
            try {
                int month = Integer.parseInt(parts[0]);
                int year = Integer.parseInt(parts[1]);

                if (month < 1 || month > 12) {
                    etExp.setError("Invalid month (01-12)");
                    etExp.requestFocus();
                    return;
                }


                int currentYearShort = 26;
                int currentMonth = 4;

                if (year < currentYearShort) {
                    etExp.setError("Card has expired");
                    etExp.requestFocus();
                    return;
                } else if (year == currentYearShort && month < currentMonth) {
                    etExp.setError("Card has expired this month");
                    etExp.requestFocus();
                    return;
                }

            } catch (NumberFormatException e) {
                etExp.setError("Invalid date format");
                etExp.requestFocus();
                return;
            }

            if (cvc.isEmpty() || cvc.length() < 3) {
                etCvc.setError("Enter a valid 3 or 4 digit CVC");
                etCvc.requestFocus();
                return;
            }

            showSuccessAndMail();
        });
    }

    private void checkPaymentStatus(EditText etCard, EditText etExp, EditText etCvc, Button btnPay) {
        new Thread(() -> {
            try {
                String queryUrl = SupabaseClient.SUPABASE_URL + "/rest/v1/payments?user_id=eq." + userId + "&order=created_at.desc&limit=1";
                URL url = new URL(queryUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SupabaseClient.SUPABASE_ANON_KEY);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();
                conn.disconnect();

                JSONArray result = new JSONArray(response.toString());
                runOnUiThread(() -> {
                    if (result.length() > 0) {
                        try {
                            JSONObject lastPayment = result.getJSONObject(0);
                            String createdAt = lastPayment.optString("created_at", "");

                            if (isWithin28Days(createdAt)) {
                                blockPaymentButton(etCard, etExp, etCvc, btnPay, createdAt);
                            }
                        } catch (Exception e) {
                            Log.e("PAYMENT_CHECK", e.getMessage());
                        }
                    }
                });
            } catch (Exception e) {
                Log.e("PAYMENT_QUERY", e.getMessage());
            }
        }).start();
    }

    private boolean isWithin28Days(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date lastPaymentDate = sdf.parse(dateStr);
            Date today = new Date();

            if (lastPaymentDate == null) return false;

            long diffInMillis = today.getTime() - lastPaymentDate.getTime();
            long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);

            return diffInDays <= 28;
        } catch (Exception e) {
            Log.e("DATE_CHECK", e.getMessage());
            return false;
        }
    }

    private void blockPaymentButton(EditText etCard, EditText etExp, EditText etCvc, Button btnPay, String paymentDate) {
        btnPay.setEnabled(false);
        btnPay.setAlpha(0.5f);
        btnPay.setText("PAID");

        etCard.setEnabled(false);
        etExp.setEnabled(false);
        etCvc.setEnabled(false);
        etCard.setAlpha(0.6f);
        etExp.setAlpha(0.6f);
        etCvc.setAlpha(0.6f);

        int daysRemaining = getDaysRemaining(paymentDate);
        String nextDueDate = getNextDueDate(paymentDate);

        Toast.makeText(this, "✓ Payment completed!\nNext payment due: " + nextDueDate + " (" + daysRemaining + " days)", Toast.LENGTH_LONG).show();
    }

    private int getDaysRemaining(String paymentDateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date paymentDate = sdf.parse(paymentDateStr);
            Date nextDueDate = new Date(paymentDate.getTime() + (28L * 24 * 60 * 60 * 1000));
            Date today = new Date();

            long diffInMillis = nextDueDate.getTime() - today.getTime();
            long daysRemaining = diffInMillis / (1000 * 60 * 60 * 24);

            return (int) daysRemaining;
        } catch (Exception e) {
            Log.e("DAYS_CALC", e.getMessage());
            return 28;
        }
    }

    private String getNextDueDate(String paymentDateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date paymentDate = sdf.parse(paymentDateStr);
            Date nextDueDate = new Date(paymentDate.getTime() + (28L * 24 * 60 * 60 * 1000));

            SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return displayFormat.format(nextDueDate);
        } catch (Exception e) {
            Log.e("DATE_FORMAT", e.getMessage());
            return "N/A";
        }
    }

    private void showSuccessAndMail() {
        sendEmailViaEmailJS(currentUserName, currentUserEmail, currentUserUnit);
        savePaymentToSupabase();

        View dv = getLayoutInflater().inflate(R.layout.dialog_payment_success, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dv)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.3f);
        }

        dialog.show();

        View circle = dv.findViewById(R.id.successCircle);
        View check  = dv.findViewById(R.id.successCheck);

        circle.setAlpha(0f);
        circle.setScaleX(0.3f);
        circle.setScaleY(0.3f);
        check.setAlpha(0f);

        ObjectAnimator sx = ObjectAnimator.ofFloat(circle, "scaleX", 0.3f, 1.1f, 1f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(circle, "scaleY", 0.3f, 1.1f, 1f);
        ObjectAnimator fa = ObjectAnimator.ofFloat(circle, "alpha", 0f, 1f);
        sx.setDuration(500);
        sy.setDuration(500);
        fa.setDuration(300);
        sx.setInterpolator(new OvershootInterpolator(2.0f));
        sy.setInterpolator(new OvershootInterpolator(2.0f));

        ObjectAnimator cf  = ObjectAnimator.ofFloat(check, "alpha", 0f, 1f);
        ObjectAnimator csx = ObjectAnimator.ofFloat(check, "scaleX", 0.5f, 1f);
        ObjectAnimator csy = ObjectAnimator.ofFloat(check, "scaleY", 0.5f, 1f);
        cf.setStartDelay(400);
        cf.setDuration(300);
        csx.setStartDelay(400);
        csx.setDuration(300);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(sx, sy, fa, cf, csx, csy);
        set.start();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing()) {
                dialog.dismiss();
                Toast.makeText(this, "Process Completed Successfully!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }, 3000);
    }

    private void savePaymentToSupabase() {
        new Thread(() -> {
            try {
                URL url = new URL(SupabaseClient.SUPABASE_URL + "/rest/v1/payments");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject paymentData = new JSONObject();
                paymentData.put("user_id", userId);
                paymentData.put("full_name", currentUserName);
                paymentData.put("unit_number", currentUserUnit);
                paymentData.put("amount", currentUserRent);
                // Add timestamp for 28-day cycle
                String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
                paymentData.put("payment_date", timestamp);

                OutputStream os = conn.getOutputStream();
                os.write(paymentData.toString().getBytes("UTF-8"));
                os.close();
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) { Log.e("DB_SAVE_ERROR", e.getMessage()); }
        }).start();
    }

    private void sendEmailViaEmailJS(String name, String email, String unit) {
        new Thread(() -> {
            try {
                String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                URL url = new URL("https://api.emailjs.com/api/v1.0/email/send");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("service_id", "service_oe9nicr");
                jsonBody.put("template_id", "template_x04k0ni");
                jsonBody.put("user_id", "xybJ_i1qV33sbYDA7");

                JSONObject templateParams = new JSONObject();
                templateParams.put("to_name", name);
                templateParams.put("user_email", email);
                templateParams.put("unit_no", unit);
                templateParams.put("rent_amount", currentUserRent);
                templateParams.put("payment_date", currentDate);

                jsonBody.put("template_params", templateParams);

                OutputStream os = conn.getOutputStream();
                os.write(jsonBody.toString().getBytes("UTF-8"));
                os.close();
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) { Log.e("EMAIL_ERROR", e.getMessage()); }
        }).start();
    }
}