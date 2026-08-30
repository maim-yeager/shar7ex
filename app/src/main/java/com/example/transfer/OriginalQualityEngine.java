package com.example.transfer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import com.example.model.TransferItem;
import com.example.util.FileUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Ensures zero-compression for "Original Quality" mode and provides
 * explicit user-requested compression only when "Compressed Mode" is selected.
 */
public class OriginalQualityEngine {

    public enum TransferMode {
        ORIGINAL_QUALITY,
        COMPRESSED_LOW,
        COMPRESSED_MEDIUM,
        COMPRESSED_HIGH
    }

    public static InputStream prepareMediaStream(Context context, Uri uri, TransferMode mode, TransferItem item) throws Exception {
        if (mode == TransferMode.ORIGINAL_QUALITY) {
            // Strict 100% bit-exact stream preservation! Zero alteration of pixel or audio streams!
            item.setOriginalQuality(true);
            item.setCompressionLevel("NONE");
            return context.getContentResolver().openInputStream(uri);
        }

        // Only compress if explicitly requested
        item.setOriginalQuality(false);
        if (item.isImage()) {
            int quality = 80;
            if (mode == TransferMode.COMPRESSED_LOW) quality = 45;
            else if (mode == TransferMode.COMPRESSED_MEDIUM) quality = 70;
            else if (mode == TransferMode.COMPRESSED_HIGH) quality = 85;
            item.setCompressionLevel(mode.name());

            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                if (bitmap != null) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
                    byte[] compressedBytes = out.toByteArray();
                    return new ByteArrayInputStream(compressedBytes);
                }
            }
        }

        // Default fall-through: original stream
        return context.getContentResolver().openInputStream(uri);
    }
}
