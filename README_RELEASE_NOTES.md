# Release notes — phone-flipper v0.2

This release completes the core prototype:
- Bilingual Android app (EN/FA)
- AI assistant (OpenAI) for generating hardware commands and wiring guidance
- Bluetooth SPP bridge support (HC-05) and ESP32 SPP example
- Arduino Mega + LCD 2x16 firmware example
- CC1101 example sketch for sending raw payloads
- Detailed docs and test plan

What's left to do (optional but recommended):
- Field testing with your actual hardware
- Add JSON-based protocol and robust ACK/ERR handling
- Integrate BLE UART for ESP32 (non-SPP) modes as alternative
- Add automated tests and CI for builds
