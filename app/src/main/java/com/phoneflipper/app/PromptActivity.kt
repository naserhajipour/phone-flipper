package com.phoneflipper.app

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

open class PromptActivity : AppCompatActivity() {
    private lateinit var apiKeyInput: EditText
    private lateinit var promptInput: EditText
    private lateinit var output: TextView
    private lateinit var generateButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "دستیار مهندسی فلیپر همراه"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }
        val heading = TextView(this).apply {
            text = "دستیار مهندسی"
            textSize = 24f
        }
        val intro = TextView(this).apply {
            text = "برای راهنمایی آفلاین، کلید API لازم نیست. برای پاسخ مولد آنلاین، کلید API سازگار با OpenAI را فقط در همین نشست وارد کنید؛ کلید ذخیره نمی‌شود."
            textSize = 14f
        }
        apiKeyInput = EditText(this).apply {
            hint = "کلید API (اختیاری؛ ذخیره نمی‌شود)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        promptInput = EditText(this).apply {
            hint = "مثلاً اتصال ESP32 به CC1101 یا تولید کد Arduino Mega"
            minLines = 3
            gravity = android.view.Gravity.TOP
        }
        generateButton = Button(this).apply { text = "دریافت راهنمایی" }
        output = TextView(this).apply {
            text = "پاسخ دستیار در این قسمت نمایش داده می‌شود."
            textSize = 15f
            setTextIsSelectable(true)
        }
        root.addView(heading)
        root.addView(intro)
        root.addView(apiKeyInput)
        root.addView(promptInput)
        root.addView(generateButton)
        root.addView(output, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 20 })
        setContentView(ScrollView(this).apply { addView(root) })

        generateButton.setOnClickListener {
            val request = promptInput.text.toString().trim()
            if (request.isEmpty()) {
                promptInput.error = "درخواست را وارد کنید"
                return@setOnClickListener
            }
            generateButton.isEnabled = false
            output.text = "در حال آماده‌سازی پاسخ…"
            val key = apiKeyInput.text.toString().trim()
            thread {
                val answer = try {
                    if (key.isNotEmpty()) AIManager().chat(request, key)
                    else HardwareAssistant(this).explainHardwareOverview("fa") +
                        "\n\n" + HardwareAssistant(this).suggestWiring("fa") +
                        "\n\nبرای پاسخ دقیق‌تر، نوع برد، ماژول و هدف پروژه را در درخواست مشخص کنید. این پاسخ محلی و قالب‌محور است، نه مدل زبانی آنلاین."
                } catch (e: Exception) {
                    "خطا در دریافت پاسخ: ${e.message ?: e.javaClass.simpleName}"
                }
                runOnUiThread {
                    output.text = answer
                    generateButton.isEnabled = true
                }
            }
        }
    }
}
