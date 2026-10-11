# phone-flipper

Phone Flipper is a proof-of-concept Android app plus hardware bridge for Flipper Zero-like workflows using a phone and external radio module.

Core idea
- Detect which features the Android phone actually exposes: NFC, HCE, IR, Bluetooth/BLE, USB-OTG, camera, and general RF availability.
- Use phone-native features when possible.
- For Sub-GHz RF (315/433/868/915 MHz), use an external module (CC1101, SDR, or a dedicated radio bridge), because most phones do not expose hardware for direct RF transmission in those bands.
- Connect the phone to the external module via BLE or USB-OTG and send commands/packets from the app.

What this repo currently includes
- Android app skeleton to detect phone features
- Device capability report screen
- Documentation about hardware limits and architecture
- ESP32 firmware stub for a radio bridge

Project structure
- `app/` — Android app (Kotlin)
- `esp32-firmware/` — ESP32 code stub for radio bridge and BLE/UART communication
- `docs/` — technical notes and wiring instructions
- `README.md` — project overview

Important limitation
- A normal Android phone does not provide reliable or standard direct Sub-GHz TX support for 315/433/868/915 MHz.
- For those functions, the phone must delegate to an external module or hardware bridge.
- NFC / IR / BLE / USB-OTG are generally more practical and implementable on Android.

Planned application capabilities
- Device scanner and capability report
- NFC tag read/write and HCE basics
- IR code emission when supported by the phone
- BLE interface to external hardware
- USB-OTG serial bridge for external dongles/controllers
- External radio module integration for low-frequency RF workflows

Legal / safety note
- RF transmission in licensed or regulated bands may be restricted by local law.
- Do not use this project to interfere with real devices or networks you do not own or are not authorized to test.

Repository status
- The project is in active prototype stage and is meant as a starting point for a real Android + external hardware implementation.
