# The guard

[← Back to the README](../README.md)

The guard keeps the phone on the simple home screen. It's a small background service in the
launcher (an Android *accessibility service*) that watches which app is on screen.

## What it does

Unless the [admin menu](admin-menu.md) has been opened in the last 15 minutes:

| When | The guard |
|------|-----------|
| The **notification pull-down** or quick settings opens | Closes it. They lead to Settings |
| **Settings**, the **app store**, the **old launcher** or another hidden app opens | Goes straight back to the home screen |
| The **screen goes off** (red key or timeout) | Puts the home screen back underneath, so the phone wakes up on it. Not during a call, and not while it's ringing |

## What it never touches

Calls (phone and WhatsApp), Messages, WhatsApp, Contacts, Recent calls, the keyboard, permission
prompts, emergency alerts and the clock app (so alarms still ring and can be stopped).

## Hidden apps

Settings, Play Store, Files, Gallery, Music, Calendar, Sound recorder, Calculator, Camera, Chrome,
YouTube, Facebook, Gmail, the maker's launcher, the stock Android launcher, the factory test app,
and two vendor extras ("Clean Memory" and a fake "Gboard" app). The full list is `BLOCKED` in
[`GuardService.kt`](../app/src/main/java/uk/me/jamez/doovdroid/GuardService.kt).

Facebook, YouTube, Chrome and Gmail are also switched off completely by
[setup](installing.md#what-setup-changes). The Play Store is left on, because WhatsApp is updated
through it.

## Notes

- **The pull-down can flash open for a moment.** Android sometimes ignores the first "close", so the
  guard keeps trying for two seconds and then presses Back if it's still open. It never presses Back
  on the lock screen.
- **What it can see.** Accessibility services can read the screen. The guard only looks at *which
  app* is in front. It doesn't read or log anything else.
- **Switching it off** happens with [`setup-phone.sh --restore`](installing.md#undoing-it).
