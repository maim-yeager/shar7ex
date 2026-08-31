package com.example.nearby;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import com.example.model.User;
import com.example.util.FileUtils;
import com.example.util.ShareUtils;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * REAL device-to-device sharing using Google's Nearby Connections API - Bluetooth/Wi-Fi
 * Direct discovery + actual file payload transfer. Nothing here is simulated: endpoints
 * found are real advertising devices, and files sent are really written to the peer's
 * storage over the established connection.
 */
public class NearbyShareManager {

    private static final String TAG = "NearbyShareManager";
    private static final String SERVICE_ID = "com.example.msharex.SERVICE_ID";
    private static final Strategy STRATEGY = Strategy.P2P_CLUSTER;

    private final Context context;
    private final ConnectionsClient connectionsClient;
    private final Map<String, String> connectedEndpoints = new HashMap<>(); // endpointId -> endpointName
    private final Map<Long, JSONObject> pendingFileMeta = new HashMap<>();  // filePayloadId -> metadata
    private final Map<Long, Payload> incomingFilePayloads = new HashMap<>();

    private NearbyDiscoveryListener discoveryListener;
    private NearbyTransferListener transferListener;
    private String localUserName = "SHAREX User";

    public interface NearbyDiscoveryListener {
        void onDeviceFound(User device);
        void onDeviceLost(String endpointId);
    }

    public interface NearbyTransferListener {
        void onConnected(String endpointId, String endpointName);
        void onFileReceived(String senderName, File file, String fileName, String mimeType);
        void onTransferProgress(String endpointId, int progress);
        void onError(String message);
    }

    public NearbyShareManager(Context context) {
        this.context = context.getApplicationContext();
        this.connectionsClient = Nearby.getConnectionsClient(this.context);
    }

    public void setLocalUserName(String name) {
        if (name != null && !name.isEmpty()) this.localUserName = name;
    }

    public void setTransferListener(NearbyTransferListener listener) {
        this.transferListener = listener;
    }

    /** Makes this device discoverable to nearby SHAREX peers. */
    public void startAdvertising() {
        AdvertisingOptions options = new AdvertisingOptions.Builder().setStrategy(STRATEGY).build();
        connectionsClient.startAdvertising(localUserName, SERVICE_ID, connectionLifecycleCallback, options)
                .addOnFailureListener(e -> Log.w(TAG, "Advertising failed: " + e.getMessage()));
    }

    /** Actively looks for other SHAREX devices that are advertising nearby. */
    public void startDiscovery(NearbyDiscoveryListener listener) {
        this.discoveryListener = listener;
        DiscoveryOptions options = new DiscoveryOptions.Builder().setStrategy(STRATEGY).build();
        connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
                .addOnFailureListener(e -> Log.w(TAG, "Discovery failed: " + e.getMessage()));
    }

    public void stopDiscovery() {
        connectionsClient.stopDiscovery();
    }

    public void stopAdvertising() {
        connectionsClient.stopAdvertising();
    }

    public void stopAll() {
        connectionsClient.stopAllEndpoints();
    }

    public boolean isConnectedTo(String endpointId) {
        return connectedEndpoints.containsKey(endpointId);
    }

    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
            User device = new User();
            device.setUid(endpointId);
            device.setDisplayName(info.getEndpointName());
            device.setUsername(info.getEndpointName());
            device.setOnline(true);
            if (discoveryListener != null) discoveryListener.onDeviceFound(device);
        }

        @Override
        public void onEndpointLost(String endpointId) {
            if (discoveryListener != null) discoveryListener.onDeviceLost(endpointId);
        }
    };

    /** Initiates a real connection request to a discovered peer (call before sending). */
    public void connectTo(String endpointId) {
        connectionsClient.requestConnection(localUserName, endpointId, connectionLifecycleCallback)
                .addOnFailureListener(e -> {
                    if (transferListener != null) transferListener.onError("Connection failed: " + e.getMessage());
                });
    }

    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(String endpointId, ConnectionInfo info) {
            // Auto-accept: this is a lightweight consumer share flow (like the original
            // "tap a device to send" UX) rather than a manual PIN-confirmation dialog.
            connectionsClient.acceptConnection(endpointId, payloadCallback);
        }

        @Override
        public void onConnectionResult(String endpointId, ConnectionResolution resolution) {
            if (resolution.getStatus().isSuccess()) {
                connectedEndpoints.put(endpointId, endpointId);
                if (transferListener != null) transferListener.onConnected(endpointId, endpointId);
            } else if (transferListener != null) {
                transferListener.onError("Could not connect to peer (status " + resolution.getStatus().getStatusCode() + ")");
            }
        }

        @Override
        public void onDisconnected(String endpointId) {
            connectedEndpoints.remove(endpointId);
        }
    };

    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String endpointId, Payload payload) {
            if (payload.getType() == Payload.Type.BYTES) {
                try {
                    byte[] bytes = payload.asBytes();
                    if (bytes == null) return;
                    JSONObject meta = new JSONObject(new String(bytes));
                    long referencedFilePayloadId = meta.getLong("filePayloadId");
                    pendingFileMeta.put(referencedFilePayloadId, meta);
                    // The file payload may have already fully arrived before its metadata did.
                    tryFinalizeFile(referencedFilePayloadId, endpointId);
                } catch (JSONException e) {
                    Log.w(TAG, "Bad metadata payload: " + e.getMessage());
                }
            } else if (payload.getType() == Payload.Type.FILE) {
                incomingFilePayloads.put(payload.getId(), payload);
            }
        }

        @Override
        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
            if (update.getStatus() == PayloadTransferUpdate.Status.IN_PROGRESS) {
                long percent = update.getTotalBytes() > 0 ? (update.getBytesTransferred() * 100) / update.getTotalBytes() : 0;
                if (transferListener != null) transferListener.onTransferProgress(endpointId, (int) percent);
            } else if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                tryFinalizeFile(update.getPayloadId(), endpointId);
            } else if (update.getStatus() == PayloadTransferUpdate.Status.FAILURE) {
                if (transferListener != null) transferListener.onError("Transfer failed for payload " + update.getPayloadId());
            }
        }
    };

    private void tryFinalizeFile(long filePayloadId, String endpointId) {
        Payload filePayload = incomingFilePayloads.get(filePayloadId);
        JSONObject meta = pendingFileMeta.get(filePayloadId);
        if (filePayload == null || meta == null) return; // wait until both parts have arrived

        File receivedFile = filePayload.asFile() != null ? filePayload.asFile().asJavaFile() : null;
        if (receivedFile == null) return;

        try {
            String fileName = meta.optString("fileName", "sharex_" + System.currentTimeMillis());
            String mimeType = meta.optString("mimeType", "application/octet-stream");
            String senderName = meta.optString("senderName", endpointId);

            if (transferListener != null) transferListener.onFileReceived(senderName, receivedFile, fileName, mimeType);
        } finally {
            incomingFilePayloads.remove(filePayloadId);
            pendingFileMeta.remove(filePayloadId);
        }
    }

    /** Really transmits the given file's bytes to a connected peer over the live connection. */
    public void sendFile(String endpointId, Uri uri, String fileName, String mimeType) {
        try {
            ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
            if (pfd == null) {
                if (transferListener != null) transferListener.onError("Could not open file to send.");
                return;
            }
            Payload filePayload = Payload.fromFile(pfd);

            JSONObject meta = new JSONObject();
            meta.put("filePayloadId", filePayload.getId());
            meta.put("fileName", fileName != null ? fileName : "shared_file");
            meta.put("mimeType", mimeType != null ? mimeType : "application/octet-stream");
            meta.put("senderName", localUserName);

            connectionsClient.sendPayload(endpointId, Payload.fromBytes(meta.toString().getBytes()));
            connectionsClient.sendPayload(endpointId, filePayload);
        } catch (Exception e) {
            if (transferListener != null) transferListener.onError("Send failed: " + e.getMessage());
        }
    }
}
