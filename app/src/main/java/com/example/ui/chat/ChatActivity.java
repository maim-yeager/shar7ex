package com.example.ui.chat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.R;
import com.example.data.ChatRepository;
import com.example.data.FirebaseManager;
import com.example.model.ChatMessage;
import com.example.model.TransferItem;
import com.example.model.User;
import com.example.transfer.OriginalQualityEngine;
import com.example.transfer.TransferManager;
import com.example.ui.viewer.MediaViewerActivity;
import com.example.util.FileUtils;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {

    private String chatId;
    private String otherUid;
    private String otherName;
    private String currentUid;
    private String currentName;
    private RecyclerView rvChatMessages;
    private MessagesAdapter adapter;
    private EditText etChatMessageInput;
    private ImageView btnSendChatMessage, btnAttachMedia, ivChatBack;
    private TextView tvChatTitleName, tvChatStatusSub;
    private ListenerRegistration messagesListener;

    private final ActivityResultLauncher<String> mediaPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::handleSelectedMediaAttachment
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        otherUid = getIntent().getStringExtra("other_uid");
        otherName = getIntent().getStringExtra("other_name");
        if (otherName == null) otherName = "SHAREX User";

        User currentUser = FirebaseManager.getInstance(this).getCurrentUser();
        currentUid = FirebaseManager.getInstance(this).getCurrentUid();
        currentName = currentUser != null ? currentUser.getDisplayName() : "Me";

        String passedChatId = getIntent().getStringExtra("chat_id");
        chatId = passedChatId != null ? passedChatId : ChatRepository.buildChatId(currentUid, otherUid);

        // Ensure the conversation document really exists (for the chats list) without
        // blocking message rendering, which can start immediately on the deterministic id.
        ChatRepository.getInstance(this).getOrCreateConversation(currentUid, currentName, otherUid, otherName, c -> {});

        rvChatMessages = findViewById(R.id.rvChatMessages);
        etChatMessageInput = findViewById(R.id.etChatMessageInput);
        btnSendChatMessage = findViewById(R.id.btnSendChatMessage);
        btnAttachMedia = findViewById(R.id.btnAttachMedia);
        ivChatBack = findViewById(R.id.ivChatBack);
        tvChatTitleName = findViewById(R.id.tvChatTitleName);
        tvChatStatusSub = findViewById(R.id.tvChatStatusSub);

        tvChatTitleName.setText(otherName);
        tvChatStatusSub.setText("Original quality sharing");

        adapter = new MessagesAdapter(currentUid, message -> {
            TransferItem item = new TransferItem();
            item.setId(message.getMessageId());
            item.setFileName(message.getFileName() != null ? message.getFileName() : "Media File");
            item.setOriginalSizeBytes(message.getFileSize());
            item.setOriginalWidth(message.getWidth());
            item.setOriginalHeight(message.getHeight());
            item.setDurationMs(message.getDurationMs());
            item.setOriginalQuality(message.isOriginalQuality());
            item.setLocalUri(message.getLocalUri());
            item.setDownloadUrl(message.getMediaUrl());
            item.setMimeType("VIDEO".equalsIgnoreCase(message.getMessageType()) ? "video/mp4" : "image/jpeg");

            Intent viewerIntent = new Intent(ChatActivity.this, MediaViewerActivity.class);
            viewerIntent.putExtra("transfer_item", item);
            startActivity(viewerIntent);
        });

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvChatMessages.setLayoutManager(lm);
        rvChatMessages.setAdapter(adapter);

        ivChatBack.setOnClickListener(v -> finish());
        btnSendChatMessage.setOnClickListener(v -> sendTextMessage());
        btnAttachMedia.setOnClickListener(v -> mediaPickerLauncher.launch("*/*"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        messagesListener = ChatRepository.getInstance(this).listenForMessages(chatId, messages -> {
            adapter.setMessages(messages);
            if (!messages.isEmpty()) {
                rvChatMessages.scrollToPosition(messages.size() - 1);
            }
        });
        ChatRepository.getInstance(this).markRead(chatId, currentUid);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
    }

    private void sendTextMessage() {
        String text = etChatMessageInput.getText().toString().trim();
        if (text.isEmpty()) return;

        ChatMessage msg = new ChatMessage();
        msg.setMessageId(UUID.randomUUID().toString());
        msg.setChatId(chatId);
        msg.setSenderId(currentUid);
        msg.setSenderName(currentName);
        msg.setReceiverId(otherUid);
        msg.setMessageType("TEXT");
        msg.setText(text);
        msg.setTimestamp(System.currentTimeMillis());
        msg.setStatus("SENT");

        ChatRepository.getInstance(this).sendMessage(chatId, currentUid, otherUid, msg);
        etChatMessageInput.setText("");
    }

    private void handleSelectedMediaAttachment(Uri uri) {
        if (uri == null) return;

        String fileName = FileUtils.getFileName(this, uri);
        long fileSize = FileUtils.getFileSize(this, uri);
        String mime = getContentResolver().getType(uri);
        boolean isVideo = mime != null && mime.startsWith("video");

        String messageId = UUID.randomUUID().toString();
        ChatMessage msg = new ChatMessage();
        msg.setMessageId(messageId);
        msg.setChatId(chatId);
        msg.setSenderId(currentUid);
        msg.setSenderName(currentName);
        msg.setReceiverId(otherUid);
        msg.setMessageType(isVideo ? "VIDEO" : "PHOTO");
        msg.setFileName(fileName);
        msg.setFileSize(fileSize);
        msg.setLocalUri(uri.toString()); // only valid on this device until the real upload finishes
        msg.setOriginalQuality(true); // 100% untouched
        msg.setTimestamp(System.currentTimeMillis());
        msg.setStatus("SENDING");

        ChatRepository.getInstance(this).sendMessage(chatId, currentUid, otherUid, msg);

        // Real background upload to Firebase Storage; once it finishes we patch this exact
        // message with a real, cross-device mediaUrl so the other person can actually see it.
        TransferManager.getInstance(this).startUpload(
                uri,
                OriginalQualityEngine.TransferMode.ORIGINAL_QUALITY,
                otherUid,
                otherName,
                new TransferManager.TransferProgressListener() {
                    @Override public void onProgress(TransferItem item, int progress) {}

                    @Override
                    public void onCompleted(TransferItem item) {
                        ChatRepository.getInstance(ChatActivity.this)
                                .attachUploadedMedia(chatId, messageId, item.getDownloadUrl(), item.getExifSummary());
                    }

                    @Override
                    public void onError(TransferItem item, String errorMessage) {
                        Toast.makeText(ChatActivity.this, "Upload failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
        );

        Toast.makeText(this, "Sending " + fileName + " (100% Original)", Toast.LENGTH_SHORT).show();
    }
}
