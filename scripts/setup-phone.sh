#!/usr/bin/env bash
# Lock the Doov phone down around the launcher: scripts/setup-phone.sh
# Undo everything:                                scripts/setup-phone.sh --restore
set -euo pipefail
cd "$(dirname "$0")/.."
SERIAL="${DOOV_SERIAL:-SERIAL}"
SSHCONF="$HOME/.ssh/config"
SSH=(ssh -F "$SSHCONF" -o BatchMode=yes -o ConnectTimeout=10 bench-laptop)
SCRIPT=scripts/phone/setup.sh
[ "${1:-}" = "--restore" ] && SCRIPT=scripts/phone/restore.sh
NAME=$(basename "$SCRIPT")
"${SSH[@]}" 'mkdir -p ~/doovdroid'
scp -q -F "$SSHCONF" "$SCRIPT" "bench-laptop:doovdroid/$NAME"
"${SSH[@]}" "adb -s $SERIAL push ~/doovdroid/$NAME /data/local/tmp/doovdroid-$NAME >/dev/null && adb -s $SERIAL shell sh /data/local/tmp/doovdroid-$NAME; adb -s $SERIAL shell rm -f /data/local/tmp/doovdroid-$NAME"
