package com.phoneflipper.app

import android.app.Activity
import android.content.Context
import android.hardware.ConsumerIrManager
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale

/**
 * IR transmitter for devices with an Android Consumer IR emitter.
 * The handset is a transmitter, not an IR receiver; it cannot learn codes by itself.
 */
class IrRemoteActivity : Activity() {
    private var irManager: ConsumerIrManager? = null
    private lateinit var statusView: TextView
    private lateinit var frequencyInput: EditText
    private lateinit var necInput: EditText
    private lateinit var rawPatternInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        irManager = getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

        val prefs = getSharedPreferences("ir_remote", MODE_PRIVATE)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)

        root.addView(TextView(this).apply {
            text = "ریموت مادون‌قرمز"
            textSize = 26f
        })
        statusView = TextView(this).apply {
            textSize = 15f
            setPadding(0, 16, 0, 16)
        }
        root.addView(statusView)

        root.addView(TextView(this).apply {
            text = "فرکانس حامل (هرتز)"
            textSize = 14f
        })
        frequencyInput = EditText(this).apply {
            hint = "مثلاً 38000"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(prefs.getString("frequency", "38000"))
        }
        root.addView(frequencyInput, matchWidth())

        root.addView(TextView(this).apply {
            text = "کد خام NEC سی‌ودوبیتی (۸ رقم هگزادسیمال)"
            textSize = 14f
            setPadding(0, 14, 0, 0)
        })
        necInput = EditText(this).apply {
            hint = "کد معتبر برای مدل دستگاه خود را وارد کنید"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            setText(prefs.getString("nec_code", ""))
        }
        root.addView(necInput, matchWidth())
        root.addView(button("ارسال کد NEC") { sendNecCode() }, matchWidth())

        root.addView(TextView(this).apply {
            text = "الگوی پالس سفارشی (میکروثانیه)"
            textSize = 14f
            setPadding(0, 18, 0, 0)
        })
        rawPatternInput = EditText(this).apply {
            hint = "9000,4500,560,560,560,1690"
            minLines = 2
            gravity = Gravity.TOP
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setText(prefs.getString("raw_pattern", ""))
        }
        root.addView(rawPatternInput, matchWidth())
        root.addView(button("ارسال الگوی سفارشی") { sendRawPattern() }, matchWidth())

        root.addView(TextView(this).apply {
            text = "نکته: گوشی فقط فرستندهٔ IR است و نمی‌تواند کد ریموت را مستقیماً دریافت یا یادگیری کند. کد و الگو بین برندها و مدل‌ها متفاوت‌اند. ابتدا روی وسیلهٔ خودت آزمایش کن؛ فرستادن یک کد لزوماً به معنی سازگاری با دستگاه نیست."
            textSize = 12f
            setPadding(0, 20, 0, 12)
        })
        root.addView(button("بازگشت") { finish() }, matchWidth())
        refreshCapabilities()
    }

    private fun refreshCapabilities() {
        val manager = irManager
        if (manager == null || !manager.hasIrEmitter()) {
            statusView.text = "فرستندهٔ مادون‌قرمز از طریق Android API در دسترس نیست."
            return
        }
        val ranges = try { manager.carrierFrequencies } catch (_: Exception) { null }
        val rangeText = if (ranges.isNullOrEmpty()) {
            "بازهٔ فرکانس گزارش نشد؛ هنگام ارسال بررسی می‌شود."
        } else {
            ranges.joinToString { range ->
                String.format(Locale.US, "%d–%d Hz", range.minFrequency, range.maxFrequency)
            }
        }
        statusView.text = "فرستندهٔ IR شناسایی شد. بازه‌های پشتیبانی‌شده: " + rangeText
    }

    private fun sendNecCode() {
        val raw = necInput.text.toString().trim().replace("0x", "").replace("0X", "").replace(" ", "")
        if (!raw.matches(Regex("(?i)[0-9a-f]{8}"))) {
            statusView.text = "کد NEC باید دقیقاً ۸ رقم هگزادسیمال باشد."
            return
        }
        val value = raw.toLongOrNull(16)
        if (value == null) {
            statusView.text = "کد واردشده معتبر نیست."
            return
        }
        val pattern = buildNecPattern(value)
        transmitPattern(pattern, "NEC " + raw.uppercase(Locale.US))
    }

    private fun buildNecPattern(code: Long): IntArray {
        val pattern = ArrayList<Int>(67)
        pattern.add(9000)
        pattern.add(4500)
        for (bit in 0 until 32) {
            pattern.add(560)
            pattern.add(if (((code ushr bit) and 1L) == 1L) 1690 else 560)
        }
        pattern.add(560)
        return pattern.toIntArray()
    }

    private fun sendRawPattern() {
        val parts = rawPatternInput.text.toString().trim()
            .split(Regex("[,;\\s]+"))
            .filter { it.isNotBlank() }
        if (parts.size < 2 || parts.size % 2 != 0 || parts.size > 200) {
            statusView.text = "الگو باید تعداد زوجی از ۲ تا ۲۰۰ زمان پالس داشته باشد."
            return
        }
        val pattern = parts.map { it.toIntOrNull() ?: 0 }.toIntArray()
        if (pattern.any { it !in 1..100000 }) {
            statusView.text = "هر زمان پالس باید بین ۱ تا ۱۰۰۰۰۰ میکروثانیه باشد."
            return
        }
        transmitPattern(pattern, "الگوی سفارشی")
    }

    private fun transmitPattern(pattern: IntArray, label: String) {
        val manager = irManager
        if (manager == null || !manager.hasIrEmitter()) {
            statusView.text = "این گوشی/سیستم فرستندهٔ IR قابل‌استفاده‌ای گزارش نمی‌کند."
            return
        }
        val frequency = frequencyInput.text.toString().toIntOrNull()
        if (frequency == null || frequency !in 20000..60000) {
            statusView.text = "فرکانس را در بازهٔ ۲۰٬۰۰۰ تا ۶۰٬۰۰۰ هرتز وارد کن."
            return
        }
        val durationUs = pattern.sumOf { it.toLong() }
        if (durationUs >= 1_900_000L) {
            statusView.text = "مدت کل الگو باید کمتر از ۱٫۹ ثانیه باشد."
            return
        }
        try {
            val ranges = manager.carrierFrequencies
            if (!ranges.isNullOrEmpty() && ranges.none { frequency >= it.minFrequency && frequency <= it.maxFrequency }) {
                statusView.text = "فرکانس " + frequency + " هرتز در بازهٔ گزارش‌شدهٔ فرستنده نیست: " +
                    ranges.joinToString { it.minFrequency.toString() + "–" + it.maxFrequency }
                return
            }
            manager.transmit(frequency, pattern)
            getSharedPreferences("ir_remote", MODE_PRIVATE).edit()
                .putString("frequency", frequency.toString())
                .putString("nec_code", necInput.text.toString().trim())
                .putString("raw_pattern", rawPatternInput.text.toString().trim())
                .apply()
            statusView.text = "الگوی «" + label + "» به API فرستنده ارسال شد. اگر وسیله واکنش نشان نداد، کد/پروتکل با مدل آن سازگار نیست."
        } catch (e: Exception) {
            statusView.text = "ارسال IR ناموفق بود: " + (e.message ?: "خطای نامشخص")
        }
    }

    private fun matchWidth(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    private fun button(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
        }
}
