package com.example.apartmentmanagementsystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity
        implements ChatAdapter.OnMessageActionListener {

    private String currentUserId;
    private String currentUserName;
    private String accessToken;

    private RecyclerView recyclerView;
    private TextInputEditText messageInput;
    private View sendButton;
    private View backButton;
    private TextView tvHeaderAvatar;

    private ChatAdapter chatAdapter;
    private List<Message> messageList = new ArrayList<>();

    private final Handler pollingHandler = new Handler(Looper.getMainLooper());
    private Runnable pollingRunnable;
    private static final int POLL_MS = 3000;
    private List<String> locallyReactedIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // ── Fix top: AppBarLayout moves below status bar ─────────────────────
        CoordinatorLayout root = findViewById(R.id.chat);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, bars.top, 0, 0);
            return insets;
        });

        // ── Fix bottom: message input card sits above navigation bar ─────────
        CardView messageInputCard = findViewById(R.id.messageInputCard);
        ViewCompat.setOnApplyWindowInsetsListener(messageInputCard, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            CoordinatorLayout.LayoutParams lp =
                    (CoordinatorLayout.LayoutParams) v.getLayoutParams();
            lp.bottomMargin = bars.bottom;
            v.setLayoutParams(lp);
            return insets;
        });

        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        currentUserId   = prefs.getString("user_id", null);
        accessToken     = prefs.getString("access_token", null);
        currentUserName = prefs.getString("full_name", "Resident");

        initViews();

        messageInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                messageInput.setHint(s.length() > 0 ? "" : "Type a message...");
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        setupRecyclerView();
        setupPolling();
    }

    private void initViews() {
        recyclerView    = findViewById(R.id.chatRecyclerView);
        messageInput    = findViewById(R.id.messageInput);
        sendButton      = findViewById(R.id.sendButton);
        backButton      = findViewById(R.id.backButton);
        tvHeaderAvatar  = findViewById(R.id.tvUserInitial);

        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                startActivity(new Intent(this, FeedActivity.class));
                finish();
            });
        }

        if (sendButton != null) sendButton.setOnClickListener(v -> sendMessage());

        if (tvHeaderAvatar != null) {
            tvHeaderAvatar.setText(getInitials(currentUserName));
        }
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()
                || name.equalsIgnoreCase("Resident")) return "ME";
        String[] words = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        if (words.length > 0 && !words[0].isEmpty())
            sb.append(words[0].toUpperCase().charAt(0));
        if (words.length > 1 && !words[1].isEmpty())
            sb.append(words[1].toUpperCase().charAt(0));
        return sb.length() > 0 ? sb.toString() : "ME";
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(this, messageList, currentUserId);
        chatAdapter.setOnMessageActionListener(this);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        recyclerView.setLayoutManager(lm);
        recyclerView.setAdapter(chatAdapter);
    }

    private void setupPolling() {
        pollingRunnable = new Runnable() {
            @Override public void run() {
                loadMessages();
                pollingHandler.postDelayed(this, POLL_MS);
            }
        };
        pollingHandler.post(pollingRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        pollingHandler.post(pollingRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pollingHandler.removeCallbacks(pollingRunnable);
    }

    private void loadMessages() {
        new Thread(() -> {
            try {
                String msgUrl = SupabaseClient.SUPABASE_URL
                        + "/rest/v1/messages"
                        + "?select=id,user_id,content,reply_to_id,created_at"
                        + "&order=created_at.asc&limit=100";

                HttpURLConnection msgConn = openGet(msgUrl);
                String messagesJson = readStream(msgConn);
                msgConn.disconnect();

                JSONArray msgArr = new JSONArray(messagesJson);
                Map<String, String> nameMap = new HashMap<>();

                if (msgArr.length() > 0) {
                    StringBuilder idList = new StringBuilder();
                    for (int i = 0; i < msgArr.length(); i++) {
                        if (i > 0) idList.append(",");
                        idList.append(msgArr.getJSONObject(i).getString("user_id"));
                    }
                    try {
                        String usersUrl = SupabaseClient.SUPABASE_URL
                                + "/rest/v1/users"
                                + "?select=id,full_name"
                                + "&id=in.(" + idList + ")";
                        HttpURLConnection uc = openGet(usersUrl);
                        JSONArray usersArr = new JSONArray(readStream(uc));
                        uc.disconnect();
                        for (int i = 0; i < usersArr.length(); i++) {
                            JSONObject u = usersArr.getJSONObject(i);
                            nameMap.put(u.getString("id"),
                                    u.optString("full_name", "Resident"));
                        }
                    } catch (Exception ignored) {}
                }

                List<Message> newList = new ArrayList<>();
                for (int i = 0; i < msgArr.length(); i++) {
                    JSONObject m = msgArr.getJSONObject(i);
                    String msgId = m.getString("id");
                    Message msgObj = new Message(
                            msgId,
                            m.getString("user_id"),
                            m.getString("content"),
                            m.isNull("reply_to_id") ? null : m.getString("reply_to_id"),
                            m.getString("created_at"),
                            nameMap.getOrDefault(m.getString("user_id"), "Resident"),
                            ""
                    );
                    if (locallyReactedIds.contains(msgId)) {
                        msgObj.setHasReacted(true);
                    }
                    newList.add(msgObj);
                }

                runOnUiThread(() -> chatAdapter.updateMessages(newList));
            } catch (Exception ignored) {}
        }).start();
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) return;
        messageInput.setText("");

        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection)
                        new URL(SupabaseClient.SUPABASE_URL + "/rest/v1/messages")
                                .openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("user_id", currentUserId);
                body.put("content", text);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }
                int responseCode = conn.getResponseCode();
                if (responseCode == 201 || responseCode == 200) {
                    runOnUiThread(this::loadMessages);
                } else {
                    android.util.Log.e("CHAT_ERROR", "Response Code: " + responseCode);
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Failed! Code: " + responseCode, Toast.LENGTH_LONG).show());
                }
                conn.disconnect();
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private HttpURLConnection openGet(String urlString) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        return conn;
    }

    private String readStream(HttpURLConnection conn) throws Exception {
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    @Override
    public void onLongPress(Message message, boolean isOwn) {
        if (isOwn) {
            new AlertDialog.Builder(this)
                    .setTitle("Options")
                    .setItems(new String[]{"Delete Message"},
                            (d, w) -> deleteMessage(message))
                    .show();
        }
    }

    private void deleteMessage(Message message) {
        new Thread(() -> {
            try {
                String url = SupabaseClient.SUPABASE_URL
                        + "/rest/v1/messages?id=eq." + message.getId();
                HttpURLConnection conn = (HttpURLConnection)
                        new URL(url).openConnection();
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("apikey", SupabaseClient.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.getResponseCode();
                conn.disconnect();
                runOnUiThread(this::loadMessages);
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Delete failed", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}