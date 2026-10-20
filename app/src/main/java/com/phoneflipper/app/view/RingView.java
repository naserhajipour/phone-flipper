package com.phoneflipper.app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.view.View;

import com.phoneflipper.app.core.Palette;
import com.phoneflipper.app.core.Ui;

import java.util.Locale;

/** Progress ring with a large centred readout, used for the capability score. */
public class RingView extends View {

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progress = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint big = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();

    private float fraction;
    private String centre = "0";
    private String label = "";
    private int color = Palette.PRIMARY;

    public RingView(Context c) {
        super(c);
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeCap(Paint.Cap.ROUND);
        track.setStrokeWidth(Ui.dp(c, 9));
        track.setColor(Palette.SURFACE_ALT);

        progress.setStyle(Paint.Style.STROKE);
        progress.setStrokeCap(Paint.Cap.ROUND);
        progress.setStrokeWidth(Ui.dp(c, 9));

        big.setColor(Palette.TEXT);
        big.setTypeface(Ui.medium());
        big.setTextAlign(Paint.Align.CENTER);

        small.setColor(Palette.TEXT_MUTED);
        small.setTypeface(Ui.medium());
        small.setTextAlign(Paint.Align.CENTER);
    }

    public void set(float fraction, String centre, String label, int color) {
        this.fraction = Math.max(0f, Math.min(1f, fraction));
        this.centre = centre;
        this.label = label;
        this.color = color;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float size = Math.min(w, h);
        float stroke = track.getStrokeWidth();
        float radius = (size - stroke) / 2f - Ui.dp(getContext(), 3);
        float cx = w / 2f;
        float cy = h / 2f;
        oval.set(cx - radius, cy - radius, cx + radius, cy + radius);

        canvas.drawArc(oval, 135, 270, false, track);

        SweepGradient gradient = new SweepGradient(cx, cy,
                new int[]{color, Palette.ACCENT, color},
                new float[]{0f, 0.55f, 1f});
        progress.setShader(gradient);
        canvas.drawArc(oval, 135, 270 * fraction, false, progress);

        big.setTextSize(Math.min(w, h) * 0.22f);
        canvas.drawText(centre, cx, cy + big.getTextSize() * 0.34f, big);

        small.setTextSize(Math.min(w, h) * 0.075f);
        canvas.drawText(label.toUpperCase(Locale.US), cx, cy + big.getTextSize() * 0.34f
                + small.getTextSize() * 1.7f, small);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        int d = Math.min(w, h > 0 ? h : w);
        setMeasuredDimension(w, h > 0 ? h : d);
    }
}
