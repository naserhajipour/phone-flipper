package com.phoneflipper.app.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.ConsumerIrManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.usb.UsbManager;
import android.nfc.NfcAdapter;
import android.os.Build;

import com.phoneflipper.app.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads the real hardware feature set from the platform. No guessing, no
 * network lookups: every row maps to a PackageManager/SensorManager query.
 */
public final class Capabilities {

    public static final int ABSENT = 0;
    public static final int BRIDGE = 1;
    public static final int NATIVE = 2;

    public static final String CAT_RADIO = "radio";
    public static final String CAT_SENSOR = "sensor";
    public static final String CAT_IO = "io";
    public static final String CAT_OPTIC = "optic";

    public static class Item {
        public final String id;
        public final int titleRes;
        public final int descRes;
        public final int icon;
        public final String category;
        public final int status;
        public final String detail;

        public Item(String id, int titleRes, int descRes, int icon, String category,
                    int status, String detail) {
            this.id = id;
            this.titleRes = titleRes;
            this.descRes = descRes;
            this.icon = icon;
            this.category = category;
            this.status = status;
            this.detail = detail;
        }
    }

    private Capabilities() {
    }

    public static List<Item> scan(Context c) {
        List<Item> out = new ArrayList<>();
        PackageManager pm = c.getPackageManager();

        // ---- NFC ------------------------------------------------------------
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(c);
        boolean hasNfc = pm.hasSystemFeature(PackageManager.FEATURE_NFC) && nfc != null;
        boolean hasHce = pm.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION);
        out.add(new Item("nfc", R.string.cap_nfc, R.string.cap_nfc_desc, IconDrawable.NFC,
                CAT_RADIO, hasNfc ? NATIVE : ABSENT,
                hasNfc
                        ? (hasHce ? c.getString(R.string.detail_nfc_hce) : c.getString(R.string.detail_nfc))
                        : c.getString(R.string.detail_absent)));

        // ---- Infrared -------------------------------------------------------
        ConsumerIrManager ir = (ConsumerIrManager) c.getSystemService(Context.CONSUMER_IR_SERVICE);
        boolean hasIr = ir != null && ir.hasIrEmitter();
        String irDetail = c.getString(R.string.detail_absent);
        if (hasIr) {
            StringBuilder sb = new StringBuilder(c.getString(R.string.detail_ir));
            try {
                ConsumerIrManager.CarrierFrequencyRange[] ranges = ir.getCarrierFrequencies();
                if (ranges != null && ranges.length > 0) {
                    sb.append(" · ");
                    for (int i = 0; i < Math.min(3, ranges.length); i++) {
                        if (i > 0) {
                            sb.append(", ");
                        }
                        sb.append(String.format(Locale.US, "%d–%d Hz",
                                ranges[i].getMinFrequency(), ranges[i].getMaxFrequency()));
                    }
                }
            } catch (Exception ignored) {
            }
            irDetail = sb.toString();
        }
        out.add(new Item("ir", R.string.cap_ir, R.string.cap_ir_desc, IconDrawable.IR,
                CAT_RADIO, hasIr ? NATIVE : BRIDGE, irDetail));

        // ---- Bluetooth ------------------------------------------------------
        BluetoothManager bm = (BluetoothManager) c.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = bm == null ? null : bm.getAdapter();
        boolean hasBt = adapter != null;
        String btDetail = hasBt
                ? (pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
                ? c.getString(R.string.detail_bt_le) : c.getString(R.string.detail_bt))
                : c.getString(R.string.detail_absent);
        out.add(new Item("bt", R.string.cap_bt, R.string.cap_bt_desc, IconDrawable.BLUETOOTH,
                CAT_RADIO, hasBt ? NATIVE : ABSENT, btDetail));

        // ---- Sub-GHz radio --------------------------------------------------
        out.add(new Item("subghz", R.string.cap_subghz, R.string.cap_subghz_desc, IconDrawable.ANTENNA,
                CAT_RADIO, BRIDGE, c.getString(R.string.detail_subghz)));

        // ---- USB host -------------------------------------------------------
        boolean usb = pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
                && c.getSystemService(Context.USB_SERVICE) instanceof UsbManager;
        out.add(new Item("usb", R.string.cap_usb, R.string.cap_usb_desc, IconDrawable.CHIP,
                CAT_IO, usb ? NATIVE : ABSENT,
                usb ? c.getString(R.string.detail_usb) : c.getString(R.string.detail_absent)));

        // ---- Camera ---------------------------------------------------------
        boolean camera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
        boolean flash = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        int cameras = 0;
        try {
            CameraManager cm = (CameraManager) c.getSystemService(Context.CAMERA_SERVICE);
            if (cm != null) {
                cameras = cm.getCameraIdList().length;
            }
        } catch (Exception ignored) {
        }
        String camDetail = camera
                ? String.format(Locale.US, "%s · %d %s%s", c.getString(R.string.detail_camera),
                cameras, c.getString(R.string.detail_camera_units),
                flash ? " · " + c.getString(R.string.detail_flash) : "")
                : c.getString(R.string.detail_absent);
        out.add(new Item("camera", R.string.cap_camera, R.string.cap_camera_desc, IconDrawable.CAMERA,
                CAT_OPTIC, camera ? NATIVE : ABSENT, camDetail));

        // ---- Microphone -----------------------------------------------------
        boolean mic = pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE);
        out.add(new Item("mic", R.string.cap_mic, R.string.cap_mic_desc, IconDrawable.AUDIO,
                CAT_OPTIC, mic ? NATIVE : ABSENT,
                mic ? c.getString(R.string.detail_mic) : c.getString(R.string.detail_absent)));

        // ---- Sensors --------------------------------------------------------
        SensorManager sm = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);
        out.add(sensor(c, sm, "accel", Sensor.TYPE_ACCELEROMETER, R.string.cap_accel,
                R.string.cap_accel_desc, IconDrawable.PULSE));
        out.add(sensor(c, sm, "gyro", Sensor.TYPE_GYROSCOPE, R.string.cap_gyro,
                R.string.cap_gyro_desc, IconDrawable.SENSOR));
        out.add(sensor(c, sm, "mag", Sensor.TYPE_MAGNETIC_FIELD, R.string.cap_mag,
                R.string.cap_mag_desc, IconDrawable.MAGNET));
        out.add(sensor(c, sm, "light", Sensor.TYPE_LIGHT, R.string.cap_light,
                R.string.cap_light_desc, IconDrawable.LIGHT));
        out.add(sensor(c, sm, "prox", Sensor.TYPE_PROXIMITY, R.string.cap_prox,
                R.string.cap_prox_desc, IconDrawable.SENSOR));
        out.add(sensor(c, sm, "baro", Sensor.TYPE_PRESSURE, R.string.cap_baro,
                R.string.cap_baro_desc, IconDrawable.GLOBE));

        // ---- Operating system ----------------------------------------------
        out.add(new Item("offline", R.string.cap_offline, R.string.cap_offline_desc, IconDrawable.SHIELD,
                CAT_IO, NATIVE, c.getString(R.string.detail_offline)));
        return out;
    }

    private static Item sensor(Context c, SensorManager sm, String id, int type,
                               int title, int desc, int icon) {
        Sensor s = sm == null ? null : sm.getDefaultSensor(type);
        String detail;
        if (s == null) {
            detail = c.getString(R.string.detail_absent);
        } else {
            detail = String.format(Locale.US, "%s · %.2f mA · %s",
                    s.getName(), s.getPower(), s.getVendor());
        }
        return new Item(id, title, desc, icon, CAT_SENSOR, s != null ? NATIVE : ABSENT, detail);
    }

    public static int count(List<Item> items, int status) {
        int n = 0;
        for (Item i : items) {
            if (i.status == status) {
                n++;
            }
        }
        return n;
    }

    public static int score(List<Item> items) {
        if (items.isEmpty()) {
            return 0;
        }
        return Math.round(100f * count(items, NATIVE) / items.size());
    }

    public static String statusLabel(Context c, int status) {
        switch (status) {
            case NATIVE:
                return c.getString(R.string.status_native);
            case BRIDGE:
                return c.getString(R.string.status_bridge);
            default:
                return c.getString(R.string.status_absent);
        }
    }

    public static int statusColor(int status) {
        switch (status) {
            case NATIVE:
                return Palette.SUCCESS;
            case BRIDGE:
                return Palette.WARNING;
            default:
                return Palette.TEXT_MUTED;
        }
    }

    public static String deviceName() {
        String manufacturer = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER;
        String model = Build.MODEL == null ? "" : Build.MODEL;
        if (model.toLowerCase(Locale.US).startsWith(manufacturer.toLowerCase(Locale.US))) {
            return model;
        }
        return (manufacturer + " " + model).trim();
    }

    public static String androidVersion() {
        return "Android " + Build.VERSION.RELEASE + " · API " + Build.VERSION.SDK_INT;
    }
}
