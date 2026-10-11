# Minimal environment for building the app in Android Studio

This project is a prototype. To build it in Android Studio:

1. Open Android Studio
2. Select "Open an existing project"
3. Choose the repo root
4. Let Gradle sync
5. Build the app on a connected Android device or emulator

Notes
- The app uses AndroidX and standard Android platform APIs.
- For certain permissions (Bluetooth/USB), runtime requests may be needed based on Android version.
- Device detection is intentionally safe and non-invasive.
