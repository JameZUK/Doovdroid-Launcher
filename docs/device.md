# The phone

Surveyed 2026-11-11 over adb from `bench-laptop`.

## Identity

| Property | Value |
|----------|-------|
| adb serial | `SERIAL` |
| Brand / model / device | `alps` / `r77` / `k62v1_64_bsp` (white-label MediaTek; Doov branding is not in the props) |
| SoC | MediaTek MT6765 platform (`ro.hardware=mt6762`), arm64-v8a |
| Android | 14 (SDK 34), security patch 2024-12-05 |
| Build | `W53-B-R777W-EN-V03-20260108`, `user` / `release-keys` |
| Kernel | 4.19.191 |
| Screen | 480×640, density 210 (≈1.31× mdpi), no navigation bar |
| Storage | 22 GB `/data` |
| SIM | O2 - UK, VoLTE |

## Security state

`ro.debuggable=0`, `ro.secure=1`, bootloader locked, verified boot green. adb gives the
ordinary `shell` uid, **no root**. So: we can install apps, grant runtime permissions, change
`settings`, enable accessibility/notification-listener services, set the home app, and
disable packages for user 0. We cannot touch system partitions.

`dpm set-device-owner` is not an option: there is an account on the phone (WhatsApp), and
device owner can't be set while accounts exist.

## Keypad

`getevent -pl`, devices: `mtk-kpd` (keypad), `gpio-keys`, `mtk-pmic-keys` (power, volume),
`sitronix_ts_i2c` + `mtk-tpd` (touch), headset jack.

There is no device-specific key layout (`mtk-kpd.kl` absent), so `Generic.kl` applies:

| Physical | Linux key | Android keycode |
|----------|-----------|-----------------|
| 0–9 | `KEY_0`…`KEY_9` | `KEYCODE_0`…`KEYCODE_9` |
| `*` / `#` | `KEY_NUMERIC_STAR` / `KEY_NUMERIC_POUND` | `KEYCODE_STAR` / `KEYCODE_POUND` |
| D-pad | `KEY_UP/DOWN/LEFT/RIGHT` | `KEYCODE_DPAD_*` |
| OK | `KEY_ENTER` | `KEYCODE_ENTER` |
| Green | `KEY_SEND` (assumed, not yet pressed in a capture) | `KEYCODE_CALL` |
| **Red** | **`KEY_POWER`** on `mtk-pmic-keys` — the red key *is* the power key (confirmed 2026-11-11) | `KEYCODE_POWER` (system: screen off/on, hold for the power menu) |
| Left dash (soft key) | `KEY_MENU` on `mtk-kpd` (confirmed) | `KEYCODE_MENU`; also unlocks the lock screen |
| Right dash (soft key) | `KEY_BACK` on `mtk-kpd` (confirmed) | `KEYCODE_BACK` |
| Side | `KEY_VOLUMEUP/DOWN` | |
| — | `KEY_END`, `KEY_H`, `KEY_B` | advertised by `mtk-kpd` but no physical key sends them |

The two keys either side of the D-pad are blank soft keys marked with a dash; the screen has to
say what they do (the launcher's bottom bar).

Because red is the power key, `end_button_behavior` (stock `2`) does nothing here. In a call the red
key follows `Settings.Secure.incall_power_button_behavior` (stock unset = `1`, screen off; `2` =
hang up).

**Confirmed with a guided capture**: `scripts/adb.sh shell getevent -lt` (takes no device list, or
exactly one device) while someone presses each key a different number of times.

## Software as found

- **Home:** `com.dp.op` ("Desktop", `/system/app/Desktop-r17-en-new-signed`, v2.3.151), a
  big-tile senior launcher across four pages:
  1. six speed-dial contact tiles (names are in the on-phone config, not here)
  2. clock/date, Messaging, Phone, Contacts, WhatsApp
  3. Settings, Clock
  4. "Caring mode", "Application Center"
- **Default dialer:** `com.android.dialer`; **default SMS:** `com.android.mms` (MediaTek stock).
- **Keyboards:** `com.marshaltec.ime.t9ime/.T9IME` (T9, installed) and Gboard (the default when found).
- **Third-party:** WhatsApp 2.26.37.73 (from Play Store; signed in, in active use), Facebook.
- **Google:** Play Store, Chrome, YouTube, Gmail, Files; **no Google account** on the phone.
- **Disabled when found:** `com.mediatek.camera`, `com.google.android.gms.supervision`, `com.android.nfc`.
- **Vendor apps** (labels from `aapt dump badging` on the pulled APKs):
  `com.example.google` "Gboard" (`/system/app/HysGboard`, has an app icon; *not* the real Gboard,
  which is `com.google.android.inputmethod.latin`), `com.meng.tools` "Clean Memory",
  `com.zkwl.zkwlesc` "ZkwlESC" (unknown), `com.bsktek.update` "FOTA Update" (system updates, leave
  alone), `com.custom.voice` "VoiceBroadcast", `com.iflytek.speechsuite`. What ZkwlESC and the
  fake Gboard actually do: **TODO**.
- **"Caring mode"** on the old launcher is `com.android.settings/.Settings$CareboardActivity`.
- **Accessibility services already on:** `com.android.settings/…ButtonLightService` (keypad backlight,
  presumably; the stored value is literally `null:com.android.settings/…`). Keep it.
- **Notification listeners already on:** `com.dp.op/…DeskNoticeService`, `com.android.launcher3/…NotificationListener`.
- **Contacts:** 258; SIM, phone and WhatsApp copies of the same people, so each speed-dial name
  matches 2–3 raw contacts that share one normalised number. None are starred.
- **Lock screen:** swipe (no PIN); the **Menu** key dismisses it. `locksettings verify` succeeds with
  no credential.
- Font scale `1.3`, screen timeout 30 s, light theme.
