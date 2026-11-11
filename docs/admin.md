# Looking after the phone

## Admin menu

On the home screen type **`0000` and then the PIN** (default PIN `3210`, so `00003210`). No real
phone number starts with `0000`, so it can't happen by accident. `*` and `#` can't be used in the
code: on this phone they're volume keys whenever no text box has focus.

The admin menu unlocks the phone for **15 minutes**:

| Item | Does |
|------|------|
| Settings | Android Settings |
| Doov launcher | The maker's old big-tile launcher, without changing the default |
| Play Store | For updating WhatsApp (see below) |
| Lock now | Ends the 15 minutes early |

Left dash key or OK selects; right dash key closes. Touch works here too.

**Change the PIN** in the settings file (below). Do it before the phone goes back to its owner.

## Settings file

`/sdcard/Android/data/uk.me.jamez.doovdroid/files/doovdroid.conf`, created by the app on first run.
It holds names, so it is never committed; edit it through `pulled/` (git-ignored):

```sh
scripts/conf.sh pull      # -> pulled/doovdroid.conf
$EDITOR pulled/doovdroid.conf
scripts/conf.sh push      # shows on the phone next time the home screen appears
```

```ini
pin=3210            # admin code is 0000 + this; 4-12 digits
style=lcd           # lcd (pale green) | light (black on white) | dark (white on black)
speed=Alice        # speed-dial rows, top to bottom, up to 8: the name exactly as in Contacts
speed=Name|07700900123   # or force a number
```

With no `speed=` lines the starred contacts are used. A name that isn't found shows "no number".

**Never `adb push` onto that file.** A file the shell user creates there gets the shell's SELinux
label and the app can't read it (it silently falls back to defaults). `conf.sh push` overwrites the
app's own file in place with `cat`, which keeps the label.

## The guard

`GuardService`, an accessibility service. While the admin menu is not unlocked it:

- **closes the notification pull-down** as soon as it opens (it leads to quick settings and Settings).
  SystemUI sometimes ignores the dismiss, so it retries for 2 s and then presses Back if the shade
  still has focus (never on the lock screen);
- **sends Settings, the Play Store, the old launchers, Files, Gallery, Calendar, the vendor's
  "Gboard"/"Clean Memory" apps and similar straight back home** (list: `GuardService.BLOCKED`);
- **brings the home screen back whenever the screen goes off** (the red key is the power key), unless a
  call is active or ringing (`AudioManager` mode isn't normal).

Calls, Messages, WhatsApp, Contacts, the keyboards, permission prompts, emergency alerts and the clock
(alarms) are never touched.

## WhatsApp updates

WhatsApp came from the Play Store, but there is **no Google account on the phone**, so the Play Store
can't update it by itself. When WhatsApp says it is out of date, either sign a Google account in from
the admin menu's Play Store, or install the current APK from <https://www.whatsapp.com/android> with
`adb install -r` (WhatsApp keeps its data).

## Undo

`scripts/setup-phone.sh --restore` puts back every setting (`docs/phone-changes.md` lists them), and
`scripts/adb.sh uninstall uk.me.jamez.doovdroid` removes the app.
