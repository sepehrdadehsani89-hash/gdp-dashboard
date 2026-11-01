#!/usr/bin/env bash
set -euo pipefail
jarvis_ocr_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
jarvis_project_dir="$(cd -- "$jarvis_ocr_dir/../.." && pwd)"
: "${ANDROID_HOME:?Set ANDROID_HOME to the Android SDK directory}"
jarvis_gradle_command="${JARVIS_GRADLE_COMMAND:-gradle}"
cd "$jarvis_ocr_dir/Tesseract4Android"
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
JITPACK=true CMAKE_BUILD_PARALLEL_LEVEL=4 "$jarvis_gradle_command" --no-daemon --max-workers=4 :tesseract4android:assembleStandardRelease
mkdir -p "$jarvis_project_dir/app/libs"
cp tesseract4android/build/outputs/aar/tesseract4android-standard-release.aar "$jarvis_project_dir/app/libs/tesseract4android-4.8.0.aar"
sha256sum "$jarvis_project_dir/app/libs/tesseract4android-4.8.0.aar"
