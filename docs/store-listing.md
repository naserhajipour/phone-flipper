# Store listing copy

Ready-to-paste metadata for Google Play, Aptoide or a direct-download page. Replace the URLs with the
repository links you use.

## Identity

| Field | Value |
|---|---|
| App name | Flipper One |
| Package | `com.phoneflipper.app` |
| Version | 1.0.0 (10) |
| Category | Tools |
| Content rating | Everyone (no user-generated content, no ads, no purchases) |
| Price | Free |
| Requires | Android 7.0 (API 24)+ |
| Size | ≈ 330 KB |

## Short description (80 chars)

**EN** — Offline hardware toolkit: sensors, FFT audio, NFC, IR, camera and radio bridge.

**FA** — جعبه‌ابزار آفلاین سخت‌افزار: حسگر، صدا، NFC، مادون قرمز، دوربین و پل رادیویی.

## Full description (EN)

Flipper One turns your phone into a field instrument — completely offline.

**Audit what your phone can actually do.** Twelve hardware checks cover NFC, infrared, Bluetooth,
USB host, camera, microphone and six sensor families. Every row is a real system query and is marked
Native, Needs bridge or Absent. Export the result as a readable report or structured JSON.

**Six labs, one interface**
• Sensor lab — magnetic field gauge with peak tracking, bubble level, compass, gyroscope, light,
  proximity and pressure.
• Audio lab — 4096-point FFT, log spectrum from 40 Hz to 20 kHz, dominant-frequency readout, dBFS
  level and waveform. Detects ultrasonic content.
• NFC reader — UID, technologies and decoded NDEF text or links, with local history.
• IR remote — NEC transmitter with presets and custom codes (on phones with an IR blaster).
• Lens finder — reflection sweep with torch, strobe and on-device detection.
• Bridge console — Bluetooth SPP terminal for ESP32/HC-05 radio modules with command presets.

**Private by design**
No internet permission. No accounts, no ads, no analytics, no tracking. Camera frames, audio and
sensor data are processed in memory and never stored. NFC history and settings stay in the app's
private storage and can be wiped at any time.

**Bilingual**
Full English and Persian interface with correct right-to-left layout.

**Built for tinkerers**
Drive an ESP32 + CC1101 bridge for sub-GHz experiments, record and replay your own IR remotes, and
export reproducible reports. The bundled field guide works with no network.

Use radio and infrared features only on equipment you own or are authorised to test.

## Full description (FA)

فلیپر وان گوشی شما را به یک ابزار میدانی کاملاً آفلاین تبدیل می‌کند.

**بررسی واقعی سخت‌افزار** — دوازده بررسی: NFC، مادون قرمز، بلوتوث، میزبان USB، دوربین، میکروفون و
شش خانوادهٔ حسگر. هر ردیف از خود سیستم پرسیده می‌شود و با «داخلی»، «نیازمند پل» یا «ناموجود» علامت
می‌خورد. گزارش متنی و JSON قابل خروجی گرفتن است.

**شش آزمایشگاه** — حسگر (میدان مغناطیسی، تراز، قطب‌نما، ژیروسکوپ، نور، مجاورت، فشار)، صدا
(FFT و طیف ۴۰ هرتز تا ۲۰ کیلوهرتز)، خوانندهٔ NFC، کنترل مادون قرمز، یابندهٔ لنز و کنسول پل بلوتوث.

**حریم خصوصی واقعی** — بدون مجوز اینترنت، بدون حساب کاربری، بدون تبلیغ و ردیابی. تصویر، صدا و
دادهٔ حسگرها فقط در حافظه پردازش می‌شوند.

**دوزبانه** — رابط کامل انگلیسی و فارسی با چیدمان راست‌به‌چپ.

از ویژگی‌های رادیویی و مادون قرمز تنها روی تجهیزات خودتان یا با اجازهٔ آزمایش استفاده کنید.

## What's new (1.0.0)

* Complete rebuild with a unified design system and custom data visualisation.
* Twelve-capability audit with text and JSON export.
* Six labs including audio FFT, NFC reader and camera reflection sweep.
* Full Persian localisation with RTL layout.
* Offline by construction: no INTERNET permission, no SDKs.
* Signed, reproducible builds published automatically.

## Graphics checklist

| Asset | Spec | Status |
|---|---|---|
| App icon | 512 × 512 PNG, 32-bit | `docs/assets/icon-preview.png` (scale to 512 from `mipmap-xxxhdpi`) |
| Feature graphic | 1024 × 500 PNG | to capture from a device |
| Phone screenshots | 1080 × 1920, 4–8 images | Dashboard, report, sensor lab, audio lab, NFC, bridge |
| 7-inch tablet | optional | layout is fluid, one column |

## Compliance notes

* **Permissions declaration**: camera (lens finder), microphone (audio lab), NFC (reader),
  Bluetooth (bridge), location on API ≤ 30 for Bluetooth discovery only, vibration.
* **Data safety form**: no data collected, no data shared, no data transmitted.
* **Radio features**: the app never transmits sub-GHz by itself; it drives external hardware the
  user owns. Include the disclaimer from `docs/privacy.md` in the listing.
