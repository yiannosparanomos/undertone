---
name: Release test run (real phone)
about: Layer 3 checklist, run on a real phone before every release
title: "Release test run: vX.Y.Z"
labels: [release]
---

Phone: <!-- model, Android/HyperOS version -->
Build: <!-- version name and commit -->

Run every item in `docs/testing/device-checklist.md` and tick it here.

### Setup and survival
- [ ] D1 Fresh install: setup tour completes, Health check is all green
- [ ] D2 Autostart and battery "No restrictions" are detected correctly
- [ ] D3 Force-stop the app: watchdog turns DND off and warns on next unlock
- [ ] D4 Reboot: rules work again without opening the app
- [ ] D5 Revoke notification access: Health check shows it, DND is released

### Notifications
- [ ] D6 Ring app plays its chosen sound while DND is on
- [ ] D7 Silent app shows in the shade with no sound
- [ ] D8 Hidden app appears in the held inbox and is released at its hours
- [ ] D9 Release all brings held notifications back with one summary
- [ ] D10 Blocked app never appears; daily count shows in the log
- [ ] D11 Tapping a log entry opens the source app

### Calls
- [ ] D12 Each ring tier with a contact, an allow-list contact and an unknown number
- [ ] D13 Repeat caller: second call within the window rings; outside it, doesn't
- [ ] D14 Silenced calls appear as missed calls

### Profiles, widget, calendar
- [ ] D15 Widget switches profiles; Quick Settings tile does too
- [ ] D16 Allow all releases everything held
- [ ] D17 A calendar rule applies during a real synced event

Notes:
