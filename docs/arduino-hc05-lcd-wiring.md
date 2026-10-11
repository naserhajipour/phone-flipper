# Wiring and hardware notes for Arduino Mega + HC-05 + LCD 2x16

## Recommended wiring
- HC-05 TX -> Mega RX1 (Pin 19)
- HC-05 RX -> Mega TX1 (Pin 18) through a simple voltage divider or level shifter
- LCD 2x16 pins: RS, E, D4, D5, D6, D7
- Power: 5V to LCD and Mega, 3.3V to HC-05 logic side if needed

## Why this is useful
- The Android app can send commands to Arduino without a USB cable.
- The Arduino can display status on the LCD and respond to commands.
- You can later add RF hardware such as CC1101 to expand the project.

## Command protocol example
- STATUS
- SHOW Hello
- SET_FREQUENCY 433920000
- SEND_RAW FFAA

## Important note
- This is not a direct Sub-GHz transmitter by itself.
- For real RF work, add a dedicated radio module.
