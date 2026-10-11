# 7. Encrypted storage and the settings format

Date: 2026-10-12. Status: accepted.

## Context

`core:data` stores two very different things:

- **History**: the log, the held inbox, recent calls, daily blocked counts. This holds
  notification titles and text and phone numbers — the content invariant 2 exists to protect.
- **Settings**: per-app levels, weekly hours, calendar rules, call tiers, the active profile.
  Rules and package names, no content.

They need different handling, and the choices are hard to undo once a user's phone holds data in
them.

## Decision

### History goes in a SQLCipher-encrypted Room database

Key handling: a random 32-byte passphrase is generated on first run and stored wrapped in
AES-GCM by a 256-bit key that lives in the **Android Keystore** and never leaves it. The wrapped
file next to the database is useless on its own.

Consequences accepted deliberately:

- If the Keystore key is lost — secure hardware reset, or a restore onto different hardware —
  the database cannot be read and is discarded. That is the right trade: it is seven days of
  local history, and losing it is far better than it being readable off the device.
- The Keystore key is created with `setUserAuthenticationRequired(false)`. The listener and the
  call screening service have to read and write while the phone is locked, which is precisely
  when most decisions are made.
- No `fallbackToDestructiveMigration`. Dropping someone's held notifications during an upgrade
  would be a silent failure of the kind invariant 6 and the "fail loud" rule exist to prevent.
  Migrations are written by hand and the schema JSON under `core/data/schemas/` is checked in so
  a migration shows up in a diff.

### Settings go in DataStore as JSON, unencrypted

- **JSON, not Protobuf**, because this is also the **export format**. A user who exports their
  settings should get a file they can read and check (spec §9).
- **Unencrypted**, because it holds no content — rules, package names and times. Encrypting it
  would imply the export is sensitive when the point is that it is not.
- The DTOs in `core.data.settings` are separate from the domain models on purpose: `core:domain`
  stays pure Kotlin with no dependencies (invariant 3), and the stored format can stay stable
  while the domain is refactored. `SCHEMA_VERSION` is checked on read and a file from a newer
  build is refused with a message rather than half-parsed.
- Collections are sorted before writing, so the same settings always serialise byte-identically
  and an export stays diffable.
- **An export carries no override.** A manual override is a momentary state of one phone, not a
  setting worth carrying to another.
- A settings file that cannot be read is replaced with the defaults rather than left to crash
  the app on each start. The Health check screen reports it, because quietly reverting
  someone's rules would be worse than saying so.

### Blocked notifications are counted, not stored

A blocked notification increments a per-app, per-day count and nothing else is kept (spec §2).
There is no row to leak and nothing to show in the log.

## Platform note found while building this

`ON CONFLICT ... DO UPDATE` (SQLite UPSERT) needs **SQLite 3.24**, and Android 10 — our
`minSdk` — ships **3.22**. The daily blocked counter therefore uses `INSERT OR IGNORE` followed
by an `UPDATE` inside a transaction. SQLCipher bundles a newer SQLite, so production would have
worked by luck; the version-independent form means any query here also works against the
platform database, which is what the in-memory tests use.

## Consequences

- `EncryptionTest` asserts against the raw database file: the header is not `SQLite format 3`,
  and a title, a message body and a package name written through the store do not appear
  anywhere in the bytes. A wrong passphrase fails to open it.
- 18 instrumented tests run on Android 10, 13 and 15 (the AVDs from ADR 0006).
- Retention (default 7 days, adjustable) sweeps the log, the held inbox, recent calls and
  blocked counts in one call, and reports counts only.
