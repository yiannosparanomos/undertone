# M1 platform probe (throwaway)

Answers the four platform questions in `docs/roadmap.md` M1. Findings are recorded in
`docs/adr/0006-platform-findings.md`; **that ADR is the deliverable, this app is scaffolding**
and gets deleted once M1 closes.

Deliberately isolated from the app:

- its own Gradle build, not included in the repo's `settings.gradle.kts`;
- its own `applicationId` (`…undertone.spike`), so it installs alongside UnderTone;
- no `INTERNET` permission;
- logs package names, API results and booleans only — never a notification title or body, a
  contact name or a phone number. Invariant 2 applies to throwaway code too.

## Build and install

```bash
export ANDROID_HOME=…/android-dev/sdk
./gradlew --project-dir tools/spike/platform-probe assembleDebug
adb install -r -g tools/spike/platform-probe/build/outputs/apk/debug/platform-probe-debug.apk
adb shell am start -n io.github.yiannosparanomos.undertone.spike/.MainActivity
adb shell cmd notification allow_listener io.github.yiannosparanomos.undertone.spike/.ProbeListenerService
adb shell cmd notification allow_dnd io.github.yiannosparanomos.undertone.spike
adb shell cmd role add-role-holder android.app.role.CALL_SCREENING io.github.yiannosparanomos.undertone.spike
```

On a phone the role and the listener need granting through Settings instead, since
`cmd role add-role-holder` is not available without adb.

## Drive it

Every step is a broadcast; read results from `adb logcat -s UTSPIKE`.

```bash
P=io.github.yiannosparanomos.undertone.spike
B() { adb shell am broadcast -a $P.CMD -n $P/.ProbeReceiver --es cmd "$@"; }

B state                              # is the listener bound?
B post --ei id 7                     # post a probe notification
B snooze --el ms 600000              # snooze it for 10 minutes
B snoozed-list                       # what the system still holds
B release-early                      # candidate: re-snooze with 1 ms
B release-zero                       # candidate: re-snooze with 0 ms
B cancel-snoozed                     # candidate: cancelNotification
B dnd --es filter priority           # all | priority | alarms | none
B sound --es usage notification      # notification | alarm | ringtone | media
B silence-calls --ez value true      # what the screening service will answer
```

Calls are simulated with `adb emu gsm call 5551110000` / `adb emu gsm cancel 5551110000`. The
in-contacts case needs a contact holding that number:

```bash
adb shell content insert --uri content://com.android.contacts/raw_contacts \
  --bind account_name:s:probe --bind account_type:s:probe
adb shell content insert --uri content://com.android.contacts/data --bind raw_contact_id:i:1 \
  --bind mimetype:s:vnd.android.cursor.item/phone_v2 --bind data1:s:5551110000
adb shell content query --uri content://com.android.contacts/phone_lookup/5551110000
```

Whether `setSilenceCall` worked is read from Telecom's own logging, not from the probe:
`adb logcat -d | grep -E 'START_RINGER|START_VIBRATOR|silentRingingRequested'`.

Note that uninstalling the probe does **not** clear notifications the system has snoozed on
its behalf, so test one early-release candidate per fresh install or the counts mislead.
