# StreamGate Android App Project

StreamGate is an open-source native Android application designed as a technical showcase for capturing, processing, and uploading video content directly to the cloud. Built purely for developers, it demonstrates how to handle complex mobile media workflows—such as local video selection, camera capture, background screen recording, and reliable direct-to-cloud resumable uploads using FastPix infrastructure.

Once an upload completes, StreamGate generates an instant, shareable playback link.

---                        

## Core Features & Architecture

StreamGate is built using a modern Android tech stack (100% Kotlin, Jetpack Compose, Dagger Hilt) and focuses on the following core capabilities:

1. **Camera Capture**: Integrates `CameraX` (`camera-camera2`, `camera-video`, etc.) for robust, native video recording directly within the app.
2. **Screen Recording**: Uses Android's `MediaProjection` API combined with Foreground Services to capture device-wide screen activity reliably.
3. **Gallery Picker**: Implements native Android intent workflows to browse and select existing local video files.
4. **Direct Cloud Uploading**: Utilizes the FastPix Resumable Uploads SDK to push large media files securely to the cloud without needing to route them through an intermediary backend server.
5. **Local Playback/Preview**: Integrates the FastPix Android Player SDK (`io.fastpix.player:android`) to preview the selected or recorded video locally before generating the final shareable link.

---

## Tech Stack & Dependencies

* **Language**: Kotlin
* **UI Framework**: Jetpack Compose (Material 3, `activity-compose`)
* **Dependency Injection**: Dagger Hilt (`hilt-android`, `hilt-navigation-compose`)
* **Camera**: CameraX (`camera-camera2`, `camera-video`, `camera-view`, `camera-lifecycle`, `camera-extensions`)
* **Networking**: Retrofit with Kotlinx Serialization (`converter-kotlinx-serialization`)
* **Media Loading**: Coil (`coil-compose`)
* **Local Storage**: Jetpack DataStore (`datastore-preferences`)
* **Build Constraints**: `minSdk = 24`, `targetSdk = 37`, `compileSdk = 37`, Java 11

---

## Setup & Build Instructions

Because StreamGate relies on FastPix's private GitHub Package Registry for its SDKs, and requires specific API keys to initialize uploads, you must configure your local environment before building.

### 1. Prerequisites
You will need:
* A **FastPix Account** (to retrieve your API Token ID and Secret Key).
* A **GitHub Personal Access Token (PAT)** with `read:packages` permissions. FastPix SDKs are hosted on GitHub Packages (`maven.pkg.github.com/FastPix/android-uploads-sdk`), and Gradle requires this token to download them.

### 2. Configure `local.properties`
Clone the repository and open the project in Android Studio. Before syncing Gradle, create or open the `local.properties` file in the root directory and add the following credentials:

```properties
# FastPix API Credentials
fastpix.tokenId=YOUR_FASTPIX_TOKEN_ID
fastpix.secretKey=YOUR_FASTPIX_SECRET_KEY

# GitHub Credentials for Maven package resolution
github.username=YOUR_GITHUB_USERNAME
github.token=YOUR_GITHUB_PERSONAL_ACCESS_TOKEN
```

*Note: The FastPix keys are injected into the app at compile-time via `buildConfigField` as `BuildConfig.FASTPIX_TOKEN_ID` and `BuildConfig.FASTPIX_SECRET_KEY`. Never commit this file to version control.*

### 3. Build & Run (Command Line)
If you prefer building from the terminal, use the following Gradle wrapper commands:

```bash
# Clone the repository
git clone https://github.com/your-username/StreamGate.git
cd StreamGate

# Ensure local.properties is set up as detailed above, then clean the project
./gradlew clean

# Build the debug APK
./gradlew assembleDebug

# Install on a connected emulator or physical device
./gradlew installDebug
```

Alternatively, just hit **"Run"** in Android Studio once the Gradle sync successfully resolves the FastPix dependencies.

---

## Important Reference Links

* FastPix Platform: [fastpix.com](https://fastpix.com/)
* FastPix Access Token Guide: [Activate Your Account](https://fastpix.com/docs/getting-started/activate-your-account)
* FastPix VOD Upload API Docs: [Direct Upload Video Media](https://fastpix.com/docs/video-on-demand-api/upload-and-import-videos/direct-upload-video-media)
* FastPix Uploads SDK Repo: [android-uploads-sdk](https://github.com/FastPix/android-uploads-sdk)
* FastPix Player SDK Repo: [fastpix-android-player](https://github.com/FastPix/fastpix-android-player)

## License
StreamGate is licensed under the MIT License.
