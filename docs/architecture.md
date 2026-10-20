# Architecture

Flipper One is a single Android application with no server side and no network layer. Everything it
does is a conversation between the app, the phone's hardware and — when the phone lacks a radio —
an external bridge over Bluetooth SPP.

## Layers

| Layer | Package | Responsibility |
|---|---|---|
| Screens | `com.phoneflipper.app.ui` | One Activity per tool. Screens only compose views and react to user input. |
| Chrome & design system | `com.phoneflipper.app.core` | `BaseActivity` (window, insets, top bar), `Ui` (spacing, type scale, cards, buttons, badges), `IconDrawable` (vector icon set drawn on Canvas). |
| Visualisation | `com.phoneflipper.app.view` | `GaugeView`, `RingView`, `SpectrumView`, `WaveView`, `CompassView`, `LevelView`, `SparkChart`, `MeterView`, `ScanOverlay`, `LogoView`. Pure `View` subclasses with no dependencies. |
| Domain | `core/Capabilities`, `core/Fft`, `core/ReportBuilder`, `core/Prefs` | Hardware probing, signal processing, report serialisation and local state. |
| Platform glue | `FlipperApp`, `core/ReportProvider`, `core/LocaleManager` | Locale wrapping, a read-only content provider for exported reports, notification-free lifecycle. |

The app deliberately contains **no third-party libraries**. That keeps the APK small, removes supply
chain risk and makes the privacy claims easy to audit (`./gradlew dependencies` prints nothing).

## Data flow

```
User action ──► Activity ──► platform API (SensorManager, Camera2, NfcAdapter, …)
                    │                    │
                    │                    └── callback / frame / tag
                    ▼
              view/*.set…()  ──► Canvas draw ──► screen
                    │
                    └──► Prefs (private SharedPreferences) or ReportBuilder → cacheDir → share sheet
```

Nothing leaves the process unless the user explicitly exports a report.

## Hardware probing

`Capabilities.scan(Context)` returns an immutable list of items:

```java
new Capabilities.Item(id, titleRes, descRes, icon, category, status, detail)
```

`status` is one of `NATIVE`, `BRIDGE`, `ABSENT`. Sources:

| Item | Source |
|---|---|
| NFC / HCE | `NfcAdapter.getDefaultAdapter`, `FEATURE_NFC`, `FEATURE_NFC_HOST_CARD_EMULATION` |
| Infrared | `ConsumerIrManager.hasIrEmitter()` + carrier ranges |
| Bluetooth | `BluetoothManager.getAdapter()` + `FEATURE_BLUETOOTH_LE` |
| Sub-GHz | Always `BRIDGE`: no Android phone exposes a transmit API |
| USB host | `FEATURE_USB_HOST` and a `UsbManager` service |
| Camera | `FEATURE_CAMERA_ANY`, flash feature, `CameraManager.getCameraIdList()` |
| Microphone | `FEATURE_MICROPHONE` |
| Sensors | `SensorManager.getDefaultSensor(TYPE_…)` with name, power and vendor |
| Offline core | Constant `NATIVE`: the manifest has no `INTERNET` permission |

## Signal processing

* **Audio**: 44.1 kHz mono PCM into a 4096-sample ring buffer → mean removal → Hann window →
  radix-2 FFT → magnitude spectrum → log-frequency bars, peak interpolation and dBFS RMS.
* **Camera**: YUV_420_888 luma plane sampled on a 64×48 grid → mean/σ statistics → adaptive
  threshold → 4-neighbour local maxima → strongest 12 spots mapped through the preview matrix into
  view coordinates.
* **Sensors**: exponential smoothing for EMF, low-pass gravity for the bubble level, rotation-vector
  heading with an accelerometer + magnetometer fallback.

## Lifecycle rules

* Sensors register in `onResume` and unregister in `onPause`; the sensor lab also exposes an explicit
  start/stop control.
* The camera is opened only when the surface is available and closed in `onPause`.
* The audio worker thread is stopped and joined before a new capture starts.
* Bluetooth sockets are closed in `onDestroy`, discovery is cancelled and the broadcast receiver is
  unregistered.

## Offline guarantees

1. No `INTERNET` permission in the manifest — the platform blocks any socket.
2. `android:usesCleartextTraffic="false"` as a second barrier.
3. `android:allowBackup="false"` so private state is not copied to a cloud backup.
4. CI fails if `aapt2 dump badging` ever reports `android.permission.INTERNET`.
