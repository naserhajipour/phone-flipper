# Release notes — Flipper One 1.0.0

First production release of the rebuilt application.

## Highlights

- **Complete rewrite of the Android app** in dependency-free Java with a single design system.
- **Twelve-capability audit** with native / bridge / absent classification and exportable text +
  JSON reports.
- **Six labs**: sensor, audio (FFT), NFC, infrared, lens finder and Bluetooth bridge console.
- **Bilingual interface** (English / فارسی) with per-app locale, full RTL support and Persian-aware
  typography.
- **Offline by construction**: no `INTERNET` permission, no analytics, no accounts, `allowBackup`
  disabled.
- **Signed release pipeline**: SHA-pinned GitHub Actions workflow, headless build script, rolling
  `apk-latest` release.
- New adaptive launcher icon generated from code.

## Technical

- `minSdk 24`, `targetSdk 35`, `versionCode 10`, ~310 KB APK, no runtime dependencies.
- Custom canvas widgets: gauge, ring, spectrum, waveform, compass, bubble level, sparkline, scan HUD.
- Radar-free camera analysis on YUV luma with adaptive thresholding and preview-matrix mapping.
- Bluetooth SPP console with Android 12+ permission handling and command presets.

## Removed

- OpenAI/AI assistant and the API-key storage that depended on `androidx.security`.
- The placeholder `payment/` server, the 15-byte dummy `flipper one.apk` and the chat/prompt
  activities that were not registered in the manifest.

## Upgrade notes

The package id (`com.phoneflipper.app`) is unchanged but the signing key is the repository keystore,
so install this build after uninstalling any earlier prototype.
