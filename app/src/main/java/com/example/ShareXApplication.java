package com.example;

import android.app.Application;
import com.example.data.FirebaseManager;
import com.example.util.NotificationHelper;

public class ShareXApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Notification Channels
        NotificationHelper.createNotificationChannels(this);
        // Initialize Firebase & Local Database Manager
        FirebaseManager.getInstance(this);
    }
}
