package com.example.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.R;
import com.example.model.TransferItem;
import com.example.util.FileUtils;
import java.util.ArrayList;
import java.util.List;

public class RecentTransfersAdapter extends RecyclerView.Adapter<RecentTransfersAdapter.ViewHolder> {

    public interface OnTransferClickListener {
        void onTransferClick(TransferItem item);
    }

    private final List<TransferItem> items = new ArrayList<>();
    private final OnTransferClickListener listener;

    public RecentTransfersAdapter(OnTransferClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<TransferItem> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_transfer, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransferItem item = items.get(position);
        holder.tvFileName.setText(item.getFileName());

        String meta = FileUtils.formatFileSize(item.getOriginalSizeBytes());
        if (item.getOriginalWidth() > 0) {
            meta += " • " + item.getOriginalWidth() + "x" + item.getOriginalHeight();
        }
        if (item.isVideo() && item.getVideoFps() > 0) {
            meta += " • " + (int)item.getVideoFps() + " FPS";
        }
        holder.tvFileMeta.setText(meta);

        if (item.isVideo()) {
            holder.ivMediaTypeIcon.setImageResource(R.drawable.ic_video);
            holder.ivActionBtn.setImageResource(R.drawable.ic_play);
        } else {
            holder.ivMediaTypeIcon.setImageResource(R.drawable.ic_image);
            holder.ivActionBtn.setImageResource(R.drawable.ic_image);
        }

        holder.tvOriginalQualityTag.setText(item.isOriginalQuality() ? "100% ORIGINAL" : "COMPRESSED");
        holder.tvOriginalQualityTag.setBackgroundResource(item.isOriginalQuality() ? R.drawable.bg_badge_hq : R.drawable.bg_badge_compressed);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTransferClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivMediaTypeIcon, ivActionBtn;
        TextView tvFileName, tvFileMeta, tvOriginalQualityTag;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMediaTypeIcon = itemView.findViewById(R.id.ivMediaTypeIcon);
            ivActionBtn = itemView.findViewById(R.id.ivActionBtn);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileMeta = itemView.findViewById(R.id.tvFileMeta);
            tvOriginalQualityTag = itemView.findViewById(R.id.tvOriginalQualityTag);
        }
    }
}
