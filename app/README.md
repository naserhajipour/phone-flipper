# Flipper One — Android module

Zero-dependency Android application (Java, minSdk 24, targetSdk 35).

## Source map

```
src/main/java/com/phoneflipper/app/
├── FlipperApp.java          Application: locale wrapping
├── core/
│   ├── BaseActivity.java    Window, insets, top bar, navigation
│   ├── Ui.java              Design system (spacing, type, cards, buttons)
│   ├── IconDrawable.java    31 vector icons drawn on Canvas
│   ├── Palette.java         Colour tokens
│   ├── Capabilities.java    Hardware probing (12 checks)
│   ├── Fft.java             Radix-2 FFT + Hann window
│   ├── Prefs.java           Private preferences + NFC history
│   ├── LocaleManager.java   Per-app locale (en / fa / system)
│   ├── ReportBuilder.java   Text + JSON report generation
│   └── ReportProvider.java  Read-only provider for exported files
├── ui/                      One Activity per screen
└── view/                    Gauges, charts, compass, level, overlay, logo
```

## Resources

* `res/values/strings.xml` – English catalogue (~315 strings).
* `res/values-fa/strings.xml` – Persian catalogue, complete parity.
* `res/values/colors.xml`, `themes.xml` – dark identity and dialog theme.
* `res/mipmap-*` – generated launcher icons (`scripts/make_icons.py`).
* `assets/guides/{en,fa}` – six offline briefings per language.

## Conventions

* No third-party libraries and no network permission.
* Screens never build chrome; they call `installScreen` / `installBare`.
* Domain logic stays in `core`, drawing stays in `view`, screens orchestrate.
* Persian text never receives letter spacing (`Ui.track` handles this).
* Every new string must exist in both `values` and `values-fa`.

## Build

See [`../docs/building.md`](../docs/building.md).
