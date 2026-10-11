# Roadmap to v1.0

Milestones in order. Each one ends with CI green and the docs it touches updated (see
`CLAUDE.md`). Tick items as they merge.

## M0: Scaffold ✅

Modules, rule engine with 51 scenarios, CI, docs, dev environment.

## M1: Platform spike (de-risk before building)

Throwaway probe (`tools/spike/platform-probe/`) run on emulators at API 29, 33 and 35, plus a
phone session for what only HyperOS can answer. Findings in
`docs/adr/0006-platform-findings.md`.

- [x] Call screening is consulted for callers **in** contacts, and `setSilenceCall` works —
      yes on all three, and silencing suppresses vibration too
- [x] UnderTone's own sounds play while DND is on, and which volume stream they follow —
      `STREAM_NOTIFICATION` under `INTERRUPTION_FILTER_PRIORITY`; never use
      `INTERRUPTION_FILTER_NONE` (it mutes alarms) and never read the ringer mode
- [x] A snoozed notification can be released early, or the fallback (re-post a copy) is needed
      — early release works on API 33+, API 29 needs the copy
- [ ] HyperOS keeps the listener alive with Autostart on and battery unrestricted — **phone
      only**, still open; batch with device-checklist D2, D3, D4, D6

## M2: Data layer (`core:data`)

- [ ] DataStore for settings (`UndertoneSettings`, profile, override)
- [ ] Encrypted Room database (SQLCipher, key in Android Keystore): log, held inbox, recent calls
- [ ] 7-day expiry (adjustable); daily blocked counts
- [ ] Settings export/import to a user-chosen file

## M3: Notification pipeline (`core:system`)

- [ ] `NotificationListenerService` → engine → leave / snooze until `releaseAt` / cancel
- [ ] Sound player for Ring apps (default + per-app sound, vibration, volume)
- [ ] DND controller (alarms always allowed)
- [ ] Watchdog + boot receiver: fail loud
- [ ] Exact alarms at rule boundaries; release summary notification
- [ ] Swipe-without-opening tracking for block suggestions

## M4: Calls

- [ ] `CallScreeningService` with the call-screening role → `CallEngine`
- [ ] Recent-call history for repeat callers; allow list (starred or expanded)

## M5: Screens

- [ ] Apps: All-apps default, focus apps, levels in/out of hours, hours editor, blocked section
- [ ] Calls: tier, allow list, repeat-caller settings
- [ ] Rules: calendar rules (calendar picker, event series/title/calendar), overrides
- [ ] Held and log: inbox, Release all, log with long-press Block, suggestions
- [ ] Sounds; Health check

## M6: Setup tour

- [ ] The 12 steps in `docs/spec/v1-spec.md` §10, with the prominent-disclosure screen

## M7: Widget and Quick Settings tile

- [ ] Glance widget (profile switch, status line, Quiet 1h, Held count); QS tile

## M8: Polish

- [ ] Dot-matrix display font (OFL), final icon
- [ ] Accessibility pass (TalkBack, contrast, 200% font)
- [ ] Emulator tests per scenario group (test matrix "L2" column)

## M9: Release

- [ ] Real-phone release test run (`docs/testing/device-checklist.md`)
- [ ] Play listing graphics and screenshots
- [ ] Closed test: 14 testers, 14 days; then production

## Later

AGP 9 upgrade (ADR 0002), Greek translation, holiday overrides, Wi-Fi profiles, keyword rules.
