#!/usr/bin/env bash
set -euo pipefail
source "$(dirname -- "${BASH_SOURCE[0]}")/common.sh"

root_path="$(jarvis_root)"
cd -- "$root_path"
jarvis_check_jdk
[[ -f gradlew ]] || { printf '%s\n' 'Missing Gradle Wrapper. Use the complete source archive.' >&2; exit 1; }
sdk_path="$(jarvis_sdk)"
[[ -n "$sdk_path" && -d "$sdk_path" ]] || { printf '%s\n' 'Set ANDROID_HOME to an installed Android SDK.' >&2; exit 1; }

# Keep callers' toolchain and network settings. Do not download unverified
# dependencies, accept new licenses, or change system configuration here.
if (($#)); then
    bash ./gradlew --console=plain "$@"
else
    bash ./gradlew --console=plain :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
fi

apk_path="$root_path/app/build/outputs/apk/debug/app-debug.apk"
if [[ -f "$apk_path" ]]; then
    printf '\nDebug APK: %s\n' "$apk_path"
    if command -v sha256sum >/dev/null 2>&1; then
        sha256sum "$apk_path"
    elif command -v shasum >/dev/null 2>&1; then
        shasum -a 256 "$apk_path"
    fi
fi
