# Architecture

UnderTone is a single-activity Kotlin app with Jetpack Compose UI. Every decision about a
notification or a call is made by one pure-Kotlin **rule engine**; Android-specific code only
feeds it events and carries out its decisions.

```
 Notifications ─┐                                     ┌─> Sound player (Ring)
 Incoming calls ─┼─> platform adapters ─> RULE ENGINE ─┼─> Notification shade (Ring, Silent)
 Calendar ──────┤      (core:system)     (core:domain) ├─> Held inbox (Hidden, via snooze)
 You (widget) ──┘                                     ├─> Log (Silent, Hidden, Blocked count)
                                                       └─> DND controller, watched by watchdog
```

## Modules

| Module | Status | Responsibility |
|---|---|---|
| `core:domain` | exists | Models, `NotificationEngine`, `CallEngine`, scenarios. Pure Kotlin, no clock. |
| `core:designsystem` | exists | Theme, typography, shared Compose components. |
| `app` | exists | Single activity, Compose navigation, the seven v1 screens under `ui/<feature>`, each with a view model over `core:data`. |
| `core:data` | exists | Encrypted Room database (log, held inbox, recent calls, blocked counts), DataStore settings, export/import. ADR 0007. |
| `core:system` | exists | `NotificationListenerService`, `CallScreeningService`, contact lookup, DND controller, sound player, boundary alarms, watchdog and boot receiver, the calendar reader and the launchable-app catalogue. |
| `widget` | planned | Home-screen widget (Jetpack Glance) and Quick Settings tile. |

Modules are added when their first real code lands; empty placeholder modules are not
created.

## The rule engine

`NotificationEngine.decide(packageName, at, state)` returns an outcome (Ring, Silent, Hidden,
Blocked, Pass-through), a reason, and for Hidden a `releaseAt` time.
`CallEngine.decide(call, state, recentCalls)` returns Ring, Silent or Pass-through.

Precedence inside My rules (ADR 0004):

1. A blocked app stays blocked.
2. An active manual override.
3. Calendar rules and the weekly schedule: the stricter wins.

For calls: an override's tier, else the stricter of the default tier and any running calendar
rule's tier; then the repeat-caller rule can still let a call ring.

`RuleState` is an immutable snapshot (profile, settings, current events, override). The
platform layer builds it and passes the current time, which keeps the engine deterministic and
lets the scenario catalog test every behaviour in milliseconds.

## Platform layer (planned design)

- **Notifications**: a `NotificationListenerService` receives each notification, asks the
  engine, then leaves it (Ring, Silent), snoozes it until `releaseAt` (Hidden), or cancels it
  (Blocked). Ring sounds are played by UnderTone's own player because DND mutes the system's.
- **Calls**: a `CallScreeningService` (call-screening role) silences calls the engine says
  shouldn't ring. Calls are never rejected.
- **DND controller**: turns Do Not Disturb on while My rules is active, with alarms always
  allowed — `INTERRUPTION_FILTER_PRIORITY`, never `INTERRUPTION_FILTER_NONE`, which mutes the
  alarm stream. The ringer mode is not a usable signal under DND (ADR 0006). A watchdog
  (boot receiver + periodic check) turns DND off and warns the user if the listener stops
  running, so the phone fails loud, not silent.
- **Rule boundaries** (e.g. 18:00): scheduled with exact alarms, re-evaluating DND and held
  notifications.

## Data

Settings in DataStore. Log, held inbox and recent calls in a Room database encrypted with
SQLCipher, key in the Android Keystore. Log entries expire after 7 days by default. Android
cloud backup is disabled; users export and import settings explicitly.

## Decisions

See [docs/adr/](adr/). New hard-to-reverse decisions get a new ADR.
