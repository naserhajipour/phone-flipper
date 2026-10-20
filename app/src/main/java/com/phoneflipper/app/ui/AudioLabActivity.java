package com.phoneflipper.app.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.phoneflipper.app.R;
import com.phoneflipper.app.core.BaseActivity;
import com.phoneflipper.app.core.Fft;
import com.phoneflipper.app.core.IconDrawable;
import com.phoneflipper.app.core.Palette;
import com.phoneflipper.app.core.Ui;
import com.phoneflipper.app.view.GaugeView;
import com.phoneflipper.app.view.MeterView;
import com.phoneflipper.app.view.SpectrumView;
import com.phoneflipper.app.view.WaveView;

import java.util.Locale;

/** Real-time audio analyser: FFT spectrum, dominant frequency and level. */
public class AudioLabActivity extends BaseActivity {

    private static final int SAMPLE_RATE = 44100;
    private static final int FFT_SIZE = 4096;
    private static final int REQUEST_MIC = 4101;

    private final Handler main = new Handler(Looper.getMainLooper());

    private GaugeView gauge;
    private SpectrumView spectrum;
    private WaveView wave;
    private MeterView level;
    private TextView peakText, rmsText, ultrasonicBadge, toggle;
    private TextView statusPill;

    private volatile boolean capturing;
    private Thread worker;
    private AudioRecord record;
    private final double[] window = new double[FFT_SIZE];
    private final double[] re = new double[FFT_SIZE];
    private final double[] im = new double[FFT_SIZE];
    private final float[] mags = new float[FFT_SIZE / 2];
    private final short[] ring = new short[FFT_SIZE];
    private final float[][] magsOut = {new float[FFT_SIZE / 2], new float[FFT_SIZE / 2]};
    private final short[][] waveOut = {new short[FFT_SIZE], new short[FFT_SIZE]};
    private int bufferSlot;
    private long lastPost;
    private float smoothedDb = -90;

    @Override
    protected void onCreateScreen(Bundle savedInstanceState) {
        Fft.hann(window);

        LinearLayout content = installScreen(R.string.title_audio_lab,
                R.string.subtitle_audio_lab, true);
        addAction(IconDrawable.INFO, R.string.audio_privacy_note, new Runnable() {
            @Override
            public void run() {
                Toast.makeText(AudioLabActivity.this, R.string.audio_privacy_note,
                        Toast.LENGTH_LONG).show();
            }
        });

        LinearLayout header = Ui.row(this);
        statusPill = Ui.badge(this, getString(R.string.status_stopped), Palette.TEXT_MUTED);
        header.addView(statusPill);
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
            header.addView(Ui.badge(this, "44.1 kHz", Palette.PRIMARY));
        }
        content.addView(header);

        TextView intro = Ui.body(this, getString(R.string.audio_intro));
        Ui.add(content, intro, 0, 12, 0, 0);

        // Gauge
        LinearLayout card = Ui.card(this);
        Ui.add(content, card, 0, 20, 0, 0);
        LinearLayout cardHeader = Ui.row(this);
        cardHeader.addView(Ui.text(this, getString(R.string.audio_dominant), 15, Palette.TEXT, true),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ultrasonicBadge = Ui.badge(this, getString(R.string.audio_ultrasonic), Palette.ACCENT);
        ultrasonicBadge.setVisibility(View.GONE);
        cardHeader.addView(ultrasonicBadge);
        card.addView(cardHeader);

        gauge = new GaugeView(this);
        Ui.add(card, gauge, 0, 8, 0, 0);
        gauge.getLayoutParams().height = Ui.dp(this, 190);
        gauge.setAccent(Palette.PRIMARY);
        gauge.setValue(0, "—", "Hz", getString(R.string.audio_waiting));

        peakText = Ui.mono(this, "—", 12, Palette.TEXT_DIM);
        Ui.add(card, peakText, 0, 6, 0, 0);

        // Level
        LinearLayout levelCard = Ui.card(this);
        Ui.add(content, levelCard, 0, 14, 0, 0);
        level = new MeterView(this);
        levelCard.addView(level);
        level.set(getString(R.string.audio_level), "-90 dBFS", 0, Palette.SUCCESS);
        rmsText = Ui.caption(this, getString(R.string.audio_level_hint));
        Ui.add(levelCard, rmsText, 0, 8, 0, 0);

        // Spectrum
        Ui.add(content, Ui.section(this, getString(R.string.audio_spectrum)), 0, 26, 0, 0);
        LinearLayout specCard = Ui.card(this);
        Ui.add(content, specCard, 0, 10, 0, 0);
        spectrum = new SpectrumView(this);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 168));
        specCard.addView(spectrum, sp);
        TextView specHint = Ui.caption(this, getString(R.string.audio_spectrum_hint));
        Ui.add(specCard, specHint, 0, 8, 0, 0);

        // Waveform
        LinearLayout waveCard = Ui.card(this);
        Ui.add(content, waveCard, 0, 14, 0, 0);
        waveCard.addView(Ui.text(this, getString(R.string.audio_waveform), 13,
                Palette.TEXT_DIM, true));
        wave = new WaveView(this);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 72));
        wp.topMargin = Ui.dp(this, 10);
        waveCard.addView(wave, wp);

        toggle = Ui.button(this, getString(R.string.action_start_capture), true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.topMargin = Ui.dp(this, 24);
        content.addView(toggle, tp);
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (capturing) {
                    stopCapture();
                } else {
                    startCapture();
                }
            }
        });

        TextView note = Ui.caption(this, getString(R.string.audio_privacy_note));
        note.setGravity(Gravity.CENTER);
        Ui.add(content, note, 8, 14, 8, 0);
    }

    // ------------------------------------------------------------------ capture

    private void startCapture() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC);
            return;
        }
        begin();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MIC) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                begin();
            } else {
                Toast.makeText(this, R.string.audio_permission_denied, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void begin() {
        capturing = true;
        toggle.setText(R.string.action_stop_capture);
        statusPill.setText(R.string.status_live);
        statusPill.setTextColor(Palette.SUCCESS);
        statusPill.setBackground(Ui.rounded(this, Ui.adjustAlpha(Palette.SUCCESS, 30), 7,
                Ui.adjustAlpha(Palette.SUCCESS, 90), 1));

        worker = new Thread(new Runnable() {
            @Override
            public void run() {
                captureLoop();
            }
        }, "audio-lab");
        worker.start();
    }

    private void captureLoop() {
        int minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int bufferSize = Math.max(minBuffer, FFT_SIZE * 2);
        AudioRecord rec = null;
        try {
            rec = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            if (rec.getState() != AudioRecord.STATE_INITIALIZED) {
                failCapture();
                return;
            }
            record = rec;
            rec.startRecording();
            short[] chunk = new short[1024];
            int filled = 0;
            while (capturing) {
                int read = rec.read(chunk, 0, chunk.length);
                if (read <= 0) {
                    continue;
                }
                if (read >= FFT_SIZE) {
                    System.arraycopy(chunk, read - FFT_SIZE, ring, 0, FFT_SIZE);
                    filled = FFT_SIZE;
                } else {
                    System.arraycopy(ring, read, ring, 0, FFT_SIZE - read);
                    System.arraycopy(chunk, 0, ring, FFT_SIZE - read, read);
                    filled = Math.min(FFT_SIZE, filled + read);
                }
                if (filled >= FFT_SIZE / 2) {
                    analyze(filled);
                }
            }
        } catch (Exception e) {
            failCapture();
        } finally {
            if (rec != null) {
                try {
                    rec.stop();
                } catch (Exception ignored) {
                }
                rec.release();
            }
            record = null;
        }
    }

    private void failCapture() {
        main.post(new Runnable() {
            @Override
            public void run() {
                capturing = false;
                toggle.setText(R.string.action_start_capture);
                statusPill.setText(R.string.status_error);
                statusPill.setTextColor(Palette.DANGER);
                Toast.makeText(AudioLabActivity.this, R.string.audio_error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void analyze(int length) {
        double mean = 0;
        for (int i = 0; i < FFT_SIZE; i++) {
            mean += ring[i];
        }
        mean /= FFT_SIZE;
        double rms = 0;
        for (int i = 0; i < FFT_SIZE; i++) {
            double sample = ring[i] - mean;
            rms += sample * sample;
            re[i] = sample * window[i];
            im[i] = 0;
        }
        rms = Math.sqrt(rms / FFT_SIZE);
        Fft.transform(re, im);

        final float[] copy = magsOut[bufferSlot];
        bufferSlot = 1 - bufferSlot;
        int peakBin = 1;
        float peakMag = 0;
        for (int i = 1; i < FFT_SIZE / 2; i++) {
            float mag = (float) Math.hypot(re[i], im[i]);
            copy[i] = mag;
            if (i > 2 && mag > peakMag) {
                peakMag = mag;
                peakBin = i;
            }
        }
        final float peakHz = peakBin * (float) SAMPLE_RATE / FFT_SIZE;
        final float db = (float) (20 * Math.log10(Math.max(1e-6, rms / 32768.0)));
        final short[] waveCopy = waveOut[bufferSlot == 0 ? 1 : 0];
        System.arraycopy(ring, 0, waveCopy, 0, FFT_SIZE);

        long now = System.currentTimeMillis();
        if (now - lastPost < 110) {
            return;
        }
        lastPost = now;
        main.post(new Runnable() {
            @Override
            public void run() {
                publish(copy, peakHz, db, waveCopy);
            }
        });
    }

    private void publish(float[] magsCopy, float peakHz, float db, short[] waveCopy) {
        smoothedDb = Math.max(db, smoothedDb * 0.82f + db * 0.18f);
        float dbFraction = Math.max(0f, Math.min(1f, (smoothedDb + 90) / 90f));
        level.set(getString(R.string.audio_level),
                String.format(Locale.US, "%.0f dBFS", smoothedDb), dbFraction,
                dbFraction > 0.86f ? Palette.DANGER : (dbFraction > 0.6f ? Palette.WARNING : Palette.SUCCESS));

        float norm = (float) (Math.log(Math.max(20f, peakHz) / 20f) / Math.log(20000 / 20.0));
        gauge.setValue(norm, peakHz >= 1000
                        ? String.format(Locale.US, "%.1f", peakHz / 1000f)
                        : String.format(Locale.US, "%.0f", peakHz),
                peakHz >= 1000 ? "kHz" : "Hz",
                peakHz > 16000 ? getString(R.string.audio_ultrasonic_hint)
                        : getString(R.string.audio_dominant_caption));
        peakText.setText(String.format(Locale.US, "%s %.1f Hz · %s %.0f dBFS",
                getString(R.string.label_peak), peakHz, getString(R.string.audio_rms), db));
        boolean ultrasonic = peakHz > 16000 && smoothedDb > -70;
        ultrasonicBadge.setVisibility(ultrasonic ? View.VISIBLE : View.GONE);
        spectrum.setMagnitudes(magsCopy, FFT_SIZE, SAMPLE_RATE);
        wave.setSamples(waveCopy, FFT_SIZE);
    }

    private void stopCapture() {
        capturing = false;
        // The capture thread owns the AudioRecord and releases it in its own
        // finally block: joining here would stall the UI thread on rec.read().
        worker = null;
        toggle.setText(R.string.action_start_capture);
        statusPill.setText(R.string.status_stopped);
        statusPill.setTextColor(Palette.TEXT_MUTED);
        statusPill.setBackground(Ui.rounded(this, Ui.adjustAlpha(Palette.TEXT_MUTED, 30), 7,
                Ui.adjustAlpha(Palette.TEXT_MUTED, 90), 1));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (capturing) {
            stopCapture();
        }
    }
}
