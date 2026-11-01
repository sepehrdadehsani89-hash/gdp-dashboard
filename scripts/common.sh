#!/usr/bin/env bash
# Shared helpers; sourcing this file does not change directories or permissions.

jarvis_root() {
    (cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
}

jarvis_sdk() {
    if [[ -n "${ANDROID_HOME:-}" ]]; then
        printf '%s\n' "$ANDROID_HOME"
    elif [[ -n "${ANDROID_SDK_ROOT:-}" ]]; then
        printf '%s\n' "$ANDROID_SDK_ROOT"
    elif [[ -f "$(jarvis_root)/local.properties" ]]; then
        # Android Studio writes sdk.dir here. For escaped Windows paths use the
        # PowerShell scripts or set ANDROID_HOME explicitly.
        sed -n 's/^sdk\.dir=//p' "$(jarvis_root)/local.properties" | head -n 1
    fi
}

jarvis_adb() {
    local sdk_path
    sdk_path="$(jarvis_sdk)"
    if [[ -n "$sdk_path" && -x "$sdk_path/platform-tools/adb" ]]; then
        printf '%s\n' "$sdk_path/platform-tools/adb"
    elif command -v adb >/dev/null 2>&1; then
        command -v adb
    else
        return 1
    fi
}

jarvis_check_jdk() {
    local java_bin javac_bin java_major version_output
    if [[ -n "${JAVA_HOME:-}" ]]; then
        java_bin="$JAVA_HOME/bin/java"
        javac_bin="$JAVA_HOME/bin/javac"
    else
        java_bin="$(command -v java || true)"
        javac_bin="$(command -v javac || true)"
    fi
    if [[ ! -x "$java_bin" || ! -x "$javac_bin" ]]; then
        printf '%s\n' 'A full JDK is required. Install JDK 17 or 21 and set JAVA_HOME.' >&2
        return 1
    fi
    version_output="$("$java_bin" -version 2>&1)"
    java_major="$(sed -n 's/.*version "\([0-9]*\).*/\1/p' <<< "$version_output" | head -n 1)"
    if [[ ! "$java_major" =~ ^[0-9]+$ ]] || ((java_major < 17 || java_major > 22)); then
        printf '%s\n' 'This Gradle 8.9 build needs a supported JDK. Use JDK 17 or 21.' >&2
        return 1
    fi
    printf 'JDK %s: available\n' "$java_major"
}
