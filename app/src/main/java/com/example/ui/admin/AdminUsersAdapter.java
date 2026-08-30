package com.example.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;
import com.example.R;
import com.example.model.User;
import java.util.ArrayList;
import java.util.List;

public class AdminUsersAdapter extends RecyclerView.Adapter<AdminUsersAdapter.ViewHolder> {

    public interface OnUserModerateListener {
        void onToggleBlock(User user);
    }

    private final List<User> userList = new ArrayList<>();
    private final OnUserModerateListener listener;

    public AdminUsersAdapter(OnUserModerateListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<User> list) {
        userList.clear();
        if (list != null) {
            userList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_user, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = userList.get(position);
        holder.tvAdminUserName.setText(user.getDisplayName() + (user.isAdmin() ? " (Admin)" : ""));
        holder.tvAdminUserMeta.setText(user.getEmail() + " • UID: " + user.getUid());

        if (user.isBlocked()) {
            holder.btnToggleBlock.setText("Unblock");
            holder.btnToggleBlock.setTextColor(0xFF00E676);
        } else {
            holder.btnToggleBlock.setText("Block");
            holder.btnToggleBlock.setTextColor(0xFFFF3366);
        }

        holder.btnToggleBlock.setOnClickListener(v -> {
            if (listener != null) listener.onToggleBlock(user);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAdminUserAvatar;
        TextView tvAdminUserName, tvAdminUserMeta;
        AppCompatButton btnToggleBlock;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAdminUserAvatar = itemView.findViewById(R.id.ivAdminUserAvatar);
            tvAdminUserName = itemView.findViewById(R.id.tvAdminUserName);
            tvAdminUserMeta = itemView.findViewById(R.id.tvAdminUserMeta);
            btnToggleBlock = itemView.findViewById(R.id.btnToggleBlock);
        }
    }
}
