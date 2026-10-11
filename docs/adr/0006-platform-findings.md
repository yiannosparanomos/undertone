# 6. Platform findings from the M1 spike

Date: 2026-10-12. Status: accepted.

## Context

The v1 design depends on four Android behaviours that the documentation does not state
plainly enough to build on. M1 exists to settle them before `core:data` and `core:system`
are written, because the answers change the design.

The probe is a throwaway app in `tools/spike/platform-probe/`: a separate Gradle build with
its own `applicationId`, deliberately absent from the repo's `settings.gradle.kts` so it can
never reach an UnderTone APK. Every step is driven by an adb broadcast, so the whole spike
runs headless and repeatably. It logs package names, API results and booleans only, never a
title, body, contact name or number: invariant 2 holds in throwaway code too.

Measured on emulators (`google_apis` x86_64, headless) at **API 29 / 33 / 35** — Android 10,
13 and 15. Findings 1-3 are emulator results; finding 4 is unanswered and phone-only.

## Findings

### 1. Call screening IS consulted for callers who are in contacts — and `setSilenceCall` works

Identical on API 29, 33 and 35. `onScreenCall` fired for every incoming call, with
`PhoneLookup` confirming `inContacts=true`, so UnderTone's call tiers can distinguish
contacts from unknown numbers by itself. This was the single biggest design risk and it is
clear.

`CallResponse.setSilenceCall(true)` is honoured by Telecom:

| | API 29 | API 33 | API 35 |
|---|---|---|---|
| `silentRingingRequested` when silencing | yes | yes | yes |
| `START_RINGER` when silencing | no | no | no |
| `START_VIBRATOR` when silencing | no | no | no |
| `START_RINGER` when allowing | yes | yes | yes |

**Silencing also suppresses vibration**, not just the ringtone — Telecom ends its ringer path
early rather than falling through to a vibrate-only branch. The app does not need to mute
anything itself. `setRejectCall(false)` and `setDisallowCall(false)` were kept throughout, so
invariant 5 (calls are never rejected) is untouched, and the dialer still posted its
missed-call notification after a silenced call.

### 2. Own sounds under DND follow the interruption filter, not the ringer mode

Streams were probed under each `setInterruptionFilter` value with the ringer set to NORMAL.
Identical on all three API levels:

| Filter | zen_mode | Notification stream | Ring stream | Alarm stream |
|---|---|---|---|---|
| `ALL` | 0 | audible | audible | audible |
| `PRIORITY` | 1 | **not muted** | **not muted** | not muted |
| `ALARMS` | 3 | muted | muted | **not muted** |
| `NONE` | 2 | muted | muted | **muted** |

Two consequences:

- **`INTERRUPTION_FILTER_NONE` must never be used.** It mutes the alarm stream, which would
  break "alarms always ring, in every profile" (spec §9). The DND controller uses
  `PRIORITY`, or `ALARMS` when nothing but alarms should get through.
- **`AudioManager.getRingerMode()` is not a usable signal.** Under `PRIORITY` it reports
  `RINGER_MODE_SILENT` (0) while the notification and ring streams are *not* muted. Code that
  checks the ringer mode to decide whether to play would stay silent when it should not.
  Decide from the interruption filter and `isStreamMute` instead.

A Ring app's sound should therefore be played with `USAGE_NOTIFICATION` (following
`STREAM_NOTIFICATION`, which is what the user's per-app volume slider will adjust), under a
`PRIORITY` filter.

### 3. A snoozed notification can be released early — on API 33 and 35 only

Three candidates were tried, each on a fresh install so no snoozed state leaked between runs:

| Candidate | API 29 | API 33 | API 35 |
|---|---|---|---|
| `snoozeNotification(key, 1L)` on an already-snoozed notification | **stays snoozed** | releases | releases |
| `snoozeNotification(key, 0L)` | stays snoozed | stays snoozed | stays snoozed |
| `cancelNotification(key)` | stays snoozed | stays snoozed | stays snoozed |

On API 33 and 35, re-snoozing with `1L` re-posts the **original** notification within about
200 ms and `getSnoozedNotifications()` empties. That matters beyond convenience: the original
keeps its actions and `PendingIntent`s, so "tap a log entry, the source app opens at that
message" (device checklist D11) survives an early release.

On **API 29 the notification cannot be released early at all.** The fallback was verified to
work there: posting a copy while the original stays snoozed appears in the shade normally. So
Android 10 gets the copy, and loses the original's actions with it.

**The boundary is not known.** Only 29, 33 and 35 were measured, so the change happened
somewhere in API 30-33 and that range is untested.

**Decision**: take the early-release path on API 33+ and the re-posted copy below it, behind
one interface in `core:system` so the engine never sees the difference. API 30-32 therefore
gets the fallback it may not need — the safe direction, since the fallback works everywhere.
Narrowing the threshold needs an API 30/31/32 emulator run and is not worth blocking M2 for.
"Release all" and the release summary behave the same either way.

### 4. HyperOS listener survival — NOT ANSWERED

*"HyperOS keeps the listener alive with Autostart on and battery unrestricted"* cannot be
tested on an emulator: there is no image of Xiaomi's battery management. It stays open until a
phone session on the Redmi Note 14 Pro, together with device-checklist items D2, D3 and D4.

## Limits of these findings

- Emulators were run with `-no-audio`, so finding 2 describes the audio **policy** the system
  reports: which streams are muted and which filter is active. Whether a speaker is actually
  audible, and whether HyperOS overrides any of it, is a phone question (D6).
- `adb emu gsm call` drives Telecom's real call path, which is what finding 1 reads, but it is
  not a real cellular call.
- Nothing here was measured on HyperOS. Xiaomi changes DND and notification behaviour, so
  every finding is re-checked once on the phone before release.

## Consequences

- `core:system` gets a snooze-release abstraction with two implementations split at API 31.
- The DND controller never uses `INTERRUPTION_FILTER_NONE`, and never reads the ringer mode.
- Call tiers can rely on contact lookup inside the screening service.
- The probe in `tools/spike/platform-probe/` is deleted once M1 closes; this ADR is the record.
