package com.phoneflipper.app.core;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.phoneflipper.app.R;

/**
 * Base screen: window styling, edge-to-edge insets, top bar and scroll body.
 * Screens only describe their content, never the chrome.
 */
public abstract class BaseActivity extends Activity {

    protected FrameLayout bodyHost;
    protected LinearLayout topActions;
    private LinearLayout rootColumn;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.wrap(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupWindow();

        rootColumn = new LinearLayout(this);
        rootColumn.setOrientation(LinearLayout.VERTICAL);
        rootColumn.setBackgroundColor(Palette.BG);

        bodyHost = new FrameLayout(this);
        rootColumn.addView(bodyHost, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(rootColumn);
        onCreateScreen(savedInstanceState);
        applyInsets(rootColumn);

        if (Prefs.keepScreenOn(this)) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    protected abstract void onCreateScreen(Bundle savedInstanceState);

    private void setupWindow() {
        Window w = getWindow();
        w.setStatusBarColor(Palette.BG);
        w.setNavigationBarColor(Palette.BG);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            w.setStatusBarContrastEnforced(false);
            w.setNavigationBarContrastEnforced(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            w.setDecorFitsSystemWindows(false);
        }
        w.getDecorView().setSystemUiVisibility(0);
    }

    private void applyInsets(View root) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return;
        }
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            }
        });
    }

    // ------------------------------------------------------------------ chrome

    /** Standard screen: top bar + vertically scrolling content. Returns the content column. */
    protected LinearLayout installScreen(int titleRes, int subtitleRes, boolean back) {
        LinearLayout top = buildTopBar(titleRes, subtitleRes, back);
        rootColumn.addView(top, 0);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout content = Ui.column(this);
        Ui.padding(content, 20, 10, 20, 36);
        scroll.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        bodyHost.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return content;
    }

    /** Custom screen: top bar + a body you fully control (no scroll wrapper). */
    protected void installCustom(int titleRes, int subtitleRes, boolean back, View body) {
        LinearLayout top = buildTopBar(titleRes, subtitleRes, back);
        rootColumn.addView(top, 0);
        bodyHost.addView(body, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    /** Bare screen without any chrome (dashboard, camera). */
    protected void installBare(View body) {
        bodyHost.addView(body, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    protected LinearLayout buildTopBar(int titleRes, int subtitleRes, boolean back) {
        LinearLayout bar = Ui.row(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(Palette.BG);
        Ui.padding(bar, 14, 10, 14, 10);
        bar.setMinimumHeight(Ui.dp(this, 62));

        if (back) {
            ImageView b = Ui.iconButton(this, IconDrawable.BACK, Palette.TEXT);
            b.setContentDescription(getString(R.string.cd_back));
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                    Ui.dp(this, 46), Ui.dp(this, 46));
            bp.setMarginEnd(Ui.dp(this, 12));
            bar.addView(b, bp);
        }

        LinearLayout titles = Ui.column(this);
        titles.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView title = Ui.text(this, getString(titleRes), 18.5f, Palette.TEXT, true);
        title.setSingleLine(true);
        titles.addView(title);
        if (subtitleRes != 0) {
            TextView sub = Ui.text(this, getString(subtitleRes), 11.5f, Palette.TEXT_MUTED, false);
            sub.setSingleLine(true);
            sub.setLetterSpacing(0.02f);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            sp.topMargin = Ui.dp(this, 3);
            titles.addView(sub, sp);
        }
        bar.addView(titles);

        topActions = Ui.row(this);
        topActions.setGravity(Gravity.CENTER_VERTICAL);
        topActions.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        bar.addView(topActions);

        LinearLayout shell = Ui.column(this);
        shell.addView(bar);
        shell.addView(Ui.divider(this));
        return shell;
    }

    protected ImageView addAction(int icon, int labelRes, final Runnable action) {
        ImageView iv = Ui.iconButton(this, icon, Palette.TEXT_DIM);
        iv.setContentDescription(getString(labelRes));
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action.run();
            }
        });
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                Ui.dp(this, 46), Ui.dp(this, 46));
        p.setMarginStart(Ui.dp(this, 8));
        topActions.addView(iv, p);
        return iv;
    }

    // ------------------------------------------------------------------ helpers

    protected void open(Class<?> target) {
        startActivity(new Intent(this, target));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    protected TextView footer(String text) {
        TextView t = Ui.caption(this, text);
        t.setGravity(Gravity.CENTER);
        t.setAlpha(0.85f);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = Ui.dp(this, 26);
        t.setLayoutParams(p);
        return t;
    }

    protected View dividerWithSpacing(float top, float bottom) {
        LinearLayout wrap = Ui.column(this);
        Ui.add(wrap, Ui.space(this, top));
        wrap.addView(Ui.divider(this));
        Ui.add(wrap, Ui.space(this, bottom));
        return wrap;
    }

    protected static int color(int c) {
        return c;
    }

    protected int transparent() {
        return Color.TRANSPARENT;
    }
}
