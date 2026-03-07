package com.example.apartmentmanagementsystem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class Noticeadapter extends RecyclerView.Adapter<Noticeadapter.NoticeViewHolder> {

    private Context context;
    private List<Notice> noticeList;

    public Noticeadapter(Context context, List<Notice> noticeList) {
        this.context = context;
        this.noticeList = noticeList;
    }

    @NonNull
    @Override
    public NoticeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notices, parent, false);
        return new NoticeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoticeViewHolder holder, int position) {
        Notice notice = noticeList.get(position);

        holder.tvSender.setText(notice.getSender());
        holder.tvTime.setText(notice.getTimeAgo());
        holder.tvBadge.setText(notice.getBadge());
        holder.tvTitle.setText(notice.getTitle());
        holder.tvBody.setText(notice.getBody());
        holder.tvLikes.setText(notice.getLikes() + " Likes");
        holder.tvComments.setText(notice.getComments() + " Comments");

        // Like button toggle
        holder.btnLike.setOnClickListener(v -> {
            if (notice.isLiked()) {
                notice.setLiked(false);
                notice.setLikes(notice.getLikes() - 1);
            } else {
                notice.setLiked(true);
                notice.setLikes(notice.getLikes() + 1);
            }
            notifyItemChanged(position);
        });

        // See more
        holder.tvSeeMore.setOnClickListener(v -> {
            Toast.makeText(context, "Opening: " + notice.getTitle(), Toast.LENGTH_SHORT).show();
            // TODO: Open full notice detail activity
        });
    }

    @Override
    public int getItemCount() {
        return noticeList.size();
    }

    // Add new notice from admin
    public void addNotice(Notice notice) {
        noticeList.add(0, notice);
        notifyItemInserted(0);
    }

    // Update full list (e.g., from Firebase)
    public void updateList(List<Notice> newList) {
        noticeList.clear();
        noticeList.addAll(newList);
        notifyDataSetChanged();
    }

    static class NoticeViewHolder extends RecyclerView.ViewHolder {
        TextView tvSender, tvTime, tvBadge, tvTitle, tvBody, tvLikes, tvComments, tvSeeMore;
        ImageView btnLike;

        public NoticeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSender = itemView.findViewById(R.id.tvSender);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvBadge = itemView.findViewById(R.id.tvBadge);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvBody = itemView.findViewById(R.id.tvBody);
            tvLikes = itemView.findViewById(R.id.tvLikes);
            tvComments = itemView.findViewById(R.id.tvComments);
            tvSeeMore = itemView.findViewById(R.id.tvSeeMore);
            btnLike = itemView.findViewById(R.id.btnLike);
        }
    }
}