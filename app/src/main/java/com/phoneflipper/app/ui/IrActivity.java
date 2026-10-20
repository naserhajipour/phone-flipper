package com.phoneflipper.app.ui;

import android.content.Context;
import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.phoneflipper.app.R;
import com.phoneflipper.app.core.BaseActivity;
import com.phoneflipper.app.core.IconDrawable;
import com.phoneflipper.app.core.Palette;
import com.phoneflipper.app.core.Ui;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Infrared emitter lab with NEC demo codes and a transmit log. */
public class IrActivity extends BaseActivity {

    private static final int CARRIER = 38000;

    private ConsumerIrManager ir;
    private LinearLayout logCard;
    private TextView logView;
    private final StringBuilder log = new StringBuilder();

    @Override
    protected void onCreateScreen(Bundle savedInstanceState) {
        ir = (ConsumerIrManager) getSystemService(Context.CONSUMER_IR_SERVICE);
        LinearLayout content = installScreen(R.string.title_ir, R.string.subtitle_ir, true);

        boolean available = ir != null && ir.hasIrEmitter();
        LinearLayout header = Ui.row(this);
        header.addView(Ui.badge(this, getString(available ? R.string.status_native : R.string.status_bridge),
                available ? Palette.SUCCESS : Palette.WARNING));
        header.addView(Ui.badge(this, "38 kHz", Palette.TEXT_DIM));
        content.addView(header);

        TextView intro = Ui.body(this, getString(available ? R.string.ir_intro : R.string.ir_missing_body));
        Ui.add(content, intro, 0, 12, 0, 0);

        if (!available) {
            buildMissing(content);
            content.addView(footer(getString(R.string.ir_footer)));
            return;
        }

        buildEmitterCard(content);
        buildPresets(content);
        buildCustom(content);
        buildLog(content);
        content.addView(footer(getString(R.string.ir_footer)));
        addLog(getString(R.string.ir_ready_log));
    }

    private void buildMissing(LinearLayout content) {
        LinearLayout card = Ui.card(this);
        Ui.add(content, card, 0, 20, 0, 0);
        card.addView(Ui.section(this, getString(R.string.ir_recommended)));
        card.addView(Ui.body(this, getString(R.string.ir_recommended_body)));
        TextView guide = Ui.outlineButton(this, getString(R.string.action_open_bridge));
        guide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                open(BridgeActivity.class);
            }
        });
        Ui.add(card, guide, 0, 14, 0, 0);

        LinearLayout report = Ui.card(this);
        Ui.add(content, report, 0, 14, 0, 0);
        report.addView(Ui.text(this, getString(R.string.ir_phone_note), 13, Palette.TEXT_DIM, false));
    }

    private void buildEmitterCard(LinearLayout content) {
        LinearLayout card = Ui.card(this);
        Ui.add(content, card, 0, 20, 0, 0);
        LinearLayout header = Ui.row(this);
        header.addView(Ui.text(this, getString(R.string.ir_emitter), 15, Palette.TEXT, true),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(Ui.badge(this, "CARRIER", Palette.PRIMARY));
        card.addView(header);

        StringBuilder ranges = new StringBuilder();
        try {
            ConsumerIrManager.CarrierFrequencyRange[] freq = ir.getCarrierFrequencies();
            if (freq != null) {
                for (ConsumerIrManager.CarrierFrequencyRange f : freq) {
                    if (ranges.length() > 0) {
                        ranges.append("  ·  ");
                    }
                    ranges.append(String.format(Locale.US, "%d–%d Hz",
                            f.getMinFrequency(), f.getMaxFrequency()));
                }
            }
        } catch (Exception ignored) {
        }
        TextView rangeView = Ui.mono(this, ranges.length() == 0
                ? getString(R.string.value_unknown) : ranges.toString(), 12, Palette.TEXT_DIM);
        Ui.add(card, rangeView, 0, 10, 0, 0);

        TextView test = Ui.button(this, getString(R.string.ir_test_pulse), false);
        test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                transmitPattern(new int[]{2400, 600, 1200, 600, 600, 600, 2400, 600},
                        getString(R.string.ir_test_pulse));
            }
        });
        Ui.add(card, test, 0, 14, 0, 0);
    }

    private void buildPresets(LinearLayout content) {
        Ui.add(content, Ui.section(this, getString(R.string.ir_presets)), 0, 26, 0, 0);

        int[] labels = {R.string.ir_code_power, R.string.ir_code_mute,
                R.string.ir_code_vol_up, R.string.ir_code_vol_down,
                R.string.ir_code_ch_up, R.string.ir_code_ch_down};
        int[] codes = {0x20DF10EF, 0x20DF906F, 0x20DF40BF,
                0x20DFC03F, 0x20DF827D, 0x20DF02FD};

        for (int i = 0; i < labels.length; i += 2) {
            LinearLayout row = Ui.row(this);
            Ui.add(content, row, 0, 10, 0, 0);
            row.addView(presetButton(labels[i], codes[i]), gridParams(true));
            row.addView(presetButton(labels[i + 1], codes[i + 1]), gridParams(false));
        }

        TextView note = Ui.caption(this, getString(R.string.ir_code_note));
        Ui.add(content, note, 0, 10, 0, 0);
    }

    private LinearLayout.LayoutParams gridParams(boolean start) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p.setMarginStart(Ui.dp(this, start ? 0 : 5));
        p.setMarginEnd(Ui.dp(this, start ? 5 : 0));
        return p;
    }

    private View presetButton(int labelRes, final int code) {
        TextView b = Ui.button(this, getString(labelRes), false);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String hex = String.format(Locale.US, "0x%08X", code);
                transmitPattern(nec(code), hex);
            }
        });
        return b;
    }

    private void buildCustom(LinearLayout content) {
        Ui.add(content, Ui.section(this, getString(R.string.ir_custom)), 0, 26, 0, 0);
        LinearLayout card = Ui.card(this);
        Ui.add(content, card, 0, 10, 0, 0);
        card.addView(Ui.body(this, getString(R.string.ir_custom_body)));

        final EditText input = Ui.input(this, "0x20DF10EF");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        Ui.add(card, input, 0, 12, 0, 0);

        TextView send = Ui.button(this, getString(R.string.action_send_code), false);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = input.getText().toString().trim().replace("0x", "");
                try {
                    int code = (int) Long.parseLong(text, 16);
                    transmitPattern(nec(code), String.format(Locale.US, "0x%08X", code));
                } catch (Exception e) {
                    Toast.makeText(IrActivity.this, R.string.ir_invalid_code, Toast.LENGTH_SHORT).show();
                }
            }
        });
        Ui.add(card, send, 0, 10, 0, 0);
    }

    private void buildLog(LinearLayout content) {
        Ui.add(content, Ui.section(this, getString(R.string.section_log)), 0, 26, 0, 0);
        logCard = Ui.card(this);
        Ui.add(content, logCard, 0, 10, 0, 0);
        logView = Ui.mono(this, "", 11, Palette.TEXT_DIM);
        logView.setLineSpacing(Ui.dp(this, 3), 1f);
        logCard.addView(logView);
    }

    private void transmitPattern(final int[] pattern, final String label) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean ok = true;
                try {
                    ir.transmit(CARRIER, pattern);
                } catch (Exception e) {
                    ok = false;
                }
                final boolean success = ok;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addLog(String.format(Locale.US, "%s %s · %d µs",
                                new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date()),
                                label, total(pattern)) + (success ? "" : " · " + getString(R.string.ir_failed)));
                    }
                });
            }
        }, "ir-tx").start();
    }

    private int total(int[] pattern) {
        int sum = 0;
        for (int p : pattern) {
            sum += p;
        }
        return sum;
    }

    private void addLog(String line) {
        if (logView == null) {
            return;
        }
        if (log.length() > 0) {
            log.insert(0, "\n");
        }
        log.insert(0, line);
        String[] lines = log.toString().split("\n");
        StringBuilder limited = new StringBuilder();
        for (int i = 0; i < Math.min(lines.length, 14); i++) {
            if (i > 0) {
                limited.append('\n');
            }
            limited.append(lines[i]);
        }
        logView.setText(limited.toString());
    }

    /** Standard NEC frame for a 32-bit code. */
    private static int[] nec(int code) {
        int[] pattern = new int[2 + 64 + 2];
        pattern[0] = 9000;
        pattern[1] = 4500;
        int index = 2;
        for (int bit = 0; bit < 32; bit++) {
            pattern[index++] = 560;
            pattern[index++] = ((code >> bit) & 1) == 1 ? 1690 : 560;
        }
        pattern[index++] = 560;
        pattern[index] = 40000;
        return pattern;
    }
}
