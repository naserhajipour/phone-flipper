package com.phoneflipper.app

import android.content.Context
import kotlin.math.min

object HardwarePromptLibrary {
    const val RF_EXAMPLE = "Design a RF command for 433.92 MHz, payload 0xFFAA, using a simple packet format. Explain what I need to send to the hardware bridge."
    const val NFC_EXAMPLE = "Generate a simple NFC NDEF payload example for a URL or text tag."
    const val IR_EXAMPLE = "Create an IR command template for a generic remote control using a 38kHz carrier and a short pulse sequence."
    const val ARDUINO_EXAMPLE = "Generate a command template for Arduino Mega + HC-05 + LCD 2x16. Show what serial commands are sent and what status is shown."

    fun buildSystemPrompt(language: String): String {
        val langText = if (language.equals("fa", ignoreCase = true)) {
            """
            شما یک دستیار فنی برای پروژه Phone Flipper هستید.
            پاسخ‌ها باید به زبان فارسی باشند.
            راه‌حل‌ها باید عملی، کوتاه و دقیق باشد.
            برای کارهای RF واقعی، همیشه به ماژول خارجی اشاره کنید.
            در صورت وجود سخت‌افزار، پیشنهادهای عملی با Arduino Mega + HC-05 + LCD 2x16 یا ESP32 + CC1101 بدهید.
            """
        } else {
            """
            You are a technical assistant for the Phone Flipper project.
            Responses should be concise, practical, and implementation-focused.
            For real RF work, always mention a hardware bridge or external radio module.
            If applicable, suggest Arduino Mega + HC-05 + LCD 2x16 or ESP32 + CC1101 workflows.
            """
        }

        return langText
    }
}

object CommandTemplates {
    fun rfSend(frequencyHz: Long, payloadHex: String): String {
        return "SET_FREQUENCY $frequencyHz\nSEND_RAW $payloadHex"
    }

    fun arduinoStatus(): String {
        return "STATUS\nSHOW Phone Flipper Ready"
    }

    fun irEmit(carrierHz: Int, patternHex: String): String {
        return "EMIT_IR $carrierHz $patternHex"
    }

    fun nfcText(payload: String): String {
        return "NFC_WRITE_TEXT $payload"
    }

    fun aiPromptForHardware(task: String): String {
        return "You are working on a hardware assistant. Please convert the request into practical commands for Arduino Mega + HC-05 + LCD 2x16, or ESP32 + CC1101. Request: $task"
    }
}

class HardwareAssistant(private val context: Context) {
    fun explainHardwareOverview(language: String): String {
        return if (language.equals("fa", ignoreCase = true)) {
            """
            معماری پیشنهادی:
            1) گوشی اندروید: رابط کاربری، ارسال دستورها، نمایش وضعیت.
            2) Arduino Mega: دریافت فرمان‌ها، نمایش روی LCD 2x16، کنترل ماژول جانبی.
            3) HC-05: ارتباط بی‌سیم بین گوشی و آردوینو.
            4) ماژول RF (اختیاری): CC1101 یا RF433، برای ارسال واقعی در باندهای 315/433/868/915 MHz.
            نتیجه: گوشی به‌تنهایی RF واقعی ندارد، اما می‌تواند با ماژول خارجی کار کند.
            """.trimIndent()
        } else {
            """
            Recommended architecture:
            1) Android phone: UI, command sender, status display.
            2) Arduino Mega: receives commands, displays status on 2x16 LCD, and manages the external module.
            3) HC-05: wireless serial bridge between phone and Arduino.
            4) RF module (optional): CC1101 or RF433 for real transmission in 315/433/868/915 MHz bands.
            Result: the phone alone does not provide direct low-frequency RF TX in most cases; it needs a hardware bridge.
            """.trimIndent()
        }
    }

    fun suggestWiring(language: String): String {
        return if (language.equals("fa", ignoreCase = true)) {
            """
            سناریوی پیشنهادی:
            - HC-05 TX -> Mega RX1 (پین 19)
            - HC-05 RX -> Mega TX1 (پین 18) با تقسیم ولتاژ
            - LCD 2x16 روی پین‌های RS, E, D4, D5, D6, D7
            - اگر RF واقعی لازم است: CC1101 به SPI آردوینو وصل شود
            - اگر از ESP32 استفاده می‌کنید، BLE یا UART را جایگزین HC-05 کنید
            """.trimIndent()
        } else {
            """
            Proposed wiring:
            - HC-05 TX -> Mega RX1 (pin 19)
            - HC-05 RX -> Mega TX1 (pin 18) using a voltage divider
            - LCD 2x16 on RS, E, D4, D5, D6, D7 pins
            - If real RF is needed: connect CC1101 to Arduino SPI bus
            - If ESP32 is used, prefer BLE or UART instead of HC-05
            """.trimIndent()
        }
    }
}
