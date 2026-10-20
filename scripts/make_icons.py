#!/usr/bin/env python3
"""Generates the Flipper One launcher icons (adaptive + legacy) with Pillow.

The mark matches the in-app LogoView: a cyan monolith with an energy cut and
an orbit arc. Run:  python3 scripts/make_icons.py
"""
import math
import os

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")

TOP = (0x38, 0xD6, 0xFF)
BOTTOM = (0x1B, 0x8F, 0xB4)
BG = (0x07, 0x0D, 0x18)

DENSITIES = {
    "mdpi": 1.0,
    "hdpi": 1.5,
    "xhdpi": 2.0,
    "xxhdpi": 3.0,
    "xxxhdpi": 4.0,
}


def gradient_square(size, radius):
    ss = 4
    big = Image.new("RGBA", (size * ss, size * ss), (0, 0, 0, 0))
    draw = ImageDraw.Draw(big)
    for y in range(size * ss):
        t = y / max(1, size * ss - 1)
        color = tuple(int(TOP[i] + (BOTTOM[i] - TOP[i]) * t) for i in range(3)) + (255,)
        draw.line([(0, y), (size * ss, y)], fill=color)
    mask = Image.new("L", (size * ss, size * ss), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        [0, 0, size * ss - 1, size * ss - 1], radius=radius * ss, fill=255)
    big.putalpha(mask)
    return big.resize((size, size), Image.LANCZOS)


def gradient_circle(size):
    ss = 4
    big = Image.new("RGBA", (size * ss, size * ss), (0, 0, 0, 0))
    draw = ImageDraw.Draw(big)
    for y in range(size * ss):
        t = y / max(1, size * ss - 1)
        color = tuple(int(TOP[i] + (BOTTOM[i] - TOP[i]) * t) for i in range(3)) + (255,)
        draw.line([(0, y), (size * ss, y)], fill=color)
    mask = Image.new("L", (size * ss, size * ss), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, size * ss - 1, size * ss - 1], fill=255)
    big.putalpha(mask)
    return big.resize((size, size), Image.LANCZOS)


def draw_mark(img, scale=1.0, color=BG):
    """Draws the bolt onto an RGBA image, centred, at `scale` of the artboard."""
    ss = 4
    size = img.width
    layer = Image.new("RGBA", (size * ss, size * ss), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    unit = size * ss / 48.0 * scale
    cx, cy = size * ss / 2, size * ss / 2

    def p(x, y):
        return (cx + x * unit, cy + y * unit)

    bolt = [p(2, -15), p(-8, 2), p(-1, 2), p(-3, 15), p(8, -2), p(1, -2)]
    d.polygon(bolt, fill=color + (255,))

    # orbit ring
    r = size * ss * 0.40 * scale
    box = [cx - r, cy - r, cx + r, cy + r]
    d.ellipse(box, outline=(255, 255, 255, 78),
              width=max(2, int(size * ss * 0.022)))

    img.alpha_composite(layer.resize((size, size), Image.LANCZOS))


def main():
    for density, factor in DENSITIES.items():
        folder = os.path.join(RES, "mipmap-" + density)
        os.makedirs(folder, exist_ok=True)

        legacy = int(48 * factor)
        icon = gradient_square(legacy, radius=int(legacy * 0.23))
        draw_mark(icon, scale=0.62)
        icon.save(os.path.join(folder, "ic_launcher.png"))

        round_icon = gradient_circle(legacy)
        draw_mark(round_icon, scale=0.58)
        round_icon.save(os.path.join(folder, "ic_launcher_round.png"))

        # Adaptive foreground: 108dp canvas, mark in the 66dp safe zone.
        fg = int(108 * factor)
        foreground = Image.new("RGBA", (fg, fg), (0, 0, 0, 0))
        draw_mark(foreground, scale=0.44)
        foreground.save(os.path.join(folder, "ic_launcher_foreground.png"))

        # Themed icon layer (Android 13+): white glyph on transparency.
        mono = Image.new("RGBA", (fg, fg), (0, 0, 0, 0))
        draw_mark(mono, scale=0.44, color=(255, 255, 255))
        mono.save(os.path.join(folder, "ic_launcher_monochrome.png"))

    anydpi = os.path.join(RES, "mipmap-anydpi-v26")
    os.makedirs(anydpi, exist_ok=True)
    adaptive = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
    <monochrome android:drawable="@mipmap/ic_launcher_monochrome" />
</adaptive-icon>
"""
    for name in ("ic_launcher.xml", "ic_launcher_round.xml"):
        with open(os.path.join(anydpi, name), "w", encoding="utf-8") as fh:
            fh.write(adaptive)

    bg_values = os.path.join(RES, "values", "ic_launcher_background.xml")
    with open(bg_values, "w", encoding="utf-8") as fh:
        fh.write('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'
                 '    <color name="ic_launcher_background">#070D18</color>\n</resources>\n')

    # Store icon preview for docs
    docs = os.path.join(ROOT, "docs", "assets")
    os.makedirs(docs, exist_ok=True)
    preview = gradient_square(512, radius=int(512 * 0.23))
    draw_mark(preview, scale=0.62)
    preview.save(os.path.join(docs, "icon-preview.png"))
    print("icons written")


if __name__ == "__main__":
    main()
