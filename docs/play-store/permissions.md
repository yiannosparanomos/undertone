# Permissions and their justifications

Keep this file in sync with the merged manifest. Each entry is the text used in the in-app
disclosure screen and, where Play asks, in the Play Console declaration.

| Permission / access | Why UnderTone needs it | When asked |
|---|---|---|
| Notification access (`BIND_NOTIFICATION_LISTENER_SERVICE`) | To see each notification and leave it, silence it, hold it until later, or dismiss it as spam, following the user's rules | Setup tour step 2 |
| Do Not Disturb access (`ACCESS_NOTIFICATION_POLICY`) | To keep the phone quiet while My rules decides what rings, and to release DND if UnderTone stops | Setup tour step 2 |
| Call-screening role (`ROLE_CALL_SCREENING`) | To silence incoming calls the user's ring tier doesn't allow. Calls are never rejected | Setup tour step 2 |
| Contacts (`READ_CONTACTS`) | To tell whether a caller is a saved or starred contact for ring tiers and the allow list | Setup tour step 4 |
| Calendar (`READ_CALENDAR`) | Optional: to apply calendar rules during matching events | Setup tour step 10, only if enabled |
| Exact alarms (`SCHEDULE_EXACT_ALARM`) | To switch rules at the exact minute hours start or end (e.g. 18:00) | When the first hours are set; optional |
| Post notifications (`POST_NOTIFICATIONS`) | To show release summaries and watchdog warnings | Setup tour |
| Run at boot (`RECEIVE_BOOT_COMPLETED`) | To restore rules after a restart | Install time (no prompt) |
| Vibrate (`VIBRATE`) | To vibrate for Ring apps while DND is on | Install time (no prompt) |
| Package visibility (`<queries>` for launcher activities) | To list launchable apps for per-app rules. **Not** `QUERY_ALL_PACKAGES` | n/a |

**Never requested:** `INTERNET` (ADR 0003), `QUERY_ALL_PACKAGES`, `READ_CALL_LOG`,
accessibility services, SMS permissions, location.

## Prominent disclosure

Before Android's own permission screens, the setup tour shows a plain-language screen for
notification access, call screening and contacts: what is read, that nothing leaves the
phone, and that notification text is kept encrypted for at most 7 days.
