/*
Arduino Mega sketch for IR record & playback using IRremote library.
This sketch works with HC-05 Bluetooth SPP: phone sends commands over serial to control recording and playback.
Commands (line-based):
  RECORD_IR_START  - begin listening for next IR transmission
  PLAY_IR          - play the last-recorded pattern
  STATUS           - respond with status

Wiring:
  - IR receiver (e.g., TSOP38238) data pin -> pin 7
  - IR transmitter (IR LED with transistor) -> pin 3 (use appropriate driver)
  - HC-05 TX -> Mega RX1 (pin 19)
  - HC-05 RX -> Mega TX1 (pin 18) via voltage divider
  - LCD optional (see other sketch)

Notes:
  - Requires IRremote library: https://github.com/Arduino-IRremote/Arduino-IRremote
  - Ensure the IR LED driver (transistor + resistor) and power are correctly set.
  - The sketch stores one recorded pattern in RAM; increase buffer if needed.
*/

#include <IRremote.h>

const int RECV_PIN = 7;   // IR receiver
const int SEND_PIN = 3;   // IR transmitter (use transistor driver)

IRrecv irrecv(RECV_PIN);
IRsend irsend;
decode_results results;

bool hasPattern = false;
unsigned int rawBuf[250];
unsigned int rawLen = 0;

void setup() {
  Serial.begin(115200);
  // Use Serial1 for HC-05 on Mega (RX1/TX1)
  Serial1.begin(9600);
  irrecv.enableIRIn();
  irsend.begin();
  pinMode(LED_BUILTIN, OUTPUT);
  Serial.println("IR recorder ready");
  Serial1.println("IR bridge ready");
}

void loop() {
  // Handle commands from Bluetooth (Serial1)
  if (Serial1.available()) {
    String line = Serial1.readStringUntil('\n');
    line.trim();
    if (line.length() == 0) return;
    Serial.print("CMD: "); Serial.println(line);

    if (line.equalsIgnoreCase("RECORD_IR_START")) {
      Serial1.println("ACK: RECORDING");
      Serial.println("Start recording IR...");
      // wait for next signal
      bool recorded = waitForIR();
      if (recorded) {
        hasPattern = true;
        Serial1.println("IR_REC_DONE");
      } else {
        Serial1.println("IR_REC_FAIL");
      }
    } else if (line.equalsIgnoreCase("PLAY_IR")) {
      if (!hasPattern) {
        Serial1.println("ERR: NO_PATTERN");
      } else {
        Serial1.println("ACK: PLAYING");
        playPattern();
        Serial1.println("IR_PLAYED");
      }
    } else if (line.equalsIgnoreCase("STATUS")) {
      Serial1.println(hasPattern ? "STATUS:HAS_PATTERN" : "STATUS:NO_PATTERN");
    } else {
      Serial1.println("ERR:UNKNOWN_CMD");
    }
  }

  // Optional: echo USB serial to BT
  if (Serial.available()) {
    String s = Serial.readStringUntil('\n');
    if (s.length() > 0) Serial1.println(s);
  }
}

bool waitForIR(unsigned long timeoutMs = 8000) {
  unsigned long start = millis();
  while (millis() - start < timeoutMs) {
    if (irrecv.decode(&results)) {
      // results.rawbuf contains timings in ticks (microseconds / USECPERTICK)
      // store skipping the first gap value
      rawLen = 0;
      for (unsigned int i = 1; i < results.rawlen && rawLen < 250; i++) {
        rawBuf[rawLen++] = results.rawbuf[i] * USECPERTICK; // convert to microseconds
      }
      irrecv.resume();
      Serial.print("Recorded raw length: "); Serial.println(rawLen);
      return true;
    }
    delay(10);
  }
  Serial.println("Timeout waiting for IR");
  return false;
}

void playPattern() {
  // IRremote's sendRaw expects microseconds array and length
  if (rawLen == 0) return;
  // Convert unsigned int array to unsigned int* as needed
  irsend.sendRaw((unsigned int*)rawBuf, rawLen, 38); // play at ~38kHz
  delay(200);
}
