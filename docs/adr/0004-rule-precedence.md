# 4. Rule precedence

Date: 2026-10-12. Status: accepted.

## Context

Several sources can apply to one notification at the same time: an app's level and hours, a
calendar rule, a manual override, the active profile. Users need to be able to predict the
result.

## Decision

1. **Profile**: Allow all and System DND step aside completely; only My rules runs the engine.
2. **Blocked apps stay blocked** under My rules, whatever else applies.
3. **A manual override beats everything else** while it is active, and ends by itself.
4. **Calendar rules and the weekly schedule: the stricter wins.** A calendar rule can make
   things quieter but never louder than the schedule.
5. Strictness order: Ring < Silent < Hidden < Blocked; for calls Everyone < Contacts <
   Allow list < Nobody.
6. Calls are never rejected. If the tier doesn't allow a caller, the repeat-caller rule
   (default: allow list, 2 calls within 2 minutes, all adjustable) can still let it ring.

## Consequences

Each rule is covered by scenarios (C5, K17 for "calendar can't loosen", L5–L6 for blocked
apps, O4–O6 for overrides).
