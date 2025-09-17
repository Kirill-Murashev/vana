# Vana Inspection

Mobile application for property inspection appraisers. The app captures photos directly inside the workflow, applies mandatory watermarks and location metadata, and organises captured evidence for quick distribution.

## Highlights
- Structured project session with project, client, valuer, and company details.
- In-app camera powered by CameraX; prevents importing external photos.
- Automatic watermark overlay with timestamp (`YYYY-MM-DD HH:MM:SS`), coordinates, altitude, project, appraiser, and optional compass bearing.
- EXIF metadata enrichment for date/time, GPS, altitude, image description, and project details.
- Configurable auto-upload options: manual archive, Google Drive staging folder, or corporate server staging folder (local copy simulation for now).
- Preferences for Wi-Fi-only uploads, keeping a local copy, and optional compass data in overlays.
- Recent photo gallery with upload status indicators and manual retry/delete actions.

## Project structure
```
.
├── app
│   ├── build.gradle.kts          # Android application module build config
│   ├── src/main
│   │   ├── AndroidManifest.xml   # Manifest with permissions and launcher activity
│   │   ├── java/com/vana/inspection
│   │   │   ├── AppViewModel.kt   # Session state, capture orchestration, uploads
│   │   │   ├── MainActivity.kt   # Compose navigation scaffold
│   │   │   ├── capture/          # Photo processing & upload helpers
│   │   │   ├── data/             # Models and DataStore-backed preferences
│   │   │   ├── location/         # Location abstraction
│   │   │   ├── network/          # Network status helper
│   │   │   └── ui/               # Compose screens and theme
│   │   └── res/                  # Strings, themes, adaptive launcher icon
├── build.gradle.kts
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
├── gradlew / gradlew.bat         # Gradle wrapper scripts (self-bootstrapping jar download)
└── scripts/bootstrap-gradle-wrapper.sh
```

## Getting started
1. **Bootstrap the Gradle wrapper (first run only).**
   ```bash
   ./scripts/bootstrap-gradle-wrapper.sh
   ```
   The wrapper scripts download `gradle-wrapper.jar` automatically if it is missing. If your environment restricts outgoing network access, download the jar manually from `https://repo.gradle.org/gradle/libs-releases-local/org/gradle/gradle-wrapper/8.7/gradle-wrapper-8.7.jar` and place it in `gradle/wrapper/`.

2. **Open in Android Studio (Giraffe or newer).**
   - File → Open → choose the project root.
   - Let Gradle sync the project; install the recommended SDK platforms (compileSdk 34, minSdk 26).

3. **Run on a device or emulator.**
   - Ensure the device/emulator has Google Play Services for location.
   - Grant camera and precise location permissions when prompted.
   - Use the *Project* tab to populate project/client/appraiser/company names before switching to the *Capture* tab.

4. **Build via CLI (optional).**
   ```bash
   ./gradlew assembleDebug
   ```
   The generated APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Upload workflow
- **Manual archive** – captured files remain in the app's picture directory. The gallery shows `Manual` status.
- **Google Drive** – files are copied to `files/uploads/google-drive/` inside app storage as a staging area. Replace `PhotoUploader` with Google Drive SDK calls to push to your corporate Drive.
- **Corporate server** – files are copied to `files/uploads/corporate-server/`. This represents an integration point for SFTP/REST uploads.
- Auto uploads respect the Wi-Fi-only toggle and fall back to `Scheduled` status until Wi-Fi is available. You can trigger a manual retry from the gallery card.

## Extending the app
- Replace `PhotoUploader` with WorkManager-backed tasks that call your real endpoints or cloud storage API.
- Hook into the `UploadTarget` enum to add more destinations (e.g., Azure Blob, Amazon S3).
- Persist captured session summaries to Room if you need offline reporting.
- Integrate biometric/App PIN requirements before entering the Capture tab for added compliance.

## Requirements
- Android Studio Giraffe (AGP 8.2.2, Kotlin 1.9.22).
- Android SDK Platform 34 and build tools.
- Android device/emulator running API 26+ with Google Play services.

## License
This project inherits the repository's license (see `LICENSE`).
