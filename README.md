<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# M.SHAREX - Premium Media Sharing

A real-time, original-quality photo & video sharing app: Firebase Auth accounts, live chat,
cloud transfers, device-to-device Nearby sharing, and QR pairing.

## What changed in this pass

The project as generated had a working UI but most features were simulated with fake/demo
data instead of real backend calls. This pass removed all of that and wired every screen to
a real implementation:

- **Sign in / Sign up** now really calls Firebase Auth (email/password + Google) instead of
  accepting any input and faking a session. The old "email contains admin = admin access" hole
  is gone; admin is now only a real Firestore `role` field you set yourself.
- **Sending a file** really uploads to Firebase Storage with live progress and a real,
  working share link - it no longer just reads the file locally and invents a fake
  `m.sharex.link` URL.
- **Share / Copy Link** buttons open the real Android Share Sheet (or copy a real link),
  instead of copying a link that resolved to nothing.
- **Download** really saves the file to your device gallery via MediaStore.
- **Chat** is now backed by real Firestore documents with live listeners, including media
  messages that sync to the real Storage URL once the upload finishes (previously chat media
  only worked on the sender's own device).
- **Nearby Share** uses the real Google Nearby Connections API (Bluetooth/Wi-Fi Direct) for
  discovery and file transfer - the old screen showed four hardcoded fake devices.
- **QR pairing** really opens the camera and scans a QR code (via ZXing), then looks up the
  scanned user in Firestore and opens a real chat - previously "Switch to Scanner" was a toast
  that did nothing.
- **Admin panel** shows real user counts/storage from Firestore, real block/unblock that
  persists, and a real broadcast that every signed-in device receives live (previously all of
  this was hardcoded numbers and a no-op button).
- **Settings** toggles are now actually saved and used (default upload quality, Wi-Fi-only
  auto-load, and a Clear Cache button that really clears Glide's disk cache).
- Fixed a build-breaking bug: the release/debug signing configs pointed at keystore files that
  don't exist in this repo, which would have made `./gradlew assemble...` fail immediately.
  Debug now uses Android's own auto-generated debug key, and release safely falls back to it
  until you add real signing secrets (see below).
- Removed every fake "sample" fallback (fake gallery items, fake online users, a fake
  BigBuckBunny video, a truncated fake SHA-256 checksum) so empty/real states show correctly.

## 1. Firebase setup (required)

Everything above needs a real Firebase project behind it.

1. Go to the [Firebase Console](https://console.firebase.google.com/) and create a project.
2. Add an Android app with package name `com.aistudio.msharex.hqshare` (matches
   `applicationId` in `app/build.gradle.kts`).
3. Download the generated **`google-services.json`** and place it at `app/google-services.json`.
4. In the Firebase Console, enable:
   - **Authentication** → Sign-in method → **Email/Password**, and optionally **Google**.
   - **Firestore Database** (start in production mode; see rules below).
   - **Storage** (start in production mode; see rules below).
5. Suggested starter Firestore rules (tighten further for production):

   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{userId} {
         allow read: if request.auth != null;
         allow write: if request.auth != null && request.auth.uid == userId;
       }
       match /transfers/{transferId} {
         allow read, write: if request.auth != null &&
           request.auth.uid in resource.data.participants;
         allow create: if request.auth != null;
       }
       match /chats/{chatId} {
         allow read, write: if request.auth != null &&
           request.auth.uid in resource.data.participants;
         allow create: if request.auth != null;
         match /messages/{messageId} {
           allow read, write: if request.auth != null;
         }
       }
       match /announcements/{id} {
         allow read: if request.auth != null;
         allow write: if request.auth != null; // tighten to admin-only via a custom claim
       }
     }
   }
   ```

6. Suggested starter Storage rules:

   ```
   rules_version = '2';
   service firebase.storage {
     match /b/{bucket}/o {
       match /transfers/{userId}/{fileName} {
         allow read: if request.auth != null;
         allow write: if request.auth != null && request.auth.uid == userId;
       }
     }
   }
   ```

7. **To make yourself an admin**: sign up in the app once, then in the Firestore Console open
   `users/<your-uid>` and change the `role` field from `"user"` to `"admin"`. The admin
   button appears on your profile once that's set.

### Google Sign-In (optional)

If you enabled Google as a sign-in provider in step 4, `google-services.json` will contain a
web client ID and the app will automatically enable the "Sign in with Google" button. If you
skip this, the button is automatically disabled at runtime instead of crashing the build.

## 2. Building the APK via GitHub

This repo includes `.github/workflows/android-build.yml`, which builds on every push and on
manual trigger (Actions tab → "Build M.SHAREX APK" → Run workflow):

- It builds both **debug** and **release** APKs and uploads them as workflow artifacts
  (Actions run → Artifacts section).
- The debug build always works out of the box.
- The release build is signed with the debug key by default (fine for installing/testing) -
  add these **repo secrets** once you're ready to sign it for real distribution:
  - `KEYSTORE_BASE64` - your `.jks` keystore, base64-encoded (`base64 -w0 your.jks`)
  - `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
- Remember to also commit your real `app/google-services.json` (or add it as a secret and
  restore it in a workflow step) so Firebase features work in the built APK - without it the
  app installs and runs, but sign-in/chat/transfers will fail since there's no backend to
  talk to.

## 3. Run locally in Android Studio

**Prerequisites:** [Android Studio](https://developer.android.com/studio)

1. Open Android Studio → **Open** → select this project's directory.
2. Let Android Studio sync/fix Gradle as needed.
3. Add `app/google-services.json` from your Firebase project (step 1 above).
4. (Optional) Create a `.env` file for `GEMINI_API_KEY` if you use any Gemini-powered
   features (see `.env.example`).
5. Run on an emulator or physical device. Nearby Share and the camera QR scanner need a
   physical device (or two) to actually test discovery/transfer.

## Known scope notes (being upfront, not fake)

- **Admin broadcast** reaches every device that currently has the app open (a live Firestore
  listener), not devices where the app is fully closed - a true "push to everyone, even
  closed apps" notification needs a Cloud Function triggering FCM, which isn't included here
  since it needs its own backend deployment.
- **Nearby Share** requires both devices to have Bluetooth/Wi-Fi enabled and be in range;
  it's fully real (Google Nearby Connections), not a cloud fallback.
- The admin storage/user totals are computed from the most recent 200 user documents for
  simplicity: real numbers, just capped for very large user bases.
