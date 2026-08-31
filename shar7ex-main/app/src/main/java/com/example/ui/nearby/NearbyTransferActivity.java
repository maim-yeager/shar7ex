package com.example.ui.nearby;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.R;
import com.example.data.FirebaseManager;
import com.example.model.User;
import com.example.nearby.NearbyShareManager;
import com.example.ui.home.OnlineUsersAdapter;
import com.example.util.FileUtils;
import com.example.util.ShareUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class NearbyTransferActivity extends AppCompatActivity {

    private ImageView ivNearbyBack;
    private ProgressBar pbRadar;
    private TextView tvNearbyStatus;
    private RecyclerView rvNearbyDevices;
    private AppCompatButton btnRefreshNearby;
    private OnlineUsersAdapter adapter;
    private NearbyShareManager nearbyManager;
    private final List<User> discoveredDevices = new ArrayList<>();
    private String pendingConnectEndpointId;

    private final ActivityResultLauncher<String[]> permissionsLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) allGranted &= Boolean.TRUE.equals(granted);
                if (allGranted) {
                    startRealNearbySession();
                } else {
                    tvNearbyStatus.setText("Nearby sharing needs Bluetooth/Wi-Fi & nearby-device permissions to work.");
                }
            }
    );

    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::sendPickedFileToPendingPeer
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_transfer);

        ivNearbyBack = findViewById(R.id.ivNearbyBack);
        pbRadar = findViewById(R.id.pbRadar);
        tvNearbyStatus = findViewById(R.id.tvNearbyStatus);
        rvNearbyDevices = findViewById(R.id.rvNearbyDevices);
        btnRefreshNearby = findViewById(R.id.btnRefreshNearby);

        ivNearbyBack.setOnClickListener(v -> finish());

        rvNearbyDevices.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapter = new OnlineUsersAdapter(device -> {
            pendingConnectEndpointId = device.getUid(); // endpointId, real Nearby Connections id
            tvNearbyStatus.setText("Connecting to " + device.getDisplayName() + "...");
            nearbyManager.connectTo(pendingConnectEndpointId);
        });
        rvNearbyDevices.setAdapter(adapter);

        nearbyManager = new NearbyShareManager(this);
        User me = FirebaseManager.getInstance(this).getCurrentUser();
        nearbyManager.setLocalUserName(me != null ? me.getDisplayName() : "SHAREX User");
        nearbyManager.setTransferListener(new NearbyShareManager.NearbyTransferListener() {
            @Override
            public void onConnected(String endpointId, String endpointName) {
                runOnUiThread(() -> {
                    tvNearbyStatus.setText("Connected! Choose a file to send at 100% original quality.");
                    filePickerLauncher.launch("*/*");
                });
            }

            @Override
            public void onFileReceived(String senderName, File file, String fileName, String mimeType) {
                runOnUiThread(() -> {
                    Toast.makeText(NearbyTransferActivity.this, "Receiving " + fileName + " from " + senderName + "...", Toast.LENGTH_SHORT).show();
                    ShareUtils.saveLocalFileToPublicStorage(NearbyTransferActivity.this, file, fileName, mimeType, (success, message) ->
                            Toast.makeText(NearbyTransferActivity.this, message, success ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show());
                });
            }

            @Override
            public void onTransferProgress(String endpointId, int progress) {
                runOnUiThread(() -> tvNearbyStatus.setText("Sending... " + progress + "%"));
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(NearbyTransferActivity.this, message, Toast.LENGTH_LONG).show());
            }
        });

        btnRefreshNearby.setOnClickListener(v -> ensurePermissionsThenStart());

        ensurePermissionsThenStart();
    }

    private void sendPickedFileToPendingPeer(Uri uri) {
        if (uri == null || pendingConnectEndpointId == null) return;
        String fileName = FileUtils.getFileName(this, uri);
        String mime = getContentResolver().getType(uri);
        nearbyManager.sendFile(pendingConnectEndpointId, uri, fileName, mime);
        Toast.makeText(this, "Sending " + fileName + " over Nearby (100% Original)...", Toast.LENGTH_SHORT).show();
    }

    private void ensurePermissionsThenStart() {
        List<String> needed = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            addIfMissing(needed, Manifest.permission.BLUETOOTH_ADVERTISE);
            addIfMissing(needed, Manifest.permission.BLUETOOTH_CONNECT);
            addIfMissing(needed, Manifest.permission.BLUETOOTH_SCAN);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            addIfMissing(needed, Manifest.permission.NEARBY_WIFI_DEVICES);
        }
        addIfMissing(needed, Manifest.permission.ACCESS_FINE_LOCATION);

        if (needed.isEmpty()) {
            startRealNearbySession();
        } else {
            permissionsLauncher.launch(needed.toArray(new String[0]));
        }
    }

    private void addIfMissing(List<String> list, String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            list.add(permission);
        }
    }

    private void startRealNearbySession() {
        discoveredDevices.clear();
        adapter.setUsers(discoveredDevices);
        pbRadar.setVisibility(View.VISIBLE);
        tvNearbyStatus.setText("Advertising this device & scanning for nearby SHAREX peers…");

        nearbyManager.startAdvertising();
        nearbyManager.startDiscovery(new NearbyShareManager.NearbyDiscoveryListener() {
            @Override
            public void onDeviceFound(User device) {
                runOnUiThread(() -> {
                    discoveredDevices.add(device);
                    adapter.setUsers(discoveredDevices);
                    tvNearbyStatus.setText("Found " + discoveredDevices.size() + " peer device(s) nearby. Tap one to connect.");
                });
            }

            @Override
            public void onDeviceLost(String endpointId) {
                runOnUiThread(() -> {
                    discoveredDevices.removeIf(u -> endpointId.equals(u.getUid()));
                    adapter.setUsers(discoveredDevices);
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (nearbyManager != null) {
            nearbyManager.stopDiscovery();
            nearbyManager.stopAdvertising();
        }
    }
}
