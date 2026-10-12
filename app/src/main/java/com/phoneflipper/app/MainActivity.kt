package com.phoneflipper.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import android.hardware.Sensor
import android.hardware.SensorManager
import kotlin.math.*

class MainActivity : AppCompatActivity() {

    private lateinit var tvReport: TextView
    private lateinit var btnScanAudio: Button
    private lateinit var btnScanCamera: Button
    private lateinit var progress: ProgressBar

    private val REQUEST_RECORD_AUDIO = 2001
    private val REQUEST_CAMERA = 2002

    private val imageCaptureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val bmp = data?.extras?.get("data") as? Bitmap
            if (bmp != null) {
                val reflections = analyzeBitmapForReflections(bmp)
                tvReport.append("\nCamera scan: found $reflections bright spots (possible reflections)")
            } else {
                tvReport.append("\nCamera scan: no image returned")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvReport = findViewById(R.id.tvReport)
        btnScanAudio = findViewById(R.id.btnScanAudio)
        btnScanCamera = findViewById(R.id.btnScanCamera)
        progress = findViewById(R.id.progress)

        title = getString(R.string.app_name)

        showHardwareReport()

        btnScanAudio.setOnClickListener {
            checkAudioPermissionAndScan()
        }
        btnScanCamera.setOnClickListener {
            startCameraPreview()
        }
    }

    private fun showHardwareReport() {
        val sb = StringBuilder()
        val pm = packageManager
        sb.append("Hardware report:\n")
        sb.append("Microphone: ${pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)}\n")
        sb.append("Camera: ${pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)}\n")
        sb.append("USB Host/OTG: ${pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)}\n")
        sb.append("Headphone jack: ${hasHeadphoneJack()}\n")

        val sm = getSystemService(SENSOR_SERVICE) as SensorManager
        fun sensorPresent(type: Int): Boolean = sm.getDefaultSensor(type) != null
        sb.append("Accelerometer: ${sensorPresent(Sensor.TYPE_ACCELEROMETER)}\n")
        sb.append("Gyroscope: ${sensorPresent(Sensor.TYPE_GYROSCOPE)}\n")
        sb.append("Magnetometer: ${sensorPresent(Sensor.TYPE_MAGNETIC_FIELD)}\n")
        sb.append("Light sensor: ${sensorPresent(Sensor.TYPE_LIGHT)}\n")
        sb.append("NFC: ${pm.hasSystemFeature(PackageManager.FEATURE_NFC)}\n")
        sb.append("BLE: ${pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)}\n")
        sb.append("IR: ${pm.hasSystemFeature(PackageManager.FEATURE_CONSUMER_IR)}\n")

        tvReport.text = sb.toString()
    }

    private fun hasHeadphoneJack(): Boolean {
        // best-effort: check AudioManager for wired headset
        val audioManager = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
        return audioManager.isWiredHeadsetOn || audioManager.isWiredHeadsetOn // note: deprecated checks, best-effort
    }

    private fun checkAudioPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
        } else {
            startAudioScan()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startAudioScan()
            } else {
                tvReport.append("\nAudio permission denied")
            }
        }
    }

    private fun startAudioScan() {
        progress.visibility = View.VISIBLE
        tvReport.append("\nAudio scan: recording...\n")
        Thread {
            try {
                val sampleRate = 44100
                val channelConfig = AudioFormat.CHANNEL_IN_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                val recorder = AudioRecord(MediaRecorder.AudioSource.DEFAULT, sampleRate, channelConfig, audioFormat, bufferSize)
                val readSamples = sampleRate // 1 second
                val audioBuffer = ShortArray(readSamples)
                recorder.startRecording()
                var offset = 0
                while (offset < readSamples) {
                    val toRead = min(bufferSize, readSamples - offset)
                    val r = recorder.read(audioBuffer, offset, toRead)
                    if (r <= 0) break
                    offset += r
                }
                recorder.stop()
                recorder.release()

                val floats = ShortArrayToDouble(audioBuffer, offset)
                // compute FFT size as power of two
                val n = 1 shl (ceil(log2(offset.toDouble())).toInt())
                val re = DoubleArray(n)
                val im = DoubleArray(n)
                for (i in 0 until offset) re[i] = floats[i]
                // zero pad
                fft(re, im)
                val magnitudes = DoubleArray(n/2)
                for (i in 0 until n/2) magnitudes[i] = sqrt(re[i]*re[i] + im[i]*im[i])
                var maxIndex = 0
                var maxVal = 0.0
                for (i in 1 until magnitudes.size) {
                    if (magnitudes[i] > maxVal) {
                        maxVal = magnitudes[i]
                        maxIndex = i
                    }
                }
                val dominantFreq = maxIndex * sampleRate.toDouble() / n
                runOnUiThread {
                    tvReport.append(String.format("Audio scan: dominant frequency ~ %.1f Hz\n", dominantFreq))
                    progress.visibility = View.GONE
                }
            } catch (e: Exception) {
                runOnUiThread {
                    tvReport.append("\nAudio scan error: ${e.message}")
                    progress.visibility = View.GONE
                }
            }
        }.start()
    }

    private fun ShortArrayToDouble(shorts: ShortArray, length: Int): DoubleArray {
        val out = DoubleArray(length)
        for (i in 0 until length) out[i] = shorts[i].toDouble()
        return out
    }

    // simple in-place Cooley-Tukey FFT (radix-2)
    private fun fft(re: DoubleArray, im: DoubleArray) {
        val n = re.size
        if (n == 0) return
        var j = 0
        // bit-reverse
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
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
                while (j2 < len/2) {
                    val uRe = re[i + j2]
                    val uIm = im[i + j2]
                    val vRe = re[i + j2 + len/2] * wRe - im[i + j2 + len/2] * wIm
                    val vIm = re[i + j2 + len/2] * wIm + im[i + j2 + len/2] * wRe
                    re[i + j2] = uRe + vRe
                    im[i + j2] = uIm + vIm
                    re[i + j2 + len/2] = uRe - vRe
                    im[i + j2 + len/2] = uIm - vIm
                    val nextWRe = wRe * wlenRe - wIm * wlenIm
                    val nextWIm = wRe * wlenIm + wIm * wlenRe
                    wRe = nextWRe
                    wIm = nextWIm
                    j2++
                }
                i += len
            }
            len = len shl 1
        }
    }

    private fun startCameraPreview() {
        // Use system camera intent (thumbnail) for quick check
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            imageCaptureLauncher.launch(takePictureIntent)
        } else {
            tvReport.append("\nCamera not available")
        }
    }

    private fun analyzeBitmapForReflections(bmp: Bitmap): Int {
        // Count number of very bright pixels blocks as a naive reflection detection
        val w = bmp.width
        val h = bmp.height
        var count = 0
        val stepX = max(4, w / 64)
        val stepY = max(4, h / 64)
        for (x in 0 until w step stepX) {
            for (y in 0 until h step stepY) {
                val p = bmp.getPixel(x, y)
                val r = (p shr 16) and 0xff
                val g = (p shr 8) and 0xff
                val b = p and 0xff
                val lum = 0.2126*r + 0.7152*g + 0.0722*b
                if (lum > 240) count++
            }
        }
        return count
    }

}
