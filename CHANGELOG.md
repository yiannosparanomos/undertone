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
- Docs: spec, architecture and ADRs, test matrix, device checklist, development setup,
  Play Store documents, privacy policy, user guide.
