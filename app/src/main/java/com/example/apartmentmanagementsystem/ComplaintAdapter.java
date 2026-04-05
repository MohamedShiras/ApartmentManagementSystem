package com.example.apartmentmanagementsystem;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ComplaintAdapter extends RecyclerView.Adapter<ComplaintAdapter.ComplaintViewHolder> {

    private List<Complaint> complaints;
    private OnComplaintClickListener listener;

    public interface OnComplaintClickListener {
        void onComplaintClick(Complaint complaint);
    }

    public ComplaintAdapter(List<Complaint> complaints, OnComplaintClickListener listener) {
        this.complaints = complaints;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ComplaintViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_complaint, parent, false);
        return new ComplaintViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ComplaintViewHolder holder, int position) {
        Complaint complaint = complaints.get(position);
        holder.textCategory.setText(complaint.getCategory());
        holder.textDate.setText(complaint.getDate());
        holder.textSubject.setText(complaint.getSubject());
        holder.textDescription.setText(complaint.getDescription());
        holder.textStatus.setText(complaint.getStatus());

        // Status color coding
        if ("Resolved".equalsIgnoreCase(complaint.getStatus())) {
            holder.textStatus.setTextColor(Color.parseColor("#10B981"));
        } else if ("In Progress".equalsIgnoreCase(complaint.getStatus())) {
            holder.textStatus.setTextColor(Color.parseColor("#EAB308"));
        } else {
            holder.textStatus.setTextColor(Color.parseColor("#EF4444"));
        }

        // Priority color coding
        String priority = complaint.getPriority();
        if (priority != null) {
            holder.textPriority.setText(priority);
            if ("High".equalsIgnoreCase(priority)) {
                holder.textPriority.setTextColor(Color.parseColor("#EF4444"));
                holder.textPriority.setBackgroundColor(Color.parseColor("#FEF2F2"));
            } else if ("Medium".equalsIgnoreCase(priority)) {
                holder.textPriority.setTextColor(Color.parseColor("#F59E0B"));
                holder.textPriority.setBackgroundColor(Color.parseColor("#FFFBEB"));
            } else {
                holder.textPriority.setTextColor(Color.parseColor("#10B981"));
                holder.textPriority.setBackgroundColor(Color.parseColor("#ECFDF5"));
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onComplaintClick(complaint);
        });
    }

    @Override
    public int getItemCount() {
        return complaints.size();
    }

    static class ComplaintViewHolder extends RecyclerView.ViewHolder {
        TextView textCategory, textDate, textSubject, textDescription, textStatus, textPriority;

        public ComplaintViewHolder(@NonNull View itemView) {
            super(itemView);
            textCategory    = itemView.findViewById(R.id.textCategory);
            textDate        = itemView.findViewById(R.id.textDate);
            textSubject     = itemView.findViewById(R.id.textSubject);
            textDescription = itemView.findViewById(R.id.textDescription);
            textStatus      = itemView.findViewById(R.id.textStatus);
            textPriority    = itemView.findViewById(R.id.textPriority);
        }
    }
}