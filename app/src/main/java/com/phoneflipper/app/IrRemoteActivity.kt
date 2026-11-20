package com.phoneflipper.app

import android.app.Activity
import android.hardware.ConsumerIrManager
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * Native IR transmitter using Android ConsumerIrManager.
 * Sends only user-entered pulse timings; it does not guess or capture device codes.
 */
class IrRemoteActivity : Activity() {
    private lateinit var irManager: ConsumerIrManager
    private lateinit var status: TextView
    private lateinit var frequencyInput: EditText
    private lateinit var timingsInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        irManager = getSystemService(CONSUMER_IR_SERVICE) as ConsumerIrManager

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 36, 36, 36)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        root.addView(TextView(this).apply {
            text = "فرستنده مادون‌قرمز"
            textSize = 24f
        })
        root.addView(TextView(this).apply {
            text = "این ابزار فقط از فرستنده IR داخلی گوشی استفاده می‌کند. الگوی پالس را فقط برای دستگاهی که اجازه کنترل آن را دارید وارد کنید."
            textSize = 15f
            setPadding(0, 16, 0, 20)
        })

        frequencyInput = EditText(this).apply {
            hint = "فرکانس حامل بر حسب هرتز (مثلاً 38000)"
            setText("38000")
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        root.addView(frequencyInput)

        timingsInput = EditText(this).apply {
            hint = "پالس‌ها بر حسب میکروثانیه، جداشده با کاما؛ مثال: 9000,4500,560,560"
            setText("9000,4500,560,560")
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            minLines = 3
            gravity = android.view.Gravity.TOP
        }
        root.addView(timingsInput)

        root.addView(Button(this).apply {
            text = "بررسی قابلیت IR"
            setOnClickListener { showCapabilities() }
        })
        root.addView(Button(this).apply {
            text = "ارسال الگوی واردشده"
            setOnClickListener { transmitPattern() }
        })
        status = TextView(this).apply {
            textSize = 14f
            setPadding(0, 20, 0, 0)
        }
        root.addView(status)
        setContentView(root)
        showCapabilities()
    }

    private fun showCapabilities() {
        if (!irManager.hasIrEmitter()) {
            status.text = "این دستگاه از دید Android فرستنده مادون‌قرمز ندارد."
            return
        }
        val ranges = irManager.carrierFrequencies
        status.text = if (ranges.isNullOrEmpty()) {
            "فرستنده IR موجود است؛ بازه فرکانسی اعلام نشده."
        } else {
            "فرستنده IR موجود است. بازه‌های پشتیبانی‌شده (Hz):\n" +
                ranges.joinToString("\n") { "${it.minFrequency} تا ${it.maxFrequency}" }
        }
    }

    private fun transmitPattern() {
        if (!irManager.hasIrEmitter()) {
            status.text = "ارسال انجام نشد: فرستنده IR در دسترس نیست."
            return
        }
        val frequency = frequencyInput.text.toString().trim().toIntOrNull()
        if (frequency == null || frequency !in 20000..60000) {
            frequencyInput.error = "فرکانس باید عددی بین 20000 و 60000 هرتز باشد."
            return
        }
        val pattern = timingsInput.text.toString().split(",")
            .mapNotNull { it.trim().toIntOrNull() }
        val rawParts = timingsInput.text.toString().split(",")
        if (rawParts.size != pattern.size || pattern.size < 2 ||
            pattern.any { it !in 1..100000 } || pattern.size % 2 != 0) {
            timingsInput.error = "حداقل دو زمان پالس مثبت و زوج، با کاما جداشده وارد کنید."
            return
        }
        try {
            irManager.transmit(frequency, pattern.toIntArray())
            status.text = "الگوی IR ارسال شد. موفقیت دریافت توسط دستگاه مقصد تضمین نمی‌شود."
        } catch (e: Exception) {
            status.text = "ارسال ناموفق: ${e.message ?: "خطای نامشخص"}"
        }
    }
}
