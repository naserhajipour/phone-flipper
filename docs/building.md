# Building Flipper One

Two build paths are supported. Both produce the same signed, zipaligned APK.

## 1. Gradle / Android Studio

Requirements: JDK 17, Android SDK Platform 35.

```bash
./gradlew assembleRelease        # app/build/outputs/apk/release/app-release.apk
./gradlew assembleDebug          # applicationId com.phoneflipper.app.debug
./gradlew lint                   # lint fails the build on errors
```

Release signing is configured in `app/build.gradle` and reads
`keystore/flipperone-release.jks`. Override the credentials with environment variables:

```bash
export FLIPPER_KEYSTORE_PASSWORD=…
export FLIPPER_KEY_ALIAS=…
export FLIPPER_KEY_PASSWORD=…
```

If the keystore is missing, the release variant is built unsigned, which is useful for CI forks.

### Toolchain

| Component | Version |
|---|---|
| Gradle | 8.9 (wrapper) |
| Android Gradle Plugin | 8.7.3 |
| compileSdk / targetSdk | 35 |
| minSdk | 24 |
| Java | 11 source level, JDK 17 toolchain |

## 2. Headless script (what CI uses)

`scripts/build_apk.sh` needs only a JDK and the Android SDK build tools. It is the fastest way to
produce a release APK on a machine without Android Studio, and it is what the GitHub Actions workflow
executes.

```bash
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
./scripts/build_apk.sh release
```

The script resolves `aapt2`, `d8`, `apksigner` and `android.jar` from `$ANDROID_HOME`. You can also
point at explicit binaries:

| Variable | Meaning |
|---|---|
| `AAPT2` | path to `aapt2` |
| `ANDROID_JAR` | path to `platforms/android-35/android.jar` |
| `D8_JAR` | path to `build-tools/*/lib/d8.jar` |
| `APKSIGNER_JAR` | path to `build-tools/*/lib/apksigner.jar` |
| `ECJ_JAR` | optional; uses Eclipse ECJ instead of `javac` |
| `KEYSTORE`, `KEYSTORE_PASS`, `KEY_ALIAS`, `KEY_PASS` | signing material |

Pipeline:

1. `aapt2 compile` the resource tree into a flat zip.
2. `aapt2 link` produces `base.apk`, `R.java`, and merges assets (the bundled guides).
3. `javac` (or ECJ) compiles `app/src/main/java` against `android.jar`.
4. `d8` converts the class files into `classes.dex`.
5. `scripts/package_apk.py` merges the dex, keeping `resources.arsc` and `classes.dex` uncompressed.
6. `scripts/zipalign.py` rewrites the archive so stored entries start on 4-byte boundaries
   (4096 for shared libraries).
7. `apksigner` signs (v1 + v2 + v3) with the repository keystore.

Output: `build/outputs/FlipperOne-<version>-<type>.apk`.

## Icons

Launcher icons are generated, not hand-drawn:

```bash
python3 scripts/make_icons.py       # requires Pillow
```

This writes every density, the adaptive-icon foreground and a documentation preview, matching the
in-app `LogoView` mark.

## CI

`.github/workflows/build-apk.yml`:

* pinned `actions/checkout`, `actions/setup-java` and `actions/upload-artifact` commits
  (the repository policy requires full SHA pins);
* installs SDK platform 35 and build-tools 35.0.0;
* runs `scripts/build_apk.sh release`;
* verifies the signature and fails if `android.permission.INTERNET` appears;
* uploads the artifact and refreshes the rolling `apk-latest` release.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `SDK location not found` | Set `ANDROID_HOME` or create `local.properties` with `sdk.dir=…`. |
| `aapt2: error: <manifest> must have a 'package' attribute` | You are running a very old aapt2; the script injects `package` automatically, pass `--manifest build/manual/AndroidManifest.xml`. |
| `java: error: release version 8 not supported` | Use JDK 17 for the script (JDK 21+ dropped `-source 8` for javac; ECJ still works). |
| Bluetooth list is empty on Android 12+ | Grant *Nearby devices* when the console asks for it. |
| NFC reader does nothing | Enable NFC in system settings; the screen shows a shortcut. |
