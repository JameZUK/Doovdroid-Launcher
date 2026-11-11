# Development

[← Back to the README](../README.md)

## Code

Plain Kotlin and Android framework APIs: no AndroidX, no Compose, no other libraries. Layouts are
built in code and sized in percentages of the screen width, so the single screen always fits
(whatever the phone's font size).

| File | What |
|------|------|
| [`HomeActivity.kt`](../app/src/main/java/uk/me/jamez/doovdroid/HomeActivity.kt) | The home screen: list, dialling, soft-key bar, keys |
| [`AdminActivity.kt`](../app/src/main/java/uk/me/jamez/doovdroid/AdminActivity.kt) | The admin menu |
| [`SpeedDialActivity.kt`](../app/src/main/java/uk/me/jamez/doovdroid/SpeedDialActivity.kt) | The speed-dial editor |
| [`LcdScreen.kt`](../app/src/main/java/uk/me/jamez/doovdroid/LcdScreen.kt) | Shared layout of the admin screens |
| [`GuardService.kt`](../app/src/main/java/uk/me/jamez/doovdroid/GuardService.kt) | [The guard](guard.md) |
| [`BadgeListener.kt`](../app/src/main/java/uk/me/jamez/doovdroid/BadgeListener.kt) | WhatsApp's unread count, from its notifications |
| [`Config.kt`](../app/src/main/java/uk/me/jamez/doovdroid/Config.kt) | Reading and writing the [settings file](settings-file.md) |
| [`PhoneData.kt`](../app/src/main/java/uk/me/jamez/doovdroid/PhoneData.kt) | Contacts, call log and SMS lookups |
| [`Admin.kt`](../app/src/main/java/uk/me/jamez/doovdroid/Admin.kt) | The 15-minute admin unlock |
| [`Ui.kt`](../app/src/main/java/uk/me/jamez/doovdroid/Ui.kt), [`StatusViews.kt`](../app/src/main/java/uk/me/jamez/doovdroid/StatusViews.kt) | Colours, text helpers, signal and battery icons |

## Scripts

| Script | Does |
|--------|------|
| `scripts/deploy.sh` | Build and install |
| `scripts/setup-phone.sh [--restore]` | [Set up the phone, or undo it](installing.md) |
| `scripts/conf.sh pull\|push` | Edit the [settings file](settings-file.md) |
| `scripts/adb.sh …` | adb against the phone, wherever it's plugged in |
| `scripts/lib.sh` | Shared: finds the phone from `scripts/local.env` |

## Build

```sh
./gradlew assembleDebug
```

Android Gradle Plugin 9.4 with its built-in Kotlin support; Kotlin 2.4. Min and target SDK 34
(the phone's Android 14), compiled against SDK 36.

## Testing on a real phone

Most testing is done by sending keys with `scripts/adb.sh shell input keyevent …` and taking
screenshots with `scripts/adb.sh exec-out screencap -p > shot.png`. Some care is needed on a phone
that's in real use:

- **Don't send OK, green, `KEYCODE_CALL` or the left soft key (`KEYCODE_MENU`) on the home screen.**
  They call whoever is highlighted. Digits and the right soft key (`KEYCODE_BACK`) are safe.
- **Don't open conversations** in Messages or WhatsApp: that marks them read and sends read receipts.
- **Don't screenshot the notification pull-down**: it shows message previews. Check its state with
  `dumpsys window | grep mCurrentFocus` instead.
- **No personal data in the repo.** For screenshots, temporarily put made-up names with numbers from
  the 07700 900xxx drama range in the settings file, then put the real file back.
- **Finding out what a key sends:** `scripts/adb.sh shell getevent -lt` (no device argument), while
  someone presses each key a different number of times.

## The mockup

[`docs/mockup.html`](mockup.html) is a clickable mockup of the phone and home screen. It's a single
self-contained page: open it in a browser.
