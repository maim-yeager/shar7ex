package com.example.ui.qr;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import com.example.R;
import com.example.data.ChatRepository;
import com.example.data.FirebaseManager;
import com.example.model.User;
import com.example.ui.chat.ChatActivity;
import com.example.util.QrCodeUtil;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

public class QrShareActivity extends AppCompatActivity {

    private ImageView ivQrBack, ivQrCodeImage;
    private TextView tvPinCode, tvQrTimer;
    private AppCompatButton btnSwitchToScanner, btnRegenerateQr;
    private CountDownTimer timer;
    private String myUid, myName;

    private final ActivityResultLauncher<ScanOptions> scanLauncher = registerForActivityResult(
            new ScanContract(), this::handleScanResult
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_share);

        ivQrBack = findViewById(R.id.ivQrBack);
        ivQrCodeImage = findViewById(R.id.ivQrCodeImage);
        tvPinCode = findViewById(R.id.tvPinCode);
        tvQrTimer = findViewById(R.id.tvQrTimer);
        btnSwitchToScanner = findViewById(R.id.btnSwitchToScanner);
        btnRegenerateQr = findViewById(R.id.btnRegenerateQr);

        User user = FirebaseManager.getInstance(this).getCurrentUser();
        myUid = user != null ? user.getUid() : FirebaseManager.getInstance(this).getCurrentUid();
        myName = user != null ? user.getDisplayName() : "SHAREX User";

        ivQrBack.setOnClickListener(v -> finish());
        btnRegenerateQr.setOnClickListener(v -> generateNewQrSession());

        btnSwitchToScanner.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setPrompt("Point the camera at your peer's SHAREX QR code");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            scanLauncher.launch(options);
        });

        generateNewQrSession();
    }

    private void handleScanResult(com.journeyapps.barcodescanner.ScanIntentResult result) {
        if (result == null || result.getContents() == null) {
            return; // user cancelled
        }
        String content = result.getContents();
        try {
            Uri parsed = Uri.parse(content);
            if (!"sharex".equals(parsed.getScheme()) || !"pair".equals(parsed.getHost())) {
                Toast.makeText(this, "That QR code isn't a SHAREX pairing code.", Toast.LENGTH_LONG).show();
                return;
            }
            String peerUid = parsed.getQueryParameter("uid");
            if (peerUid == null || peerUid.equals(myUid)) {
                Toast.makeText(this, "Invalid pairing code.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Real Firestore lookup of the scanned peer's profile.
            FirebaseManager.getInstance(this).getFirestore()
                    .collection(FirebaseManager.COLLECTION_USERS).document(peerUid).get()
                    .addOnSuccessListener(doc -> {
                        User peer = doc.exists() ? doc.toObject(User.class) : null;
                        String peerName = peer != null ? peer.getDisplayName() : "SHAREX User";

                        ChatRepository.getInstance(this).getOrCreateConversation(myUid, myName, peerUid, peerName, conversation -> {
                            Intent intent = new Intent(QrShareActivity.this, ChatActivity.class);
                            intent.putExtra("chat_id", conversation != null ? conversation.getChatId() : null);
                            intent.putExtra("other_uid", peerUid);
                            intent.putExtra("other_name", peerName);
                            startActivity(intent);
                            finish();
                        });
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Couldn't reach that peer's profile.", Toast.LENGTH_LONG).show());
        } catch (Exception e) {
            Toast.makeText(this, "Unrecognized QR code.", Toast.LENGTH_SHORT).show();
        }
    }

    private void generateNewQrSession() {
        int pin = 100000 + new Random().nextInt(900000);
        String formattedPin = "PIN: " + (pin / 1000) + "-" + (pin % 1000);
        tvPinCode.setText(formattedPin);

        String sessionPayload = "sharex://pair?uid=" + myUid + "&session=" + UUID.randomUUID().toString() + "&pin=" + pin;
        Bitmap qrBitmap = QrCodeUtil.generateQrCode(sessionPayload, 500, 500);
        if (qrBitmap != null) {
            ivQrCodeImage.setImageBitmap(qrBitmap);
        }

        if (timer != null) timer.cancel();
        timer = new CountDownTimer(10 * 60 * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long min = millisUntilFinished / 60000;
                long sec = (millisUntilFinished % 60000) / 1000;
                tvQrTimer.setText(String.format(Locale.getDefault(), "Session expires in %02d:%02d", min, sec));
            }

            @Override
            public void onFinish() {
                tvQrTimer.setText("Session expired. Tap Regenerate.");
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }
}
