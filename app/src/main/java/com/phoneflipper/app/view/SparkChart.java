package com.phoneflipper.app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;

import com.phoneflipper.app.core.Palette;
import com.phoneflipper.app.core.Ui;

/** Compact scrolling line chart used for live histories. */
public class SparkChart extends View {

    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Path area = new Path();
    private final float[] values = new float[160];
    private int count;
    private float maxValue = 1f;
    private int color = Palette.PRIMARY;

    public SparkChart(Context c) {
        super(c);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(Ui.dp(c, 2));
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeJoin(Paint.Join.ROUND);
        fillPaint.setStyle(Paint.Style.FILL);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void setColor(int c) {
        this.color = c;
        line.setColor(c);
        invalidate();
    }

    public void reset() {
        count = 0;
        maxValue = 1f;
        invalidate();
    }

    public void push(float value) {
        if (count < values.length) {
            values[count++] = value;
        } else {
            System.arraycopy(values, 1, values, 0, values.length - 1);
            values[values.length - 1] = value;
        }
        maxValue = Math.max(maxValue * 0.995f, Math.max(0.0001f, value));
        invalidate();
    }

    public void setMaximum(float max) {
        maxValue = Math.max(0.0001f, max);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        fillPaint.setShader(new LinearGradient(0, 0, 0, h,
                Ui.adjustAlpha(color, 70), Color.TRANSPARENT, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0 || count < 2) {
            return;
        }
        path.reset();
        area.reset();
        float pad = line.getStrokeWidth();
        for (int i = 0; i < count; i++) {
            float x = pad + (w - 2 * pad) * i / (float) (count - 1);
            float norm = Math.min(1f, values[i] / maxValue);
            float y = h - pad - (h - 2 * pad) * norm;
            if (i == 0) {
                path.moveTo(x, y);
                area.moveTo(x, h);
                area.lineTo(x, y);
            } else {
                path.lineTo(x, y);
                area.lineTo(x, y);
            }
        }
        area.lineTo(pad + (w - 2 * pad) * (count - 1) / (float) (count - 1), h);
        area.close();
        canvas.drawPath(area, fillPaint);
        canvas.drawPath(path, line);
    }
}
