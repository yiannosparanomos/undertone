#!/usr/bin/env bash
# UnderTone dev environment: Android Studio, SDK, emulators and Gradle in ONE folder ($DEV),
# plus an optional remote desktop (xrdp + XFCE) listening on localhost only.
#
# Usage (from the folder where you downloaded Android Studio):
#   bash android-dev-setup.sh android-studio-*-linux.tar.gz          # Studio only
#   bash android-dev-setup.sh android-studio-*-linux.tar.gz --xrdp   # Studio + remote desktop
#
# Re-running is safe.
#
# Afterwards:
#   laptop:   ssh -L 3390:localhost:3389 <you>@<server>
#   Remote Desktop client -> localhost:3390, your server username/password
#   in that desktop, open a terminal and run:  $DEV/studio
#
# To remove everything later:  bash $DEV/remove.sh

set -euo pipefail

DEV="${DEV:-$HOME/remote_developement/Projects/android-projects/android-dev}"
STUDIO_TARBALL=""
WITH_XRDP=false

for arg in "$@"; do
  case "$arg" in
    *.tar.gz) STUDIO_TARBALL="$arg" ;;
    --xrdp)   WITH_XRDP=true ;;
    *) echo "Unknown argument: $arg (expected a .tar.gz or --xrdp)"; exit 1 ;;
  esac
done

mkdir -p "$DEV"/{sdk,avd,gradle,android-user,studio-data/{config,system,plugins,log}}

# --- Remote desktop: xrdp + XFCE (needs sudo) ------------------------------
if $WITH_XRDP; then
  if ss -ltn | grep -q ':3389 ' && ! dpkg -s xrdp >/dev/null 2>&1; then
    echo "Port 3389 is already used by something else; not installing xrdp."; exit 1
  fi
  echo "Installing xrdp and XFCE ..."
  sudo apt-get install -y xrdp xfce4 xfce4-goodies
  echo "xfce4-session" > "$HOME/.xsession"
  sudo adduser xrdp ssl-cert >/dev/null
  # Listen on localhost only: reachable through your SSH tunnel, nowhere else.
  sudo sed -i 's|^port=3389$|port=tcp://127.0.0.1:3389|' /etc/xrdp/xrdp.ini
  sudo systemctl restart xrdp
  echo "xrdp installed:"; ss -ltn | grep 3389 || true
fi

# --- Android Studio --------------------------------------------------------
if [[ -n "$STUDIO_TARBALL" ]]; then
  echo "Extracting Android Studio into $DEV ..."
  rm -rf "$DEV/android-studio"
  tar -xzf "$STUDIO_TARBALL" -C "$DEV"
elif [[ ! -d "$DEV/android-studio" ]]; then
  echo "No Android Studio .tar.gz given and none found in $DEV; skipping Studio."
fi

# --- Environment: every tool told to use $DEV ------------------------------
cat > "$DEV/env.sh" <<EOF
export ANDROID_HOME="$DEV/sdk"
export ANDROID_SDK_ROOT="$DEV/sdk"
export ANDROID_USER_HOME="$DEV/android-user"   # replaces ~/.android
export ANDROID_AVD_HOME="$DEV/avd"             # emulator images
export GRADLE_USER_HOME="$DEV/gradle"          # replaces ~/.gradle
export STUDIO_PROPERTIES="$DEV/studio-data/idea.properties"
export PATH="$DEV/sdk/platform-tools:$DEV/sdk/emulator:$DEV/sdk/cmdline-tools/latest/bin:\$PATH"
EOF

# Android Studio's own settings, caches, plugins and logs (normally ~/.config, ~/.cache, ~/.local/share)
cat > "$DEV/studio-data/idea.properties" <<EOF
idea.config.path=$DEV/studio-data/config
idea.system.path=$DEV/studio-data/system
idea.plugins.path=$DEV/studio-data/plugins
idea.log.path=$DEV/studio-data/log
EOF

# --- Resource limits (generous, shared machine) ----------------------------
cat > "$DEV/gradle/gradle.properties" <<EOF
org.gradle.workers.max=16
org.gradle.jvmargs=-Xmx8g -XX:+UseParallelGC
org.gradle.parallel=true
org.gradle.caching=true
kotlin.daemon.jvmargs=-Xmx4g
EOF

# --- Launcher --------------------------------------------------------------
cat > "$DEV/studio" <<EOF
#!/usr/bin/env bash
source "$DEV/env.sh"
exec "$DEV/android-studio/bin/studio.sh" "\$@"
EOF
chmod +x "$DEV/studio"

# --- Removal script --------------------------------------------------------
cat > "$DEV/remove.sh" <<EOF
#!/usr/bin/env bash
# Removes the Android dev environment. Pass --xrdp to also remove xrdp and XFCE.
set -euo pipefail
pkill -f "$DEV/android-studio" 2>/dev/null || true
pkill -f "$DEV/sdk/emulator" 2>/dev/null || true
rm -rf "$DEV"
if [[ "\${1:-}" == "--xrdp" ]]; then
  sudo apt-get purge -y xrdp xfce4 xfce4-goodies
  sudo apt-get autoremove -y
  rm -f "\$HOME/.xsession"
  echo "xrdp removed. Port 3389 is free again."
fi
echo "Removed $DEV."
EOF
chmod +x "$DEV/remove.sh"

echo
echo "Done. Everything lives in: $DEV"
echo "Start Android Studio from the remote desktop with:  $DEV/studio"
echo "On first launch, accept the SDK location it proposes ($DEV/sdk)."
