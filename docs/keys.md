# Keys

[← Back to the README](../README.md)

The two keys either side of the D-pad are **soft keys**: they're blank (marked with a dash) and do
whatever the bar at the bottom of the screen says above them.

## What each key does

| Key | Home screen | While dialling | In the admin screens |
|-----|-------------|----------------|----------------------|
| **▲ ▼** | Move the highlight | — | Move the highlight |
| **OK** | Call the person / open the item | Call the number | Select |
| **Green** | Call the person / open the item | Call the number | Select |
| **Left soft key** | Same as OK (*Call* / *Open*) | Call (*Call*) | Select (*Select*, *Options*, *Done*) |
| **Right soft key** | Nothing | Delete a digit; hold to clear (*Delete*) | Back / close |
| **0–9** | Start dialling | Add a digit; hold **0** for **+** | — |
| **\*** **#** | Volume down / up | Volume down / up | — |
| **Red** | Screen off | Screen off (and clear) | Screen off |

Elsewhere:

| Where | Key | Does |
|-------|-----|------|
| In a call | **Red** | Hangs up |
| Incoming call | **Green** | Answers |
| Lock screen | **Left soft key** | Unlocks |
| Any app | **Right soft key** | Back |
| Anywhere | Hold **red** | Power menu (power off, restart) |

## This phone's odd keys

- **Red is the power key.** It isn't an "end call" key at all. Setup tells Android to make it hang
  up during calls; the rest of the time it turns the screen off, and the launcher makes sure the
  phone wakes up on the home screen.
- **`*` and `#` are volume keys** whenever no text box is open. The phone does this itself, and apps
  never see the keys, so they can't be dialled from the home screen and can't be part of the
  admin code.
- **Left soft key unlocks** the lock screen. So that a double press can't unlock *and* call, the home
  screen ignores OK, green and the left soft key for a moment after the screen comes back.

Details, and how they were found: [The phone](device.md).
