package com.example.ui.settings;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.SwitchCompat;
import com.bumptech.glide.Glide;
import com.example.R;
import com.example.data.FirebaseManager;
import com.example.util.AppPrefs;
import com.example.util.FileUtils;
import java.io.File;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {

    private ImageView ivSettingsBack;
    private SwitchCompat switchOriginalDefault, switchWifiAuto;
    private AppCompatButton btnClearCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ivSettingsBack = findViewById(R.id.ivSettingsBack);
        switchOriginalDefault = findViewById(R.id.switchOriginalDefault);
        switchWifiAuto = findViewById(R.id.switchWifiAuto);
        btnClearCache = findViewById(R.id.btnClearCache);

        ivSettingsBack.setOnClickListener(v -> finish());

        // Real initial state - reflects what was actually saved last time, not a fixed default.
        switchOriginalDefault.setChecked(AppPrefs.isOriginalQualityDefault(this));
        switchWifiAuto.setChecked(AppPrefs.isWifiAutoDownloadEnabled(this));

        switchOriginalDefault.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppPrefs.setOriginalQualityDefault(this, isChecked);
            Toast.makeText(this, isChecked ? "Original Quality will be the default for new transfers" : "Compressed Mode will be the default for new transfers", Toast.LENGTH_SHORT).show();
        });

        switchWifiAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppPrefs.setWifiAutoDownloadEnabled(this, isChecked);
            Toast.makeText(this, isChecked ? "Media will auto-load only on Wi-Fi" : "Media will always ask before loading on mobile data", Toast.LENGTH_SHORT).show();
        });

        btnClearCache.setOnClickListener(v -> clearRealCache());
    }

    private void clearRealCache() {
        btnClearCache.setEnabled(false);
        Executors.newSingleThreadExecutor().execute(() -> {
            long before = folderSize(getCacheDir()) + folderSize(getExternalCacheDir());
            try {
                Glide.get(getApplicationContext()).clearDiskCache();
            } catch (Exception ignored) {}
            deleteContents(getCacheDir());
            deleteContents(getExternalCacheDir());
            long freed = before; // cache dirs are now empty

            runOnUiThread(() -> {
                Glide.get(getApplicationContext()).clearMemory();
                btnClearCache.setEnabled(true);
                Toast.makeText(this, "Cleared " + FileUtils.formatFileSize(freed) + " of cached media", Toast.LENGTH_LONG).show();
            });
        });
    }

    private long folderSize(File dir) {
        if (dir == null || !dir.exists()) return 0;
        long size = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;
        for (File f : files) {
            size += f.isDirectory() ? folderSize(f) : f.length();
        }
        return size;
    }

    private void deleteContents(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) deleteContents(f);
            f.delete();
        }
    }
}
