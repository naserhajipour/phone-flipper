# Android app hardware detection prototype (Flipper One)

This app checks what the phone can actually do and shows a clear capability report to the user.

It is intentionally designed as a practical foundation for a Flipper One-like experience built around a phone plus external hardware. The app examines:

- NFC, HCE, IR
- Bluetooth / BLE
- USB-OTG and USB port availability
- Headphone jack (if present)
- Sensors: accelerometer, gyroscope, magnetometer (compass), light sensor, etc.

Use this as a starting point to connect the phone to an ESP32 or other radio bridge for Sub‑GHz RF functionality.
