package com.example.service;

import androidx.annotation.NonNull;
import com.example.util.NotificationHelper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class ShareXFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // Handle FCM token refresh
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        String title = message.getNotification() != null ? message.getNotification().getTitle() : "M.SHAREX Update";
        String body = message.getNotification() != null ? message.getNotification().getBody() : "You received a new high-quality transfer";

        NotificationHelper.showTransferCompleted(this, (int) System.currentTimeMillis(), title, body);
    }
}
