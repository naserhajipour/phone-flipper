# کد پایه ESP32

این فایل یک نمونه اولیه برای ESP32 است که منتظر فرمان‌ها از گوشی می‌ماند و پاسخ می‌دهد.

```cpp
#include <Arduino.h>

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("Phone Flipper bridge started");
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
        Serial.println("OK: frequency set received");
      } else if (line.startsWith("SEND_RAW")) {
        Serial.println("OK: raw command queued");
      } else {
        Serial.println("ERR: unsupported command");
      }
    }
  }

  delay(20);
}
```

## هدف این نمونه

- ساده و قابل درک باشد
- بعداً برای CC1101 و RF واقعی توسعه یابد
- به‌عنوان پایه‌ای برای رابط BLE/UART عمل کند
