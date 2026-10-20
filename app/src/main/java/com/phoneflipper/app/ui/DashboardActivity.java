package com.phoneflipper.app.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.phoneflipper.app.BuildConfig;
import com.phoneflipper.app.R;
import com.phoneflipper.app.core.BaseActivity;
import com.phoneflipper.app.core.Capabilities;
import com.phoneflipper.app.core.IconDrawable;
import com.phoneflipper.app.core.Palette;
import com.phoneflipper.app.core.Prefs;
import com.phoneflipper.app.core.Ui;
import com.phoneflipper.app.view.LogoView;
import com.phoneflipper.app.view.RingView;

import java.util.List;
import java.util.Locale;

/** Home screen: identity, capability score, tool grid and device facts. */
public class DashboardActivity extends BaseActivity {

    private LinearLayout content;
    private List<Capabilities.Item> caps;

    @Override
    protected void onCreateScreen(Bundle savedInstanceState) {
        caps = Capabilities.scan(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        content = Ui.column(this);
        Ui.padding(content, 20, 14, 20, 36);
        scroll.addView(content, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        installBare(scroll);

        buildHeader();
        buildHero();
        buildScore();
        buildTools();
        buildDevice();
        buildFooter();

        if (!Prefs.safetyAccepted(this)) {
            showSafetyDialog();
        }
    }

    // ------------------------------------------------------------------ header

    private void buildHeader() {
        LinearLayout row = Ui.row(this);
        LogoView logo = new LogoView(this);
        row.addView(logo, new LinearLayout.LayoutParams(Ui.dp(this, 46), Ui.dp(this, 46)));

        LinearLayout names = Ui.column(this);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        np.setMarginStart(Ui.dp(this, 12));
        TextView name = Ui.text(this, getString(R.string.app_name), 20, Palette.TEXT, true);
        name.setLetterSpacing(0.01f);
        TextView kicker = Ui.text(this, getString(R.string.brand_kicker), 9.5f,
                Palette.TEXT_MUTED, true);
        Ui.track(this, kicker, 0.28f);
        kicker.setAllCaps(true);
        LinearLayout.LayoutParams kp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        kp.topMargin = Ui.dp(this, 3);
        names.addView(name);
        names.addView(kicker, kp);
        row.addView(names, np);

        ImageView settings = Ui.iconButton(this, IconDrawable.SETTINGS, Palette.TEXT_DIM);
        settings.setContentDescription(getString(R.string.title_settings));
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                open(SettingsActivity.class);
            }
        });
        row.addView(settings, new LinearLayout.LayoutParams(Ui.dp(this, 46), Ui.dp(this, 46)));
        content.addView(row);
    }

    private void buildHero() {
        TextView title = Ui.text(this, getString(R.string.dashboard_headline), 32, Palette.TEXT, true);
        title.setLineSpacing(Ui.dp(this, 2), 1f);
        Ui.add(content, title, 0, 26, 0, 0);

        TextView sub = Ui.body(this, getString(R.string.dashboard_tagline));
        sub.setTextSize(14);
        Ui.add(content, sub, 0, 10, 8, 0);

        LinearLayout chips = Ui.row(this);
        chips.setGravity(Gravity.CENTER_VERTICAL);
        Ui.add(chips, Ui.badge(this, getString(R.string.chip_offline), Palette.SUCCESS), 0, 16, 6, 0);
        Ui.add(chips, Ui.badge(this, getString(R.string.chip_no_tracking), Palette.PRIMARY), 0, 16, 6, 0);
        Ui.add(chips, Ui.badge(this, String.format(Locale.US, "v%s", BuildConfig.VERSION_NAME),
                Palette.TEXT_DIM), 0, 16, 0, 0);
        content.addView(chips);
    }

    // ------------------------------------------------------------------ score

    private void buildScore() {
        LinearLayout card = Ui.card(this);
        Ui.add(content, card, 0, 24, 0, 0);

        LinearLayout header = Ui.row(this);
        header.addView(Ui.section(this, getString(R.string.section_score)));
        card.addView(header);

        LinearLayout body = Ui.row(this);
        body.setGravity(Gravity.CENTER_VERTICAL);
        Ui.add(card, body, 0, 14, 0, 0);

        RingView ring = new RingView(this);
        int score = Capabilities.score(caps);
        ring.set(score / 100f, String.valueOf(score),
                getString(R.string.score_native_short), Palette.PRIMARY);
        body.addView(ring, new LinearLayout.LayoutParams(Ui.dp(this, 132), Ui.dp(this, 132)));

        LinearLayout stats = Ui.column(this);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        sp.setMarginStart(Ui.dp(this, 22));
        body.addView(stats, sp);

        stats.addView(statRow(getString(R.string.score_native),
                String.valueOf(Capabilities.count(caps, Capabilities.NATIVE)), Palette.SUCCESS));
        stats.addView(statRow(getString(R.string.score_bridge),
                String.valueOf(Capabilities.count(caps, Capabilities.BRIDGE)), Palette.WARNING));
        stats.addView(statRow(getString(R.string.score_absent),
                String.valueOf(Capabilities.count(caps, Capabilities.ABSENT)), Palette.TEXT_MUTED));

        TextView caption = Ui.caption(this, String.format(Locale.US,
                getString(R.string.score_caption), caps.size()));
        Ui.add(stats, caption, 0, 12, 0, 0);
    }

    private LinearLayout statRow(String label, String value, int color) {
        LinearLayout row = Ui.row(this);
        View dot = new View(this);
        dot.setBackground(Ui.rounded(this, color, 4, Color.TRANSPARENT, 0));
        row.addView(dot, new LinearLayout.LayoutParams(Ui.dp(this, 8), Ui.dp(this, 8)));

        TextView l = Ui.text(this, label, 12.5f, Palette.TEXT_DIM, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMarginStart(Ui.dp(this, 8));
        row.addView(l, lp);

        TextView v = Ui.mono(this, value, 14, Palette.TEXT);
        row.addView(v);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.topMargin = Ui.dp(this, 7);
        row.setLayoutParams(rp);
        return row;
    }

    // ------------------------------------------------------------------ tools

    private void buildTools() {
        Ui.add(content, Ui.section(this, getString(R.string.section_tools)), 0, 28, 0, 0);

        Object[][] tools = new Object[][]{
                {IconDrawable.SENSOR, R.string.tool_sensor, R.string.tool_sensor_sub,
                        SensorLabActivity.class, statusFor("sensor")},
                {IconDrawable.AUDIO, R.string.tool_audio, R.string.tool_audio_sub,
                        AudioLabActivity.class, statusFor("mic")},
                {IconDrawable.NFC, R.string.tool_nfc, R.string.tool_nfc_sub,
                        NfcActivity.class, statusFor("nfc")},
                {IconDrawable.IR, R.string.tool_ir, R.string.tool_ir_sub,
                        IrActivity.class, statusFor("ir")},
                {IconDrawable.CAMERA, R.string.tool_lens, R.string.tool_lens_sub,
                        LensFinderActivity.class, statusFor("camera")},
                {IconDrawable.BLUETOOTH, R.string.tool_bridge, R.string.tool_bridge_sub,
                        BridgeActivity.class, statusFor("bt")},
                {IconDrawable.REPORT, R.string.tool_report, R.string.tool_report_sub,
                        CapabilitiesActivity.class, Capabilities.NATIVE},
                {IconDrawable.BOOK, R.string.tool_guide, R.string.tool_guide_sub,
                        GuidesActivity.class, Capabilities.NATIVE},
        };

        for (int i = 0; i < tools.length; i += 2) {
            LinearLayout row = Ui.row(this);
            row.setGravity(Gravity.TOP);
            Ui.add(content, row, 0, 12, 0, 0);
            row.addView(toolCard(tools[i]), gridParams(i));
            if (i + 1 < tools.length) {
                row.addView(toolCard(tools[i + 1]), gridParams(i + 1));
            } else {
                View filler = new View(this);
                row.addView(filler, gridParams(i + 1));
            }
        }
    }

    private LinearLayout.LayoutParams gridParams(int index) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p.setMarginStart(Ui.dp(this, index % 2 == 0 ? 0 : 6));
        p.setMarginEnd(Ui.dp(this, index % 2 == 0 ? 6 : 0));
        return p;
    }

    private int statusFor(String id) {
        if ("sensor".equals(id)) {
            int n = 0;
            for (Capabilities.Item item : caps) {
                if (Capabilities.CAT_SENSOR.equals(item.category)
                        && item.status == Capabilities.NATIVE) {
                    n++;
                }
            }
            return n >= 3 ? Capabilities.NATIVE : (n > 0 ? Capabilities.BRIDGE : Capabilities.ABSENT);
        }
        for (Capabilities.Item item : caps) {
            if (item.id.equals(id)) {
                return item.status;
            }
        }
        return Capabilities.NATIVE;
    }

    private View toolCard(Object[] tool) {
        final Class<?> target = (Class<?>) tool[3];
        LinearLayout card = Ui.tapCard(this);
        card.setMinimumHeight(Ui.dp(this, 138));
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                open(target);
            }
        });

        ImageView icon = Ui.icon(this, (Integer) tool[0], Palette.PRIMARY, 21);
        icon.setBackground(Ui.rounded(this, Ui.adjustAlpha(Palette.PRIMARY, 22), 12, Color.TRANSPARENT, 0));
        Ui.padding(icon, 9);
        card.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, 39), Ui.dp(this, 39)));

        TextView title = Ui.text(this, getString((Integer) tool[1]), 14, Palette.TEXT, true);
        Ui.add(card, title, 0, 12, 0, 0);

        TextView sub = Ui.text(this, getString((Integer) tool[2]), 11.5f, Palette.TEXT_MUTED, false);
        sub.setMaxLines(2);
        sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
        Ui.add(card, sub, 0, 4, 0, 0);

        int status = (Integer) tool[4];
        TextView badge = Ui.badge(this, Capabilities.statusLabel(this, status),
                Capabilities.statusColor(status));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.topMargin = Ui.dp(this, 12);
        badge.setLayoutParams(bp);
        card.addView(badge);
        return card;
    }

    // ------------------------------------------------------------------ device

    private void buildDevice() {
        Ui.add(content, Ui.section(this, getString(R.string.section_device)), 0, 28, 0, 0);

        LinearLayout card = Ui.card(this);
        Ui.add(content, card, 0, 12, 0, 0);

        addFact(card, getString(R.string.device_model), Capabilities.deviceName());
        addFact(card, getString(R.string.device_android), Capabilities.androidVersion());
        String chipset = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && Build.SOC_MODEL != null
                ? Build.SOC_MODEL : getString(R.string.value_unknown);
        addFact(card, getString(R.string.device_chipset), chipset);
        addFact(card, getString(R.string.device_abi), Build.SUPPORTED_ABIS.length > 0
                ? Build.SUPPORTED_ABIS[0] : getString(R.string.value_unknown));
        addFact(card, getString(R.string.device_security),
                Build.VERSION.SECURITY_PATCH == null || Build.VERSION.SECURITY_PATCH.isEmpty()
                        ? getString(R.string.value_unknown) : Build.VERSION.SECURITY_PATCH);
    }

    private void addFact(LinearLayout parent, String label, String value) {
        LinearLayout row = Ui.row(this);
        TextView l = Ui.text(this, label, 12.5f, Palette.TEXT_MUTED, false);
        row.addView(l, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView v = Ui.mono(this, value, 12, Palette.TEXT_DIM);
        v.setSingleLine(true);
        row.addView(v);
        if (parent.getChildCount() > 0) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            p.topMargin = Ui.dp(this, 11);
            parent.addView(row, p);
        } else {
            parent.addView(row);
        }
    }

    private void buildFooter() {
        TextView note = Ui.caption(this, getString(R.string.footer_offline));
        note.setGravity(Gravity.CENTER);
        Ui.add(content, note, 8, 28, 8, 0);

        TextView built = Ui.caption(this, String.format(Locale.US,
                getString(R.string.footer_build), BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE));
        built.setGravity(Gravity.CENTER);
        built.setAlpha(0.7f);
        Ui.add(content, built, 8, 8, 8, 0);

        TextView legal = Ui.caption(this, getString(R.string.footer_legal));
        legal.setGravity(Gravity.CENTER);
        legal.setAlpha(0.55f);
        Ui.add(content, legal, 12, 16, 12, 0);
    }

    // ------------------------------------------------------------------ consent

    private void showSafetyDialog() {
        ScrollView scroller = new ScrollView(this);
        TextView body = Ui.body(this, getString(R.string.consent_body));
        Ui.padding(body, 22, 8, 22, 8);
        scroller.addView(body);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.consent_title))
                .setView(scroller)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.consent_accept),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Prefs.setSafetyAccepted(DashboardActivity.this, true);
                            }
                        })
                .setNegativeButton(getString(R.string.consent_guide),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Prefs.setSafetyAccepted(DashboardActivity.this, true);
                                startActivity(new Intent(DashboardActivity.this, GuidesActivity.class));
                            }
                        })
                .show();
    }
}
