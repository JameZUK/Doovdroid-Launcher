# Settings file

[← Back to the README](../README.md)

The launcher's settings live in a text file on the phone:

```
/sdcard/Android/data/uk.me.jamez.doovdroid/files/doovdroid.conf
```

The launcher creates it, with comments, the first time it runs. Changes are read every time the home
screen appears.

## What's in it

```ini
# Admin menu: type 0000 and then this PIN on the home screen (4 to 12 digits).
pin=3210

# Screen colours: lcd (pale green), light (black on white) or dark (white on black).
style=lcd

# Speed dial, top to bottom, up to 8.
speed=Alice
speed=Ben
speed=Carol|07700900003
```

| Setting | Means | Default |
|---------|-------|---------|
| `pin=` | The admin PIN. The code to type is `0000` followed by this | `3210` |
| `style=` | Screen colours: `lcd`, `light` or `dark` | `lcd` |
| `speed=` | One person per line, top to bottom. The name exactly as it is in Contacts | none: starred contacts are shown |
| `speed=Name\|number` | A person with a fixed number (they don't need to be in Contacts) | |

Lines starting with `#` are ignored. The [speed-dial editor](speed-dial.md) only rewrites the
`speed=` lines; everything else is left as it is.

## Editing it from a computer

```sh
scripts/conf.sh pull     # copies it to pulled/doovdroid.conf
# edit pulled/doovdroid.conf
scripts/conf.sh push     # puts it back
```

`pulled/` is ignored by git, because the file contains people's names.

> **Don't use `adb push` to replace the file.** A file the adb shell creates there gets the wrong
> security label, and the launcher can't read it. It then quietly uses the defaults. `conf.sh push`
> overwrites the launcher's own file in place instead, which keeps it readable. If the file is
> missing, open the launcher once and it will create one.
