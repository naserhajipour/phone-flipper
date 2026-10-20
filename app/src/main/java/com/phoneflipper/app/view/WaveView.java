package com.phoneflipper.app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import com.phoneflipper.app.core.Palette;
import com.phoneflipper.app.core.Ui;

/** Oscilloscope-style waveform drawn from the most recent PCM window. */
public class WaveView extends View {

    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centre = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private short[] samples;
    private int count;

    public WaveView(Context c) {
        super(c);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(Ui.dp(c, 1.6f));
        line.setColor(Palette.PRIMARY);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeJoin(Paint.Join.ROUND);
        centre.setStyle(Paint.Style.STROKE);
        centre.setStrokeWidth(Ui.dp(c, 0.8f));
        centre.setColor(Palette.STROKE);
    }

    public void setSamples(short[] data, int length) {
        this.samples = data;
        this.count = length;
        invalidate();
    }

    public void clear() {
        samples = null;
        count = 0;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        canvas.drawLine(0, h / 2f, w, h / 2f, centre);
        if (samples == null || count < 2) {
            return;
        }
        path.reset();
        // Draw at most one point per horizontal pixel.
        int points = Math.min((int) w, 512);
        for (int i = 0; i < points; i++) {
            int idx = (int) ((long) i * count / points);
            if (idx >= count) {
                idx = count - 1;
            }
            float x = w * i / (float) (points - 1);
            float y = h / 2f - (samples[idx] / 32768f) * (h / 2f) * 0.82f;
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        canvas.drawPath(path, line);
    }
}
