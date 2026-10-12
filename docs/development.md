# Development environment

The project is developed on a remote Ubuntu server. The server runs Android Studio, the SDK,
emulators and builds; a laptop only shows the remote desktop and, later, holds the phone's USB
cable.

```
laptop                                   server (Ubuntu 22.04)
──────                                   ─────────────────────
VS Code  ─── SSH (Remote-SSH) ─────────> repo, Claude Code, Gradle
RDP client ─ SSH tunnel 3390→3389 ─────> xrdp + XFCE ─ Android Studio, emulator
phone (USB) <─ adb over SSH (later) ──── Gradle installs
```

## Folder layout on the server

```
~/remote_developement/Projects/android-projects/
├── android-dev/   Android Studio, SDK, emulators, Gradle cache (self-contained)
└── undertone/     this repository
```

## One-time setup

1. Download Android Studio (Linux `.tar.gz`) from developer.android.com/studio into
   `android-projects/` and check its SHA-256 against the download page.
2. Run the setup script from this repo:

   ```bash
   cd ~/remote_developement/Projects/android-projects
   bash undertone/tools/dev-env/android-dev-setup.sh android-studio-*-linux.tar.gz --xrdp
   ```

   It puts everything Android-related in `android-dev/` (via `ANDROID_HOME`,
   `ANDROID_USER_HOME`, `ANDROID_AVD_HOME`, `GRADLE_USER_HOME` and Android Studio's
   `idea.properties`), installs xrdp with an XFCE session listening on **127.0.0.1:3389
   only**, and creates `android-dev/remove.sh`.
3. Make sure your home folder isn't readable by other users on the shared machine:
   `chmod 750 ~`.

## Connecting

From the laptop:

```bash
ssh -L 3390:localhost:3389 <user>@<server>
```

Then connect a Remote Desktop client to `localhost:3390` (Windows: Remote Desktop
Connection; macOS: Windows App; Linux: Remmina) and log in with the server account. In the
XFCE desktop, open a terminal and run:

```bash
~/remote_developement/Projects/android-projects/android-dev/studio
```

On first launch, accept the SDK location Android Studio proposes (inside `android-dev/sdk`),
then open the `undertone/` folder.

VS Code Remote-SSH keeps working as before; both can have the repo open at once.

## Emulators

Three AVDs cover the versions the project promises: **`UnderTone_API_29`** (Android 10, our
minSdk), **`UnderTone_API_33`** (Android 13, where `POST_NOTIFICATIONS` became a runtime
permission) and **`UnderTone_API_35`** (Android 15). Each is a Pixel 6 on a `google_apis`
x86_64 image with 4 GB of RAM and 4 cores.

`google_apis` rather than `google_apis_playstore` on purpose: Play Store images refuse
`adb root`, which you want for looking inside the encrypted database. Both flavours include
the Dialer and Contacts, without which the call-screening role cannot be granted at all.

They run headless, which is how the tests are driven:

```bash
cd ~/remote_developement/Projects/android-projects/android-dev
export ANDROID_HOME=$PWD/sdk ANDROID_AVD_HOME=$PWD/avd
sdk/emulator/emulator -avd UnderTone_API_35 -port 5558 -no-window -no-audio \
  -no-boot-anim -gpu swiftshader_indirect &
until [ "$(sdk/platform-tools/adb -s emulator-5558 shell getprop sys.boot_completed | tr -d '\r')" = 1 ]; do sleep 5; done
```

They boot in roughly 20–25 s. Ports 5554/5556/5558 keep all three up at once, which is how
`./gradlew connectedDebugAndroidTest` runs every instrumented test on all three versions in one
go. `adb -s emulator-5558 emu kill` shuts one down; close them when you are done, and note that
hardware acceleration comes from an ACL on `/dev/kvm` rather than membership of the `kvm` group.

Useful while testing by hand:

```bash
adb shell cmd notification allow_listener <pkg>/io.github.yiannosparanomos.undertone.core.system.notification.UndertoneListenerService
adb shell cmd notification allow_dnd <pkg>
adb shell cmd role add-role-holder android.app.role.CALL_SCREENING <pkg>
adb emu gsm call 5551110000      # and: adb emu gsm cancel 5551110000
adb exec-out screencap -p > shot.png
```

Creating them again from scratch, if the folder is ever wiped:

```bash
sdkmanager "system-images;android-35;google_apis;x86_64" "platforms;android-35"
avdmanager create avd -n UnderTone_API_35 -k "system-images;android-35;google_apis;x86_64" -d pixel_6
```

On the shared server, Device Manager can give an emulator up to 8 cores and 8–12 GB of RAM
(edit → Advanced) if you need a heavier one.

Build limits for Gradle are set in `android-dev/gradle/gradle.properties` (16 workers, 8 GB
heap).

## Claude Code in Android Studio

Install the **Claude Code** plugin from Android Studio's plugin marketplace and run `claude`
in Studio's terminal: changes then show in Studio's diff viewer and Claude can read Studio's
diagnostics. Claude Code in VS Code or a plain terminal works on the same repo.

## Testing on the phone (later)

The phone plugs into the laptop. To let the server's tools see it, run the laptop's adb server
and forward its port over SSH:

```bash
# laptop
adb start-server
ssh -R 5037:localhost:5037 <user>@<server>
# server (in that SSH session)
adb kill-server 2>/dev/null; adb devices   # the phone appears
```

While forwarded, the server's adb talks to the laptop, so the server's own emulators aren't
visible; use one or the other at a time. scrcpy on the laptop mirrors and controls the phone.

## Removing everything

```bash
bash ~/remote_developement/Projects/android-projects/android-dev/remove.sh          # Android tools
bash ~/remote_developement/Projects/android-projects/android-dev/remove.sh --xrdp   # + remote desktop
```
