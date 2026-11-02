# Jarvis 4.0 — Neural Core verification

Signed phone artifact: `downloads/neural/Jarvis-4.0.0-arm64.apk`, package `com.jarvis.next`, version 4.0.0 / code 4, Android 10+ ARM64, launcher name **Jarvis**. It updates the existing separate Jarvis Next/Jarvis 2.0/3.0 installation with the retained key. Original Jarvis, its data and the original repository branches remain separate and unchanged. No uninstall is required.

[Implementation/setup report](../../JARVIS_NEURAL_CORE.md) · [Model audit](AUDIT.md) · [Evaluation method](EVALUATION.md) · [Individual results/build identity](../../downloads/neural/build-info.json) · [SHA256SUMS](../../downloads/neural/SHA256SUMS).

| Check | Result |
| --- | --- |
| Debug and signed release ARM64 builds | Passed |
| JVM tests | 178 passed; no failures/errors/skips |
| Debug/release lint | No errors; 24 warnings per variant |
| Android regression sweep and targeted recheck | 37 passed, 1 optional neural-voice check skipped; no unresolved failures |
| Signature and update identity | v2 verified; retained Jarvis Next certificate |
| Package, version, Jarvis labels, ARM64 ELF libraries and non-debuggable release | Verified |
| ZIP/native packaging alignment | `zipalign -c -P 16 4` passed; Vosk ELF limitation below |
| No private signing files or model weight GGUFs inside APK | Verified |
| Existing 26 original Sentinel sound resources | Retained in signed APK |
| Private signing backup | Retained outside Git; previously restored/byte-compared against this same retained key |
| Public APK retrieval | Pending immutable public-download verification |

Production source: `fd373b7cc377c6220fcb617f1a1d5ec169156d1a`. The focused live-control recheck changes only its outdated app-opening caption assertion; actual target-window, tap, draft-edit, stale-target and Send-refusal checks remain intact. The phone runtime is unchanged for the recheck. The complete sweep's original failure and the focused result remain in the machine-readable record; its final per-case results supersede that stale-caption failure.

## What the Android checks establish

Tests use the dedicated API 29 x86_64 software emulator, 480×800 mdpi, with separate build directories from ARM64. The final 38-case sweep includes all original regression flows and nine new Neural Core checks:

- Real SQLite FTS searches for chosen test text near the end of a document, Persian character normalization, missing-evidence handling, private export structure and index deletion. Existing credentials remain unchanged.
- Actual signed-catalog pack bytes, SHA-256 rejection of altered content, transactional installation, stored source/version/date/license and retrieval from the installed notes.
- Real Android-visible launchable-app discovery, a user-created alias for an available package, rejection of an unavailable package and alias revocation.
- Real WorkManager opt-in enqueue and cancellation on disable, without starting the microphone. This is scheduling-state verification, not proof of exact-time or multi-day notification delivery.
- A briefing assembled from an actual local task fixture and phone date with network/calendar options disabled, without invented external sources.
- Navigation to the new private settings pane, Spanish/Persian control navigation and no microphone activation.
- Actual JNI entry into the bundled native library, rejection of deliberately corrupt GGUF input and handle cleanup. This is an error-path check, not successful model inference.
- Private briefing playback selects on-device speech even when cloud voice and public locked chat are enabled. Actual acoustic behavior and interruption on a physical locked phone remain device checks.

The remaining regression cases cover Room/runtime/provider settings, local choice without quota-reset claims, private-memory speech routing, explicit memory save/search/categories, navigation and Persian recreation, current-target taps/draft edits and Send refusal, real projection capture, persisted read-only folder grants/read/reconnect/revoke, native OCR, actual system battery/charging events, telemetry visibility owners, location denial, Codex cancellation, native wallpaper preview/system selection/hidden/screen-off drawing, original sound decoding, actual AudioTrack playback-cursor metering and additive Sentinel settings.

The optional installed-neural-voices test is skipped because its three language models are absent. The software emulator has no acoustic output device. It can verify digital PCM cursor behavior and SoundPool decoding/queuing, not voice/sound quality. Cold startup can stall Android System UI/Quickstep under software emulation; only specifically identified system ANR dialogs were dismissed, never a Jarvis crash. Test battery overrides are restored.

Unit checks cover strict bounded model tool schemas, rejection of unknown/unsafe actions, current-input binding for app/city/date/news arguments, exact 10-second voice windows, repeated-wake/stale-result guards, follow-up rearming, app ambiguity/phonetic variants/corrections, irrelevant-memory exclusion, lexical retrieval, forecast source fields and missing rain, paired evaluation coverage, local-only overrides, profile memory/thermal gates, download resume-range integrity, public-source address validation, bounded HTML text extraction, quiet hours and time-zone/DST scheduling.

## Model and hardware limits

The official Hugging Face model metadata endpoint is blocked by this cloud's enforced destination policy (HTTP 403). There are no usable model weights in this workspace; the vendored Qwen GGUF vocabulary fixture has zero tensors and is not substituted for weights. Actual download bytes, real inference latency/throughput/RAM, multilingual quality, tool-generation quality and hallucination rate were **not measured**. The audit records unavailable values as null.

Lite is retained. Balanced is explicitly experimental, with independent license/size verification and a higher RAM admission gate. No tested replacement winner is claimed. Advanced is unavailable until suitable hardware/model support is verified. The app's 27 paired evaluation cases produce 54 answers using frozen 3.0 and current prompts in the same current runtime; this is not an original-APK, original-hardware or controlled battery comparison. A user must install a compatible model and actually run/review it on the phone.

Live Open-Meteo/weather and news egress from this cloud is not verified; weather transport/parsing/failures/cache use controlled tests. The source reader is implemented with bounded public HTTPS/DNS policy and reference attribution; a broad live-research accuracy claim is not made. Knowledge packs are small original CC0 starter notes, not an encyclopedia, and retrieval is lexical rather than cross-language semantic embedding search.

Physical Redmi 9 inference, microphone recognition/noise, EN/ES/FA speech quality, Bluetooth/calls, background activation, public locked presentation, enrolled biometric/keyguard success, notification permissions on Android 13+, Xiaomi battery optimization, long sessions, thermals, battery drain, memory pressure and sustained UI performance remain phone tests. FPS values remain caps. Exact scheduled delivery is not promised. Private histories/indexes use app-private SQLite; no blanket application-layer encryption/security-audit claim is made.

The release is intentionally unminified to retain JNI ABI. ZIP alignment passes, but bundled `libvosk.so` still has 4KiB ELF LOAD alignment; full native 16KiB-page-device compatibility is not established. No app identity or architecture change is claimed to clear Play Protect or reset Google's HTTP 429 quota.

Certificate SHA-256: `56188545a9204a61153d5bfcb7c6fe230d0b024d0f2b457256da2827523b6719`. The retained private key and encrypted backup stay outside Git. [Private recovery instructions](../../JARVIS_NEURAL_CORE.md#components-and-delivery).

Merged manifest comparison against Jarvis 3.0 found one new normal permission, `RECEIVE_BOOT_COMPLETED`, for WorkManager scheduling. No new dangerous runtime permission, broad package visibility, overlay or exact-alarm permission is declared.
