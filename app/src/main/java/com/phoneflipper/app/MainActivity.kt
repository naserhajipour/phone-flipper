package com.phoneflipper.app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val reportText = findViewById<TextView>(R.id.reportText)
        val refreshButton = findViewById<MaterialButton>(R.id.refreshButton)

        refreshButton.setOnClickListener {
            updateReport(reportText)
        }

        updateReport(reportText)
    }

    private fun updateReport(reportText: TextView) {
        val detector = DeviceFeatureDetector(this)
        val items = detector.detectCapabilities()

        val builder = StringBuilder()
        builder.append("Phone capability report\n")
        builder.append("========================\n\n")

        items.forEach { item ->
            val state = if (item.supported) "SUPPORTED" else "NOT SUPPORTED"
            builder.append("[${item.category}] ${item.name}: ${state}\n")
            builder.append("  ${item.detail}\n\n")
        }

        reportText.text = builder.toString()
    }
}
