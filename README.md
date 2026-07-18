# Freelancing Document

A lightweight, personal Android app (Kotlin + Jetpack Compose, Material 3) that stores your
documents — subject, description, photos, videos — directly in **your own Google Drive**,
inside an auto-created `Freelancing Document` folder. Sign in with the same Google account on
any Android phone and everything syncs automatically.

This is a complete, buildable Android Studio project. It is **not** a runnable APK — Android
apps must be compiled with the Android SDK, which isn't available in this chat environment.
Follow the steps below in Android Studio (free, ~15 minutes one-time setup) to build and install it.

---

## 1. What's included

- Kotlin + Jetpack Compose UI, Material 3, light/dark theme (remembered via DataStore)
- Google Sign-In (`play-services-auth`) scoped to `drive.file` — the app can only see files
  *it* creates, never your whole Drive
- Google Drive v3 REST client for folder creation, upload, download, delete
- Room database as a local offline cache, synced in the background with WorkManager
- Full CRUD: create / edit / delete documents, search by subject, add/remove photos & videos
- Settings screen: theme toggle, Sync Now, Logout, About

```
Freelancing Document/          (created automatically in the user's Drive)
├── Documents/                 one JSON file per document (subject, description, dates)
├── Photos/                    uploaded photos
├── Videos/                    uploaded videos
├── Backup/                    reserved for future full-export backups
└── Settings/                  reserved for future cross-device settings sync
```

## 2. Google Cloud Console setup (required, one-time)

Google Sign-In + Drive access needs an OAuth client registered to your app.

1. Go to [console.cloud.google.com](https://console.cloud.google.com) and create a new project
   (e.g. "Freelancing Document").
2. **APIs & Services → Library** → enable the **Google Drive API**.
3. **APIs & Services → OAuth consent screen** → choose **External** (or Internal if you use
   Google Workspace) → fill in app name, your email, and add yourself as a test user. Since this
   is for personal use, it can stay in "Testing" mode indefinitely for your own account.
4. **APIs & Services → Credentials → Create Credentials → OAuth client ID**:
   - Create one of type **Android**: package name `com.personal.freelancingdocument`, and your
     debug/release SHA-1 fingerprint (get it by running, in the project root, on your machine:
     `./gradlew signingReport` — copy the SHA1 line under `debug`).
   - Create a second one of type **Web application** (no redirect URIs needed). This "Web client
     ID" is what enables server-side Drive access and is required even though you sign in on
     Android — Google's sign-in library needs it to issue the right token.
5. Copy the **Web client ID** and paste it into
   `app/src/main/res/values/strings.xml` → `default_web_client_id`.

   (No `google-services.json` or Firebase project is needed — Google Sign-In and the Drive
   REST API only need the OAuth client IDs from step 4.)

## 3. Build & run

1. Open the project folder in **Android Studio** (Hedgehog / 2023.1+ recommended).
2. Let Gradle sync (it will download dependencies — needs internet).
3. Set `default_web_client_id` as described above.
4. Run on a device or emulator with **Google Play Services** installed (required for Sign-In).
5. First launch: tap **Sign in with Google**, grant Drive permission → the app creates the
   `Freelancing Document` folder tree in your Drive automatically.

### Building without a computer (phone-only, free)

This repo includes `.github/workflows/build-debug-apk.yml`, a GitHub Actions workflow that
compiles a debug APK in the cloud and attaches it as a downloadable artifact — no Android SDK
needed on your device. See "Building the APK from your phone" below for the full walkthrough.

## 4. Project structure

```
app/src/main/java/com/personal/freelancingdocument/
├── MainActivity.kt                 Compose entry point, theme + nav host
├── FreelancingApp.kt                Application class, schedules periodic sync
├── auth/GoogleAuthManager.kt        Google Sign-In + Drive OAuth credential
├── data/
│   ├── model/Document.kt            Domain models (Document, MediaItem)
│   ├── local/                       Room entities, DAO, database
│   ├── drive/DriveServiceHelper.kt  Drive v3 REST calls (folders, upload/download/delete)
│   └── repository/DocumentRepository.kt   Single source of truth, Room ⇄ Drive sync logic
├── sync/SyncWorker.kt               WorkManager background + manual sync
├── ui/
│   ├── theme/                       Material 3 color scheme, light & dark
│   ├── viewmodel/                   AuthViewModel, DocumentViewModel
│   ├── screens/                     Login, DocumentList, DocumentEdit, Settings
│   └── navigation/NavGraph.kt       Compose Navigation graph
└── util/PreferencesManager.kt       DataStore: theme choice, signed-in account
```

## 5. Security notes

- Uses the **`drive.file`** OAuth scope — the narrowest Drive scope Google offers. The app can
  only read/write files and folders it created; it has zero access to the rest of your Drive.
- No custom backend, no third-party server ever sees your data — it goes straight from your
  phone to Google's servers over HTTPS.
- Local Room cache is excluded from Android's auto backup so a device-transfer backup can't leak
  cached data outside your control (Drive remains the real source of truth).
- Logging out clears the local cache and all app preferences.

## 6. Building the APK from your phone (no computer needed)

Compiling an Android app requires the Android SDK and Gradle, which don't fit on a phone
comfortably — so the trick is to push this code to GitHub and let **GitHub Actions** (free,
cloud-hosted) do the compiling for you. The included workflow
(`.github/workflows/build-debug-apk.yml`) already does this automatically on every push.

**What you need:** a free GitHub account, and the free **Termux** app (install from
[F-Droid](https://f-droid.org/packages/com.termux/) — not the outdated Play Store version).

1. **Install Termux** from F-Droid, open it, then run:
   ```
   pkg update && pkg install git -y
   termux-setup-storage
   ```
2. **Get this project onto your phone.** Download the ZIP from this chat to your phone's
   Downloads folder, then in Termux:
   ```
   cd ~/storage/downloads
   unzip FreelancingDocument-AndroidProject.zip
   cd FreelancingDocument
   ```
3. **Create a new empty repository** on github.com (via your phone's browser): tap **+ → New
   repository**, name it e.g. `freelancing-document`, keep it **Private** if you like, don't
   add a README — then create it.
4. **Create a GitHub Personal Access Token** (used as your Git password from Termux): on
   github.com go to **Settings → Developer settings → Personal access tokens → Tokens
   (classic) → Generate new token**, tick the `repo` scope, generate, and copy it somewhere safe.
5. **Push the code**, back in Termux:
   ```
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/YOUR_USERNAME/freelancing-document.git
   git push -u origin main
   ```
   When prompted for a password, paste the **token** from step 4 (not your GitHub password).
6. **Watch it build:** open your repo on github.com in your phone's browser → the **Actions**
   tab → you'll see "Build debug APK" running (takes a few minutes the first time).
7. **Download the APK:** once it finishes (green check), open that run → scroll to
   **Artifacts** → tap `freelancing-document-debug-apk` to download a zip containing
   `app-debug.apk`. Unzip it with any phone file manager, tap the APK, and allow "install from
   this source" if asked — that installs the app.

Every time you push a change, a fresh APK is built automatically — check the Actions tab for
the latest one. This produces a **debug build**: fine for personal use on your own phone, but
not signed for Play Store distribution.

## 7. Adding future file types (as noted in the spec)

The `MediaType` enum in `data/model/Document.kt` and the `Photos`/`Videos` Drive subfolders are
the extension points. To add PDFs, Word/Excel files, ZIPs, voice notes, tags, or favorites later:
add a new `MediaType` value (or a separate `Attachments` folder), extend `DriveServiceHelper`
picker MIME types in `DocumentEditScreen.kt`, and add a matching Room column/migration.
