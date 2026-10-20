# Hardware guide: phone + radio bridge

The phone is the controller and the screen; a microcontroller does the radio work.

## Recommended hardware

| Part | Purpose |
|---|---|
| ESP32 dev board | Bluetooth SPP/BLE + command parser |
| CC1101 module | Sub-GHz transmit/receive for 315/433/868/915 MHz |
| 3.3 V regulator, decoupling caps | Stable radio supply |
| IR LED + NPN transistor + resistor | Infrared output for phones without an emitter |
| TSOP38238 receiver | IR capture |

## Wiring concept

```
ESP32                CC1101
GPIO18  ── SCK  ───►  SCLK
GPIO23  ── MOSI ───►  SI
GPIO19  ── MISO ◄───  SO
GPIO5   ── CSN  ───►  CSN
GPIO2   ◄── GDO0 ────  GDO0
3V3     ───────────►  VCC
GND     ───────────►  GND
```

Keep SPI leads short, add 100 nF close to the module and use a proper antenna for the target band.

## Pairing with the app

1. Power the bridge; it advertises as `ESP32_PhoneFlipper` (SPP example).
2. Pair it in system Bluetooth settings.
3. Open **Bridge console**, grant *Nearby devices* on Android 12+, and tap the device.

## Line protocol

One ASCII command per line, one reply line per command:

| Command | Meaning | Example reply |
|---|---|---|
| `STATUS` | Firmware + radio state | `OK: bridge ready` |
| `SET_FREQUENCY <hz>` | Centre frequency | `OK: frequency=433920000` |
| `SEND_RAW <hex>` | Transmit a raw payload | `OK: queued 4 bytes` |
| `RECORD_IR_START` | Arm the IR receiver | `OK: listening` |
| `PLAY_IR` | Replay captured IR timing | `OK: playing 68 edges` |
| `PING` | Link check | `ACK:PING` |

Errors use the `ERR:` prefix; the console prints them verbatim.

## Regulatory note

Transmitting is regulated. Confirm the allowed frequency, power and duty cycle in your country
before sending anything, and only operate on equipment you own or are authorised to test.
