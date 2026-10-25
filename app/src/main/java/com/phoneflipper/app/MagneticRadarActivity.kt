package com.phoneflipper.app

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale
import kotlin.math.sqrt

/**
 * Live magnetometer viewer. Readings are environmental magnetic-field values,
 * not a metal/gold detector and not a reliable material identifier.
 */
class MagneticRadarActivity : Activity(), SensorEventListener {
    private lateinit var manager: SensorManager
    private var magneticSensor: Sensor? = null
    private lateinit var valueText: TextView
    private lateinit var statusText: TextView
    private var baseline: Double? = null
    private var lastMagnitude = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        manager = getSystemService(SENSOR_SERVICE) as SensorManager
        magneticSensor = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(32, 32, 32, 32)
        }
        root.addView(TextView(this).apply {
            text = "رادار مغناطیسی زنده"
            textSize = 25f
        })
        valueText = TextView(this).apply {
            text = "-- µT"
            textSize = 34f
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 24)
        }
        root.addView(valueText)
        statusText = TextView(this).apply {
            textSize = 16f
            text = "برای شروع، گوشی را دور از آهن‌ربا و فلزات بزرگ نگه دارید."
            setPadding(0, 0, 0, 24)
        }
        root.addView(statusText)
        root.addView(Button(this).apply {
            text = "ثبت مقدار مبنا"
            setOnClickListener {
                baseline = lastMagnitude
                updateReading(floatArrayOf(0f, 0f, 0f), lastMagnitude)
                statusText.text = String.format(
                    Locale.US,
                    "مقدار مبنا ثبت شد: %.1f µT. تغییرات بعدی نسبت به همین مقدار نمایش داده می‌شود.",
                    lastMagnitude
                )
            }
        })
        root.addView(Button(this).apply {
            text = "پاک‌کردن مقدار مبنا"
            setOnClickListener {
                baseline = null
                statusText.text = "مقدار مبنا پاک شد."
            }
        })
        root.addView(Button(this).apply {
            text = "بازگشت"
            setOnClickListener { finish() }
        })
        setContentView(root)

        if (magneticSensor == null) {
            valueText.text = "حسگر موجود نیست"
            statusText.text = "این دستگاه حسگر میدان مغناطیسی قابل دسترسی ندارد."
        }
    }

    override fun onResume() {
        super.onResume()
        magneticSensor?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        manager.unregisterListener(this)
        super.onPause()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_MAGNETIC_FIELD || event.values.size < 3) return
        val x = event.values[0].toDouble()
        val y = event.values[1].toDouble()
        val z = event.values[2].toDouble()
        lastMagnitude = sqrt(x * x + y * y + z * z)
        valueText.text = String.format(Locale.US, "%.1f µT", lastMagnitude)
        val delta = baseline?.let { lastMagnitude - it }
        statusText.text = if (delta == null) {
            String.format(Locale.US, "X %.1f · Y %.1f · Z %.1f µT\nمقدار مبنا ثبت نشده است.", x, y, z)
        } else {
            String.format(Locale.US, "X %.1f · Y %.1f · Z %.1f µT\nتغییر نسبت به مبنا: %+.1f µT", x, y, z, delta)
        }
    }

    private fun updateReading(values: FloatArray, magnitude: Double) {
        valueText.text = String.format(Locale.US, "%.1f µT", magnitude)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
