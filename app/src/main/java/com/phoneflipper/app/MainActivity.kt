package com.phoneflipper.app

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val title = TextView(this).apply {
            text = "Phone Flipper - Device Capability Check"
            textSize = 22f
            setPadding(0, 0, 0, 12)
        }

        val refreshButton = Button(this).apply {
            text = "Refresh capability report"
        }

        val reportText = TextView(this).apply {
            movementMethod = ScrollingMovementMethod()
            textSize = 15f
            setPadding(0, 16, 0, 0)
            isVerticalScrollBarEnabled = true
            overScrollMode = TextView.OVER_SCROLL_ALWAYS
        }

        root.addView(title)
        root.addView(refreshButton)
        root.addView(reportText)

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)

        refreshButton.setOnClickListener {
            updateReport(reportText)
        }

        updateReport(reportText)
    }

    private fun updateReport(reportText: TextView) {
        val detector = DeviceFeatureDetector(this)
        val capabilities = detector.detectCapabilities()

        val builder = StringBuilder()
        builder.append("Phone capability report\n")
        builder.append("========================\n\n")

        capabilities.forEach { item ->
            val state = if (item.supported) "SUPPORTED" else "NOT SUPPORTED"
            builder.append("[${item.category}] ${item.name}: ${state}\n")
            builder.append("  ${item.detail}\n\n")
        }

        builder.append("\nPractical conclusion:\n")
        builder.append("- Direct Sub-GHz RF TX on a phone is not usually possible with standard Android APIs.\n")
        builder.append("- Use NFC, IR, BLE, USB-OTG, and other available hardware directly when supported.\n")
        builder.append("- For low-frequency RF workflows, use an external radio bridge module connected to the phone via BLE or USB.")

        reportText.text = builder.toString()
    }
}
