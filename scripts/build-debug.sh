#!/usr/bin/env bash
set -euo pipefail
command -v gradle >/dev/null || { echo "Gradle 9.3.1 is required; use Android Studio or GitHub Actions." >&2; exit 1; }
command -v keytool >/dev/null || { echo "JDK 21 keytool was not found." >&2; exit 1; }
if [[ ! -f debug.keystore ]]; then
  keytool -genkeypair -v -keystore debug.keystore -storepass android \
    -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 \
    -validity 10000 -dname "CN=Android Debug,O=Clg 1%,C=IN"
fi
gradle testDebugUnitTest --no-daemon
gradle assembleDebug --no-daemon
echo "APK: app/build/outputs/apk/debug/app-debug.apk"
