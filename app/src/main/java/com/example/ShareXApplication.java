package com.example;

import android.app.Application;
import com.example.util.NotificationHelper;

public class ShareXApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Notification Channels
        NotificationHelper.createNotificationChannels(this);
        // NOTE: Firebase is intentionally NOT touched here. Firebase auto-initializes itself
        // from app/google-services.json at process start; if that file is missing/placeholder,
        // FirebaseApp.getInstance() throws immediately, which would crash the app before any
        // screen even shows. FirebaseManager checks this safely on first real use instead
        // (see SplashActivity), so a missing config shows a message instead of a crash.
    }
}
