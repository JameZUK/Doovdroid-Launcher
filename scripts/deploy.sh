#!/usr/bin/env bash
# Build the launcher and install it on the Doov phone (via bench-laptop). Keeps app data (-r).
set -euo pipefail
cd "$(dirname "$0")/.."
SERIAL="${DOOV_SERIAL:-SERIAL}"
SSH=(ssh -F "$HOME/.ssh/config" -o BatchMode=yes -o ConnectTimeout=10 bench-laptop)

./gradlew --offline -q assembleDebug
APK=app/build/outputs/apk/debug/app-debug.apk
"${SSH[@]}" 'mkdir -p ~/doovdroid'
scp -q -F "$HOME/.ssh/config" "$APK" bench-laptop:doovdroid/doovdroid.apk
"${SSH[@]}" "adb -s $SERIAL install -r ~/doovdroid/doovdroid.apk"
