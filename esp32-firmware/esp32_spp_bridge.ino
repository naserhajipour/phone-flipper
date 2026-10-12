/*
ESP32 (Arduino) sketch: Classic SPP using SerialBT (acts like HC-05)
This allows an ESP32 to be used like a Bluetooth serial bridge if you prefer ESP32 instead of HC-05.

Connect ESP32 RX/TX to CC1101 or other module as needed. This sketch simply relays commands from Bluetooth to Serial and vice versa.
*/

#include "BluetoothSerial.h"
BluetoothSerial SerialBT;

void setup() {
  Serial.begin(115200);
  SerialBT.begin("ESP32_PhoneFlipper"); // Bluetooth device name
  Serial.println("ESP32 Bluetooth SPP bridge started");
}

void loop() {
  if (SerialBT.available()) {
    String line = SerialBT.readStringUntil('\n');
    line.trim();
    Serial.print("BT> "); Serial.println(line);
    // Echo or handle commands
    if (line.startsWith("STATUS")) {
      SerialBT.println("OK:esp32_bridge");
    } else {
      SerialBT.println("ACK:" + line);
    }
  }

  if (Serial.available()) {
    String s = Serial.readStringUntil('\n');
    SerialBT.println(s);
  }
  delay(10);
}
