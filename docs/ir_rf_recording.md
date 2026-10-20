# IR and RF recording and replay (English + Persian)

Legal first: record and replay signals only for devices you own or have explicit permission to test.

---

## Recording IR (phone + HC-05 + Arduino Mega)

1. **Hardware** – TSOP38238 receiver on Mega pin 7, IR LED with transistor driver on pin 3,
   HC-05 on `Serial1`, optional 2×16 LCD.
2. **Flash** – upload `esp32-firmware/ir_record_play_mega.ino`.
3. **Pair** – pair the HC-05 with the phone.
4. **Record** – open **Bridge console**, connect to the module and send `RECORD_IR_START`.
   Point the original remote at the receiver and press a button; the firmware answers with
   `IR_REC_DONE`.
5. **Replay** – send `PLAY_IR`. The Arduino reproduces the captured timing through the IR LED.

Timing-sensitive protocols need a stable driver; this approach is fine for most consumer remotes.

---

## Recording RF (CC1101)

1. **Hardware** – CC1101 on SPI, 3.3 V supply and a matched antenna for the target band.
2. **Flash** – `esp32-firmware/cc1101_receive_example.ino` to capture, or integrate the same code
   into the SPP bridge firmware.
3. **Capture** – put the radio in RX mode and press remote buttons near it; packets appear as hex
   lines on the serial console.
4. **Replay** – send `SEND_RAW <hex>` from the app. The bridge transmits the payload.

Many RF remotes use rolling codes or encryption; replaying captured packets will not work and may be
illegal.

---

## خلاصهٔ فارسی

این راهنما نشان می‌دهد چگونه به‌صورت قانونی سیگنال‌های سادهٔ مادون قرمز و RF را ضبط و پخش کنید.

مهم: فقط برای دستگاه‌هایی که مالک آن‌ها هستید و مجوز دارید اقدام کنید.

### مراحل IR

۱. سخت‌افزار: گیرندهٔ IR به پین ۷ آردوینو، فرستندهٔ IR به پین ۳، HC-05 به `Serial1`.
۲. اسکچ `ir_record_play_mega.ino` را آپلود کنید.
۳. HC-05 را با گوشی جفت کنید.
۴. در برنامه **کنسول پل** را باز کنید، به ماژول وصل شوید و `RECORD_IR_START` را بفرستید؛ سپس با
   ریموت اصلی دکمه را فشار دهید.
۵. برای پخش، `PLAY_IR` را بفرستید.

### مراحل RF با CC1101

۱. CC1101 را روی SPI وصل کنید و آنتن مناسب باند را ببندید.
۲. اسکچ `cc1101_receive_example.ino` را آپلود کنید.
۳. دکمه‌ها را نزدیک ماژول فشار دهید و خروجی hex را در سریال ببینید.
۴. برای ارسال، از فرمان `SEND_RAW <hex>` در کنسول استفاده کنید.

بسیاری از ریموت‌های RF از کد غلتان یا رمزنگاری استفاده می‌کنند؛ بازپخش بسته‌های ضبط‌شده کار
نمی‌کند و می‌تواند غیرقانونی باشد.
