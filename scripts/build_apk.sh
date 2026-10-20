#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Offline APK builder for environments without the Android SDK / Gradle.
#
# It uses any aapt2 + android.jar + d8 + apksigner available through
# environment variables, so CI (or a maintainer laptop) can produce a
# signed, zipaligned release APK with nothing but a JDK installed.
#
#   AAPT2=/path/to/aapt2
#   ANDROID_JAR=/path/to/android.jar
#   D8_JAR=/path/to/d8.jar
#   APKSIGNER_JAR=/path/to/apksigner.jar
#   KEYSTORE=/path/to/release.jks  KEYSTORE_PASS=...  KEY_ALIAS=...  KEY_PASS=...
#
# Usage: scripts/build_apk.sh [debug|release]
# ---------------------------------------------------------------------------
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Resolve the toolchain from an installed Android SDK when the environment does
# not point at explicit binaries.
if [ -n "${ANDROID_HOME:-}${ANDROID_SDK_ROOT:-}" ]; then
  SDK="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
  BUILD_TOOLS=$(ls -d "$SDK"/build-tools/* 2>/dev/null | sort -V | tail -1 || true)
  PLATFORM=$(ls -d "$SDK"/platforms/android-35 2>/dev/null | head -1 || true)
  if [ -z "${AAPT2:-}" ] && [ -n "$BUILD_TOOLS" ]; then AAPT2="$BUILD_TOOLS/aapt2"; fi
  if [ -z "${D8_JAR:-}" ] && [ -n "$BUILD_TOOLS" ]; then D8_JAR="$BUILD_TOOLS/lib/d8.jar"; fi
  if [ -z "${APKSIGNER_JAR:-}" ] && [ -n "$BUILD_TOOLS" ]; then APKSIGNER_JAR="$BUILD_TOOLS/lib/apksigner.jar"; fi
  if [ -z "${ANDROID_JAR:-}" ] && [ -n "$PLATFORM" ]; then ANDROID_JAR="$PLATFORM/android.jar"; fi
fi
MODE="${1:-release}"
BUILD="$ROOT/build/manual"
OUT="$ROOT/build/outputs"
APP="$ROOT/app/src/main"

: "${AAPT2:?set AAPT2}"
: "${ANDROID_JAR:?set ANDROID_JAR}"
: "${D8_JAR:?set D8_JAR}"
: "${APKSIGNER_JAR:?set APKSIGNER_JAR}"
: "${JAVA_HOME:?set JAVA_HOME}"
JAVA="$JAVA_HOME/bin/java"
JAVAC="$JAVA_HOME/bin/javac"

VERSION_NAME=$(grep -oE "versionName '[^']+'" "$ROOT/app/build.gradle" | head -1 | cut -d"'" -f2)
VERSION_CODE=$(grep -oE "versionCode [0-9]+" "$ROOT/app/build.gradle" | head -1 | awk '{print $2}')
MIN_SDK=24
TARGET_SDK=35
if [ "$MODE" = "debug" ]; then
  DEBUGGABLE=true
else
  DEBUGGABLE=false
fi
NAMESPACE=$(grep -oE "namespace '[^']+'" "$ROOT/app/build.gradle" | head -1 | cut -d"'" -f2)

rm -rf "$BUILD" && mkdir -p "$BUILD"/{res,gen,classes,dex} "$OUT"

# Gradle 8 takes the package name from `namespace`; older aapt2 needs it in the
# manifest, so the build uses a copy with the attribute injected.
python3 - "$APP/AndroidManifest.xml" "$BUILD/AndroidManifest.xml" "$NAMESPACE" <<'PY'
import sys
src, dst, namespace = sys.argv[1], sys.argv[2], sys.argv[3]
data = open(src, encoding="utf-8").read()
data = data.replace("<manifest ",
                    '<manifest package="%s" ' % namespace, 1)
data = data.replace("${applicationId}", namespace)
open(dst, "w", encoding="utf-8").write(data)
PY

echo "==> aapt2 compile"
"$AAPT2" compile --dir "$APP/res" -o "$BUILD/res/resources.zip"

echo "==> aapt2 link ($VERSION_NAME / $VERSION_CODE)"
LINK_FLAGS=(
  -o "$BUILD/base.apk"
  -I "$ANDROID_JAR"
  --manifest "$BUILD/AndroidManifest.xml"
  --java "$BUILD/gen"
  --min-sdk-version "$MIN_SDK"
  --target-sdk-version "$TARGET_SDK"
  --version-code "$VERSION_CODE"
  --version-name "$VERSION_NAME"
  --auto-add-overlay
  -A "$APP/assets"
)
if [ "$MODE" = "debug" ]; then
  LINK_FLAGS+=(--debug-mode)
fi
"$AAPT2" link "${LINK_FLAGS[@]}" "$BUILD/res/resources.zip"

# BuildConfig equivalent (Gradle generates this file itself).
APP_ID=$(grep -oE "applicationId '[^']+'" "$ROOT/app/build.gradle" | head -1 | cut -d"'" -f2)
mkdir -p "$BUILD/gen/com/phoneflipper/app"
cat > "$BUILD/gen/com/phoneflipper/app/BuildConfig.java" <<JAVA
package com.phoneflipper.app;

public final class BuildConfig {
    public static final boolean DEBUG = ${DEBUGGABLE};
    public static final String APPLICATION_ID = "$APP_ID";
    public static final String BUILD_TYPE = "$MODE";
    public static final int VERSION_CODE = $VERSION_CODE;
    public static final String VERSION_NAME = "$VERSION_NAME";
}
JAVA

echo "==> compile Java"
find "$APP/java" "$BUILD/gen" -name '*.java' > "$BUILD/sources.txt"
if [ -n "${ECJ_JAR:-}" ]; then
  "$JAVA" -jar "$ECJ_JAR" -source 8 -target 8 -bootclasspath "$ANDROID_JAR" \
    -d "$BUILD/classes" -nowarn @"$BUILD/sources.txt"
else
  "$JAVAC" -source 8 -target 8 -bootclasspath "$ANDROID_JAR" \
    -d "$BUILD/classes" -nowarn -Xlint:-options @"$BUILD/sources.txt"
fi

echo "==> d8"
find "$BUILD/classes" -name '*.class' > "$BUILD/classes.txt"
"$JAVA" -cp "$D8_JAR" com.android.tools.r8.D8 \
  --lib "$ANDROID_JAR" --min-api "$MIN_SDK" \
  --output "$BUILD/dex" @"$BUILD/classes.txt"

echo "==> package"
python3 "$ROOT/scripts/package_apk.py" "$BUILD/base.apk" "$BUILD/dex/classes.dex" \
  "$BUILD/unsigned.apk"

echo "==> zipalign"
python3 "$ROOT/scripts/zipalign.py" "$BUILD/unsigned.apk" "$BUILD/aligned.apk"

echo "==> sign"
NAME="FlipperOne-${VERSION_NAME}-${MODE}.apk"
if [ -n "${KEYSTORE:-}" ] && [ -f "$KEYSTORE" ]; then
  "$JAVA" -jar "$APKSIGNER_JAR" sign \
    --ks "$KEYSTORE" --ks-key-alias "${KEY_ALIAS:-flipperone}" \
    --ks-pass "pass:${KEYSTORE_PASS:-flipperone2026}" \
    --key-pass "pass:${KEY_PASS:-${KEYSTORE_PASS:-flipperone2026}}" \
    --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true \
    --out "$OUT/$NAME" "$BUILD/aligned.apk"
  # Write the verification report first: piping it into head can raise SIGPIPE
  # under `set -o pipefail` and fail an otherwise successful build.
  "$JAVA" -jar "$APKSIGNER_JAR" verify --print-certs "$OUT/$NAME" > "$BUILD/verify.txt"
  grep -E "certificate (DN|SHA-256)" "$BUILD/verify.txt" || true
else
  cp "$BUILD/aligned.apk" "$OUT/$NAME"
  echo "WARNING: unsigned APK (no KEYSTORE provided)"
fi

echo "==> done: $OUT/$NAME ($(du -h "$OUT/$NAME" | cut -f1))"
