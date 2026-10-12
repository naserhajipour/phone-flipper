/*
CC1101 receive example (Arduino/ESP32) using Elechouse CC1101 library.
This sketch listens for packets and prints received bytes as hex lines on serial.
Useful for capturing simple ASK/OOK/FSK transmissions from remotes for analysis.

Wiring: CC1101 connected to SPI bus. Power 3.3V.

Note: capturing complex rolling-code remote signals may not be sufficient with simple sniffing.
Use this for simple on-off keying or raw packet capture testing.
*/

#include <ELECHOUSE_CC1101_SRC_DRV.h>

void setup() {
  Serial.begin(115200);
  ELECHOUSE_cc1101.Init();
  ELECHOUSE_cc1101.setRx();
  Serial.println("CC1101 RX ready");
}

void loop() {
  byte buf[64];
  int len = ELECHOUSE_cc1101.ReceiveData(buf);
  if (len > 0) {
    Serial.print("RCV:");
    for (int i = 0; i < len; i++) {
      if (buf[i] < 16) Serial.print('0');
      Serial.print(buf[i], HEX);
    }
    Serial.println();
  }
  delay(50);
}
