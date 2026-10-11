#include <Arduino.h>

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("Phone Flipper ESP32 bridge booted");
  Serial.println("Waiting for commands...");
}

void loop() {
  if (Serial.available()) {
    String line = Serial.readStringUntil('\n');
    line.trim();

    if (line.length() > 0) {
      Serial.print("Received: ");
      Serial.println(line);

      if (line.startsWith("STATUS")) {
        Serial.println("OK: bridge ready");
      } else if (line.startsWith("SET_FREQUENCY")) {
        Serial.println("OK: frequency command accepted by bridge");
      } else if (line.startsWith("SEND_RAW")) {
        Serial.println("OK: raw RF command queued");
      } else {
        Serial.println("ERR: unsupported command");
      }
    }
  }

  delay(20);
}
