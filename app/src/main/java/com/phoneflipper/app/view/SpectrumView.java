package com.phoneflipper.app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;

import com.phoneflipper.app.core.Palette;
import com.phoneflipper.app.core.Ui;

import java.util.Locale;

/**
 * Log-frequency spectrum display (40 Hz – 20 kHz) with a peak marker.
 * Magnitudes are expected as linear FFT magnitudes, already Hann-windowed.
 */
public class SpectrumView extends View {

    private static final float MIN_HZ = 40f;
    private static final float MAX_HZ = 20000f;
    private static final int BARS = 44;

    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barDim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint peakPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] levels = new float[BARS];
    private float[] source;
    private int sourceSize;
    private float sampleRate = 44100f;
    private float peakHz;
    private float peakLevel;

    public SpectrumView(Context c) {
        super(c);
        bar.setStyle(Paint.Style.FILL);
        barDim.setStyle(Paint.Style.FILL);
        grid.setStyle(Paint.Style.STROKE);
        grid.setStrokeWidth(Ui.dp(c, 0.8f));
        grid.setColor(Palette.STROKE_SOFT);
        label.setColor(Palette.TEXT_MUTED);
        label.setTextSize(Ui.sp(c, 9));
        label.setTypeface(Ui.medium());
        peakPaint.setStyle(Paint.Style.STROKE);
        peakPaint.setStrokeWidth(Ui.dp(c, 1.4f));
        peakPaint.setColor(Palette.ACCENT);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    /** Feed a magnitude array (index 0 = DC). */
    public void setMagnitudes(float[] mags, int size, float rate) {
        this.source = mags;
        this.sourceSize = size;
        this.sampleRate = rate;
        compute();
    }

    public void clear() {
        source = null;
        for (int i = 0; i < BARS; i++) {
            levels[i] = 0;
        }
        peakHz = 0;
        peakLevel = 0;
        invalidate();
    }

    private void compute() {
        if (source == null || sourceSize < 8) {
            return;
        }
        float binHz = sampleRate / sourceSize;
        float max = 0.000001f;
        float[] raw = new float[BARS];
        for (int i = 0; i < BARS; i++) {
            float f0 = freqAt(i);
            float f1 = freqAt(i + 1);
            int b0 = Math.max(1, (int) (f0 / binHz));
            int b1 = Math.min(sourceSize / 2 - 1, Math.max(b0 + 1, (int) (f1 / binHz)));
            float sum = 0;
            int n = 0;
            for (int b = b0; b <= b1; b++) {
                sum += source[b];
                n++;
            }
            raw[i] = n == 0 ? 0 : sum / n;
            max = Math.max(max, raw[i]);
        }
        int peakBin = 1;
        float peakMag = 0;
        for (int b = 1; b < sourceSize / 2; b++) {
            if (source[b] > peakMag) {
                peakMag = source[b];
                peakBin = b;
            }
        }
        peakHz = peakBin * binHz;
        peakLevel = max > 0 ? peakMag / max : 0;

        for (int i = 0; i < BARS; i++) {
            float norm = (float) (Math.log10(1 + 9 * (raw[i] / max))); // soft-knee compression
            levels[i] = Math.max(levels[i] * 0.72f, Math.min(1f, norm));
        }
        invalidate();
    }

    private static float freqAt(int index) {
        return (float) (MIN_HZ * Math.pow(MAX_HZ / MIN_HZ, index / (double) BARS));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float bottom = h - Ui.dp(getContext(), 16);
        float top = Ui.dp(getContext(), 10);
        float usable = bottom - top;

        for (int hz : new int[]{100, 1000, 10000}) {
            float x = xFor(hz, w);
            canvas.drawLine(x, top, x, bottom, grid);
            String text = hz >= 1000 ? (hz / 1000) + "k" : String.valueOf(hz);
            canvas.drawText(text, x + Ui.dp(getContext(), 3), h - Ui.dp(getContext(), 3), label);
        }

        float gap = Ui.dp(getContext(), 1.6f);
        float barWidth = (w - gap * (BARS - 1)) / (float) BARS;
        if (barWidth < 1) {
            barWidth = 1;
        }
        for (int i = 0; i < BARS; i++) {
            float x = i * (barWidth + gap);
            float level = levels[i];
            float bh = Math.max(Ui.dp(getContext(), 1.5f), usable * level);
            float fraction = i / (float) BARS;
            int color = fraction > 0.8f ? Palette.ACCENT : Palette.PRIMARY;
            bar.setColor(Ui.adjustAlpha(color, 210));
            barDim.setColor(Ui.adjustAlpha(color, 55));
            canvas.drawRoundRect(x, bottom - usable, x + barWidth, bottom, barWidth / 2,
                    barWidth / 2, barDim);
            canvas.drawRoundRect(x, bottom - bh, x + barWidth, bottom, barWidth / 2,
                    barWidth / 2, bar);
        }

        if (peakHz > 0 && peakLevel > 0.02f) {
            float px = xFor(peakHz, w);
            canvas.drawLine(px, top, px, bottom, peakPaint);
            String caption = peakHz >= 1000
                    ? String.format(Locale.US, "%.1f kHz", peakHz / 1000f)
                    : String.format(Locale.US, "%.0f Hz", peakHz);
            label.setColor(Palette.ACCENT);
            float tw = label.measureText(caption);
            canvas.drawText(caption, Math.max(0, Math.min(w - tw, px - tw / 2)),
                    top - Ui.dp(getContext(), 1), label);
            label.setColor(Palette.TEXT_MUTED);
        }
    }

    private float xFor(float hz, int width) {
        double norm = Math.log(hz / MIN_HZ) / Math.log(MAX_HZ / MIN_HZ);
        return (float) (norm * width);
    }
}
