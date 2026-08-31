package com.example.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.example.ui.MainActivity;

public class NotificationHelper {

    public static final String CHANNEL_TRANSFERS = "sharex_transfers";
    public static final String CHANNEL_MESSAGES = "sharex_messages";
    public static final String CHANNEL_SYSTEM = "sharex_system";

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return;

            NotificationChannel transfersChannel = new NotificationChannel(
                    CHANNEL_TRANSFERS,
                    "High-Quality File Transfers",
                    NotificationManager.IMPORTANCE_LOW
            );
            transfersChannel.setDescription("Live progress of photo & video transfers in original quality");

            NotificationChannel messagesChannel = new NotificationChannel(
                    CHANNEL_MESSAGES,
                    "Direct Messages & Media",
                    NotificationManager.IMPORTANCE_HIGH
            );
            messagesChannel.setDescription("Notifications for incoming original files and messages");

            NotificationChannel systemChannel = new NotificationChannel(
                    CHANNEL_SYSTEM,
                    "System & Announcements",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            manager.createNotificationChannel(transfersChannel);
            manager.createNotificationChannel(messagesChannel);
            manager.createNotificationChannel(systemChannel);
        }
    }

    public static void showTransferProgress(Context context, int notificationId, String title, String subtitle, int progress) {
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_TRANSFERS)
                    .setSmallIcon(android.R.drawable.stat_sys_upload)
                    .setContentTitle(title)
                    .setContentText(subtitle)
                    .setProgress(100, progress, progress == 0)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW);

            NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        } catch (SecurityException ignored) {}
    }

    public static void showTransferCompleted(Context context, int notificationId, String title, String message) {
        try {
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_TRANSFERS)
                    .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setAutoCancel(true)
                    .setContentIntent(pi)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        } catch (SecurityException ignored) {}
    }

    public static void cancelNotification(Context context, int notificationId) {
        try {
            NotificationManagerCompat.from(context).cancel(notificationId);
        } catch (Exception ignored) {}
    }

    public static void showSystemAnnouncement(Context context, String message) {
        try {
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_SYSTEM)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("M.SHAREX Announcement")
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setAutoCancel(true)
                    .setContentIntent(pi)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            NotificationManagerCompat.from(context).notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException ignored) {}
    }
}
