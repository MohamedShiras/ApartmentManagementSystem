package com.example.apartmentmanagementsystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class AdminReservationMaintenanceAdapter extends RecyclerView.Adapter<AdminReservationMaintenanceAdapter.ViewHolder> {

    public interface OnReservationActionListener {
        void onEditClick(AdminReservation reservation);
        void onDeleteClick(int position);
    }

    private final List<AdminReservation> reservations;
    private final OnReservationActionListener listener;

    public AdminReservationMaintenanceAdapter(List<AdminReservation> reservations,
                                              OnReservationActionListener listener) {
        this.reservations = reservations;
        this.listener = listener;
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
        AdminReservation reservation = reservations.get(position);

        String imageUri = reservation.getImageUri();
        if (imageUri != null && !imageUri.trim().isEmpty()) {
            Glide.with(holder.itemView)
                    .load(imageUri)
                    .placeholder(R.drawable.apartment_logo)
                    .error(R.drawable.apartment_logo)
                    .into(holder.ivReservationImage);
        } else {
            holder.ivReservationImage.setImageResource(reservation.getImageResId());
        }

        holder.tvStatus.setText(reservation.getStatus());
        holder.tvServiceName.setText(reservation.getServiceName());
        holder.tvBookedBy.setText("Booked by: " + reservation.getBookedBy());
        holder.tvDescription.setText(reservation.getDescription());
        holder.tvDateTime.setText(reservation.getDateTime());

        if ("Pending".equalsIgnoreCase(reservation.getStatus())) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_pending);
            holder.tvStatus.setTextColor(0xFFF39C12);
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_confirmed);
            holder.tvStatus.setTextColor(0xFF2ECC71);
        }

        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(reservation));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(holder.getBindingAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return reservations.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivReservationImage;
        TextView tvStatus;
        TextView tvServiceName;
        TextView tvBookedBy;
        TextView tvDescription;
        TextView tvDateTime;
        MaterialButton btnEdit;
        MaterialButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivReservationImage = itemView.findViewById(R.id.ivBookedReservationImage);
            tvStatus = itemView.findViewById(R.id.tvBookingStatus);
            tvServiceName = itemView.findViewById(R.id.tvServiceName);
            tvBookedBy = itemView.findViewById(R.id.tvBookedByUser);
            tvDescription = itemView.findViewById(R.id.tvBookingDescription);
            tvDateTime = itemView.findViewById(R.id.tvBookedDateTime);
            btnEdit = itemView.findViewById(R.id.btnEditBookedReservation);
            btnDelete = itemView.findViewById(R.id.btnDeleteBookedReservation);
        }
    }
}
