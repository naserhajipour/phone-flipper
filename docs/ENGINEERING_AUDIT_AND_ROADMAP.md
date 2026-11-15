# Flipper Hamrah — Engineering audit and delivery roadmap

Last reviewed: 2026-11-15
Repository: `naserhajipour/phone-flipper`

## Current baseline

The repository contains an Android/Kotlin prototype, an NFC reader activity, a capability detector, audio FFT sampling, a short sensor trace, ESP32/CC1101 examples and hardware documentation. This is a useful foundation, but the release notes describe some planned capabilities as if they were already complete. Treat those items as roadmap until there is implementation and a reproducible test.

## First integration changes

- The dashboard now uses one capability-report builder instead of maintaining a second, partial list of hardware flags.
- The report includes Android API/device identity, all sensors exposed by `SensorManager`, sensor vendor/range/resolution/minimum delay, and grouped capabilities.
- The report explicitly distinguishes phone-native features from external-module requirements and documents important limits (IR receive, 125 kHz RFID, Sub-GHz RF, magnetometer/metal detection, USB adapter compatibility).
- Bluetooth adapter state inspection now handles the Android 12+ `BLUETOOTH_CONNECT` permission boundary without crashing the capability report.

## Gaps confirmed in the current source

1. **USB OTG:** manifest advertises USB Host, but there is no end-to-end USB device enumeration, permission request, serial transport, framing, timeout or reconnect implementation in the reviewed source.
2. **BLE bridge:** BLE capability detection exists, but a complete GATT UART client, service/characteristic discovery, MTU handling, notification subscription and reconnect state machine are still needed.
3. **IR remote:** Consumer IR is detected, but no transmitter UI, carrier validation, learned-code model or device profile workflow is integrated.
4. **NFC:** basic reader mode and NDEF parsing exist. This is not a general-purpose card emulator, credential cloner or 125 kHz RFID tool.
5. **AI engineering assistant:** release notes mention an OpenAI assistant, but the reviewed Android source does not contain a complete assistant screen, secure key strategy, conversation flow, or tool execution boundary.
6. **Circuit design:** no schematic editor, netlist model, symbol library, ERC, PCB layout or export pipeline was found in the reviewed paths.
7. **Project/code workspace:** no complete Arduino/ESP32 project generator, compile/upload pipeline, project persistence or wiring-diagram renderer was found in the reviewed paths.
8. **Build reproducibility:** Gradle project files exist, but the repository paths checked did not contain the Gradle wrapper. CI should establish whether the app builds from a clean checkout.

## Delivery sequence

### P0 — Buildable and hardware-safe foundation
- Establish CI build on every push.
- Keep capability detection truthful and permission-safe.
- Add automated unit tests for capability classification and protocol framing.
- Separate UI, platform adapters, transports and project models.

### P1 — Real phone-native tools
- IR transmit screen using `ConsumerIrManager` with device capability checks.
- Magnetic field live meter with calibrated units, axes, peak/hold and clear "not a metal detector" guidance.
- Motion/sensor dashboard with start/stop lifecycle handling and CSV export.
- NFC reader improvements: tag technology summary, safe NDEF parsing and clear unsupported-tag messaging.

### P2 — External hardware bridge
- BLE GATT UART client for ESP32.
- USB Host device discovery and explicit user permission flow.
- USB serial transport with supported chipset drivers, framed messages, ACK/ERR, timeout and reconnect.
- Common bridge protocol and module registry; no RF transmit action without region/frequency/power safeguards.

### P3 — Engineering workbench
- AI assistant with structured project requirements and generated artifacts.
- Arduino Mega / ESP32 code generation with board profiles and pin validation.
- Schematic editor based on a structured netlist and reusable symbol library.
- Wiring diagrams, BOM and project export.
- Project storage, revision history and build/upload guidance.

## Acceptance gates

A feature is not marked complete until it has:
- implementation in the Android app or firmware,
- explicit hardware/OS prerequisites,
- permission and failure-state handling,
- at least one automated test where practical,
- a manual test procedure and a recorded device result.

## Device-specific validation

The primary target is POCO M6 Pro 4G. Do not infer exact hardware availability from the model name alone: query the live device through Android APIs and verify NFC/IR/sensor/USB behavior on the actual handset and regional variant.
