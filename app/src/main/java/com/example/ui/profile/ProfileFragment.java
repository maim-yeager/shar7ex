package com.example.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.R;
import com.example.data.FirebaseManager;
import com.example.model.User;
import com.example.ui.admin.AdminActivity;
import com.example.ui.auth.AuthActivity;
import com.example.ui.settings.SettingsActivity;
import com.example.util.FileUtils;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private TextView tvProfileDisplayName, tvProfileUsername, tvProfileEmail, tvStatSent, tvStatRecv, tvStatStorage;
    private View btnMenuSettings, btnMenuAdmin, btnMenuLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvProfileDisplayName = view.findViewById(R.id.tvProfileDisplayName);
        tvProfileUsername = view.findViewById(R.id.tvProfileUsername);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvStatSent = view.findViewById(R.id.tvStatSent);
        tvStatRecv = view.findViewById(R.id.tvStatRecv);
        tvStatStorage = view.findViewById(R.id.tvStatStorage);
        btnMenuSettings = view.findViewById(R.id.btnMenuSettings);
        btnMenuAdmin = view.findViewById(R.id.btnMenuAdmin);
        btnMenuLogout = view.findViewById(R.id.btnMenuLogout);

        loadUserData();

        btnMenuSettings.setOnClickListener(v -> startActivity(new Intent(getContext(), SettingsActivity.class)));

        btnMenuAdmin.setOnClickListener(v -> startActivity(new Intent(getContext(), AdminActivity.class)));

        btnMenuLogout.setOnClickListener(v -> {
            if (getContext() != null) {
                FirebaseManager.getInstance(getContext()).logout();
                Toast.makeText(getContext(), "Logged out of M.SHAREX", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getContext(), AuthActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

        return view;
    }

    private void loadUserData() {
        if (getContext() == null) return;
        User user = FirebaseManager.getInstance(getContext()).getCurrentUser();
        if (user == null) return;

        tvProfileDisplayName.setText(user.getDisplayName());
        tvProfileUsername.setText("@" + user.getUsername() + " • UID: " + user.getUid());
        tvProfileEmail.setText(user.getEmail());
        btnMenuAdmin.setVisibility(user.isAdmin() ? View.VISIBLE : View.GONE);

        // Real counts computed straight from the transfers collection (not a cached counter
        // that would require writing to someone else's user document to keep in sync).
        FirebaseFirestore db = FirebaseManager.getInstance(getContext()).getFirestore();
        String uid = user.getUid();

        db.collection("transfers").whereEqualTo("senderUid", uid).count().get(AggregateSource.SERVER)
                .addOnSuccessListener(snap -> { if (isAdded()) tvStatSent.setText(String.valueOf(snap.getCount())); });

        db.collection("transfers").whereEqualTo("receiverUid", uid).count().get(AggregateSource.SERVER)
                .addOnSuccessListener(snap -> { if (isAdded()) tvStatRecv.setText(String.valueOf(snap.getCount())); });

        db.collection("transfers").whereEqualTo("senderUid", uid)
                .aggregate(com.google.firebase.firestore.AggregateField.sum("originalSizeBytes"))
                .get(AggregateSource.SERVER)
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    Number sum = (Number) snap.get(com.google.firebase.firestore.AggregateField.sum("originalSizeBytes"));
                    tvStatStorage.setText(FileUtils.formatFileSize(sum != null ? sum.longValue() : 0));
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserData();
    }
}
