package com.phoneflipper.app.core;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * Vector-style icon set drawn with Canvas. Keeping the icons in code removes
 * dozens of XML files, keeps every glyph pixel-consistent and lets them tint
 * instantly for different states.
 */
public class IconDrawable extends Drawable {

    public static final int SENSOR = 1;
    public static final int AUDIO = 2;
    public static final int NFC = 3;
    public static final int IR = 4;
    public static final int CAMERA = 5;
    public static final int BLUETOOTH = 6;
    public static final int REPORT = 7;
    public static final int BOOK = 8;
    public static final int SETTINGS = 9;
    public static final int BACK = 10;
    public static final int BOLT = 11;
    public static final int SHIELD = 12;
    public static final int CHECK = 13;
    public static final int CLOSE = 14;
    public static final int PLAY = 15;
    public static final int STOP = 16;
    public static final int COPY = 17;
    public static final int TRASH = 18;
    public static final int SHARE = 19;
    public static final int CHEVRON = 20;
    public static final int FLASHLIGHT = 21;
    public static final int ANTENNA = 22;
    public static final int CHIP = 23;
    public static final int TAG = 24;
    public static final int INFO = 25;
    public static final int WARNING = 26;
    public static final int GLOBE = 27;
    public static final int PULSE = 28;
    public static final int MAGNET = 29;
    public static final int LIGHT = 30;
    public static final int LOCK = 31;

    private final int type;
    private final int color;
    private final float strokeDp;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF oval = new RectF();

    public IconDrawable(int type, int color, float strokeDp) {
        this.type = type;
        this.color = color;
        this.strokeDp = strokeDp;
        paint.setColor(color);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        fill.setColor(color);
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        float size = Math.min(b.width(), b.height());
        if (size <= 0) {
            return;
        }
        float density = 1f;
        // Bounds are set in pixels by callers; derive a stable stroke width.
        float sw = strokeDp * Math.max(1f, size / 24f / 1.0f);
        // Scale stroke with the icon size but keep a sensible minimum.
        sw = Math.max(1.2f, strokeDp * size / 24f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(sw);
        fill.setStyle(Paint.Style.FILL);

        canvas.save();
        canvas.translate(b.left + (b.width() - size) / 2f, b.top + (b.height() - size) / 2f);
        canvas.scale(size / 48f, size / 48f);
        canvas.translate(24, 24); // 48x48 artboard centred on 0,0
        drawIcon(canvas);
        canvas.restore();
    }

    private void drawIcon(Canvas c) {
        path.reset();
        switch (type) {
            case SENSOR: {
                c.drawCircle(0, 12, 3.2f, fill);
                oval.set(-19, -6, 19, 32);
                c.drawArc(oval, 200, 140, false, paint);
                oval.set(-12, 1, 12, 25);
                c.drawArc(oval, 200, 140, false, paint);
                break;
            }
            case AUDIO: {
                float[] heights = {8, 16, 26, 18, 10};
                for (int i = 0; i < heights.length; i++) {
                    float x = -16 + i * 8;
                    c.drawLine(x, -heights[i] / 2f, x, heights[i] / 2f, paint);
                }
                break;
            }
            case NFC: {
                oval.set(-6, -18, 32, 20);
                c.drawArc(oval, 300, 120, false, paint);
                oval.set(-6, -10, 24, 14);
                c.drawArc(oval, 300, 120, false, paint);
                oval.set(-6, -2, 16, 8);
                c.drawArc(oval, 300, 120, false, paint);
                c.drawCircle(-14, 0, 2.4f, fill);
                break;
            }
            case IR: {
                c.drawRoundRect(-20, -8, -8, 8, 4, 4, paint);
                oval.set(-2, -12, 14, 12);
                c.drawArc(oval, -55, 110, false, paint);
                oval.set(2, -18, 24, 18);
                c.drawArc(oval, -60, 120, false, paint);
                break;
            }
            case CAMERA: {
                c.drawRoundRect(-20, -13, 20, 14, 6, 6, paint);
                c.drawCircle(0, 0, 7.5f, paint);
                c.drawCircle(13, -8, 1.6f, fill);
                break;
            }
            case BLUETOOTH: {
                c.drawLine(0, -18, 0, 18, paint);
                c.drawLine(0, -18, 10, -9, paint);
                c.drawLine(10, -9, -10, 9, paint);
                c.drawLine(0, 18, 10, 9, paint);
                c.drawLine(10, 9, -10, -9, paint);
                break;
            }
            case REPORT: {
                c.drawRoundRect(-14, -19, 14, 19, 4, 4, paint);
                c.drawLine(-7, -9, 7, -9, paint);
                c.drawLine(-7, -2, 7, -2, paint);
                c.drawLine(-7, 5, 3, 5, paint);
                break;
            }
            case BOOK: {
                c.drawRoundRect(-19, -15, 19, 15, 3, 3, paint);
                c.drawLine(0, -15, 0, 15, paint);
                c.drawLine(-13, -8, -5, -8, paint);
                c.drawLine(5, -8, 13, -8, paint);
                break;
            }
            case SETTINGS: {
                c.drawCircle(0, 0, 7.5f, paint);
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45);
                    float x1 = (float) Math.cos(a) * 12;
                    float y1 = (float) Math.sin(a) * 12;
                    float x2 = (float) Math.cos(a) * 17;
                    float y2 = (float) Math.sin(a) * 17;
                    c.drawLine(x1, y1, x2, y2, paint);
                }
                break;
            }
            case BACK: {
                path.moveTo(6, -15);
                path.lineTo(-7, 0);
                path.lineTo(6, 15);
                c.drawPath(path, paint);
                break;
            }
            case CHEVRON: {
                path.moveTo(-6, -13);
                path.lineTo(6, 0);
                path.lineTo(-6, 13);
                c.drawPath(path, paint);
                break;
            }
            case BOLT: {
                path.moveTo(2, -20);
                path.lineTo(-11, 2);
                path.lineTo(-1, 2);
                path.lineTo(-4, 20);
                path.lineTo(11, -3);
                path.lineTo(1, -3);
                path.close();
                c.drawPath(path, fill);
                break;
            }
            case SHIELD: {
                path.moveTo(0, -19);
                path.lineTo(16, -11);
                path.lineTo(16, 3);
                path.cubicTo(16, 12, 9, 17, 0, 20);
                path.cubicTo(-9, 17, -16, 12, -16, 3);
                path.lineTo(-16, -11);
                path.close();
                c.drawPath(path, paint);
                break;
            }
            case CHECK: {
                path.moveTo(-15, 1);
                path.lineTo(-5, 11);
                path.lineTo(15, -11);
                c.drawPath(path, paint);
                break;
            }
            case CLOSE: {
                c.drawLine(-13, -13, 13, 13, paint);
                c.drawLine(13, -13, -13, 13, paint);
                break;
            }
            case PLAY: {
                path.moveTo(-10, -16);
                path.lineTo(15, 0);
                path.lineTo(-10, 16);
                path.close();
                c.drawPath(path, fill);
                break;
            }
            case STOP: {
                c.drawRoundRect(-11, -11, 11, 11, 3, 3, fill);
                break;
            }
            case COPY: {
                c.drawRoundRect(-17, -17, 3, 3, 3, 3, paint);
                c.drawRoundRect(-3, -3, 17, 17, 3, 3, paint);
                break;
            }
            case TRASH: {
                c.drawLine(-17, -12, 17, -12, paint);
                c.drawLine(-6, -12, -6, -17, paint);
                c.drawLine(6, -12, 6, -17, paint);
                c.drawLine(-6, -17, 6, -17, paint);
                c.drawRoundRect(-13, -12, 13, 18, 3, 3, paint);
                c.drawLine(-4, -5, -4, 11, paint);
                c.drawLine(4, -5, 4, 11, paint);
                break;
            }
            case SHARE: {
                c.drawCircle(12, 0, 3.4f, fill);
                c.drawCircle(-12, -12, 3.4f, fill);
                c.drawCircle(-12, 12, 3.4f, fill);
                c.drawLine(-9, -10, 9, -2, paint);
                c.drawLine(-9, 10, 9, 2, paint);
                break;
            }
            case FLASHLIGHT: {
                path.moveTo(-14, -17);
                path.lineTo(4, -17);
                path.lineTo(12, 17);
                path.lineTo(-6, 17);
                path.close();
                c.drawPath(path, paint);
                c.drawLine(-10, -8, 1, -8, paint);
                break;
            }
            case ANTENNA: {
                c.drawLine(0, 17, 0, -4, paint);
                c.drawCircle(0, 19, 2.6f, fill);
                oval.set(-17, -12, 17, 22);
                c.drawArc(oval, 200, 140, false, paint);
                oval.set(-9, -4, 9, 14);
                c.drawArc(oval, 200, 140, false, paint);
                break;
            }
            case CHIP: {
                c.drawRoundRect(-12, -12, 12, 12, 3, 3, paint);
                c.drawRoundRect(-6, -6, 6, 6, 2, 2, paint);
                for (int i = -8; i <= 8; i += 8) {
                    c.drawLine(i, -12, i, -18, paint);
                    c.drawLine(i, 12, i, 18, paint);
                    c.drawLine(-12, i, -18, i, paint);
                    c.drawLine(12, i, 18, i, paint);
                }
                break;
            }
            case TAG: {
                path.moveTo(-18, -12);
                path.lineTo(0, -12);
                path.lineTo(18, 6);
                path.lineTo(6, 18);
                path.lineTo(-12, 0);
                path.close();
                c.drawPath(path, paint);
                c.drawCircle(-9, -5, 2.6f, fill);
                break;
            }
            case INFO: {
                c.drawCircle(0, 0, 18, paint);
                c.drawCircle(0, -10, 2.1f, fill);
                c.drawLine(0, -3, 0, 11, paint);
                break;
            }
            case WARNING: {
                path.moveTo(0, -17);
                path.lineTo(19, 14);
                path.lineTo(-19, 14);
                path.close();
                c.drawPath(path, paint);
                c.drawLine(0, -7, 0, 5, paint);
                c.drawCircle(0, 10, 2.1f, fill);
                break;
            }
            case GLOBE: {
                c.drawCircle(0, 0, 18, paint);
                oval.set(-8, -18, 8, 18);
                c.drawOval(oval, paint);
                c.drawLine(-17, 0, 17, 0, paint);
                c.drawLine(-15, -9, 15, -9, paint);
                c.drawLine(-15, 9, 15, 9, paint);
                break;
            }
            case PULSE: {
                path.moveTo(-19, 0);
                path.lineTo(-10, 0);
                path.lineTo(-5, -12);
                path.lineTo(1, 12);
                path.lineTo(6, 0);
                path.lineTo(19, 0);
                c.drawPath(path, paint);
                break;
            }
            case MAGNET: {
                path.moveTo(-14, -16);
                path.lineTo(-14, 6);
                path.cubicTo(-14, 16, 14, 16, 14, 6);
                path.lineTo(14, -16);
                c.drawPath(path, paint);
                c.drawLine(-14, -8, -6, -8, paint);
                c.drawLine(14, -8, 6, -8, paint);
                break;
            }
            case LIGHT: {
                c.drawCircle(0, 0, 7.5f, paint);
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45);
                    float x1 = (float) Math.cos(a) * 12;
                    float y1 = (float) Math.sin(a) * 12;
                    float x2 = (float) Math.cos(a) * 18;
                    float y2 = (float) Math.sin(a) * 18;
                    c.drawLine(x1, y1, x2, y2, paint);
                }
                break;
            }
            case LOCK: {
                c.drawRoundRect(-14, -2, 14, 17, 3, 3, paint);
                oval.set(-9, -17, 9, 1);
                c.drawArc(oval, 180, 180, false, paint);
                c.drawLine(-9, 0, -9, 1, paint);
                c.drawLine(9, 0, 9, 1, paint);
                break;
            }
            default:
                c.drawCircle(0, 0, 9, paint);
        }
    }

    public IconDrawable tint(int newColor) {
        return new IconDrawable(type, newColor, strokeDp);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        fill.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        paint.setColorFilter(cf);
        fill.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return 48;
    }

    @Override
    public int getIntrinsicHeight() {
        return 48;
    }
}
