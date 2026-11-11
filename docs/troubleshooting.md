# Troubleshooting

[← Back to the README](../README.md)

## A person shows "no number"

No contact has exactly that name any more (it was renamed or deleted). In
[Speed dial](speed-dial.md), remove them and add them again from Contacts.

## Changes to the settings file are ignored

The file was probably replaced with `adb push`, which leaves it unreadable to the launcher, so it
falls back to the defaults. Delete it (`scripts/adb.sh shell rm /sdcard/Android/data/uk.me.jamez.doovdroid/files/doovdroid.conf`),
open the launcher once so it writes a fresh one, then use `scripts/conf.sh push`. See
[Settings file](settings-file.md).

## `*` or `#` won't type on the home screen

That's the phone: it turns them into volume keys whenever no text box is open. Inside the dialler
and Messages they type normally.

## The notification pull-down flashes open

Expected. The [guard](guard.md) closes it within a couple of seconds.

## Stuck in Settings after using the admin menu

Press **red** to turn the screen off, then wake the phone: it comes back to the home screen. If the
15 minutes haven't run out, open the admin menu and choose **Lock now**.

## WhatsApp says it's out of date

WhatsApp is updated from the Play Store, which needs a Google account. If the phone hasn't got one,
either:

- open the [admin menu](admin-menu.md) › **Play Store**, sign in and update WhatsApp, or
- download the APK from <https://www.whatsapp.com/android> and install it with
  `scripts/adb.sh install -r WhatsApp.apk`. WhatsApp keeps its chats.

## The maker's launcher appears

It's the fallback if the launcher isn't running, for example just after an update. The guard
switches back to the launcher straight away. If it keeps happening, run `scripts/setup-phone.sh`
again.

## A call wasn't ended by the red key

Run `scripts/setup-phone.sh` again: it sets the "power key ends call" setting.

## Put everything back as it was

```sh
scripts/setup-phone.sh --restore
scripts/adb.sh uninstall uk.me.jamez.doovdroid
```

See [Installing](installing.md#undoing-it).
