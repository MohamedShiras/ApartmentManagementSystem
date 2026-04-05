package com.example.apartmentmanagementsystem;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PostRepository {

    private static final String BASE_URL = SupabaseClient.SUPABASE_URL;
    private static final String ANON_KEY = SupabaseClient.SUPABASE_ANON_KEY;
    private static final String BUCKET   = "posts";  // Supabase Storage bucket name
    private static final String TABLE    = "posts";  // Supabase DB table name

    // ── Image compression settings ────────────────────────────────────
    private static final int MAX_IMAGE_SIZE = 1080;
    private static final int JPEG_QUALITY   = 85;

    // ── Callback interface ────────────────────────────────────────────
    public interface PostCallback {
        void onSuccess(String postId);
        void onError(String errorMessage);
    }

    private final OkHttpClient    httpClient;
    private final ExecutorService executor;
    private final Handler         mainHandler;

    public PostRepository() {
        httpClient  = new OkHttpClient();
        executor    = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public void createPost(Context context,
                           String userId,
                           String userName,
                           String userUnit,
                           String caption,
                           Uri imageUri,
                           PostCallback callback) {

        executor.execute(() -> {
            try {
                String imageUrl = null;
                if (imageUri != null) {
                    imageUrl = uploadImage(context, imageUri, userId);
                }

                String postId = insertPost(userId, userName, userUnit, caption, imageUrl);

                final String finalId = postId;
                mainHandler.post(() -> callback.onSuccess(finalId));

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> callback.onError(
                        e.getMessage() != null ? e.getMessage() : "Unknown error"));
            }
        });
    }


    private String uploadImage(Context context, Uri imageUri, String userId) throws Exception {

        byte[] imageBytes = compressImage(context, imageUri);

        // Unique file path: userId/timestamp.jpg
        String fileName  = userId + "/" + System.currentTimeMillis() + ".jpg";
        String uploadUrl = BASE_URL + "/storage/v1/object/" + BUCKET + "/" + fileName;

        RequestBody body = RequestBody.create(imageBytes, MediaType.parse("image/jpeg"));

        Request request = new Request.Builder()
                .url(uploadUrl)
                .post(body)
                .addHeader("Authorization", "Bearer " + ANON_KEY)
                .addHeader("apikey",        ANON_KEY)
                .addHeader("Content-Type",  "image/jpeg")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "";
                throw new Exception("Image upload failed (" + response.code() + "): " + err);
            }
        }

        // Return public URL to store in DB
        return BASE_URL + "/storage/v1/object/public/" + BUCKET + "/" + fileName;
    }


    private String insertPost(String userId,
                              String userName,
                              String userUnit,
                              String caption,
                              String imageUrl) throws Exception {

        JSONObject json = new JSONObject();
        json.put("user_id",   userId);
        json.put("user_name", userName);
        json.put("user_unit", userUnit);
        json.put("caption",   caption != null ? caption : "");
        if (imageUrl != null) {
            json.put("image_url", imageUrl);
        }

        RequestBody body = RequestBody.create(
                json.toString(), MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(BASE_URL + "/rest/v1/" + TABLE)
                .post(body)
                .addHeader("Authorization", "Bearer " + ANON_KEY)
                .addHeader("apikey",        ANON_KEY)
                .addHeader("Content-Type",  "application/json")
                .addHeader("Prefer",        "return=representation")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "";
                throw new Exception("Insert failed (" + response.code() + "): " + err);
            }
            String responseBody = response.body() != null ? response.body().string() : "[]";
            JSONArray array = new JSONArray(responseBody);
            return array.getJSONObject(0).getString("id");
        }
    }

    private byte[] compressImage(Context context, Uri uri) throws Exception {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        if (inputStream == null) throw new Exception("Cannot open image");

        Bitmap original = BitmapFactory.decodeStream(inputStream);
        inputStream.close();
        if (original == null) throw new Exception("Cannot decode image");

        Bitmap scaled = scaleBitmap(original, MAX_IMAGE_SIZE);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);

        if (scaled != original) scaled.recycle();
        original.recycle();

        return out.toByteArray();
    }

    private Bitmap scaleBitmap(Bitmap src, int maxSize) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= maxSize && h <= maxSize) return src;

        float scale = maxSize / (float) Math.max(w, h);
        int   newW  = Math.round(w * scale);
        int   newH  = Math.round(h * scale);
        return Bitmap.createScaledBitmap(src, newW, newH, true);
    }
}