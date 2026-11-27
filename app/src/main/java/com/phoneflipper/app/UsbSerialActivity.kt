package com.phoneflipper.app

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import androidx.appcompat.app.AppCompatActivity

/** USB host inspector and basic CDC-ACM serial terminal. Not a universal USB-UART driver. */
class UsbSerialActivity : AppCompatActivity() {
    private lateinit var usb: UsbManager
    private lateinit var status: TextView
    private lateinit var log: TextView
    private lateinit var command: EditText
    private var connection: UsbDeviceConnection? = null
    private var dataInterface: UsbInterface? = null
    private var inputEndpoint: UsbEndpoint? = null
    private var outputEndpoint: UsbEndpoint? = null
    private val reading = AtomicBoolean(false)
    private var receiverRegistered = false
    private val permissionAction = "com.phoneflipper.app.USB_PERMISSION"

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != permissionAction) return
            val device = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            }
            if (device != null && intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) connectCdcAcm(device)
            else status.text = "مجوز USB داده نشد."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usb = getSystemService(Context.USB_SERVICE) as UsbManager
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 20, 20, 20) }
        status = TextView(this).apply { textSize = 16f; text = "برای بررسی تجهیزات، دستگاه را با OTG متصل کنید." }
        val refresh = Button(this).apply { text = "جست‌وجوی تجهیزات USB"; setOnClickListener { showDevices() } }
        command = EditText(this).apply { hint = "متن فرمان سریال" }
        val send = Button(this).apply { text = "ارسال فرمان"; setOnClickListener { sendLine(command.text.toString()) } }
        val disconnect = Button(this).apply { text = "قطع اتصال"; setOnClickListener { closeConnection(); status.text = "اتصال قطع شد." } }
        log = TextView(this).apply { textSize = 14f; text = "گزارش ارتباط در این قسمت نمایش داده می‌شود."; setTextIsSelectable(true) }
        root.addView(status); root.addView(refresh); root.addView(command); root.addView(send); root.addView(disconnect); root.addView(log)
        setContentView(ScrollView(this).apply { addView(root) })
        val filter = IntentFilter(permissionAction)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(permissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else {
            @Suppress("DEPRECATION")
            registerReceiver(permissionReceiver, filter)
        }
        receiverRegistered = true
        showDevices()
    }

    private fun showDevices() {
        val devices = usb.deviceList.values.toList()
        if (devices.isEmpty()) {
            status.text = "هیچ دستگاه USB شناسایی نشد. OTG، کابل و تبدیل را بررسی کنید."
            log.text = "USB Host فعال است؛ دستگاه متصل پیدا نشد."
            return
        }
        log.text = devices.joinToString("\n\n") { d ->
            val interfaces = (0 until d.interfaceCount).joinToString { i ->
                val intf = d.getInterface(i)
                "if#$i class=\${intf.interfaceClass} subclass=\${intf.interfaceSubclass}"
            }
            "نام: \${d.deviceName}\nVID: %04X  PID: %04X\nاینترفیس‌ها: %s".format(d.vendorId, d.productId, interfaces)
        }
        val candidate = devices.firstOrNull { findCdcDataInterface(it) != null }
        status.text = if (candidate != null) "دستگاه CDC-ACM محتمل پیدا شد." else
            "دستگاه USB پیدا شد، اما رابط CDC-ACM استاندارد تشخیص داده نشد؛ ممکن است درایور اختصاصی لازم باشد."
        if (candidate != null) {
            val connect = Button(this).apply {
                text = "اتصال به \${candidate.deviceName}"
                setOnClickListener { requestUsbPermission(candidate) }
            }
            (status.parent as? LinearLayout)?.let { parent ->
                if (parent.childCount > 1) parent.removeViewAt(1)
                parent.addView(connect, 1)
            }
        }
    }

    private fun requestUsbPermission(device: UsbDevice) {
        if (usb.hasPermission(device)) { connectCdcAcm(device); return }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
        val pending = PendingIntent.getBroadcast(this, 0, Intent(permissionAction).setPackage(packageName), flags)
        usb.requestPermission(device, pending)
        status.text = "منتظر تأیید مجوز USB از طرف اندروید…"
    }

    private fun findCdcDataInterface(device: UsbDevice): UsbInterface? {
        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            if (intf.interfaceClass == 10) return intf
        }
        return null
    }

    private fun connectCdcAcm(device: UsbDevice) {
        closeConnection()
        val data = findCdcDataInterface(device)
        if (data == null) { status.text = "این دستگاه رابط CDC Data ندارد."; return }
        val endpoints = (0 until data.endpointCount).map { data.getEndpoint(it) }
        val inEp = endpoints.firstOrNull { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK && it.direction == UsbConstants.USB_DIR_IN }
        val outEp = endpoints.firstOrNull { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK && it.direction == UsbConstants.USB_DIR_OUT }
        if (inEp == null || outEp == null) { status.text = "Bulk IN/OUT endpoint پیدا نشد."; return }
        val conn = usb.openDevice(device)
        if (conn == null || !conn.claimInterface(data, true)) {
            conn?.close(); status.text = "بازکردن یا claim کردن رابط USB ناموفق بود."; return
        }
        connection = conn; dataInterface = data; inputEndpoint = inEp; outputEndpoint = outEp
        val control = (0 until device.interfaceCount).map { device.getInterface(it) }.firstOrNull { it.interfaceClass == 2 }
        if (control != null) {
            val lineCoding = ByteBuffer.allocate(7).order(ByteOrder.LITTLE_ENDIAN).putInt(115200).put(0).put(8).array()
            conn.controlTransfer(0x21, 0x20, 0, control.id, lineCoding, 7, 1000)
            conn.controlTransfer(0x21, 0x22, 1, control.id, null, 0, 1000)
        }
        status.text = "اتصال CDC-ACM برقرار شد (115200, 8-N-1)."
        appendLog("\n\n--- USB connected: \${device.deviceName} ---")
        startReader()
    }

    private fun startReader() {
        val conn = connection ?: return
        val endpoint = inputEndpoint ?: return
        reading.set(true)
        thread(name = "usb-cdc-reader") {
            val buffer = ByteArray(maxOf(64, endpoint.maxPacketSize * 4))
            while (reading.get()) {
                val count = conn.bulkTransfer(endpoint, buffer, buffer.size, 250)
                if (count > 0) {
                    val received = String(buffer, 0, count, Charsets.UTF_8)
                    runOnUiThread { appendLog(received) }
                }
            }
        }
    }

    private fun sendLine(value: String) {
        val conn = connection; val endpoint = outputEndpoint
        if (conn == null || endpoint == null) { status.text = "ابتدا دستگاه سریال سازگار را متصل کنید."; return }
        val bytes = (value + "\n").toByteArray(Charsets.UTF_8)
        thread(name = "usb-cdc-writer") {
            val sent = conn.bulkTransfer(endpoint, bytes, bytes.size, 1500)
            runOnUiThread { appendLog(if (sent >= 0) "\n> $value" else "\nارسال ناموفق بود.") }
        }
    }

    private fun appendLog(value: String) {
        log.append(value)
        if (log.length() > 12000) log.text = log.text.takeLast(8000)
    }

    private fun closeConnection() {
        reading.set(false)
        try { dataInterface?.let { connection?.releaseInterface(it) } } catch (_: Exception) { }
        connection?.close()
        connection = null; dataInterface = null; inputEndpoint = null; outputEndpoint = null
    }

    override fun onDestroy() {
        closeConnection()
        if (receiverRegistered) unregisterReceiver(permissionReceiver)
        super.onDestroy()
    }
}
