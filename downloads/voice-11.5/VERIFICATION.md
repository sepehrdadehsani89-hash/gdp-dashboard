# Jarvis 11.5 release verification

The final signed pipeline completed successfully in 5m 4s. Source base: `2ec0be984d055461cd4d27a886c3ad7c50653d52`; the adjacent source-input manifest freezes 1571 actual build inputs, including the new source and tests. Its SHA-256 is `3514bf7c9f87dd425ee37db7a2ed0146a1a8b011c862f30381bf2be70cbd3775`. The publishing commit also contains the source changes.

| Check | Observed result |
| --- | --- |
| Package / version | `com.jarvis.next` / `11.5.0-reinstall` / code 21 |
| Unit tests | 474 passed; 0 failures, 0 errors, 0 skipped |
| Repeated logical voice protocol | 20 complete policy/coordinator sequences; not acoustic tests |
| Lint | 0 errors/fatals; 44 warnings |
| Android instrumentation | `compileDebugAndroidTestKotlin` passed; 0 runtime cases executed |
| APK signature | Verified V2, retained certificate SHA-256 `1094ecc1e01dfb873a8e63689b09dc156fff9d41f77ffdfae27e702991711bda` |
| APK alignment | 4-byte ZIP alignment verified |
| Build target | ARM64 only, min SDK 29, target SDK 35, non-debuggable |
| APK bytes | 84,609,461 |
| APK SHA-256 | `9befe589da0175b4e6c894d6319a36acc163c00cd12ff19f8b22027799145de8` |
| Merged permissions versus verified 11.4 | No additions |
| Packaging inspection | No private signing files, passwords, data export, optional model weights or test-only classes; Gmail/auth integration remains excluded |
| Prior capability presence | Existing charging, alert/reminder, widget, content, lock countdown, speech and conversation classes retained in DEX |
| Host speaker inference | Actual SHA-verified model; synthetic local fixtures only; [results](../../docs/voice-11.5/speaker-host-results.json) |
| Host Vosk acoustic benchmark | Not run: official model host explicitly denied by environment network policy |
| Connected physical device | None in ADB device list |
| Redmi 9 / real owner voice / Fish acoustic playback / measured FPS | Not verified |
| Private signer | Retained existing key; no replacement key generated or published |

The unit tests cover actual policies/coordinator with deterministic transcripts, injected clocks and synthetic embeddings; they do not prove audio was recognized on Android. The host speaker test is not human enrollment, a real-person false-accept/reject study or a Redmi 9 latency benchmark. The default adjustable owner threshold is 0.90 cosine similarity; calibrate/test new owner phrases on the phone. Replay/similar-voice rejection is not guaranteed, and Android authentication remains required for protected actions.

The attached master feature document remains a staged backlog. This is a development build. See the [implementation report and hardware acceptance steps](../../docs/voice-11.5/REPORT.md). Preserve the installed application/data if a signing mismatch is reported.
