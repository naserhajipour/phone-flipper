package com.phoneflipper.app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import com.phoneflipper.app.core.Palette;
import com.phoneflipper.app.core.Ui;

/** Bubble level driven by the accelerometer's X/Y axes. */
public class LevelView extends View {

    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cross = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bubble = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private float x;
    private float y;

    public LevelView(Context c) {
        super(c);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(Ui.dp(c, 1.5f));
        ring.setColor(Palette.STROKE);
        cross.setStyle(Paint.Style.STROKE);
        cross.setStrokeWidth(Ui.dp(c, 1));
        cross.setColor(Ui.adjustAlpha(Palette.TEXT_MUTED, 90));
        bubble.setStyle(Paint.Style.FILL);
        bubble.setColor(Palette.PRIMARY);
        glow.setStyle(Paint.Style.FILL);
        glow.setColor(Ui.adjustAlpha(Palette.PRIMARY, 40));
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void setTilt(float x, float y) {
        this.x = Math.max(-1f, Math.min(1f, x));
        this.y = Math.max(-1f, Math.min(1f, y));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float size = Math.min(w, h);
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = size / 2f - Ui.dp(getContext(), 4);
        boolean level = Math.abs(x) < 0.06f && Math.abs(y) < 0.06f;

        rect.set(cx - radius, cy - radius, cx + radius, cy + radius);
        canvas.drawOval(rect, ring);
        rect.set(cx - radius * 0.55f, cy - radius * 0.55f, cx + radius * 0.55f,
                cy + radius * 0.55f);
        canvas.drawOval(rect, cross);
        canvas.drawLine(cx - radius, cy, cx + radius, cy, cross);
        canvas.drawLine(cx, cy - radius, cx, cy + radius, cross);

        float bx = cx + x * radius * 0.8f;
        float by = cy + y * radius * 0.8f;
        int color = level ? Palette.SUCCESS : Palette.PRIMARY;
        bubble.setColor(color);
        glow.setColor(Ui.adjustAlpha(color, 45));
        canvas.drawCircle(bx, by, radius * 0.30f, glow);
        canvas.drawCircle(bx, by, radius * 0.19f, bubble);
    }
}
