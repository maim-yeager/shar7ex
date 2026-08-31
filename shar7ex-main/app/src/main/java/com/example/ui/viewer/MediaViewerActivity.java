package com.example.ui.viewer;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import com.bumptech.glide.Glide;
import com.example.R;
import com.example.model.TransferItem;
import com.example.ui.transfer.TransferDetailsDialog;
import com.example.util.FileUtils;
import com.example.util.ShareUtils;

public class MediaViewerActivity extends AppCompatActivity {

    private TransferItem item;
    private ImageView ivFullPhoto, ivViewerBack, ivViewerInfo;
    private PlayerView playerView;
    private ExoPlayer player;
    private TextView tvViewerFileName, tvViewerMeta;
    private AppCompatButton btnViewerDownload, btnViewerShareLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_viewer);

        item = (TransferItem) getIntent().getSerializableExtra("transfer_item");
        if (item == null) {
            finish();
            return;
        }

        ivFullPhoto = findViewById(R.id.ivFullPhoto);
        ivViewerBack = findViewById(R.id.ivViewerBack);
        ivViewerInfo = findViewById(R.id.ivViewerInfo);
        playerView = findViewById(R.id.playerView);
        tvViewerFileName = findViewById(R.id.tvViewerFileName);
        tvViewerMeta = findViewById(R.id.tvViewerMeta);
        btnViewerDownload = findViewById(R.id.btnViewerDownload);
        btnViewerShareLink = findViewById(R.id.btnViewerShareLink);

        tvViewerFileName.setText(item.getFileName());

        String meta = (item.isOriginalQuality() ? "100% Original • " : "Compressed • ") + FileUtils.formatFileSize(item.getOriginalSizeBytes());
        if (item.getOriginalWidth() > 0) {
            meta += " • " + item.getOriginalWidth() + "x" + item.getOriginalHeight();
        }
        tvViewerMeta.setText(meta);

        ivViewerBack.setOnClickListener(v -> finish());

        ivViewerInfo.setOnClickListener(v -> {
            TransferDetailsDialog dialog = new TransferDetailsDialog(this, item);
            dialog.show();
        });

        // Real system Share Sheet instead of copying a fabricated link.
        btnViewerShareLink.setOnClickListener(v -> ShareUtils.shareTransferItem(this, item));

        // Real save-to-device instead of a fake "saved" toast.
        btnViewerDownload.setOnClickListener(v -> {
            btnViewerDownload.setEnabled(false);
            ShareUtils.downloadTransferItem(this, item, (success, message) -> {
                btnViewerDownload.setEnabled(true);
                Toast.makeText(this, message, success ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
            });
        });

        if (item.isVideo()) {
            setupVideoPlayer();
        } else {
            setupImageViewer();
        }
    }

    private void setupImageViewer() {
        ivFullPhoto.setVisibility(View.VISIBLE);
        playerView.setVisibility(View.GONE);

        if (item.getLocalUri() != null) {
            Glide.with(this).load(Uri.parse(item.getLocalUri())).fitCenter().into(ivFullPhoto);
        } else if (item.getDownloadUrl() != null) {
            if (canAutoLoadRemoteMedia()) {
                Glide.with(this).load(item.getDownloadUrl()).fitCenter().into(ivFullPhoto);
            } else {
                showTapToLoadOnMobileData(this::setupImageViewer);
            }
        } else {
            ivFullPhoto.setImageResource(R.drawable.ic_sharex_logo);
        }
    }

    /** Real check: auto-load only when actually on Wi-Fi AND the setting allows it; mobile data always prompts first. */
    private boolean canAutoLoadRemoteMedia() {
        return com.example.util.NetworkUtils.isOnWifi(this) && com.example.util.AppPrefs.isWifiAutoDownloadEnabled(this);
    }

    private void showTapToLoadOnMobileData(Runnable onConfirmed) {
        ivFullPhoto.setImageResource(R.drawable.ic_sharex_logo);
        Toast.makeText(this, "On mobile data - tap the file name to load full quality, or connect to Wi-Fi.", Toast.LENGTH_LONG).show();
        tvViewerFileName.setOnClickListener(v -> {
            Toast.makeText(this, "Loading over mobile data...", Toast.LENGTH_SHORT).show();
            onConfirmed.run();
        });
    }

    private void setupVideoPlayer() {
        Uri videoUri = null;
        if (item.getLocalUri() != null) {
            videoUri = Uri.parse(item.getLocalUri());
        } else if (item.getDownloadUrl() != null) {
            if (!canAutoLoadRemoteMedia()) {
                ivFullPhoto.setVisibility(View.VISIBLE);
                playerView.setVisibility(View.GONE);
                showTapToLoadOnMobileData(this::setupVideoPlayer);
                return;
            }
            videoUri = Uri.parse(item.getDownloadUrl());
        }

        if (videoUri == null) {
            // No fake sample video fallback - show a real "not available" state instead.
            ivFullPhoto.setVisibility(View.VISIBLE);
            playerView.setVisibility(View.GONE);
            ivFullPhoto.setImageResource(R.drawable.ic_video);
            Toast.makeText(this, "Video preview isn't available yet - it may still be uploading.", Toast.LENGTH_SHORT).show();
            return;
        }

        ivFullPhoto.setVisibility(View.GONE);
        playerView.setVisibility(View.VISIBLE);

        try {
            player = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(player);

            MediaItem mediaItem = MediaItem.fromUri(videoUri);
            player.setMediaItem(mediaItem);
            player.prepare();
            player.setPlayWhenReady(true);
        } catch (Exception e) {
            ivFullPhoto.setVisibility(View.VISIBLE);
            playerView.setVisibility(View.GONE);
            ivFullPhoto.setImageResource(R.drawable.ic_video);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
    }
}
