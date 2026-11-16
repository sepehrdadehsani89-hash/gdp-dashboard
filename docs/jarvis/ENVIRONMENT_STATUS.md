# Environment status — 2026-11-16

Managed Codex Cloud, Debian 13 x86_64, independent of the owner's Codespace. Base checkpoint a7f2c83906d7c1f7f23bad6199dda632a94e69c7 was verified on origin/jarvis-next in this session. Local original rollback remains checkpoint/jarvis-next-before-core-repair at 0d57181; new pre-repair checkpoint is checkpoint/jarvis-next-before-voice-repair at a7f2c83. No remote writes authorized/performed in this milestone.

Installed in /workspace/.jarvis-toolchain: Temurin JDK 21.0.13+9, Android platform 35, build-tools 35.0.0, NDK 26.3.11579264, CMake 3.22.1. Existing Gradle 8.9 / AGP 8.7.3 / Kotlin 2.0.21 retained. Source /workspace/.jarvis-toolchain/env.sh before invoking the wrapper. No emulator installed. Original minimal Kotlin compilation passed.

Current baseline: Kotlin compilation and 201 selected voice/sentinel/power JVM cases passed. The focused new 20-case output-drain suite passed. Full regression passed 638 cases with 1 optional skipped case; debug lint passed with 0 errors, 48 warnings and 1 informational finding. Exact receipts are in TEST_RESULTS.md.

ADB enumeration previously failed with `adb_utils.cpp:315 Cannot mkdir '/home/agent/.android': Read-only file system`. No connected Android device or authorized private device route is established. Physical microphone, playback, FPS, lifecycle, power and iPad tests remain blocked. Release signing directories are not configured; never generate a replacement release key.

Companion requirements installed only in an isolated work-directory venv: cryptography 46.0.3, websocket-client 1.9.0, qrcode 8.2. No Android dependencies added/upgraded. Existing companion suite passed 36 tests, including an actual local HTTPS + Chromium fixture. These checks do not imply Codespace/native Windows/macOS access or an internet deployment.
