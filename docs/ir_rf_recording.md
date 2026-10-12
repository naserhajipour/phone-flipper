# IR & RF recording and replay guide (English + Persian)

## Overview
This guide explains how to legally record and replay simple IR and RF remote signals for your own devices using the Phone Flipper project.

IMPORTANT: Only record and replay signals for devices you own or have explicit permission to test. Do not attempt to capture or replay signals from devices you do not own (e.g., car keyfobs, garage remotes of others, security systems).

---

## IR recording (Phone + HC-05 + Arduino Mega)

1) Hardware
- IR receiver (e.g., TSOP38238) connected to Arduino Mega pin 7
- IR transmitter (IR LED with transistor driver) connected to pin 3
- HC-05 paired with phone and connected to Mega Serial1 (RX1/TX1)
- Optional LCD 2x16 to show status

2) Flash the sketch
- Upload esp32-firmware/ir_record_play_mega.ino to Arduino Mega

3) Steps to record
- Pair HC-05 with phone
- In the Phone Flipper app, connect to HC-05 (enter BT MAC in Chat screen)
- In the Chat screen send the command via Bluetooth: RECORD_IR_START (you can also use the UI 'Record IR' button)
- Point the original remote to the IR receiver and press the button
- The Arduino will capture the raw timings and respond with IR_REC_DONE

4) Steps to playback
- From the app send PLAY_IR (or press 'Play IR')
- Arduino plays back the recorded pattern through the IR LED

5) Notes
- Timing-sensitive protocols need accurate drivers and hardware; this approach is good for most consumer remotes.

---

## RF recording (CC1101)

1) Hardware
- CC1101 module connected to an MCU (Arduino/ESP32) via SPI
- Antenna and 3.3V power

2) Flash the sketch
- Upload esp32-firmware/cc1101_receive_example.ino to the MCU

3) Steps to capture
- Put CC1101 into RX mode and observe serial logs
- Press remote buttons near CC1101; received packets show as hex lines

4) Replay
- Use esp32-firmware/cc1101_send_example.ino (or integrate into your bridge firmware) and send SEND_RAW <hex> commands to transmit captured payloads

5) Legal warning
- Many RF remote systems (especially car remotes and security sensors) use rolling codes or encryption; simply replaying captured packets may not work and can be illegal.

---

## Persian (خلاصه فارسی)

این راهنما نشان می‌دهد چگونه به‌صورت قانونی سیگنال‌های ساده مادون‌قرمز و RF را ضبط و پخش کنید.

مهم: فقط برای دستگاه‌هایی که مالک آن‌ها هستید و مجوز دارید اقدام کنید.

مراحل IR:
1) سخت‌افزار: گیرنده IR به پین 7 آردوینو، فرستنده IR به پین 3
2) آپلود اسکچ ir_record_play_mega.ino
3) جفت‌سازی HC-05 و گوشی
4) در اپ دکمه RECORD_IR_START را بزنید، سپس با ریموت اصلی دکمه را فشار دهید
5) برای پخش، PLAY_IR را بفرستید

مراحل RF با CC1101:
1) CC1101 را به SPI وصل کنید
2) اسکچ cc1101_receive_example.ino را آپلود کنید
3) دکمه‌ها را بزنید و خروجی hex را در سریال ببینید
4) برای ارسال از cc1101_send_example.ino استفاده کنید

