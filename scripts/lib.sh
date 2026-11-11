# Shared by the scripts in this folder: how to reach the phone.
#
# Put these in scripts/local.env (git-ignored; start from scripts/local.env.example) or the environment:
#   DOOV_SERIAL       adb serial of the phone. Leave empty if it's the only device attached.
#   DOOV_SSH          Run adb on another computer over SSH (the one the phone is plugged into),
#                     e.g. "bench-laptop". Leave empty to use adb on this computer.
#   DOOV_SSH_CONFIG   An ssh config file to use for DOOV_SSH (optional).
#   DOOV_GRADLE_ARGS  Extra Gradle arguments for deploy.sh, e.g. "--offline" (optional).

DOOV_ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
# shellcheck disable=SC1091
[ -f "$DOOV_ROOT/scripts/local.env" ] && . "$DOOV_ROOT/scripts/local.env"
DOOV_SERIAL=${DOOV_SERIAL:-}
DOOV_SSH=${DOOV_SSH:-}
DOOV_SSH_CONFIG=${DOOV_SSH_CONFIG:-}
DOOV_GRADLE_ARGS=${DOOV_GRADLE_ARGS:-}

_doov_ssh_opts=(-o BatchMode=yes -o ConnectTimeout=10)
[ -n "$DOOV_SSH_CONFIG" ] && _doov_ssh_opts=(-F "$DOOV_SSH_CONFIG" "${_doov_ssh_opts[@]}")

# adb_run ARGS...   Run adb against the phone, here or on DOOV_SSH. Arguments pass through intact.
adb_run() {
  local args=()
  [ -n "$DOOV_SERIAL" ] && args=(-s "$DOOV_SERIAL")
  args+=("$@")
  if [ -n "$DOOV_SSH" ]; then
    ssh "${_doov_ssh_opts[@]}" "$DOOV_SSH" "adb $(printf '%q ' "${args[@]}")"
  else
    adb "${args[@]}"
  fi
}

# adb_push LOCAL_FILE PHONE_PATH   Copy a file from this computer onto the phone.
adb_push() {
  local src=$1 dst=$2
  if [ -n "$DOOV_SSH" ]; then
    local tmp
    tmp="doovdroid-upload-$(basename "$src")"
    scp -q "${_doov_ssh_opts[@]}" "$src" "$DOOV_SSH:$tmp"
    adb_run push "$tmp" "$dst" >/dev/null
    ssh "${_doov_ssh_opts[@]}" "$DOOV_SSH" "rm -f $(printf '%q' "$tmp")"
  else
    adb_run push "$src" "$dst" >/dev/null
  fi
}
