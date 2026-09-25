# FastPix StreamGate - Android video capture & resumable upload demo app

[![Platform: Android](https://img.shields.io/badge/Platform-Android%2024%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Uploads SDK](https://img.shields.io/badge/SDK-android--uploads--sdk-5D09C7)](https://github.com/FastPix/android-uploads-sdk)
[![License: MIT](https://img.shields.io/github/license/FastPix/android-StreamGate)](https://github.com/FastPix/android-StreamGate/blob/main/LICENSE)

**StreamGate is a ready-to-run, open-source Android app that shows how to capture, record, preview, and upload video to FastPix.** It demonstrates a complete mobile media workflow: **camera capture** (CameraX), device-wide **screen recording** (MediaProjection), and **gallery import**, followed by **resumable, direct-to-cloud uploads** with the [FastPix Android Uploads SDK](https://github.com/FastPix/android-uploads-sdk) and local **preview** with the [FastPix Android Player SDK](https://github.com/FastPix/fastpix-android-player). When an upload finishes, StreamGate returns an instant, shareable playback link - no intermediary backend required.

Use it as a reference implementation for **video upload apps, creator and UGC tools, screen-recording and bug-capture tools, and mobile video onboarding flows**.

**Built with:** Kotlin · Jetpack Compose (Material 3) · Dagger Hilt · CameraX · Retrofit · the FastPix Android Uploads & Player SDKs · Android 7.0 (API 24) and later

📖 **Docs:** https://fastpix.com/docs &nbsp;·&nbsp; 🚀 **Free account:** https://dashboard.fastpix.com

---

## Start here

If you are running StreamGate for the first time, follow these steps in order:

1. [Check your Java version](#1-check-your-java-version)
2. [Install Android Studio and the Android SDK](#2-install-android-studio-and-the-android-sdk)
3. [Clone the repository](#3-clone-the-repository)
4. [Get your FastPix credentials](#4-get-your-fastpix-credentials)
5. [Create a GitHub access token](#5-create-a-github-access-token)
6. [Configure local.properties](#6-configure-localproperties)
7. [Build and run on a device](#7-build-and-run-on-a-device)
8. [Capture, upload, and verify](#8-capture-upload-and-verify)
9. [Understand how it works](#9-understand-how-it-works)

Steps 1 and 2 set up your machine; do them once. Do not skip the verification in step 8.

## What StreamGate does

- **Camera capture** - native video recording with `CameraX` (`camera-camera2`, `camera-video`, and more).
- **Screen recording** - device-wide capture with Android's `MediaProjection` API and a foreground service.
- **Gallery picker** - browse and select existing local video files with native Android intents.
- **Direct-to-cloud resumable uploads** - push large video files straight to FastPix with the Uploads SDK, no intermediary backend server.
- **Local playback / preview** - preview the selected or recorded video with the FastPix Android Player SDK before you share it.
- **Instant shareable link** - once the upload completes, StreamGate generates a shareable playback link.

## Before you begin

You will need:

- **A computer running macOS, Windows, or Linux** with **Android Studio** and **JDK 21** - set up in [step 1](#1-check-your-java-version) and [step 2](#2-install-android-studio-and-the-android-sdk).
- **A physical Android device** running API level 24 (Android 7.0) or higher, recommended for camera capture and screen recording. An emulator can run the app and upload from the gallery, but it cannot capture a real camera.
- **A FastPix account** - sign up on the [FastPix dashboard](https://dashboard.fastpix.com).
- **A GitHub account**, used to authenticate to GitHub Packages where the FastPix SDKs are published - set up in [step 5](#5-create-a-github-access-token).

---

## 1. Check your Java version

The Gradle build runs on **JDK 21** (pinned in the project's `gradle/gradle-daemon-jvm.properties`). If you plan to build from the command line, confirm you have it:

```bash
java -version
```

Output is similar to:

```
openjdk version "21.0.4" 2024-07-16
OpenJDK Runtime Environment (build 21.0.4+7)
OpenJDK 64-Bit Server VM (build 21.0.4+7, mixed mode)
```

If the command is not found, or the version is below 21, install JDK 21. If you build inside Android Studio ([step 7](#7-build-and-run-on-a-device)), Android Studio's bundled JDK is used, so a separate system JDK is optional. (The app itself compiles to Java 11 bytecode; JDK 21 is what runs the Gradle build.)

## 2. Install Android Studio and the Android SDK

The app builds with **Android Studio**, which bundles a compatible JDK and manages the Android SDK. If you do not have it, download it from [developer.android.com/studio](https://developer.android.com/studio). On first launch, install an **Android SDK with API level 24 or higher**.

You do not need to install Gradle separately - the project includes the Gradle wrapper - and it builds on macOS, Windows, or Linux.

## 3. Clone the repository

```bash
git clone https://github.com/FastPix/android-StreamGate.git
cd android-StreamGate
```

Then open the project in Android Studio and let it sync Gradle.

## 4. Get your FastPix credentials

StreamGate authenticates its uploads with a FastPix access token. To get one:

1. Log in to the [FastPix dashboard](https://dashboard.fastpix.com).
2. Open **Access Tokens** and create (or copy) a token. Note both the **Token ID** and the **Secret Key**.

You will paste these into `local.properties` in [step 6](#6-configure-localproperties); they are injected at compile time as `BuildConfig.FASTPIX_TOKEN_ID` and `BuildConfig.FASTPIX_SECRET_KEY` and never leave your machine. Learn more in [Activate your account](https://fastpix.com/docs/getting-started/activate-your-account).

## 5. Create a GitHub access token

The FastPix Android SDKs are published to **GitHub Packages**, which requires authentication even for public packages. So Gradle needs a GitHub Personal Access Token (PAT) to download the [android-uploads-sdk](https://github.com/FastPix/android-uploads-sdk) and [fastpix-android-player](https://github.com/FastPix/fastpix-android-player):

1. Go to **GitHub → Settings → Developer settings → Personal access tokens**.
2. Create a new token with the **`read:packages`** scope.
3. Copy the token and note your GitHub username.

## 6. Configure local.properties

Create or open the `local.properties` file in the project root directory and add your credentials:

```properties
# FastPix API Credentials
fastpix.tokenId=YOUR_FASTPIX_TOKEN_ID
fastpix.secretKey=YOUR_FASTPIX_SECRET_KEY

# GitHub Credentials for Maven package resolution
github.username=YOUR_GITHUB_USERNAME
github.token=YOUR_GITHUB_PERSONAL_ACCESS_TOKEN
```

Replace each `YOUR_…` placeholder with the values from [step 4](#4-get-your-fastpix-credentials) and [step 5](#5-create-a-github-access-token). `local.properties` is git-ignored, so your credentials are not committed.

## 7. Build and run on a device

### Option A: Using Android Studio

1. Open the project and let Android Studio sync Gradle (it resolves the FastPix SDKs from GitHub Packages using your token).
2. Connect a physical Android device (API 24+) with USB debugging enabled, or start an emulator.
3. Click the **Run** button (or press `Shift + F10`).
4. Grant the camera, microphone, and notification permissions when the app asks.

### Option B: Using the command line

With `local.properties` set up and a device connected:

```bash
# Ensure local.properties is set up as detailed above, then clean the project
./gradlew clean

# Build the debug APK
./gradlew assembleDebug

# Install on a connected emulator or physical device
./gradlew installDebug
```

## 8. Capture, upload, and verify

1. In the app, **capture** a video with the camera, **record** your screen, or **pick** a clip from the gallery.
2. **Preview** it, then start the **upload** to FastPix.
3. When the upload completes, StreamGate shows a **shareable playback link** - open it to confirm the video plays.
4. Confirm it landed in FastPix: on the [dashboard](https://dashboard.fastpix.com), the new media appears and reaches the `Ready` status once processing finishes.

If the upload fails or the SDKs will not resolve, see [Troubleshooting](#troubleshooting).

## 9. Understand how it works

StreamGate captures media on the device, uploads it straight to FastPix with resumable, direct-to-cloud uploads, and hands you back a shareable HLS playback link.

<Image alt="How StreamGate uploads video to FastPix: capture, record, or pick a video, preview it in the app, resumable upload to FastPix, FastPix hosts and encodes, then a shareable playback link." border={false} src="https://static.fastpix.com/android-streamgate-workflow.png" />

---

## Troubleshooting

### Gradle sync fails to find the FastPix SDKs

**Problem:** `Could not find io.fastpix:uploads` or `io.fastpix.player:android`

**Solution:**

1. Verify `github.username` and `github.token` in `local.properties`.
2. Ensure your GitHub PAT has the `read:packages` scope.
3. Run `./gradlew clean --refresh-dependencies`.

### Build fails with "BuildConfig constants not found"

**Problem:** `BuildConfig.FASTPIX_TOKEN_ID` is undefined

**Solution:** confirm `fastpix.tokenId` and `fastpix.secretKey` are set in `local.properties`, then clean and rebuild with `./gradlew clean assembleDebug`.

### Build fails with a Java or toolchain error

**Solution:** the Gradle build runs on **JDK 21** (see [step 1](#1-check-your-java-version)). Install JDK 21, or build inside Android Studio, which uses its bundled JDK.

### Camera or screen recording does not work

Use a **physical device** - an emulator cannot capture a real camera - and grant the camera, microphone, and notification permissions the app requests.

## FAQ

**What does StreamGate do?**
It is a runnable Android demo that captures, records, or picks a video and uploads it to FastPix with resumable direct-to-cloud uploads, then returns a shareable playback link. See [What StreamGate does](#what-streamgate-does).

**Which FastPix SDKs does it use?**
The [FastPix Android Uploads SDK](https://github.com/FastPix/android-uploads-sdk) for resumable uploads and the [FastPix Android Player SDK](https://github.com/FastPix/fastpix-android-player) for local preview.

**Why do I need a GitHub token to build it?**
The FastPix SDKs are hosted on GitHub Packages, which requires authentication. The token (with `read:packages`) only lets Gradle download the SDKs. See [step 5](#5-create-a-github-access-token).

**Where do I get my FastPix Token ID and Secret Key?**
From the **Access Tokens** section of the [FastPix dashboard](https://dashboard.fastpix.com). See [step 4](#4-get-your-fastpix-credentials).

**Can I run it on an emulator?**
The app runs and can upload from the gallery on an emulator, but camera capture and screen recording need a physical device.

**What is a resumable, direct-to-cloud upload?**
The app uploads large files to FastPix in chunks that resume after interruptions, sending them straight to FastPix without routing through your own backend server.

**Can I use this in production as-is?**
It is a reference/demo app (MIT licensed). Use it to learn the capture-and-upload flow, then load credentials securely (not from `local.properties`) for production.

**What Android version do I need?**
API level 24 (Android 7.0) or higher on the device, with Android Studio and JDK 21 on your machine.

## Related FastPix repositories

| What you want | Repository |
|---|---|
| The resumable upload SDK StreamGate uses | [android-uploads-sdk](https://github.com/FastPix/android-uploads-sdk) |
| The Android video player SDK it previews with | [fastpix-android-player](https://github.com/FastPix/fastpix-android-player) |
| Collect Android playback analytics (QoE) | [android-core-data-sdk](https://github.com/FastPix/android-core-data-sdk) |
| StreamGate on iOS | [iOS-StreamGate](https://github.com/FastPix/iOS-StreamGate) |
| StreamGate on Flutter | [flutter-StreamGate](https://github.com/FastPix/flutter-StreamGate) |
| Create and manage media from a backend | [node-sdk](https://github.com/FastPix/node-sdk) · [fastpix-python](https://github.com/FastPix/fastpix-python) |

Browse everything in the [FastPix organization](https://github.com/orgs/FastPix/repositories).

## Reference documentation

- **FastPix platform:** [fastpix.com](https://fastpix.com/)
- **FastPix docs:** [fastpix.com/docs](https://fastpix.com/docs)
- **Access token guide:** [Activate your account](https://fastpix.com/docs/getting-started/activate-your-account)
- **Direct upload API:** [Upload media from device](https://fastpix.com/docs/video-on-demand-api/input-video/direct-upload-video-media)
- **Uploads SDK:** [android-uploads-sdk](https://github.com/FastPix/android-uploads-sdk)
- **Player SDK:** [fastpix-android-player](https://github.com/FastPix/fastpix-android-player)

## License

StreamGate is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

## Support

For issues or questions:

1. Check the [Troubleshooting](#troubleshooting) section above.
2. Review the [FastPix documentation](https://fastpix.com/docs).
3. Open an issue on [GitHub Issues](https://github.com/FastPix/android-StreamGate/issues).
4. Contact FastPix support at [support@fastpix.com](mailto:support@fastpix.com).
