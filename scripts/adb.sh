#!/usr/bin/env bash
# Run adb against the Doov phone, which is plugged into the bench laptop (bench-laptop / bench-laptop).
#   scripts/adb.sh shell getprop ro.product.model
#   scripts/adb.sh exec-out screencap -p > shot.png
set -euo pipefail
SERIAL="${DOOV_SERIAL:-SERIAL}"
SSH=(ssh -F "$HOME/.ssh/config" -o BatchMode=yes -o ConnectTimeout=10 bench-laptop)
# Quote every argument for the remote shell so spaces and quotes survive the SSH hop.
exec "${SSH[@]}" "adb -s $SERIAL $(printf '%q ' "$@")"
