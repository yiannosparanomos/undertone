# Security policy

## Reporting a vulnerability

Please **don't open a public issue**. Report privately through GitHub:
[Security → Report a vulnerability](https://github.com/yiannosparanomos/undertone/security/advisories/new).

Include what you found, how to reproduce it, and which version you tested. You'll get an
acknowledgement within 7 days. Fixes for confirmed issues are released as soon as practical,
and you'll be credited in the release notes unless you prefer not to be.

## Supported versions

Only the latest release on Google Play (and the `main` branch) receives security fixes.

## Scope

UnderTone handles sensitive data (notification content, caller numbers, calendar events) and
holds powerful permissions (notification access, Do Not Disturb access, call screening).
Especially welcome are reports about:

- anything that would let data leave the device (UnderTone must have no network access);
- notification content, numbers or names appearing outside the encrypted database
  (logs, exports, backups, other apps);
- any component another app could trigger or bind to;
- ways to use UnderTone against someone other than the phone's owner.
