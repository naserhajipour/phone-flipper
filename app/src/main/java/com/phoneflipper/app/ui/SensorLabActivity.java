package com.phoneflipper.app.ui;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.phoneflipper.app.R;
import com.phoneflipper.app.core.BaseActivity;
import com.phoneflipper.app.core.Palette;
import com.phoneflipper.app.core.Prefs;
import com.phoneflipper.app.core.Ui;
import com.phoneflipper.app.view.CompassView;
import com.phoneflipper.app.view.GaugeView;
import com.phoneflipper.app.view.LevelView;
import com.phoneflipper.app.view.MeterView;
import com.phoneflipper.app.view.SparkChart;

import java.util.Locale;

/** Live laboratory for the phone's built-in sensors. */
public class SensorLabActivity extends BaseActivity implements SensorEventListener {

    private SensorManager sm;
    private Sensor accel, gyro, mag, light, proximity, pressure, rotation;

    private boolean running;
    private long lastUi;
    private long lastSpark;
    private float emfSmoothed;
    private float emfPeak;
    private float[] gravity = new float[3];
    private float[] accelValues = new float[3];
    private float[] gyroValues = new float[3];
    private float[] magValues = new float[3];
    private float azimuth;
    private boolean hasHeading;
    private float lux;
    private float proxValue = -1;
    private float hPa;

    private GaugeView emfGauge;
    private SparkChart emfChart;
    private TextView emfPeakView;
    private TextView emfQuality;
    private MeterView magnitudeMeter, accelX, accelY, accelZ;
    private MeterView gyroX, gyroY, gyroZ;
    private LevelView levelView;
    private CompassView compass;
    private TextView headingView;
    private MeterView lightMeter;
    private TextView lightValue, proxValueView, pressureValue;
    private TextView toggle;
    private TextView statusPill;

    @Override
    protected void onCreateScreen(Bundle savedInstanceState) {
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mag = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        light = sm.getDefaultSensor(Sensor.TYPE_LIGHT);
        proximity = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        pressure = sm.getDefaultSensor(Sensor.TYPE_PRESSURE);
        rotation = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        LinearLayout content = installScreen(R.string.title_sensor_lab,
                R.string.subtitle_sensor_lab, true);

        LinearLayout header = Ui.row(this);
        statusPill = Ui.badge(this, getString(R.string.status_stopped), Palette.TEXT_MUTED);
        header.addView(statusPill);
        content.addView(header);

        TextView intro = Ui.body(this, getString(R.string.sensor_intro));
        Ui.add(content, intro, 0, 12, 0, 0);

        buildEmfCard(content);
        buildMotionCard(content);
        buildCompassCard(content);
        buildGyroCard(content);
        buildEnvironmentCard(content);
        buildControls(content);
        content.addView(footer(getString(R.string.sensor_footer)));
    }

    // ------------------------------------------------------------------ cards

    private void buildEmfCard(LinearLayout content) {
        Ui.add(content, Ui.section(this, getString(R.string.section_magnetic)), 0, 24, 0, 0);
        LinearLayout card = Ui.card(this);
        Ui.add(content, card, 0, 10, 0, 0);

        LinearLayout header = Ui.row(this);
        TextView title = Ui.text(this, getString(R.string.sensor_magnetometer), 15, Palette.TEXT, true);
        header.addView(title, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(Ui.badge(this, unitLabel(), Palette.PRIMARY));
        card.addView(header);

        emfGauge = new GaugeView(this);
        emfGauge.setAccent(Palette.PRIMARY);
        emfGauge.setValue(0f, "—", unitLabel(), getString(R.string.emf_background));
        Ui.add(card, emfGauge, 0, 8, 0, 0);
        emfGauge.getLayoutParams().height = Ui.dp(this, 188);

        LinearLayout meta = Ui.row(this);
        emfPeakView = Ui.mono(this, "—", 12, Palette.TEXT_DIM);
        TextView peakLabel = Ui.caption(this, getString(R.string.label_peak) + "  ");
        meta.addView(peakLabel);
        meta.addView(emfPeakView);
        View spacer = new View(this);
        meta.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));
        emfQuality = Ui.badge(this, getString(R.string.emf_background), Palette.SUCCESS);
        meta.addView(emfQuality);
        Ui.add(card, meta, 0, 4, 0, 0);

        emfChart = new SparkChart(this);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 58));
        cp.topMargin = Ui.dp(this, 12);
        card.addView(emfChart, cp);

        if (mag == null) {
            card.addView(unavailableNote());
        }
    }

    private void buildMotionCard(LinearLayout content) {
        Ui.add(content, Ui.section(this, getString(R.string.section_motion)), 0, 26, 0, 0);
        LinearLayout card = Ui.card(this);
        Ui.add(content, card, 0, 10, 0, 0);

        LinearLayout header = Ui.row(this);
        header.addView(Ui.text(this, getString(R.string.sensor_accelerometer), 15, Palette.TEXT, true),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(Ui.badge(this, "m/s²", Palette.TEXT_DIM));
        card.addView(header);

        magnitudeMeter = new MeterView(this);
        Ui.add(card, magnitudeMeter, 0, 14, 0, 0);
        magnitudeMeter.set(getString(R.string.label_magnitude), "—", 0, Palette.PRIMARY);

        accelX = new MeterView(this);
        accelY = new MeterView(this);
        accelZ = new MeterView(this);
        Ui.add(card, accelX, 0, 8, 0, 0);
        Ui.add(card, accelY, 0, 8, 0, 0);
        Ui.add(card, accelZ, 0, 8, 0, 0);
        accelX.set("X", "—", 0, Palette.PRIMARY);
        accelY.set("Y", "—", 0, Palette.ACCENT);
        accelZ.set("Z", "—", 0, Palette.WARNING);

        levelView = new LevelView(this);
        LinearLayout levelRow = Ui.row(this);
        levelRow.setGravity(Gravity.CENTER_VERTICAL);
        levelRow.addView(levelView, new LinearLayout.LayoutParams(Ui.dp(this, 108), Ui.dp(this, 108)));
        TextView levelCaption = Ui.caption(this, getString(R.string.level_hint));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMarginStart(Ui.dp(this, 16));
        levelRow.addView(levelCaption, lp);
        Ui.add(card, levelRow, 0, 14, 0, 0);

        if (accel == null) {
            card.addView(unavailableNote());
        }
    }

    private void buildCompassCard(LinearLayout content) {
        Ui.add(content, Ui.section(this, getString(R.string.section_orientation)), 0, 26, 0, 0);
        LinearLayout card = Ui.card(this);
        Ui.add(content, card, 0, 10, 0, 0);

        LinearLayout header = Ui.row(this);
        header.addView(Ui.text(this, getString(R.string.sensor_compass), 15, Palette.TEXT, true),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        headingView = Ui.mono(this, "—", 12, Palette.TEXT_MUTED);
        header.addView(headingView);
        card.addView(header);

        compass = new CompassView(this);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 196));
        cp.topMargin = Ui.dp(this, 10);
        card.addView(compass, cp);

        TextView hint = Ui.caption(this, getString(R.string.compass_hint));
        Ui.add(card, hint, 0, 6, 0, 0);

        if (mag == null && rotation == null) {
            card.addView(unavailableNote());
        }
    }

    private void buildGyroCard(LinearLayout content) {
        if (gyro == null) {
            return;
        }
        Ui.add(content, Ui.section(this, getString(R.string.section_rotation)), 0, 26, 0, 0);
        LinearLayout card = Ui.card(this);
        Ui.add(content, card, 0, 10, 0, 0);

        LinearLayout header = Ui.row(this);
        header.addView(Ui.text(this, getString(R.string.sensor_gyroscope), 15, Palette.TEXT, true),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(Ui.badge(this, "rad/s", Palette.TEXT_DIM));
        card.addView(header);

        gyroX = new MeterView(this);
        gyroY = new MeterView(this);
        gyroZ = new MeterView(this);
        Ui.add(card, gyroX, 0, 14, 0, 0);
        Ui.add(card, gyroY, 0, 8, 0, 0);
        Ui.add(card, gyroZ, 0, 8, 0, 0);
        gyroX.set("X", "—", 0, Palette.PRIMARY);
        gyroY.set("Y", "—", 0, Palette.ACCENT);
        gyroZ.set("Z", "—", 0, Palette.WARNING);
    }

    private void buildEnvironmentCard(LinearLayout content) {
        Ui.add(content, Ui.section(this, getString(R.string.section_environment)), 0, 26, 0, 0);
        LinearLayout card = Ui.card(this);
        Ui.add(content, card, 0, 10, 0, 0);

        TextView lightTitle = Ui.text(this, getString(R.string.sensor_light), 15, Palette.TEXT, true);
        card.addView(lightTitle);
        lightMeter = new MeterView(this);
        Ui.add(card, lightMeter, 0, 12, 0, 0);
        lightMeter.set(getString(R.string.label_illuminance), "—", 0, Palette.ACCENT);
        lightValue = Ui.mono(this, "—", 12, Palette.TEXT_DIM);
        Ui.add(card, lightValue, 0, 2, 0, 0);

        proxValueView = Ui.mono(this, "—", 12, Palette.TEXT_DIM);
        addEnvironmentRow(card, R.string.sensor_proximity,
                getString(R.string.label_distance), proxValueView);

        pressureValue = Ui.mono(this, "—", 12, Palette.TEXT_DIM);
        addEnvironmentRow(card, R.string.sensor_barometer,
                getString(R.string.label_pressure), pressureValue);

        if (light == null && proximity == null && pressure == null) {
            card.addView(unavailableNote());
        }
    }

    private void addEnvironmentRow(LinearLayout card, int titleRes, String unit, TextView value) {
        LinearLayout row = Ui.row(this);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = Ui.dp(this, 16);
        row.setLayoutParams(p);
        TextView t = Ui.text(this, getString(titleRes), 13, Palette.TEXT_DIM, false);
        row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(value);
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        vp.setMarginEnd(Ui.dp(this, 10));
        value.setLayoutParams(vp);
        row.addView(Ui.badge(this, unit, Palette.TEXT_MUTED));
        card.addView(row);
    }

    private View unavailableNote() {
        TextView t = Ui.caption(this, getString(R.string.sensor_unavailable));
        t.setTextColor(Palette.WARNING);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = Ui.dp(this, 12);
        t.setLayoutParams(p);
        return t;
    }

    private void buildControls(LinearLayout content) {
        toggle = Ui.button(this, getString(R.string.action_start_lab), true);
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                running = !running;
                if (running) {
                    register();
                } else {
                    unregister();
                    resetReadings();
                }
                updateToggle();
            }
        });
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = Ui.dp(this, 26);
        content.addView(toggle, p);

        TextView hint = Ui.caption(this, getString(R.string.sensor_battery_hint));
        hint.setGravity(Gravity.CENTER);
        Ui.add(content, hint, 8, 10, 8, 0);
    }

    // ------------------------------------------------------------------ sensors

    private void register() {
        running = true;
        int delay = SensorManager.SENSOR_DELAY_GAME;
        if (accel != null) {
            sm.registerListener(this, accel, delay);
        }
        if (gyro != null) {
            sm.registerListener(this, gyro, delay);
        }
        if (mag != null) {
            sm.registerListener(this, mag, delay);
        }
        if (light != null) {
            sm.registerListener(this, light, SensorManager.SENSOR_DELAY_UI);
        }
        if (proximity != null) {
            sm.registerListener(this, proximity, SensorManager.SENSOR_DELAY_UI);
        }
        if (pressure != null) {
            sm.registerListener(this, pressure, SensorManager.SENSOR_DELAY_UI);
        }
        if (rotation != null) {
            sm.registerListener(this, rotation, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    private void unregister() {
        sm.unregisterListener(this);
    }

    private void updateToggle() {
        if (toggle == null) {
            return;
        }
        toggle.setText(running ? R.string.action_stop_lab : R.string.action_start_lab);
        statusPill.setText(running ? R.string.status_live : R.string.status_stopped);
        int color = running ? Palette.SUCCESS : Palette.TEXT_MUTED;
        statusPill.setTextColor(color);
        statusPill.setBackground(Ui.rounded(this, Ui.adjustAlpha(color, 30), 7,
                Ui.adjustAlpha(color, 90), 1));
    }

    private void resetReadings() {
        emfSmoothed = 0;
        emfPeak = 0;
        if (emfGauge != null) {
            emfGauge.setValue(0, "—", unitLabel(), getString(R.string.emf_background));
        }
        if (emfChart != null) {
            emfChart.reset();
        }
        if (emfPeakView != null) {
            emfPeakView.setText("—");
        }
        if (compass != null) {
            compass.setAzimuth(0, false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        running = true;
        register();
        updateToggle();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregister();
        running = false;
        updateToggle();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();
        switch (type) {
            case Sensor.TYPE_MAGNETIC_FIELD:
                magValues = event.values.clone();
                updateMag(event.values);
                break;
            case Sensor.TYPE_ACCELEROMETER:
                accelValues = event.values.clone();
                updateAccel(event.values);
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyroValues = event.values.clone();
                updateGyro(event.values);
                break;
            case Sensor.TYPE_LIGHT:
                lux = event.values[0];
                break;
            case Sensor.TYPE_PROXIMITY:
                proxValue = event.values[0];
                break;
            case Sensor.TYPE_PRESSURE:
                hPa = event.values[0];
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                updateHeading(event.values);
                break;
            default:
                break;
        }
        long now = System.currentTimeMillis();
        if (now - lastUi > 80) {
            lastUi = now;
            updateMisc();
        }
    }

    private void updateMag(float[] v) {
        float magnitude = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        emfSmoothed = emfSmoothed == 0 ? magnitude : emfSmoothed * 0.82f + magnitude * 0.18f;
        emfPeak = Math.max(emfPeak, magnitude);
        if (emfGauge == null) {
            return;
        }
        boolean mg = Prefs.UNIT_MG.equals(Prefs.emfUnit(this));
        float shown = mg ? emfSmoothed * 10f : emfSmoothed;
        float peakShown = mg ? emfPeak * 10f : emfPeak;
        float max = mg ? 2000f : 200f;
        emfGauge.setValue(shown / max, String.format(Locale.US, "%.1f", shown),
                mg ? "mG" : "μT", qualityLabel(emfSmoothed));
        if (emfPeakView != null) {
            emfPeakView.setText(String.format(Locale.US, "%.1f %s", peakShown, mg ? "mG" : "μT"));
        }
        if (emfQuality != null) {
            int color = emfSmoothed > 120 ? Palette.DANGER
                    : (emfSmoothed > 45 ? Palette.WARNING : Palette.SUCCESS);
            emfQuality.setText(qualityLabel(emfSmoothed));
            emfQuality.setTextColor(color);
            emfQuality.setBackground(Ui.rounded(this, Ui.adjustAlpha(color, 30), 7,
                    Ui.adjustAlpha(color, 90), 1));
        }
        long now = System.currentTimeMillis();
        if (now - lastSpark > 120 && emfChart != null) {
            lastSpark = now;
            emfChart.push(shown);
        }
    }

    private String qualityLabel(float microTesla) {
        if (microTesla > 120) {
            return getString(R.string.emf_strong);
        }
        if (microTesla > 45) {
            return getString(R.string.emf_elevated);
        }
        return getString(R.string.emf_background);
    }

    private void updateAccel(float[] v) {
        float magnitude = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        for (int i = 0; i < 3; i++) {
            gravity[i] = gravity[i] * 0.85f + v[i] * 0.15f;
        }
        if (magnitudeMeter == null) {
            return;
        }
        magnitudeMeter.set(getString(R.string.label_magnitude),
                String.format(Locale.US, "%.2f m/s²", magnitude),
                Math.min(1f, magnitude / 25f), Palette.PRIMARY);
        accelX.set("X", String.format(Locale.US, "%+.2f", v[0]),
                Math.min(1f, Math.abs(v[0]) / 20f), Palette.PRIMARY);
        accelY.set("Y", String.format(Locale.US, "%+.2f", v[1]),
                Math.min(1f, Math.abs(v[1]) / 20f), Palette.ACCENT);
        accelZ.set("Z", String.format(Locale.US, "%+.2f", v[2]),
                Math.min(1f, Math.abs(v[2]) / 20f), Palette.WARNING);
        if (levelView != null) {
            levelView.setTilt(-gravity[0] / 9.81f, gravity[1] / 9.81f);
        }
        if (rotation == null) {
            updateHeadingFromAccelAndMag();
        }
    }

    private void updateGyro(float[] v) {
        if (gyroX == null) {
            return;
        }
        gyroX.set("X", String.format(Locale.US, "%+.2f", v[0]),
                Math.min(1f, Math.abs(v[0]) / 8f), Palette.PRIMARY);
        gyroY.set("Y", String.format(Locale.US, "%+.2f", v[1]),
                Math.min(1f, Math.abs(v[1]) / 8f), Palette.ACCENT);
        gyroZ.set("Z", String.format(Locale.US, "%+.2f", v[2]),
                Math.min(1f, Math.abs(v[2]) / 8f), Palette.WARNING);
    }

    private void updateHeadingFromAccelAndMag() {
        float[] r = new float[9];
        float[] orientation = new float[3];
        try {
            if (SensorManager.getRotationMatrix(r, null, accelValues, magValues)) {
                SensorManager.getOrientation(r, orientation);
                azimuth = (float) Math.toDegrees(orientation[0]);
                if (azimuth < 0) {
                    azimuth += 360;
                }
                hasHeading = true;
            }
        } catch (Exception ignored) {
        }
    }

    private void updateHeading(float[] values) {
        float[] rotationMatrix = new float[9];
        float[] orientation = new float[3];
        try {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, values);
            SensorManager.getOrientation(rotationMatrix, orientation);
            azimuth = (float) Math.toDegrees(orientation[0]);
            if (azimuth < 0) {
                azimuth += 360;
            }
            hasHeading = true;
        } catch (Exception ignored) {
        }
    }

    private void updateMisc() {
        if (compass != null) {
            compass.setAzimuth(azimuth, hasHeading);
        }
        if (headingView != null) {
            headingView.setText(hasHeading
                    ? String.format(Locale.US, "%03.0f° %s", azimuth, cardinal(azimuth))
                    : "—");
        }
        if (lightMeter != null) {
            float norm = (float) (Math.log10(lux + 1) / 4f);
            lightMeter.set(getString(R.string.label_illuminance),
                    String.format(Locale.US, "%.0f lx", lux), Math.min(1f, norm), Palette.ACCENT);
            lightValue.setText(lightText(lux));
        }
        if (proxValueView != null) {
            proxValueView.setText(proxValue < 0 ? "—"
                    : String.format(Locale.US, "%.0f cm", proxValue));
        }
        if (pressureValue != null) {
            pressureValue.setText(hPa <= 0 ? "—"
                    : String.format(Locale.US, "%.1f hPa", hPa));
        }
    }

    private String lightText(float value) {
        if (value < 10) {
            return getString(R.string.light_dark);
        }
        if (value < 200) {
            return getString(R.string.light_indoor);
        }
        if (value < 10000) {
            return getString(R.string.light_daylight);
        }
        return getString(R.string.light_direct);
    }

    private String cardinal(float degrees) {
        String[] dirs = getResources().getStringArray(R.array.compass_cardinals);
        int index = (int) ((degrees + 22.5f) / 45f) % 8;
        return dirs[index];
    }

    private String unitLabel() {
        return Prefs.UNIT_MG.equals(Prefs.emfUnit(this)) ? "mG" : "μT";
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
