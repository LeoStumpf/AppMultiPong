#!/usr/bin/env bash
# build.sh — single-command release build for Multi Pong (SpaceHats)
#
# Usage:
#   ./build.sh           build signed release APK + AAB
#   ./build.sh debug     build unsigned debug APK
#   ./build.sh clean     wipe build outputs
#
# Prerequisites (first run):
#   1. Copy keystore.properties.example → keystore.properties and fill in your keystore details.
#   2. Ensure ANDROID_HOME is set (or sdk.dir is in local.properties).

set -euo pipefail

MODE="${1:-release}"

check_keystore() {
    if [ ! -f keystore.properties ]; then
        echo "ERROR: keystore.properties not found."
        echo "Copy keystore.properties.example → keystore.properties and fill in your values."
        echo ""
        echo "To generate a new keystore:"
        echo "  keytool -genkey -v -keystore multipong-release.jks \\"
        echo "          -alias multipong -keyalg RSA -keysize 2048 -validity 10000"
        exit 1
    fi
}

case "$MODE" in
    release)
        check_keystore
        echo "Building release APK + AAB..."
        ./gradlew assembleRelease bundleRelease
        echo ""
        echo "Outputs:"
        echo "  APK: app/build/outputs/apk/release/app-release.apk"
        echo "  AAB: app/build/outputs/bundle/release/app-release.aab"
        echo ""
        echo "Upload the AAB to the Google Play Console."
        ;;
    debug)
        echo "Building debug APK..."
        ./gradlew assembleDebug
        echo "Output: app/build/outputs/apk/debug/app-debug.apk"
        ;;
    clean)
        echo "Cleaning build outputs..."
        ./gradlew clean
        echo "Done."
        ;;
    *)
        echo "Unknown mode: $MODE"
        echo "Usage: ./build.sh [release|debug|clean]"
        exit 1
        ;;
esac
