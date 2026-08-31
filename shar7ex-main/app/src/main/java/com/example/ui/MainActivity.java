package com.example.ui;

import android.animation.ValueAnimator;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.R;
import com.example.data.FirebaseManager;
import com.example.ui.chat.ChatsFragment;
import com.example.ui.home.HomeFragment;
import com.example.ui.profile.ProfileFragment;
import com.example.ui.transfer.TransfersFragment;
import com.example.util.NotificationHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "sharex_prefs";
    private static final String KEY_LAST_ANNOUNCEMENT_TS = "last_announcement_ts";

    private BottomNavigationView bottomNav;
    private ListenerRegistration announcementListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNavigation);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            animateNavSelection(id);
            if (id == R.id.nav_home) {
                loadFragment(new HomeFragment());
                return true;
            } else if (id == R.id.nav_chats) {
                loadFragment(new ChatsFragment());
                return true;
            } else if (id == R.id.nav_transfers) {
                loadFragment(new TransfersFragment());
                return true;
            } else if (id == R.id.nav_profile) {
                loadFragment(new ProfileFragment());
                return true;
            }
            return false;
        });

        listenForRealAnnouncements();
    }

    /** Apple-style springy bounce on the tapped nav item - real motion, not a static icon swap. */
    private void animateNavSelection(int itemId) {
        View itemView = bottomNav.findViewById(itemId);
        if (itemView == null) return;
        itemView.animate().cancel();
        ValueAnimator animator = ValueAnimator.ofFloat(0.82f, 1f);
        animator.setDuration(260);
        animator.setInterpolator(new OvershootInterpolator(3.2f));
        animator.addUpdateListener(a -> {
            float scale = (float) a.getAnimatedValue();
            itemView.setScaleX(scale);
            itemView.setScaleY(scale);
        });
        animator.start();
    }

    /** Real-time: any admin broadcast written to Firestore reaches every signed-in device instantly. */
    private void listenForRealAnnouncements() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        long lastSeen = prefs.getLong(KEY_LAST_ANNOUNCEMENT_TS, System.currentTimeMillis());

        announcementListener = FirebaseManager.getInstance(this).getFirestore()
                .collection("announcements")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((snap, error) -> {
                    if (error != null || snap == null || snap.isEmpty()) return;
                    com.google.firebase.firestore.DocumentSnapshot doc = snap.getDocuments().get(0);
                    Long ts = doc.getLong("timestamp");
                    String message = doc.getString("message");
                    if (ts == null || message == null || ts <= lastSeen) return;

                    prefs.edit().putLong(KEY_LAST_ANNOUNCEMENT_TS, ts).apply();
                    NotificationHelper.showSystemAnnouncement(this, message);
                });
    }

    public void selectTab(int navItemId) {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(navItemId);
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fragment_fade_in, R.anim.fragment_fade_out)
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (announcementListener != null) announcementListener.remove();
    }
}
