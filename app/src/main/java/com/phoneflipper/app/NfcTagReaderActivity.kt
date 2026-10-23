package com.phoneflipper.app

import android.app.Activity
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.nio.charset.Charset
import java.util.Locale

class NfcTagReaderActivity : Activity(), NfcAdapter.ReaderCallback {
    private var adapter: NfcAdapter? = null
    private lateinit var status: TextView
    private lateinit var details: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
        }
        status = TextView(this).apply {
            textSize = 20f
            text = "NFC: در حال بررسی…"
        }
        details = TextView(this).apply {
            textSize = 16f
            text = "برای خواندن تگ، NFC را روشن کنید و تگ را به پشت گوشی نزدیک کنید. این صفحه فقط اطلاعات قابل‌خواندن را نمایش می‌دهد."
            setPadding(0, 24, 0, 24)
        }
        val back = Button(this).apply {
            text = "بازگشت"
            setOnClickListener { finish() }
        }
        root.addView(status)
        root.addView(details)
        root.addView(back)
        setContentView(ScrollView(this).apply { addView(root) })

        adapter = NfcAdapter.getDefaultAdapter(this)
        when {
            adapter == null -> status.text = "NFC در این گوشی در دسترس نیست."
            adapter?.isEnabled != true -> status.text = "NFC خاموش است؛ آن را از تنظیمات گوشی روشن کنید."
            else -> status.text = "آمادهٔ خواندن تگ NFC"
        }
    }

    override fun onResume() {
        super.onResume()
        val nfc = adapter ?: return
        if (nfc.isEnabled) {
            nfc.enableReaderMode(
                this, this,
                NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                null
            )
            status.text = "آماده‌ام؛ تگ را به پشت گوشی نزدیک کنید."
        }
    }

    override fun onPause() {
        adapter?.disableReaderMode(this)
        super.onPause()
    }

    override fun onTagDiscovered(tag: Tag) {
        val uid = tag.id.joinToString(":") { String.format(Locale.US, "%02X", it.toInt() and 0xFF) }
        val output = StringBuilder()
        output.append("تگ پیدا شد\nUID: ").append(uid)
        output.append("\nفناوری‌ها: ").append(tag.techList.joinToString(", ") { it.substringAfterLast('.') })
        val messages = tag.let { t ->
            try {
                val ndef = android.nfc.tech.Ndef.get(t)
                if (ndef != null) {
                    ndef.connect()
                    val msg = ndef.ndefMessage
                    ndef.close()
                    msg
                } else null
            } catch (_: Exception) { null }
        }
        if (messages != null) {
            output.append("\n\nNDEF records: ").append(messages.records.size)
            messages.records.forEachIndexed { index, record ->
                output.append("\n\nرکورد ").append(index + 1).append(": ")
                output.append(readRecord(record))
            }
        } else {
            output.append("\n\nپیام NDEF قابل‌خواندن پیدا نشد؛ ممکن است تگ از نوع دیگری باشد یا قفل/محافظت شده باشد.")
        }
        runOnUiThread {
            status.text = "تگ خوانده شد"
            details.text = output.toString()
        }
    }

    private fun readRecord(record: NdefRecord): String {
        return try {
            when {
                record.tnf == NdefRecord.TNF_WELL_KNOWN &&
                    record.type.contentEquals(NdefRecord.RTD_TEXT) -> {
                    val p = record.payload
                    val langLength = p[0].toInt() and 0x3F
                    val charset = if ((p[0].toInt() and 0x80) != 0) Charsets.UTF_16 else Charsets.UTF_8
                    String(p, 1 + langLength, p.size - 1 - langLength, charset)
                }
                record.tnf == NdefRecord.TNF_WELL_KNOWN &&
                    record.type.contentEquals(NdefRecord.RTD_URI) -> {
                    val p = record.payload
                    val prefix = arrayOf("", "http://www.", "https://www.", "http://", "https://")
                    val prefixIndex = p.firstOrNull()?.toInt()?.and(0xFF) ?: 0
                    (prefix.getOrNull(prefixIndex) ?: "") + String(p, 1, (p.size - 1).coerceAtLeast(0), Charsets.UTF_8)
                }
                else -> "نوع رکورد: ${record.tnf}، نوع: ${String(record.type, Charsets.UTF_8)}، طول داده: ${record.payload.size} بایت"
            }
        } catch (_: Exception) {
            "رکورد موجود است اما قالب آن پشتیبانی نمی‌شود."
        }
    }
}
