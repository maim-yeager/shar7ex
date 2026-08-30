package com.example.ui.transfer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.R;
import com.example.data.FirebaseManager;
import com.example.data.TransferRepository;
import com.example.model.TransferItem;
import com.example.ui.viewer.MediaViewerActivity;
import com.example.util.ShareUtils;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;

public class TransfersFragment extends Fragment {

    private RecyclerView rvTransfersList;
    private TransfersAdapter adapter;
    private TextView chipAll, chipOriginal, chipPhotos, chipVideos;
    private View btnNewTransferFab;
    private List<TransferItem> allTransfers = new ArrayList<>();
    private String currentFilter = "ALL";
    private ListenerRegistration listenerRegistration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transfers, container, false);

        rvTransfersList = view.findViewById(R.id.rvTransfersList);
        chipAll = view.findViewById(R.id.chipAll);
        chipOriginal = view.findViewById(R.id.chipOriginal);
        chipPhotos = view.findViewById(R.id.chipPhotos);
        chipVideos = view.findViewById(R.id.chipVideos);
        btnNewTransferFab = view.findViewById(R.id.btnNewTransferFab);

        rvTransfersList.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TransfersAdapter(new TransfersAdapter.OnTransferActionListener() {
            @Override
            public void onViewMedia(TransferItem item) {
                Intent intent = new Intent(getContext(), MediaViewerActivity.class);
                intent.putExtra("transfer_item", item);
                startActivity(intent);
            }

            @Override
            public void onShareLink(TransferItem item) {
                ShareUtils.shareTransferItem(requireContext(), item);
            }

            @Override
            public void onShowDetails(TransferItem item) {
                TransferDetailsDialog dialog = new TransferDetailsDialog(requireContext(), item);
                dialog.show();
            }
        });
        rvTransfersList.setAdapter(adapter);
        adapter.setCurrentUid(FirebaseManager.getInstance(requireContext()).getCurrentUid());

        // Show cached data instantly, then real-time Firestore data takes over.
        allTransfers = TransferRepository.getInstance(requireContext()).getCachedTransfers();
        applyFilter(currentFilter, chipAll);

        setupFilters();

        btnNewTransferFab.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), MediaPickerActivity.class));
        });

        return view;
    }

    private void setupFilters() {
        chipAll.setOnClickListener(v -> applyFilter("ALL", chipAll));
        chipOriginal.setOnClickListener(v -> applyFilter("ORIGINAL", chipOriginal));
        chipPhotos.setOnClickListener(v -> applyFilter("PHOTOS", chipPhotos));
        chipVideos.setOnClickListener(v -> applyFilter("VIDEOS", chipVideos));
    }

    private void applyFilter(String filter, TextView activeChip) {
        currentFilter = filter;
        chipAll.setBackgroundResource(R.drawable.bg_button_secondary);
        chipOriginal.setBackgroundResource(R.drawable.bg_button_secondary);
        chipPhotos.setBackgroundResource(R.drawable.bg_button_secondary);
        chipVideos.setBackgroundResource(R.drawable.bg_button_secondary);

        chipAll.setTextColor(0xFF8B9CB5);
        chipOriginal.setTextColor(0xFF8B9CB5);
        chipPhotos.setTextColor(0xFF8B9CB5);
        chipVideos.setTextColor(0xFF8B9CB5);

        activeChip.setBackgroundResource(R.drawable.bg_button_primary);
        activeChip.setTextColor(0xFFFFFFFF);

        List<TransferItem> filtered = new ArrayList<>();
        for (TransferItem item : allTransfers) {
            if ("ALL".equals(filter)) {
                filtered.add(item);
            } else if ("ORIGINAL".equals(filter) && item.isOriginalQuality()) {
                filtered.add(item);
            } else if ("PHOTOS".equals(filter) && item.isImage()) {
                filtered.add(item);
            } else if ("VIDEOS".equals(filter) && item.isVideo()) {
                filtered.add(item);
            }
        }
        adapter.setTransfers(filtered);
    }

    @Override
    public void onResume() {
        super.onResume();
        String uid = FirebaseManager.getInstance(requireContext()).getCurrentUid();
        if (uid != null) {
            listenerRegistration = TransferRepository.getInstance(requireContext()).listenForTransfers(uid, items -> {
                allTransfers = items;
                applyFilter(currentFilter, currentChip());
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    private TextView currentChip() {
        switch (currentFilter) {
            case "ORIGINAL": return chipOriginal;
            case "PHOTOS": return chipPhotos;
            case "VIDEOS": return chipVideos;
            default: return chipAll;
        }
    }
}
