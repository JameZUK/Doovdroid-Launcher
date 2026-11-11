#!/usr/bin/env bash
# Edit the launcher's settings file on the phone. It holds names, so it lives in pulled/ (git-ignored).
#   scripts/conf.sh pull    -> pulled/doovdroid.conf
#   scripts/conf.sh push    <- pulled/doovdroid.conf  (shows on the phone next time home appears)
set -euo pipefail
cd "$(dirname "$0")/.."
SERIAL="${DOOV_SERIAL:-SERIAL}"
SSH=(ssh -F "$HOME/.ssh/config" -o BatchMode=yes -o ConnectTimeout=10 bench-laptop)
REMOTE=/sdcard/Android/data/uk.me.jamez.doovdroid/files/doovdroid.conf
mkdir -p pulled
case "${1:-}" in
  pull) "${SSH[@]}" "adb -s $SERIAL exec-out cat $REMOTE" > pulled/doovdroid.conf; echo "pulled/doovdroid.conf" ;;
  push)
    # Never `adb push` onto the file: a file created by the shell user gets the shell's SELinux
    # label and the app can't read it. Overwrite the app's own file in place with cat instead
    # (the app creates it on first start).
    scp -q -F "$HOME/.ssh/config" pulled/doovdroid.conf bench-laptop:doovdroid/doovdroid.conf
    "${SSH[@]}" "adb -s $SERIAL push ~/doovdroid/doovdroid.conf /data/local/tmp/doovdroid.conf >/dev/null && rm ~/doovdroid/doovdroid.conf &&
      adb -s $SERIAL shell 'test -f $REMOTE || { echo \"$REMOTE missing: open the launcher once first\" >&2; exit 1; }; cat /data/local/tmp/doovdroid.conf > $REMOTE && rm /data/local/tmp/doovdroid.conf' && echo pushed" ;;
  *) echo "usage: $0 pull|push" >&2; exit 2 ;;
esac
