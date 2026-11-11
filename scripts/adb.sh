#!/usr/bin/env bash
# Run adb against the phone (see scripts/lib.sh for where it is):
#   scripts/adb.sh shell getprop ro.product.model
#   scripts/adb.sh exec-out screencap -p > screen.png
set -euo pipefail
. "$(dirname "$0")/lib.sh"
adb_run "$@"
