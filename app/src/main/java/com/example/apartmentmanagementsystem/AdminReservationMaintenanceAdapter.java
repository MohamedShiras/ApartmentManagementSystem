package com.example.apartmentmanagementsystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class AdminReservationMaintenanceAdapter
        extends RecyclerView.Adapter<AdminReservationMaintenanceAdapter.ViewHolder> {

    public interface ReservationActionListener {
        void onEdit(AdminReservation reservation);
        void onDelete(AdminReservation reservation);
    }

    private final List<AdminReservation> items = new ArrayList<>();
    private final ReservationActionListener actionListener;

    public AdminReservationMaintenanceAdapter(List<AdminReservation> reservations, ReservationActionListener actionListener) {
        this.items.addAll(reservations);
        this.actionListener = actionListener;
    }

    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
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
        holder.txtTime.setText(safe(reservation.getTimePeriod(), "Time Period"));
        holder.txtCapacity.setText(safe(reservation.getMaxGuests(), "Max Guests"));

        holder.btnEdit.setOnClickListener(v -> actionListener.onEdit(reservation));
        holder.btnDelete.setOnClickListener(v -> actionListener.onDelete(reservation));
        holder.itemView.setOnClickListener(v -> actionListener.onEdit(reservation));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtServiceName;
        TextView txtDescription;
        TextView txtTime;
        TextView txtCapacity;
        MaterialButton btnEdit;
        MaterialButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtServiceName = itemView.findViewById(R.id.txtServiceName);
            txtDescription = itemView.findViewById(R.id.txtDescription);
            txtTime = itemView.findViewById(R.id.txtTime);
            txtCapacity = itemView.findViewById(R.id.txtCapacity);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
