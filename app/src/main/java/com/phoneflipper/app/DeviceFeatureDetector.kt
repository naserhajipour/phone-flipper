package com.phoneflipper.app

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.ConsumerIrManager
import android.nfc.NfcAdapter
import android.os.Build
import android.util.Log
import android.bluetooth.BluetoothAdapter

private const val TAG = "DeviceDetector"

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
                    "Adapter present = ${adapter != null}; HCE available = ${pm.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)}"
                } else {
                    "Phone does not expose NFC support."
                },
                category = "NFC"
            )
        )

        val hasHce = pm.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)
        list.add(
            CapabilityItem(
                name = "Host Card Emulation (HCE)",
                supported = hasHce,
                detail = if (hasHce) "Device supports NFC emulation for supported card scenarios." else "HCE not available on this device.",
                category = "NFC"
            )
        )

        val hasIr = pm.hasSystemFeature(PackageManager.FEATURE_CONSUMER_IR)
        val irManager = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
        list.add(
            CapabilityItem(
                name = "Infrared (IR)",
                supported = hasIr,
                detail = if (hasIr) {
                    "IR emitter available; carrier range = ${irManager?.carrierFrequencies?.joinToString() ?: "unknown"}"
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
                detail = if (hasBle) "BLE is available for communication with external bridge hardware." else "No BLE support detected.",
                category = "Connectivity"
            )
        )

        val hasBt = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        list.add(
            CapabilityItem(
                name = "Bluetooth Classic",
                supported = hasBt,
                detail = if (hasBt) "Classic Bluetooth available." else "Classic Bluetooth not available.",
                category = "Connectivity"
            )
        )

        val hasUsbHost = pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
        list.add(
            CapabilityItem(
                name = "USB Host (OTG)",
                supported = hasUsbHost,
                detail = if (hasUsbHost) "Phone can act as USB host and may attach external dongles." else "No OTG host support detected.",
                category = "Connectivity"
            )
        )

        val hasCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        list.add(
            CapabilityItem(
                name = "Camera",
                supported = hasCamera,
                detail = if (hasCamera) "Camera exists; this can support optical or vision-based workflows if needed." else "No camera found.",
                category = "Sensors"
            )
        )

        val hasTelephony = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
        list.add(
            CapabilityItem(
                name = "Cellular / Telephony",
                supported = hasTelephony,
                detail = if (hasTelephony) "Cellular modem available." else "No telephony hardware detected.",
                category = "Connectivity"
            )
        )

        val hasWifiDirect = pm.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)
        list.add(
            CapabilityItem(
                name = "Wi‑Fi Direct",
                supported = hasWifiDirect,
                detail = if (hasWifiDirect) "Wi‑Fi Direct available for peer-to-peer communication." else "No Wi‑Fi Direct support detected.",
                category = "Connectivity"
            )
        )

        // Sub-GHz is not a standard Android feature; most devices won't expose it.
        list.add(
            CapabilityItem(
                name = "Sub‑GHz RF (315/433/868/915 MHz)",
                supported = false,
                detail = "Most phones do not have direct built-in RF TX hardware for low-frequency bands. Use an external module such as CC1101 or SDR to bridge from the phone.",
                category = "RF"
            )
        )

        val btAdapter = try {
            BluetoothAdapter.getDefaultAdapter()
        } catch (e: SecurityException) {
            Log.w(TAG, "Bluetooth adapter access requires permission", e)
            null
        }
        list.add(
            CapabilityItem(
                name = "Bluetooth Adapter",
                supported = btAdapter != null,
                detail = if (btAdapter != null) "Adapter available, state = ${btAdapter.state}" else "No bluetooth adapter discovered.",
                category = "Connectivity"
            )
        )

        val supportedCount = list.count { it.supported }
        list.add(
            CapabilityItem(
                name = "Phone capability summary",
                supported = supportedCount > 0,
                detail = "This phone supports ${supportedCount} major feature groups directly. For full RF-like Flipper workflows, external hardware is still required for Sub‑GHz operations.",
                category = "Summary"
            )
        )

        return list
    }
}
