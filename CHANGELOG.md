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
- Docs: spec, architecture and ADRs, test matrix, device checklist, development setup,
  Play Store documents, privacy policy, user guide.
