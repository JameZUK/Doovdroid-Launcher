#!/usr/bin/env bash
# Build the launcher and install it on the phone, keeping its data.
set -euo pipefail
. "$(dirname "$0")/lib.sh"
cd "$DOOV_ROOT"
# shellcheck disable=SC2086
./gradlew -q $DOOV_GRADLE_ARGS assembleDebug
adb_push app/build/outputs/apk/debug/app-debug.apk /data/local/tmp/doovdroid.apk
adb_run shell pm install -r /data/local/tmp/doovdroid.apk
adb_run shell rm -f /data/local/tmp/doovdroid.apk
