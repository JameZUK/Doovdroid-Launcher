# Changes made on the phone

Newest first. Every entry says how to undo it. `A` below means
`adb -s SERIAL` run on `bench-laptop` (`ssh -F ~/.ssh/config bench-laptop`).

To put the phone back exactly as found, run every **Undo** line, newest first.

## 2026-11-11 — lock-down: Doovdroid is the home screen

Applied with `scripts/setup-phone.sh` (runs `scripts/phone/setup.sh` on the phone).
**Undo all of it:** `scripts/setup-phone.sh --restore` (runs `scripts/phone/restore.sh`).

| Change | Undo by hand |
|--------|--------------|
| Home app: `cmd role add-role-holder android.app.role.HOME uk.me.jamez.doovdroid` (was `com.dp.op`) | `A shell cmd role add-role-holder android.app.role.HOME com.dp.op` |
| Disabled for user 0: Facebook, YouTube, Chrome, Gmail (`pm disable-user --user 0 …`) | `A shell pm enable --user 0 <pkg>` |
| `secure incall_power_button_behavior` = `2` (red/power key hangs up; was unset = screen off) | `A shell settings delete secure incall_power_button_behavior` |
| Default keyboard: T9 `com.marshaltec.ime.t9ime/.T9IME` (was Gboard; T9 was already enabled) | `A shell ime set com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME` |
| Accessibility: `uk.me.jamez.doovdroid/…GuardService` appended to `enabled_accessibility_services` (was `null:com.android.settings/com.android.settings.ButtonLightService`; `accessibility_enabled` was already 1) | `A shell settings put secure enabled_accessibility_services null:com.android.settings/com.android.settings.ButtonLightService` |
| Notification access: `cmd notification allow_listener uk.me.jamez.doovdroid/…BadgeListener` | `A shell cmd notification disallow_listener uk.me.jamez.doovdroid/uk.me.jamez.doovdroid.BadgeListener` |

**Not** disabled, though the plan said "the store": the Play Store. WhatsApp is from the Play Store
and must be updated from it now and then, so it stays enabled and the guard hides it; it opens from
the admin menu.

## 2026-11-11 — launcher installed (not yet the home screen)

- **Installed** `uk.me.jamez.doovdroid` (debug-signed with `~/.android/debug.keystore`) via
  `scripts/deploy.sh`. Undo: `A uninstall uk.me.jamez.doovdroid` (also removes everything below).
- **Granted** it `CALL_PHONE`, `READ_CONTACTS`, `READ_CALL_LOG`, `READ_SMS` with `pm grant`.
  Undo: `A shell pm revoke uk.me.jamez.doovdroid android.permission.<NAME>`, or uninstall.
- **Settings file** `/sdcard/Android/data/uk.me.jamez.doovdroid/files/doovdroid.conf`: the six
  speed-dial names from the old launcher's first page, in the same order (`speed=` lines; the
  names stay on the phone, see `scripts/conf.sh`). Undo: uninstall, or edit and push.

Nothing else changed: the Doov launcher (`com.dp.op`) is still the home screen.
