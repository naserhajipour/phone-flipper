package com.phoneflipper.app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

import com.phoneflipper.app.core.Palette;
import com.phoneflipper.app.core.Ui;

/** Compact labelled bar meter with a numeric readout. */
public class MeterView extends View {

    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private String label = "";
    private String value = "";
    private float fraction;
    private int color = Palette.PRIMARY;

    public MeterView(Context c) {
        super(c);
        labelPaint.setColor(Palette.TEXT_DIM);
        labelPaint.setTextSize(Ui.sp(c, 11.5f));
        labelPaint.setTypeface(Ui.medium());
        valuePaint.setColor(Palette.TEXT);
        valuePaint.setTextSize(Ui.sp(c, 11.5f));
        valuePaint.setTypeface(Ui.mono());
        valuePaint.setTextAlign(Paint.Align.RIGHT);
        trackPaint.setColor(Palette.SURFACE_ALT);
        fillPaint.setColor(color);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void set(String label, String value, float fraction, int color) {
        this.label = label;
        this.value = value;
        this.fraction = Math.max(0f, Math.min(1f, fraction));
        this.color = color;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int h = Ui.dp(getContext(), 34);
        setMeasuredDimension(getMeasuredWidth(), h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        float textHeight = Ui.dp(getContext(), 16);
        canvas.drawText(label, 0, textHeight * 0.85f, labelPaint);
        canvas.drawText(value, w, textHeight * 0.85f, valuePaint);

        float top = textHeight + Ui.dp(getContext(), 5);
        float barHeight = Ui.dp(getContext(), 5);
        rect.set(0, top, w, top + barHeight);
        canvas.drawRoundRect(rect, barHeight / 2, barHeight / 2, trackPaint);
        if (fraction > 0) {
            rect.set(0, top, Math.max(barHeight, w * fraction), top + barHeight);
            fillPaint.setShader(new LinearGradient(0, 0, w, 0,
                    Ui.adjustAlpha(color, 170), color, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(rect, barHeight / 2, barHeight / 2, fillPaint);
        }
    }
}
