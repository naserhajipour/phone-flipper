package com.phoneflipper.app.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Size;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.phoneflipper.app.R;
import com.phoneflipper.app.core.BaseActivity;
import com.phoneflipper.app.core.Palette;
import com.phoneflipper.app.core.Ui;
import com.phoneflipper.app.view.ScanOverlay;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Lens finder: live camera feed, strobe and local reflection analysis.
 * Everything is processed in memory; no frame is ever stored or uploaded.
 */
public class LensFinderActivity extends BaseActivity {

    private static final int REQUEST_CAMERA = 4201;

    private TextureView textureView;
    private ScanOverlay overlay;
    private TextView countView;
    private TextView torchBtn;
    private TextView strobeBtn;
    private TextView hintView;

    private CameraManager manager;
    private String cameraId;
    private CameraCharacteristics characteristics;
    private CameraDevice camera;
    private CameraCaptureSession session;
    private ImageReader reader;
    private HandlerThread background;
    private Handler handler;
    private final Handler main = new Handler(Looper.getMainLooper());

    private Size previewSize;
    private Size analysisSize = new Size(640, 480);
    private boolean torch;
    private boolean strobe;
    private float sensitivity = 0.55f;
    private long lastAnalysis;
    private final float[] previewMatrix = new float[9];
    private boolean matrixReady;

    private final Runnable strobeRunnable = new Runnable() {
        @Override
        public void run() {
            if (!strobe) {
                return;
            }
            setTorch(!torch);
            main.postDelayed(this, 480);
        }
    };

    @Override
    protected void onCreateScreen(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        String found = null;
        try {
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics ch = manager.getCameraCharacteristics(id);
                Integer facing = ch.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    found = id;
                    characteristics = ch;
                    break;
                }
            }
        } catch (Exception ignored) {
        }
        cameraId = found;

        FrameLayout root = new FrameLayout(this);
        textureView = new TextureView(this);
        root.addView(textureView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        overlay = new ScanOverlay(this);
        root.addView(overlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (cameraId == null) {
            super.installCustom(R.string.title_lens, R.string.subtitle_lens, true, messageRoot());
            return;
        }

        root.addView(buildHud(), hudParams());
        super.installCustom(R.string.title_lens, R.string.subtitle_lens, true, root);

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                if (hasPermission()) {
                    openCamera(width, height);
                } else {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                configureTransform(width, height);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                closeCamera();
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
    }

    private View messageRoot() {
        LinearLayout content = Ui.column(this);
        Ui.padding(content, 22, 22, 22, 22);
        LinearLayout card = Ui.card(this);
        card.addView(Ui.text(this, getString(R.string.lens_unavailable_title), 17, Palette.TEXT, true));
        TextView body = Ui.body(this, getString(R.string.lens_unavailable_body));
        Ui.add(card, body, 0, 10, 0, 0);
        content.addView(card);
        return content;
    }

    private FrameLayout.LayoutParams hudParams() {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.gravity = Gravity.BOTTOM;
        p.setMargins(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 18));
        return p;
    }

    private View buildHud() {
        LinearLayout hud = Ui.column(this);
        hud.setBackground(Ui.rounded(this, Ui.adjustAlpha(Palette.BG, 235), 22, Palette.STROKE, 1));
        Ui.padding(hud, 18, 16, 18, 16);

        LinearLayout header = Ui.row(this);
        LinearLayout countBlock = Ui.column(this);
        header.addView(countBlock, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        countBlock.addView(Ui.section(this, getString(R.string.lens_reflections)));
        countView = Ui.mono(this, "0", 26, Palette.ACCENT);
        countBlock.addView(countView);

        LinearLayout buttons = Ui.row(this);
        torchBtn = Ui.button(this, getString(R.string.lens_torch), false);
        torchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTorch(!torch);
            }
        });
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.setMarginEnd(Ui.dp(this, 8));
        buttons.addView(torchBtn, bp);
        strobeBtn = Ui.button(this, getString(R.string.lens_strobe), false);
        strobeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleStrobe();
            }
        });
        buttons.addView(strobeBtn);
        header.addView(buttons);
        hud.addView(header);

        hintView = Ui.caption(this, getString(R.string.lens_hint));
        Ui.add(hud, hintView, 0, 10, 0, 0);

        LinearLayout sensitivityRow = Ui.row(this);
        Ui.add(hud, sensitivityRow, 0, 12, 0, 0);
        TextView label = Ui.caption(this, getString(R.string.lens_sensitivity));
        sensitivityRow.addView(label);
        SeekBar bar = new SeekBar(this);
        bar.setMax(100);
        bar.setProgress((int) (sensitivity * 100));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sensitivity = progress / 100f;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        sp.setMarginStart(Ui.dp(this, 10));
        sensitivityRow.addView(bar, sp);
        return hud;
    }

    // ------------------------------------------------------------------ camera

    private boolean hasPermission() {
        return checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (textureView.isAvailable()) {
                    openCamera(textureView.getWidth(), textureView.getHeight());
                }
            } else {
                hintView.setText(R.string.lens_permission_denied);
                hintView.setTextColor(Palette.WARNING);
            }
        }
    }

    private void pickSizes(int viewWidth, int viewHeight) {
        try {
            StreamConfigurationMap map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) {
                return;
            }
            Size[] sizes = map.getOutputSizes(ImageFormat.YUV_420_888);
            if (sizes == null) {
                return;
            }
            previewSize = sizes[0];
            for (Size s : sizes) {
                if (s.getWidth() <= 1280 && s.getHeight() <= 720
                        && s.getWidth() * 9 == s.getHeight() * 16) {
                    if (previewSize == null || s.getWidth() > previewSize.getWidth()) {
                        previewSize = s;
                    }
                }
            }
            analysisSize = sizes[0];
            for (Size s : sizes) {
                if (s.getWidth() == 640 && s.getHeight() == 480) {
                    analysisSize = s;
                    break;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void openCamera(int width, int height) {
        if (camera != null || cameraId == null) {
            return;
        }
        if (background == null) {
            background = new HandlerThread("lens-finder");
            background.start();
            handler = new Handler(background.getLooper());
        }
        pickSizes(width, height);
        if (previewSize == null) {
            previewSize = new Size(1280, 720);
        }
        try {
            reader = ImageReader.newInstance(analysisSize.getWidth(), analysisSize.getHeight(),
                    ImageFormat.YUV_420_888, 2);
            reader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader r) {
                    Image image = null;
                    try {
                        image = r.acquireLatestImage();
                        long now = System.currentTimeMillis();
                        if (now - lastAnalysis > 220) {
                            lastAnalysis = now;
                            analyze(image);
                        }
                    } catch (Exception ignored) {
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
            }, handler);

            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice cam) {
                    camera = cam;
                    startPreview();
                }

                @Override
                public void onDisconnected(CameraDevice cam) {
                    cam.close();
                    camera = null;
                }

                @Override
                public void onError(CameraDevice cam, int error) {
                    cam.close();
                    camera = null;
                }
            }, handler);
        } catch (SecurityException | CameraAccessException e) {
            main.post(new Runnable() {
                @Override
                public void run() {
                    hintView.setText(R.string.lens_error);
                }
            });
        }
    }

    private void startPreview() {
        if (camera == null || !textureView.isAvailable() || reader == null) {
            return;
        }
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            if (texture == null) {
                return;
            }
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface surface = new Surface(texture);
            final CaptureRequest.Builder builder =
                    camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(surface);
            builder.addTarget(reader.getSurface());
            builder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            camera.createCaptureSession(Arrays.asList(surface, reader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession s) {
                            if (camera == null) {
                                return;
                            }
                            session = s;
                            try {
                                s.setRepeatingRequest(builder.build(), null, handler);
                                main.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        configureTransform(textureView.getWidth(),
                                                textureView.getHeight());
                                    }
                                });
                            } catch (Exception ignored) {
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession s) {
                            main.post(new Runnable() {
                                @Override
                                public void run() {
                                    hintView.setText(R.string.lens_error);
                                }
                            });
                        }
                    }, handler);
        } catch (Exception ignored) {
        }
    }

    /** Center-crop the landscape sensor buffer into the portrait view. */
    private void configureTransform(int viewWidth, int viewHeight) {
        if (textureView == null || previewSize == null || viewWidth == 0 || viewHeight == 0) {
            return;
        }
        int rotation = 90;
        try {
            Integer sensor = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            if (sensor != null) {
                rotation = sensor;
            }
        } catch (Exception ignored) {
        }
        if (rotation != 90 && rotation != 270) {
            rotation = 90;
        }

        float bufferW = previewSize.getWidth();
        float bufferH = previewSize.getHeight();
        // After rotation the buffer occupies (bufferH x bufferW)
        float rotatedW = bufferH;
        float rotatedH = bufferW;
        float scale = Math.max(viewWidth / rotatedW, viewHeight / rotatedH);
        float targetW = rotatedW * scale;
        float targetH = rotatedH * scale;
        float left = (viewWidth - targetW) / 2f;
        float top = (viewHeight - targetH) / 2f;

        float[] src = new float[]{0, 0, bufferW, 0, bufferW, bufferH, 0, bufferH};
        float[] dst = new float[8];
        if (rotation == 90) {
            dst[0] = left + targetW; dst[1] = top;
            dst[2] = left + targetW; dst[3] = top + targetH;
            dst[4] = left;           dst[5] = top + targetH;
            dst[6] = left;           dst[7] = top;
        } else {
            dst[0] = left;           dst[1] = top + targetH;
            dst[2] = left;           dst[3] = top;
            dst[4] = left + targetW; dst[5] = top;
            dst[6] = left + targetW; dst[7] = top + targetH;
        }
        Matrix matrix = new Matrix();
        matrix.setPolyToPoly(src, 0, dst, 0, 4);
        textureView.setTransform(matrix);
        matrix.getValues(previewMatrix);
        matrixReady = true;
    }

    // ------------------------------------------------------------------ analysis

    private void analyze(Image image) {
        try {
            Image.Plane plane = image.getPlanes()[0];
            ByteBuffer buffer = plane.getBuffer();
            int width = image.getWidth();
            int height = image.getHeight();
            int rowStride = plane.getRowStride();

            int cols = 64;
            int rows = 48;
            int[][] cells = new int[rows][cols];
            double sum = 0;
            int samples = 0;
            for (int gy = 0; gy < rows; gy++) {
                int py = gy * height / rows;
                for (int gx = 0; gx < cols; gx++) {
                    int px = gx * width / cols;
                    int index = py * rowStride + px;
                    if (index < 0 || index >= buffer.limit()) {
                        continue;
                    }
                    int luma = buffer.get(index) & 0xFF;
                    cells[gy][gx] = luma;
                    sum += luma;
                    samples++;
                }
            }
            if (samples == 0) {
                return;
            }
            double mean = sum / samples;
            double variance = 0;
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    double d = cells[y][x] - mean;
                    variance += d * d;
                }
            }
            double std = Math.sqrt(variance / samples);
            double k = 1.7 + (1 - sensitivity) * 2.2;
            double threshold = Math.max(mean + k * std, 205 - sensitivity * 45);

            List<float[]> raw = new ArrayList<>();
            for (int y = 1; y < rows - 1; y++) {
                for (int x = 1; x < cols - 1; x++) {
                    int v = cells[y][x];
                    if (v < threshold) {
                        continue;
                    }
                    boolean localMax = v >= cells[y - 1][x] && v >= cells[y + 1][x]
                            && v >= cells[y][x - 1] && v >= cells[y][x + 1];
                    if (!localMax) {
                        continue;
                    }
                    float bx = (x + 0.5f) * width / cols;
                    float by = (y + 0.5f) * height / rows;
                    raw.add(new float[]{bx, by, v});
                }
            }
            // Keep the strongest spots only.
            java.util.Collections.sort(raw, new java.util.Comparator<float[]>() {
                @Override
                public int compare(float[] a, float[] b) {
                    return Float.compare(b[2], a[2]);
                }
            });
            final List<float[]> points = new ArrayList<>();
            float density = getResources().getDisplayMetrics().density;
            float radius = Math.max(10 * density, 14f * density);
            float scaleX = previewSize == null ? 1f
                    : previewSize.getWidth() / (float) analysisSize.getWidth();
            float scaleY = previewSize == null ? 1f
                    : previewSize.getHeight() / (float) analysisSize.getHeight();
            for (int i = 0; i < Math.min(12, raw.size()); i++) {
                float[] spot = raw.get(i);
                float[] mapped = new float[]{spot[0] * scaleX, spot[1] * scaleY};
                if (matrixReady) {
                    Matrix m = new Matrix();
                    m.setValues(previewMatrix);
                    m.mapPoints(mapped);
                }
                points.add(new float[]{mapped[0], mapped[1], radius});
            }
            final int count = points.size();
            final float brightness = (float) mean;
            main.post(new Runnable() {
                @Override
                public void run() {
                    overlay.setSpots(points);
                    countView.setText(String.format(Locale.US, "%d", count));
                    if (brightness < 35) {
                        hintView.setText(R.string.lens_too_dark);
                    } else if (brightness > 210) {
                        hintView.setText(R.string.lens_too_bright);
                    } else {
                        hintView.setText(count > 0
                                ? getString(R.string.lens_found_hint)
                                : getString(R.string.lens_hint));
                    }
                }
            });
        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------------------------ torch

    private void setTorch(boolean on) {
        torch = on;
        try {
            manager.setTorchMode(cameraId, on);
        } catch (Exception e) {
            Toast.makeText(this, R.string.lens_torch_unavailable, Toast.LENGTH_SHORT).show();
        }
        if (torchBtn != null) {
            torchBtn.setText(on ? R.string.lens_torch_on : R.string.lens_torch);
            torchBtn.setBackground(Ui.rounded(this,
                    on ? Palette.PRIMARY : Palette.SURFACE_ALT, 14,
                    on ? Color.TRANSPARENT : Palette.STROKE, 1));
            torchBtn.setTextColor(on ? Palette.ON_PRIMARY : Palette.TEXT);
        }
    }

    private void toggleStrobe() {
        strobe = !strobe;
        if (strobe) {
            main.post(strobeRunnable);
        } else {
            main.removeCallbacks(strobeRunnable);
            setTorch(false);
        }
        if (strobeBtn != null) {
            strobeBtn.setText(strobe ? R.string.lens_strobe_on : R.string.lens_strobe);
            strobeBtn.setTextColor(strobe ? Palette.ACCENT : Palette.TEXT);
        }
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    protected void onPause() {
        super.onPause();
        strobe = false;
        main.removeCallbacks(strobeRunnable);
        setTorch(false);
        closeCamera();
    }

    private void closeCamera() {
        if (session != null) {
            try {
                session.close();
            } catch (Exception ignored) {
            }
            session = null;
        }
        if (camera != null) {
            camera.close();
            camera = null;
        }
        if (reader != null) {
            reader.close();
            reader = null;
        }
        if (overlay != null) {
            overlay.clearSpots();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeCamera();
        if (background != null) {
            background.quitSafely();
            background = null;
            handler = null;
        }
    }
}
