# Changes made on the phone

Newest first. Every entry says how to undo it. `A` below means
`adb -s SERIAL` run on `bench-laptop` (`ssh -F ~/.ssh/config bench-laptop`).

To put the phone back exactly as found, run every **Undo** line, newest first.

## 2026-11-11 — launcher installed (not yet the home screen)

- **Installed** `uk.me.jamez.doovdroid` (debug-signed with `~/.android/debug.keystore`) via
  `scripts/deploy.sh`. Undo: `A uninstall uk.me.jamez.doovdroid` (also removes everything below).
- **Granted** it `CALL_PHONE`, `READ_CONTACTS`, `READ_CALL_LOG`, `READ_SMS` with `pm grant`.
  Undo: `A shell pm revoke uk.me.jamez.doovdroid android.permission.<NAME>`, or uninstall.
- **Settings file** `/sdcard/Android/data/uk.me.jamez.doovdroid/files/doovdroid.conf`: the six
  speed-dial names from the old launcher's first page, in the same order (`speed=` lines; the
  names stay on the phone, see `scripts/conf.sh`). Undo: uninstall, or edit and push.

Nothing else changed: the Doov launcher (`com.dp.op`) is still the home screen.
