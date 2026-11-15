package com.phoneflipper.app

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.hardware.ConsumerIrManager

/**
 * Builds a capability report from Android's actual package features and hardware APIs.
 * A positive feature flag means the API/hardware is exposed; it does not guarantee
 * every protocol or operation is supported.
 */
class HardwareCapabilityReport(private val context: Context) {

    fun build(): String {
        val output = StringBuilder()
        val capabilities = DeviceFeatureDetector(context).detectCapabilities()
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)

        output.append("FLIPPER HAMRAH / DEVICE CAPABILITY REPORT\n")
        output.append("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
        output.append("Android API: ${Build.VERSION.SDK_INT}\n")
        output.append("========================================\n\n")

        val groups = capabilities.groupBy { it.category }
        groups.forEach { (category, items) ->
            output.append("[$category]\n")
            items.forEach { item ->
                val status = if (item.supported) "AVAILABLE" else "NOT DETECTED"
                output.append("• ${item.name}: $status\n")
                output.append("  ${item.detail}\n")
            }
            output.append("\n")
        }

        output.append("[SENSORS: ${sensors.size} detected]\n")
        if (sensors.isEmpty()) {
            output.append("No Android sensors were reported by SensorManager.\n")
        } else {
            sensors.forEach { sensor ->
                output.append("• ${sensor.name} (type=${sensor.type})\n")
                output.append("  vendor=${sensor.vendor}, range=${sensor.maximumRange}, resolution=${sensor.resolution}, minDelay=${sensor.minDelay}µs\n")
            }
        }

        output.append("\n[IMPORTANT LIMITATIONS]\n")
        output.append("• Phone IR blasters are transmitters; standard Android APIs do not provide general IR reception.\n")
        output.append("• NFC support does not imply 125 kHz RFID or every access-card protocol.\n")
        output.append("• Sub-GHz RF requires a compatible external radio module, such as ESP32 + CC1101.\n")
        output.append("• Magnetometer readings show local magnetic-field changes; they are not a professional metal detector.\n")
        output.append("• USB Host detection does not guarantee a particular USB adapter or serial chipset is supported.\n")
        output.append("• Feature availability can depend on Android permissions, regional hardware variants and firmware.\n")

        return output.toString()
    }
}
