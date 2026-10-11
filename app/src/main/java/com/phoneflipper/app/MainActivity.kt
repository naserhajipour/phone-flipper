package com.phoneflipper.app

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.ConsumerIrManager
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.bluetooth.BluetoothAdapter

class MainActivity : AppCompatActivity() {
    private val TAG = "PhoneFlipper"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Simple UI: a scrollable TextView + Refresh button
        val scroll = ScrollView(this)
        val tv = TextView(this)
        tv.setPadding(24,24,24,24)
        val btn = Button(this)
        btn.text = "Refresh"
        btn.setOnClickListener {
            detectFeatures(tv)
        }

        // layout
        val layout = androidx.appcompat.widget.LinearLayoutCompat(this)
        layout.orientation = androidx.appcompat.widget.LinearLayoutCompat.VERTICAL
        layout.addView(btn)
        layout.addView(tv)
        scroll.addView(layout)
        setContentView(scroll)

        detectFeatures(tv)
    }

    private fun detectFeatures(tv: TextView) {
        val pm = packageManager
        val sb = StringBuilder()

        fun add(line: String) {
            sb.append(line).append("\n")
            Log.i(TAG, line)
        }

        add("Phone Flipper — Hardware detection report")
        add("Android SDK: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")

        // NFC
        val hasNfc = pm.hasSystemFeature(PackageManager.FEATURE_NFC)
        add("NFC supported: $hasNfc")
        if (hasNfc) {
            val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
            add("  NFC adapter present: ${nfcAdapter != null}")
            add("  NFC enabled: ${nfcAdapter?.isEnabled}")
            val hasHce = pm.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)
            add("  Host Card Emulation (HCE) support: $hasHce")
        }

        // Consumer IR
        val hasIr = pm.hasSystemFeature(PackageManager.FEATURE_CONSUMER_IR)
        add("IR (Consumer IR) supported: $hasIr")
        if (hasIr) {
            val ir = getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
            add("  ConsumerIrManager present: ${ir != null}")
            try {
                add("  IR carrier frequency range available: ${ir?.carrierFrequencies?.contentToString()}")
            } catch (e: Throwable) {
                // carrierFrequencies is API 33+, may throw on older
            }
        }

        // Bluetooth / BLE
        val hasBluetooth = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        val hasBle = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        add("Bluetooth supported: $hasBluetooth")
        add("Bluetooth LE (BLE) supported: $hasBle")
        try {
            val btAdapter = BluetoothAdapter.getDefaultAdapter()
            add("  BluetoothAdapter available: ${btAdapter != null}")
            if (btAdapter != null) {
                // Accessing isEnabled may require BLUETOOTH_CONNECT on Android 12+, handle safely
                val enabled = try { btAdapter.isEnabled } catch (e: SecurityException) { null }
                add("  Bluetooth enabled (may require permission): $enabled")
            }
        } catch (e: Throwable) {
            add("  Bluetooth adapter check failed: ${e.message}")
        }

        // USB host (OTG)
        val hasUsbHost = pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
        add("USB Host (OTG) supported: $hasUsbHost")

        // Telephony (cellular)
        val hasTelephony = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
        add("Cellular (telephony) supported: $hasTelephony")

        // Wi‑Fi Direct
        val hasWifiDirect = pm.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)
        add("Wi‑Fi Direct supported: $hasWifiDirect")

        // Camera (some devices use camera-based IR or aux)
        val hasCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        add("Camera present: $hasCamera")

        // Other hints
        add("Note: Most phones do NOT have Sub‑GHz transmit hardware (315/433/868/915 MHz). For Sub‑GHz RF you usually need an external module (CC1101, SDR, or Flipper Zero). This app will detect phone features and, when available, use them; otherwise it will connect to an external module over BLE or USB.")

        tv.text = sb.toString()
    }
}
