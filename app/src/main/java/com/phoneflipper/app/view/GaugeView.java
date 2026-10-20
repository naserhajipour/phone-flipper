package com.phoneflipper.app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.view.View;

import com.phoneflipper.app.core.Palette;
import com.phoneflipper.app.core.Ui;

import java.util.Locale;

/**
 * Radial gauge with 240° sweep. Shows a large value, unit, caption and an
 * optional live trend ring. Used by the EMF, audio and scoring screens.
 */
public class GaugeView extends View {

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tick = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint value = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint unit = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint caption = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();

    private float fraction;
    private String valueText = "—";
    private String unitText = "";
    private String captionText = "";
    private int color = Palette.PRIMARY;
    private boolean showTicks = true;

    public GaugeView(Context c) {
        super(c);
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeCap(Paint.Cap.ROUND);
        track.setStrokeWidth(Ui.dp(c, 10));
        track.setColor(Palette.SURFACE_ALT);

        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeCap(Paint.Cap.ROUND);
        arc.setStrokeWidth(Ui.dp(c, 10));

        tick.setStyle(Paint.Style.STROKE);
        tick.setStrokeWidth(Ui.dp(c, 1.4f));
        tick.setColor(Palette.STROKE);

        value.setColor(Palette.TEXT);
        value.setTypeface(Ui.medium());
        value.setTextAlign(Paint.Align.CENTER);

        unit.setColor(Palette.PRIMARY);
        unit.setTypeface(Ui.medium());
        unit.setTextAlign(Paint.Align.CENTER);

        caption.setColor(Palette.TEXT_MUTED);
        caption.setTextAlign(Paint.Align.CENTER);
        caption.setTypeface(Ui.medium());
    }

    public void setValue(float fraction, String valueText, String unitText, String captionText) {
        this.fraction = Math.max(0f, Math.min(1f, fraction));
        this.valueText = valueText;
        this.unitText = unitText == null ? "" : unitText;
        this.captionText = captionText == null ? "" : captionText;
        invalidate();
    }

    public void setAccent(int color) {
        this.color = color;
        invalidate();
    }

    public void setShowTicks(boolean show) {
        this.showTicks = show;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h * 0.52f;
        float pad = Ui.dp(getContext(), 18);
        float radius = Math.min(w - 2 * pad, (h - 2 * pad) * 1.12f) / 2f;
        float stroke = track.getStrokeWidth();
        oval.set(cx - radius, cy - radius, cx + radius, cy + radius);

        // Ticks
        if (showTicks) {
            for (int i = 0; i <= 24; i++) {
                double angle = Math.toRadians(150 + i * 10);
                float inner = radius + stroke * 0.85f;
                float outer = inner + Ui.dp(getContext(), i % 6 == 0 ? 7 : 3.5f);
                float x1 = cx + (float) Math.cos(angle) * inner;
                float y1 = cy + (float) Math.sin(angle) * inner;
                float x2 = cx + (float) Math.cos(angle) * outer;
                float y2 = cy + (float) Math.sin(angle) * outer;
                canvas.drawLine(x1, y1, x2, y2, tick);
            }
        }

        canvas.drawArc(oval, 150, 240, false, track);

        int arcColor = valueColor();
        arc.setShader(null);
        arc.setColor(arcColor);
        canvas.drawArc(oval, 150, 240 * fraction, false, arc);

        // Value block
        float base = cy + radius * 0.10f;
        value.setTextSize(Math.min(w, h) * 0.19f);
        value.setColor(Palette.TEXT);
        canvas.drawText(valueText, cx, base, value);

        unit.setTextSize(Ui.dp(getContext(), 12.5f));
        unit.setColor(arcColor);
        canvas.drawText(unitText.toUpperCase(Locale.US), cx, base + Ui.dp(getContext(), 26), unit);

        caption.setTextSize(Ui.dp(getContext(), 10.5f));
        canvas.drawText(captionText, cx, base + Ui.dp(getContext(), 46), caption);
    }

    private int valueColor() {
        if (fraction >= 0.82f) {
            return Palette.DANGER;
        }
        if (fraction >= 0.55f) {
            return Palette.WARNING;
        }
        return color;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        if (h == 0) {
            h = (int) (w * 0.72f);
        }
        setMeasuredDimension(w, h);
    }
}
