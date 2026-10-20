package com.phoneflipper.app.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.phoneflipper.app.R;
import com.phoneflipper.app.core.BaseActivity;
import com.phoneflipper.app.core.Capabilities;
import com.phoneflipper.app.core.IconDrawable;
import com.phoneflipper.app.core.Palette;
import com.phoneflipper.app.core.ReportBuilder;
import com.phoneflipper.app.core.ReportProvider;
import com.phoneflipper.app.core.Ui;

import java.io.File;
import java.util.List;
import java.util.Locale;

/** Full hardware audit with export. */
public class CapabilitiesActivity extends BaseActivity {

    private LinearLayout content;
    private List<Capabilities.Item> caps;

    @Override
    protected void onCreateScreen(Bundle savedInstanceState) {
        caps = Capabilities.scan(this);
        content = installScreen(R.string.title_capabilities, R.string.subtitle_capabilities, true);
        addAction(IconDrawable.SHARE, new Runnable() {
            @Override
            public void run() {
                shareText();
            }
        });

        buildSummary();
        String[] order = {Capabilities.CAT_RADIO, Capabilities.CAT_SENSOR,
                Capabilities.CAT_OPTIC, Capabilities.CAT_IO};
        for (String category : order) {
            buildCategory(category);
        }
        buildExport();
        content.addView(footer(getString(R.string.report_footer)));
    }

    private void buildSummary() {
        LinearLayout card = Ui.card(this);
        Ui.add(content, card, 0, 12, 0, 0);

        LinearLayout top = Ui.row(this);
        top.setGravity(Gravity.BOTTOM);
        TextView score = Ui.mono(this, String.valueOf(Capabilities.score(caps)), 40, Palette.TEXT);
        top.addView(score);
        TextView pct = Ui.text(this, getString(R.string.score_percent_native), 12,
                Palette.TEXT_MUTED, true);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pp.bottomMargin = Ui.dp(this, 9);
        pp.setMarginStart(Ui.dp(this, 2));
        top.addView(pct, pp);
        card.addView(top);

        TextView summary = Ui.body(this, String.format(Locale.US,
                getString(R.string.capabilities_summary), caps.size(),
                Capabilities.count(caps, Capabilities.NATIVE),
                Capabilities.count(caps, Capabilities.BRIDGE),
                Capabilities.count(caps, Capabilities.ABSENT)));
        Ui.add(card, summary, 0, 8, 0, 0);

        LinearLayout bar = Ui.row(this);
        bar.setBackground(Ui.rounded(this, Palette.SURFACE_ALT, 4, Color.TRANSPARENT, 0));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 6));
        bp.topMargin = Ui.dp(this, 16);
        bar.setLayoutParams(bp);
        int total = Math.max(1, caps.size());
        bar.addView(segment(Capabilities.count(caps, Capabilities.NATIVE) / (float) total,
                Palette.SUCCESS, 0));
        bar.addView(segment(Capabilities.count(caps, Capabilities.BRIDGE) / (float) total,
                Palette.WARNING, 2));
        bar.addView(segment(Capabilities.count(caps, Capabilities.ABSENT) / (float) total,
                Palette.TEXT_MUTED, 2));
        card.addView(bar);
    }

    private View segment(float fraction, int color, float marginDp) {
        View v = new View(this);
        v.setBackground(Ui.rounded(this, color, 3, Color.TRANSPARENT, 0));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,
                Math.max(0.001f, fraction));
        p.setMarginStart(Ui.dp(this, marginDp));
        v.setLayoutParams(p);
        return v;
    }

    private void buildCategory(String category) {
        boolean has = false;
        for (Capabilities.Item item : caps) {
            if (item.category.equals(category)) {
                has = true;
                break;
            }
        }
        if (!has) {
            return;
        }
        Ui.add(content, Ui.section(this, ReportBuilder.categoryLabel(this, category)),
                0, 26, 0, 0);

        LinearLayout card = Ui.card(this);
        Ui.add(content, card, 0, 10, 0, 0);
        boolean first = true;
        for (Capabilities.Item item : caps) {
            if (!item.category.equals(category)) {
                continue;
            }
            if (!first) {
                LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Ui.dp(this, 0.7f)));
                dp.topMargin = Ui.dp(this, 14);
                dp.bottomMargin = Ui.dp(this, 14);
                card.addView(Ui.divider(this), dp);
            }
            card.addView(itemRow(item));
            first = false;
        }
    }

    private View itemRow(Capabilities.Item item) {
        LinearLayout row = Ui.row(this);
        row.setGravity(Gravity.TOP);

        ImageView icon = Ui.icon(this, item.icon,
                item.status == Capabilities.NATIVE ? Palette.PRIMARY : Palette.TEXT_MUTED, 19);
        icon.setBackground(Ui.rounded(this,
                item.status == Capabilities.NATIVE
                        ? Ui.adjustAlpha(Palette.PRIMARY, 20) : Ui.adjustAlpha(Palette.TEXT_MUTED, 18),
                11, Color.TRANSPARENT, 0));
        Ui.padding(icon, 8);
        row.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, 35), Ui.dp(this, 35)));

        LinearLayout texts = Ui.column(this);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tp.setMarginStart(Ui.dp(this, 12));
        row.addView(texts, tp);

        LinearLayout titleRow = Ui.row(this);
        TextView title = Ui.text(this, getString(item.titleRes), 14, Palette.TEXT, true);
        titleRow.addView(title, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView badge = Ui.badge(this, Capabilities.statusLabel(this, item.status),
                Capabilities.statusColor(item.status));
        titleRow.addView(badge);
        texts.addView(titleRow);

        TextView desc = Ui.text(this, getString(item.descRes), 12, Palette.TEXT_DIM, false);
        Ui.add(texts, desc, 0, 4, 0, 0);

        TextView detail = Ui.text(this, item.detail, 10.5f, Palette.TEXT_MUTED, false);
        detail.setLineSpacing(Ui.dp(this, 2), 1f);
        Ui.add(texts, detail, 0, 3, 0, 0);
        return row;
    }

    private void buildExport() {
        Ui.add(content, Ui.section(this, getString(R.string.section_export)), 0, 28, 0, 0);

        LinearLayout row = Ui.row(this);
        Ui.add(content, row, 0, 12, 0, 8);

        TextView textBtn = Ui.button(this, getString(R.string.action_share_report), true);
        textBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareText();
            }
        });
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p1.setMarginEnd(Ui.dp(this, 6));
        row.addView(textBtn, p1);

        TextView jsonBtn = Ui.button(this, getString(R.string.action_copy_json), false);
        jsonBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyJson();
            }
        });
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p2.setMarginStart(Ui.dp(this, 6));
        row.addView(jsonBtn, p2);

        TextView shareJson = Ui.outlineButton(this, getString(R.string.action_share_json));
        shareJson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareJson();
            }
        });
        LinearLayout.LayoutParams p3 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p3.topMargin = Ui.dp(this, 10);
        content.addView(shareJson, p3);
    }

    // ------------------------------------------------------------------ export

    private void shareText() {
        String text = ReportBuilder.toText(this, caps);
        String name = "flipper-one-report.txt";
        File f = ReportBuilder.write(this, name, text);
        if (!f.exists()) {
            Toast.makeText(this, R.string.error_export, Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri = ReportProvider.uriFor(this, name);
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.report_title));
        send.putExtra(Intent.EXTRA_TEXT, text);
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(send, getString(R.string.share_via)));
    }

    private void shareJson() {
        String json = ReportBuilder.toJson(this, caps);
        String name = "flipper-one-report.json";
        File f = ReportBuilder.write(this, name, json);
        if (!f.exists()) {
            Toast.makeText(this, R.string.error_export, Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri = ReportProvider.uriFor(this, name);
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("application/json");
        send.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.report_title));
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(send, getString(R.string.share_via)));
    }

    private void copyJson() {
        String json = ReportBuilder.toJson(this, caps);
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("Flipper One JSON", json));
            Toast.makeText(this, R.string.toast_copied, Toast.LENGTH_SHORT).show();
        }
    }
}
