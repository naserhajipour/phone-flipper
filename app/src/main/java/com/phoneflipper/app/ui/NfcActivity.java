package com.phoneflipper.app.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.phoneflipper.app.R;
import com.phoneflipper.app.core.BaseActivity;
import com.phoneflipper.app.core.IconDrawable;
import com.phoneflipper.app.core.Palette;
import com.phoneflipper.app.core.Prefs;
import com.phoneflipper.app.core.Ui;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** NFC reader: UID, technologies, NDEF payloads and a local scan history. */
public class NfcActivity extends BaseActivity {

    private NfcAdapter adapter;
    private LinearLayout content;
    private LinearLayout lastCard;
    private LinearLayout historyCard;
    private TextView stateText;
    private ImageView stateIcon;

    @Override
    protected void onCreateScreen(Bundle savedInstanceState) {
        adapter = NfcAdapter.getDefaultAdapter(this);
        content = installScreen(R.string.title_nfc, R.string.subtitle_nfc, true);
        addAction(IconDrawable.TRASH, new Runnable() {
            @Override
            public void run() {
                clearHistory();
            }
        });

        buildStateCard();
        lastCard = Ui.column(this);
        content.addView(lastCard);
        buildHistory();

        content.addView(footer(getString(R.string.nfc_footer)));
    }

    private void buildStateCard() {
        LinearLayout card = Ui.card(this);
        Ui.add(content, card, 0, 12, 0, 0);

        LinearLayout top = Ui.row(this);
        stateIcon = Ui.icon(this, IconDrawable.NFC, Palette.PRIMARY, 26);
        stateIcon.setBackground(Ui.rounded(this, Ui.adjustAlpha(Palette.PRIMARY, 22), 14,
                Color.TRANSPARENT, 0));
        Ui.padding(stateIcon, 12);
        top.addView(stateIcon, new LinearLayout.LayoutParams(Ui.dp(this, 50), Ui.dp(this, 50)));

        LinearLayout texts = Ui.column(this);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tp.setMarginStart(Ui.dp(this, 14));
        top.addView(texts, tp);
        stateText = Ui.text(this, getString(R.string.nfc_initializing), 16, Palette.TEXT, true);
        texts.addView(stateText);
        TextView hint = Ui.caption(this, getString(R.string.nfc_hold_hint));
        Ui.add(texts, hint, 0, 4, 0, 0);
        card.addView(top);

        if (adapter == null) {
            stateText.setText(R.string.nfc_unavailable);
            stateText.setTextColor(Palette.WARNING);
            stateIcon = null;
            TextView note = Ui.body(this, getString(R.string.nfc_unavailable_body));
            Ui.add(card, note, 0, 14, 0, 0);
            return;
        }
        if (!adapter.isEnabled()) {
            stateText.setText(R.string.nfc_disabled);
            stateText.setTextColor(Palette.WARNING);
            TextView button = Ui.outlineButton(this, getString(R.string.nfc_open_settings));
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
                }
            });
            Ui.add(card, button, 0, 14, 0, 0);
        }
    }

    private void buildHistory() {
        Ui.add(content, Ui.section(this, getString(R.string.nfc_history)), 0, 26, 0, 0);
        historyCard = Ui.card(this);
        Ui.add(content, historyCard, 0, 10, 0, 0);
        renderHistory();
    }

    private void renderHistory() {
        historyCard.removeAllViews();
        List<Prefs.NfcRecord> records = Prefs.nfcHistory(this);
        if (records.isEmpty()) {
            historyCard.addView(Ui.body(this, getString(R.string.nfc_no_history)));
            return;
        }
        SimpleDateFormat fmt = new SimpleDateFormat("MMM d · HH:mm", Locale.US);
        boolean first = true;
        for (final Prefs.NfcRecord r : records) {
            if (!first) {
                LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Ui.dp(this, 0.7f)));
                dp.topMargin = Ui.dp(this, 12);
                dp.bottomMargin = Ui.dp(this, 12);
                historyCard.addView(Ui.divider(this), dp);
            }
            first = false;
            LinearLayout row = Ui.row(this);
            row.setGravity(Gravity.TOP);
            LinearLayout texts = Ui.column(this);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            row.addView(texts, tp);
            texts.addView(Ui.mono(this, r.uid, 13, Palette.TEXT));
            String meta = fmt.format(new Date(r.timestamp)) + " · " + r.tech;
            TextView m = Ui.caption(this, meta);
            Ui.add(texts, m, 0, 3, 0, 0);
            if (r.payload != null && !r.payload.isEmpty()) {
                TextView p = Ui.text(this, r.payload, 12, Palette.TEXT_DIM, false);
                p.setMaxLines(2);
                Ui.add(texts, p, 0, 4, 0, 0);
            }
            TextView copy = Ui.caption(this, getString(R.string.action_copy));
            copy.setTextColor(Palette.PRIMARY);
            copy.setPadding(Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 8), Ui.dp(this, 4));
            copy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    copyToClipboard(r.uid);
                }
            });
            row.addView(copy);
            historyCard.addView(row);
        }
    }

    private void showTag(final String uid, String tech, String payload) {
        lastCard.removeAllViews();
        Ui.add(lastCard, Ui.section(this, getString(R.string.nfc_last_scan)), 0, 26, 0, 0);
        LinearLayout card = Ui.card(this);
        Ui.add(lastCard, card, 0, 10, 0, 0);

        LinearLayout header = Ui.row(this);
        TextView label = Ui.caption(this, getString(R.string.nfc_uid));
        header.addView(label, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView copy = Ui.caption(this, getString(R.string.action_copy));
        copy.setTextColor(Palette.PRIMARY);
        copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard(uid);
            }
        });
        header.addView(copy);
        card.addView(header);

        TextView uidView = Ui.mono(this, uid, 19, Palette.TEXT);
        uidView.setLetterSpacing(0.06f);
        Ui.add(card, uidView, 0, 6, 0, 0);

        TextView techView = Ui.text(this, tech, 11.5f, Palette.TEXT_MUTED, false);
        Ui.add(card, techView, 0, 8, 0, 0);

        if (payload != null && !payload.isEmpty()) {
            TextView payloadTitle = Ui.section(this, getString(R.string.nfc_payload));
            Ui.add(card, payloadTitle, 0, 16, 0, 0);
            TextView payloadView = Ui.text(this, payload, 13, Palette.TEXT_DIM, false);
            payloadView.setTextIsSelectable(true);
            Ui.add(card, payloadView, 0, 6, 0, 0);
        }
    }

    private void copyToClipboard(String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("NFC UID", text));
            Toast.makeText(this, R.string.toast_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void clearHistory() {
        Prefs.clearNfcHistory(this);
        renderHistory();
        Toast.makeText(this, R.string.nfc_history_cleared, Toast.LENGTH_SHORT).show();
    }

    // ------------------------------------------------------------------ reader

    private final NfcAdapter.ReaderCallback readerCallback = new NfcAdapter.ReaderCallback() {
        @Override
        public void onTagDiscovered(final Tag tag) {
            final String uid = hex(tag.getId());
            final StringBuilder tech = new StringBuilder();
            for (String t : tag.getTechList()) {
                if (tech.length() > 0) {
                    tech.append(" · ");
                }
                tech.append(t.substring(t.lastIndexOf('.') + 1));
            }
            String payload = readNdef(tag);
            final String payloadFinal = payload;
            Prefs.addNfcRecord(NfcActivity.this,
                    new Prefs.NfcRecord(uid, tech.toString().isEmpty() ? "—" : tech.toString(),
                            payload, System.currentTimeMillis()));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showTag(uid, tech.length() == 0 ? "—" : tech.toString(), payloadFinal);
                    renderHistory();
                }
            });
        }
    };

    private String readNdef(Tag tag) {
        Ndef ndef = Ndef.get(tag);
        if (ndef == null) {
            return "";
        }
        try {
            ndef.connect();
            NdefMessage message = ndef.getNdefMessage();
            ndef.close();
            if (message == null) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (NdefRecord record : message.getRecords()) {
                String parsed = parseRecord(record);
                if (parsed != null && !parsed.isEmpty()) {
                    if (sb.length() > 0) {
                        sb.append('\n');
                    }
                    sb.append(parsed);
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String parseRecord(NdefRecord record) {
        try {
            if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN) {
                if (Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {
                    byte[] payload = record.getPayload();
                    int status = payload[0] & 0xFF;
                    int langLength = status & 0x3F;
                    Charset charset = (status & 0x80) == 0 ? Charset.forName("UTF-8")
                            : Charset.forName("UTF-16");
                    return new String(payload, 1 + langLength,
                            payload.length - 1 - langLength, charset);
                }
                if (Arrays.equals(record.getType(), NdefRecord.RTD_URI)) {
                    byte[] payload = record.getPayload();
                    int prefix = payload[0] & 0xFF;
                    String[] prefixes = {"", "http://www.", "https://www.", "http://",
                            "https://", "tel:", "mailto:", "ftp://anonymous:anonymous@",
                            "ftp://ftp.", "ftps://", "sftp://", "smb://", "nfs://",
                            "ftp://", "dav://", "news:", "telnet://", "imap:",
                            "rtsp://", "urn:", "pop:", "sip:", "sips:", "tftp:",
                            "btspp://", "btl2cap://", "btgoep://", "tcpobex://",
                            "irdaobex://", "file://", "urn:epc:id:", "urn:epc:tag:",
                            "urn:epc:pat:", "urn:epc:raw:", "urn:epc:", "urn:nfc:"};
                    String p = prefix < prefixes.length ? prefixes[prefix] : "";
                    return p + new String(payload, 1, payload.length - 1, "UTF-8");
                }
            }
            if (record.getTnf() == NdefRecord.TNF_MIME_MEDIA) {
                return new String(record.getType(), "UTF-8") + ": "
                        + new String(record.getPayload(), "UTF-8");
            }
            if (record.getTnf() == NdefRecord.TNF_ABSOLUTE_URI) {
                return new String(record.getPayload(), "UTF-8");
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private static String hex(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            if (sb.length() > 0) {
                sb.append(':');
            }
            sb.append(String.format(Locale.US, "%02X", b));
        }
        return sb.toString();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null && adapter.isEnabled()) {
            stateText.setText(R.string.nfc_ready);
            stateText.setTextColor(Palette.SUCCESS);
            adapter.enableReaderMode(this, readerCallback,
                    NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B
                            | NfcAdapter.FLAG_READER_NFC_F | NfcAdapter.FLAG_READER_NFC_V
                            | NfcAdapter.FLAG_READER_NFC_BARCODE, null);
        } else if (adapter != null) {
            stateText.setText(R.string.nfc_disabled);
            stateText.setTextColor(Palette.WARNING);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (adapter != null) {
            adapter.disableReaderMode(this);
        }
    }
}
