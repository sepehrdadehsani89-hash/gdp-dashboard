#!/usr/bin/env bash
set -euo pipefail
source "$(dirname -- "${BASH_SOURCE[0]}")/common.sh"

usage() {
    printf '%s\n' 'Usage: scripts/install.sh [--serial SERIAL] [--apk FILE] [--no-build]' \
        'Builds a debug APK, installs it on one authorized Android device, and opens Jarvis.' \
        'No permissions or special access are granted by this script.'
}

root_path="$(jarvis_root)"
serial=""
apk_path="$root_path/app/build/outputs/apk/debug/app-debug.apk"
build_apk=true
while (($#)); do
    case "$1" in
        --serial) [[ $# -ge 2 && -n "$2" ]] || { usage >&2; exit 2; }; serial="$2"; shift 2 ;;
        --apk) [[ $# -ge 2 && -n "$2" ]] || { usage >&2; exit 2; }; apk_path="$2"; build_apk=false; shift 2 ;;
        --no-build) build_apk=false; shift ;;
        --help|-h) usage; exit 0 ;;
        *) printf 'Unknown option: %s\n' "$1" >&2; usage >&2; exit 2 ;;
    esac
done

adb_bin="$(jarvis_adb)" || { printf '%s\n' 'ADB is missing. Install Android platform-tools or install the APK directly on your phone.' >&2; exit 1; }
device_listing="$("$adb_bin" devices)"
if [[ -n "$serial" ]]; then
    state="$(awk -v device="$serial" '$1 == device { print $2 }' <<< "$device_listing")"
    [[ "$state" == device ]] || { printf 'Device %s is not connected and authorized (state: %s).\n' "$serial" "${state:-absent}" >&2; exit 1; }
else
    connected_devices=()
    while IFS= read -r connected_serial; do
        [[ -z "$connected_serial" ]] || connected_devices+=("$connected_serial")
    done < <(awk 'NR > 1 && $2 == "device" { print $1 }' <<< "$device_listing")
    if ((${#connected_devices[@]} != 1)); then
        printf '%s\n' "$device_listing" >&2
        printf '%s\n' 'Connect and authorize exactly one device, or select one with --serial SERIAL.' >&2
        exit 1
    fi
    serial="${connected_devices[0]}"
fi

if [[ "$build_apk" == true ]]; then
    bash "$root_path/scripts/build.sh" :app:assembleDebug
fi
[[ -f "$apk_path" ]] || { printf 'APK not found: %s\n' "$apk_path" >&2; exit 1; }

printf 'Installing on authorized device: %s\n' "$serial"
"$adb_bin" -s "$serial" install -r "$apk_path"
"$adb_bin" -s "$serial" shell am start -n com.jarvis.assistant/.MainActivity
printf '%s\n' 'Jarvis is open. Approve only the permissions you want inside onboarding/Settings.' \
    'To use offline speech, download the selected language model in Jarvis Settings.' \
    'Test the voice, then explicitly enable wake listening while the app is visible.'
