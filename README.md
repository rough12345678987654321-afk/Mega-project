# Sway — native Android mindful-pause app

This repository now contains a native Android implementation of the core interaction: choose one or more installed apps, enable Sway's accessibility service, and see a pause bridge when a chosen app opens. The bridge offers **Continue** (returns to the selected app) or **Not now** (returns to the home screen). It is a gentle intervention, not an app lock.

## Get an installable APK

A GitHub Actions workflow builds a debug APK for each push to the project branch. Once the **Build Sway Android app** workflow completes successfully:

1. Open the repository's **Actions** tab.
2. Open the latest **Build Sway Android app** run.
3. In **Artifacts**, download **`sway-debug-apk`** and extract it.
4. Transfer `app-debug.apk` to your Android phone, tap it, and approve installation if Android asks.
5. Open **Sway**, choose apps, and tap **Enable pause detection**.
6. Read the in-app Accessibility disclosure, continue to Android **Settings → Accessibility → Sway app pause**, and enable it.
7. Open one of the selected apps. Sway should show the breathing bridge; choose **Continue** or **Not now**.

Android may require you to explicitly allow installing an APK from the browser or file manager you used. Debug APKs are for testing, not Play Store distribution.

## Build locally

Open the `android-app` directory in Android Studio (Meerkat 2024.3.1 Patch 1 or newer) and let Gradle sync. The project uses JDK 17, Android SDK Platform 36, and Build Tools 36.0.0. From a machine with Android SDK and Gradle installed, you can also run:

```sh
cd android-app
gradle assembleDebug
```

The output is `android-app/app/build/outputs/apk/debug/app-debug.apk`.

## Privacy and platform behavior

- The user explicitly selects the app packages to monitor; the service updates its event filter to those selected packages.
- The Accessibility Service listens to window-state changes only. `canRetrieveWindowContent` is disabled: the service does not read view text or screen contents.
- App selection and consent are stored in local SharedPreferences. The app requests no Internet permission and includes no analytics SDK.
- Accessibility access is optional and can be switched off in Android Settings. Sway does not attempt to prevent users from disabling or uninstalling it.
- This is an early, unsigned-for-release debug build. The Android project has not yet been validated on a device or approved for Google Play. OEM behavior, Android accessibility-policy review, and supported-device testing remain to be completed.
