# Data safety form answers

Answers for Play Console → App content → Data safety. Google defines "collected" as data
transmitted off the device; processing that stays on the device is not collection.

| Question | Answer | Reason |
|---|---|---|
| Does your app collect or share any of the required user data types? | **No** | No internet permission; nothing leaves the device |
| Is all user data encrypted in transit? | Not applicable | No data is transmitted |
| Do you provide a way for users to request that their data is deleted? | Yes | Data is on-device; uninstalling or *Clear log* deletes it; log entries auto-expire after 7 days |
| Committed to Play Families Policy? | Not applicable | Not directed at children |
| Independent security review? | No | |

## Data processed on the device only (for the privacy policy, not the form)

Notification content, caller numbers, contact starred status, calendar events (optional),
list of launchable apps. See `docs/privacy-policy.md`.

## Re-check before every release

If a dependency or feature ever transmits data (it must not; ADR 0003), these answers become
wrong and the release must stop.
