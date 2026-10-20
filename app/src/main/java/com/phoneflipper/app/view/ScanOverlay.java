package com.phoneflipper.app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import com.phoneflipper.app.core.Palette;
import com.phoneflipper.app.core.Ui;

import java.util.ArrayList;
import java.util.List;

/** HUD overlay for the lens finder: reticle, corner brackets and hit markers. */
public class ScanOverlay extends View {

    private final Paint hud = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint marker = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final List<float[]> spots = new ArrayList<>();

    public ScanOverlay(Context c) {
        super(c);
        hud.setStyle(Paint.Style.STROKE);
        hud.setStrokeWidth(Ui.dp(c, 1.2f));
        hud.setColor(Ui.adjustAlpha(0xFFFFFFFF, 70));
        marker.setStyle(Paint.Style.STROKE);
        marker.setStrokeWidth(Ui.dp(c, 1.8f));
        marker.setColor(Palette.ACCENT);
        markerFill.setStyle(Paint.Style.FILL);
        markerFill.setColor(Ui.adjustAlpha(Palette.ACCENT, 45));
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void setSpots(List<float[]> points) {
        spots.clear();
        if (points != null) {
            spots.addAll(points);
        }
        invalidate();
    }

    public void clearSpots() {
        spots.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float inset = Ui.dp(getContext(), 26);
        float len = Ui.dp(getContext(), 22);
        float r = Ui.dp(getContext(), 10);

        // corner brackets
        canvas.drawRoundRect(inset, inset, inset + len, inset + len, r, r, hud);
        canvas.drawRoundRect(w - inset - len, inset, w - inset, inset + len, r, r, hud);
        canvas.drawRoundRect(inset, h - inset - len, inset + len, h - inset, r, r, hud);
        canvas.drawRoundRect(w - inset - len, h - inset - len, w - inset, h - inset, r, r, hud);

        // centre reticle
        float cx = w / 2f;
        float cy = h / 2f;
        canvas.drawCircle(cx, cy, Ui.dp(getContext(), 26), hud);
        canvas.drawLine(cx - Ui.dp(getContext(), 36), cy, cx - Ui.dp(getContext(), 14), cy, hud);
        canvas.drawLine(cx + Ui.dp(getContext(), 14), cy, cx + Ui.dp(getContext(), 36), cy, hud);
        canvas.drawLine(cx, cy - Ui.dp(getContext(), 36), cx, cy - Ui.dp(getContext(), 14), hud);
        canvas.drawLine(cx, cy + Ui.dp(getContext(), 14), cx, cy + Ui.dp(getContext(), 36), hud);

        for (float[] spot : spots) {
            float x = spot[0];
            float y = spot[1];
            float radius = spot[2];
            canvas.drawCircle(x, y, radius * 1.6f, markerFill);
            canvas.drawCircle(x, y, radius * 1.6f, marker);
            canvas.drawCircle(x, y, Ui.dp(getContext(), 1.6f), marker);
        }
    }
}
