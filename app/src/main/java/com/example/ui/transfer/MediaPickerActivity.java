package com.example.ui.transfer;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.R;
import com.example.model.TransferItem;
import com.example.transfer.OriginalQualityEngine;
import com.example.transfer.TransferManager;
import com.example.ui.viewer.MediaViewerActivity;
import com.example.util.FileUtils;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MediaPickerActivity extends AppCompatActivity {

    public static final String EXTRA_RECEIVER_UID = "receiver_uid";
    public static final String EXTRA_RECEIVER_NAME = "receiver_name";

    private RecyclerView rvMediaGrid;
    private MediaGridAdapter adapter;
    private TextView tvBatchTotalSize, tvQualityWarning, btnSelectAllToggle, tvGallerySubtitle;
    private RadioGroup rgQualityMode;
    private RadioButton rbOriginalQuality, rbCompressedMode;
    private AppCompatButton btnSendSelectedMedia;
    private ImageView ivPickerBack;
    private ChipGroup chipGroupCategories;
    private View layoutEmptyGallery;

    private OriginalQualityEngine.TransferMode selectedMode = OriginalQualityEngine.TransferMode.ORIGINAL_QUALITY;
    private String receiverUid;
    private String receiverName;

    private final ActivityResultLauncher<String[]> permissionsLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> loadLocalMedia()
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_picker);

        receiverUid = getIntent().getStringExtra(EXTRA_RECEIVER_UID);
        receiverName = getIntent().getStringExtra(EXTRA_RECEIVER_NAME);

        rvMediaGrid = findViewById(R.id.rvMediaGrid);
        tvBatchTotalSize = findViewById(R.id.tvBatchTotalSize);
        tvQualityWarning = findViewById(R.id.tvQualityWarning);
        btnSelectAllToggle = findViewById(R.id.btnSelectAllToggle);
        tvGallerySubtitle = findViewById(R.id.tvGallerySubtitle);
        rgQualityMode = findViewById(R.id.rgQualityMode);
        rbOriginalQuality = findViewById(R.id.rbOriginalQuality);
        rbCompressedMode = findViewById(R.id.rbCompressedMode);
        btnSendSelectedMedia = findViewById(R.id.btnSendSelectedMedia);
        ivPickerBack = findViewById(R.id.ivPickerBack);
        chipGroupCategories = findViewById(R.id.chipGroupCategories);
        layoutEmptyGallery = findViewById(R.id.layoutEmptyGallery);

        rvMediaGrid.setLayoutManager(new GridLayoutManager(this, 3));
        rvMediaGrid.setHasFixedSize(true);

        adapter = new MediaGridAdapter(new MediaGridAdapter.OnGalleryInteractionListener() {
            @Override
            public void onSelectionChanged(int selectedCount, long totalSizeBytes) {
                updateSelectionState(selectedCount, totalSizeBytes);
            }

            @Override
            public void onPreviewRequested(MediaGridAdapter.MediaItem item) {
                TransferItem previewItem = new TransferItem();
                previewItem.setFileName(item.name);
                previewItem.setLocalUri(item.uri.toString());
                previewItem.setOriginalSizeBytes(item.size);
                previewItem.setMimeType(item.isVideo ? "video/mp4" : "image/jpeg");
                previewItem.setOriginalQuality(selectedMode == OriginalQualityEngine.TransferMode.ORIGINAL_QUALITY);
                previewItem.setCompressionLevel(selectedMode == OriginalQualityEngine.TransferMode.ORIGINAL_QUALITY ? "NONE" : "MEDIUM");

                Intent intent = new Intent(MediaPickerActivity.this, MediaViewerActivity.class);
                intent.putExtra("transfer_item", previewItem);
                startActivity(intent);
            }
        });
        rvMediaGrid.setAdapter(adapter);

        ivPickerBack.setOnClickListener(v -> finish());

        btnSelectAllToggle.setOnClickListener(v -> {
            adapter.toggleSelectAll();
            btnSelectAllToggle.setText(adapter.isAllSelected() ? "Deselect All" : "Select All");
        });

        // Filter Category Chips
        chipGroupCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipPhotos) {
                adapter.setFilterMode(1);
            } else if (checkedId == R.id.chipVideos) {
                adapter.setFilterMode(2);
            } else if (checkedId == R.id.chipLargeFiles) {
                adapter.setFilterMode(3);
            } else {
                adapter.setFilterMode(0); // All
            }
            layoutEmptyGallery.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        });

        rgQualityMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbOriginalQuality) {
                selectedMode = OriginalQualityEngine.TransferMode.ORIGINAL_QUALITY;
                tvQualityWarning.setText("✨ High-Quality Mode preserves 100% original pixel data, color profiles, and EXIF metadata.");
                tvQualityWarning.setTextColor(0xFF00E676);
            } else {
                selectedMode = OriginalQualityEngine.TransferMode.COMPRESSED_MEDIUM;
                tvQualityWarning.setText("⚡ Compressed Mode reduces file size for faster transfer on low bandwidth.");
                tvQualityWarning.setTextColor(0xFFFF9100);
            }
            updateSelectionState(adapter.getSelectedItems().size(), getSelectedTotalBytes());
        });

        btnSendSelectedMedia.setOnClickListener(v -> sendSelectedMedia());

        if (receiverUid != null) {
            tvGallerySubtitle.setText("Sending directly to " + (receiverName != null ? receiverName : "peer"));
        }

        boolean originalDefault = com.example.util.AppPrefs.isOriginalQualityDefault(this);
        selectedMode = originalDefault ? OriginalQualityEngine.TransferMode.ORIGINAL_QUALITY : OriginalQualityEngine.TransferMode.COMPRESSED_MEDIUM;
        rgQualityMode.check(originalDefault ? R.id.rbOriginalQuality : R.id.rbCompressedMode);

        checkPermissionsAndLoadMedia();
    }

    private void updateSelectionState(int selectedCount, long totalSizeBytes) {
        tvBatchTotalSize.setText(selectedCount + " selected • " + FileUtils.formatFileSize(totalSizeBytes));
        btnSelectAllToggle.setText(adapter.isAllSelected() ? "Deselect All" : "Select All");

        if (selectedCount > 0) {
            btnSendSelectedMedia.setEnabled(true);
            String modeStr = (selectedMode == OriginalQualityEngine.TransferMode.ORIGINAL_QUALITY) ? "100% Original" : "Compressed";
            String verb = receiverUid != null ? "Send" : "Upload & Get Link for";
            btnSendSelectedMedia.setText(verb + " " + selectedCount + " Media (" + FileUtils.formatFileSize(totalSizeBytes) + " • " + modeStr + ")");
        } else {
            btnSendSelectedMedia.setEnabled(false);
            btnSendSelectedMedia.setText("Select Media to Share (0 Selected)");
        }
    }

    private long getSelectedTotalBytes() {
        long total = 0;
        for (MediaGridAdapter.MediaItem item : adapter.getSelectedItems()) {
            total += item.size;
        }
        return total;
    }

    private void checkPermissionsAndLoadMedia() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsLauncher.launch(new String[]{
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.READ_MEDIA_VIDEO
            });
        } else {
            permissionsLauncher.launch(new String[]{
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
            });
        }
    }

    private void loadLocalMedia() {
        List<MediaGridAdapter.MediaItem> items = new ArrayList<>();
        try {
            // Query Device Images via MediaStore
            Uri imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            String[] imgProjection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.SIZE};
            try (Cursor cursor = getContentResolver().query(imageUri, imgProjection, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC LIMIT 60")) {
                if (cursor != null) {
                    int idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                    int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                    int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);

                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idCol);
                        MediaGridAdapter.MediaItem item = new MediaGridAdapter.MediaItem();
                        item.uri = ContentUris.withAppendedId(imageUri, id);
                        item.name = cursor.getString(nameCol);
                        item.size = cursor.getLong(sizeCol);
                        item.isVideo = false;
                        item.isHq = item.size > 15L * 1024L * 1024L;
                        items.add(item);
                    }
                }
            }

            // Query Device Videos via MediaStore
            Uri videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            String[] vidProjection = {MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.SIZE, MediaStore.Video.Media.DURATION};
            try (Cursor cursor = getContentResolver().query(videoUri, vidProjection, null, null, MediaStore.Video.Media.DATE_ADDED + " DESC LIMIT 40")) {
                if (cursor != null) {
                    int idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                    int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
                    int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
                    int durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);

                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idCol);
                        MediaGridAdapter.MediaItem item = new MediaGridAdapter.MediaItem();
                        item.uri = ContentUris.withAppendedId(videoUri, id);
                        item.name = cursor.getString(nameCol);
                        item.size = cursor.getLong(sizeCol);
                        item.durationMs = cursor.getLong(durCol);
                        item.isVideo = true;
                        item.isHq = true;
                        items.add(item);
                    }
                }
            }
        } catch (Exception ignored) {}

        // No fake sample gallery fallback - an empty device gallery just shows the real empty state.
        adapter.setMediaItems(items);
        tvGallerySubtitle.setText(items.isEmpty() ? "No media found on this device" : items.size() + " uncompressed media files found on device");
        layoutEmptyGallery.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void sendSelectedMedia() {
        List<MediaGridAdapter.MediaItem> selected = adapter.getSelectedItems();
        if (selected.isEmpty()) {
            Toast.makeText(this, "Please select at least 1 media item", Toast.LENGTH_SHORT).show();
            return;
        }

        if (receiverUid != null) {
            // Direct send to a real, known recipient (opened from a chat/nearby peer).
            for (MediaGridAdapter.MediaItem item : selected) {
                TransferManager.getInstance(this).startUpload(item.uri, selectedMode, receiverUid, receiverName, null);
            }
            Toast.makeText(this, "Started " + selected.size() + " transfer(s) to " + receiverName + " in " +
                    (selectedMode == OriginalQualityEngine.TransferMode.ORIGINAL_QUALITY ? "100% Original" : "Compressed") + " mode", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // No recipient chosen - upload each file for real and generate a real shareable link,
        // then hand the finished links to the system Share Sheet (WeTransfer-style flow).
        btnSendSelectedMedia.setEnabled(false);
        Toast.makeText(this, "Uploading " + selected.size() + " file(s)...", Toast.LENGTH_SHORT).show();

        List<String> links = new ArrayList<>();
        AtomicInteger remaining = new AtomicInteger(selected.size());

        for (MediaGridAdapter.MediaItem item : selected) {
            TransferManager.getInstance(this).startUpload(item.uri, selectedMode, null, null, new TransferManager.TransferProgressListener() {
                @Override public void onProgress(TransferItem item, int progress) {}

                @Override
                public void onCompleted(TransferItem uploaded) {
                    links.add(uploaded.getFileName() + ": " + uploaded.getDownloadUrl());
                    if (remaining.decrementAndGet() == 0) finishWithShareSheet(links);
                }

                @Override
                public void onError(TransferItem item, String errorMessage) {
                    if (remaining.decrementAndGet() == 0) finishWithShareSheet(links);
                }
            });
        }
    }

    private void finishWithShareSheet(List<String> links) {
        if (links.isEmpty()) {
            Toast.makeText(this, "Upload failed. Please check your connection and try again.", Toast.LENGTH_LONG).show();
            btnSendSelectedMedia.setEnabled(true);
            return;
        }
        StringBuilder body = new StringBuilder("Shared via M.SHAREX (100% original quality):\n");
        for (String link : links) body.append(link).append("\n");

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, body.toString());
        startActivity(Intent.createChooser(shareIntent, "Share links"));
        finish();
    }
}
