#!/usr/bin/env bash
# Set the phone up around the launcher (docs/installing.md):   scripts/setup-phone.sh
# Undo every setting it changed:                                scripts/setup-phone.sh --restore
set -euo pipefail
. "$(dirname "$0")/lib.sh"
cd "$DOOV_ROOT"
SCRIPT=scripts/phone/setup.sh
[ "${1:-}" = "--restore" ] && SCRIPT=scripts/phone/restore.sh
TMP=/data/local/tmp/doovdroid-$(basename "$SCRIPT")
adb_push "$SCRIPT" "$TMP"
adb_run shell sh "$TMP"
adb_run shell rm -f "$TMP"
