/*
CC1101 example using Elechouse CC1101 library (Arduino)
This sketch demonstrates sending raw payload via CC1101.
Connect CC1101 to SPI pins and power it with 3.3V.
*/

#include <ELECHOUSE_CC1101_SRC_DRV.h>

void setup() {
  Serial.begin(115200);
  ELECHOUSE_cc1101.Init();
  Serial.println("CC1101 init done");
}

void loop() {
  if (Serial.available()) {
    String line = Serial.readStringUntil('\n');
    line.trim();
    if (line.startsWith("SEND_RAW")) {
      // format: SEND_RAW FFAA1122
      String hex = line.substring(8);
      int len = hex.length() / 2;
      byte buf[64];
      for (int i=0;i<len && i<64;i++) {
        String b = hex.substring(i*2, i*2+2);
        buf[i] = (byte) strtol(b.c_str(), NULL, 16);
      }
      ELECHOUSE_cc1101.sendData(buf, len);
      Serial.println("OK: cc1101 sent");
    }
  }
  delay(20);
}
