package com.phoneflipper.app.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.phoneflipper.app.R;
import com.phoneflipper.app.core.BaseActivity;
import com.phoneflipper.app.core.LocaleManager;
import com.phoneflipper.app.core.Palette;
import com.phoneflipper.app.core.Ui;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/** Renders a bundled guide with a very small Markdown subset. */
public class GuideDetailActivity extends BaseActivity {

    public static final String EXTRA_FILE = "file";

    public static void open(Context c, String file) {
        Intent i = new Intent(c, GuideDetailActivity.class);
        i.putExtra(EXTRA_FILE, file);
        c.startActivity(i);
    }

    @Override
    protected void onCreateScreen(Bundle savedInstanceState) {
        String file = getIntent().getStringExtra(EXTRA_FILE);
        if (file == null) {
            finish();
            return;
        }
        LinearLayout content = installScreen(R.string.title_guides, R.string.subtitle_guides, true);

        String lang = LocaleManager.isPersian(this) ? "fa" : "en";
        String text = read("guides/" + lang + "/" + file + ".txt");
        if (text == null) {
            content.addView(Ui.body(this, getString(R.string.guide_missing)));
            return;
        }
        render(content, text);
    }

    private String read(String path) {
        try {
            InputStream in = getAssets().open(path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private void render(LinearLayout content, String text) {
        String[] lines = text.split("\n");
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) {
                Ui.add(content, Ui.space(this, 8));
                continue;
            }
            if (line.startsWith("### ")) {
                TextView t = Ui.text(this, line.substring(4), 14, Palette.PRIMARY, true);
                Ui.add(content, t, 0, 14, 0, 0);
            } else if (line.startsWith("## ")) {
                TextView t = Ui.text(this, line.substring(3), 16, Palette.TEXT, true);
                Ui.add(content, t, 0, 18, 0, 0);
            } else if (line.startsWith("# ")) {
                TextView t = Ui.title(this, line.substring(2));
                Ui.add(content, t, 0, 8, 0, 0);
            } else if (line.startsWith("> ")) {
                TextView t = Ui.text(this, line.substring(2), 13, Palette.WARNING, false);
                t.setBackground(Ui.rounded(this, Ui.adjustAlpha(Palette.WARNING, 16), 12,
                        Ui.adjustAlpha(Palette.WARNING, 60), 1));
                Ui.padding(t, 14, 12, 14, 12);
                Ui.add(content, t, 0, 10, 0, 0);
            } else if (line.startsWith("- ") || line.startsWith("• ")) {
                LinearLayout row = Ui.row(this);
                row.setGravity(Gravity.TOP);
                TextView dot = Ui.text(this, "•", 14, Palette.PRIMARY, true);
                row.addView(dot);
                TextView body = Ui.body(this, line.substring(2));
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                p.setMarginStart(Ui.dp(this, 8));
                row.addView(body, p);
                Ui.add(content, row, 0, 6, 0, 0);
            } else if (line.matches("^\\d+\\).*")) {
                TextView t = Ui.body(this, line);
                t.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                Ui.add(content, t, 2, 5, 0, 0);
            } else if (line.equals("---")) {
                Ui.add(content, Ui.divider(this), 0, 14, 0, 14);
            } else {
                TextView t = Ui.body(this, line);
                t.setTextSize(14);
                Ui.add(content, t, 0, 5, 0, 0);
            }
        }
        content.addView(footer(getString(R.string.guides_offline_note)));
    }
}
