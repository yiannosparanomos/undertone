# UnderTone

**Quiet notification rules for Android.** UnderTone decides, per app and per caller, what
rings, what stays quiet, and what waits until later, and it never needs the internet to do it.

> Status: early development (v0.1). Not yet on Google Play.

## What it does

- **Three profiles**, one tap apart on a home-screen widget: *My rules*, *Allow all*,
  *System DND*.
- **Four levels per app**: Ring (with a sound you choose), Silent (visible, no sound),
  Hidden (held until the app's hours start), Blocked (spam, dismissed).
- **Active hours per app**: Teams rings on weekdays 09:00–18:00 and waits quietly otherwise.
- **Calendar rules** from any calendar synced to the phone: quiet during every
  *Weekly standup*, hidden during focus blocks.
- **Call tiers**: Everyone, Contacts, Allow list or Nobody. Calls are never rejected, only
  silenced.
- **Repeat callers**: someone on your allow list who calls twice within 2 minutes gets
  through (both numbers adjustable).
- **A log of what was quieted**, so nothing important disappears.

## Privacy by construction

UnderTone has **no internet permission**: Android itself prevents it from sending anything off
your phone. Notification content is stored only in an encrypted database on the device and
deleted after 7 days. CI fails the build if the internet permission ever appears. See the
[privacy policy](docs/privacy-policy.md).

## Building

Requirements: JDK 17 and the Android SDK (Android Studio installs both).

```bash
./gradlew :core:domain:test   # rule-engine scenarios
./gradlew assembleDebug       # app/build/outputs/apk/debug/
```

Setting up a development machine, including the remote-server setup the project uses:
[docs/development.md](docs/development.md).

## Project docs

| | |
|---|---|
| Product spec | [docs/spec/v1-spec.md](docs/spec/v1-spec.md) |
| Architecture and decisions | [docs/architecture.md](docs/architecture.md), [docs/adr/](docs/adr/) |
| Testing | [docs/testing/test-matrix.md](docs/testing/test-matrix.md) |
| User guide | [docs/user-guide.md](docs/user-guide.md) |
| Contributing | [CONTRIBUTING.md](CONTRIBUTING.md) |
| Security | [SECURITY.md](SECURITY.md) |

## Built with Claude

UnderTone is designed and maintained by [@yiannosparanomos](https://github.com/yiannosparanomos)
and developed with the help of [Claude](https://claude.ai) (Anthropic). Commits that Claude
helped write carry a `Co-Authored-By: Claude` trailer. Every change is reviewed by the
maintainer, and the same rules apply to all contributions (see [CONTRIBUTING.md](CONTRIBUTING.md)).

## License

[Apache License 2.0](LICENSE). Copyright 2026 Yiannos Paranomos.
