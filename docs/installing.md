# Installing

[← Back to the README](../README.md)

## You need

- The **Android SDK** (platform 36 and its build tools) and **JDK 17** or newer.
- **`adb`**, and the phone with **USB debugging** switched on (Settings › About phone › tap *Build
  number* seven times, then Settings › System › Developer options › USB debugging).
- The phone plugged in, and the computer allowed when the phone asks.

No root is needed.

## 1. Tell the scripts where the phone is (optional)

With one phone plugged into this computer, there's nothing to do. Otherwise copy
`scripts/local.env.example` to `scripts/local.env` (git ignores it) and fill in:

| Setting | For |
|---------|-----|
| `DOOV_SERIAL` | More than one device attached: the phone's serial from `adb devices -l` |
| `DOOV_SSH` | The phone is plugged into a *different* computer: its SSH name. The scripts run adb there |
| `DOOV_SSH_CONFIG` | An ssh config file for that host, if it isn't in `~/.ssh/config` |
| `DOOV_GRADLE_ARGS` | E.g. `--offline` once Gradle has everything it needs |

## 2. Build and install

```sh
scripts/deploy.sh
```

This builds a debug APK, installs it and keeps the launcher's data on later installs. Keep using the
same debug keystore (`~/.android/debug.keystore`), or the next install will have to uninstall
first.

## 3. Set the phone up

```sh
scripts/setup-phone.sh
```

It's safe to run again. It prints each step as it goes.

### What setup changes

| Change | Why |
|--------|-----|
| Grants the launcher *Phone*, *Contacts*, *Call log* and *SMS* permissions | To call, find speed-dial numbers and count unread texts and missed calls. Nobody is asked |
| Gives the launcher **notification access** | To read WhatsApp's unread count |
| Turns on the **guard** (an accessibility service), alongside any already on | See [The guard](guard.md) |
| Makes the **T9 keypad keyboard** the default | Texts are typed on the keys |
| Makes the **red (power) key hang up** calls | It would otherwise just turn the screen off |
| Switches off **Facebook, YouTube, Chrome and Gmail** | Not wanted, and they post notifications |
| Makes the launcher the **home screen** | |

The Play Store is deliberately left on: WhatsApp is updated through it.

## 4. Add people

On the phone, type `00003210` to open the [admin menu](admin-menu.md), choose **Speed dial** and add
people. Change the PIN in the [settings file](settings-file.md) if you like.

## Undoing it

```sh
scripts/setup-phone.sh --restore             # every setting back as it was
scripts/adb.sh uninstall uk.me.jamez.doovdroid   # and remove the launcher
```

`--restore` puts the maker's launcher back as the home screen, switches the four apps back on, puts
Gboard back as the keyboard, returns the red key to its normal behaviour, and turns off the guard and
notification access. Other accessibility services are left on. The scripts it runs on the phone are
[`scripts/phone/setup.sh`](../scripts/phone/setup.sh) and
[`scripts/phone/restore.sh`](../scripts/phone/restore.sh).
