package com.phoneflipper.app.core;

import android.content.Context;
import android.os.Build;

import com.phoneflipper.app.BuildConfig;
import com.phoneflipper.app.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Builds the human-readable and machine-readable hardware reports. */
public final class ReportBuilder {

    private ReportBuilder() {
    }

    private static String timestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
    }

    public static String toText(Context c, List<Capabilities.Item> items) {
        StringBuilder sb = new StringBuilder();
        sb.append(c.getString(R.string.app_name)).append(" — ")
                .append(c.getString(R.string.report_title)).append('\n');
        sb.append("========================================\n");
        sb.append(c.getString(R.string.device_model)).append(": ")
                .append(Capabilities.deviceName()).append('\n');
        sb.append(c.getString(R.string.device_android)).append(": ")
                .append(Capabilities.androidVersion()).append('\n');
        sb.append(c.getString(R.string.report_app_version)).append(": ")
                .append(BuildConfig.VERSION_NAME).append(" (").append(BuildConfig.VERSION_CODE)
                .append(")\n");
        sb.append(c.getString(R.string.report_generated)).append(": ")
                .append(timestamp()).append('\n');
        sb.append(c.getString(R.string.report_score)).append(": ")
                .append(Capabilities.score(items)).append("%\n\n");

        String lastCategory = null;
        for (Capabilities.Item item : items) {
            if (!item.category.equals(lastCategory)) {
                lastCategory = item.category;
                sb.append("[").append(categoryLabel(c, item.category)).append("]\n");
            }
            sb.append(" • ").append(c.getString(item.titleRes)).append(" — ")
                    .append(Capabilities.statusLabel(c, item.status)).append('\n');
            sb.append("   ").append(item.detail).append('\n');
        }
        sb.append('\n').append(c.getString(R.string.report_footer)).append('\n');
        return sb.toString();
    }

    public static String toJson(Context c, List<Capabilities.Item> items) {
        JSONObject root = new JSONObject();
        try {
            root.put("app", c.getString(R.string.app_name));
            root.put("version", BuildConfig.VERSION_NAME);
            root.put("versionCode", BuildConfig.VERSION_CODE);
            root.put("generated", timestamp());
            JSONObject device = new JSONObject();
            device.put("manufacturer", Build.MANUFACTURER);
            device.put("model", Build.MODEL);
            device.put("android", Build.VERSION.RELEASE);
            device.put("api", Build.VERSION.SDK_INT);
            device.put("securityPatch", Build.VERSION.SECURITY_PATCH);
            device.put("abi", Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "");
            root.put("device", device);
            root.put("score", Capabilities.score(items));

            JSONArray caps = new JSONArray();
            for (Capabilities.Item item : items) {
                JSONObject o = new JSONObject();
                o.put("id", item.id);
                o.put("name", c.getString(item.titleRes));
                o.put("category", item.category);
                o.put("status", Capabilities.statusLabel(c, item.status));
                o.put("detail", item.detail);
                caps.put(o);
            }
            root.put("capabilities", caps);
        } catch (JSONException ignored) {
        }
        try {
            return root.toString(2);
        } catch (JSONException e) {
            return root.toString();
        }
    }

    public static String categoryLabel(Context c, String category) {
        if (Capabilities.CAT_RADIO.equals(category)) {
            return c.getString(R.string.category_radio);
        }
        if (Capabilities.CAT_SENSOR.equals(category)) {
            return c.getString(R.string.category_sensors);
        }
        if (Capabilities.CAT_OPTIC.equals(category)) {
            return c.getString(R.string.category_optics);
        }
        return c.getString(R.string.category_io);
    }

    /** Writes a report into cache/reports and returns the file. */
    public static File write(Context c, String name, String content) {
        File dir = new File(c.getCacheDir(), "reports");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File f = new File(dir, name);
        try {
            FileOutputStream out = new FileOutputStream(f);
            out.write(content.getBytes("UTF-8"));
            out.close();
        } catch (Exception ignored) {
        }
        return f;
    }
}
