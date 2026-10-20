# Documentation

| Document | Contents |
|---|---|
| [architecture.md](architecture.md) / [architecture-farsi.md](architecture-farsi.md) | Layers, data flow, hardware probing, signal processing, offline guarantees |
| [screens.md](screens.md) | Screen-by-screen tour of the UI |
| [building.md](building.md) | Gradle and headless build paths, CI, troubleshooting |
| [privacy.md](privacy.md) | What is stored, permissions, how to verify the offline claims |
| [testing.md](testing.md) | Manual acceptance checklist |
| [store-listing.md](store-listing.md) | Ready-to-publish store metadata (EN/FA) |
| [hardware-guide.md](hardware-guide.md) / [hardware-guide-farsi.md](hardware-guide-farsi.md) | ESP32 + CC1101 bridge, wiring and protocol |
| [hardware_detection.md](hardware_detection.md) | What each capability check means (Persian) |
| [ir_rf_recording.md](ir_rf_recording.md) | Recording and replaying IR and RF signals legally |
| [arduino-hc05-lcd-wiring.md](arduino-hc05-lcd-wiring.md) | Arduino Mega + HC-05 + LCD wiring notes |
| [sales/](sales/) | Commercial documents for hardware bundles |

## Firmware

The `esp32-firmware/` folder contains example sketches that speak the protocol used by the bridge
console:

* `src/main.cpp` – serial command parser (PlatformIO)
* `esp32_spp_bridge.ino` – ESP32 as a Bluetooth SPP bridge
* `ir_record_play_mega.ino` – IR record/playback on Arduino Mega
* `cc1101_receive_example.ino` / `cc1101_send_example.ino` – raw Sub-GHz examples
