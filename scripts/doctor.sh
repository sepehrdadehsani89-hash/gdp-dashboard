#!/usr/bin/env bash
set -euo pipefail
source "$(dirname -- "${BASH_SOURCE[0]}")/common.sh"

root_path="$(jarvis_root)"
sdk_path="$(jarvis_sdk)"
issues=0
jarvis_check_jdk || issues=$((issues + 1))

if [[ -f "$root_path/gradlew" && -f "$root_path/gradle/wrapper/gradle-wrapper.jar" ]]; then
    printf '%s\n' 'Gradle Wrapper: available'
else
    printf '%s\n' 'Missing Gradle Wrapper files. Use a complete source archive/checkout.' >&2
    issues=$((issues + 1))
fi

if [[ -z "$sdk_path" || ! -d "$sdk_path" ]]; then
    printf '%s\n' 'Android SDK not found. Set ANDROID_HOME to the SDK directory.' >&2
    issues=$((issues + 1))
else
    for sdk_component in platforms/android-35/android.jar build-tools/35.0.0/aapt2 ndk/26.3.11579264/build/cmake/android.toolchain.cmake cmake/3.22.1/bin/cmake; do
        if [[ -f "$sdk_path/$sdk_component" ]]; then
            printf 'SDK component: %s\n' "$sdk_component"
        else
            printf 'Missing SDK component: %s\n' "$sdk_component" >&2
            issues=$((issues + 1))
        fi
    done
fi

if adb_bin="$(jarvis_adb)"; then
    printf '%s\n' 'ADB: available (a connected phone is optional for building)'
    if ! "$adb_bin" devices -l; then
        printf '%s\n' 'ADB could not enumerate devices. Check USB/wireless authorization and local permissions.' >&2
    fi
else
    printf '%s\n' 'ADB: missing; install Android platform-tools for computer-to-phone installation.'
fi

if ((issues > 0)); then
    printf 'Doctor found %s required build prerequisite(s) to fix. See docs/INSTALL.md.\n' "$issues" >&2
    exit 1
fi
printf '%s\n' 'Build prerequisites found. This check does not validate phone audio or model inference.'
