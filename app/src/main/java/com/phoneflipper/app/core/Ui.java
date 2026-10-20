package com.phoneflipper.app.core;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Small, dependency-free design system used by every screen. Building the UI in
 * code keeps the APK tiny and lets the whole interface share one visual language.
 */
public final class Ui {

    public static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    public static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private Ui() {
    }

    // ------------------------------------------------------------------ metrics

    public static int dp(Context c, float value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    public static float sp(Context c, float value) {
        return value * c.getResources().getDisplayMetrics().scaledDensity;
    }

    public static int screenWidth(Context c) {
        return c.getResources().getDisplayMetrics().widthPixels;
    }

    public static boolean isRtl(Context c) {
        return c.getResources().getConfiguration().getLayoutDirection()
                == View.LAYOUT_DIRECTION_RTL;
    }

    /** Tracking looks right in Latin text but breaks Persian typography. */
    public static void track(Context c, TextView view, float value) {
        view.setLetterSpacing(isRtl(c) ? 0f : value);
    }

    // ------------------------------------------------------------------ layout

    public static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setLayoutParams(new LinearLayout.LayoutParams(MATCH, WRAP));
        return l;
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setLayoutParams(new LinearLayout.LayoutParams(MATCH, WRAP));
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    public static LinearLayout.LayoutParams lp(int width, int height, float weight) {
        return new LinearLayout.LayoutParams(width, height, weight);
    }

    /** Adds a child with start/end aware margins (dp). */
    public static <T extends View> T add(LinearLayout parent, T child,
                                         float start, float top, float end, float bottom) {
        LinearLayout.LayoutParams p = child.getLayoutParams() instanceof LinearLayout.LayoutParams
                ? (LinearLayout.LayoutParams) child.getLayoutParams()
                : new LinearLayout.LayoutParams(MATCH, WRAP);
        p.setMarginStart(dp(parent.getContext(), start));
        p.setMarginEnd(dp(parent.getContext(), end));
        p.topMargin = dp(parent.getContext(), top);
        p.bottomMargin = dp(parent.getContext(), bottom);
        parent.addView(child, p);
        return child;
    }

    public static <T extends View> T add(LinearLayout parent, T child) {
        return add(parent, child, 0, 0, 0, 0);
    }

    public static View space(Context c, float heightDp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(MATCH, dp(c, heightDp)));
        return v;
    }

    public static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(Palette.STROKE_SOFT);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(MATCH, Math.max(1, dp(c, 0.7f)));
        v.setLayoutParams(p);
        return v;
    }

    public static void padding(View v, float l, float t, float r, float b) {
        Context c = v.getContext();
        v.setPadding(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
    }

    public static void padding(View v, float all) {
        padding(v, all, all, all, all);
    }

    // ------------------------------------------------------------------ surfaces

    public static GradientDrawable rounded(Context c, int fill, float radiusDp,
                                           int strokeColor, float strokeDp) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(fill);
        d.setCornerRadius(dp(c, radiusDp));
        if (strokeColor != Color.TRANSPARENT) {
            d.setStroke(Math.max(1, dp(c, strokeDp)), strokeColor);
        }
        return d;
    }

    public static GradientDrawable gradient(Context c, int start, int end, float radiusDp) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, new int[]{start, end});
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    public static RippleDrawable ripple(Context c, Drawable content, int color) {
        return new RippleDrawable(ColorStateList.valueOf(color), content, null);
    }

    /** A standard card container with consistent padding and border. */
    public static LinearLayout card(Context c) {
        LinearLayout l = column(c);
        l.setBackground(rounded(c, Palette.SURFACE, 20, Palette.STROKE_SOFT, 1));
        padding(l, 18, 18, 18, 18);
        return l;
    }

    /** A card that can be tapped; adds a ripple and focus handling. */
    public static LinearLayout tapCard(Context c) {
        LinearLayout l = card(c);
        l.setClickable(true);
        l.setFocusable(true);
        l.setBackground(ripple(c, rounded(c, Palette.SURFACE, 20, Palette.STROKE_SOFT, 1), Palette.RIPPLE));
        return l;
    }

    // ------------------------------------------------------------------ text

    public static Typeface medium() {
        return Typeface.create("sans-serif-medium", Typeface.NORMAL);
    }

    public static Typeface bold() {
        return Typeface.create("sans-serif", Typeface.BOLD);
    }

    public static Typeface mono() {
        return Typeface.create("monospace", Typeface.NORMAL);
    }

    public static TextView text(Context c, CharSequence value, float sizeSp, int color, boolean medium) {
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextSize(sizeSp);
        t.setTextColor(color);
        t.setTypeface(medium ? medium() : Typeface.create("sans-serif", Typeface.NORMAL));
        t.setIncludeFontPadding(false);
        t.setLineSpacing(dp(c, 3), 1f);
        return t;
    }

    public static TextView title(Context c, CharSequence value) {
        return text(c, value, 22, Palette.TEXT, true);
    }

    public static TextView body(Context c, CharSequence value) {
        return text(c, value, 14.5f, Palette.TEXT_DIM, false);
    }

    public static TextView caption(Context c, CharSequence value) {
        return text(c, value, 12, Palette.TEXT_MUTED, false);
    }

    public static TextView mono(Context c, CharSequence value, float sizeSp, int color) {
        TextView t = text(c, value, sizeSp, color, false);
        t.setTypeface(mono());
        return t;
    }

    /** Section label: uppercase, tracked, muted. */
    public static TextView section(Context c, CharSequence value) {
        TextView t = text(c, value, 11, Palette.TEXT_MUTED, true);
        track(c, t, 0.16f);
        t.setAllCaps(true);
        return t;
    }

    public static TextView badge(Context c, CharSequence value, int color) {
        TextView t = text(c, value, 10, color, true);
        track(c, t, 0.08f);
        t.setAllCaps(true);
        t.setGravity(Gravity.CENTER);
        GradientDrawable bg = rounded(c, adjustAlpha(color, 30), 7, adjustAlpha(color, 90), 1);
        t.setBackground(bg);
        padding(t, 8, 4, 8, 4);
        return t;
    }

    // ------------------------------------------------------------------ buttons

    public static TextView button(Context c, CharSequence label, boolean primary) {
        TextView b = text(c, label, 14, primary ? Palette.ON_PRIMARY : Palette.TEXT, true);
        b.setGravity(Gravity.CENTER);
        b.setClickable(true);
        b.setFocusable(true);
        b.setMinHeight(dp(c, 48));
        b.setMaxLines(2);
        b.setEllipsize(TextUtils.TruncateAt.END);
        padding(b, 18, 12, 18, 12);
        Drawable content = rounded(c, primary ? Palette.PRIMARY : Palette.SURFACE_ALT, 14,
                primary ? Color.TRANSPARENT : Palette.STROKE, 1);
        b.setBackground(ripple(c, content, primary ? 0x33000000 : Palette.RIPPLE));
        return b;
    }

    public static TextView outlineButton(Context c, CharSequence label) {
        TextView b = text(c, label, 14, Palette.PRIMARY, true);
        b.setGravity(Gravity.CENTER);
        b.setClickable(true);
        b.setFocusable(true);
        b.setMinHeight(dp(c, 48));
        padding(b, 18, 12, 18, 12);
        b.setBackground(ripple(c, rounded(c, Color.TRANSPARENT, 14, Palette.PRIMARY_DIM, 1), Palette.GLOW_PRIMARY));
        return b;
    }

    public static android.widget.ImageView iconButton(Context c, int icon, int color) {
        IconDrawable d = new IconDrawable(icon, color, 2.0f);
        int size = dp(c, 24);
        d.setBounds(0, 0, size, size);
        android.widget.ImageView iv = new android.widget.ImageView(c);
        iv.setImageDrawable(d);
        iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        iv.setClickable(true);
        iv.setFocusable(true);
        iv.setContentDescription("");
        int pad = dp(c, 11);
        iv.setPadding(pad, pad, pad, pad);
        iv.setBackground(ripple(c, rounded(c, Palette.SURFACE_ALT, 14, Palette.STROKE_SOFT, 1), Palette.RIPPLE));
        return iv;
    }

    /** Bare icon, no background — used inside cards and lists. */
    public static android.widget.ImageView icon(Context c, int icon, int color, float sizeDp) {
        IconDrawable d = new IconDrawable(icon, color, 1.9f);
        int size = dp(c, sizeDp);
        d.setBounds(0, 0, size, size);
        android.widget.ImageView iv = new android.widget.ImageView(c);
        iv.setImageDrawable(d);
        iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        iv.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        return iv;
    }

    // ------------------------------------------------------------------ inputs

    public static EditText input(Context c, CharSequence hint) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setTextSize(14.5f);
        e.setTextColor(Palette.TEXT);
        e.setHintTextColor(Palette.TEXT_MUTED);
        e.setTypeface(Typeface.create("monospace", Typeface.NORMAL));
        e.setSingleLine(true);
        e.setBackground(rounded(c, Palette.SURFACE_ALT, 12, Palette.STROKE, 1));
        padding(e, 14, 12, 14, 12);
        return e;
    }

    public static void hideKeyboard(Activity a) {
        View focus = a.getCurrentFocus();
        if (focus == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) a.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }

    // ------------------------------------------------------------------ misc

    public static int adjustAlpha(int color, int alpha0to255) {
        return Color.argb(alpha0to255, Color.red(color), Color.green(color), Color.blue(color));
    }

    public static void haptic(View v) {
        if (v == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK);
        } else {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    public static void setVisible(View v, boolean visible) {
        if (v != null) {
            v.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}
