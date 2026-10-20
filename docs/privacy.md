# Privacy

Flipper One is designed so that its privacy claims can be verified with standard Android tooling.

## Verify it yourself

```bash
# Any INTERNET (or other network) permission would appear here — there is none.
aapt2 dump badging FlipperOne.apk | grep permission

# No services, no receivers exported to the network, one read-only provider.
aapt2 dump xmltree --file AndroidManifest.xml FlipperOne.apk | grep -E "provider|service|receiver"

# No third-party dependencies.
./gradlew :app:dependencies
```

## What is stored and where

| Data | Location | Lifetime |
|---|---|---|
| Language, units, toggles | private `SharedPreferences` (`flipper_prefs`) | until you reset it |
| NFC scan history (max 40 entries: UID, technology, payload) | private `SharedPreferences` | until cleared in Settings |
| Export reports | app `cacheDir/reports` | until the system clears the cache |
| Camera frames, audio buffers, sensor values | RAM only | discarded after each analysis |
| Bridge console log | RAM only | cleared when the screen closes |

No database, no files in shared storage, no external storage permission.

## What is never collected

* No accounts, e-mail addresses or device identifiers.
* No analytics, advertising or attribution SDK.
* No crash reporting.
* No background services, jobs or alarms.
* No clipboard reads. The app only *writes* to the clipboard when you tap Copy.

## Permissions and why

| Permission | Used by | Notes |
|---|---|---|
| `CAMERA` | Lens finder | Requested when the tool opens; frames are not saved. |
| `RECORD_AUDIO` | Audio lab | Requested on first capture; buffers stay in RAM. |
| `NFC` | NFC reader | Read-only reader mode; no card emulation. |
| `BLUETOOTH_CONNECT` / `BLUETOOTH_SCAN` | Bridge console | Android 12+; asked on first use. |
| `BLUETOOTH` / `BLUETOOTH_ADMIN` | Bridge console | Android ≤ 11 legacy permissions. |
| `ACCESS_FINE_LOCATION` | Bridge console | Only on Android ≤ 11, where the platform requires it to discover Bluetooth devices. `neverForLocation` is set on `BLUETOOTH_SCAN` for Android 12+. |
| `VIBRATE` | UI feedback | Optional haptics. |

## Backups

`android:allowBackup="false"` keeps private state out of cloud backups and device-to-device transfer.

## Sharing

Reports leave the app only through `ACTION_SEND` with a one-shot URI grant scoped to a single file in
the app cache. The provider is not exported and supports read-only access.

## Data deletion

`Settings → Local data` offers "Clear NFC history" and "Reset all preferences". Uninstalling the app
removes everything else.
