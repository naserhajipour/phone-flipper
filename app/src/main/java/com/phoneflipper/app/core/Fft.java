package com.phoneflipper.app.core;

/** Iterative radix-2 FFT plus a Hann window helper. */
public final class Fft {

    private Fft() {
    }

    public static void hann(double[] window) {
        int n = window.length;
        for (int i = 0; i < n; i++) {
            window[i] = 0.5 * (1 - Math.cos(2 * Math.PI * i / (n - 1)));
        }
    }

    public static void transform(double[] re, double[] im) {
        int n = re.length;
        int j = 0;
        for (int i = 1; i < n; i++) {
            int bit = n >> 1;
            while ((j & bit) != 0) {
                j ^= bit;
                bit >>= 1;
            }
            j ^= bit;
            if (i < j) {
                double tr = re[i];
                double ti = im[i];
                re[i] = re[j];
                im[i] = im[j];
                re[j] = tr;
                im[j] = ti;
            }
        }
        for (int len = 2; len <= n; len <<= 1) {
            double angle = -2 * Math.PI / len;
            double wRe = Math.cos(angle);
            double wIm = Math.sin(angle);
            for (int i = 0; i < n; i += len) {
                double curRe = 1;
                double curIm = 0;
                for (int k = 0; k < len / 2; k++) {
                    int a = i + k;
                    int b = a + len / 2;
                    double vRe = re[b] * curRe - im[b] * curIm;
                    double vIm = re[b] * curIm + im[b] * curRe;
                    re[b] = re[a] - vRe;
                    im[b] = im[a] - vIm;
                    re[a] += vRe;
                    im[a] += vIm;
                    double nextRe = curRe * wRe - curIm * wIm;
                    curIm = curRe * wIm + curIm * wRe;
                    curRe = nextRe;
                }
            }
        }
    }
}
