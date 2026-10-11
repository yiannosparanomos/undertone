---
name: release-checklist
description: Prepare an UnderTone release - version bump, changelog, CI and emulator checks, real-phone test run issue, Play Console upload steps. Use when the maintainer asks to cut, prepare or check a release.
---

# Prepare a release

Follow `docs/release-checklist.md`. In order:

1. **Confirm main is green**: CI and the latest emulator run both pass. If not, stop and report.
2. **Bump the version** in `app/build.gradle.kts`: `versionCode` +1, `versionName` following
   semver (patch = fixes, minor = features).
3. **Changelog**: move "Unreleased" entries in `CHANGELOG.md` under the new version and date.
4. **Docs check**: if any permission or data use changed since the last release, confirm
   `docs/play-store/permissions.md`, `docs/play-store/data-safety.md` and
   `docs/privacy-policy.md` match the code. Grep the merged manifest for permissions.
5. **Open a PR** `chore(release): vX.Y.Z` with those changes.
6. **Real-phone run**: open a "Release test run" issue from the template and ask the
   maintainer to run it. Do not tag until every item is ticked or explicitly waived.
7. **Tag** `vX.Y.Z` on main after merge; this triggers the emulator workflow.
8. **Hand over the Play Console steps** from `docs/release-checklist.md`; the maintainer
   builds and signs the release bundle. Never handle signing keys or upload credentials.
