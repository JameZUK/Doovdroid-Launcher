# CLAUDE.md — working in Doovdroid

A Nokia-3210-style launcher for a Doov keypad phone. Read `README.md`, then `docs/device.md` and
`docs/development.md`. **The GitHub repo is public** (`JameZUK/Doovdroid-Launcher`).

## Rules

- **No personal data in the repo, ever:** no phone numbers, contact names, messages, the phone's
  serial, or internal host names. Screenshots use made-up names (07700 900xxx numbers) swapped into
  the settings file and then swapped back. Where this checkout's phone is lives in the git-ignored
  `scripts/local.env`.
- **Anything setup changes on a phone** goes in `scripts/phone/setup.sh` *and* its exact undo in
  `scripts/phone/restore.sh`, and in the "What setup changes" table in `docs/installing.md`.
- **Commit between changes**, one logical step per commit. User-visible changes go in `CHANGELOG.md`.
- **Docs are one page per function** under `docs/`, linked from the README table. Keep them short
  and plain; update the page a change affects.
- **Testing on the real phone** (it's in use): never send OK / green / `KEYCODE_CALL` /
  `KEYCODE_MENU` on the home screen (it calls someone), never open conversations, never screenshot
  the notification pull-down. See "Testing on a real phone" in `docs/development.md`.

## Build and run

`scripts/deploy.sh` builds and installs; `scripts/adb.sh …` reaches the phone; `scripts/conf.sh`
edits its settings file. All read `scripts/local.env`.
