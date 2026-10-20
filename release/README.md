# Flipper One — release APK

| | |
|---|---|
| File | `FlipperOne-1.0.0.apk` |
| Package | `com.phoneflipper.app` |
| Version | 1.0.0 (versionCode 10) |
| Size | 340K (344140 bytes) |
| SHA-256 | `8b8091bed72a0caca2d4586c40742c7c8689e8059485e23a9fdfeb5fbb0d109d` |
| Signing | APK Signature Scheme v2 + v3, RSA 4096, certificate SHA-256 `9d66111220d04f19cc3ac0ff67baba460a41e7a839b10eb6f6c0c9102e732cb4` |
| Requires | Android 7.0 (API 24) or newer |
| Network | none — the manifest declares no `INTERNET` permission |

## Install

1. Copy the APK to the phone (USB, cloud drive, or download it from the GitHub release).
2. Open it in the files app and allow "install unknown apps" for that source when prompted.
3. The package is signed with a stable demo key committed to this repository, so future builds
   update in place. Replace `keystore/flipperone-release.jks` with your own key before publishing
   to a store.

## Verify the download

```bash
sha256sum FlipperOne-1.0.0.apk
apksigner verify --print-certs FlipperOne-1.0.0.apk
```

The SHA-256 above matches the copy in this folder. Builds from GitHub Actions are rebuilt from the
same sources and signed with the same key, so the certificate fingerprint always matches even when
the byte-level hash differs (the compiler records timestamps).

## First run

The app shows a one-time safety notice. NFC, camera, microphone and Bluetooth permissions are
requested only when you open the matching lab. Everything works without a network connection.
