package com.example.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.R;
import com.example.model.ChatConversation;
import com.example.util.FileUtils;
import java.util.ArrayList;
import java.util.List;

public class ConversationsAdapter extends RecyclerView.Adapter<ConversationsAdapter.ViewHolder> {

    public interface OnConversationClickListener {
        void onConversationClick(ChatConversation conversation);
    }

    private final List<ChatConversation> conversations = new ArrayList<>();
    private final OnConversationClickListener listener;

    public ConversationsAdapter(OnConversationClickListener listener) {
        this.listener = listener;
    }

    public void setConversations(List<ChatConversation> list) {
        conversations.clear();
        if (list != null) {
            conversations.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatConversation c = conversations.get(position);
        holder.tvChatUserName.setText(c.getOtherUserName());
        holder.tvChatLastMessage.setText(c.getLastMessage());
        holder.tvChatTimestamp.setText(FileUtils.formatChatTime(c.getLastMessageTime()));
        holder.vChatOnline.setVisibility(c.isOtherUserOnline() ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onConversationClick(c);
        });
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivChatAvatar;
        View vChatOnline;
        TextView tvChatUserName, tvChatTimestamp, tvChatLastMessage;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivChatAvatar = itemView.findViewById(R.id.ivChatAvatar);
            vChatOnline = itemView.findViewById(R.id.vChatOnline);
            tvChatUserName = itemView.findViewById(R.id.tvChatUserName);
            tvChatTimestamp = itemView.findViewById(R.id.tvChatTimestamp);
            tvChatLastMessage = itemView.findViewById(R.id.tvChatLastMessage);
        }
    }
}
