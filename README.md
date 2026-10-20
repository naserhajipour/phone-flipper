# Flipper One

**A field toolkit for Android that works with no network, no account and no companion app.**

<img src="docs/assets/icon-preview.png" width="120" align="right" alt="Flipper One icon" />

Flipper One turns an ordinary Android phone (API 24+) into an instrument for inspecting hardware,
reading sensors and talking to an external radio bridge. The whole product is one self-contained
APK: 12 hardware checks, six labs, a bilingual interface (English / فارسی) and a signed release
pipeline.

---

## Why it is different

| | |
|---|---|
| **Offline by construction** | The manifest has **no `INTERNET` permission**. There is no analytics SDK, no ads, no account, no crash reporter and no background service. This is verifiable in `aapt2 dump badging`. |
| **No third-party runtime dependencies** | The UI, charts, icons and FFT are written directly on the Android framework, so the APK stays around 300 KB and cannot be broken by a library upgrade. |
| **Real measurements, honest labels** | Every capability row is a `PackageManager` / `SensorManager` / `CameraManager` query, not a guess. Features that phones cannot do (Sub-GHz transmit) are labelled **Bridge**, never faked. |
| **Bilingual and RTL-correct** | Full English and Persian catalogues, per-app locale, mirrored layouts and Persian-aware typography (no letter tracking that damages joining). |
| **Reproducible signed builds** | GitHub Actions builds and signs a release APK on every push using SHA-pinned actions, then publishes the rolling `apk-latest` release. |

---

## Download

* **This release** – [**v1.0.0**](../../releases/tag/v1.0.0) carries the signed `FlipperOne.apk` and the full notes.
* **Always current** – [**apk-latest**](../../releases/tag/apk-latest) is refreshed by every green build.
* **In the repository** – [`release/FlipperOne-1.0.0.apk`](release/FlipperOne-1.0.0.apk) with its
  [checksum and install notes](release/README.md).
* **Per-commit builds** – every workflow run stores `FlipperOne-<version>-release.apk` as an artifact.

Requirements: Android 7.0 (API 24) or newer. No Google services required. All hardware features are
declared optional, so the app installs on devices without NFC, IR or a camera.

---

## What it does

### Capability audit
Twelve checks across radio, sensors, optics and I/O. Each row reports **Native**, **Needs bridge** or
**Absent**, with the concrete system detail (carrier frequencies, camera count, sensor vendor, ABIs,
security patch level, …). The report is exportable as text or JSON through the Android share sheet.

### Six labs

| Tool | Works on | What it does |
|---|---|---|
| **Sensor lab** | every phone | Magnetometer gauge with peak tracking and history, accelerometer magnitude + bubble level, compass with heading fallback, gyroscope, light (lux), proximity and pressure. |
| **Audio lab** | every phone | 44.1 kHz capture, 4096-point Hann-windowed FFT, log-frequency spectrum, dominant-frequency gauge, dBFS level meter and waveform. Flags ultrasonic content above 16 kHz. |
| **NFC reader** | NFC phones | Reader mode for A/B/F/V and barcode, UID, technology list, NDEF text/URI/MIME decoding and a local scan history. |
| **IR remote** | IR blasters | NEC frame transmitter at 38 kHz with six demo codes, an arbitrary 32-bit code field and a transmit log. |
| **Lens finder** | camera phones | Camera2 preview with torch, strobe, sensitivity slider and on-device luma peak detection drawn as a HUD. |
| **Bridge console** | Bluetooth phones | Bluetooth Classic SPP terminal for ESP32 / HC-05 with paired-device discovery and command presets (`STATUS`, `SET_FREQUENCY`, `SEND_RAW`, `RECORD_IR_START`, `PLAY_IR`). |

### Field guide
Six offline briefings (quick start, radio bridge, safety and law, NFC, magnetic fields, privacy)
shipped inside the APK, in English and Persian.

### Settings
Per-app language, µT/mG units, keep-screen-on, haptics, local-data reset and a transparent
"what we store" summary.

---

## Architecture

```
┌──────────────────────────── Flipper One APK ────────────────────────────┐
│  ui/         Dashboard · Capabilities · SensorLab · AudioLab            │
│              Nfc · Ir · LensFinder · Bridge · Guides · Settings         │
│  view/       Gauge · Ring · Spectrum · Waveform · Compass · Level       │
│              Spark chart · Scan overlay · Logo                          │
│  core/       BaseActivity · Ui design system · IconDrawable             │
│              Capabilities · Fft · Prefs · ReportBuilder · Provider      │
└───────────────┬──────────────────────────────────────────────────────────┘
                │ Android framework only (no SDKs, no network)
┌───────────────▼───────────────┐        ┌────────────────────────────────┐
│ Phone hardware                │        │ External bridge (optional)     │
│ NFC · IR · Camera · Mic       │  SPP   │ ESP32 / HC-05 + CC1101 / IR LED│
│ Sensors · Bluetooth           │◄──────►│ 315/433/868/915 MHz transmit   │
└───────────────────────────────┘        └────────────────────────────────┘
```

Phones cannot transmit Sub-GHz radio; the app is explicit about that and provides the console and
protocol to drive hardware that can. See [`docs/architecture.md`](docs/architecture.md) and
[`docs/hardware-guide.md`](docs/hardware-guide.md).

---

## Privacy

* No `INTERNET` permission, therefore no uploads, telemetry or remote configuration.
* Reports are written to the app cache only when you export them.
* Camera frames and audio buffers exist in RAM and are discarded after analysis.
* NFC history and preferences live in private app storage and can be wiped from Settings.

Details: [`docs/privacy.md`](docs/privacy.md).

---

## Build

### Android Studio / Gradle (recommended for development)

```bash
./gradlew assembleRelease      # signed if keystore/flipperone-release.jks is present
./gradlew assembleDebug
```

Requirements: JDK 17, Android SDK 35. The project uses AGP 8.7.3 and the Gradle 8.9 wrapper.

### Headless script (no Gradle, no Android Studio)

CI uses this path because it needs nothing but a JDK and the Android SDK tools:

```bash
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk          # provides aapt2, d8, apksigner, android.jar
./scripts/build_apk.sh release
# -> build/outputs/FlipperOne-1.0.0-release.apk
```

The script compiles resources with `aapt2`, compiles Java, dexes with `d8`, packages, zipaligns
(4-byte aligned, `resources.arsc` and `classes.dex` stored uncompressed) and signs with the
repository keystore. See [`docs/building.md`](docs/building.md).

Release signing uses `keystore/flipperone-release.jks` (alias `flipperone`, password
`flipperone2026`). **Replace it with your own keystore before publishing to a store** and pass the
secrets through `FLIPPER_KEYSTORE_PASSWORD`, `FLIPPER_KEY_ALIAS` and `FLIPPER_KEY_PASSWORD`.

---

## Continuous integration

`.github/workflows/build-apk.yml` runs on every push:

1. checkout and JDK 17 – both actions pinned to full commit SHAs (repository policy);
2. install `platforms;android-35` and `build-tools;35.0.0`;
3. build the signed release APK with `scripts/build_apk.sh`;
4. verify the signature and **fail the build if an `INTERNET` permission ever appears**;
5. upload the APK as an artifact and refresh the rolling `apk-latest` release.

---

## Repository layout

```
app/                     Android module (Java, zero runtime dependencies)
  src/main/java/.../core   design system, capabilities, prefs, reports
  src/main/java/.../ui     screens
  src/main/java/.../view   custom canvases (gauges, charts, overlays)
  src/main/assets/guides   bundled EN/FA field guide
  src/main/res             strings (en, fa), icons, themes
esp32-firmware/          ESP32 / Arduino bridge examples and SPP protocol
docs/                    architecture, hardware, privacy, build, testing
scripts/                 offline APK builder, zipalign, icon generator
keystore/                release signing material (demo key)
```

---

## Hardware notes

* **Sub-GHz**: ESP32 + CC1101, driven over Bluetooth SPP with the line protocol in
  [`docs/hardware-guide.md`](docs/hardware-guide.md).
* **IR on phones without an emitter**: use the bridge with an IR LED and transistor driver
  ([`docs/ir_rf_recording.md`](docs/ir_rf_recording.md)).
* **Bluetooth SPP**: Android 12+ needs the *Nearby devices* permission; the console requests it on
  first use.

---

## Testing

A manual acceptance checklist lives in [`docs/testing.md`](docs/testing.md) (capability audit,
sensors, audio, NFC, IR, camera, bridge, export, RTL and rotation).

---

## فارسی

**فلیپر وان** یک جعبه‌ابزار میدانی برای اندروید است که کاملاً آفلاین کار می‌کند: بدون اینترنت،
بدون حساب کاربری و بدون برنامهٔ جانبی.

- **۱۲ بررسی سخت‌افزاری**: NFC، مادون قرمز، بلوتوث، USB، دوربین، میکروفون، شتاب‌سنج، ژیروسکوپ،
  مغناطیس‌سنج، نور، مجاورت و فشارسنج — با برچسب «داخلی»، «نیازمند پل» یا «ناموجود».
- **شش آزمایشگاه**: حسگر، صدا (FFT)، خوانندهٔ NFC، کنترل مادون قرمز، یابندهٔ لنز و کنسول پل.
- **رابط دوزبانه** با چیدمان راست‌به‌چپ درست و تایپوگرافی فارسی.
- **حریم خصوصی قابل راستی‌آزمایی**: برنامه مجوز اینترنت ندارد؛ تصویر، صدا و دادهٔ حسگرها فقط در
  حافظه پردازش و دور ریخته می‌شوند.
- **APK امضاشده** در هر ارسال به‌صورت خودکار ساخته می‌شود و از بخش Releases قابل دریافت است.

ساخت محلی:

```bash
./gradlew assembleRelease
# یا بدون Gradle
ANDROID_HOME=/path/to/sdk ./scripts/build_apk.sh release
```

---

## Disclaimer

Flipper One is a diagnostic toolkit. Radio and infrared transmission is regulated: use it only on
equipment you own or are authorised to test, and follow local law. EMF, reflection and ultrasonic
readings are indicators, not certified measurements.
