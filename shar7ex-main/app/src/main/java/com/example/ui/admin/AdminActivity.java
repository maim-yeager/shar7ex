package com.example.ui.admin;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.R;
import com.example.data.FirebaseManager;
import com.example.model.User;
import com.example.util.FileUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminActivity extends AppCompatActivity {

    private ImageView ivAdminBack;
    private TextView tvAdminTotalUsers, tvAdminTotalStorage;
    private RecyclerView rvAdminUsers;
    private AdminUsersAdapter adapter;
    private EditText etAnnouncementMessage;
    private AppCompatButton btnBroadcast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        User currentUser = FirebaseManager.getInstance(this).getCurrentUser();
        if (currentUser == null || !currentUser.isAdmin()) {
            // Real access guard - role must actually be "admin" in Firestore, no email trick.
            Toast.makeText(this, "Admin access required.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        ivAdminBack = findViewById(R.id.ivAdminBack);
        tvAdminTotalUsers = findViewById(R.id.tvAdminTotalUsers);
        tvAdminTotalStorage = findViewById(R.id.tvAdminTotalStorage);
        rvAdminUsers = findViewById(R.id.rvAdminUsers);
        etAnnouncementMessage = findViewById(R.id.etAnnouncementMessage);
        btnBroadcast = findViewById(R.id.btnBroadcast);

        ivAdminBack.setOnClickListener(v -> finish());

        rvAdminUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminUsersAdapter(user -> {
            boolean newBlocked = !user.isBlocked();
            user.setBlocked(newBlocked);
            adapter.notifyDataSetChanged();
            // Real persistence - this actually changes what the user can do, not just a local toggle.
            FirebaseManager.getInstance(this).getFirestore()
                    .collection(FirebaseManager.COLLECTION_USERS).document(user.getUid())
                    .update("blocked", newBlocked);
            Toast.makeText(this, (newBlocked ? "Blocked " : "Unblocked ") + user.getDisplayName(), Toast.LENGTH_SHORT).show();
        });
        rvAdminUsers.setAdapter(adapter);

        loadRealAdminData();

        btnBroadcast.setOnClickListener(v -> {
            String msg = etAnnouncementMessage.getText().toString().trim();
            if (msg.isEmpty()) {
                Toast.makeText(this, "Please enter an announcement message", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Object> announcement = new HashMap<>();
            announcement.put("message", msg);
            announcement.put("timestamp", System.currentTimeMillis());
            announcement.put("sentBy", currentUser.getDisplayName());

            // Real Firestore write - every signed-in device has a live listener (see MainActivity)
            // that surfaces this the instant it lands, no fake "pushed!" toast with nothing behind it.
            FirebaseManager.getInstance(this).getFirestore().collection("announcements").add(announcement)
                    .addOnSuccessListener(ref -> {
                        Toast.makeText(this, "Announcement broadcast to all signed-in devices!", Toast.LENGTH_LONG).show();
                        etAnnouncementMessage.setText("");
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Broadcast failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });
    }

    private void loadRealAdminData() {
        tvAdminTotalUsers.setText("Loading…");
        tvAdminTotalStorage.setText("Loading…");

        FirebaseManager.getInstance(this).getAllUsersForAdmin(users -> {
            adapter.setUsers(users);
            tvAdminTotalUsers.setText(users.size() + (users.size() >= 200 ? "+" : ""));
        });

        // Real total across every transfer ever uploaded (not a per-user field that's never populated).
        FirebaseManager.getInstance(this).getFirestore().collection("transfers")
                .aggregate(com.google.firebase.firestore.AggregateField.sum("originalSizeBytes"))
                .get(com.google.firebase.firestore.AggregateSource.SERVER)
                .addOnSuccessListener(snap -> {
                    Number sum = (Number) snap.get(com.google.firebase.firestore.AggregateField.sum("originalSizeBytes"));
                    tvAdminTotalStorage.setText(FileUtils.formatFileSize(sum != null ? sum.longValue() : 0));
                })
                .addOnFailureListener(e -> tvAdminTotalStorage.setText("N/A"));
    }
}
