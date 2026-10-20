package com.phoneflipper.app.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.phoneflipper.app.R;
import com.phoneflipper.app.core.BaseActivity;
import com.phoneflipper.app.core.IconDrawable;
import com.phoneflipper.app.core.Palette;
import com.phoneflipper.app.core.Ui;

/** Offline field guide library. Content ships inside the APK as plain text. */
public class GuidesActivity extends BaseActivity {

    private static final String[] FILES = {
            "quickstart", "bridge", "safety", "nfc", "emf", "privacy"
    };

    private static final int[] TITLES = {
            R.string.guide_quickstart, R.string.guide_bridge, R.string.guide_safety,
            R.string.guide_nfc, R.string.guide_emf, R.string.guide_privacy
    };

    private static final int[] SUBTITLES = {
            R.string.guide_quickstart_sub, R.string.guide_bridge_sub, R.string.guide_safety_sub,
            R.string.guide_nfc_sub, R.string.guide_emf_sub, R.string.guide_privacy_sub
    };

    private static final int[] ICONS = {
            IconDrawable.BOLT, IconDrawable.ANTENNA, IconDrawable.WARNING,
            IconDrawable.NFC, IconDrawable.MAGNET, IconDrawable.LOCK
    };

    @Override
    protected void onCreateScreen(Bundle savedInstanceState) {
        LinearLayout content = installScreen(R.string.title_guides, R.string.subtitle_guides, true);
        TextView intro = Ui.body(this, getString(R.string.guides_intro));
        content.addView(intro);

        LinearLayout card = Ui.card(this);
        Ui.add(content, card, 0, 16, 0, 0);

        for (int i = 0; i < FILES.length; i++) {
            final String file = FILES[i];
            if (i > 0) {
                LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Ui.dp(this, 0.7f)));
                dp.topMargin = Ui.dp(this, 12);
                dp.bottomMargin = Ui.dp(this, 12);
                card.addView(Ui.divider(this), dp);
            }
            LinearLayout row = Ui.row(this);
            row.setClickable(true);
            row.setFocusable(true);
            row.setBackground(Ui.ripple(this,
                    Ui.rounded(this, Color.TRANSPARENT, 12, Color.TRANSPARENT, 0), Palette.RIPPLE));
            Ui.padding(row, 2, 4, 2, 4);
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    GuideDetailActivity.open(GuidesActivity.this, file);
                }
            });

            ImageView icon = Ui.icon(this, ICONS[i], Palette.PRIMARY, 19);
            icon.setBackground(Ui.rounded(this, Ui.adjustAlpha(Palette.PRIMARY, 20), 11,
                    Color.TRANSPARENT, 0));
            Ui.padding(icon, 9);
            row.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, 37), Ui.dp(this, 37)));

            LinearLayout texts = Ui.column(this);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tp.setMarginStart(Ui.dp(this, 12));
            row.addView(texts, tp);
            texts.addView(Ui.text(this, getString(TITLES[i]), 14, Palette.TEXT, true));
            TextView sub = Ui.caption(this, getString(SUBTITLES[i]));
            Ui.add(texts, sub, 0, 3, 0, 0);

            ImageView chevron = Ui.icon(this, IconDrawable.CHEVRON, Palette.TEXT_MUTED, 16);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    Ui.dp(this, 16), Ui.dp(this, 16));
            cp.setMarginStart(Ui.dp(this, 8));
            chevron.setLayoutParams(cp);
            row.addView(chevron);
            card.addView(row);
        }

        TextView note = Ui.caption(this, getString(R.string.guides_offline_note));
        note.setGravity(Gravity.CENTER);
        Ui.add(content, note, 8, 22, 8, 0);
    }
}
