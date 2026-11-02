# Jarvis 3.0 — Sentinel verification

Phone artifact: `downloads/sentinel/Jarvis-3.0.0-arm64.apk`, Android 10+ ARM64, package `com.jarvis.next`, code 3, launcher label **Jarvis**. Source revision: `fcb489512034b7efee784fc6622cd1ba63e772e1`. It updates the separate Jarvis Next/Jarvis 2.0 installation with its retained signing key. Original Jarvis and its data remain separate. No uninstall is required.

[Feature/setup and private recovery guide](../JARVIS_SENTINEL.md) · [Build identity and individual Android results](../downloads/sentinel/build-info.json) · [SHA256SUMS](../downloads/sentinel/SHA256SUMS).

| Check | Result |
| --- | --- |
| Debug and signed release ARM64 builds | Passed |
| JVM tests | 144 passed; no failures/errors/skips |
| Debug/release lint | No errors; 21 warnings per variant |
| Complete Android regression sweep | 28 passed, 1 optional neural-model check skipped; no failures |
| APK signature | v2 verified; retained Jarvis Next certificate |
| ARM64-only ELF libraries, non-debuggable release, Jarvis locale labels | Verified |
| ZIP/native packaging alignment | `zipalign -c -P 16 4` passed; see Vosk ELF limit below |
| Original sound resources | All 26 packaged and decoded by SoundPool; queued playback exercised |
| Private signing backup | Decrypted and byte-compared against retained key/password; no secrets published |
| Public APK retrieval | Passed: 78,717,872 bytes, SHA-256 matches signed local artifact |

## What was exercised

The dedicated API 29 x86_64 software emulator uses 480×800 mdpi, without hardware acceleration or an acoustic output device. Its APKs use separate build directories; the phone artifact contains ARM64 libraries only. Debug output also exists at `app/build/outputs/apk/debug/app-debug.apk`; it is for development and is not the signed phone download.

The 29-case final sweep covers actual Room/runtime/provider flows; local-AI choice without claiming quota reset; private-memory speech routing; native OCR; genuine Android MediaProjection capture; target-bound accessibility taps and draft edits with Send refusal; persisted read-only SAF grants/read/reconnect/revoke; real Android battery/charger overrides and re-entry; visibility owners; searchable categorized memory; navigation and Persian recreation; location denial and Codex cancellation. New Sentinel checks cover:

- The visible settings button opens Android's real live-wallpaper preview. Preview does not start a microphone or silently install the wallpaper. The test uses Android's **Set wallpaper** confirmation, verifies the selected component and unchanged launcher, and observes actual battery updates on the home-screen engine.
- Reduced-motion drawing is sampled after the deliberate battery/surface changes settle. Emulator sample: 0 submitted frames over 1001 ms; app PSS 165,549 KiB on the software emulator. This is submitted Canvas frames and app PSS, not physical FPS, CPU utilization, or a battery benchmark.
- Screen-off and covered wallpaper surfaces stop drawing. The dedicated test restores the emulator's wallpaper and battery after checking; the release requests no silent wallpaper-setting permission.
- Sentinel preferences preserve existing memory and provider credentials. All 26 original sound assets decode, and policy-permitted playback queues without enabling the mic. This does not establish acoustic quality.
- An actual Android AudioTrack plays a small synthetic PCM fixture. The meter must observe nonzero energy at an advancing playback cursor, close its worker, and release its state. Neural models are not required for this digital metering check.
- Native Canvas draws a real battery fixture recognized by OCR; portrait and landscape Persian artwork renders using Android's shaped text layout. Fixture images are synthetic test data.

Unit tests include the exact 10-second monotonic boundary, rejection of late results, repeated wake phrases not extending the deadline, recorder-ready follow-up windows without another wake phrase, stop/cancel stale-result isolation, activation choreography and return timing, foreground eligibility, frame caps, audio mixing, bounded playback bins with silence/unavailable distinction, preferences, confidence, weather routing and explicit media commands. These checks do not substitute for microphone recognition with installed language models.

Exploratory runs found a UI-thread playback-meter delay, system picker dialogs obscured by emulator System UI/Quickstep ANRs, and redundant static-wallpaper redraws from unrelated runtime state. Playback sampling now has a bounded short-lived worker, the test dismisses only specifically identified Android system ANR dialogs, and the wallpaper filters updates to its artwork. The final full sweep follows these fixes. No Jarvis crash is dismissed or hidden by the test helper.

Lint warnings comprise existing persistence/SDK/dependency warnings and license TextView text assembly. The three trust-manager findings point into the bundled Bouncy Castle dependency; application HTTP clients do not install permissive trust managers or hostname verifiers. Lint success is not a claim of a full security audit.

## Remaining checks on the phone

- Enrolled biometric/device-credential success; physical microphone/voice quality; noisy English/Spanish/Persian wake detection with each compatible selected model; ten-second spoken conversations end to end; calls, Bluetooth, screen-off service survival, notification permissions and Xiaomi battery optimization. The optional three-language neural synthesis check is skipped because its models are absent. System/cloud speaking motion is labelled decorative where PCM is unavailable.
- Real Open-Meteo egress is blocked by this cloud's restricted network policy. HTTP parsing, failures, cancellation, city/coordinate and stale-cache behavior use controlled tests. No live forecast is represented as verified here.
- Actual downloaded Qwen generation and real WhatsApp/photo sends are not exercised. Models remain explicit optional downloads; sending stays manual. Local inference does not alter Google's HTTP 429 quota.
- Lock-screen wallpaper destinations and automatic background activity launch depend on Android/Xiaomi. The notification is the fallback; the app never bypasses keyguard or moves a known foreign live-control target.
- Physical Redmi 9 FPS, thermal behavior, CPU use, long sessions, memory pressure, large-font/rotation stress and battery drain remain unmeasured. Rendering caps are policy limits. The static emulator sample does not prove a leak-free long-running session.
- ZIP alignment passes, but bundled `libvosk.so` retains 4KiB ELF LOAD alignment. Native 16KiB-page Android compatibility is not established; the intended Redmi 9/API 29 emulator is not a 16KiB-page test device.

The release remains intentionally unminified to preserve existing JNI bindings. Certificate SHA-256: `56188545a9204a61153d5bfcb7c6fe230d0b024d0f2b457256da2827523b6719`. The private keystore, password and encrypted key archive stay outside Git and the APK. [Copy the encrypted backup privately and keep its unlock file separately](../JARVIS_SENTINEL.md#files-and-delivery); the cloud workspace is not a durable backup.

No claim is made that the app identity clears Play Protect or changes Google quota. Remote `main` and `jarvis-android-download` are preserved.

Emulator evidence: [selected home wallpaper](images/sentinel-home-emulator.png) · [synthetic Persian landscape renderer fixture](images/sentinel-rtl-landscape-fixture.png).

Immutable verified [phone download](https://raw.githubusercontent.com/sepehrdadehsani89-hash/gdp-dashboard/478c7e43a021689fe05d7be11e34b751a38d1bbd/downloads/sentinel/Jarvis-3.0.0-arm64.apk). SHA-256: `9bd1bc1d31a39602df4ad74ffe7f74bfb6ed32bc5be6eecd32a6146eba3f82ab`. Public retrieval matched the signed local APK byte for byte.
