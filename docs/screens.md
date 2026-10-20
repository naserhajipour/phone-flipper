# Screen tour

Every screen is built at runtime from `core/Ui` primitives; there are no layout XML files. This
keeps the visual language consistent and the APK small.

## Dashboard

* Brand row with the vector logo, product name, kicker and a settings action.
* Hero headline, tagline and three chips: *100% offline*, *No tracking*, version.
* Capability score card: 270° ring, native/bridge/absent counters and the check count.
* Tool grid (2 × 4) with icon tiles, status badges and two lines of description.
* Device card: model, Android version, chipset, ABI, security patch.
* Footer: offline statement, build number and legal note.

## Capability report

* Summary card with score, counts and a three-segment bar.
* Four grouped sections (Radio, Sensors, Optics & audio, System & I/O). Each row has an accent icon,
  status badge and a technical detail line.
* Export card: share text, copy JSON, share JSON.

## Sensor lab

* Live pill and intro copy.
* Magnetic card: gauge (µT or mG), peak readout, quality badge (background / elevated / strong),
  rolling history chart.
* Motion card: magnitude and X/Y/Z meters, bubble level.
* Compass card: rotating dial, cardinal letters, numeric heading.
* Rotation card (when a gyroscope exists) and environment card (light, proximity, pressure).

## Audio lab

* Dominant-frequency gauge with kHz/Hz auto-scaling and an *Ultrasonic* badge.
* dBFS meter with a hint about clipping.
* Spectrum view: 44 log-spaced bars from 40 Hz to 20 kHz with grid labels and a peak marker.
* Waveform view of the most recent buffer.
* Start/stop control and a privacy note.

## NFC reader

* State card (unavailable / disabled with a settings shortcut / active).
* Last-scan card: UID, technology list and decoded NDEF payload with a copy action.
* History list with timestamps and per-entry copy.

## Infrared

* Status chips, carrier-range readout and a test pulse.
* Six NEC preset buttons and a custom hex field.
* Transmit log with timestamps.

## Lens finder

* Full-bleed camera preview with a HUD overlay (corner brackets, reticle, markers).
* Bottom sheet: reflection counter, torch, strobe, hint line and sensitivity slider.

## Bridge console

* Status pill and intro.
* Device list: paired and discovered devices with name, MAC and bond state.
* Console: monospace log, input field and quick presets.
* Protocol note and legal footer.

## Field guide

* Six rows with icons and subtitles.
* Detail screen renders a small Markdown subset (headings, bullets, quotes, rules) from the bundled
  asset in the active language.

## Settings

* Language: system / English / فارسی with immediate restart.
* Units: µT or mG.
* Behaviour: keep screen on, haptics.
* Local data: clear NFC history, reset everything.
* About: version, package, network status, privacy, permissions, disclaimer, review notice.
