# Phone Flipper

Phone Flipper is a bilingual Android project that models a realistic phone-plus-hardware architecture for Flipper Zero-like workflows.

English overview
- The app detects the actual hardware capabilities of the Android device.
- It checks NFC, HCE, IR, BLE, USB Host, camera, and related features.
- It reports that Sub-GHz RF is not normally available directly on consumer phones.
- For real RF transmission in 315/433/868/915 MHz, a separate radio bridge is needed.

Persian overview
- این پروژه یک اپ اندرویدی دوزبانه است که معماری واقع‌گرایانه گوشی + سخت‌افزار جانبی را برای کارهای مشابه فلیپر زیرو مدل می‌کند.
- اپ قابلیت‌های واقعی گوشی را تشخیص می‌دهد.
- NFC، HCE، IR، BLE، USB Host، دوربین و سایر ویژگی‌ها بررسی می‌شوند.
- RF زیرگیگاه معمولاً مستقیماً در گوشی‌های مصرفی در دسترس نیست.
- برای ارسال واقعی RF در باندهای 315/433/868/915 مگاهرتز، ماژول جانبی لازم است.

Architecture
- Phone = UI and local feature controller
- External bridge = actual RF transmitting hardware
- Communication = BLE or USB-OTG

Recommended hardware
- ESP32
- CC1101 radio module
- 3.3V power supply and common ground
- BLE or USB serial bridge

License
- See `LICENSE` if present. Otherwise, use this repository as a prototype and adapt it to your own deployment needs.

Important note
- RF transmission can be regulated by local law. Use only in authorized test environments.
