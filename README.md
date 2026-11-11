# Doovdroid

A replacement home screen (launcher) that makes the Doov keypad Android phone work like an
old Nokia 3210: one screen, driven entirely by the keypad, nothing to swipe, nothing to get
lost in.

## The phone

A Doov-branded MediaTek keypad phone (reports as `alps` / `r77`), Android 14, 480×640 screen,
full hardware keypad. Details, key map and quirks: [`docs/device.md`](docs/device.md).

It is plugged into USB on the bench laptop `bench-laptop` (`bench-laptop` in the home-lab SSH
config); everything is driven with `adb` there over SSH:

```sh
ssh -F ~/.ssh/config bench-laptop 'adb devices -l'
```

## What the user sees

```
┌──────────────────────────┐
│ ▂▄▆█ O2 - UK      ▮ 100% │
│          20:48           │
│     Wed 11 November      │
│▓▓ Person 1 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓│  ← highlight bar, moved with ▲ ▼
│   Person 2               │
│   Person 3               │
│   Person 4               │
│   Person 5               │
│   Person 6               │
│   Messages            11 │
│   WhatsApp            23 │
│   Recent calls   2 missed│
│   Contacts               │
│──────────────────────────│
│ Call                     │  ← what the left dash key does
└──────────────────────────┘
```

- **One screen.** No pages, no scrolling; touch is ignored on the home screen.
- **▲ ▼** move the highlight. **OK** or **green** calls the highlighted person, or opens the
  highlighted item.
- **The two dash keys** (blank soft keys either side of the D-pad) do what the bar at the bottom
  of the screen says: left is **Call** / **Open**, right is **Delete** while dialling.
- **Typing digits** starts dialling a number, shown large; green, OK or the left dash key calls it;
  the right dash key deletes a digit (hold to clear).
- **Red is the power key** on this phone. It hangs up a call; otherwise it turns the screen off,
  and waking the phone always comes back to this screen.
- Unread counts for Messages and WhatsApp, and missed calls, appear on their rows.
- Lock screen: the phone's own, unchanged; the **left dash key** unlocks it.

## Admin access

Settings and everything else are hidden. Type the admin code on the home screen keypad
(`*#` + PIN + `#`, see [`docs/admin.md`](docs/admin.md)) to open the admin menu: Settings,
the original Doov launcher, Play Store, for 15 minutes.

## Repo layout

| Path | What |
|------|------|
| `app/` | The launcher app (Kotlin, framework APIs only, no AndroidX) |
| `docs/mockup.html` | Interactive mockup of the home screen (published: https://claude.ai/artifact/JQ1YQoC5fXoZaYqUyvtSGe) |
| `docs/device.md` | What the phone is: hardware, keys, installed software, quirks |
| `docs/phone-changes.md` | **Every change made on the phone, with the command to undo it** |
| `docs/admin.md` | Admin code, config file on the phone, how the guard works |
| `scripts/` | Build, deploy and lock-down helpers |

## Privacy

The speed-dial list and anything else personal stays **on the phone** (config file under
`/sdcard/Android/data/uk.me.jamez.doovdroid/files/`). No names-to-numbers, contacts, messages or
screenshots of personal data go in this repo.
