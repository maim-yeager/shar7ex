package com.example.ui.transfer;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.example.R;
import com.example.model.TransferItem;
import com.example.util.FileUtils;
import com.example.util.ShareUtils;

public class TransferDetailsDialog extends Dialog {

    private final TransferItem item;

    public TransferDetailsDialog(@NonNull Context context, TransferItem item) {
        super(context);
        this.item = item;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_transfer_details);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvFileName = findViewById(R.id.tvDetailsFileName);
        TextView tvSize = findViewById(R.id.tvDetailsSize);
        TextView tvRes = findViewById(R.id.tvDetailsResolution);
        TextView tvQuality = findViewById(R.id.tvDetailsQualityState);
        TextView tvChecksum = findViewById(R.id.tvDetailsChecksum);
        TextView tvExif = findViewById(R.id.tvDetailsExif);
        TextView tvShareUrl = findViewById(R.id.tvShareLinkUrl);
        ImageView btnCopy = findViewById(R.id.btnCopyShareLink);
        ImageView btnShare = findViewById(R.id.btnShareDetailsLink);
        ImageView btnClose = findViewById(R.id.ivCloseDetailsDialog);

        if (item != null) {
            tvFileName.setText("File: " + item.getFileName());
            tvSize.setText("Exact Size: " + String.format("%,d", item.getOriginalSizeBytes()) + " Bytes (" + FileUtils.formatFileSize(item.getOriginalSizeBytes()) + ")");

            String resText = "Resolution: " + (item.getOriginalWidth() > 0 ? (item.getOriginalWidth() + " x " + item.getOriginalHeight()) : "Native Media Stream");
            if (item.isVideo() && item.getVideoFps() > 0) {
                resText += " (" + (int) item.getVideoFps() + " FPS)";
            }
            tvRes.setText(resText);

            tvQuality.setText("Quality: " + (item.isOriginalQuality() ? "100% Original Untouched Bitstream" : "Compressed (" + item.getCompressionLevel() + ")"));
            tvChecksum.setText("SHA-256 Checksum: " + (item.getSha256Checksum() != null ? item.getSha256Checksum() : "Not yet computed"));
            tvExif.setText("Stream / EXIF Metadata: " + (item.getExifSummary() != null && !item.getExifSummary().isEmpty() ? item.getExifSummary() : "No metadata extracted for this file"));

            boolean hasRealLink = item.getShareLink() != null && !item.getShareLink().isEmpty();
            tvShareUrl.setText(hasRealLink ? item.getShareLink() : "Still uploading - link will appear once the transfer completes");

            btnCopy.setOnClickListener(v -> {
                if (!hasRealLink) {
                    Toast.makeText(getContext(), "This file hasn't finished uploading yet.", Toast.LENGTH_SHORT).show();
                    return;
                }
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("SHAREX Link", item.getShareLink());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), "Direct HQ Share Link copied to clipboard!", Toast.LENGTH_SHORT).show();
            });

            btnShare.setOnClickListener(v -> ShareUtils.shareTransferItem(getContext(), item));
        }

        btnClose.setOnClickListener(v -> dismiss());
    }
}
