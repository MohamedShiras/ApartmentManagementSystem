package com.example.apartmentmanagementsystem.admin.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apartmentmanagementsystem.R;
import com.example.apartmentmanagementsystem.admin.model.Announcement;

import java.util.ArrayList;
import java.util.List;

public class AdminAnnouncementAdapter extends RecyclerView.Adapter<AdminAnnouncementAdapter.AnnouncementViewHolder> {

    public interface OnAnnouncementActionListener {
        void onEdit(Announcement announcement);
        void onDelete(Announcement announcement);
        void onStatusChange(Announcement announcement, Announcement.AnnouncementStatus newStatus);
    }

    private final List<Announcement> items = new ArrayList<>();
    private final OnAnnouncementActionListener actionListener;

    public AdminAnnouncementAdapter(OnAnnouncementActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void submitList(List<Announcement> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AnnouncementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.admin_item_announcement_card_item, parent, false);
        return new AnnouncementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AnnouncementViewHolder holder, int position) {
        Announcement announcement = items.get(position);
        holder.bind(announcement, actionListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class AnnouncementViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvTitle;
        private final TextView tvBody;
        private final TextView tvSender;
        private final TextView tvTime;
        private final TextView tvType;
        private final TextView tvStatus;
        private final TextView tvAudience;
        private final TextView tvLikes;
        private final TextView tvComments;
        private final ImageView btnDelete;
        private final ImageView btnEdit;

        AnnouncementViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvBody = itemView.findViewById(R.id.tvBody);
            tvSender = itemView.findViewById(R.id.tvSender);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvType = itemView.findViewById(R.id.tvType);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvAudience = itemView.findViewById(R.id.tvAudience);
            tvLikes = itemView.findViewById(R.id.tvLikes);
            tvComments = itemView.findViewById(R.id.tvComments);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnEdit = itemView.findViewById(R.id.btnEdit);
        }

        void bind(Announcement announcement, OnAnnouncementActionListener actionListener) {
            tvTitle.setText(announcement.getTitle());
            tvBody.setText(announcement.getBody());
            tvSender.setText(announcement.getSender());
            tvTime.setText(announcement.getTimestamp());
            tvAudience.setText(announcement.getAudience());
            tvLikes.setText(announcement.getLikes() + " Likes");
            tvComments.setText(announcement.getComments() + " Comments");

            tvType.setText(announcement.getType().name());
            applyPillBackground(tvType, getTypeColor(announcement.getType()), getTypeTextColor(announcement.getType()));

            tvStatus.setText(formatStatus(announcement.getStatus()));
            applyPillBackground(tvStatus, getStatusColor(announcement.getStatus()), getStatusTextColor(announcement.getStatus()));

            btnEdit.setOnClickListener(v -> actionListener.onEdit(announcement));
            btnDelete.setOnClickListener(v -> actionListener.onDelete(announcement));
        }

        private String formatStatus(Announcement.AnnouncementStatus status) {
            switch (status) {
                case PUBLISHED_LIVE:
                    return "Live";
                case SCHEDULED:
                    return "Scheduled";
                case NEEDS_REVIEW:
                    return "Review";
                default:
                    return "Draft";
            }
        }

        private int getTypeColor(Announcement.AnnouncementType type) {
            switch (type) {
                case ALERT:
                    return Color.parseColor("#FFE7E7");
                case NOTICE:
                    return Color.parseColor("#FFF4E5");
                case INFO:
                    return Color.parseColor("#E8F1FF");
                case EVENT:
                    return Color.parseColor("#F1E9FF");
                case UPDATE:
                    return Color.parseColor("#E8F7ED");
                default:
                    return Color.parseColor("#F5F5F5");
            }
        }

        private int getTypeTextColor(Announcement.AnnouncementType type) {
            switch (type) {
                case ALERT:
                    return Color.parseColor("#B42318");
                case NOTICE:
                    return Color.parseColor("#B54708");
                case INFO:
                    return Color.parseColor("#1D4ED8");
                case EVENT:
                    return Color.parseColor("#7E22CE");
                case UPDATE:
                    return Color.parseColor("#15803D");
                default:
                    return Color.parseColor("#374151");
            }
        }

        private int getStatusColor(Announcement.AnnouncementStatus status) {
            switch (status) {
                case PUBLISHED_LIVE:
                    return Color.parseColor("#DCFCE7");
                case SCHEDULED:
                    return Color.parseColor("#DBEAFE");
                case NEEDS_REVIEW:
                    return Color.parseColor("#FFEDD5");
                default:
                    return Color.parseColor("#E5E7EB");
            }
        }

        private int getStatusTextColor(Announcement.AnnouncementStatus status) {
            switch (status) {
                case PUBLISHED_LIVE:
                    return Color.parseColor("#166534");
                case SCHEDULED:
                    return Color.parseColor("#1D4ED8");
                case NEEDS_REVIEW:
                    return Color.parseColor("#B45309");
                default:
                    return Color.parseColor("#4B5563");
            }
        }

        private void applyPillBackground(TextView view, int backgroundColor, int textColor) {
            GradientDrawable pill = new GradientDrawable();
            pill.setShape(GradientDrawable.RECTANGLE);
            pill.setCornerRadius(dpToPx(10));
            pill.setColor(backgroundColor);
            view.setBackground(pill);
            view.setTextColor(textColor);
        }

        private float dpToPx(int dp) {
            return TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    dp,
                    itemView.getResources().getDisplayMetrics()
            );
        }
    }
}
