package com.example.apartmentmanagementsystem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_SENT     = 1;
    private static final int VIEW_RECEIVED = 2;

    private final Context       context;
    private final List<Message> messages;
    private final String        currentUserId;
    private OnMessageActionListener listener;

    public interface OnMessageActionListener {
        void onLongPress(Message message, boolean isOwn);
    }

    public ChatAdapter(Context context, List<Message> messages, String currentUserId) {
        this.context       = context;
        this.messages      = messages;
        this.currentUserId = currentUserId;
    }

    public void setOnMessageActionListener(OnMessageActionListener l) { this.listener = l; }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getUserId().equals(currentUserId)
                ? VIEW_SENT : VIEW_RECEIVED;
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(context);
        if (viewType == VIEW_SENT) {
            View v = inf.inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(v);
        } else {
            View v = inf.inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message msg = messages.get(position);
        if (holder instanceof SentViewHolder)     bindSent((SentViewHolder) holder, msg);
        else                                      bindReceived((ReceivedViewHolder) holder, msg);
    }

    private void bindSent(SentViewHolder h, Message msg) {
        h.tvContent.setText(msg.getContent());
        h.tvTime.setText(formatTime(msg.getCreatedAt()));

        if (msg.getReplyToContent() != null && !msg.getReplyToContent().isEmpty()) {
            h.layoutReply.setVisibility(View.VISIBLE);
            h.tvReplyPreview.setText(msg.getReplyToContent());
        } else {
            h.layoutReply.setVisibility(View.GONE);
        }

        h.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onLongPress(msg, true);
            return true;
        });
    }

    private void bindReceived(ReceivedViewHolder h, Message msg) {
        h.tvContent.setText(msg.getContent());
        h.tvTime.setText(formatTime(msg.getCreatedAt()));

        String name = msg.getSenderName() != null && !msg.getSenderName().isEmpty()
                ? msg.getSenderName() : "Resident";
        h.tvSenderName.setText(name);
        h.tvAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());

        if (msg.getReplyToContent() != null && !msg.getReplyToContent().isEmpty()) {
            h.layoutReply.setVisibility(View.VISIBLE);
            h.tvReplyPreview.setText(msg.getReplyToContent());
        } else {
            h.layoutReply.setVisibility(View.GONE);
        }

        h.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onLongPress(msg, false);
            return true;
        });
    }

    @Override
    public int getItemCount() { return messages.size(); }

    public void updateMessages(List<Message> newList) {
        messages.clear();
        messages.addAll(newList);
        notifyDataSetChanged();
    }

    private String formatTime(String iso) {
        if (iso == null || iso.length() < 19) return "";
        try {
            SimpleDateFormat in  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            in.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = in.parse(iso.substring(0, 19));
            SimpleDateFormat out = new SimpleDateFormat("h:mm a", Locale.getDefault());
            return out.format(date);
        } catch (Exception e) {
            return "";
        }
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView      tvContent, tvTime, tvReplyPreview;
        LinearLayout  layoutReply;

        SentViewHolder(View v) {
            super(v);
            tvContent      = v.findViewById(R.id.tvMessageContent);
            tvTime         = v.findViewById(R.id.tvMessageTime);
            tvReplyPreview = v.findViewById(R.id.tvReplyPreview);
            layoutReply    = v.findViewById(R.id.layoutReplyPreview);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView      tvContent, tvTime, tvSenderName, tvReplyPreview, tvAvatar;
        LinearLayout  layoutReply;

        ReceivedViewHolder(View v) {
            super(v);
            tvContent      = v.findViewById(R.id.tvMessageContent);
            tvTime         = v.findViewById(R.id.tvMessageTime);
            tvSenderName   = v.findViewById(R.id.tvSenderName);
            tvReplyPreview = v.findViewById(R.id.tvReplyPreview);
            layoutReply    = v.findViewById(R.id.layoutReplyPreview);
            tvAvatar       = v.findViewById(R.id.tvAvatar);
        }
    }
}