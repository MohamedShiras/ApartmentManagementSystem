package com.example.apartmentmanagementsystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminReservationMaintenanceAdapter
        extends RecyclerView.Adapter<AdminReservationMaintenanceAdapter.ViewHolder> {

    public interface ReservationActionListener {
        void onEdit(AdminReservation reservation);
        void onDelete(AdminReservation reservation);
    }

    private final List<AdminReservation> items = new ArrayList<>();
    private final ReservationActionListener actionListener;

    public AdminReservationMaintenanceAdapter(ReservationActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void submitList(List<AdminReservation> reservations) {
        items.clear();
        items.addAll(reservations);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_reservation_maintenance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AdminReservation reservation = items.get(position);

        holder.txtServiceName.setText(safe(reservation.getServiceName(), "Service Name"));
        holder.txtDescription.setText(safe(reservation.getDescription(), "Description"));
        holder.txtTime.setText(safe(reservation.getReservationTime(), "Time"));
        holder.txtCapacity.setText(safe(reservation.getCapacity(), "Max guests"));
        holder.txtStatus.setText(safe(reservation.getStatus(), "Available"));
        // Optionally, you can color status here if needed

        // If you want to show booked by, date, duration, add more TextViews and bind here
        // Example:
        // holder.txtBookedBy.setText(safe(reservation.getBookedBy(), "Booked By"));
        // holder.txtDate.setText(safe(reservation.getReservationDate(), "Date"));
        // holder.txtDuration.setText(safe(reservation.getDuration(), "Duration"));

        int fallbackImage = getFallbackImageRes(reservation.getServiceName());
        holder.imgReservation.setImageResource(fallbackImage);
        String imageUrl = reservation.getImageUrl();
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            holder.imgReservation.setTag(imageUrl);
            loadImageFromUrl(holder.imgReservation, imageUrl, fallbackImage);
        } else {
            holder.imgReservation.setTag(null);
        }

        holder.btnEdit.setOnClickListener(v -> actionListener.onEdit(reservation));
        holder.btnDelete.setOnClickListener(v -> actionListener.onDelete(reservation));
        holder.itemView.setOnClickListener(v -> actionListener.onEdit(reservation));
    }

    private void loadImageFromUrl(ImageView imageView, String url, int fallbackRes) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(7000);
                connection.setReadTimeout(7000);
                connection.setDoInput(true);
                connection.connect();

                if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) {
                    imageView.post(() -> imageView.setImageResource(fallbackRes));
                    return;
                }

                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(connection.getInputStream());
                imageView.post(() -> {
                    Object currentTag = imageView.getTag();
                    if (url.equals(currentTag) && bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    }
                });
            } catch (Exception e) {
                imageView.post(() -> imageView.setImageResource(fallbackRes));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private int getFallbackImageRes(String serviceName) {
        String normalized = safe(serviceName, "").toLowerCase(Locale.US);
        if (normalized.contains("pool") || normalized.contains("swim")) {
            return R.drawable.img_pool;
        }
        if (normalized.contains("gym") || normalized.contains("fitness")) {
            return R.drawable.img_gym;
        }
        if (normalized.contains("restaurant") || normalized.contains("table") || normalized.contains("dining")) {
            return R.drawable.img_restaurant;
        }
        return R.drawable.img_pool;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imgReservation;
        TextView txtServiceName;
        TextView txtDescription;
        TextView txtTime;
        TextView txtCapacity;
        TextView txtStatus;
        MaterialButton btnEdit;
        MaterialButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgReservation = itemView.findViewById(R.id.imgReservation);
            txtServiceName = itemView.findViewById(R.id.txtServiceName);
            txtDescription = itemView.findViewById(R.id.txtDescription);
            txtTime = itemView.findViewById(R.id.txtTime);
            txtCapacity = itemView.findViewById(R.id.txtCapacity);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
