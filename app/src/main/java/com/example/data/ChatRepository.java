package com.example.data;

import android.content.Context;
import com.example.model.ChatConversation;
import com.example.model.ChatMessage;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Real Firestore-backed 1:1 chat - no pre-seeded fake conversations.
 * chats/{chatId} holds the conversation metadata (chatId is a deterministic
 * "smallerUid_largerUid" pair so both sides always land on the same document),
 * chats/{chatId}/messages/{messageId} holds the real-time message thread.
 */
public class ChatRepository {

    private static final String CHATS = "chats";
    private static final String MESSAGES = "messages";

    private static ChatRepository instance;
    private final Context context;
    private final FirebaseFirestore firestore;

    public interface ConversationCallback {
        void onLoaded(ChatConversation conversation);
    }

    public interface ConversationsListener {
        void onUpdated(List<ChatConversation> conversations);
    }

    public interface MessagesListener {
        void onUpdated(List<ChatMessage> messages);
    }

    private ChatRepository(Context context) {
        this.context = context.getApplicationContext();
        this.firestore = FirebaseFirestore.getInstance();
    }

    public static synchronized ChatRepository getInstance(Context context) {
        if (instance == null) {
            instance = new ChatRepository(context);
        }
        return instance;
    }

    public static String buildChatId(String uidA, String uidB) {
        if (uidA == null || uidB == null) return null;
        return uidA.compareTo(uidB) < 0 ? (uidA + "_" + uidB) : (uidB + "_" + uidA);
    }

    /** Ensures a real chat document exists between the two users and returns it. */
    public void getOrCreateConversation(String myUid, String myName, String otherUid, String otherName, ConversationCallback callback) {
        String chatId = buildChatId(myUid, otherUid);
        if (chatId == null) {
            if (callback != null) callback.onLoaded(null);
            return;
        }
        firestore.collection(CHATS).document(chatId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                if (callback != null) callback.onLoaded(toConversation(doc, myUid));
                return;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("chatId", chatId);
            data.put("participants", Arrays.asList(myUid, otherUid));
            Map<String, String> names = new HashMap<>();
            names.put(myUid, myName != null ? myName : "Me");
            names.put(otherUid, otherName != null ? otherName : "SHAREX User");
            data.put("participantNames", names);
            data.put("lastMessage", "Connected via SHAREX");
            data.put("lastMessageType", "TEXT");
            data.put("lastMessageTime", System.currentTimeMillis());
            data.put("lastSenderId", myUid);
            Map<String, Long> unread = new HashMap<>();
            unread.put(myUid, 0L);
            unread.put(otherUid, 0L);
            data.put("unreadCounts", unread);

            firestore.collection(CHATS).document(chatId).set(data)
                    .addOnSuccessListener(v -> firestore.collection(CHATS).document(chatId).get()
                            .addOnSuccessListener(freshDoc -> {
                                if (callback != null) callback.onLoaded(toConversation(freshDoc, myUid));
                            }))
                    .addOnFailureListener(e -> {
                        if (callback != null) callback.onLoaded(null);
                    });
        });
    }

    public ListenerRegistration listenForConversations(String myUid, ConversationsListener listener) {
        return firestore.collection(CHATS)
                .whereArrayContains("participants", myUid)
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, error) -> {
                    if (error != null || snap == null) return;
                    List<ChatConversation> conversations = new ArrayList<>();
                    List<String> otherUids = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        ChatConversation c = toConversation(doc, myUid);
                        if (c != null) {
                            conversations.add(c);
                            if (c.getOtherUid() != null) otherUids.add(c.getOtherUid());
                        }
                    }
                    if (otherUids.isEmpty()) {
                        if (listener != null) listener.onUpdated(conversations);
                        return;
                    }
                    // Enrich with each peer's REAL live presence/photo from their profile doc.
                    firestore.collection(FirebaseManager.COLLECTION_USERS)
                            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), otherUids.subList(0, Math.min(otherUids.size(), 30)))
                            .get()
                            .addOnSuccessListener(usersSnap -> {
                                Map<String, DocumentSnapshot> byUid = new HashMap<>();
                                for (DocumentSnapshot ud : usersSnap.getDocuments()) byUid.put(ud.getId(), ud);
                                for (ChatConversation c : conversations) {
                                    DocumentSnapshot ud = byUid.get(c.getOtherUid());
                                    if (ud != null) {
                                        Boolean online = ud.getBoolean("online");
                                        c.setOtherUserOnline(online != null && online);
                                        String photo = ud.getString("photoUrl");
                                        c.setOtherUserPhoto(photo);
                                    }
                                }
                                if (listener != null) listener.onUpdated(conversations);
                            })
                            .addOnFailureListener(e -> {
                                if (listener != null) listener.onUpdated(conversations);
                            });
                });
    }

    public ListenerRegistration listenForMessages(String chatId, MessagesListener listener) {
        return firestore.collection(CHATS).document(chatId).collection(MESSAGES)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(500)
                .addSnapshotListener((snap, error) -> {
                    if (error != null || snap == null) return;
                    List<ChatMessage> messages = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        ChatMessage m = doc.toObject(ChatMessage.class);
                        if (m != null) {
                            m.setMessageId(doc.getId());
                            messages.add(m);
                        }
                    }
                    if (listener != null) listener.onUpdated(messages);
                });
    }

    public void sendMessage(String chatId, String myUid, String otherUid, ChatMessage message) {
        firestore.collection(CHATS).document(chatId).collection(MESSAGES)
                .document(message.getMessageId())
                .set(message);

        Map<String, Object> chatUpdate = new HashMap<>();
        chatUpdate.put("lastMessage", "TEXT".equals(message.getMessageType())
                ? message.getText()
                : "Shared " + message.getFileName() + " (" + (message.isOriginalQuality() ? "Original Quality" : "Compressed") + ")");
        chatUpdate.put("lastMessageType", message.getMessageType());
        chatUpdate.put("lastMessageTime", message.getTimestamp());
        chatUpdate.put("lastSenderId", myUid);
        chatUpdate.put("unreadCounts." + otherUid, FieldValue.increment(1));
        chatUpdate.put("unreadCounts." + myUid, 0L);
        firestore.collection(CHATS).document(chatId).update(chatUpdate);
    }

    public void markRead(String chatId, String myUid) {
        firestore.collection(CHATS).document(chatId)
                .update("unreadCounts." + myUid, 0L);
    }

    /** Once the real Storage upload finishes, patch the message so the OTHER device can load it too. */
    public void attachUploadedMedia(String chatId, String messageId, String downloadUrl, String exifSummary) {
        if (chatId == null || messageId == null || downloadUrl == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("mediaUrl", downloadUrl);
        updates.put("status", "SENT");
        firestore.collection(CHATS).document(chatId).collection(MESSAGES).document(messageId).update(updates);
    }

    @SuppressWarnings("unchecked")
    private ChatConversation toConversation(DocumentSnapshot doc, String myUid) {
        if (!doc.exists()) return null;
        List<String> participants = (List<String>) doc.get("participants");
        if (participants == null || participants.size() < 2) return null;
        String otherUid = participants.get(0).equals(myUid) ? participants.get(1) : participants.get(0);

        Map<String, String> names = (Map<String, String>) doc.get("participantNames");
        String otherName = names != null ? names.get(otherUid) : null;

        Map<String, Object> unreadCounts = (Map<String, Object>) doc.get("unreadCounts");
        int unread = 0;
        if (unreadCounts != null && unreadCounts.get(myUid) instanceof Long) {
            unread = ((Long) unreadCounts.get(myUid)).intValue();
        }

        ChatConversation c = new ChatConversation();
        c.setChatId(doc.getId());
        c.setParticipantUids(participants);
        c.setOtherUid(otherUid);
        c.setOtherUserName(otherName);
        c.setLastMessage(doc.getString("lastMessage"));
        c.setLastMessageType(doc.getString("lastMessageType"));
        Long lastTime = doc.getLong("lastMessageTime");
        c.setLastMessageTime(lastTime != null ? lastTime : 0);
        c.setUnreadCount(unread);
        return c;
    }
}
