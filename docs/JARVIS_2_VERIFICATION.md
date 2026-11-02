# Jarvis 2.0 verification

Phone artifact: `downloads/jarvis/Jarvis-2.0.0-arm64.apk`, Android 10+ ARM64, package `com.jarvis.next`, code 2. The label is Jarvis in English, Spanish and Persian. This updates the separate Jarvis Next installation with its retained key; original Jarvis remains separate. See the [feature and private recovery guide](../JARVIS_2.md).

Final machine-readable results and checksum are in [build-info.json](../downloads/jarvis/build-info.json) and [SHA256SUMS](../downloads/jarvis/SHA256SUMS).

| Check | Result |
| --- | --- |
| Debug and signed release ARM64 build | Passed |
| JVM tests | 117 passed, zero errors/failures |
| Debug/release lint | Zero errors; 20 warnings per variant |
| Android regression sweep + focused navigation recheck | 22 passed, 1 optional neural-voice check skipped; final unresolved failures: 0 |
| APK signature | v2 verified, retained Jarvis Next certificate |
| ARM64-only native libraries, non-debuggable release, Jarvis locale labels | Verified |
| ZIP/native alignment | Verified |
| Private encrypted signing backup decrypt/compare | Verified; secrets remain outside Git |
| Public download byte-for-byte check | Passed: 78,271,819 bytes, SHA-256 matches signed local artifact |

Android checks exercise real Room/runtime flows, provider settings with explicit test fixtures, native OCR, actual MediaProjection capture, target-bound accessibility tap/edit with Send refusal, connected SAF folder grants/read/reconnect/revoke, actual system battery overrides and foreground re-entry, overlapping visibility owners, startup telemetry, memory search/category/save, navigation, RTL/recreation, reduced-motion preferences, location denial, and Codex cancellation. Test battery overrides are always reset. UI tests run on a dedicated 480×800 API 29 emulator; screenshots use synthetic test data.

The 23-case full sweep and the focused navigation recheck supersede earlier exploratory failures. The final navigation assertion checks the visible composer because returning Home correctly restores the conversation scroll position; the application runtime was unchanged for that recheck. Those identified lazy-list scrolling assertions, repeated synthetic note names, inherited emulator AC power, and immediate battery/overlapping-activity observation. Tests were updated for the new UI and the runtime observation issues were fixed. The private-memory speech-routing check retains its exact safety assertion and allows more time on the software emulator.

The release is intentionally unminified to preserve the proven native JNI field/callback ABI. Signing certificate SHA-256: `56188545a9204a61153d5bfcb7c6fe230d0b024d0f2b457256da2827523b6719`. No private keystore, password or encrypted key archive is packaged in the APK or published in the repository.

## Limits and blocked checks

- Open-Meteo's real endpoint is blocked by the cloud's restricted network policy (proxy CONNECT 403). Parsing, HTTP errors, cancellation, bounds, city/coordinate requests and stale-cache logic pass controlled tests. A live forecast from this cloud environment is **blocked**, not a passed live-service test.
- The optional three neural voice models are absent on the emulator, so that synthesis test is skipped. Physical microphone quality, deep-voice sound quality, noisy wake recognition, background activation and call interruption need an actual configured phone. English/Spanish/Persian phrase matching and microphone level calculations are unit-tested; no English model is represented as multilingual.
- Authentication state isolation, expiry, failure/cancel/replay rules are unit-tested; cancellation is exercised through the real app UI. Successful enrolled biometric/device-credential authentication is not exercised on this emulator. Android's official prompt remains required; the app never reads a PIN.
- Actual Qwen model generation and real WhatsApp/photo sends are not exercised here. Local inference requires the explicit model download; Send remains user-controlled. Device-specific battery optimization and Android background launch restrictions still apply.
- Portrait small-screen rendering and Persian activity recreation are covered; physical Redmi 9 performance, landscape/font-scale stress, battery drain and long microphone sessions are not measured. FPS values are caps, not benchmark results.
- System/cloud speaking animation is expressly decorative. The visualizer reports real captured microphone levels when available. CPU load, network bandwidth and battery-time estimates are not invented.

No claim is made that renaming the app clears Play Protect or fixes Google's HTTP 429 quota.

Immutable verified [phone download](https://raw.githubusercontent.com/sepehrdadehsani89-hash/gdp-dashboard/2227668a8bbb8db2b0c89de8376a607f404ad171/downloads/jarvis/Jarvis-2.0.0-arm64.apk). Public retrieval matched SHA-256 `cc0951f90ceaeb0dbb009afe40e00cd41b28d4bac903e7425d0b34d48abadf65`. Remote `main` and `jarvis-android-download` were verified unchanged after publication.
