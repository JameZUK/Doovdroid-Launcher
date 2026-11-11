# The phone

[← Back to the README](../README.md)

Notes on the Doov keypad phone this was built for, from looking at it over adb. Useful if you're
trying the launcher on a different keypad phone: the [keys](#keys) are the part most likely to
differ.

## At a glance

| | |
|---|---|
| Reports itself as | `alps` / `r77` / `k62v1_64_bsp` (a white-label MediaTek design; "Doov" isn't in its system properties) |
| Chip | MediaTek MT6765 family (`mt6762`), 64-bit ARM |
| Android | 14 (SDK 34) |
| Build | `W53-B-R777W-EN-V03-20260108`, a locked `user` build |
| Screen | 480 × 640, 210 dpi, no on-screen navigation bar; small touchscreen |
| Keypad | Full hardware keypad, see below |

It's a locked-down production phone: no root, bootloader locked. Over adb you can install apps,
grant permissions, change settings, switch apps off and choose the home app, which is all this
project needs. "Device owner" (proper kiosk mode) isn't possible once any account is signed in, and
WhatsApp counts as one.

## Keys

The keypad has no layout file of its own, so Android's generic one applies. Each key was confirmed
by pressing it while watching `getevent -lt`.

| Key | Sends | Notes |
|-----|-------|-------|
| 0–9 | `KEYCODE_0`…`9` | |
| `*` `#` | `KEYCODE_STAR` / `POUND` | **Turned into media volume down / up** when no text box has focus. Apps never see them. Something in the vendor software does this |
| D-pad, OK | `KEYCODE_DPAD_*`, `KEYCODE_ENTER` | |
| Green | `KEYCODE_CALL` | |
| **Red** | **`KEYCODE_POWER`** | The red key *is* the power key: screen off/on, hold for the power menu. In calls it follows the "power button ends call" setting |
| Left soft key (blank, "–") | `KEYCODE_MENU` | Also unlocks the lock screen |
| Right soft key (blank, "–") | `KEYCODE_BACK` | |
| Side keys | Volume up / down | |

The keypad driver also advertises `KEY_END`, `KEY_H` and `KEY_B`, but no physical key sends them.

## Software it came with

- **Launcher:** `com.dp.op`, a big-tile "senior" launcher over four pages: speed-dial tiles; Messaging,
  Phone, Contacts, WhatsApp; Settings and Clock; "Caring mode" (a Settings screen) and an app store.
- **Dialler and messages:** the stock Android dialler (`com.android.dialer`) and MediaTek's messaging
  app (`com.android.mms`).
- **Keyboards:** Gboard (default) and a T9 keypad keyboard, `com.marshaltec.ime.t9ime`.
- **Google apps:** Play Store, Chrome, YouTube, Gmail, Files. Plus Facebook.
- **Vendor extras:** a "Clean Memory" app (`com.meng.tools`), an app labelled "Gboard" that is *not*
  Gboard (`com.example.google`), "FOTA Update" (`com.bsktek.update`, system updates: leave it alone),
  "VoiceBroadcast" (`com.custom.voice`), and "ZkwlESC" (`com.zkwl.zkwlesc`, purpose unknown).
- **Already-enabled services to keep:** an accessibility service in Settings, `ButtonLightService`
  (probably the keypad backlight). Setup adds the guard *beside* it.
- **Lock screen:** swipe, no PIN; the left soft key unlocks it.
- Font size is set larger than normal (1.3×), and the screen times out after 30 seconds.
