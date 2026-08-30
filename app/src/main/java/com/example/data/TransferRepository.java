package com.example.data;

import android.content.Context;
import androidx.annotation.Nullable;
import com.example.model.TransferItem;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Real Firestore-backed transfer history - no seeded/sample transfers.
 * A transfer document lives at transfers/{id} and carries a "participants"
 * array ([senderUid, receiverUid]) so both sides of a share can query it.
 */
public class TransferRepository {

    private static final String COLLECTION = "transfers";

    private static TransferRepository instance;
    private final Context context;
    private final FirebaseFirestore firestore;
    private final List<TransferItem> cache = new ArrayList<>();

    public interface TransferCallback {
        void onLoaded(List<TransferItem> items);
    }

    public interface TransferListener {
        void onUpdated(List<TransferItem> items);
    }

    private TransferRepository(Context context) {
        this.context = context.getApplicationContext();
        this.firestore = FirebaseFirestore.getInstance();
    }

    public static synchronized TransferRepository getInstance(Context context) {
        if (instance == null) {
            instance = new TransferRepository(context);
        }
        return instance;
    }

    /** Last known snapshot - safe to call synchronously for an instant first paint. */
    public List<TransferItem> getCachedTransfers() {
        return new ArrayList<>(cache);
    }

    /** One-shot real fetch of everything the current user sent or received. */
    public void refreshTransfers(String currentUid, TransferCallback callback) {
        if (currentUid == null) {
            if (callback != null) callback.onLoaded(new ArrayList<>());
            return;
        }
        firestore.collection(COLLECTION)
                .whereArrayContains("participants", currentUid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(200)
                .get()
                .addOnSuccessListener(snap -> {
                    cache.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        TransferItem item = doc.toObject(TransferItem.class);
                        if (item != null) {
                            item.setId(doc.getId());
                            cache.add(item);
                        }
                    }
                    if (callback != null) callback.onLoaded(new ArrayList<>(cache));
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onLoaded(new ArrayList<>(cache));
                });
    }

    /** Live real-time updates (new transfer appears the instant it's written). */
    public ListenerRegistration listenForTransfers(String currentUid, TransferListener listener) {
        return firestore.collection(COLLECTION)
                .whereArrayContains("participants", currentUid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(200)
                .addSnapshotListener((snap, error) -> {
                    if (error != null || snap == null) return;
                    cache.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        TransferItem item = doc.toObject(TransferItem.class);
                        if (item != null) {
                            item.setId(doc.getId());
                            cache.add(item);
                        }
                    }
                    if (listener != null) listener.onUpdated(new ArrayList<>(cache));
                });
    }

    public void addTransfer(TransferItem item) {
        if (item.getId() == null) return;
        item.setParticipants(Arrays.asList(nullToEmpty(item.getSenderUid()), nullToEmpty(item.getReceiverUid())));
        cache.add(0, item);
        firestore.collection(COLLECTION).document(item.getId()).set(item);
    }

    public void updateTransfer(TransferItem item) {
        if (item.getId() == null) return;
        item.setParticipants(Arrays.asList(nullToEmpty(item.getSenderUid()), nullToEmpty(item.getReceiverUid())));
        for (int i = 0; i < cache.size(); i++) {
            if (item.getId().equals(cache.get(i).getId())) {
                cache.set(i, item);
                break;
            }
        }
        firestore.collection(COLLECTION).document(item.getId()).set(item);
    }

    public void deleteTransfer(String id) {
        if (id == null) return;
        for (int i = 0; i < cache.size(); i++) {
            if (id.equals(cache.get(i).getId())) {
                cache.remove(i);
                break;
            }
        }
        firestore.collection(COLLECTION).document(id).delete();
    }

    @Nullable
    public TransferItem findCached(String id) {
        for (TransferItem item : cache) {
            if (item.getId() != null && item.getId().equals(id)) return item;
        }
        return null;
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}
