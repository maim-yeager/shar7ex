package com.example.ui.transfer;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.example.R;
import com.example.util.FileUtils;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * High-performance RecyclerView Adapter for media gallery grid,
 * powered by Glide with optimized caching and thumbnail decoding.
 */
public class MediaGridAdapter extends RecyclerView.Adapter<MediaGridAdapter.ViewHolder> {

    public interface OnGalleryInteractionListener {
        void onSelectionChanged(int selectedCount, long totalSizeBytes);
        void onPreviewRequested(MediaItem item);
    }

    public static class MediaItem {
        public Uri uri;
        public String name;
        public long size;
        public boolean isVideo;
        public long durationMs;
        public boolean isHq;
    }

    private final List<MediaItem> allItems = new ArrayList<>();
    private final List<MediaItem> displayedItems = new ArrayList<>();
    private final Set<Uri> selectedUris = new HashSet<>();
    private final OnGalleryInteractionListener listener;
    private int currentFilterMode = 0; // 0: All, 1: Photos, 2: Videos, 3: Large Files

    public MediaGridAdapter(OnGalleryInteractionListener listener) {
        this.listener = listener;
    }

    public void setMediaItems(List<MediaItem> items) {
        allItems.clear();
        selectedUris.clear();
        if (items != null) {
            allItems.addAll(items);
        }
        applyFilter(currentFilterMode);
    }

    public void setFilterMode(int filterMode) {
        this.currentFilterMode = filterMode;
        applyFilter(filterMode);
    }

    private void applyFilter(int filterMode) {
        displayedItems.clear();
        for (MediaItem item : allItems) {
            switch (filterMode) {
                case 1: // Photos Only
                    if (!item.isVideo) displayedItems.add(item);
                    break;
                case 2: // Videos Only
                    if (item.isVideo) displayedItems.add(item);
                    break;
                case 3: // Large Files > 50MB
                    if (item.size >= 50L * 1024L * 1024L) displayedItems.add(item);
                    break;
                case 0: // All
                default:
                    displayedItems.add(item);
                    break;
            }
        }
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    public void toggleSelectAll() {
        if (selectedUris.size() >= displayedItems.size() && !displayedItems.isEmpty()) {
            selectedUris.clear();
        } else {
            for (MediaItem item : displayedItems) {
                selectedUris.add(item.uri);
            }
        }
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    public boolean isAllSelected() {
        return !displayedItems.isEmpty() && selectedUris.size() >= displayedItems.size();
    }

    public List<MediaItem> getSelectedItems() {
        List<MediaItem> selected = new ArrayList<>();
        for (MediaItem item : allItems) {
            if (selectedUris.contains(item.uri)) {
                selected.add(item);
            }
        }
        return selected;
    }

    private void notifySelectionChanged() {
        if (listener != null) {
            long totalBytes = 0;
            for (MediaItem item : allItems) {
                if (selectedUris.contains(item.uri)) {
                    totalBytes += item.size;
                }
            }
            listener.onSelectionChanged(selectedUris.size(), totalBytes);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_media_grid, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MediaItem item = displayedItems.get(position);
        boolean isSelected = selectedUris.contains(item.uri);

        holder.cbMediaSelected.setChecked(isSelected);
        holder.vSelectionScrim.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        holder.cardMediaItem.setStrokeColor(isSelected ? 0xFF00D2FF : 0xFF243042);
        holder.cardMediaItem.setStrokeWidth(isSelected ? 4 : 1);

        holder.tvGridSize.setText(FileUtils.formatFileSize(item.size));

        // High quality indicator
        if (item.size > 20L * 1024L * 1024L || item.isHq) {
            holder.tvGridHqBadge.setVisibility(View.VISIBLE);
            holder.tvGridHqBadge.setText(item.isVideo ? "4K UHD" : "RAW HQ");
        } else {
            holder.tvGridHqBadge.setVisibility(View.GONE);
        }

        // Video Indicator & Duration
        if (item.isVideo) {
            holder.layoutVideoDuration.setVisibility(View.VISIBLE);
            holder.tvGridDuration.setText(FileUtils.formatDuration(item.durationMs));
        } else {
            holder.layoutVideoDuration.setVisibility(View.GONE);
        }

        // Efficient Glide image/thumbnail loading with disk cache and fast downsampled thumbnail
        RequestOptions requestOptions = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .placeholder(R.drawable.bg_card_dark)
                .error(R.drawable.bg_card_dark);

        Glide.with(holder.itemView.getContext())
                .load(item.uri)
                .apply(requestOptions)
                .thumbnail(0.25f) // loads 25% downsampled preview first for instant smooth scroll
                .transition(DrawableTransitionOptions.withCrossFade(150))
                .into(holder.ivGridThumbnail);

        // Click on whole card toggles selection
        holder.cardMediaItem.setOnClickListener(v -> {
            if (selectedUris.contains(item.uri)) {
                selectedUris.remove(item.uri);
            } else {
                selectedUris.add(item.uri);
            }
            notifyItemChanged(holder.getAdapterPosition());
            notifySelectionChanged();
        });

        // Quick Fullscreen Preview Icon Click
        holder.btnQuickPreview.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPreviewRequested(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return displayedItems.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardMediaItem;
        ImageView ivGridThumbnail;
        View vSelectionScrim;
        CheckBox cbMediaSelected;
        TextView tvGridHqBadge;
        View layoutVideoDuration;
        TextView tvGridDuration;
        TextView tvGridSize;
        ImageView btnQuickPreview;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardMediaItem = itemView.findViewById(R.id.cardMediaItem);
            ivGridThumbnail = itemView.findViewById(R.id.ivGridThumbnail);
            vSelectionScrim = itemView.findViewById(R.id.vSelectionScrim);
            cbMediaSelected = itemView.findViewById(R.id.cbMediaSelected);
            tvGridHqBadge = itemView.findViewById(R.id.tvGridHqBadge);
            layoutVideoDuration = itemView.findViewById(R.id.layoutVideoDuration);
            tvGridDuration = itemView.findViewById(R.id.tvGridDuration);
            tvGridSize = itemView.findViewById(R.id.tvGridSize);
            btnQuickPreview = itemView.findViewById(R.id.btnQuickPreview);
        }
    }
}
