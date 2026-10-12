package com.phoneflipper.app

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log

// Simple BLE scanning scaffold: discover nearby BLE devices (for users who use ESP32 BLE mode)
class BLEClient(private val context: Context) {
    private val TAG = "BLEClient"

    fun startScan(onDeviceFound: (String, String) -> Unit) {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
        val scanner = adapter.bluetoothLeScanner ?: return
        scanner.startScan(object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.device?.let { dev ->
                    val name = dev.name ?: ""
                    val addr = dev.address
                    Log.i(TAG, "Found BLE: $name $addr")
                    onDeviceFound(name, addr)
                }
            }
        })
    }

    fun stopScan() {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
        val scanner = adapter.bluetoothLeScanner ?: return
        try { scanner.stopScan(object : ScanCallback() {}) } catch (_: Exception) {}
    }
}
