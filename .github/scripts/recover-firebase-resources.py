#!/usr/bin/env python3
"""Recover non-secret Firebase Android client strings from a verified older APK.

This fallback is used only if FIREBASE_ANDROID_CONFIG_JSON was not configured
in GitHub secrets. FCM keeps its original sender/project and device tokens.
Never log the actual client values.
"""
from pathlib import Path
from html import escape
import re
import sys

if len(sys.argv) != 3:
    raise SystemExit("Usage: recover-firebase-resources.py aapt2-dump destination.xml")

dump = Path(sys.argv[1]).read_text(encoding="utf-8", errors="replace").splitlines()
out = Path(sys.argv[2])
keys = (
    "google_app_id",
    "google_api_key",
    "gcm_defaultSenderId",
    "project_id",
    "google_storage_bucket",
    "firebase_database_url",
)
values = {}
resource_line = re.compile(r"(?<![A-Za-z])string/(" + "|".join(keys) + r")\b")
quoted = re.compile(r'"((?:\\.|[^"\\])*)"')

for index, line in enumerate(dump):
    found = resource_line.search(line)
    if not found:
        continue
    key = found.group(1)
    if key in values:
        continue
    for candidate in dump[index : min(index + 8, len(dump))]:
        if candidate != line and re.match(r"\s*resource\s+0x", candidate):
            break
        # Default configuration has empty parentheses in AAPT2 output.
        if candidate != line and not re.search(r"^\s*\(\s*\)", candidate):
            continue
        match = quoted.search(candidate)
        if match:
            values[key] = match.group(1)
            break

required = {"google_app_id", "google_api_key", "gcm_defaultSenderId", "project_id"}
missing = required - values.keys()
if missing:
    raise SystemExit("Verified previous APK does not contain required Firebase resources: "
                     + ", ".join(sorted(missing)) + ". Please configure FIREBASE_ANDROID_CONFIG_JSON.")

out.parent.mkdir(parents=True, exist_ok=True)
xml = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
for key in keys:
    if key in values:
        xml.append('    <string name="' + key + '" translatable="false">' +
                   escape(values[key]) + "</string>")
xml.append("</resources>")
out.write_text("\n".join(xml) + "\n", encoding="utf-8")
print("Recovered Firebase client resources from the verified previous release.")
