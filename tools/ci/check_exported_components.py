#!/usr/bin/env python3
"""Invariant 6: nothing is exported but the launcher activity.

UnderTone acts only on the phone it is installed on. An exported component is a door into the
app from other apps, so the release manifest must contain exactly one: the launcher activity.
Library manifests merge into ours, so this is checked after merging rather than by reading our
own manifest -- androidx.profileinstaller, for one, contributes an exported receiver.

Usage: check_exported_components.py <merged-manifest.xml> [allowed-name ...]
"""

import sys
import xml.etree.ElementTree as ET

ANDROID = "{http://schemas.android.com/apk/res/android}"
DEFAULT_ALLOWED = ("io.github.yiannosparanomos.undertone.MainActivity",)


def exported_components(manifest_path):
    application = ET.parse(manifest_path).getroot().find("application")
    if application is None:
        raise SystemExit(f"No <application> in {manifest_path}")
    found = []
    for tag in ("activity", "activity-alias", "service", "receiver", "provider"):
        for element in application.findall(tag):
            if element.get(ANDROID + "exported") == "true":
                found.append(
                    (
                        tag,
                        element.get(ANDROID + "name"),
                        element.get(ANDROID + "permission"),
                    )
                )
    return found


def main(argv):
    if len(argv) < 2:
        raise SystemExit(__doc__)
    manifest = argv[1]
    allowed = set(argv[2:]) or set(DEFAULT_ALLOWED)

    found = exported_components(manifest)
    unexpected = [c for c in found if c[1] not in allowed]

    for tag, name, permission in found:
        marker = "  ok" if name in allowed else "FAIL"
        print(f"{marker} {tag} {name} permission={permission}")

    if unexpected:
        names = ", ".join(name for _, name, _ in unexpected)
        print(f"::error::Unexpected exported components (invariant 6): {names}")
        return 1
    if not found:
        print("::error::No exported components at all -- the launcher activity is missing")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
