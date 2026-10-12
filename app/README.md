# Flipper One

This app inspects the real hardware features of an Android device and gives a clear capability report to the user.

It is designed as a practical prototype for a phone-first "Flipper-like" workflow with external hardware support.

## What it checks

- NFC
- HCE
- IR
- Bluetooth / BLE
- USB Host / OTG
- Headphone jack
- Microphone
- Camera
- Accelerometer
- Gyroscope
- Magnetometer / Compass
- Light sensor

## Purpose

The app helps the user understand what the device can do on its own and when an external bridge or module is necessary.

## Limitations

- Most phones cannot transmit Sub-GHz RF directly.
- Audio and visual detection is heuristic and prototype-level.
- Advanced sensing requires external hardware or calibrated sensors.
