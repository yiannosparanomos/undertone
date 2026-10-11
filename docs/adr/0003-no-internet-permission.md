# 3. No internet permission

Date: 2026-10-12. Status: accepted.

## Context

UnderTone reads notification content, caller numbers, contacts and calendar events. A promise
not to misuse that data is only as good as the code behind it. An app without the internet
permission is blocked by Android itself from opening network sockets.

## Decision

UnderTone never declares `android.permission.INTERNET`, directly or through a dependency. CI
checks the merged manifest on every build and fails if the permission appears. No analytics,
crash-reporting or other network SDKs are added.

## Consequences

- Crash reports come only from Google Play's Android vitals (which needs no SDK), and from the
  local diagnostic log the user can export manually.
- Features that would need the network (sync, cloud backup, remote config) are out of scope.
  Backup is a user-initiated settings export.
- Any dependency must be checked for manifest-merged permissions before it is added.
