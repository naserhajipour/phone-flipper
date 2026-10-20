package com.phoneflipper.app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

import com.phoneflipper.app.core.Palette;
import com.phoneflipper.app.core.Ui;

/** The Flipper One mark: a rounded monolith with an energy cut. */
public class LogoView extends View {

    private final Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bolt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path cut = new Path();

    public LogoView(Context c) {
        super(c);
        body.setStyle(Paint.Style.FILL);
        bolt.setStyle(Paint.Style.FILL);
        bolt.setColor(Palette.BG);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeWidth(Ui.dp(c, 1.6f));
        arc.setColor(Ui.adjustAlpha(0xFFFFFFFF, 90));
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float radius = Math.min(w, h) * 0.28f;
        rect.set(0, 0, w, h);
        body.setShader(new LinearGradient(0, 0, w, h,
                Palette.PRIMARY, Palette.PRIMARY_DIM, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, radius, radius, body);

        float s = Math.min(w, h);
        canvas.save();
        canvas.translate(w / 2f, h / 2f);
        canvas.scale(s / 48f, s / 48f);
        cut.reset();
        cut.moveTo(2, -15);
        cut.lineTo(-8, 2);
        cut.lineTo(-1, 2);
        cut.lineTo(-3, 15);
        cut.lineTo(8, -2);
        cut.lineTo(1, -2);
        cut.close();
        canvas.drawPath(cut, bolt);
        canvas.restore();

        rect.set(w * 0.12f, h * 0.12f, w * 0.88f, h * 0.88f);
        canvas.drawArc(rect, 205, 130, false, arc);
    }
}
