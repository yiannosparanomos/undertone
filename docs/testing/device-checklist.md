# Real-phone checklist (layer 3)

Run before every release on a real phone (reference device: Xiaomi Redmi Note 14 Pro,
HyperOS). Open a **Release test run** issue from the template and tick items there.
Emulators cannot reproduce HyperOS battery management, Xiaomi permission screens or real
audio routing, so these checks only happen here.

Prepare: install the release candidate, keep a second phone (or a friend) to place calls, and
have a test app or account that can send notifications on demand (Messenger, Gmail).

| ID | Check | How | Pass when |
|---|---|---|---|
| D1 | Fresh install | Uninstall, install, run the setup tour | Every step completes; Health check is all green |
| D2 | Xiaomi settings detection | Toggle Autostart and battery mode | Health check reflects each change |
| D3 | Watchdog | Settings → Apps → UnderTone → Force stop, with My rules active | DND turns off; warning on next unlock |
| D4 | Reboot | Reboot with My rules active, don't open the app | Rules still apply after unlock |
| D5 | Permission loss | Revoke notification access | Health check flags it; DND is released |
| D6 | Own sounds under DND | Message from a Ring app | Chosen sound plays; volume slider works |
| D7 | Silent | Message from a Silent app | In the shade, no sound or vibration |
| D8 | Hidden and release | Hidden app outside hours, then wait for hours to start (or set hours a few minutes ahead) | In held inbox; released at start with one summary |
| D9 | Release all | Several held, tap Release all | All in the shade, one summary |
| D10 | Blocked | Notification from a blocked app | Never visible; daily count in log |
| D11 | Open from log | Tap a log entry | Source app opens at that message (before a reboot) |
| D12 | Ring tiers | For each tier, call from a contact, an allow-list contact, an unknown number | Rings exactly as the tier says |
| D13 | Repeat callers | Tier Nobody: call twice within 2 min, then twice 3 min apart | Second call rings only within the window |
| D14 | Missed calls | Any silenced call | Appears as missed in the dialer |
| D15 | Widget and tile | Switch profiles from each | Profile and status line update everywhere |
| D16 | Allow all | Switch while notifications are held | All held released |
| D17 | Calendar | Create a short event matching a rule in the synced calendar | Rule applies during the event, ends after |

Record failures as bug issues linked from the release test run.
