# Flipper One

This project is a practical Android prototype designed to inspect real hardware capabilities of a phone and pair them with external hardware for extended functionality.

## Overview

Flipper One turns a smartphone into a capability-aware control center. It checks what the device can do natively and what requires a bridge, such as ESP32, USB-OTG module, or external sensor hardware.

## Core idea

- A phone can expose useful capabilities such as microphone, camera, accelerometer, gyroscope, magnetometer, USB-OTG, and NFC.
- Some features are limited by hardware and OS support, such as Sub-GHz RF or certain radio operations.
- For advanced tasks, an external hardware bridge is required.

## Modules

- `app/` — Android app
- `esp32-firmware/` — firmware for ESP32 bridge and RF handling
- `docs/` — project docs and technical notes
- `payment/` — payment / licensing / support hooks
- `README.md` — project overview

## Main features in the app

- Device capability report
- Microphone scan and basic audio frequency analysis
- Camera capture and reflection-like detection for hidden-lens checks
- Sensor trace for motion and magnetic field detection
- Export of the report as JSON
- Privacy notice before microphone/camera use

## Usage

1. Open the app on a compatible Android device.
2. Tap Refresh report to inspect the device hardware.
3. Use the scan buttons to test microphone, camera, and motion sensors.
4. Export the report for sharing or debugging.

## Known limitations

- Most phones do not support direct Sub-GHz RF transmission.
- A real RF bridge or external hardware is usually required.
- Audio and reflection analysis are prototype-grade, not absolute detectors.
- Hidden camera detection is heuristic and may produce false positives.

## Privacy

The app will request microphone or camera permissions only when the user explicitly starts the corresponding scan. The app clearly states the purpose of the access.

## Status

This repository is in a mature MVP/prototype phase for hardware capability reporting and experimental scans. It is suitable for internal testing, showcase demos, and further extension into a more complete product.
