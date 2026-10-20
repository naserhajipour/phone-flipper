package com.phoneflipper.app.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.phoneflipper.app.BuildConfig;
import com.phoneflipper.app.R;
import com.phoneflipper.app.core.BaseActivity;
import com.phoneflipper.app.core.IconDrawable;
import com.phoneflipper.app.core.Palette;
import com.phoneflipper.app.core.Prefs;
import com.phoneflipper.app.core.Ui;

import java.util.Locale;

/** Language, units, privacy and app information. */
public class SettingsActivity extends BaseActivity {

    private LinearLayout content;

    @Override
    protected void onCreateScreen(Bundle savedInstanceState) {
        content = installScreen(R.string.title_settings, R.string.subtitle_settings, true);
        buildLanguage();
        buildUnits();
        buildBehavior();
        buildData();
        buildAbout();
        content.addView(footer(String.format(Locale.US, "Flipper One v%s (%d)",
                BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)));
    }

    private void buildLanguage() {
        section(R.string.section_language);
        LinearLayout card = card();
        addChoice(card, Prefs.LANG_SYSTEM, R.string.language_system, R.string.language_system_sub);
        divider(card);
        addChoice(card, Prefs.LANG_EN, R.string.language_en, R.string.language_en_sub);
        divider(card);
        addChoice(card, Prefs.LANG_FA, R.string.language_fa, R.string.language_fa_sub);
    }

    private void buildUnits() {
        section(R.string.section_units);
        LinearLayout card = card();
        card.addView(Ui.body(this, getString(R.string.units_body)));
        LinearLayout row = Ui.row(this);
        Ui.add(card, row, 0, 12, 0, 0);
        row.addView(unitButton(Prefs.UNIT_UT, "μT", true), weight(true));
        row.addView(unitButton(Prefs.UNIT_MG, "mG", false), weight(false));
    }

    private LinearLayout.LayoutParams weight(boolean start) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p.setMarginStart(Ui.dp(this, start ? 0 : 5));
        p.setMarginEnd(Ui.dp(this, start ? 5 : 0));
        return p;
    }

    private TextView unitButton(final String unit, String label, boolean start) {
        final boolean selected = Prefs.emfUnit(this).equals(unit);
        TextView b = Ui.button(this, label, selected);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.setEmfUnit(SettingsActivity.this, unit);
                recreate();
            }
        });
        return b;
    }

    private void buildBehavior() {
        section(R.string.section_behavior);
        LinearLayout card = card();
        addSwitch(card, R.string.setting_keep_on, R.string.setting_keep_on_sub,
                Prefs.keepScreenOn(this), new SwitchListener() {
                    @Override
                    public void onChanged(boolean checked) {
                        Prefs.setKeepScreenOn(SettingsActivity.this, checked);
                    }
                });
        divider(card);
        addSwitch(card, R.string.setting_haptics, R.string.setting_haptics_sub,
                Prefs.haptics(this), new SwitchListener() {
                    @Override
                    public void onChanged(boolean checked) {
                        Prefs.setHaptics(SettingsActivity.this, checked);
                    }
                });
    }

    private void buildData() {
        section(R.string.section_data);
        LinearLayout card = card();

        LinearLayout clearNfc = actionRow(R.string.data_clear_nfc, R.string.data_clear_nfc_sub,
                IconDrawable.TRASH);
        clearNfc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.clearNfcHistory(SettingsActivity.this);
                Toast.makeText(SettingsActivity.this, R.string.nfc_history_cleared,
                        Toast.LENGTH_SHORT).show();
            }
        });
        card.addView(clearNfc);
        divider(card);

        LinearLayout clearAll = actionRow(R.string.data_clear_all, R.string.data_clear_all_sub,
                IconDrawable.CLOSE);
        clearAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmReset();
            }
        });
        card.addView(clearAll);
    }

    private void buildAbout() {
        section(R.string.section_about);
        LinearLayout card = card();
        addFact(card, R.string.about_version,
                String.format(Locale.US, "%s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
        divider(card);
        addFact(card, R.string.about_package, BuildConfig.APPLICATION_ID);
        divider(card);
        addFact(card, R.string.about_network, getString(R.string.about_network_value));

        TextView privacyTitle = Ui.section(this, getString(R.string.about_privacy));
        Ui.add(card, privacyTitle, 0, 20, 0, 0);
        TextView privacy = Ui.body(this, getString(R.string.about_privacy_body));
        Ui.add(card, privacy, 0, 8, 0, 0);

        TextView permTitle = Ui.section(this, getString(R.string.about_permissions));
        Ui.add(card, permTitle, 0, 20, 0, 0);
        TextView perms = Ui.body(this, getString(R.string.about_permissions_body));
        Ui.add(card, perms, 0, 8, 0, 0);

        TextView legalTitle = Ui.section(this, getString(R.string.about_disclaimer));
        Ui.add(card, legalTitle, 0, 20, 0, 0);
        TextView legal = Ui.body(this, getString(R.string.about_disclaimer_body));
        Ui.add(card, legal, 0, 8, 0, 0);

        TextView review = Ui.outlineButton(this, getString(R.string.about_review_notice));
        review.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.setSafetyAccepted(SettingsActivity.this, false);
                Intent i = new Intent(SettingsActivity.this, DashboardActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finish();
            }
        });
        Ui.add(card, review, 0, 18, 0, 0);
    }

    // ------------------------------------------------------------------ widgets

    private void section(int textRes) {
        Ui.add(content, Ui.section(this, getString(textRes)), 0, 26, 0, 0);
    }

    private LinearLayout card() {
        LinearLayout card = Ui.card(this);
        Ui.add(content, card, 0, 10, 0, 0);
        return card;
    }

    private void divider(LinearLayout card) {
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Ui.dp(this, 0.7f)));
        dp.topMargin = Ui.dp(this, 14);
        dp.bottomMargin = Ui.dp(this, 14);
        card.addView(Ui.divider(this), dp);
    }

    private void addChoice(LinearLayout card, final String lang, int title, int subtitle) {
        boolean selected = Prefs.language(this).equals(lang);
        LinearLayout row = Ui.row(this);
        row.setClickable(true);
        row.setFocusable(true);
        row.setBackground(Ui.ripple(this, Ui.rounded(this, Color.TRANSPARENT, 10,
                Color.TRANSPARENT, 0), Palette.RIPPLE));
        Ui.padding(row, 2, 4, 2, 4);

        LinearLayout texts = Ui.column(this);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(texts, tp);
        texts.addView(Ui.text(this, getString(title), 14, Palette.TEXT, selected));
        TextView sub = Ui.caption(this, getString(subtitle));
        Ui.add(texts, sub, 0, 3, 0, 0);

        android.widget.ImageView check = Ui.icon(this,
                selected ? IconDrawable.CHECK : IconDrawable.CHEVRON,
                selected ? Palette.PRIMARY : Palette.TEXT_MUTED, 17);
        row.addView(check);

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Prefs.language(SettingsActivity.this).equals(lang)) {
                    Prefs.setLanguage(SettingsActivity.this, lang);
                    restartApp();
                }
            }
        });
        card.addView(row);
    }

    private void restartApp() {
        Intent i = new Intent(this, DashboardActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finishAffinity();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void addSwitch(LinearLayout card, int title, int subtitle, boolean checked,
                           final SwitchListener listener) {
        LinearLayout row = Ui.row(this);
        LinearLayout texts = Ui.column(this);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(texts, tp);
        texts.addView(Ui.text(this, getString(title), 14, Palette.TEXT, true));
        TextView sub = Ui.caption(this, getString(subtitle));
        Ui.add(texts, sub, 0, 3, 0, 0);

        Switch toggle = new Switch(this);
        toggle.setChecked(checked);
        toggle.setContentDescription(getString(title));
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                listener.onChanged(isChecked);
            }
        });
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.setMarginStart(Ui.dp(this, 12));
        row.addView(toggle, sp);
        card.addView(row);
    }

    private LinearLayout actionRow(int title, int subtitle, int icon) {
        LinearLayout row = Ui.row(this);
        row.setClickable(true);
        row.setFocusable(true);
        row.setBackground(Ui.ripple(this, Ui.rounded(this, Color.TRANSPARENT, 10,
                Color.TRANSPARENT, 0), Palette.RIPPLE));
        Ui.padding(row, 2, 4, 2, 4);
        row.addView(Ui.icon(this, icon, Palette.TEXT_MUTED, 18));

        LinearLayout texts = Ui.column(this);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tp.setMarginStart(Ui.dp(this, 12));
        row.addView(texts, tp);
        texts.addView(Ui.text(this, getString(title), 14, Palette.TEXT, true));
        TextView sub = Ui.caption(this, getString(subtitle));
        Ui.add(texts, sub, 0, 3, 0, 0);
        return row;
    }

    private void addFact(LinearLayout card, int label, String value) {
        LinearLayout row = Ui.row(this);
        TextView l = Ui.text(this, getString(label), 13, Palette.TEXT_MUTED, false);
        row.addView(l, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView v = Ui.mono(this, value, 12, Palette.TEXT_DIM);
        v.setGravity(Gravity.END);
        row.addView(v);
        card.addView(row);
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.data_clear_all)
                .setMessage(R.string.data_clear_all_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Prefs.clearAll(SettingsActivity.this);
                        restartApp();
                    }
                })
                .show();
    }

    private interface SwitchListener {
        void onChanged(boolean checked);
    }
}
