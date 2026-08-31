package com.example.data;

import android.content.Context;
import androidx.annotation.Nullable;
import com.example.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

/**
 * Central access point for Firebase Auth / Firestore.
 * (Cloud Storage is intentionally not used - see com.example.cloud.CloudinaryUploader.)
 * The signed-in user is always the REAL FirebaseAuth user - there is no
 * local fake/demo account. Profile data is mirrored to Firestore under
 * users/{uid} so it is real and shared across devices.
 */
public class FirebaseManager {

    public static final String COLLECTION_USERS = "users";

    private static FirebaseManager instance;
    private final Context context;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private User currentUserCache;
    private boolean firebaseAvailable = false;

    public interface UserCallback {
        void onResult(@Nullable User user);
    }

    public interface UsersCallback {
        void onResult(List<User> users);
    }

    private FirebaseManager(Context context) {
        this.context = context.getApplicationContext();
        initFirebase();
    }

    public static synchronized FirebaseManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseManager(context);
        }
        return instance;
    }

    private void initFirebase() {
        try {
            // If google-services.json was missing/placeholder at build time, no FirebaseApp
            // exists yet in this process. Guard instead of letting FirebaseAuth.getInstance()
            // throw and crash the whole app on launch.
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(context);
            }
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                firebaseAvailable = false;
                return;
            }

            auth = FirebaseAuth.getInstance();
            firestore = FirebaseFirestore.getInstance();

            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build();
            firestore.setFirestoreSettings(settings);
            firebaseAvailable = true;
        } catch (Exception e) {
            firebaseAvailable = false;
        }
    }

    /** True once a real Firebase project (app/google-services.json) is actually configured. */
    public boolean isFirebaseAvailable() {
        return firebaseAvailable;
    }

    public FirebaseAuth getAuth() { return auth; }
    public FirebaseFirestore getFirestore() { return firestore; }
    public boolean isLoggedIn() {
        return firebaseAvailable && auth != null && auth.getCurrentUser() != null;
    }

    @Nullable
    public String getCurrentUid() {
        if (!firebaseAvailable || auth == null) return null;
        FirebaseUser fu = auth.getCurrentUser();
        return fu != null ? fu.getUid() : null;
    }

    /** Cached user snapshot (may be stale). Use loadCurrentUser() for a fresh Firestore read. */
    @Nullable
    public User getCurrentUser() {
        return currentUserCache;
    }

    public void setCurrentUserCache(User user) {
        this.currentUserCache = user;
    }

    /** Creates (if needed) and returns the Firestore-backed profile for the given auth user. */
    public void ensureUserProfile(FirebaseUser firebaseUser, String username, UserCallback callback) {
        if (firebaseUser == null) {
            if (callback != null) callback.onResult(null);
            return;
        }
        String uid = firebaseUser.getUid();
        firestore.collection(COLLECTION_USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            user.setUid(uid);
                            user.setOnline(true);
                            currentUserCache = user;
                            firestore.collection(COLLECTION_USERS).document(uid)
                                    .update("online", true, "lastSeen", System.currentTimeMillis());
                            if (callback != null) callback.onResult(user);
                            return;
                        }
                    }
                    // First time we see this auth user - create a real, empty (non-fake) profile.
                    String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";
                    String name = username != null && !username.isEmpty()
                            ? username
                            : (firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName()
                            : (email.contains("@") ? email.substring(0, email.indexOf('@')) : "user"));
                    User newUser = new User(uid, name.toLowerCase().replace(" ", "_"), email, name,
                            firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "");
                    firestore.collection(COLLECTION_USERS).document(uid).set(newUser)
                            .addOnSuccessListener(v -> {
                                currentUserCache = newUser;
                                if (callback != null) callback.onResult(newUser);
                            })
                            .addOnFailureListener(e -> {
                                currentUserCache = newUser;
                                if (callback != null) callback.onResult(newUser);
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onResult(null);
                });
    }

    /** Re-fetches the current user's profile from Firestore (real data, not cached). */
    public void loadCurrentUser(UserCallback callback) {
        String uid = getCurrentUid();
        if (uid == null) {
            currentUserCache = null;
            if (callback != null) callback.onResult(null);
            return;
        }
        firestore.collection(COLLECTION_USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    User user = doc.exists() ? doc.toObject(User.class) : null;
                    if (user != null) user.setUid(uid);
                    currentUserCache = user;
                    if (callback != null) callback.onResult(user);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onResult(currentUserCache);
                });
    }

    public void saveUserToFirestore(User user) {
        if (user == null || user.getUid() == null) return;
        currentUserCache = user;
        firestore.collection(COLLECTION_USERS).document(user.getUid()).set(user);
    }

    public void updateUserFields(java.util.Map<String, Object> fields) {
        String uid = getCurrentUid();
        if (uid == null) return;
        firestore.collection(COLLECTION_USERS).document(uid).update(fields);
    }

    /** Real Firestore query for other online users (no fake/sample data). */
    public void getOnlineUsers(UsersCallback callback) {
        String myUid = getCurrentUid();
        firestore.collection(COLLECTION_USERS)
                .whereEqualTo("online", true)
                .orderBy("lastSeen", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(snap -> {
                    List<User> users = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                        User u = doc.toObject(User.class);
                        if (u != null) {
                            u.setUid(doc.getId());
                            if (myUid == null || !myUid.equals(u.getUid())) {
                                users.add(u);
                            }
                        }
                    }
                    if (callback != null) callback.onResult(users);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onResult(new ArrayList<>());
                });
    }

    /** Real Firestore query across ALL users, for the admin console (no fake sample list). */
    public void getAllUsersForAdmin(UsersCallback callback) {
        firestore.collection(COLLECTION_USERS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(200)
                .get()
                .addOnSuccessListener(snap -> {
                    List<User> users = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                        User u = doc.toObject(User.class);
                        if (u != null) {
                            u.setUid(doc.getId());
                            users.add(u);
                        }
                    }
                    if (callback != null) callback.onResult(users);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onResult(new ArrayList<>());
                });
    }

    public void logout() {
        String uid = getCurrentUid();
        if (uid != null) {
            try {
                firestore.collection(COLLECTION_USERS).document(uid)
                        .update("online", false, "lastSeen", System.currentTimeMillis());
            } catch (Exception ignored) {}
        }
        try {
            auth.signOut();
        } catch (Exception ignored) {}
        currentUserCache = null;
    }
}
