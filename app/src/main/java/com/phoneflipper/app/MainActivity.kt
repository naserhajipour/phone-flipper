package com.phoneflipper.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.*

class MainActivity : AppCompatActivity() {

    private lateinit var tvReport: TextView
    private lateinit var progress: ProgressBar
    private lateinit var btnRefresh: Button
    private lateinit var btnScanAudio: Button
    private lateinit var btnScanCamera: Button
    private lateinit var btnSensorTrace: Button
    private lateinit var btnExport: Button

    private val audioRequestCode = 2001
    private val cameraRequestCode = 2002
    private var sensorTraceText = ""

    private val imageCaptureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val bmp = data?.extras?.get("data") as? Bitmap
            if (bmp != null) {
                val reflectionCount = analyzeBitmapForReflections(bmp)
                tvReport.append("\nCamera scan: $reflectionCount bright reflection-like pixels detected.")
            } else {
                tvReport.append("\nCamera scan: no image returned.")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvReport = findViewById(R.id.tvReport)
        progress = findViewById(R.id.progress)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnScanAudio = findViewById(R.id.btnScanAudio)
        btnScanCamera = findViewById(R.id.btnScanCamera)
        btnSensorTrace = findViewById(R.id.btnSensorTrace)
        btnExport = findViewById(R.id.btnExport)

        title = getString(R.string.app_name)

        btnRefresh.setOnClickListener { renderHardwareReport() }
        btnScanAudio.setOnClickListener { requestAudioAndScan() }
        btnScanCamera.setOnClickListener { requestCameraAndCapture() }
        btnSensorTrace.setOnClickListener { startSensorTrace() }
        btnExport.setOnClickListener { exportHardwareReport() }

        renderHardwareReport()
    }

    private fun renderHardwareReport() {
        val pm = packageManager
        val sm = getSystemService(SensorManager::class.java) as SensorManager
        val summary = StringBuilder()
        summary.append("Hardware report:\n")
        summary.append("Microphone: ${pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)}\n")
        summary.append("Camera: ${pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)}\n")
        summary.append("USB Host / OTG: ${pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)}\n")
        summary.append("Headphone jack: ${hasHeadphoneJack()}\n")
        summary.append("NFC: ${pm.hasSystemFeature(PackageManager.FEATURE_NFC)}\n")
        summary.append("BLE: ${pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)}\n")
        summary.append("IR: ${pm.hasSystemFeature(PackageManager.FEATURE_CONSUMER_IR)}\n")
        summary.append("Accelerometer: ${sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null}\n")
        summary.append("Gyroscope: ${sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null}\n")
        summary.append("Magnetometer: ${sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null}\n")
        summary.append("Light sensor: ${sm.getDefaultSensor(Sensor.TYPE_LIGHT) != null}\n")
        summary.append("\n")
        summary.append(getString(R.string.report_conclusion))

        tvReport.text = summary.toString()
    }

    private fun hasHeadphoneJack(): Boolean {
        val audioManager = getSystemService(android.media.AudioManager::class.java)
        return (audioManager as android.media.AudioManager).isWiredHeadsetOn
    }

    private fun requestAudioAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), audioRequestCode)
        } else {
            startAudioScan()
        }
    }

    private fun requestCameraAndCapture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), cameraRequestCode)
        } else {
            startCameraPreview()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            audioRequestCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startAudioScan()
                } else {
                    tvReport.append("\nAudio scan permission was denied.")
                }
            }
            cameraRequestCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCameraPreview()
                } else {
                    tvReport.append("\nCamera scan permission was denied.")
                }
            }
        }
    }

    private fun startAudioScan() {
        progress.visibility = View.VISIBLE
        tvReport.append("\nAudio scan: recording 1 second...")

        Thread {
            try {
                val sampleRate = 44100
                val channelConfig = AudioFormat.CHANNEL_IN_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                if (bufferSize <= 0) {
                    throw IllegalStateException("Audio buffer size invalid: $bufferSize")
                }

                val recorder = AudioRecord(
                    MediaRecorder.AudioSource.DEFAULT,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
                val readFrames = sampleRate
                val audioBuffer = ShortArray(readFrames)
                recorder.startRecording()
                var offset = 0
                while (offset < readFrames) {
                    val remaining = readFrames - offset
                    val chunk = min(bufferSize, remaining)
                    val read = recorder.read(audioBuffer, offset, chunk)
                    if (read <= 0) break
                    offset += read
                }
                recorder.stop()
                recorder.release()

                val values = DoubleArray(offset)
                for (i in 0 until offset) values[i] = audioBuffer[i].toDouble()
                val fftSize = 1 shl (ceil(log2(offset.toDouble())).toInt())
                val re = DoubleArray(fftSize)
                val im = DoubleArray(fftSize)
                for (i in 0 until min(offset, fftSize)) {
                    re[i] = values[i]
                }
                fft(re, im)

                var maxIndex = 0
                var maxMag = 0.0
                for (i in 1 until fftSize / 2) {
                    val mag = sqrt(re[i] * re[i] + im[i] * im[i])
                    if (mag > maxMag) {
                        maxMag = mag
                        maxIndex = i
                    }
                }
                val dominantHz = maxIndex * sampleRate.toDouble() / fftSize

                runOnUiThread {
                    tvReport.append(String.format(Locale.US, "\nAudio scan result: dominant frequency ≈ %.1f Hz", dominantHz))
                    progress.visibility = View.GONE
                }
            } catch (e: Exception) {
                runOnUiThread {
                    tvReport.append("\nAudio scan failed: ${e.message}")
                    progress.visibility = View.GONE
                }
            }
        }.start()
    }

    private fun fft(re: DoubleArray, im: DoubleArray) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n / 2
            while (j and bit != 0) {
                j = j xor bit
                bit /= 2
            }
            j = j xor bit
            if (i < j) {
                val tempRe = re[i]
                val tempIm = im[i]
                re[i] = re[j]
                im[i] = im[j]
                re[j] = tempRe
                im[j] = tempIm
            }
        }

        var len = 2
        while (len <= n) {
            val ang = -2.0 * Math.PI / len
            val wlenRe = cos(ang)
            val wlenIm = sin(ang)
            var i = 0
            while (i < n) {
                var wRe = 1.0
                var wIm = 0.0
                var j2 = 0
                while (j2 < len / 2) {
                    val uRe = re[i + j2]
                    val uIm = im[i + j2]
                    val vRe = re[i + j2 + len / 2] * wRe - im[i + j2 + len / 2] * wIm
                    val vIm = re[i + j2 + len / 2] * wRe + im[i + j2 + len / 2] * wIm
                    re[i + j2] = uRe + vRe
                    im[i + j2] = uIm + vIm
                    re[i + j2 + len / 2] = uRe - vRe
                    im[i + j2 + len / 2] = uIm - vIm
                    val nextWRe = wRe * wlenRe - wIm * wlenIm
                    val nextWIm = wRe * wlenIm + wIm * wlenRe
                    wRe = nextWRe
                    wIm = nextWIm
                    j2++
                }
                i += len
            }
            len *= 2
        }
    }

    private fun startCameraPreview() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            imageCaptureLauncher.launch(intent)
        } else {
            tvReport.append("\nCamera capture is not available on this device.")
        }
    }

    private fun analyzeBitmapForReflections(bmp: Bitmap): Int {
        val w = bmp.width
        val h = bmp.height
        var count = 0
        val stepX = max(4, w / 64)
        val stepY = max(4, h / 64)
        for (x in 0 until w step stepX) {
            for (y in 0 until h step stepY) {
                val p = bmp.getPixel(x, y)
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                val lum = 0.2126 * r + 0.7152 * g + 0.0722 * b
                if (lum > 240) count++
            }
        }
        return count
    }

    private fun startSensorTrace() {
        val sm = getSystemService(SensorManager::class.java) as SensorManager
        val listeners = mutableListOf<SensorEventListener>()
        var data = "Sensor trace:\n"

        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val mag = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (accel == null && gyro == null && mag == null) {
            tvReport.append("\nNo motion sensors detected on this device.")
            return
        }

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val axis = event.values.joinToString(separator = ", ") { it.toString() }
                data += "${event.sensor.type}: [$axis]\n"
                sensorTraceText = data
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (accel != null) sm.registerListener(sensorListener, accel, SensorManager.SENSOR_DELAY_NORMAL)
        if (gyro != null) sm.registerListener(sensorListener, gyro, SensorManager.SENSOR_DELAY_NORMAL)
        if (mag != null) sm.registerListener(sensorListener, mag, SensorManager.SENSOR_DELAY_NORMAL)

        Thread {
            Thread.sleep(3000)
            runOnUiThread {
                sm.unregisterListener(sensorListener)
                tvReport.append("\n" + sensorTraceText.ifEmpty { "Sensor trace: no reading captured." })
            }
        }.start()
    }

    private fun exportHardwareReport() {
        val reportText = tvReport.text.toString()
        val file = File(filesDir, "flipper_one_report.json")
        try {
            val payload = "{\n  \"device\": \"Flipper One\",\n  \"report\": \"${reportText.replace("\n", "\\n").replace("\"", "\\\"")}\"\n}\n"
            FileOutputStream(file).use { it.write(payload.toByteArray()) }
            tvReport.append("\nReport exported to: ${file.absolutePath}")
        } catch (e: Exception) {
            tvReport.append("\nExport failed: ${e.message}")
        }
    }
}
