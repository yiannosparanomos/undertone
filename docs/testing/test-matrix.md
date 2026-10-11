# Test matrix

Every promised behaviour, the scenario that checks it, and the layers that cover it.

| Layer | Where | When |
|---|---|---|
| **1. Scenarios** | `ScenarioCatalog` (JUnit, `core:domain`) | Every push and PR (`ci.yml`) |
| **2. Emulator** | Instrumented tests on Android 10, 13, 15 | Nightly, on release tags (`emulator.yml`) |
| **3. Real phone** | `device-checklist.md` | Before every release ("Release test run" issue) |

Layer 2 currently has a Home-screen smoke test. As the platform adapters land, each scenario
group gets an emulator test that drives the real listener and call-screening services with
fake apps and simulated calls; the "L2" column tracks that.

## Profiles

| ID | Behaviour | L1 | L2 | L3 |
|---|---|---|---|---|
| P1 | Allow all lets everything through | ✓ | planned | D16 |
| P2 | System DND leaves notifications to Android | ✓ | planned | D15 |
| P3 | Allow all leaves calls to the phone | ✓ | planned | D12 |

## Levels and batches

| ID | Behaviour | L1 | L2 | L3 |
|---|---|---|---|---|
| L1 | Unconfigured app follows the All apps default | ✓ | planned | |
| L2 | Changing the default changes unconfigured apps | ✓ | planned | |
| L3 | Focus app without hours always uses its level | ✓ | planned | D6 |
| L4 | Blocked app is dismissed | ✓ | planned | D10 |
| L5 | Blocked stays blocked under Let everything through | ✓ | planned | |
| L6 | Blocked stays blocked under a permissive calendar rule | ✓ | planned | |

## Hours

| ID | Behaviour | L1 | L2 | L3 |
|---|---|---|---|---|
| H1 | Inside hours → in-hours level | ✓ | planned | |
| H2 | End time is exclusive; release at next start | ✓ | planned | D8 |
| H3 | Weekend hold released Monday at start of hours | ✓ | planned | D8 |
| H4 | One minute before end still rings | ✓ | | |
| H5 | Sunday night hold released Monday | ✓ | | |
| H6 | One minute before start: release at start | ✓ | | |
| H7 | Overnight window: held until morning | ✓ | planned | |
| H8 | Overnight window begun yesterday: released this morning | ✓ | | |
| H9 | Just before an overnight window: rings | ✓ | | |

## Calendar

| ID | Behaviour | L1 | L2 | L3 |
|---|---|---|---|---|
| C1 | Event rule silences other apps | ✓ | planned | D17 |
| C2 | Event rule keeps chosen apps ringing | ✓ | planned | D17 |
| C3 | Rule ends with the event (end exclusive) | ✓ | planned | |
| C4 | Event rule applies over in-hours apps | ✓ | | |
| C5 | Calendar can't loosen a stricter schedule | ✓ | | |
| C6 | Title match; hidden until event end | ✓ | planned | |
| C7 | All-day events ignored by default | ✓ | | |
| C8 | Overlapping events: strictest wins | ✓ | | |

## Overrides

| ID | Behaviour | L1 | L2 | L3 |
|---|---|---|---|---|
| O1 | Quiet override silences apps | ✓ | planned | D15 |
| O2 | Quiet override can keep chosen apps ringing | ✓ | | |
| O3 | Override ends by itself | ✓ | planned | |
| O4 | Let everything through beats schedule | ✓ | | |
| O5 | Hiding override releases at its end | ✓ | | D9 |
| O6 | Override beats calendar rule | ✓ | | |

## Calls

| ID | Behaviour | L1 | L2 | L3 |
|---|---|---|---|---|
| K1 | Contacts tier: contact rings | ✓ | planned | D12 |
| K2 | Contacts tier: unknown silent | ✓ | planned | D12, D14 |
| K3 | Allow-list tier: other contact silent | ✓ | planned | D12 |
| K4 | Allow-list tier: allow list rings | ✓ | planned | D12 |
| K5 | Nobody tier: single call silent | ✓ | planned | D13 |
| K6 | Repeat within window rings | ✓ | planned | D13 |
| K7 | Repeat after window silent | ✓ | planned | D13 |
| K8 | Repeat group allow list excludes others | ✓ | | |
| K9 | Repeat group everyone includes unknown | ✓ | | |
| K10 | Calls needed 3: two not enough | ✓ | | |
| K11 | Calls needed 3: third rings | ✓ | | |
| K12 | Window 5 min | ✓ | | |
| K13 | Repeat callers off | ✓ | | |
| K14 | Number formatting ignored | ✓ | | |
| K15 | Window edge inclusive | ✓ | | |
| K16 | Calendar tightens tier | ✓ | | |
| K17 | Calendar can't loosen tier | ✓ | | |
| K18 | Quiet override tier: allow list only | ✓ | | |
| K19 | Let everything through: everyone rings | ✓ | | |

## Phone-only (layer 3)

Behaviours that depend on the real phone and HyperOS: D1–D5 (setup, survival, watchdog,
reboot, permission loss), D6 (own sounds under DND), D11 (opening source apps). See
`device-checklist.md`.
