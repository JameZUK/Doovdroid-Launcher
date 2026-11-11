# CLAUDE.md — working in Doovdroid

A Nokia-3210-style launcher for one specific keypad phone that belongs to an older relative.
Read `README.md`, then `docs/device.md`.

## Rules

- **Every change on the phone goes in `docs/phone-changes.md`** (newest first) with its undo
  command, in the same commit as the code/script that needed it.
- **Commit between changes.** One logical step per commit.
- **No personal data in the repo**: no phone numbers, contact lists, messages, or screenshots
  of the phone showing them. Speed-dial config lives on the phone only.
- **Never press the green key / place calls / send messages while testing** on the real phone,
  and don't open Messages or WhatsApp conversations (it marks them read and sends read receipts).
  Test the dialler by typing digits and clearing them.
- The phone is reached through `bench-laptop` (`ssh -F ~/.ssh/config bench-laptop`), adb serial
  `SERIAL`. No root.

## Build

`./gradlew assembleDebug` (offline works: AGP 9.4.1 / Gradle 9.8.1 are in `~/.gradle`).
`scripts/deploy.sh` copies the APK to bench-laptop and installs it.
