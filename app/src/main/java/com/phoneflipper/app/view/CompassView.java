package com.phoneflipper.app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import com.phoneflipper.app.core.Palette;
import com.phoneflipper.app.core.Ui;

/** Rotating compass dial; the needle stays fixed at the top like a real compass. */
public class CompassView extends View {

    private final Paint dial = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint major = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint minor = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint needle = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();

    private float azimuth;
    private boolean hasHeading;

    public CompassView(Context c) {
        super(c);
        dial.setStyle(Paint.Style.STROKE);
        dial.setStrokeWidth(Ui.dp(c, 1.2f));
        dial.setColor(Palette.STROKE);
        major.setStyle(Paint.Style.STROKE);
        major.setStrokeWidth(Ui.dp(c, 2));
        major.setColor(Palette.TEXT_DIM);
        minor.setStyle(Paint.Style.STROKE);
        minor.setStrokeWidth(Ui.dp(c, 1));
        minor.setColor(Ui.adjustAlpha(Palette.TEXT_MUTED, 120));
        text.setColor(Palette.TEXT);
        text.setTypeface(Ui.medium());
        text.setTextAlign(Paint.Align.CENTER);
        needle.setStyle(Paint.Style.FILL);
        needle.setColor(Palette.DANGER);
    }

    public void setAzimuth(float degrees, boolean hasHeading) {
        this.azimuth = degrees;
        this.hasHeading = hasHeading;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = Math.min(w, h) / 2f - Ui.dp(getContext(), 10);

        canvas.drawCircle(cx, cy, radius, dial);
        canvas.drawCircle(cx, cy, radius * 0.72f, dial);

        for (int deg = 0; deg < 360; deg += 15) {
            double angle = Math.toRadians(deg - azimuth - 90);
            float len = deg % 90 == 0 ? Ui.dp(getContext(), 11) : Ui.dp(getContext(), 5);
            float x1 = cx + (float) Math.cos(angle) * radius;
            float y1 = cy + (float) Math.sin(angle) * radius;
            float x2 = cx + (float) Math.cos(angle) * (radius - len);
            float y2 = cy + (float) Math.sin(angle) * (radius - len);
            canvas.drawLine(x1, y1, x2, y2, deg % 90 == 0 ? major : minor);
        }

        String[] labels = {"N", "E", "S", "W"};
        text.setTextSize(Ui.sp(getContext(), 14));
        for (int i = 0; i < 4; i++) {
            double angle = Math.toRadians(i * 90 - azimuth - 90);
            float r = radius - Ui.dp(getContext(), 27);
            float x = cx + (float) Math.cos(angle) * r;
            float y = cy + (float) Math.sin(angle) * r + text.getTextSize() * 0.35f;
            text.setColor(i == 0 ? Palette.DANGER : Palette.TEXT);
            canvas.drawText(labels[i], x, y, text);
        }

        // Fixed heading marker at the top
        needle.setColor(hasHeading ? Palette.PRIMARY : Palette.TEXT_MUTED);
        float top = cy - radius - Ui.dp(getContext(), 2);
        canvas.drawRoundRect(cx - Ui.dp(getContext(), 5), top - Ui.dp(getContext(), 6),
                cx + Ui.dp(getContext(), 5), top + Ui.dp(getContext(), 3),
                Ui.dp(getContext(), 3), Ui.dp(getContext(), 3), needle);

        text.setColor(Palette.TEXT_MUTED);
        text.setTextSize(Ui.sp(getContext(), 10));
        canvas.drawText(hasHeading ? String.format(java.util.Locale.US, "%03.0f°", azimuth)
                : "—", cx, cy + Ui.dp(getContext(), 4), text);
    }
}
