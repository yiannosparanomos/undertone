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

Create emulators in Device Manager. Suggested set: a Pixel-size phone on Android 15 for daily
work, plus Android 10 (minSdk) and Android 13 for checks. On the shared server, give each
emulator up to 8 cores and 8–12 GB of RAM (Device Manager → edit → Advanced) and close them
when done.

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
