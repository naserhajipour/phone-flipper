# Acceptance checklist

Run this on a real device before shipping. Each line is pass/fail; record the Android version.

## Install & first run

- [ ] APK installs on API 24 and on the newest Android available to you.
- [ ] The launcher icon renders on API 26+ (adaptive) and on API 24–25 (legacy PNG).
- [ ] Long-pressing the icon shows the three shortcuts and each opens the right screen.
- [ ] The safety notice shows once; "I understand" dismisses it permanently.
- [ ] Dashboard shows model, Android version, chipset, ABI and security patch.
- [ ] Capability ring score matches the number of native rows.

## Capability report

- [ ] NFC, IR, Bluetooth, USB, camera and microphone statuses match the device spec sheet.
- [ ] Sensor rows list vendor and power for present sensors; absent ones are labelled *Absent*.
- [ ] Sub-GHz is always labelled *Bridge*.
- [ ] Share report opens the system share sheet with a `.txt` file.
- [ ] Share JSON opens the sheet with a `.json` file; JSON opens in a viewer.
- [ ] Copy JSON puts valid JSON on the clipboard.

## Sensor lab

- [ ] EMF gauge moves when a magnet approaches; peak value is retained.
- [ ] Unit switch in Settings changes µT ↔ mG everywhere.
- [ ] Accelerometer bars respond to movement; bubble level centres on a flat surface.
- [ ] Compass heading is correct within ±10°, including on devices without a rotation vector.
- [ ] Light, proximity and pressure update (where the sensors exist).
- [ ] Leaving the screen stops the sensors (no drain in the battery screen).

## Audio lab

- [ ] Permission prompt appears on first capture only.
- [ ] A 1 kHz tone shows a peak at ≈1 kHz and a clean spike in the spectrum.
- [ ] Speech moves the level meter; loud input approaches 0 dBFS.
- [ ] Above 16 kHz content lights the *Ultrasonic* badge.
- [ ] Backgrounding the app stops the capture.
- [ ] No audio file appears in storage.

## NFC reader

- [ ] With NFC off, the card offers a shortcut to system settings.
- [ ] A text tag shows UID + technology + decoded text.
- [ ] A URL tag shows the full URI, including the `https://` prefix.
- [ ] History keeps the newest first and survives a process restart.
- [ ] Clear history (toolbar or Settings) empties the list.

## IR

- [ ] On a phone with an emitter, the test pulse is emitted (visible through another phone camera).
- [ ] Preset codes and a custom 32-bit code reach the transmit log.
- [ ] Invalid hex shows an error instead of crashing.
- [ ] On a phone without an emitter the screen explains the bridge option.

## Lens finder

- [ ] Camera preview fills the screen without stretching or mirroring artefacts.
- [ ] Torch and strobe toggle the LED.
- [ ] A torch reflection in a mirror produces at least one HUD marker.
- [ ] Sensitivity slider changes the marker count.
- [ ] Leaving the screen releases the camera (other apps can use it).

## Bridge console

- [ ] Permission request appears on Android 12+ and the device list fills after granting.
- [ ] Without Bluetooth the screen states it clearly.
- [ ] Connecting to an ESP32/HC-05 shows *Connected* and the console echoes replies.
- [ ] Presets send the documented lines; `STATUS` returns an `OK`/`ACK` line.
- [ ] Turning the module off marks the link closed without crashing.

## Localisation

- [ ] English and Persian both render completely (no missing strings, no clipping).
- [ ] Persian layout mirrors: back arrow on the right, cards and grids flipped.
- [ ] Persian labels are not letter-spaced or broken.
- [ ] Language change restarts into the new locale; the persisted choice survives restart.
- [ ] System language `fa` is honoured on first install.

## Robustness

- [ ] Rotate every screen; no crash and state is preserved or rebuilt cleanly.
- [ ] Deny camera, microphone and Bluetooth permissions; each screen degrades with a message.
- [ ] Background/foreground cycles release camera, audio and sensors.
- [ ] Airplane mode does not change behaviour (the app has no network path).
