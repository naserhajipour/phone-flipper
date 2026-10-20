package com.phoneflipper.app.ui;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.phoneflipper.app.R;
import com.phoneflipper.app.core.BaseActivity;
import com.phoneflipper.app.core.IconDrawable;
import com.phoneflipper.app.core.Palette;
import com.phoneflipper.app.core.Prefs;
import com.phoneflipper.app.core.Ui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Bluetooth SPP console for the ESP32 / HC-05 radio bridge. */
public class BridgeActivity extends BaseActivity {

    private static final UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_BT = 5101;

    private final Handler main = new Handler(Looper.getMainLooper());
    private BluetoothAdapter adapter;
    private BluetoothSocket socket;
    private OutputStream output;
    private boolean connected;

    private LinearLayout deviceList;
    private TextView statusPill;
    private TextView console;
    private TextView connectState;
    private final Map<String, BluetoothDevice> found = new LinkedHashMap<>();
    private final List<String> logLines = new ArrayList<>();

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    found.put(device.getAddress(), device);
                    renderDevices();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                appendLog("· " + getString(R.string.bridge_scan_finished));
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                renderDevices();
            }
        }
    };

    @Override
    protected void onCreateScreen(Bundle savedInstanceState) {
        BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = bm == null ? null : bm.getAdapter();

        LinearLayout content = installScreen(R.string.title_bridge, R.string.subtitle_bridge, true);
        addAction(IconDrawable.TRASH, R.string.section_console, new Runnable() {
            @Override
            public void run() {
                logLines.clear();
                renderConsole();
            }
        });

        LinearLayout header = Ui.row(this);
        statusPill = Ui.badge(this, getString(R.string.bridge_disconnected), Palette.TEXT_MUTED);
        header.addView(statusPill);
        if (adapter != null) {
            header.addView(Ui.badge(this, getString(R.string.bridge_spp), Palette.PRIMARY));
        }
        content.addView(header);

        TextView intro = Ui.body(this, getString(R.string.bridge_intro));
        Ui.add(content, intro, 0, 12, 0, 0);

        if (adapter == null) {
            LinearLayout card = Ui.card(this);
            Ui.add(content, card, 0, 18, 0, 0);
            card.addView(Ui.text(this, getString(R.string.bridge_unavailable), 15, Palette.WARNING, true));
            card.addView(Ui.body(this, getString(R.string.bridge_unavailable_body)));
            return;
        }

        buildDeviceCard(content);
        buildConsole(content);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, filter);
        }

        ensurePermissions();
        renderDevices();
    }

    // ------------------------------------------------------------------ devices

    private void buildDeviceCard(LinearLayout content) {
        Ui.add(content, Ui.section(this, getString(R.string.section_devices)), 0, 24, 0, 0);
        LinearLayout card = Ui.card(this);
        Ui.add(content, card, 0, 10, 0, 0);

        LinearLayout header = Ui.row(this);
        header.addView(Ui.text(this, getString(R.string.bridge_paired), 14, Palette.TEXT, true),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView scan = Ui.caption(this, getString(R.string.bridge_scan));
        scan.setTextColor(Palette.PRIMARY);
        scan.setPadding(Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 8), Ui.dp(this, 4));
        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScan();
            }
        });
        header.addView(scan);
        card.addView(header);

        deviceList = Ui.column(this);
        Ui.add(card, deviceList, 0, 10, 0, 0);
    }

    private void renderDevices() {
        if (deviceList == null) {
            return;
        }
        deviceList.removeAllViews();
        if (!hasConnectPermission()) {
            deviceList.addView(Ui.body(this, getString(R.string.bridge_permission_needed)));
            return;
        }
        if (!adapter.isEnabled()) {
            deviceList.addView(Ui.body(this, getString(R.string.bridge_bt_off)));
            TextView enable = Ui.outlineButton(this, getString(R.string.bridge_enable));
            enable.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                }
            });
            Ui.add(deviceList, enable, 0, 10, 0, 0);
            return;
        }
        try {
            for (BluetoothDevice device : adapter.getBondedDevices()) {
                found.put(device.getAddress(), device);
            }
        } catch (SecurityException ignored) {
        }
        if (found.isEmpty()) {
            deviceList.addView(Ui.body(this, getString(R.string.bridge_no_devices)));
            return;
        }
        List<BluetoothDevice> devices = new ArrayList<>(found.values());
        Collections.sort(devices, new Comparator<BluetoothDevice>() {
            @Override
            public int compare(BluetoothDevice a, BluetoothDevice b) {
                return safeName(a).compareToIgnoreCase(safeName(b));
            }
        });
        boolean first = true;
        for (final BluetoothDevice device : devices) {
            if (!first) {
                LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Ui.dp(this, 0.7f)));
                dp.topMargin = Ui.dp(this, 10);
                dp.bottomMargin = Ui.dp(this, 10);
                deviceList.addView(Ui.divider(this), dp);
            }
            first = false;
            LinearLayout row = Ui.row(this);
            row.setClickable(true);
            row.setFocusable(true);
            row.setBackground(Ui.ripple(this, Ui.rounded(this, Color.TRANSPARENT, 10,
                    Color.TRANSPARENT, 0), Palette.RIPPLE));
            Ui.padding(row, 2, 5, 2, 5);

            android.widget.ImageView icon = Ui.icon(this, IconDrawable.BLUETOOTH,
                    device.getBondState() == BluetoothDevice.BOND_BONDED
                            ? Palette.PRIMARY : Palette.TEXT_MUTED, 19);
            row.addView(icon);

            LinearLayout texts = Ui.column(this);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tp.setMarginStart(Ui.dp(this, 12));
            row.addView(texts, tp);
            texts.addView(Ui.text(this, safeName(device), 13.5f, Palette.TEXT, true));
            texts.addView(Ui.mono(this, device.getAddress(), 11, Palette.TEXT_MUTED));

            TextView state = Ui.badge(this, device.getBondState() == BluetoothDevice.BOND_BONDED
                    ? getString(R.string.bridge_paired_badge)
                    : getString(R.string.bridge_found_badge), Palette.TEXT_MUTED);
            row.addView(state);

            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    connect(device);
                }
            });
            deviceList.addView(row);
        }
    }

    private String safeName(BluetoothDevice device) {
        try {
            String name = device.getName();
            return name == null || name.trim().isEmpty() ? getString(R.string.bridge_unknown) : name;
        } catch (SecurityException e) {
            return getString(R.string.bridge_unknown);
        }
    }

    private void startScan() {
        if (!hasScanPermission()) {
            ensurePermissions();
            return;
        }
        if (!adapter.isEnabled()) {
            Toast.makeText(this, R.string.bridge_bt_off, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            if (adapter.isDiscovering()) {
                adapter.cancelDiscovery();
            }
            adapter.startDiscovery();
            appendLog("· " + getString(R.string.bridge_scanning));
        } catch (SecurityException e) {
            Toast.makeText(this, R.string.bridge_permission_needed, Toast.LENGTH_SHORT).show();
        }
    }

    // ------------------------------------------------------------------ console

    private void buildConsole(LinearLayout content) {
        Ui.add(content, Ui.section(this, getString(R.string.section_console)), 0, 26, 0, 0);

        LinearLayout card = Ui.card(this);
        Ui.add(content, card, 0, 10, 0, 0);

        connectState = Ui.caption(this, getString(R.string.bridge_not_connected));
        card.addView(connectState);

        ScrollView consoleScroll = new ScrollView(this);
        consoleScroll.setVerticalScrollBarEnabled(false);
        LinearLayout.LayoutParams csp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 170));
        csp.topMargin = Ui.dp(this, 10);
        consoleScroll.setBackground(Ui.rounded(this, Palette.BG_ELEV, 12, Palette.STROKE_SOFT, 1));
        Ui.padding(consoleScroll, 12, 10, 12, 10);
        console = Ui.mono(this, getString(R.string.bridge_console_hint), 11, Palette.TEXT_DIM);
        console.setLineSpacing(Ui.dp(this, 3), 1f);
        consoleScroll.addView(console);
        card.addView(consoleScroll, csp);

        LinearLayout inputRow = Ui.row(this);
        Ui.add(card, inputRow, 0, 12, 0, 0);
        final EditText input = Ui.input(this, "STATUS");
        inputRow.addView(input, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView send = Ui.button(this, getString(R.string.action_send), true);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.setMarginStart(Ui.dp(this, 8));
        inputRow.addView(send, sp);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = input.getText().toString().trim();
                if (!text.isEmpty()) {
                    sendLine(text);
                    input.setText("");
                }
            }
        });

        Ui.add(card, Ui.section(this, getString(R.string.bridge_presets)), 0, 18, 0, 0);
        String[][] presets = {
                {"STATUS", getString(R.string.preset_status)},
                {"SET_FREQUENCY 433920000", getString(R.string.preset_frequency)},
                {"SEND_RAW A1B2C3D4", getString(R.string.preset_raw)},
                {"RECORD_IR_START", getString(R.string.preset_ir_record)},
                {"PLAY_IR", getString(R.string.preset_ir_play)},
                {"PING", getString(R.string.preset_ping)},
        };
        for (int i = 0; i < presets.length; i += 2) {
            LinearLayout row = Ui.row(this);
            Ui.add(card, row, 0, 8, 0, 0);
            row.addView(presetChip(presets[i][0], presets[i][1]), presetParams(true));
            if (i + 1 < presets.length) {
                row.addView(presetChip(presets[i + 1][0], presets[i + 1][1]), presetParams(false));
            }
        }

        TextView note = Ui.caption(this, getString(R.string.bridge_protocol_note));
        Ui.add(card, note, 0, 14, 0, 0);
        content.addView(footer(getString(R.string.bridge_footer)));
    }

    private LinearLayout.LayoutParams presetParams(boolean start) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p.setMarginStart(Ui.dp(this, start ? 0 : 4));
        p.setMarginEnd(Ui.dp(this, start ? 4 : 0));
        return p;
    }

    private View presetChip(final String command, String label) {
        TextView chip = Ui.button(this, label, false);
        chip.setTextSize(11.5f);
        chip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendLine(command);
            }
        });
        return chip;
    }

    private void sendLine(String line) {
        if (!connected || output == null) {
            Toast.makeText(this, R.string.bridge_not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            output.write((line + "\n").getBytes(Charset.forName("UTF-8")));
            output.flush();
            appendLog("> " + line);
        } catch (Exception e) {
            appendLog("! " + getString(R.string.bridge_send_failed));
            closeSocket();
        }
    }

    private void appendLog(final String line) {
        main.post(new Runnable() {
            @Override
            public void run() {
                String stamp = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
                logLines.add(stamp + "  " + line);
                while (logLines.size() > 120) {
                    logLines.remove(0);
                }
                renderConsole();
            }
        });
    }

    private void renderConsole() {
        if (console == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (String line : logLines) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(line);
        }
        console.setText(sb.length() == 0 ? getString(R.string.bridge_console_hint) : sb.toString());
    }

    // ------------------------------------------------------------------ socket

    private void connect(final BluetoothDevice device) {
        if (!hasConnectPermission()) {
            ensurePermissions();
            return;
        }
        closeSocket();
        setStatus(R.string.bridge_connecting, Palette.WARNING);
        appendLog("· " + getString(R.string.bridge_connecting_to) + " " + safeName(device));
        Prefs.setBridgeAddress(this, device.getAddress());

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    try {
                        adapter.cancelDiscovery();
                    } catch (SecurityException ignored) {
                    }
                    BluetoothSocket s = device.createRfcommSocketToServiceRecord(SPP);
                    s.connect();
                    socket = s;
                    output = s.getOutputStream();
                    connected = true;
                    main.post(new Runnable() {
                        @Override
                        public void run() {
                            setStatus(R.string.bridge_connected, Palette.SUCCESS);
                            connectState.setText(getString(R.string.bridge_connected_to,
                                    safeName(device), device.getAddress()));
                        }
                    });
                    appendLog("· " + getString(R.string.bridge_connected));
                    listen(new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8")));
                } catch (Exception e) {
                    connected = false;
                    main.post(new Runnable() {
                        @Override
                        public void run() {
                            setStatus(R.string.bridge_disconnected, Palette.DANGER);
                            connectState.setText(R.string.bridge_connect_failed);
                        }
                    });
                    appendLog("! " + getString(R.string.bridge_connect_failed));
                }
            }
        }, "spp-connect").start();
    }

    private void listen(BufferedReader reader) {
        try {
            String line;
            while (connected && (line = reader.readLine()) != null) {
                appendLog("< " + line);
            }
        } catch (Exception ignored) {
        }
        if (connected) {
            appendLog("· " + getString(R.string.bridge_link_closed));
        }
        connected = false;
        main.post(new Runnable() {
            @Override
            public void run() {
                setStatus(R.string.bridge_disconnected, Palette.TEXT_MUTED);
            }
        });
    }

    private void closeSocket() {
        connected = false;
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
            socket = null;
        }
        output = null;
        setStatus(R.string.bridge_disconnected, Palette.TEXT_MUTED);
    }

    private void setStatus(int textRes, int color) {
        if (statusPill == null) {
            return;
        }
        statusPill.setText(textRes);
        statusPill.setTextColor(color);
        statusPill.setBackground(Ui.rounded(this, Ui.adjustAlpha(color, 30), 7,
                Ui.adjustAlpha(color, 90), 1));
    }

    // ------------------------------------------------------------------ permissions

    private boolean hasConnectPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasScanPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void ensurePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            List<String> missing = new ArrayList<>();
            if (!hasConnectPermission()) {
                missing.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (!hasScanPermission()) {
                missing.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (!missing.isEmpty()) {
                requestPermissions(missing.toArray(new String[0]), REQUEST_BT);
            }
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_BT);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BT) {
            renderDevices();
        }
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(receiver);
        } catch (Exception ignored) {
        }
        if (adapter != null) {
            try {
                adapter.cancelDiscovery();
            } catch (SecurityException ignored) {
            }
        }
        closeSocket();
    }
}
