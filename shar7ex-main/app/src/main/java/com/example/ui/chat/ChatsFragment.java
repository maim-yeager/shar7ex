package com.example.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.R;
import com.example.data.ChatRepository;
import com.example.data.FirebaseManager;
import com.example.model.ChatConversation;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;

public class ChatsFragment extends Fragment {

    private RecyclerView rvConversations;
    private ConversationsAdapter adapter;
    private EditText etSearchChats;
    private List<ChatConversation> allConversations = new ArrayList<>();
    private ListenerRegistration listenerRegistration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chats, container, false);

        rvConversations = view.findViewById(R.id.rvConversations);
        etSearchChats = view.findViewById(R.id.etSearchChats);

        rvConversations.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ConversationsAdapter(conversation -> {
            Intent intent = new Intent(getContext(), ChatActivity.class);
            intent.putExtra("chat_id", conversation.getChatId());
            intent.putExtra("other_uid", conversation.getOtherUid());
            intent.putExtra("other_name", conversation.getOtherUserName());
            startActivity(intent);
        });
        rvConversations.setAdapter(adapter);

        etSearchChats.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterChats(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void filterChats(String query) {
        if (query.isEmpty()) {
            adapter.setConversations(allConversations);
            return;
        }
        List<ChatConversation> filtered = new ArrayList<>();
        for (ChatConversation c : allConversations) {
            if (c.getOtherUserName().toLowerCase().contains(query.toLowerCase()) ||
                    (c.getLastMessage() != null && c.getLastMessage().toLowerCase().contains(query.toLowerCase()))) {
                filtered.add(c);
            }
        }
        adapter.setConversations(filtered);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getContext() == null) return;
        String uid = FirebaseManager.getInstance(getContext()).getCurrentUid();
        if (uid == null) return;
        listenerRegistration = ChatRepository.getInstance(getContext()).listenForConversations(uid, conversations -> {
            allConversations = conversations;
            filterChats(etSearchChats.getText().toString());
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }
}
