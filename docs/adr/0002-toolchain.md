# 2. Toolchain and upgrade policy

Date: 2026-10-12. Status: accepted.

## Context

The scaffold was written in an environment that could not download Android dependencies, so
the first build happens in CI. The newest Android Gradle Plugin line (9.x, Gradle 9.6) changes
how Kotlin is applied to Android modules. Starting on the newest major would add risk to the
very first build.

## Decision

Start on a well-established set:

| Component | Version |
|---|---|
| Android Gradle Plugin | 8.13.2 |
| Gradle | 8.14.3 |
| Kotlin | 2.2.20 |
| Compose BOM | 2025.09.01 |
| JDK | 17 |
| compileSdk / targetSdk | 36 |
| minSdk | 29 (Android 10: needed for silencing calls through call screening) |

Versions live only in `gradle/libs.versions.toml`. Dependabot proposes minor and patch
updates weekly; CI must pass before merging. Major AGP upgrades are ignored by Dependabot and
done deliberately, in their own PR, using Android Studio's upgrade assistant.

## Consequences

- Upgrading to AGP 9 is a planned task once the app has its core features and CI history.
- Google Play's target-API requirement must be checked before each release
  (`docs/release-checklist.md`).
