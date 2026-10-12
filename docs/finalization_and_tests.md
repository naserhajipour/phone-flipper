# Finalization checklist and test plan

This document describes the final steps to make the project fully usable and how to test each path.

1) Prerequisites
- Android device (API 24+)
- Arduino Mega or ESP32
- HC-05 (if using Mega) or an ESP32 built-in Bluetooth
- LCD 2x16 (optional) for status display
- CC1101 or RF433 module (optional) for Sub-GHz RF

2) Steps to test the control-only path (no RF module)
- Pair phone with HC-05 (or use ESP32 SPP name)
- Upload Arduino Mega sketch (esp32-firmware/arduino-mega-hc05-lcd.ino)
- In app Settings add OpenAI API key if you want AI assistance
- Open Chat, ask AI to generate a command (example: "SET_FREQUENCY 433920000\nSEND_RAW FFAA")
- Connect to BT in Chat screen (enter BT MAC)
- Press "Send last AI to hardware"
- Observe LCD and serial logs on Arduino

3) Steps to test with CC1101
- Wire CC1101 to Arduino SPI
- Upload cc1101_send_example.ino to the Arduino (or ESP32)
- Use AI to generate SEND_RAW commands and send them
- Observe CC1101 transmission (requires external receiver or scope)

4) Troubleshooting
- If BT connection fails: ensure device paired and the MAC is correct
- If commands show ERR:unknown_cmd on Arduino: verify command format (line-based)
- If CC1101 doesn't transmit: check wiring, 3.3V power, antenna

5) Next improvements
- Better parsing/ACK protocol (JSON frames, checksums)
- UI polish with native Material components for all screens
- Local LLM support for offline AI

