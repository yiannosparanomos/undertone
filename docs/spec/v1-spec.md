# UnderTone v1 specification

The behaviour UnderTone v1 promises. Every rule here is checked by a scenario
(`docs/testing/test-matrix.md`). The original design discussion is in the shared design doc;
this file is the source of truth from now on.

## 1. Profiles

| Profile | What happens | DND |
|---|---|---|
| **My rules** (default) | The full setup: per-app levels, hours, calendar rules, call tiers, repeat callers | Owned by UnderTone |
| **Allow all** | UnderTone steps aside; the phone behaves normally; everything held is released | Off |
| **System DND** | UnderTone steps aside; Android's own DND settings apply | On, phone's own rules |

Switched from the home-screen widget, the Quick Settings tile or the Home screen.

## 2. Notification levels

Each app has a level **inside its hours** and one **outside its hours** (no hours = always
inside).

| Level | Sound/vibration | Visible on unlock | Log |
|---|---|---|---|
| Ring | Yes, sound chosen in UnderTone | Yes | No |
| Silent | No | Yes, in the shade | Yes |
| Hidden | No | No; held until the app's level stops being Hidden, then released | Yes |
| Blocked | No | No; dismissed, never released | Daily count only |

### Batch setup

1. **All apps**: one default level (suggested: Ring).
2. **Focus apps**: own level, hours and sound (e.g. Phone, Messenger, Mail, Teams).
3. **As you go**: demote annoying apps to Silent or Blocked, from the log or a suggestion.

### Blocking spam

Android does not let apps add items to other apps' notification long-press menus, so
blocking happens in UnderTone: long-press a log or Recent entry → *Block app* or *Block this
type* (that notification channel only). After repeated swipes without opening (e.g. 10 in a
week) UnderTone suggests Silent or Block. Blocked apps can be restored from the Apps screen.

## 3. Where notifications go

- **Shade**: Ring and Silent, as on any phone; emptied by opening, swiping, or the source app.
- **Held inbox**: Hidden notifications, listed in the app with a count on the widget.
  Released into the shade when the app's hours start, on Allow all, on *Release all*, or
  one by one by opening them. A release arrives quietly with one summary notification. If the
  source app withdraws a notification while it is held, it leaves the inbox too.
- **Log**: history of everything silenced or held (app, title, text, time); read-only;
  entries delete themselves after 7 days. Tapping an entry opens the source app.

## 4. Rules and precedence

Inside My rules, strongest first (ADR 0004):

1. Blocked apps stay blocked.
2. **Manual override**: *Quiet for 1 h / until 18:00 / until this event ends* (chosen apps
   may keep ringing) or *Let everything through for 30 min*. Ends by itself.
3. **Calendar rules** and the **weekly schedule**: the stricter wins.

Calendar rules read any calendar synced to the phone (Google, Outlook, …) through Android's
calendar provider; no account login. A rule targets an event series, a single event, a whole
calendar, or titles containing a word. It sets which apps ring, what every other app does,
and the call tier. All-day events are ignored by default.

## 5. Calls

Calls are never rejected: a call either rings or arrives silently and shows as missed.

| Tier | Who rings |
|---|---|
| Everyone | All calls |
| Contacts | Saved contacts |
| Allow list | The allow list |
| Nobody | No one, except through repeat callers |

Tier source: an active override's tier, otherwise the stricter of My rules' default tier and
any running calendar rule's tier.

**Allow list**: either *starred contacts* (the same list Android's DND uses) or *expanded*
(starred plus people or numbers added inside UnderTone).

**Repeat callers** (all adjustable): on/off; who counts (allow list or everyone); calls needed
(default **2**); window (2, 5 or 15 minutes, default **2**). The window includes its edge.

## 6. Sounds

While My rules is active DND mutes the phone, so UnderTone plays Ring sounds itself: one
default sound plus optional per-app sounds, vibration per app, UnderTone's own volume slider,
and a Test button. Calls use the normal ringtone.

## 7. Widget and tile

Home-screen widget (~4×2): profile switch; status line ("Teams quiet until Mon 09:00 · 4
held"); quick actions *Quiet 1h* and *Held (n)*. A Quick Settings tile switches profiles.

## 8. Reliability

- UnderTone owns DND while My rules is active.
- **Watchdog**: if UnderTone stops (battery killing, update, crash, permission reset,
  reboot), DND is turned off and the user is warned on next unlock: it fails loud.
- Alarms always ring, in every profile.
- The setup tour and Health check screen cover notification access, DND access, the
  call-screening role, contacts, calendar, and Xiaomi's Autostart and battery settings.

## 9. Privacy

No internet permission (ADR 0003). Encrypted local storage, 7-day log retention, no cloud
backup, settings export only on request. Diagnostic log holds package names and decisions,
never content.

## 10. Setup tour

1 Welcome · 2 Permissions · 3 Xiaomi/battery settings · 4 Allow list · 5 Ring tier ·
6 Repeat callers · 7 All apps default · 8 Focus apps · 9 Sounds · 10 Calendar (optional) ·
11 Widget · 12 Test.

## 11. Screens

Home (active profile and why, overrides, held count) · Apps · Calls · Rules · Held and log ·
Sounds · Health check.

## Out of scope for v1

Greek translation, holiday overrides, Wi-Fi-based profiles, keyword rules, extra custom
profiles.
