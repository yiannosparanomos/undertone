# Changelog

All notable changes are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- Project scaffold: `app`, `core:domain`, `core:designsystem` modules, version catalog, CI.
- Rule engine (`NotificationEngine`, `CallEngine`): profiles, four levels, weekly hours
  including overnight windows, calendar rules, manual overrides, call tiers and the
  repeat-caller rule (default 2 calls within 2 minutes).
- 51 behaviour scenarios covering the v1 spec (`ScenarioCatalog`).
- First Home screen: profile switch and a live preview of an engine decision.
- M1 platform spike: throwaway probe in `tools/spike/platform-probe/` and the findings it
  produced in ADR 0006 — call screening fires for callers in contacts, `setSilenceCall`
  suppresses ringtone and vibration, early release of a snoozed notification works on API 33+
  but not on API 29, and `INTERRUPTION_FILTER_NONE` must never be used because it mutes
  alarms. HyperOS listener survival is still open (phone only).
- `core:data`: encrypted Room database (SQLCipher, passphrase wrapped by an Android Keystore
  key) holding the log, held inbox, recent calls and daily blocked counts; DataStore settings in
  the same JSON that settings export produces; retention sweep (7 days, adjustable); settings
  export and import. See ADR 0007.
- `core:system`: notification listener driving the engine (leave, snooze until `releaseAt`,
  cancel), sound player for Ring apps while DND is on, DND controller, exact alarms at rule
  boundaries, release summary, watchdog and boot receiver, and swipe-without-opening tracking
  for block suggestions.
- `RuleBoundaries` in `core:domain`: when the rules next change, which the boundary alarms use.
- Calls: `CallScreeningService` driving `CallEngine` through a `CallPipeline`, contact and
  allow-list resolution (starred contacts, or expanded with people and numbers added in
  UnderTone), and recent-call history for the repeat-caller rule. Calls are only ever silenced.
- Screens: Home (profile, override, counts, a tap to everything), Apps (All-apps default, focus
  apps, per-app levels and hours, blocked), Calls (tier, allow list, repeat callers), Rules
  (calendar rules, retention, settings export and import), Held and log (inbox, Release all,
  log with Block and Silence, block suggestions), Sounds and Health check.
- `core:system` gains the launchable-app catalogue (via a `<queries>` entry, not
  `QUERY_ALL_PACKAGES`) and the real calendar reader.
- CI now also checks invariant 6 — the release manifest exports nothing but the launcher
  activity (`tools/ci/check_exported_components.py`).
- Docs: spec, architecture and ADRs, test matrix, device checklist, development setup,
  Play Store documents, privacy policy, user guide.
