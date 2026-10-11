# UnderTone: project guide for Claude

UnderTone is an Android app that decides, per app and per caller, what rings, what stays
quiet and what waits until later. Profiles, weekly hours, calendar rules, call tiers and a
repeat-caller rule; switched from a home-screen widget. Open source (Apache-2.0), built
with Claude, maintained by @yiannosparanomos.

Product spec: `docs/spec/v1-spec.md`. Architecture: `docs/architecture.md`.

## Non-negotiable invariants

1. **No network access, ever.** The manifest must never declare `android.permission.INTERNET`,
   directly or through a dependency. CI fails if the merged manifest contains it. Do not add
   analytics, crash-reporting SDKs or any library that needs the network.
2. **Notification content stays in the encrypted local database.** Titles, text, phone numbers
   and contact names never go to Logcat, the diagnostic log, files, or exports. The diagnostic
   log holds package names, rule ids and decisions only.
3. **The rule engine is pure Kotlin.** `core:domain` has no Android imports and no clock: time
   is passed in. Every decision goes through `NotificationEngine` or `CallEngine`.
4. **Every behaviour has a scenario.** A change to how decisions are made needs a scenario in
   `core/domain/src/test/.../scenario/ScenarioCatalog.kt` and a row in
   `docs/testing/test-matrix.md`. Never edit an existing scenario's expectation just to make a
   test pass; if the expected behaviour really changed, say so in the PR description.
5. **Calls are never rejected**, only silenced. Blocked apps stay blocked under every My rules
   override.
6. **Nothing can be used against another person.** UnderTone acts only on the phone it is
   installed on: no remote control, no accounts, no exported components other than the
   launcher activity and the system services Android binds to.

## Modules

| Module | Holds | May depend on |
|---|---|---|
| `app` | Entry point, navigation, screens (`ui/<feature>`) | everything |
| `core:domain` | Rule engine, models, scenarios | nothing (pure Kotlin) |
| `core:designsystem` | Theme, type, shared Compose components | Compose only |

Planned (create when the first real code lands, not before): `core:data` (encrypted Room
database, DataStore settings), `core:system` (notification listener, call screening, DND
controller, sound player, calendar reader, watchdog), `widget` (Glance widget, Quick
Settings tile), and `feature:*` modules if `app` grows past a handful of screens.

## Conventions

- Package root: `io.github.yiannosparanomos.undertone`. Kotlin only; Compose for all UI.
- Style: ktlint `ktlint_official` (see `.editorconfig`). Run `ktlint -F` before committing.
- UI: only `core:designsystem` components and `MaterialTheme` tokens; no hard-coded colours
  in screens. All user-visible text in `strings.xml`. See `docs/design/ui-direction.md`.
- Dependencies: add to `gradle/libs.versions.toml` only; never hard-code versions in build
  files. Toolchain upgrades follow `docs/adr/0002-toolchain.md`.
- Architecture decisions that are hard to reverse get an ADR in `docs/adr/`.
- Commits: Conventional Commits (`feat:`, `fix:`, `docs:`, `test:`, `build:`, `ci:`,
  `refactor:`, `chore:`). Small PRs into `main`; CI must be green.

## Commands

```bash
./gradlew :core:domain:test        # scenario tests, seconds
./gradlew testDebugUnitTest        # all JVM unit tests
./gradlew assembleDebug            # debug APK
./gradlew lintDebug                # Android lint
./gradlew connectedDebugAndroidTest  # instrumented tests on a running emulator/phone
ktlint "**/*.kt" "**/*.kts"        # style check (-F to fix)
```

Dev environment (remote server + NoMachine + emulator): `docs/development.md`.

## Testing layers

1. Scenarios and unit tests: every PR, in CI (`ci.yml`).
2. Emulator tests on Android 10, 13 and 15: nightly and on tags (`emulator.yml`).
3. Real-phone checklist (`docs/testing/device-checklist.md`): before every release, tracked
   as a "Release test run" issue. HyperOS behaviour can only be checked here.

## Docs to keep in sync

When behaviour changes, update in the same PR: `docs/spec/v1-spec.md`, `docs/user-guide.md`,
`docs/testing/test-matrix.md`, and, if a permission or data use changes,
`docs/play-store/permissions.md`, `docs/play-store/data-safety.md` and
`docs/privacy-policy.md`. Add a line to `CHANGELOG.md` under Unreleased.

## Skills in this repo

- `add-scenario`: add or change a rule behaviour, test-first.
- `release-checklist`: prepare and verify a release build.
