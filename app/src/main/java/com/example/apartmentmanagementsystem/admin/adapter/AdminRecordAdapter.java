package com.example.apartmentmanagementsystem.admin.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apartmentmanagementsystem.R;
import com.example.apartmentmanagementsystem.admin.model.MaintenancePriority;
import com.example.apartmentmanagementsystem.admin.model.MaintenanceStatus;
import com.example.apartmentmanagementsystem.admin.model.UserMaintenanceRecord;

import java.util.ArrayList;
import java.util.List;

public class AdminRecordAdapter extends RecyclerView.Adapter<AdminRecordAdapter.RecordViewHolder> {

    public interface OnRecordClickListener {
        void onRecordClick(UserMaintenanceRecord record);
    }

    private final List<UserMaintenanceRecord> items = new ArrayList<>();
    private final OnRecordClickListener clickListener;

    public AdminRecordAdapter(OnRecordClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void submitList(List<UserMaintenanceRecord> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.admin_item_user_record, parent, false);
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        UserMaintenanceRecord record = items.get(position);
        holder.bind(record, clickListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class RecordViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvRecordId;
        private final TextView tvSubmittedDate;
        private final TextView tvIssue;
        private final TextView tvResident;
        private final TextView tvMeta;
        private final TextView tvStatus;
        private final TextView tvPriority;

        RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRecordId = itemView.findViewById(R.id.tvRecordId);
            tvSubmittedDate = itemView.findViewById(R.id.tvSubmittedDate);
            tvIssue = itemView.findViewById(R.id.tvIssueTitle);
            tvResident = itemView.findViewById(R.id.tvResidentName);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPriority = itemView.findViewById(R.id.tvPriority);
        }

        void bind(UserMaintenanceRecord record, OnRecordClickListener clickListener) {
            tvRecordId.setText(record.getRecordId());
            tvSubmittedDate.setText(record.getSubmittedDate());
            tvIssue.setText(record.getIssueSummary());
            tvResident.setText(record.getResidentName());
            tvMeta.setText(record.getMaintenanceType() + " • Unit " + record.getUnit());
            tvStatus.setText(formatStatus(record.getStatus()));
            tvPriority.setText(formatPriority(record.getPriority()));
            tvStatus.setBackgroundColor(statusColor(record.getStatus()));
            tvPriority.setBackgroundColor(priorityColor(record.getPriority()));

            itemView.setOnClickListener(v -> clickListener.onRecordClick(record));
        }

        private String formatStatus(MaintenanceStatus status) {
            switch (status) {
                case IN_PROGRESS:
                    return "In Progress";
                case RESOLVED:
                    return "Resolved";
                case OVERDUE:
                    return "Overdue";
                default:
                    return "Pending";
            }
        }

        private String formatPriority(MaintenancePriority priority) {
            switch (priority) {
                case HIGH:
                    return "High";
                case MEDIUM:
                    return "Medium";
                default:
                    return "Low";
            }
        }

        private int statusColor(MaintenanceStatus status) {
            switch (status) {
                case IN_PROGRESS:
                    return Color.parseColor("#DBEAFE");
                case RESOLVED:
                    return Color.parseColor("#DCFCE7");
                case OVERDUE:
                    return Color.parseColor("#FFE4E6");
                default:
                    return Color.parseColor("#FEF3C7");
            }
        }

        private int priorityColor(MaintenancePriority priority) {
            switch (priority) {
                case HIGH:
                    return Color.parseColor("#FECACA");
                case MEDIUM:
                    return Color.parseColor("#FDE68A");
                default:
                    return Color.parseColor("#BFDBFE");
            }
        }
    }
}

