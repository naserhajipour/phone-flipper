# Hardware guide: connecting the phone to external radio hardware

This project is designed to support the realistic architecture: the phone acts as the controller and UI layer, while a separate radio bridge handles Sub-GHz transmission.

Recommended hardware
- ESP32 development board
- CC1101 radio module (315/433/868/915 MHz depending on target region)
- 3.3V regulator or stable supply if needed
- Wiring / headers / soldering tools
- Optional USB-UART adapter for programming

ESP32 + CC1101 wiring concept
- ESP32 SPI pins to CC1101 SPI pins
- GDO0/GDO2 from CC1101 to ESP32 GPIO inputs for interrupt or packet detection
- 3.3V to CC1101 VCC
- GND proper common ground
- Add decoupling capacitors near the radio module if needed

Phone to radio bridge connection options
- BLE: simplest wireless option for end users
- USB-OTG: more direct and reliable when working with a USB connection on supported phones

BLE flow
- ESP32 exposes a UART-like BLE service
- Android app connects to the BLE device
- App sends commands such as:
  - `SET_FREQUENCY 433920000`
  - `SEND_RAW <hex payload>`
  - `STATUS`
- ESP32 forwards to CC1101 or routes to the supported hardware

USB-OTG flow
- Connect ESP32 over USB serial to the phone using OTG adaptor
- Use Android USB host APIs and serial library to read/write bytes
- The app sends command frames to the bridge and receives status back

Important practical rules
- Do not assume a phone can send Sub-GHz RF directly.
- Confirm your target frequencies and local regulations before transmitting.
- Use an external module when your workflow requires 315/433/868/915 MHz transmissions.

This project will aim to support both BLE and USB-OTG bridge interfaces, but BLE is the best first default because it is simpler for phone users.
