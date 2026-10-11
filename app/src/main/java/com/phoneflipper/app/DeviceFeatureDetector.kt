package com.phoneflipper.app

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.ConsumerIrManager
import android.nfc.NfcAdapter
import android.os.Build
import android.util.Log
import android.bluetooth.BluetoothAdapter

data class CapabilityItem(
    val name: String,
    val supported: Boolean,
    val detail: String,
    val category: String = "General"
)

class DeviceFeatureDetector(private val context: Context) {

    fun detectCapabilities(): List<CapabilityItem> {
        val pm = context.packageManager
        val list = mutableListOf<CapabilityItem>()

        val hasNfc = pm.hasSystemFeature(PackageManager.FEATURE_NFC)
        list.add(
            CapabilityItem(
                name = "NFC",
                supported = hasNfc,
                detail = if (hasNfc) {
                    val adapter = NfcAdapter.getDefaultAdapter(context)
                    "Adapter present = ${adapter != null}; HCE support = ${pm.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)}"
                } else {
                    "This phone does not expose NFC support."
                },
                category = "NFC"
            )
        )

        val hasHce = pm.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)
        list.add(
            CapabilityItem(
                name = "HCE",
                supported = hasHce,
                detail = if (hasHce) "Phone supports Host Card Emulation." else "HCE is unavailable on this device.",
                category = "NFC"
            )
        )

        val hasIr = pm.hasSystemFeature(PackageManager.FEATURE_CONSUMER_IR)
        val irManager = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
        list.add(
            CapabilityItem(
                name = "Infrared",
                supported = hasIr,
                detail = if (hasIr) {
                    "Consumer IR hardware available. Carrier frequencies: ${irManager?.carrierFrequencies?.joinToString() ?: "unknown"}"
                } else {
                    "No Consumer IR hardware detected."
                },
                category = "IR"
            )
        )

        val hasBle = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        list.add(
            CapabilityItem(
                name = "Bluetooth LE",
                supported = hasBle,
                detail = if (hasBle) "BLE is available for communication with an external bridge." else "No BLE support detected.",
                category = "Connectivity"
            )
        )

        val hasBt = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        list.add(
            CapabilityItem(
                name = "Bluetooth Classic",
                supported = hasBt,
                detail = if (hasBt) "Classic Bluetooth is available." else "Classic Bluetooth is not available.",
                category = "Connectivity"
            )
        )

        val hasUsbHost = pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
        list.add(
            CapabilityItem(
                name = "USB Host / OTG",
                supported = hasUsbHost,
                detail = if (hasUsbHost) "The phone can act as USB Host and attach external dongles." else "No OTG host support detected.",
                category = "Connectivity"
            )
        )

        val hasCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        list.add(
            CapabilityItem(
                name = "Camera",
                supported = hasCamera,
                detail = if (hasCamera) "Camera hardware is present." else "No camera was detected.",
                category = "Sensors"
            )
        )

        val hasTelephony = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
        list.add(
            CapabilityItem(
                name = "Cellular",
                supported = hasTelephony,
                detail = if (hasTelephony) "Cellular modem is present." else "No cellular hardware found.",
                category = "Connectivity"
            )
        )

        val hasWifiDirect = pm.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)
        list.add(
            CapabilityItem(
                name = "Wi‑Fi Direct",
                supported = hasWifiDirect,
                detail = if (hasWifiDirect) "Wi‑Fi Direct is available for peer-to-peer communication." else "Wi‑Fi Direct support is not available.",
                category = "Connectivity"
            )
        )

        list.add(
            CapabilityItem(
                name = "Sub‑GHz RF",
                supported = false,
                detail = "Standard Android phones usually do not expose direct 315/433/868/915 MHz TX hardware. You should use an external radio bridge such as ESP32 + CC1101.",
                category = "RF"
            )
        )

        val btAdapter = try {
            BluetoothAdapter.getDefaultAdapter()
        } catch (e: SecurityException) {
            Log.w("PhoneFlipper", "Bluetooth adapter access requires runtime permission", e)
            null
        }

        list.add(
            CapabilityItem(
                name = "Bluetooth adapter",
                supported = btAdapter != null,
                detail = if (btAdapter != null) "Bluetooth adapter available; state = ${btAdapter.state}" else "No Bluetooth adapter discovered.",
                category = "Connectivity"
            )
        )

        list.add(
            CapabilityItem(
                name = "Device summary",
                supported = list.count { it.supported } > 0,
                detail = "This device supports ${list.count { it.supported }} major feature groups directly. For full Flipper-like RF workflows, external hardware is still required.",
                category = "Summary"
            )
        )

        return list
    }
}
