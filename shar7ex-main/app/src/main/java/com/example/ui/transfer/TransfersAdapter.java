package com.example.ui.transfer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;
import com.example.R;
import com.example.model.TransferItem;
import com.example.util.FileUtils;
import java.util.ArrayList;
import java.util.List;

public class TransfersAdapter extends RecyclerView.Adapter<TransfersAdapter.ViewHolder> {

    public interface OnTransferActionListener {
        void onViewMedia(TransferItem item);
        void onShareLink(TransferItem item);
        void onShowDetails(TransferItem item);
    }

    private final List<TransferItem> list = new ArrayList<>();
    private final OnTransferActionListener listener;
    private String currentUid;

    public TransfersAdapter(OnTransferActionListener listener) {
        this.listener = listener;
    }

    public void setCurrentUid(String currentUid) {
        this.currentUid = currentUid;
        notifyDataSetChanged();
    }

    public void setTransfers(List<TransferItem> items) {
        list.clear();
        if (items != null) {
            list.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transfer_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransferItem item = list.get(position);
        holder.tvTransferFileName.setText(item.getFileName());

        boolean iAmSender = currentUid != null && currentUid.equals(item.getSenderUid());
        String peerLabel = iAmSender
                ? "To: " + safeName(item.getReceiverName())
                : "From: " + safeName(item.getSenderName());
        holder.tvTransferPeers.setText(peerLabel + " • " + FileUtils.formatDate(item.getTimestamp()));

        String sizeRes = FileUtils.formatFileSize(item.getOriginalSizeBytes());
        if (item.getOriginalWidth() > 0) {
            sizeRes += " • " + item.getOriginalWidth() + "x" + item.getOriginalHeight();
        }
        holder.tvTransferSize.setText(sizeRes);
        holder.tvQualityBadge.setText(item.isOriginalQuality() ? "100% ORIGINAL" : "COMPRESSED");
        holder.tvQualityBadge.setBackgroundResource(item.isOriginalQuality() ? R.drawable.bg_badge_hq : R.drawable.bg_badge_compressed);

        holder.tvTransferStatus.setText(item.getStatus());
        if ("UPLOADING".equalsIgnoreCase(item.getStatus()) || "DOWNLOADING".equalsIgnoreCase(item.getStatus())) {
            holder.pbTransferProgress.setVisibility(View.VISIBLE);
            holder.pbTransferProgress.setProgress(item.getProgress());
        } else {
            holder.pbTransferProgress.setVisibility(View.GONE);
        }

        if (item.isVideo()) {
            holder.ivTransferIcon.setImageResource(R.drawable.ic_video);
        } else {
            holder.ivTransferIcon.setImageResource(R.drawable.ic_image);
        }

        holder.btnViewMedia.setOnClickListener(v -> {
            if (listener != null) listener.onViewMedia(item);
        });

        holder.btnShareLink.setOnClickListener(v -> {
            if (listener != null) listener.onShareLink(item);
        });

        holder.btnTransferDetails.setOnClickListener(v -> {
            if (listener != null) listener.onShowDetails(item);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private static String safeName(String name) {
        return name != null && !name.isEmpty() ? name : "SHAREX User";
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivTransferIcon, btnTransferDetails;
        TextView tvTransferFileName, tvTransferPeers, tvQualityBadge, tvTransferSize, tvTransferStatus;
        ProgressBar pbTransferProgress;
        AppCompatButton btnViewMedia, btnShareLink;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivTransferIcon = itemView.findViewById(R.id.ivTransferIcon);
            btnTransferDetails = itemView.findViewById(R.id.btnTransferDetails);
            tvTransferFileName = itemView.findViewById(R.id.tvTransferFileName);
            tvTransferPeers = itemView.findViewById(R.id.tvTransferPeers);
            tvQualityBadge = itemView.findViewById(R.id.tvQualityBadge);
            tvTransferSize = itemView.findViewById(R.id.tvTransferSize);
            tvTransferStatus = itemView.findViewById(R.id.tvTransferStatus);
            pbTransferProgress = itemView.findViewById(R.id.pbTransferProgress);
            btnViewMedia = itemView.findViewById(R.id.btnViewMedia);
            btnShareLink = itemView.findViewById(R.id.btnShareLink);
        }
    }
}
