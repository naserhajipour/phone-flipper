# ESP32 firmware stub

This is a minimal stub for an ESP32-based radio bridge. The final firmware will expose a BLE UART or USB serial service and will relay commands to a CC1101 module.

## Structure
- `src/main.cpp` — main firmware entry point
- `platformio.ini` — PlatformIO configuration

## Planned behavior
- Boot and initialize serial
- Expose BLE service or UART interface
- Parse commands from the phone
- Forward radio commands to CC1101
- Report status back to the Android app

The current stub is intentionally small and easy to extend.
