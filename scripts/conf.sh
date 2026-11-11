#!/usr/bin/env bash
# Edit the launcher's settings file from this computer (docs/settings-file.md).
# It holds names, so it lives in pulled/, which git ignores.
#   scripts/conf.sh pull    phone -> pulled/doovdroid.conf
#   scripts/conf.sh push    pulled/doovdroid.conf -> phone (shows next time the home screen appears)
set -euo pipefail
. "$(dirname "$0")/lib.sh"
cd "$DOOV_ROOT"
REMOTE=/sdcard/Android/data/uk.me.jamez.doovdroid/files/doovdroid.conf
mkdir -p pulled
case "${1:-}" in
  pull)
    adb_run exec-out cat "$REMOTE" > pulled/doovdroid.conf
    echo "pulled/doovdroid.conf" ;;
  push)
    # Never `adb push` onto the file itself: a file created by the shell user gets the shell's
    # SELinux label and the app can't read it. Overwrite the app's own file in place instead
    # (the app creates it the first time it runs).
    adb_push pulled/doovdroid.conf /data/local/tmp/doovdroid.conf
    adb_run shell "test -f $REMOTE || { echo '$REMOTE is missing: open the launcher once first' >&2; exit 1; }
      cat /data/local/tmp/doovdroid.conf > $REMOTE && rm /data/local/tmp/doovdroid.conf"
    echo "pushed" ;;
  *) echo "usage: $0 pull|push" >&2; exit 2 ;;
esac
