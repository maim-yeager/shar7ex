package com.example.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.R;
import com.example.model.ChatMessage;
import com.example.util.FileUtils;
import java.util.ArrayList;
import java.util.List;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder> {

    public interface OnMessageMediaClickListener {
        void onMediaClick(ChatMessage message);
    }

    private final String currentUserId;
    private final List<ChatMessage> messages = new ArrayList<>();
    private final OnMessageMediaClickListener mediaClickListener;

    public MessagesAdapter(String currentUserId, OnMessageMediaClickListener mediaClickListener) {
        this.currentUserId = currentUserId;
        this.mediaClickListener = mediaClickListener;
    }

    public void setMessages(List<ChatMessage> list) {
        messages.clear();
        if (list != null) {
            messages.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        boolean isOutgoing = msg.getSenderId() != null && msg.getSenderId().equals(currentUserId);

        if (isOutgoing) {
            holder.layoutOutgoing.setVisibility(View.VISIBLE);
            holder.layoutIncoming.setVisibility(View.GONE);

            if ("TEXT".equalsIgnoreCase(msg.getMessageType())) {
                holder.tvOutgoingText.setVisibility(View.VISIBLE);
                holder.tvOutgoingText.setText(msg.getText());
                holder.cardOutgoingMedia.setVisibility(View.GONE);
            } else {
                holder.tvOutgoingText.setVisibility(View.GONE);
                holder.cardOutgoingMedia.setVisibility(View.VISIBLE);
                holder.tvOutgoingMediaName.setText(msg.getFileName());
                holder.tvOutgoingQualityBadge.setText(msg.isOriginalQuality() ? "100% ORIGINAL" : "COMPRESSED");
                holder.cardOutgoingMedia.setOnClickListener(v -> {
                    if (mediaClickListener != null) mediaClickListener.onMediaClick(msg);
                });
            }
            holder.tvOutgoingTime.setText(FileUtils.formatChatTime(msg.getTimestamp()) + " • Sent");
        } else {
            holder.layoutOutgoing.setVisibility(View.GONE);
            holder.layoutIncoming.setVisibility(View.VISIBLE);

            if ("TEXT".equalsIgnoreCase(msg.getMessageType())) {
                holder.tvIncomingText.setVisibility(View.VISIBLE);
                holder.tvIncomingText.setText(msg.getText());
                holder.cardIncomingMedia.setVisibility(View.GONE);
            } else {
                holder.tvIncomingText.setVisibility(View.GONE);
                holder.cardIncomingMedia.setVisibility(View.VISIBLE);
                holder.tvIncomingMediaName.setText(msg.getFileName());
                holder.tvIncomingQualityBadge.setText(msg.isOriginalQuality() ? "100% ORIGINAL" : "COMPRESSED");
                holder.cardIncomingMedia.setOnClickListener(v -> {
                    if (mediaClickListener != null) mediaClickListener.onMediaClick(msg);
                });
            }
            holder.tvIncomingTime.setText(FileUtils.formatChatTime(msg.getTimestamp()));
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View layoutOutgoing, layoutIncoming, cardOutgoingMedia, cardIncomingMedia;
        TextView tvOutgoingText, tvIncomingText, tvOutgoingMediaName, tvIncomingMediaName, tvOutgoingTime, tvIncomingTime, tvOutgoingQualityBadge, tvIncomingQualityBadge;
        ImageView ivOutgoingMediaPreview, ivIncomingMediaPreview;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutOutgoing = itemView.findViewById(R.id.layoutOutgoing);
            layoutIncoming = itemView.findViewById(R.id.layoutIncoming);
            cardOutgoingMedia = itemView.findViewById(R.id.cardOutgoingMedia);
            cardIncomingMedia = itemView.findViewById(R.id.cardIncomingMedia);

            tvOutgoingText = itemView.findViewById(R.id.tvOutgoingText);
            tvIncomingText = itemView.findViewById(R.id.tvIncomingText);
            tvOutgoingMediaName = itemView.findViewById(R.id.tvOutgoingMediaName);
            tvIncomingMediaName = itemView.findViewById(R.id.tvIncomingMediaName);
            tvOutgoingTime = itemView.findViewById(R.id.tvOutgoingTime);
            tvIncomingTime = itemView.findViewById(R.id.tvIncomingTime);
            tvOutgoingQualityBadge = itemView.findViewById(R.id.tvOutgoingQualityBadge);
            tvIncomingQualityBadge = itemView.findViewById(R.id.tvIncomingQualityBadge);

            ivOutgoingMediaPreview = itemView.findViewById(R.id.ivOutgoingMediaPreview);
            ivIncomingMediaPreview = itemView.findViewById(R.id.ivIncomingMediaPreview);
        }
    }
}
