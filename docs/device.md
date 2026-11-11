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
| Green | `KEY_SEND` | `KEYCODE_CALL` |
| Red | `KEY_END` | `KEYCODE_ENDCALL` (handled by the system; apps never see it) |
| Left / right soft keys | `KEY_MENU` / `KEY_BACK` (assumed: Menu unlocks the lock screen) | `KEYCODE_MENU` / `KEYCODE_BACK` |
| Side | `KEY_VOLUMEUP/DOWN`, `KEY_POWER` | |
| ? | `KEY_H`, `KEY_B` | advertised by `mtk-kpd`; no physical key found yet. **TODO** confirm |

**Red key behaviour** is `Settings.System.end_button_behavior` (stock value `2` = sleep).

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
- **Vendor oddities, not yet looked at:** `com.example.google`, `com.zkwl.zkwlesc`,
  `com.meng.tools`, `com.bsktek.update` (FOTA?), `com.custom.voice`, `com.iflytek.speechsuite`. **TODO**.
- **Contacts:** 258; SIM, phone and WhatsApp copies of the same people, so each speed-dial name
  matches 2–3 raw contacts that share one normalised number. None are starred.
- **Lock screen:** swipe (no PIN); the **Menu** key dismisses it. `locksettings verify` succeeds with
  no credential.
- Font scale `1.3`, screen timeout 30 s, light theme.
