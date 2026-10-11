# Project architecture

The design is intentionally split into two layers:

1. Phone layer
   - Android app detects hardware capabilities.
   - App presents a capability report and offers tool actions based on available hardware.
   - For NFC, IR, BLE, and USB-OTG, Android can do useful work directly.
   - For low-frequency RF, the app sends commands to an external radio bridge.

2. External hardware layer
   - A dedicated controller (for example ESP32 + CC1101) handles radio transmission and protocol translation.
   - The phone sends commands to the bridge over BLE or serial/USB.
   - The bridge works as a simple hardware adapter for frequencies such as 315/433/868/915 MHz.

Why this architecture is necessary
- Android APIs do not expose a generic Sub-GHz TX interface for most phones.
- Most phones cannot directly transmit at 315/433/868/915 MHz without extra hardware.
- This makes the phone a controller and UI layer, while the radio hardware does the actual RF work.

Typical workflow
- App starts and detects device capabilities.
- User selects a capability or tool.
- If the feature is present on the phone: use it directly.
- If the feature is missing: the app connects to the external bridge, transmits commands, and receives status back.

Recommended implementation order
1. Device capability detection and UI
2. BLE bridge to external module
3. USB-OTG bridge support
4. NFC and HCE basics
5. IR emit logic
6. Sub-GHz packet sending via radio bridge

This keeps the project realistic and incremental, while staying faithful to the actual hardware constraints of Android devices.
