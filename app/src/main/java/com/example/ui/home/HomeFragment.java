package com.example.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.R;
import com.example.data.FirebaseManager;
import com.example.data.TransferRepository;
import com.example.ui.MainActivity;
import com.example.ui.chat.ChatActivity;
import com.example.ui.nearby.NearbyTransferActivity;
import com.example.ui.qr.QrShareActivity;
import com.example.ui.transfer.MediaPickerActivity;
import com.example.ui.viewer.MediaViewerActivity;
import com.google.firebase.firestore.ListenerRegistration;

public class HomeFragment extends Fragment {

    private RecyclerView rvOnlineUsers, rvRecentTransfers;
    private OnlineUsersAdapter onlineUsersAdapter;
    private RecentTransfersAdapter recentTransfersAdapter;
    private View btnQuickSend, btnQuickReceive, btnQuickNearby;
    private TextView tvViewAllTransfers;
    private ImageView ivHeaderProfile;
    private EditText etSearchUsers;
    private ListenerRegistration transfersListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        rvOnlineUsers = view.findViewById(R.id.rvOnlineUsers);
        rvRecentTransfers = view.findViewById(R.id.rvRecentTransfers);
        btnQuickSend = view.findViewById(R.id.btnQuickSend);
        btnQuickReceive = view.findViewById(R.id.btnQuickReceive);
        btnQuickNearby = view.findViewById(R.id.btnQuickNearby);
        tvViewAllTransfers = view.findViewById(R.id.tvViewAllTransfers);
        ivHeaderProfile = view.findViewById(R.id.ivHeaderProfile);
        etSearchUsers = view.findViewById(R.id.etSearchUsers);

        setupRecyclerViews();
        setupClickListeners();

        return view;
    }

    private void setupRecyclerViews() {
        rvOnlineUsers.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        onlineUsersAdapter = new OnlineUsersAdapter(user -> {
            Intent intent = new Intent(getContext(), ChatActivity.class);
            intent.putExtra("other_uid", user.getUid());
            intent.putExtra("other_name", user.getDisplayName());
            startActivity(intent);
        });
        rvOnlineUsers.setAdapter(onlineUsersAdapter);

        rvRecentTransfers.setLayoutManager(new LinearLayoutManager(getContext()));
        recentTransfersAdapter = new RecentTransfersAdapter(item -> {
            Intent intent = new Intent(getContext(), MediaViewerActivity.class);
            intent.putExtra("transfer_item", item);
            startActivity(intent);
        });
        rvRecentTransfers.setAdapter(recentTransfersAdapter);
    }

    private void setupClickListeners() {
        // No specific recipient chosen here - MediaPickerActivity falls back to
        // generating a real shareable link (like WeTransfer) via the system Share Sheet.
        btnQuickSend.setOnClickListener(v -> startActivity(new Intent(getContext(), MediaPickerActivity.class)));

        btnQuickReceive.setOnClickListener(v -> startActivity(new Intent(getContext(), QrShareActivity.class)));

        btnQuickNearby.setOnClickListener(v -> startActivity(new Intent(getContext(), NearbyTransferActivity.class)));

        tvViewAllTransfers.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).selectTab(R.id.nav_transfers);
            }
        });

        ivHeaderProfile.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).selectTab(R.id.nav_profile);
            }
        });
    }

    private void loadData() {
        if (getContext() == null) return;

        FirebaseManager.getInstance(getContext()).getOnlineUsers(users -> onlineUsersAdapter.setUsers(users));

        recentTransfersAdapter.setItems(TransferRepository.getInstance(getContext()).getCachedTransfers());
        String uid = FirebaseManager.getInstance(getContext()).getCurrentUid();
        if (uid != null) {
            transfersListener = TransferRepository.getInstance(getContext()).listenForTransfers(uid, items -> {
                if (isAdded()) recentTransfersAdapter.setItems(items);
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (transfersListener != null) {
            transfersListener.remove();
            transfersListener = null;
        }
    }
}
